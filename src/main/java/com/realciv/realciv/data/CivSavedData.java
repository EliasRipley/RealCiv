package com.realciv.realciv.data;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RewardRule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class CivSavedData extends SavedData {
    private static final String DATA_NAME = "realciv_data";

    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    private final Map<String, CivilizationRecord> civilizations = new HashMap<>();
    private final Map<UUID, String> playerCivilization = new HashMap<>();
    private final Set<UUID> founderApprovals = new HashSet<>();

    public static CivSavedData get(MinecraftServer server) {
        ServerLevel overworld = Objects.requireNonNull(server.overworld(), "Overworld is not available");
        SavedData.Factory<CivSavedData> factory = new SavedData.Factory<>(CivSavedData::new, CivSavedData::load);
        CivSavedData data = overworld.getDataStorage().computeIfAbsent(factory, DATA_NAME);
        data.ensureDefaultCivilizationExists();
        return data;
    }

    public static CivSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CivSavedData data = new CivSavedData();

        ListTag playerTags = tag.getList("players", Tag.TAG_COMPOUND);
        for (Tag entry : playerTags) {
            if (!(entry instanceof CompoundTag playerTag)) {
                continue;
            }
            try {
                UUID playerId = UUID.fromString(playerTag.getString("id"));
                data.players.put(playerId, PlayerRecord.load(playerTag));
            } catch (Exception ignored) {
            }
        }

        if (tag.contains("civilizations", Tag.TAG_LIST)) {
            ListTag civTags = tag.getList("civilizations", Tag.TAG_COMPOUND);
            for (Tag entry : civTags) {
                if (!(entry instanceof CompoundTag civTag)) {
                    continue;
                }
                CivilizationRecord civ = CivilizationRecord.load(civTag);
                if (civ != null) {
                    data.civilizations.put(civ.id(), civ);
                }
            }
        } else {
            // Legacy migration: old single-civilization data into default civ.
            String defaultId = RealCivConfig.defaultCivilizationId();
            CivilizationRecord migrated = new CivilizationRecord(defaultId, RealCivConfig.defaultCivilizationName());

            CompoundTag stockTag = tag.getCompound("hubStock");
            for (String key : stockTag.getAllKeys()) {
                migrated.hubStock().put(key, Math.max(0L, stockTag.getLong(key)));
            }

            ListTag plotTags = tag.getList("plots", Tag.TAG_COMPOUND);
            for (Tag entry : plotTags) {
                if (!(entry instanceof CompoundTag plotTag)) {
                    continue;
                }
                PlotRecord plot = PlotRecord.load(plotTag);
                if (plot != null) {
                    migrated.plots().put(plot.plotKey(), plot);
                }
            }

            if (tag.contains("mayor")) {
                try {
                    migrated.setMayorId(UUID.fromString(tag.getString("mayor")));
                } catch (Exception ignored) {
                }
            }

            ListTag logs = tag.getList("auditLogs", Tag.TAG_STRING);
            for (Tag entry : logs) {
                migrated.auditLogs().add(entry.getAsString());
            }

            data.civilizations.put(defaultId, migrated);
        }

        CompoundTag membershipTag = tag.getCompound("playerCivilization");
        for (String playerIdRaw : membershipTag.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(playerIdRaw);
                String civId = normalizeCivId(membershipTag.getString(playerIdRaw));
                if (civId != null) {
                    data.playerCivilization.put(playerId, civId);
                }
            } catch (Exception ignored) {
            }
        }

        ListTag approvalsTag = tag.getList("founderApprovals", Tag.TAG_STRING);
        for (Tag entry : approvalsTag) {
            try {
                data.founderApprovals.add(UUID.fromString(entry.getAsString()));
            } catch (Exception ignored) {
            }
        }

        data.ensureDefaultCivilizationExists();

        // Lazy legacy account migration into default civ account.
        String defaultId = RealCivConfig.defaultCivilizationId();
        for (PlayerRecord record : data.players.values()) {
            record.migrateLegacyAccount(defaultId);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag playerTags = new ListTag();
        for (Map.Entry<UUID, PlayerRecord> entry : players.entrySet()) {
            CompoundTag playerTag = entry.getValue().save();
            playerTag.putString("id", entry.getKey().toString());
            playerTags.add(playerTag);
        }
        tag.put("players", playerTags);

        ListTag civTags = new ListTag();
        for (CivilizationRecord civ : civilizations.values()) {
            civTags.add(civ.save());
        }
        tag.put("civilizations", civTags);

        CompoundTag membershipTag = new CompoundTag();
        for (Map.Entry<UUID, String> entry : playerCivilization.entrySet()) {
            membershipTag.putString(entry.getKey().toString(), entry.getValue());
        }
        tag.put("playerCivilization", membershipTag);

        ListTag approvalsTag = new ListTag();
        for (UUID approved : founderApprovals) {
            approvalsTag.add(StringTag.valueOf(approved.toString()));
        }
        tag.put("founderApprovals", approvalsTag);

        return tag;
    }

    @Nullable
    private static String normalizeCivId(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String id = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return id.isEmpty() ? null : id;
    }

    private static String sanitizeDisplayName(@Nullable String displayName, String fallback) {
        if (displayName == null) {
            return fallback;
        }
        String name = displayName.trim();
        return name.isEmpty() ? fallback : name;
    }

    private static String slugifyCivName(String rawName) {
        String lowercase = rawName.toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder();
        boolean lastDash = false;
        for (int i = 0; i < lowercase.length(); i++) {
            char ch = lowercase.charAt(i);
            boolean allowed = (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
            if (allowed) {
                out.append(ch);
                lastDash = false;
                continue;
            }
            if (!lastDash && out.length() > 0) {
                out.append('-');
                lastDash = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '-') {
            out.deleteCharAt(out.length() - 1);
        }
        return out.toString();
    }

    private static String truncatedForSuffix(String base, int suffix) {
        String suffixText = "-" + suffix;
        int maxBaseLength = Math.max(1, 32 - suffixText.length());
        String trimmed = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
        while (trimmed.endsWith("-")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? "civ" : trimmed;
    }

    private boolean civilizationDisplayNameTaken(String displayName, @Nullable String exceptId) {
        String wanted = displayName.trim();
        for (CivilizationRecord civ : civilizations.values()) {
            if (exceptId != null && civ.id().equals(exceptId)) {
                continue;
            }
            if (civ.displayName().equalsIgnoreCase(wanted)) {
                return true;
            }
        }
        return false;
    }

    public void ensureDefaultCivilizationExists() {
        String id = RealCivConfig.defaultCivilizationId();
        if (!civilizations.containsKey(id)) {
            civilizations.put(id, new CivilizationRecord(id, RealCivConfig.defaultCivilizationName()));
            setDirty();
        }
    }

    public String suggestCivilizationId(String displayNameRaw) {
        String base = slugifyCivName(displayNameRaw == null ? "" : displayNameRaw);
        if (base.isEmpty()) {
            base = "civ";
        }
        if (base.length() > 32) {
            base = base.substring(0, 32);
        }
        while (base.endsWith("-")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            base = "civ";
        }

        String candidate = base;
        int suffix = 2;
        while (civilizations.containsKey(candidate)) {
            String clipped = truncatedForSuffix(base, suffix);
            candidate = clipped + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    public boolean createCivilization(String idRaw, String displayName, String actorName) {
        String id = normalizeCivId(idRaw);
        if (id == null || civilizations.containsKey(id)) {
            return false;
        }
        String name = sanitizeDisplayName(displayName, id);
        if (civilizationDisplayNameTaken(name, null)) {
            return false;
        }
        civilizations.put(id, new CivilizationRecord(id, name));
        addAuditLog(id, actorName + " created civilization '" + name + "'", RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean renameCivilization(String idRaw, String displayName, String actorName) {
        CivilizationRecord civ = getCivilization(idRaw);
        if (civ == null) {
            return false;
        }
        String name = sanitizeDisplayName(displayName, civ.id());
        if (civilizationDisplayNameTaken(name, civ.id())) {
            return false;
        }
        civ.setDisplayName(name);
        addAuditLog(civ.id(), actorName + " renamed civilization to '" + name + "'", RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    @Nullable
    public String findCivilizationIdByDisplayName(String displayNameRaw) {
        if (displayNameRaw == null) {
            return null;
        }
        String name = displayNameRaw.trim();
        if (name.isEmpty()) {
            return null;
        }
        for (CivilizationRecord civ : civilizations.values()) {
            if (civ.displayName().equalsIgnoreCase(name)) {
                return civ.id();
            }
        }
        return null;
    }

    @Nullable
    public DeleteCivilizationResult deleteCivilization(String civIdRaw, String fallbackCivRaw, String actorName) {
        String civId = normalizeCivId(civIdRaw);
        String fallbackId = normalizeCivId(fallbackCivRaw);
        if (civId == null || fallbackId == null || civId.equals(fallbackId)) {
            return null;
        }

        CivilizationRecord removed = civilizations.remove(civId);
        if (removed == null) {
            return null;
        }

        CivilizationRecord fallback = getOrCreateCivilization(fallbackId);
        int reassignedMembers = 0;
        for (Map.Entry<UUID, String> entry : playerCivilization.entrySet()) {
            if (civId.equals(normalizeCivId(entry.getValue()))) {
                entry.setValue(fallback.id());
                reassignedMembers++;
            }
        }

        int migratedAccounts = 0;
        for (PlayerRecord record : players.values()) {
            if (record.migrateCivAccount(civId, fallback.id())) {
                migratedAccounts++;
            }
        }

        int transferredStockEntries = 0;
        long transferredStockItems = 0L;
        for (Map.Entry<String, Long> entry : removed.hubStock().entrySet()) {
            long value = Math.max(0L, entry.getValue());
            if (value <= 0L) {
                continue;
            }
            fallback.hubStock().merge(entry.getKey(), value, Long::sum);
            transferredStockEntries++;
            transferredStockItems += value;
        }

        int removedPlots = removed.plots().size();
        addAuditLog(
                fallback.id(),
                actorName + " deleted civilization '" + removed.displayName() + "' [" + removed.id() + "]"
                        + " and reassigned " + reassignedMembers + " member(s) to " + fallback.id(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return new DeleteCivilizationResult(
                removed.id(),
                removed.displayName(),
                reassignedMembers,
                migratedAccounts,
                transferredStockEntries,
                transferredStockItems,
                removedPlots);
    }

    public boolean isFounderApproved(UUID playerId) {
        return founderApprovals.contains(playerId);
    }

    public void setFounderApproved(UUID playerId, boolean approved, String actorName) {
        if (approved) {
            founderApprovals.add(playerId);
        } else {
            founderApprovals.remove(playerId);
        }
        addAuditLog(
                RealCivConfig.defaultCivilizationId(),
                actorName + (approved ? " approved " : " revoked approval for ") + playerId + " as founder",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
    }

    public boolean consumeFounderApproval(UUID playerId, String actorName) {
        if (!founderApprovals.remove(playerId)) {
            return false;
        }
        addAuditLog(
                RealCivConfig.defaultCivilizationId(),
                actorName + " consumed founder approval for " + playerId,
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public List<UUID> founderApprovalsSorted() {
        return founderApprovals.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
    }

    public List<String> civilizationIdsSorted() {
        return civilizations.keySet().stream().sorted().toList();
    }

    @Nullable
    public CivilizationRecord getCivilization(String idRaw) {
        String id = normalizeCivId(idRaw);
        if (id == null) {
            return null;
        }
        return civilizations.get(id);
    }

    public CivilizationRecord getOrCreateCivilization(String idRaw) {
        String id = normalizeCivId(idRaw);
        if (id == null) {
            id = RealCivConfig.defaultCivilizationId();
        }
        CivilizationRecord existing = civilizations.get(id);
        if (existing != null) {
            return existing;
        }
        CivilizationRecord created = new CivilizationRecord(id, id);
        civilizations.put(id, created);
        setDirty();
        return created;
    }

    public String getOrAssignCivilization(UUID playerId) {
        String current = normalizeCivId(playerCivilization.get(playerId));
        if (current != null && civilizations.containsKey(current)) {
            return current;
        }
        ensureDefaultCivilizationExists();
        String fallback = RealCivConfig.defaultCivilizationId();
        playerCivilization.put(playerId, fallback);
        setDirty();
        return fallback;
    }

    public boolean setPlayerCivilization(UUID playerId, String civIdRaw, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        playerCivilization.put(playerId, civ.id());
        addAuditLog(civ.id(), actorName + " assigned " + playerId + " to civilization " + civ.id(), RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public PlayerRecord getOrCreatePlayer(UUID playerId) {
        return players.computeIfAbsent(playerId, ignored -> new PlayerRecord());
    }

    public long getHubStock(String civIdRaw, ResourceLocation itemId) {
        return getOrCreateCivilization(civIdRaw).hubStock().getOrDefault(itemId.toString(), 0L);
    }

    public List<Map.Entry<String, Long>> getHubStockEntriesSorted(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).hubStock().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0L)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .toList();
    }

    public boolean tryWithdrawFromHub(String civIdRaw, ResourceLocation itemId, long amount) {
        if (amount <= 0L) {
            return false;
        }
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        String key = itemId.toString();
        long current = civ.hubStock().getOrDefault(key, 0L);
        if (current < amount) {
            return false;
        }
        long remaining = current - amount;
        if (remaining <= 0L) {
            civ.hubStock().remove(key);
        } else {
            civ.hubStock().put(key, remaining);
        }
        setDirty();
        return true;
    }

    public void applyDeposit(
            String civIdRaw,
            UUID playerId,
            ResourceLocation itemId,
            int itemCount,
            RewardRule rewardRule,
            String actorName) {
        if (itemCount <= 0) {
            return;
        }

        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        PlayerRecord record = getOrCreatePlayer(playerId);
        long count = itemCount;

        civ.hubStock().merge(itemId.toString(), count, Long::sum);
        record.contributions(civ.id()).merge(itemId.toString(), count, Long::sum);
        record.addSocialCreditCents(civ.id(), rewardRule.creditsPerItemCents() * count);

        int professionGain = rewardRule.professionXpPerItem() * itemCount;
        switch (rewardRule.profession()) {
            case FARMER -> {
                record.farmerXp += professionGain;
                record.farmerActions = 0;
            }
            case MINER -> {
                record.minerXp += professionGain;
                record.minerActions = 0;
            }
            case LUMBERJACK -> {
                record.lumberjackXp += professionGain;
                record.lumberjackActions = 0;
            }
            case HUNTER -> {
                record.hunterXp += professionGain;
                record.hunterActions = 0;
            }
            case CRAFTER -> {
                record.crafterXp += professionGain;
                record.crafterActions = 0;
            }
            case NONE -> {
            }
        }

        record.generalXp += rewardRule.generalXpPerItem() * itemCount;
        addAuditLog(civ.id(), actorName + " deposited " + itemCount + "x " + itemId, RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
    }

    public boolean isMayor(String civIdRaw, UUID playerId) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return civ.mayorId() != null && civ.mayorId().equals(playerId);
    }

    @Nullable
    public UUID getMayorId(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).mayorId();
    }

    public void setMayor(String civIdRaw, @Nullable UUID newMayorId, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.setMayorId(newMayorId);
        if (newMayorId == null) {
            addAuditLog(civ.id(), actorName + " cleared mayor assignment", RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            addAuditLog(civ.id(), actorName + " set mayor to " + newMayorId, RealCivConfig.MAX_AUDIT_LOGS.get());
        }
        setDirty();
    }

    public boolean isCivicManager(String civIdRaw, UUID playerId) {
        return getOrCreateCivilization(civIdRaw).civicManagers().contains(playerId);
    }

    public List<UUID> civilizationMembersSorted(String civIdRaw) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return List.of();
        }
        String civId = civ.id();
        return playerCivilization.entrySet().stream()
                .filter(entry -> civId.equals(normalizeCivId(entry.getValue())))
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
    }

    public long civTreasuryCents(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).treasuryCents();
    }

    public void addCivTreasuryCents(String civIdRaw, long delta) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.setTreasuryCents(civ.treasuryCents() + delta);
        setDirty();
    }

    public void setCivicManager(String civIdRaw, UUID playerId, boolean allowed, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        if (allowed) {
            civ.civicManagers().add(playerId);
            addAuditLog(civ.id(), actorName + " added civic manager " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            civ.civicManagers().remove(playerId);
            addAuditLog(civ.id(), actorName + " removed civic manager " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
        }
        setDirty();
    }

    public void addAuditLog(String civIdRaw, String message, int maxLogs) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.auditLogs().add(Instant.now() + " | " + message);
        while (civ.auditLogs().size() > Math.max(1, maxLogs)) {
            civ.auditLogs().remove(0);
        }
        setDirty();
    }

    public List<String> getRecentAuditLogs(String civIdRaw, int count) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        int safeCount = Math.max(0, count);
        if (safeCount == 0 || civ.auditLogs().isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, civ.auditLogs().size() - safeCount);
        return new ArrayList<>(civ.auditLogs().subList(start, civ.auditLogs().size()));
    }

    public String chunkKey(String dimension, long chunkX, long chunkZ) {
        return dimension + "|" + chunkX + "|" + chunkZ;
    }

    @Nullable
    public PlotRecord getPlot(String civIdRaw, String dimension, long chunkX, long chunkZ) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return civ.plots().get(chunkKey(dimension, chunkX, chunkZ));
    }

    public void setPlot(
            String civIdRaw,
            String dimension,
            long chunkX,
            long chunkZ,
            LandClass landClass,
            @Nullable UUID ownerId,
            long gameTime,
            long initialPaidTicks) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        PlotRecord plot = new PlotRecord(dimension, chunkX, chunkZ, landClass, ownerId);
        long interval = Math.max(1L, RealCivConfig.upkeepIntervalTicks());
        plot.setNextUpkeepTick(gameTime + Math.max(interval, Math.max(0L, initialPaidTicks)));
        plot.setDelinquentSinceTick(-1L);
        civ.plots().put(plot.plotKey(), plot);
        setDirty();
    }

    public boolean clearPlot(String civIdRaw, String dimension, long chunkX, long chunkZ) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        PlotRecord removed = civ.plots().remove(chunkKey(dimension, chunkX, chunkZ));
        if (removed != null) {
            setDirty();
            return true;
        }
        return false;
    }

    @Nullable
    public PlotLookup getPlotAnyCivilization(String dimension, long chunkX, long chunkZ) {
        String key = chunkKey(dimension, chunkX, chunkZ);
        for (CivilizationRecord civ : civilizations.values()) {
            PlotRecord plot = civ.plots().get(key);
            if (plot != null) {
                return new PlotLookup(civ.id(), plot);
            }
        }
        return null;
    }

    public boolean canBuildOnPlot(String civIdRaw, PlotRecord plot, UUID playerId, boolean bypass) {
        if (bypass) {
            return true;
        }
        return switch (plot.landClass()) {
            case PUBLIC -> false;
            case CIVIC -> isMayor(civIdRaw, playerId) || isCivicManager(civIdRaw, playerId);
            case PRIVATE -> plot.ownerId() != null && plot.ownerId().equals(playerId);
        };
    }

    public boolean canBreakOnPlot(String civIdRaw, PlotRecord plot, UUID playerId, boolean bypass) {
        if (bypass) {
            return true;
        }
        return switch (plot.landClass()) {
            case PUBLIC -> true;
            case CIVIC -> isMayor(civIdRaw, playerId) || isCivicManager(civIdRaw, playerId);
            case PRIVATE -> plot.ownerId() != null && plot.ownerId().equals(playerId);
        };
    }

    public int privatePlotCountForOwner(String civIdRaw, UUID ownerId) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        int count = 0;
        for (PlotRecord plot : civ.plots().values()) {
            if (plot.landClass() == LandClass.PRIVATE && ownerId.equals(plot.ownerId())) {
                count++;
            }
        }
        return count;
    }

    public int delinquentPrivatePlotCountForOwner(String civIdRaw, UUID ownerId) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        int count = 0;
        for (PlotRecord plot : civ.plots().values()) {
            if (plot.landClass() == LandClass.PRIVATE && ownerId.equals(plot.ownerId()) && plot.delinquentSinceTick() >= 0L) {
                count++;
            }
        }
        return count;
    }

    public long earliestPrivatePlotUpkeepTick(String civIdRaw, UUID ownerId) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        long earliest = Long.MAX_VALUE;
        for (PlotRecord plot : civ.plots().values()) {
            if (plot.landClass() == LandClass.PRIVATE && ownerId.equals(plot.ownerId())) {
                earliest = Math.min(earliest, plot.nextUpkeepTick());
            }
        }
        return earliest == Long.MAX_VALUE ? -1L : earliest;
    }

    public int prepayPrivatePlotUpkeep(String civIdRaw, UUID ownerId, int cycles, long gameTime, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        int safeCycles = Math.max(0, cycles);
        if (safeCycles == 0) {
            return 0;
        }

        long interval = Math.max(1L, RealCivConfig.upkeepIntervalTicks());
        long extension = interval * safeCycles;
        int updated = 0;

        for (PlotRecord plot : civ.plots().values()) {
            if (plot.landClass() != LandClass.PRIVATE || !ownerId.equals(plot.ownerId())) {
                continue;
            }
            long base = Math.max(plot.nextUpkeepTick(), gameTime);
            plot.setNextUpkeepTick(base + extension);
            plot.setDelinquentSinceTick(-1L);
            updated++;
        }

        if (updated > 0) {
            addAuditLog(
                    civ.id(),
                    actorName + " prepaid upkeep for " + updated + " private plot(s) owned by " + ownerId
                            + " for " + safeCycles + " cycle(s)",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            setDirty();
        }
        return updated;
    }

    public void processUpkeep(long gameTime) {
        long interval = Math.max(1L, RealCivConfig.upkeepIntervalTicks());
        long grace = Math.max(1L, RealCivConfig.upkeepGraceTicks());
        long cost = Math.max(0L, RealCivConfig.upkeepCostCents());
        boolean changed = false;

        for (CivilizationRecord civ : civilizations.values()) {
            Iterator<Map.Entry<String, PlotRecord>> it = civ.plots().entrySet().iterator();
            while (it.hasNext()) {
                PlotRecord plot = it.next().getValue();
                if (plot.landClass() != LandClass.PRIVATE || plot.ownerId() == null) {
                    continue;
                }
                if (cost <= 0L) {
                    plot.setNextUpkeepTick(Math.max(plot.nextUpkeepTick(), gameTime + interval));
                    plot.setDelinquentSinceTick(-1L);
                    changed = true;
                    continue;
                }

                PlayerRecord owner = getOrCreatePlayer(plot.ownerId());
                int safety = 0;
                while (gameTime >= plot.nextUpkeepTick() && safety < 4096) {
                    if (owner.socialCreditCents(civ.id()) >= cost) {
                        owner.addSocialCreditCents(civ.id(), -cost);
                        civ.setTreasuryCents(civ.treasuryCents() + cost);
                        plot.setNextUpkeepTick(plot.nextUpkeepTick() + interval);
                        plot.setDelinquentSinceTick(-1L);
                        changed = true;
                    } else {
                        if (plot.delinquentSinceTick() < 0L) {
                            plot.setDelinquentSinceTick(plot.nextUpkeepTick());
                            addAuditLog(civ.id(), "Plot " + plot.plotKey() + " became delinquent for owner " + plot.ownerId(), RealCivConfig.MAX_AUDIT_LOGS.get());
                            changed = true;
                        }
                        break;
                    }
                    safety++;
                }

                if (plot.delinquentSinceTick() >= 0L && gameTime - plot.delinquentSinceTick() >= grace) {
                    UUID previousOwner = plot.ownerId();
                    plot.setLandClass(LandClass.PUBLIC);
                    plot.setOwnerId(null);
                    plot.setDelinquentSinceTick(-1L);
                    plot.setNextUpkeepTick(gameTime + interval);
                    addAuditLog(civ.id(), "Plot " + plot.plotKey() + " repossessed after grace. Previous owner " + previousOwner, RealCivConfig.MAX_AUDIT_LOGS.get());
                    changed = true;
                }
            }
        }
        if (changed) {
            setDirty();
        }
    }

    // Compatibility wrappers (default civ / legacy command paths)
    public long getHubStock(ResourceLocation itemId) {
        return getHubStock(RealCivConfig.defaultCivilizationId(), itemId);
    }

    public List<Map.Entry<String, Long>> getHubStockEntriesSorted() {
        return getHubStockEntriesSorted(RealCivConfig.defaultCivilizationId());
    }

    public boolean tryWithdrawFromHub(ResourceLocation itemId, long amount) {
        return tryWithdrawFromHub(RealCivConfig.defaultCivilizationId(), itemId, amount);
    }

    public void applyDeposit(UUID playerId, ResourceLocation itemId, int itemCount, RewardRule rewardRule, String actorName) {
        applyDeposit(RealCivConfig.defaultCivilizationId(), playerId, itemId, itemCount, rewardRule, actorName);
    }

    public boolean isMayor(UUID playerId) {
        return isMayor(RealCivConfig.defaultCivilizationId(), playerId);
    }

    @Nullable
    public UUID getMayorId() {
        return getMayorId(RealCivConfig.defaultCivilizationId());
    }

    public void setMayor(@Nullable UUID newMayorId, String actorName) {
        setMayor(RealCivConfig.defaultCivilizationId(), newMayorId, actorName);
    }

    public void addAuditLog(String message, int maxLogs) {
        addAuditLog(RealCivConfig.defaultCivilizationId(), message, maxLogs);
    }

    public List<String> getRecentAuditLogs(int count) {
        return getRecentAuditLogs(RealCivConfig.defaultCivilizationId(), count);
    }

    @Nullable
    public PlotRecord getActivePlot(String dimension, long chunkX, long chunkZ, long gameTime) {
        PlotLookup lookup = getPlotAnyCivilization(dimension, chunkX, chunkZ);
        return lookup == null ? null : lookup.plot();
    }

    public void setPlot(String dimension, long chunkX, long chunkZ, UUID renterId, long expiresAtTick) {
        long now = 0L;
        long initialPaidTicks = Math.max(0L, expiresAtTick - now);
        setPlot(RealCivConfig.defaultCivilizationId(), dimension, chunkX, chunkZ, LandClass.PRIVATE, renterId, now, initialPaidTicks);
    }

    public boolean clearPlot(String dimension, long chunkX, long chunkZ) {
        return clearPlot(RealCivConfig.defaultCivilizationId(), dimension, chunkX, chunkZ);
    }

    public record PlotLookup(String civilizationId, PlotRecord plot) {
    }

    public record DeleteCivilizationResult(
            String deletedId,
            String deletedDisplayName,
            int reassignedMembers,
            int migratedAccounts,
            int transferredStockEntries,
            long transferredStockItems,
            int removedPlots) {
    }

    public static final class CivilizationRecord {
        private final String id;
        private String displayName;
        private long treasuryCents;
        private final Map<String, Long> hubStock = new HashMap<>();
        private final Map<String, PlotRecord> plots = new HashMap<>();
        private final List<String> auditLogs = new ArrayList<>();
        private final Set<UUID> civicManagers = new HashSet<>();
        @Nullable
        private UUID mayorId;

        public CivilizationRecord(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public long treasuryCents() {
            return treasuryCents;
        }

        public void setTreasuryCents(long value) {
            treasuryCents = Math.max(0L, value);
        }

        public Map<String, Long> hubStock() {
            return hubStock;
        }

        public Map<String, PlotRecord> plots() {
            return plots;
        }

        public List<String> auditLogs() {
            return auditLogs;
        }

        public Set<UUID> civicManagers() {
            return civicManagers;
        }

        @Nullable
        public UUID mayorId() {
            return mayorId;
        }

        public void setMayorId(@Nullable UUID mayorId) {
            this.mayorId = mayorId;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("displayName", displayName);
            tag.putLong("treasuryCents", Math.max(0L, treasuryCents));

            CompoundTag stockTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : hubStock.entrySet()) {
                stockTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
            }
            tag.put("hubStock", stockTag);

            ListTag plotTags = new ListTag();
            for (PlotRecord plot : plots.values()) {
                plotTags.add(plot.save());
            }
            tag.put("plots", plotTags);

            ListTag logs = new ListTag();
            for (String entry : auditLogs) {
                logs.add(StringTag.valueOf(entry));
            }
            tag.put("auditLogs", logs);

            ListTag managers = new ListTag();
            for (UUID manager : civicManagers) {
                managers.add(StringTag.valueOf(manager.toString()));
            }
            tag.put("civicManagers", managers);

            if (mayorId != null) {
                tag.putString("mayor", mayorId.toString());
            }
            return tag;
        }

        @Nullable
        public static CivilizationRecord load(CompoundTag tag) {
            if (!tag.contains("id")) {
                return null;
            }
            String id = normalizeCivId(tag.getString("id"));
            if (id == null) {
                return null;
            }
            String name = tag.contains("displayName") ? tag.getString("displayName") : id;
            CivilizationRecord record = new CivilizationRecord(id, name);
            record.treasuryCents = Math.max(0L, tag.getLong("treasuryCents"));

            CompoundTag stockTag = tag.getCompound("hubStock");
            for (String key : stockTag.getAllKeys()) {
                record.hubStock.put(key, Math.max(0L, stockTag.getLong(key)));
            }

            ListTag plotTags = tag.getList("plots", Tag.TAG_COMPOUND);
            for (Tag entry : plotTags) {
                if (!(entry instanceof CompoundTag plotTag)) {
                    continue;
                }
                PlotRecord plot = PlotRecord.load(plotTag);
                if (plot != null) {
                    record.plots.put(plot.plotKey(), plot);
                }
            }

            ListTag logs = tag.getList("auditLogs", Tag.TAG_STRING);
            for (Tag entry : logs) {
                record.auditLogs.add(entry.getAsString());
            }

            ListTag managers = tag.getList("civicManagers", Tag.TAG_STRING);
            for (Tag entry : managers) {
                try {
                    record.civicManagers.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }

            if (tag.contains("mayor")) {
                try {
                    record.mayorId = UUID.fromString(tag.getString("mayor"));
                } catch (Exception ignored) {
                }
            }
            return record;
        }
    }

    public static final class PlotRecord {
        private final String dimension;
        private final long chunkX;
        private final long chunkZ;
        private LandClass landClass;
        @Nullable
        private UUID ownerId;
        private long nextUpkeepTick;
        private long delinquentSinceTick = -1L;

        public PlotRecord(String dimension, long chunkX, long chunkZ, LandClass landClass, @Nullable UUID ownerId) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.landClass = landClass;
            this.ownerId = ownerId;
        }

        public String plotKey() {
            return dimension + "|" + chunkX + "|" + chunkZ;
        }

        public String dimension() {
            return dimension;
        }

        public long chunkX() {
            return chunkX;
        }

        public long chunkZ() {
            return chunkZ;
        }

        public LandClass landClass() {
            return landClass;
        }

        public void setLandClass(LandClass value) {
            this.landClass = value;
        }

        @Nullable
        public UUID ownerId() {
            return ownerId;
        }

        public void setOwnerId(@Nullable UUID value) {
            this.ownerId = value;
        }

        public long nextUpkeepTick() {
            return nextUpkeepTick;
        }

        public void setNextUpkeepTick(long value) {
            this.nextUpkeepTick = value;
        }

        public long delinquentSinceTick() {
            return delinquentSinceTick;
        }

        public void setDelinquentSinceTick(long value) {
            this.delinquentSinceTick = value;
        }

        // Compatibility with old command formatting.
        @Nullable
        public UUID renterId() {
            return ownerId;
        }

        public long expiresAtTick() {
            return nextUpkeepTick;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", dimension);
            tag.putLong("chunkX", chunkX);
            tag.putLong("chunkZ", chunkZ);
            tag.putString("landClass", landClass.name());
            if (ownerId != null) {
                tag.putString("ownerId", ownerId.toString());
            }
            tag.putLong("nextUpkeepTick", nextUpkeepTick);
            tag.putLong("delinquentSinceTick", delinquentSinceTick);
            return tag;
        }

        @Nullable
        public static PlotRecord load(CompoundTag tag) {
            if (!tag.contains("dimension")) {
                return null;
            }
            try {
                String dimension = tag.getString("dimension");
                long chunkX = tag.getLong("chunkX");
                long chunkZ = tag.getLong("chunkZ");

                LandClass landClass = LandClass.PUBLIC;
                if (tag.contains("landClass")) {
                    LandClass parsed = LandClass.fromConfig(tag.getString("landClass"));
                    if (parsed != null) {
                        landClass = parsed;
                    }
                }

                UUID owner = null;
                if (tag.contains("ownerId")) {
                    owner = UUID.fromString(tag.getString("ownerId"));
                } else if (tag.contains("renterId")) {
                    owner = UUID.fromString(tag.getString("renterId"));
                    landClass = LandClass.PRIVATE;
                }

                PlotRecord record = new PlotRecord(dimension, chunkX, chunkZ, landClass, owner);
                if (tag.contains("nextUpkeepTick")) {
                    record.nextUpkeepTick = tag.getLong("nextUpkeepTick");
                } else if (tag.contains("expiresAtTick")) {
                    record.nextUpkeepTick = tag.getLong("expiresAtTick");
                }
                record.delinquentSinceTick = tag.contains("delinquentSinceTick") ? tag.getLong("delinquentSinceTick") : -1L;
                return record;
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public static final class PlayerRecord {
        private long socialCreditCents;
        private int farmerActions;
        private int minerActions;
        private int farmerXp;
        private int minerXp;
        private int lumberjackActions;
        private int lumberjackXp;
        private int hunterActions;
        private int hunterXp;
        private int crafterActions;
        private int crafterXp;
        private int generalXp;
        private final Map<String, CivAccount> civAccounts = new HashMap<>();

        // Legacy fields retained for migration.
        private final Map<String, Long> contributions = new HashMap<>();
        private final Map<String, Long> personalWithdrawals = new HashMap<>();
        @Nullable
        private Double personalWithdrawRatioOverride;

        private CivAccount account(String civIdRaw) {
            String civId = normalizeCivId(civIdRaw);
            if (civId == null) {
                civId = RealCivConfig.defaultCivilizationId();
            }
            return civAccounts.computeIfAbsent(civId, ignored -> new CivAccount());
        }

        public boolean migrateCivAccount(String fromCivRaw, String toCivRaw) {
            String from = normalizeCivId(fromCivRaw);
            String to = normalizeCivId(toCivRaw);
            if (from == null || to == null || from.equals(to)) {
                return false;
            }
            CivAccount fromAccount = civAccounts.remove(from);
            if (fromAccount == null) {
                return false;
            }

            CivAccount target = account(to);
            target.socialCreditCents = Math.max(0L, target.socialCreditCents + Math.max(0L, fromAccount.socialCreditCents));
            for (Map.Entry<String, Long> entry : fromAccount.contributions.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value > 0L) {
                    target.contributions.merge(entry.getKey(), value, Long::sum);
                }
            }
            for (Map.Entry<String, Long> entry : fromAccount.personalWithdrawals.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value > 0L) {
                    target.personalWithdrawals.merge(entry.getKey(), value, Long::sum);
                }
            }
            if (target.personalWithdrawRatioOverride == null && fromAccount.personalWithdrawRatioOverride != null) {
                target.personalWithdrawRatioOverride = clampRatio(fromAccount.personalWithdrawRatioOverride);
            }
            return true;
        }

        public void migrateLegacyAccount(String civId) {
            CivAccount account = account(civId);
            if (account.socialCreditCents <= 0L && socialCreditCents > 0L) {
                account.socialCreditCents = socialCreditCents;
            }
            if (account.contributions.isEmpty() && !contributions.isEmpty()) {
                account.contributions.putAll(contributions);
            }
            if (account.personalWithdrawals.isEmpty() && !personalWithdrawals.isEmpty()) {
                account.personalWithdrawals.putAll(personalWithdrawals);
            }
            if (account.personalWithdrawRatioOverride == null && personalWithdrawRatioOverride != null) {
                account.personalWithdrawRatioOverride = clampRatio(personalWithdrawRatioOverride);
            }
        }

        public long socialCreditCents(String civId) {
            return account(civId).socialCreditCents;
        }

        public void setSocialCreditCents(String civId, long cents) {
            account(civId).socialCreditCents = Math.max(0L, cents);
        }

        public void addSocialCreditCents(String civId, long delta) {
            setSocialCreditCents(civId, socialCreditCents(civId) + delta);
        }

        public int farmerActions() {
            return farmerActions;
        }

        public void setFarmerActions(int value) {
            this.farmerActions = Math.max(0, value);
        }

        public int minerActions() {
            return minerActions;
        }

        public void setMinerActions(int value) {
            this.minerActions = Math.max(0, value);
        }

        public int farmerXp() {
            return farmerXp;
        }

        public int minerXp() {
            return minerXp;
        }

        public int lumberjackActions() {
            return lumberjackActions;
        }

        public void setLumberjackActions(int value) {
            this.lumberjackActions = Math.max(0, value);
        }

        public int lumberjackXp() {
            return lumberjackXp;
        }

        public int hunterActions() {
            return hunterActions;
        }

        public void setHunterActions(int value) {
            this.hunterActions = Math.max(0, value);
        }

        public int hunterXp() {
            return hunterXp;
        }

        public int crafterActions() {
            return crafterActions;
        }

        public void setCrafterActions(int value) {
            this.crafterActions = Math.max(0, value);
        }

        public int crafterXp() {
            return crafterXp;
        }

        public int generalXp() {
            return generalXp;
        }

        public Map<String, Long> contributions(String civId) {
            return account(civId).contributions;
        }

        public long contributedCount(String civId, ResourceLocation itemId) {
            return account(civId).contributions.getOrDefault(itemId.toString(), 0L);
        }

        public long personalWithdrawnCount(String civId, ResourceLocation itemId) {
            return account(civId).personalWithdrawals.getOrDefault(itemId.toString(), 0L);
        }

        public double effectivePersonalWithdrawRatio(String civId) {
            CivAccount account = account(civId);
            if (account.personalWithdrawRatioOverride == null) {
                return RealCivConfig.defaultPersonalWithdrawRatio();
            }
            return clampRatio(account.personalWithdrawRatioOverride);
        }

        @Nullable
        public Double personalWithdrawRatioOverride(String civId) {
            return account(civId).personalWithdrawRatioOverride;
        }

        public void setPersonalWithdrawRatioOverride(String civId, @Nullable Double ratio) {
            CivAccount account = account(civId);
            if (ratio == null) {
                account.personalWithdrawRatioOverride = null;
                return;
            }
            account.personalWithdrawRatioOverride = clampRatio(ratio);
        }

        public long personalWithdrawLimit(String civId, ResourceLocation itemId) {
            long contributed = contributedCount(civId, itemId);
            return (long) Math.floor(contributed * effectivePersonalWithdrawRatio(civId));
        }

        public long remainingPersonalWithdraw(String civId, ResourceLocation itemId) {
            long remaining = personalWithdrawLimit(civId, itemId) - personalWithdrawnCount(civId, itemId);
            return Math.max(0L, remaining);
        }

        public void recordPersonalWithdrawal(String civId, ResourceLocation itemId, long count) {
            if (count <= 0L) {
                return;
            }
            account(civId).personalWithdrawals.merge(itemId.toString(), count, Long::sum);
        }

        // Compatibility wrappers (default civ).
        public long socialCreditCents() {
            return socialCreditCents(RealCivConfig.defaultCivilizationId());
        }

        public Map<String, Long> contributions() {
            return contributions(RealCivConfig.defaultCivilizationId());
        }

        public long personalWithdrawnCount(ResourceLocation itemId) {
            return personalWithdrawnCount(RealCivConfig.defaultCivilizationId(), itemId);
        }

        public double effectivePersonalWithdrawRatio() {
            return effectivePersonalWithdrawRatio(RealCivConfig.defaultCivilizationId());
        }

        @Nullable
        public Double personalWithdrawRatioOverride() {
            return personalWithdrawRatioOverride(RealCivConfig.defaultCivilizationId());
        }

        public void setPersonalWithdrawRatioOverride(@Nullable Double ratio) {
            setPersonalWithdrawRatioOverride(RealCivConfig.defaultCivilizationId(), ratio);
        }

        public long personalWithdrawLimit(ResourceLocation itemId) {
            return personalWithdrawLimit(RealCivConfig.defaultCivilizationId(), itemId);
        }

        public long remainingPersonalWithdraw(ResourceLocation itemId) {
            return remainingPersonalWithdraw(RealCivConfig.defaultCivilizationId(), itemId);
        }

        public void recordPersonalWithdrawal(ResourceLocation itemId, long count) {
            recordPersonalWithdrawal(RealCivConfig.defaultCivilizationId(), itemId, count);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("socialCreditCents", socialCreditCents);
            tag.putInt("farmerActions", farmerActions);
            tag.putInt("minerActions", minerActions);
            tag.putInt("farmerXp", farmerXp);
            tag.putInt("minerXp", minerXp);
            tag.putInt("lumberjackActions", lumberjackActions);
            tag.putInt("lumberjackXp", lumberjackXp);
            tag.putInt("hunterActions", hunterActions);
            tag.putInt("hunterXp", hunterXp);
            tag.putInt("crafterActions", crafterActions);
            tag.putInt("crafterXp", crafterXp);
            tag.putInt("generalXp", generalXp);

            CompoundTag accountsTag = new CompoundTag();
            for (Map.Entry<String, CivAccount> entry : civAccounts.entrySet()) {
                accountsTag.put(entry.getKey(), entry.getValue().save());
            }
            tag.put("civAccounts", accountsTag);

            CompoundTag contributionTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : contributions.entrySet()) {
                contributionTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
            }
            tag.put("contributions", contributionTag);

            CompoundTag withdrawalTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : personalWithdrawals.entrySet()) {
                withdrawalTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
            }
            tag.put("personalWithdrawals", withdrawalTag);

            if (personalWithdrawRatioOverride != null) {
                tag.putDouble("personalWithdrawRatioOverride", clampRatio(personalWithdrawRatioOverride));
            }
            return tag;
        }

        public static PlayerRecord load(CompoundTag tag) {
            PlayerRecord record = new PlayerRecord();
            record.socialCreditCents = Math.max(0L, tag.getLong("socialCreditCents"));
            record.farmerActions = Math.max(0, tag.getInt("farmerActions"));
            record.minerActions = Math.max(0, tag.getInt("minerActions"));
            record.farmerXp = Math.max(0, tag.getInt("farmerXp"));
            record.minerXp = Math.max(0, tag.getInt("minerXp"));
            record.lumberjackActions = Math.max(0, tag.getInt("lumberjackActions"));
            record.lumberjackXp = Math.max(0, tag.getInt("lumberjackXp"));
            record.hunterActions = Math.max(0, tag.getInt("hunterActions"));
            record.hunterXp = Math.max(0, tag.getInt("hunterXp"));
            record.crafterActions = Math.max(0, tag.getInt("crafterActions"));
            record.crafterXp = Math.max(0, tag.getInt("crafterXp"));
            record.generalXp = Math.max(0, tag.getInt("generalXp"));

            CompoundTag accountsTag = tag.getCompound("civAccounts");
            for (String civId : accountsTag.getAllKeys()) {
                record.civAccounts.put(civId, CivAccount.load(accountsTag.getCompound(civId)));
            }

            CompoundTag contributionTag = tag.getCompound("contributions");
            for (String key : contributionTag.getAllKeys()) {
                record.contributions.put(key, Math.max(0L, contributionTag.getLong(key)));
            }

            CompoundTag withdrawalTag = tag.getCompound("personalWithdrawals");
            for (String key : withdrawalTag.getAllKeys()) {
                record.personalWithdrawals.put(key, Math.max(0L, withdrawalTag.getLong(key)));
            }

            if (tag.contains("personalWithdrawRatioOverride")) {
                record.personalWithdrawRatioOverride = clampRatio(tag.getDouble("personalWithdrawRatioOverride"));
            }
            return record;
        }

        public int levelFor(Profession profession) {
            return switch (profession) {
                case FARMER -> RealCivConfig.professionLevelFromXp(farmerXp());
                case MINER -> RealCivConfig.professionLevelFromXp(minerXp());
                case LUMBERJACK -> RealCivConfig.professionLevelFromXp(lumberjackXp());
                case HUNTER -> RealCivConfig.professionLevelFromXp(hunterXp());
                case CRAFTER -> RealCivConfig.professionLevelFromXp(crafterXp());
                case NONE -> 0;
            };
        }

        public int generalLevel() {
            return RealCivConfig.generalLevelFromXp(generalXp());
        }

        private static double clampRatio(double ratio) {
            return Math.max(0.0D, Math.min(1.0D, ratio));
        }
    }

    public static final class CivAccount {
        private long socialCreditCents;
        private final Map<String, Long> contributions = new HashMap<>();
        private final Map<String, Long> personalWithdrawals = new HashMap<>();
        @Nullable
        private Double personalWithdrawRatioOverride;

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("socialCreditCents", socialCreditCents);

            CompoundTag contributionTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : contributions.entrySet()) {
                contributionTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
            }
            tag.put("contributions", contributionTag);

            CompoundTag withdrawalTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : personalWithdrawals.entrySet()) {
                withdrawalTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
            }
            tag.put("personalWithdrawals", withdrawalTag);

            if (personalWithdrawRatioOverride != null) {
                tag.putDouble("personalWithdrawRatioOverride", Math.max(0.0D, Math.min(1.0D, personalWithdrawRatioOverride)));
            }
            return tag;
        }

        public static CivAccount load(CompoundTag tag) {
            CivAccount account = new CivAccount();
            account.socialCreditCents = Math.max(0L, tag.getLong("socialCreditCents"));

            CompoundTag contributionTag = tag.getCompound("contributions");
            for (String key : contributionTag.getAllKeys()) {
                account.contributions.put(key, Math.max(0L, contributionTag.getLong(key)));
            }

            CompoundTag withdrawalTag = tag.getCompound("personalWithdrawals");
            for (String key : withdrawalTag.getAllKeys()) {
                account.personalWithdrawals.put(key, Math.max(0L, withdrawalTag.getLong(key)));
            }

            if (tag.contains("personalWithdrawRatioOverride")) {
                account.personalWithdrawRatioOverride = Math.max(0.0D, Math.min(1.0D, tag.getDouble("personalWithdrawRatioOverride")));
            }
            return account;
        }
    }
}
