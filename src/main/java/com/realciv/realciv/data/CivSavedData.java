package com.realciv.realciv.data;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.integration.RealCivFTBChunksMirror;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RewardRule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
    private static final String DEFAULT_LEADER_TITLE = "Mayor";
    private static final int MAX_ROLE_ID_LENGTH = 48;
    private static final int MAX_ROLE_DISPLAY_NAME_LENGTH = 40;
    private static final int MAX_PERMISSION_KEY_LENGTH = 64;
    private static final int MAX_LEADER_TITLE_LENGTH = 32;

    // Shared permission keys that role assignments can grant to non-mayor civ staff.
    public static final String ROLE_PERMISSION_MANAGE_DIPLOMACY = "manage_diplomacy";
    public static final String ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE = "manage_friendly_fire";
    public static final String ROLE_PERMISSION_MANAGE_PROFESSION_FOCUS = "manage_profession_focus";
    public static final String ROLE_PERMISSION_MANAGE_EXPLOSIVES = "manage_explosives";
    public static final String ROLE_PERMISSION_MANAGE_REDSTONERS = "manage_redstoners";
    public static final String ROLE_PERMISSION_MANAGE_TOWN_CLAIMS = "manage_town_claims";
    public static final String ROLE_PERMISSION_MANAGE_LAND_ZONING = "manage_land_zoning";
    public static final String ROLE_PERMISSION_MANAGE_LAND_MANAGERS = "manage_land_managers";
    public static final String ROLE_PERMISSION_MANAGE_FTB_MODE = "manage_ftb_mode";
    public static final String ROLE_PERMISSION_MANAGE_CENSUS = "manage_census";
    public static final String ROLE_PERMISSION_POLICE_MEMBERS = "police_members";
    public static final String ROLE_PERMISSION_MANAGE_CENSUS_ROLES = "manage_census_roles";
    public static final String ROLE_PERMISSION_MANAGE_LEADERSHIP = "manage_leadership";
    public static final String ROLE_PERMISSION_MANAGE_WITHDRAW_RATES = "manage_withdraw_rates";
    public static final String ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION = "manage_hub_distribution";
    public static final String ROLE_PERMISSION_MANAGE_HUB_WITHDRAWALS = "manage_hub_withdrawals";
    public static final String ROLE_PERMISSION_VIEW_HUB_LOGS = "view_hub_logs";
    public static final String ROLE_PERMISSION_VIEW_HUB_QUOTAS = "view_hub_quotas";
    public static final String ROLE_PERMISSION_MANAGE_UPKEEP = "manage_upkeep";
    public static final String ROLE_PERMISSION_MANAGE_GOVERNANCE = "manage_governance";

    private static final List<String> KNOWN_ROLE_PERMISSIONS = List.of(
            ROLE_PERMISSION_MANAGE_DIPLOMACY,
            ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE,
            ROLE_PERMISSION_MANAGE_PROFESSION_FOCUS,
            ROLE_PERMISSION_MANAGE_EXPLOSIVES,
            ROLE_PERMISSION_MANAGE_REDSTONERS,
            ROLE_PERMISSION_MANAGE_TOWN_CLAIMS,
            ROLE_PERMISSION_MANAGE_LAND_ZONING,
            ROLE_PERMISSION_MANAGE_LAND_MANAGERS,
            ROLE_PERMISSION_MANAGE_FTB_MODE,
            ROLE_PERMISSION_MANAGE_CENSUS,
            ROLE_PERMISSION_POLICE_MEMBERS,
            ROLE_PERMISSION_MANAGE_CENSUS_ROLES,
            ROLE_PERMISSION_MANAGE_LEADERSHIP,
            ROLE_PERMISSION_MANAGE_WITHDRAW_RATES,
            ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION,
            ROLE_PERMISSION_MANAGE_HUB_WITHDRAWALS,
            ROLE_PERMISSION_VIEW_HUB_LOGS,
            ROLE_PERMISSION_VIEW_HUB_QUOTAS,
            ROLE_PERMISSION_MANAGE_UPKEEP,
            ROLE_PERMISSION_MANAGE_GOVERNANCE);

    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    private final Map<String, CivilizationRecord> civilizations = new HashMap<>();
    private final Map<UUID, String> playerCivilization = new HashMap<>();
    private final Map<String, DiplomacyState> diplomacy = new HashMap<>();
    private final Map<String, WarCasualtyRecord> warCasualties = new HashMap<>();
    private final Set<UUID> founderApprovals = new HashSet<>();
    @Nullable
    private transient MinecraftServer attachedServer;

    public static CivSavedData get(MinecraftServer server) {
        ServerLevel overworld = Objects.requireNonNull(server.overworld(), "Overworld is not available");
        SavedData.Factory<CivSavedData> factory = new SavedData.Factory<>(CivSavedData::new, CivSavedData::load);
        CivSavedData data = overworld.getDataStorage().computeIfAbsent(factory, DATA_NAME);
        data.attachedServer = server;
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

        ListTag diplomacyTags = tag.getList("diplomacy", Tag.TAG_COMPOUND);
        for (Tag entry : diplomacyTags) {
            if (!(entry instanceof CompoundTag diplomacyTag)) {
                continue;
            }
            String civA = normalizeCivId(diplomacyTag.getString("civA"));
            String civB = normalizeCivId(diplomacyTag.getString("civB"));
            DiplomacyState state = DiplomacyState.fromSerializedName(diplomacyTag.getString("state"));
            if (civA == null || civB == null || state == null) {
                continue;
            }
            data.setDiplomacyStateInternal(civA, civB, state);
        }

        ListTag warCasualtyTags = tag.getList("warCasualties", Tag.TAG_COMPOUND);
        for (Tag entry : warCasualtyTags) {
            if (!(entry instanceof CompoundTag casualtyTag)) {
                continue;
            }
            String civA = normalizeCivId(casualtyTag.getString("civA"));
            String civB = normalizeCivId(casualtyTag.getString("civB"));
            if (civA == null || civB == null || civA.equals(civB)) {
                continue;
            }
            String key = diplomacyKey(civA, civB);
            if (key == null) {
                continue;
            }
            WarCasualtyRecord casualtyRecord = new WarCasualtyRecord();
            casualtyRecord.firstCasualties = Math.max(0L, casualtyTag.getLong("casualtiesA"));
            casualtyRecord.secondCasualties = Math.max(0L, casualtyTag.getLong("casualtiesB"));
            if (casualtyRecord.firstCasualties <= 0L && casualtyRecord.secondCasualties <= 0L) {
                continue;
            }
            data.warCasualties.put(key, casualtyRecord);
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

        ListTag diplomacyTags = new ListTag();
        for (Map.Entry<String, DiplomacyState> entry : diplomacy.entrySet()) {
            DiplomacyPair pair = diplomacyPairFromKey(entry.getKey());
            if (pair == null) {
                continue;
            }
            CompoundTag relationTag = new CompoundTag();
            relationTag.putString("civA", pair.firstCivId());
            relationTag.putString("civB", pair.secondCivId());
            relationTag.putString("state", entry.getValue().serializedName());
            diplomacyTags.add(relationTag);
        }
        tag.put("diplomacy", diplomacyTags);

        ListTag casualtyTags = new ListTag();
        for (Map.Entry<String, WarCasualtyRecord> entry : warCasualties.entrySet()) {
            DiplomacyPair pair = diplomacyPairFromKey(entry.getKey());
            if (pair == null) {
                continue;
            }
            WarCasualtyRecord casualtyRecord = entry.getValue();
            if (casualtyRecord == null) {
                continue;
            }
            long first = Math.max(0L, casualtyRecord.firstCasualties);
            long second = Math.max(0L, casualtyRecord.secondCasualties);
            if (first <= 0L && second <= 0L) {
                continue;
            }
            CompoundTag casualtyTag = new CompoundTag();
            casualtyTag.putString("civA", pair.firstCivId());
            casualtyTag.putString("civB", pair.secondCivId());
            casualtyTag.putLong("casualtiesA", first);
            casualtyTag.putLong("casualtiesB", second);
            casualtyTags.add(casualtyTag);
        }
        tag.put("warCasualties", casualtyTags);

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

    @Nullable
    private static String normalizeRoleId(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String input = raw.trim().toLowerCase(Locale.ROOT);
        if (input.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        boolean previousSeparator = false;
        for (int i = 0; i < input.length() && out.length() < MAX_ROLE_ID_LENGTH; i++) {
            char ch = input.charAt(i);
            boolean alphaNum = (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
            boolean separator = ch == '_' || ch == '-' || ch == '.';
            if (alphaNum) {
                out.append(ch);
                previousSeparator = false;
                continue;
            }
            if (Character.isWhitespace(ch) || separator) {
                if (!previousSeparator && out.length() > 0) {
                    out.append('_');
                    previousSeparator = true;
                }
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '_') {
            out.deleteCharAt(out.length() - 1);
        }
        return out.isEmpty() ? null : out.toString();
    }

    private static String sanitizeRoleDisplayName(@Nullable String raw) {
        if (raw == null) {
            return "Role";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "Role";
        }
        if (trimmed.length() > MAX_ROLE_DISPLAY_NAME_LENGTH) {
            return trimmed.substring(0, MAX_ROLE_DISPLAY_NAME_LENGTH);
        }
        return trimmed;
    }

    @Nullable
    private static String normalizePermissionKey(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String input = raw.trim().toLowerCase(Locale.ROOT);
        if (input.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        boolean previousSeparator = false;
        for (int i = 0; i < input.length() && out.length() < MAX_PERMISSION_KEY_LENGTH; i++) {
            char ch = input.charAt(i);
            boolean alphaNum = (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
            boolean separator = ch == '_' || ch == '-' || ch == '.';
            if (alphaNum) {
                out.append(ch);
                previousSeparator = false;
                continue;
            }
            if (Character.isWhitespace(ch) || separator) {
                if (!previousSeparator && out.length() > 0) {
                    out.append('_');
                    previousSeparator = true;
                }
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '_') {
            out.deleteCharAt(out.length() - 1);
        }
        return out.isEmpty() ? null : out.toString();
    }

    private static String sanitizeLeaderTitle(@Nullable String raw) {
        if (raw == null) {
            return DEFAULT_LEADER_TITLE;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_LEADER_TITLE;
        }
        if (trimmed.length() > MAX_LEADER_TITLE_LENGTH) {
            return trimmed.substring(0, MAX_LEADER_TITLE_LENGTH);
        }
        return trimmed;
    }

    private static double clampUpkeepRateMultiplier(double multiplier) {
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
            return 1.0D;
        }
        return Math.max(0.25D, Math.min(5.0D, multiplier));
    }

    private static double clampUnitRatio(double ratio) {
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, ratio));
    }

    @Nullable
    private static String diplomacyKey(String civAraw, String civBraw) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB)) {
            return null;
        }
        return civA.compareTo(civB) < 0 ? civA + "|" + civB : civB + "|" + civA;
    }

    @Nullable
    private static DiplomacyPair diplomacyPairFromKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split("\\|", 2);
        if (parts.length != 2) {
            return null;
        }
        String first = normalizeCivId(parts[0]);
        String second = normalizeCivId(parts[1]);
        if (first == null || second == null || first.equals(second)) {
            return null;
        }
        return new DiplomacyPair(first, second);
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
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, id);
        }
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
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, civ.id());
        }
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
        removeDiplomacyLinksForCivilization(civId);
        removeWarCasualtiesForCivilization(civId);

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
        if (attachedServer != null) {
            for (PlotRecord removedPlot : removed.plots().values()) {
                RealCivFTBChunksMirror.clearClaimAt(
                        attachedServer,
                        removedPlot.dimension(),
                        removedPlot.chunkX(),
                        removedPlot.chunkZ());
            }
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, fallback.id());
        }
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
        @Nullable String previousCivId = normalizeCivId(playerCivilization.get(playerId));

        for (CivilizationRecord each : civilizations.values()) {
            each.joinRequests().remove(playerId);
            each.invitedPlayers().remove(playerId);
            each.civicManagers().remove(playerId);
            each.explosivesExperts().remove(playerId);
            each.redstoners().remove(playerId);
            removePlayerFromAllCustomRoles(each, playerId);
            if (each.mayorId() != null && each.mayorId().equals(playerId) && !each.id().equals(civ.id())) {
                each.setMayorId(null);
                addAuditLog(each.id(), actorName + " cleared mayor assignment for migrating player " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
            }
        }

        playerCivilization.put(playerId, civ.id());
        addAuditLog(civ.id(), actorName + " assigned " + playerId + " to civilization " + civ.id(), RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, civ.id());
            if (previousCivId != null && !previousCivId.equals(civ.id())) {
                RealCivFTBChunksMirror.syncCivilization(attachedServer, this, previousCivId);
            }
        }
        return true;
    }

    public PlayerRecord getOrCreatePlayer(UUID playerId) {
        return players.computeIfAbsent(playerId, ignored -> new PlayerRecord());
    }

    @Nullable
    public Profession playerFocusProfession(UUID playerId) {
        return getOrCreatePlayer(playerId).focusedProfession();
    }

    public boolean setPlayerFocusProfession(UUID playerId, @Nullable Profession profession, String actorName) {
        PlayerRecord record = getOrCreatePlayer(playerId);
        Profession normalized = profession == Profession.NONE ? null : profession;
        if (record.focusedProfession() == normalized) {
            return false;
        }
        record.setFocusedProfession(normalized);
        String civId = getOrAssignCivilization(playerId);
        if (normalized == null) {
            addAuditLog(civId, actorName + " cleared profession focus for " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            addAuditLog(civId, actorName + " set profession focus to " + normalized.name() + " for " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
        }
        setDirty();
        return true;
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

    public boolean addToHubStock(String civIdRaw, ResourceLocation itemId, long amount, String actorName) {
        if (amount <= 0L) {
            return false;
        }
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.hubStock().merge(itemId.toString(), amount, Long::sum);
        addAuditLog(
                civ.id(),
                actorName + " added " + amount + "x " + itemId + " to Community Hub stock",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public void applyDeposit(
            String civIdRaw,
            UUID playerId,
            ResourceLocation itemId,
            int itemCount,
            RewardRule rewardRule,
            int restoredActions,
            String actorName) {
        if (itemCount <= 0) {
            return;
        }

        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        PlayerRecord record = getOrCreatePlayer(playerId);
        long count = itemCount;

        civ.hubStock().merge(itemId.toString(), count, Long::sum);
        record.contributions(civ.id()).merge(itemId.toString(), count, Long::sum);
        long earnedCredits = rewardRule.creditsPerItemCents() * count;
        long appliedCredits = record.addSocialCreditCents(civ.id(), earnedCredits);
        long treasuryShare = Math.round(appliedCredits * RealCivConfig.civTreasuryDepositRatio());
        if (treasuryShare > 0L) {
            civ.setTreasuryCents(civ.treasuryCents() + treasuryShare);
        }

        int professionGain = rewardRule.professionXpPerItem() * itemCount;
        int safeRestoredActions = Math.max(0, restoredActions);
        switch (rewardRule.profession()) {
            case FARMER -> {
                record.setFarmerActions(record.farmerActions() - safeRestoredActions);
            }
            case MINER -> {
                record.setMinerActions(record.minerActions() - safeRestoredActions);
                record.clearMinerBlockWindow();
            }
            case TERRAFORMER -> {
                record.setTerraformerActions(record.terraformerActions() - safeRestoredActions);
                record.clearTerraformerBlockWindow();
            }
            case LUMBERJACK -> {
                record.setLumberjackActions(record.lumberjackActions() - safeRestoredActions);
                record.clearLumberjackBlockWindow();
            }
            case FISHER -> {
                record.setFisherActions(record.fisherActions() - safeRestoredActions);
            }
            case HUNTER -> {
                record.restoreHunterActions(safeRestoredActions);
            }
            case WARRIOR -> {
                record.setWarriorActions(record.warriorActions() - safeRestoredActions);
            }
            case EXPLOSIVES_EXPERT -> {
                record.setExplosivesExpertActions(record.explosivesExpertActions() - safeRestoredActions);
            }
            case CRAFTER -> {
                record.setCrafterActions(record.crafterActions() - safeRestoredActions);
            }
            case ENCHANTER -> {
                record.setEnchanterActions(record.enchanterActions() - safeRestoredActions);
            }
            case BREWER -> {
                record.setBrewerActions(record.brewerActions() - safeRestoredActions);
            }
            case TRADER -> {
                record.setTraderActions(record.traderActions() - safeRestoredActions);
            }
            case SHEPHERD -> {
                record.setShepherdActions(record.shepherdActions() - safeRestoredActions);
            }
            case EXPLORER -> {
                record.setExplorerActions(record.explorerActions() - safeRestoredActions);
            }
            case TREASURE_HUNTER -> {
                record.setTreasureHunterActions(record.treasureHunterActions() - safeRestoredActions);
            }
            case BREEDER -> {
                record.setBreederActions(record.breederActions() - safeRestoredActions);
            }
            case SMITHY -> {
                record.setSmithyActions(record.smithyActions() - safeRestoredActions);
            }
            case SMELTER -> {
                record.setSmelterActions(record.smelterActions() - safeRestoredActions);
            }
            case NONE -> {
            }
        }
        record.addProfessionXp(rewardRule.profession(), professionGain);

        record.addGeneralXp(rewardRule.generalXpPerItem() * itemCount);
        addAuditLog(
                civ.id(),
                actorName + " deposited " + itemCount + "x " + itemId
                        + " | restored " + safeRestoredActions + " " + rewardRule.profession().name().toLowerCase(java.util.Locale.ROOT) + " action(s)",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
    }

    public int countPlotsByClass(String civIdRaw, LandClass landClass) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        int count = 0;
        for (PlotRecord plot : civ.plots().values()) {
            if (plot.landClass() == landClass) {
                count++;
            }
        }
        return count;
    }

    public boolean isMayor(String civIdRaw, UUID playerId) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return civ.mayorId() != null && civ.mayorId().equals(playerId);
    }

    public boolean isCivilizationMember(String civIdRaw, UUID playerId) {
        String civId = normalizeCivId(civIdRaw);
        String memberCivId = normalizeCivId(playerCivilization.get(playerId));
        return civId != null && memberCivId != null && civId.equals(memberCivId);
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
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, civ.id());
        }
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

    public int plotCount(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).plots().size();
    }

    public void addCivTreasuryCents(String civIdRaw, long delta) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.setTreasuryCents(civ.treasuryCents() + delta);
        setDirty();
    }

    @Nullable
    public HubLocation getHubLocation(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        if (civ.hubDimension() == null) {
            return null;
        }
        return new HubLocation(civ.hubDimension(), civ.hubX(), civ.hubY(), civ.hubZ());
    }

    public void setHubLocation(String civIdRaw, String dimension, int x, int y, int z) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.setHubLocation(dimension, x, y, z);
        setDirty();
    }

    public boolean hasStarterTownAreaGranted(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).starterTownAreaGranted();
    }

    public void setStarterTownAreaGranted(String civIdRaw, boolean granted) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        if (civ.starterTownAreaGranted() == granted) {
            return;
        }
        civ.setStarterTownAreaGranted(granted);
        setDirty();
    }

    public boolean clearHubLocation(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        if (civ.hubDimension() == null) {
            return false;
        }
        civ.clearHubLocation();
        setDirty();
        return true;
    }

    @Nullable
    public String findCivilizationIdByHubPosition(String dimension, int x, int y, int z) {
        for (CivilizationRecord civ : civilizations.values()) {
            if (civ.hubDimension() == null) {
                continue;
            }
            if (!civ.hubDimension().equals(dimension)) {
                continue;
            }
            if (civ.hubX() == x && civ.hubY() == y && civ.hubZ() == z) {
                return civ.id();
            }
        }
        return null;
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
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, civ.id());
        }
    }

    public boolean isExplosivesExpert(String civIdRaw, UUID playerId) {
        return getOrCreateCivilization(civIdRaw).explosivesExperts().contains(playerId);
    }

    public int explosivesExpertCount(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).explosivesExperts().size();
    }

    public List<UUID> explosivesExpertsSorted(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).explosivesExperts().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
    }

    public boolean setExplosivesExpert(String civIdRaw, UUID playerId, boolean allowed, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        boolean changed;
        if (allowed) {
            changed = civ.explosivesExperts().add(playerId);
            if (changed) {
                addAuditLog(civ.id(), actorName + " designated explosives expert " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
            }
        } else {
            changed = civ.explosivesExperts().remove(playerId);
            if (changed) {
                addAuditLog(civ.id(), actorName + " removed explosives expert " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
            }
        }
        if (!changed) {
            return false;
        }
        setDirty();
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, civ.id());
        }
        return true;
    }

    public boolean isRedstoner(String civIdRaw, UUID playerId) {
        return getOrCreateCivilization(civIdRaw).redstoners().contains(playerId);
    }

    public int redstonerCount(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).redstoners().size();
    }

    public List<UUID> redstonersSorted(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).redstoners().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
    }

    public boolean setRedstoner(String civIdRaw, UUID playerId, boolean allowed, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        boolean changed;
        if (allowed) {
            changed = civ.redstoners().add(playerId);
            if (changed) {
                addAuditLog(civ.id(), actorName + " designated redstoner " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
            }
        } else {
            changed = civ.redstoners().remove(playerId);
            if (changed) {
                addAuditLog(civ.id(), actorName + " removed redstoner " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
            }
        }
        if (!changed) {
            return false;
        }
        setDirty();
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, civ.id());
        }
        return true;
    }

    public static List<String> knownRolePermissions() {
        return KNOWN_ROLE_PERMISSIONS;
    }

    @Nullable
    public static String canonicalRoleId(@Nullable String raw) {
        return normalizeRoleId(raw);
    }

    @Nullable
    public static String canonicalRolePermission(@Nullable String raw) {
        return normalizePermissionKey(raw);
    }

    public String leaderTitle(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).leaderTitle();
    }

    public boolean setLeaderTitle(String civIdRaw, String titleRaw, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        String newTitle = sanitizeLeaderTitle(titleRaw);
        if (civ.leaderTitle().equals(newTitle)) {
            return false;
        }
        civ.setLeaderTitle(newTitle);
        addAuditLog(
                civ.id(),
                actorName + " set leadership title to '" + newTitle + "'",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public double upkeepRateMultiplier(String civIdRaw) {
        return clampUpkeepRateMultiplier(getOrCreateCivilization(civIdRaw).upkeepRateMultiplier());
    }

    public long upkeepCostPerPlotCents(String civIdRaw) {
        double scaled = RealCivConfig.upkeepCostCents() * upkeepRateMultiplier(civIdRaw);
        if (scaled <= 0.0D) {
            return 0L;
        }
        return Math.max(0L, Math.round(scaled));
    }

    public boolean setUpkeepRateMultiplier(String civIdRaw, double multiplier, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        double next = clampUpkeepRateMultiplier(multiplier);
        if (Math.abs(civ.upkeepRateMultiplier() - next) < 0.00001D) {
            return false;
        }
        civ.setUpkeepRateMultiplier(next);
        addAuditLog(
                civ.id(),
                actorName + " set upkeep rate multiplier to " + String.format(Locale.ROOT, "%.2f", next),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public GovernanceModel governanceModel(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).governanceModel();
    }

    public boolean setGovernanceModel(String civIdRaw, GovernanceModel model, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        GovernanceModel normalized = model == null ? GovernanceModel.AUTOCRATIC : model;
        if (civ.governanceModel() == normalized) {
            return false;
        }
        civ.setGovernanceModel(normalized);
        // Model changes can alter voter eligibility/quorum math, so discard any in-flight proposal safely.
        civ.setGovernanceProposal(null);
        addAuditLog(
                civ.id(),
                actorName + " set governance model to " + normalized.serializedName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    @Nullable
    public GovernanceProposalRecord governanceProposal(String civIdRaw) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null || civ.governanceProposal() == null) {
            return null;
        }
        return civ.governanceProposal().copy();
    }

    public void setGovernanceProposal(String civIdRaw, @Nullable GovernanceProposalRecord proposal) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return;
        }
        civ.setGovernanceProposal(proposal == null ? null : proposal.copy());
        setDirty();
    }

    public boolean clearGovernanceProposal(String civIdRaw) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null || civ.governanceProposal() == null) {
            return false;
        }
        civ.setGovernanceProposal(null);
        setDirty();
        return true;
    }

    @Nullable
    public LeadershipContestRecord leadershipContest(String civIdRaw) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null || civ.leadershipContest() == null) {
            return null;
        }
        return civ.leadershipContest().copy();
    }

    public LeadershipContestDecision startLeadershipElection(String civIdRaw, UUID initiatorId, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return LeadershipContestDecision.denied("Civilization not found.");
        }
        if (!isCivilizationMember(civ.id(), initiatorId)) {
            return LeadershipContestDecision.denied("Only civilization members can call an election.");
        }

        LeadershipContestDecision resolved = resolveLeadershipContest(civ.id(), actorName, false);
        if (resolved.changed()) {
            // Fall through after resolving stale/finished contest.
        }
        if (civ.leadershipContest() != null) {
            return LeadershipContestDecision.denied("A leadership contest is already active.");
        }

        long now = System.currentTimeMillis();
        LeadershipContestRecord contest = LeadershipContestRecord.newElection(
                initiatorId,
                now,
                now + RealCivConfig.governanceElectionDurationMillis());
        contest.candidates().add(initiatorId);
        civ.setLeadershipContest(contest);
        addAuditLog(
                civ.id(),
                actorName + " started a leadership election (candidate signup open).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return LeadershipContestDecision.changed("Leadership election started. You are registered as a candidate.");
    }

    public LeadershipContestDecision joinLeadershipElectionCandidate(String civIdRaw, UUID candidateId, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return LeadershipContestDecision.denied("Civilization not found.");
        }
        if (!isCivilizationMember(civ.id(), candidateId)) {
            return LeadershipContestDecision.denied("Only civilization members can run in elections.");
        }
        LeadershipContestRecord contest = civ.leadershipContest();
        if (contest == null || contest.contestType() != LeadershipContestType.ELECTION) {
            return LeadershipContestDecision.denied("No active election to join.");
        }
        if (contest.expiresAtMillis() <= System.currentTimeMillis()) {
            LeadershipContestDecision resolved = resolveLeadershipContest(civ.id(), actorName, true);
            return resolved.changed()
                    ? resolved
                    : LeadershipContestDecision.denied("Election has ended.");
        }
        if (!contest.candidates().add(candidateId)) {
            return LeadershipContestDecision.denied("You are already registered as a candidate.");
        }
        addAuditLog(
                civ.id(),
                actorName + " registered as an election candidate.",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return LeadershipContestDecision.changed("You are now registered as an election candidate.");
    }

    public LeadershipContestDecision voteLeadershipElectionCandidate(
            String civIdRaw,
            UUID voterId,
            UUID candidateId,
            String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return LeadershipContestDecision.denied("Civilization not found.");
        }
        if (!isCivilizationMember(civ.id(), voterId)) {
            return LeadershipContestDecision.denied("Only civilization members can vote.");
        }
        LeadershipContestRecord contest = civ.leadershipContest();
        if (contest == null || contest.contestType() != LeadershipContestType.ELECTION) {
            return LeadershipContestDecision.denied("No active election to vote in.");
        }
        if (!isCivilizationMember(civ.id(), candidateId) || !contest.candidates().contains(candidateId)) {
            return LeadershipContestDecision.denied("That candidate is not registered.");
        }
        contest.electionVotes().put(voterId, candidateId);
        setDirty();
        LeadershipContestDecision maybeResolved = resolveLeadershipContest(civ.id(), actorName, false);
        if (maybeResolved.changed()) {
            return maybeResolved;
        }
        return LeadershipContestDecision.changed("Election vote recorded.");
    }

    public LeadershipContestDecision startLeadershipCoup(
            String civIdRaw,
            UUID initiatorId,
            UUID proposedLeaderId,
            String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return LeadershipContestDecision.denied("Civilization not found.");
        }
        if (!isCivilizationMember(civ.id(), initiatorId)) {
            return LeadershipContestDecision.denied("Only civilization members can start a coup vote.");
        }
        if (!isCivilizationMember(civ.id(), proposedLeaderId)) {
            return LeadershipContestDecision.denied("The proposed coup leader must be a civilization member.");
        }
        int members = civilizationMembersSorted(civ.id()).size();
        int minimum = RealCivConfig.governanceCoupMinMembers();
        if (members < minimum) {
            return LeadershipContestDecision.denied(
                    "Coup voting requires at least " + minimum + " members (current: " + members + ").");
        }

        LeadershipContestDecision resolved = resolveLeadershipContest(civ.id(), actorName, false);
        if (resolved.changed()) {
            // Fall through after resolving stale/finished contest.
        }
        if (civ.leadershipContest() != null) {
            return LeadershipContestDecision.denied("A leadership contest is already active.");
        }

        long now = System.currentTimeMillis();
        LeadershipContestRecord contest = LeadershipContestRecord.newCoup(
                initiatorId,
                proposedLeaderId,
                now,
                now + RealCivConfig.governanceCoupDurationMillis());
        contest.coupApprovals().add(initiatorId);
        civ.setLeadershipContest(contest);
        addAuditLog(
                civ.id(),
                actorName + " initiated a coup vote for leader " + proposedLeaderId + ".",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        LeadershipContestDecision maybeResolved = resolveLeadershipContest(civ.id(), actorName, false);
        if (maybeResolved.changed()) {
            return maybeResolved;
        }
        return LeadershipContestDecision.changed("Coup vote started.");
    }

    public LeadershipContestDecision approveLeadershipCoup(String civIdRaw, UUID voterId, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return LeadershipContestDecision.denied("Civilization not found.");
        }
        if (!isCivilizationMember(civ.id(), voterId)) {
            return LeadershipContestDecision.denied("Only civilization members can vote.");
        }
        LeadershipContestRecord contest = civ.leadershipContest();
        if (contest == null || contest.contestType() != LeadershipContestType.COUP) {
            return LeadershipContestDecision.denied("No active coup vote.");
        }
        if (contest.expiresAtMillis() <= System.currentTimeMillis()) {
            LeadershipContestDecision resolved = resolveLeadershipContest(civ.id(), actorName, true);
            return resolved.changed()
                    ? resolved
                    : LeadershipContestDecision.denied("Coup vote has ended.");
        }
        if (!contest.coupApprovals().add(voterId)) {
            return LeadershipContestDecision.denied("You have already approved this coup vote.");
        }
        setDirty();
        LeadershipContestDecision maybeResolved = resolveLeadershipContest(civ.id(), actorName, false);
        if (maybeResolved.changed()) {
            return maybeResolved;
        }
        return LeadershipContestDecision.changed("Coup approval recorded.");
    }

    public LeadershipContestDecision resolveLeadershipContest(String civIdRaw, String actorName, boolean force) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null || civ.leadershipContest() == null) {
            return LeadershipContestDecision.denied("No active leadership contest.");
        }
        LeadershipContestRecord contest = civ.leadershipContest();
        int memberCount = civilizationMembersSorted(civ.id()).size();
        long now = System.currentTimeMillis();
        boolean expired = now >= contest.expiresAtMillis();

        if (!force && !expired) {
            if (contest.contestType() == LeadershipContestType.ELECTION) {
                int totalVotes = 0;
                for (Map.Entry<UUID, UUID> vote : contest.electionVotes().entrySet()) {
                    if (isCivilizationMember(civ.id(), vote.getKey())
                            && contest.candidates().contains(vote.getValue())
                            && isCivilizationMember(civ.id(), vote.getValue())) {
                        totalVotes++;
                    }
                }
                if (memberCount <= 0 || totalVotes < memberCount) {
                    return LeadershipContestDecision.denied("Leadership contest remains active.");
                }
            } else if (contest.contestType() == LeadershipContestType.COUP) {
                int approvals = countValidCoupApprovals(civ.id(), contest);
                int required = requiredCoupApprovals(memberCount);
                if (approvals < required) {
                    return LeadershipContestDecision.denied("Leadership contest remains active.");
                }
            }
        }

        if (contest.contestType() == LeadershipContestType.ELECTION) {
            @Nullable UUID winner = electionWinner(civ.id(), contest);
            civ.setLeadershipContest(null);
            setDirty();
            if (winner == null) {
                addAuditLog(
                        civ.id(),
                        actorName + " closed election with no valid winner.",
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                return LeadershipContestDecision.changed("Election ended with no winner (no valid votes/candidates).");
            }
            setMayor(civ.id(), winner, actorName);
            addAuditLog(
                    civ.id(),
                    actorName + " resolved election winner: " + winner + ".",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            return LeadershipContestDecision.changed("Election resolved. New leader selected.");
        }

        @Nullable UUID coupLeaderId = contest.coupLeaderId();
        int approvals = countValidCoupApprovals(civ.id(), contest);
        int required = requiredCoupApprovals(memberCount);
        civ.setLeadershipContest(null);
        setDirty();
        if (coupLeaderId == null || !isCivilizationMember(civ.id(), coupLeaderId)) {
            addAuditLog(
                    civ.id(),
                    actorName + " resolved coup vote with invalid leader target.",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            return LeadershipContestDecision.changed("Coup vote ended without a valid leader target.");
        }
        if (approvals >= required) {
            setMayor(civ.id(), coupLeaderId, actorName);
            addAuditLog(
                    civ.id(),
                    actorName + " resolved successful coup vote. New leader: " + coupLeaderId
                            + " (" + approvals + "/" + required + " approvals).",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            return LeadershipContestDecision.changed("Coup approved. New leader selected.");
        }
        addAuditLog(
                civ.id(),
                actorName + " resolved failed coup vote (" + approvals + "/" + required + " approvals).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        return LeadershipContestDecision.changed("Coup vote failed to reach required approval.");
    }

    public List<String> resolveLeadershipContestsSystem() {
        List<String> resolved = new ArrayList<>();
        for (String civId : civilizationIdsSorted()) {
            CivilizationRecord civ = getCivilization(civId);
            if (civ == null || civ.leadershipContest() == null) {
                continue;
            }
            LeadershipContestDecision decision = resolveLeadershipContest(civ.id(), "system", false);
            if (decision.changed()) {
                resolved.add(civ.id() + ": " + decision.message());
            }
        }
        return resolved;
    }

    private int requiredCoupApprovals(int memberCount) {
        int safeMembers = Math.max(1, memberCount);
        return Math.max(1, (int) Math.ceil(safeMembers * 0.51D));
    }

    private int countValidCoupApprovals(String civId, LeadershipContestRecord contest) {
        int approvals = 0;
        for (UUID voterId : contest.coupApprovals()) {
            if (isCivilizationMember(civId, voterId)) {
                approvals++;
            }
        }
        return approvals;
    }

    @Nullable
    private UUID electionWinner(String civId, LeadershipContestRecord contest) {
        Map<UUID, Integer> votes = new HashMap<>();
        for (Map.Entry<UUID, UUID> voteEntry : contest.electionVotes().entrySet()) {
            UUID voterId = voteEntry.getKey();
            UUID candidateId = voteEntry.getValue();
            if (!isCivilizationMember(civId, voterId)) {
                continue;
            }
            if (!contest.candidates().contains(candidateId) || !isCivilizationMember(civId, candidateId)) {
                continue;
            }
            votes.merge(candidateId, 1, Integer::sum);
        }
        if (votes.isEmpty()) {
            return null;
        }
        @Nullable UUID incumbent = getMayorId(civId);
        UUID winner = null;
        int bestVotes = Integer.MIN_VALUE;
        for (Map.Entry<UUID, Integer> entry : votes.entrySet()) {
            UUID candidateId = entry.getKey();
            int value = entry.getValue();
            if (winner == null || value > bestVotes) {
                winner = candidateId;
                bestVotes = value;
                continue;
            }
            if (value == bestVotes) {
                if (incumbent != null && candidateId.equals(incumbent) && !winner.equals(incumbent)) {
                    winner = candidateId;
                    continue;
                }
                String currentRaw = winner.toString();
                String candidateRaw = candidateId.toString();
                if (candidateRaw.compareTo(currentRaw) < 0) {
                    winner = candidateId;
                }
            }
        }
        return winner;
    }

    public HubDistributionMode hubDistributionMode(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).hubDistributionMode();
    }

    public boolean setHubDistributionMode(String civIdRaw, @Nullable HubDistributionMode mode, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        HubDistributionMode normalized = mode == null
                ? HubDistributionMode.CONTRIBUTION_RATIO
                : mode;
        if (civ.hubDistributionMode() == normalized) {
            return false;
        }
        civ.setHubDistributionMode(normalized);
        addAuditLog(
                civ.id(),
                actorName + " set hub distribution mode to " + normalized.serializedName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public double hubSharedWithdrawRatio(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return clampUnitRatio(civ.hubSharedWithdrawRatio());
    }

    public boolean setHubSharedWithdrawRatio(String civIdRaw, double ratio, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        double next = clampUnitRatio(ratio);
        if (Math.abs(next - civ.hubSharedWithdrawRatio()) < 0.000001D) {
            return false;
        }
        civ.setHubSharedWithdrawRatio(next);
        addAuditLog(
                civ.id(),
                actorName + " set hub shared-stock withdraw ratio to "
                        + String.format(Locale.ROOT, "%.2f", next * 100.0D) + "%",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public long hubSharedStockDailyLimit(String civIdRaw, ResourceLocation itemId) {
        long stock = Math.max(0L, getHubStock(civIdRaw, itemId));
        if (stock <= 0L) {
            return 0L;
        }
        double ratio = clampUnitRatio(hubSharedWithdrawRatio(civIdRaw));
        if (ratio <= 0.0D) {
            return 0L;
        }
        long limit = (long) Math.floor(stock * ratio);
        if (limit <= 0L) {
            return 1L;
        }
        return limit;
    }

    public TaxPaymentMode taxPaymentMode(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return civ.taxPaymentMode();
    }

    public boolean setTaxPaymentMode(String civIdRaw, @Nullable TaxPaymentMode mode, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        TaxPaymentMode next = mode == null ? TaxPaymentMode.KARMA : mode;
        if (civ.taxPaymentMode() == next) {
            return false;
        }
        civ.setTaxPaymentMode(next);
        addAuditLog(
                civ.id(),
                actorName + " set tax payment mode to " + next.serializedName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public ResourceLocation taxItemId(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        try {
            return ResourceLocation.parse(civ.taxItemId());
        } catch (Exception ignored) {
            return ResourceLocation.parse("minecraft:gold_nugget");
        }
    }

    public int taxItemCountPerPlot(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return Math.max(1, civ.taxItemCountPerPlot());
    }

    public long taxItemCostPerPlotCurrentRate(String civIdRaw) {
        long base = Math.max(1L, taxItemCountPerPlot(civIdRaw));
        double scaled = base * upkeepRateMultiplier(civIdRaw);
        return Math.max(1L, Math.round(scaled));
    }

    public boolean setTaxItemRule(
            String civIdRaw,
            ResourceLocation itemId,
            int countPerPlot,
            String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        String key = itemId.toString();
        int safeCount = Math.max(1, countPerPlot);
        if (civ.taxItemId().equals(key) && civ.taxItemCountPerPlot() == safeCount) {
            return false;
        }
        civ.setTaxItemId(key);
        civ.setTaxItemCountPerPlot(safeCount);
        addAuditLog(
                civ.id(),
                actorName + " set tax item rule to " + key + " x" + safeCount + "/plot/cycle",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public List<Map.Entry<String, Integer>> hubDailyAllowanceEntriesSorted(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return civ.hubDailyAllowances().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .toList();
    }

    public int hubDailyAllowanceLimit(String civIdRaw, ResourceLocation itemId) {
        return Math.max(0, getOrCreateCivilization(civIdRaw).hubDailyAllowances().getOrDefault(itemId.toString(), 0));
    }

    public boolean setHubDailyAllowanceLimit(
            String civIdRaw,
            ResourceLocation itemId,
            int dailyCount,
            String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        String key = itemId.toString();
        int safeCount = Math.max(0, dailyCount);
        int previous = Math.max(0, civ.hubDailyAllowances().getOrDefault(key, 0));
        if (safeCount <= 0) {
            if (previous <= 0) {
                return false;
            }
            civ.hubDailyAllowances().remove(key);
            addAuditLog(
                    civ.id(),
                    actorName + " cleared daily allowance limit for " + key,
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            setDirty();
            return true;
        }
        if (previous == safeCount) {
            return false;
        }
        civ.hubDailyAllowances().put(key, safeCount);
        addAuditLog(
                civ.id(),
                actorName + " set daily allowance limit for " + key + " to " + safeCount + "/day",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public int clearAllHubDailyAllowanceLimits(String civIdRaw, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null || civ.hubDailyAllowances().isEmpty()) {
            return 0;
        }
        int cleared = civ.hubDailyAllowances().size();
        civ.hubDailyAllowances().clear();
        addAuditLog(
                civ.id(),
                actorName + " cleared all daily allowance limits (" + cleared + " item entries)",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return cleared;
    }

    public List<CivRoleView> customRolesSorted(String civIdRaw) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return List.of();
        }
        List<CivRoleView> out = new ArrayList<>();
        for (CivilizationRecord.RoleRecord role : civ.customRoles().values()) {
            List<UUID> members = role.members().stream()
                    .sorted(Comparator.comparing(UUID::toString))
                    .toList();
            List<String> sortedPermissions = role.permissions().stream().sorted().toList();
            out.add(new CivRoleView(
                    role.roleId(),
                    role.displayName(),
                    Collections.unmodifiableSet(new java.util.LinkedHashSet<>(sortedPermissions)),
                    members));
        }
        out.sort(Comparator.comparing(CivRoleView::roleId));
        return out;
    }

    public boolean customRoleExists(String civIdRaw, String roleIdRaw) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        @Nullable String roleId = normalizeRoleId(roleIdRaw);
        if (roleId == null) {
            return false;
        }
        return civ.customRoles().containsKey(roleId);
    }

    public boolean createCustomRole(
            String civIdRaw,
            String roleIdRaw,
            @Nullable String displayNameRaw,
            String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        @Nullable String roleId = normalizeRoleId(roleIdRaw);
        if (roleId == null || civ.customRoles().containsKey(roleId)) {
            return false;
        }
        String displayName = sanitizeRoleDisplayName(displayNameRaw == null || displayNameRaw.isBlank()
                ? roleId
                : displayNameRaw);
        civ.customRoles().put(roleId, new CivilizationRecord.RoleRecord(roleId, displayName));
        addAuditLog(
                civ.id(),
                actorName + " created civ role '" + displayName + "' [" + roleId + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean renameCustomRole(String civIdRaw, String roleIdRaw, String displayNameRaw, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        @Nullable String roleId = normalizeRoleId(roleIdRaw);
        if (roleId == null) {
            return false;
        }
        CivilizationRecord.RoleRecord role = civ.customRoles().get(roleId);
        if (role == null) {
            return false;
        }
        String displayName = sanitizeRoleDisplayName(displayNameRaw);
        if (role.displayName().equals(displayName)) {
            return false;
        }
        role.setDisplayName(displayName);
        addAuditLog(
                civ.id(),
                actorName + " renamed civ role [" + roleId + "] to '" + displayName + "'",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean deleteCustomRole(String civIdRaw, String roleIdRaw, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        @Nullable String roleId = normalizeRoleId(roleIdRaw);
        if (roleId == null) {
            return false;
        }
        CivilizationRecord.RoleRecord removed = civ.customRoles().remove(roleId);
        if (removed == null) {
            return false;
        }
        addAuditLog(
                civ.id(),
                actorName + " deleted civ role '" + removed.displayName() + "' [" + roleId + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean setCustomRolePermission(
            String civIdRaw,
            String roleIdRaw,
            String permissionRaw,
            boolean allowed,
            String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        @Nullable String roleId = normalizeRoleId(roleIdRaw);
        @Nullable String permission = normalizePermissionKey(permissionRaw);
        if (roleId == null || permission == null) {
            return false;
        }
        CivilizationRecord.RoleRecord role = civ.customRoles().get(roleId);
        if (role == null) {
            return false;
        }
        boolean changed;
        if (allowed) {
            changed = role.permissions().add(permission);
        } else {
            changed = role.permissions().remove(permission);
        }
        if (!changed) {
            return false;
        }
        addAuditLog(
                civ.id(),
                actorName + (allowed ? " granted " : " revoked ")
                        + "permission '" + permission + "' for role '" + role.displayName() + "' [" + roleId + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean setCustomRoleMember(
            String civIdRaw,
            String roleIdRaw,
            UUID playerId,
            boolean allowed,
            String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        @Nullable String roleId = normalizeRoleId(roleIdRaw);
        if (roleId == null) {
            return false;
        }
        CivilizationRecord.RoleRecord role = civ.customRoles().get(roleId);
        if (role == null) {
            return false;
        }
        @Nullable String memberCiv = normalizeCivId(playerCivilization.get(playerId));
        if (allowed && (memberCiv == null || !memberCiv.equals(civ.id()))) {
            return false;
        }
        boolean changed;
        if (allowed) {
            changed = role.members().add(playerId);
        } else {
            changed = role.members().remove(playerId);
        }
        if (!changed) {
            return false;
        }
        addAuditLog(
                civ.id(),
                actorName + (allowed ? " assigned " : " removed ")
                        + "player " + playerId + (allowed ? " to " : " from ")
                        + "role '" + role.displayName() + "' [" + roleId + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean hasCustomRolePermission(String civIdRaw, UUID playerId, String permissionRaw) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        @Nullable String permission = normalizePermissionKey(permissionRaw);
        if (permission == null) {
            return false;
        }
        @Nullable String memberCiv = normalizeCivId(playerCivilization.get(playerId));
        if (memberCiv == null || !memberCiv.equals(civ.id())) {
            return false;
        }
        for (CivilizationRecord.RoleRecord role : civ.customRoles().values()) {
            if (role.members().contains(playerId) && role.permissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasJoinRequest(String civIdRaw, UUID playerId) {
        return getOrCreateCivilization(civIdRaw).joinRequests().contains(playerId);
    }

    public boolean hasInvite(String civIdRaw, UUID playerId) {
        return getOrCreateCivilization(civIdRaw).invitedPlayers().contains(playerId);
    }

    public boolean addJoinRequest(String civIdRaw, UUID playerId, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        if (!civ.joinRequests().add(playerId)) {
            return false;
        }
        addAuditLog(civ.id(), actorName + " requested to join civilization", RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean removeJoinRequest(String civIdRaw, UUID playerId, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        if (!civ.joinRequests().remove(playerId)) {
            return false;
        }
        addAuditLog(civ.id(), actorName + " cleared a join request", RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean addInvite(String civIdRaw, UUID playerId, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.joinRequests().remove(playerId);
        if (!civ.invitedPlayers().add(playerId)) {
            return false;
        }
        addAuditLog(civ.id(), actorName + " invited player " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public boolean removeInvite(String civIdRaw, UUID playerId, String actorName) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        if (!civ.invitedPlayers().remove(playerId)) {
            return false;
        }
        addAuditLog(civ.id(), actorName + " revoked invite for player " + playerId, RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public List<UUID> joinRequestsSorted(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return civ.joinRequests().stream().sorted(Comparator.comparing(UUID::toString)).toList();
    }

    public List<UUID> invitedPlayersSorted(String civIdRaw) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        return civ.invitedPlayers().stream().sorted(Comparator.comparing(UUID::toString)).toList();
    }

    public DiplomacyState diplomacyState(String civAraw, String civBraw) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB)) {
            return DiplomacyState.NEUTRAL;
        }
        String key = diplomacyKey(civA, civB);
        if (key == null) {
            return DiplomacyState.NEUTRAL;
        }
        return diplomacy.getOrDefault(key, DiplomacyState.NEUTRAL);
    }

    public boolean setDiplomacyState(String civAraw, String civBraw, DiplomacyState state, String actorName) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB) || state == null) {
            return false;
        }
        CivilizationRecord civARecord = getCivilization(civA);
        CivilizationRecord civBRecord = getCivilization(civB);
        if (civARecord == null || civBRecord == null) {
            return false;
        }

        DiplomacyState current = diplomacyState(civA, civB);
        if (current == state) {
            return false;
        }
        setDiplomacyStateInternal(civA, civB, state);

        addAuditLog(
                civARecord.id(),
                actorName + " set diplomacy with " + civBRecord.displayName() + " [" + civBRecord.id() + "] to "
                        + state.displayName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        addAuditLog(
                civBRecord.id(),
                actorName + " set diplomacy with " + civARecord.displayName() + " [" + civARecord.id() + "] to "
                        + state.displayName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    public WarCasualtyView warCasualtiesBetween(String civAraw, String civBraw) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB)) {
            return new WarCasualtyView(0L, 0L);
        }
        String key = diplomacyKey(civA, civB);
        if (key == null) {
            return new WarCasualtyView(0L, 0L);
        }
        WarCasualtyRecord record = warCasualties.get(key);
        if (record == null) {
            return new WarCasualtyView(0L, 0L);
        }
        DiplomacyPair pair = diplomacyPairFromKey(key);
        if (pair == null) {
            return new WarCasualtyView(0L, 0L);
        }
        if (civA.equals(pair.firstCivId())) {
            return new WarCasualtyView(Math.max(0L, record.firstCasualties), Math.max(0L, record.secondCasualties));
        }
        return new WarCasualtyView(Math.max(0L, record.secondCasualties), Math.max(0L, record.firstCasualties));
    }

    public void recordWarCasualty(String killerCivRaw, String victimCivRaw) {
        String killerCiv = normalizeCivId(killerCivRaw);
        String victimCiv = normalizeCivId(victimCivRaw);
        if (killerCiv == null || victimCiv == null || killerCiv.equals(victimCiv)) {
            return;
        }
        String key = diplomacyKey(killerCiv, victimCiv);
        DiplomacyPair pair = key == null ? null : diplomacyPairFromKey(key);
        if (key == null || pair == null) {
            return;
        }
        WarCasualtyRecord record = warCasualties.computeIfAbsent(key, ignored -> new WarCasualtyRecord());
        if (victimCiv.equals(pair.firstCivId())) {
            record.firstCasualties = Math.min(Long.MAX_VALUE, record.firstCasualties + 1L);
        } else if (victimCiv.equals(pair.secondCivId())) {
            record.secondCasualties = Math.min(Long.MAX_VALUE, record.secondCasualties + 1L);
        }
        setDirty();
    }

    public List<DiplomacyView> nonNeutralDiplomacyEntriesFor(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null || !civilizations.containsKey(civId)) {
            return List.of();
        }
        List<DiplomacyView> out = new ArrayList<>();
        for (String otherId : civilizations.keySet()) {
            if (civId.equals(otherId)) {
                continue;
            }
            DiplomacyState state = diplomacyState(civId, otherId);
            if (state != DiplomacyState.NEUTRAL) {
                out.add(new DiplomacyView(otherId, state));
            }
        }
        out.sort(Comparator.comparing(DiplomacyView::otherCivilizationId));
        return out;
    }

    public boolean allowIntraCivPvp(String civIdRaw) {
        return getOrCreateCivilization(civIdRaw).allowIntraCivPvp();
    }

    public boolean setAllowIntraCivPvp(String civIdRaw, boolean allowed, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null) {
            return false;
        }
        if (civ.allowIntraCivPvp() == allowed) {
            return false;
        }
        civ.setAllowIntraCivPvp(allowed);
        addAuditLog(
                civ.id(),
                actorName + " set intra-civilization PvP to " + (allowed ? "enabled" : "disabled"),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return true;
    }

    private void setDiplomacyStateInternal(String civAraw, String civBraw, DiplomacyState state) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB) || state == null) {
            return;
        }
        String key = diplomacyKey(civA, civB);
        if (key == null) {
            return;
        }
        if (state == DiplomacyState.NEUTRAL) {
            diplomacy.remove(key);
        } else {
            diplomacy.put(key, state);
        }
    }

    private int removeDiplomacyLinksForCivilization(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return 0;
        }
        int removed = 0;
        Iterator<Map.Entry<String, DiplomacyState>> iterator = diplomacy.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DiplomacyState> entry = iterator.next();
            DiplomacyPair pair = diplomacyPairFromKey(entry.getKey());
            if (pair == null) {
                iterator.remove();
                removed++;
                continue;
            }
            if (civId.equals(pair.firstCivId()) || civId.equals(pair.secondCivId())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private int removeWarCasualtiesForCivilization(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return 0;
        }
        int removed = 0;
        Iterator<Map.Entry<String, WarCasualtyRecord>> iterator = warCasualties.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WarCasualtyRecord> entry = iterator.next();
            DiplomacyPair pair = diplomacyPairFromKey(entry.getKey());
            if (pair == null) {
                iterator.remove();
                removed++;
                continue;
            }
            if (civId.equals(pair.firstCivId()) || civId.equals(pair.secondCivId())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private static void removePlayerFromAllCustomRoles(CivilizationRecord civ, UUID playerId) {
        for (CivilizationRecord.RoleRecord role : civ.customRoles().values()) {
            role.members().remove(playerId);
        }
    }

    public boolean removeMemberToDefault(String civIdRaw, UUID playerId, String actorName) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return false;
        }
        String current = normalizeCivId(playerCivilization.get(playerId));
        if (current == null || !current.equals(civId)) {
            return false;
        }
        String fallback = RealCivConfig.defaultCivilizationId();
        playerCivilization.put(playerId, fallback);
        CivilizationRecord civ = getOrCreateCivilization(civId);
        civ.civicManagers().remove(playerId);
        civ.explosivesExperts().remove(playerId);
        civ.redstoners().remove(playerId);
        removePlayerFromAllCustomRoles(civ, playerId);
        civ.joinRequests().remove(playerId);
        civ.invitedPlayers().remove(playerId);
        if (civ.mayorId() != null && civ.mayorId().equals(playerId)) {
            civ.setMayorId(null);
            addAuditLog(civ.id(), actorName + " removed mayor " + playerId + " from civilization", RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            addAuditLog(civ.id(), actorName + " removed member " + playerId + " from civilization", RealCivConfig.MAX_AUDIT_LOGS.get());
        }
        setDirty();
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, civId);
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, fallback);
        }
        return true;
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
        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncPlotChange(
                    attachedServer,
                    this,
                    civ.id(),
                    dimension,
                    chunkX,
                    chunkZ,
                    true);
        }
    }

    public boolean clearPlot(String civIdRaw, String dimension, long chunkX, long chunkZ) {
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        PlotRecord removed = civ.plots().remove(chunkKey(dimension, chunkX, chunkZ));
        if (removed != null) {
            setDirty();
            if (attachedServer != null) {
                RealCivFTBChunksMirror.syncPlotChange(
                        attachedServer,
                        this,
                        civ.id(),
                        dimension,
                        chunkX,
                        chunkZ,
                        false);
            }
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
            case COMMUNITY -> isCivilizationMember(civIdRaw, playerId);
            case CIVIC -> isMayor(civIdRaw, playerId) || isCivicManager(civIdRaw, playerId);
            case PRIVATE -> plot.ownerId() != null && plot.ownerId().equals(playerId);
        };
    }

    public boolean canBreakOnPlot(String civIdRaw, PlotRecord plot, UUID playerId, boolean bypass) {
        if (bypass) {
            return true;
        }
        return switch (plot.landClass()) {
            case COMMUNITY -> isCivilizationMember(civIdRaw, playerId);
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
        boolean changed = false;

        for (CivilizationRecord civ : civilizations.values()) {
            long cost = upkeepCostPerPlotCents(civ.id());
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
                    plot.setLandClass(LandClass.COMMUNITY);
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
        applyDeposit(
                RealCivConfig.defaultCivilizationId(),
                playerId,
                itemId,
                itemCount,
                rewardRule,
                itemCount,
                actorName);
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

    public record HubLocation(String dimension, int x, int y, int z) {
    }

    public enum DiplomacyState {
        ALLY,
        NEUTRAL,
        WAR;

        @Nullable
        public static DiplomacyState fromSerializedName(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return switch (raw.trim().toUpperCase(java.util.Locale.ROOT)) {
                case "ALLY", "ALLIES" -> ALLY;
                case "NEUTRAL", "NONE" -> NEUTRAL;
                case "WAR", "HOSTILE", "HOSTILES" -> WAR;
                default -> null;
            };
        }

        public String serializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public String displayName() {
            return switch (this) {
                case ALLY -> "ALLY";
                case NEUTRAL -> "NEUTRAL";
                case WAR -> "WAR";
            };
        }
    }

    public enum GovernanceModel {
        AUTOCRATIC,
        COUNCIL,
        DEMOCRATIC;

        @Nullable
        public static GovernanceModel fromSerializedName(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "AUTOCRATIC", "AUTOCRACY", "AUTO" -> AUTOCRATIC;
                case "COUNCIL", "OLIGARCHY" -> COUNCIL;
                case "DEMOCRATIC", "DEMOCRACY", "DEMO" -> DEMOCRATIC;
                default -> null;
            };
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum HubDistributionMode {
        CONTRIBUTION_RATIO,
        SHARED_STOCK_RATIO,
        DAILY_ALLOWANCE;

        @Nullable
        public static HubDistributionMode fromSerializedName(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "CONTRIBUTION_RATIO", "RATIO", "CONTRIBUTION", "DEFAULT" -> CONTRIBUTION_RATIO;
                case "SHARED_STOCK_RATIO", "SHARED", "GLOBAL", "ALL_GOODS", "STOCK_RATIO" -> SHARED_STOCK_RATIO;
                case "DAILY_ALLOWANCE", "ALLOWANCE", "DAILY" -> DAILY_ALLOWANCE;
                default -> null;
            };
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum TaxPaymentMode {
        KARMA,
        ITEM;

        @Nullable
        public static TaxPaymentMode fromSerializedName(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "KARMA", "CREDIT", "CREDITS", "COMMUNITY_KARMA" -> KARMA;
                case "ITEM", "ITEMS", "GOODS" -> ITEM;
                default -> null;
            };
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final class GovernanceProposalRecord {
        private final String actionType;
        private final String payload;
        private final String summary;
        private final String permissionKey;
        private final GovernanceModel governanceModel;
        @Nullable
        private final UUID proposerId;
        private final int requiredYesVotes;
        private final long expiresAtMillis;
        private final Set<UUID> yesVotes = new HashSet<>();
        private final Set<UUID> noVotes = new HashSet<>();

        public GovernanceProposalRecord(
                String actionType,
                String payload,
                String summary,
                String permissionKey,
                GovernanceModel governanceModel,
                @Nullable UUID proposerId,
                int requiredYesVotes,
                long expiresAtMillis) {
            this.actionType = actionType;
            this.payload = payload;
            this.summary = summary;
            this.permissionKey = permissionKey;
            this.governanceModel = governanceModel == null ? GovernanceModel.AUTOCRATIC : governanceModel;
            this.proposerId = proposerId;
            this.requiredYesVotes = Math.max(1, requiredYesVotes);
            this.expiresAtMillis = expiresAtMillis;
        }

        public String actionType() {
            return actionType;
        }

        public String payload() {
            return payload;
        }

        public String summary() {
            return summary;
        }

        public String permissionKey() {
            return permissionKey;
        }

        public GovernanceModel governanceModel() {
            return governanceModel;
        }

        @Nullable
        public UUID proposerId() {
            return proposerId;
        }

        public int requiredYesVotes() {
            return requiredYesVotes;
        }

        public long expiresAtMillis() {
            return expiresAtMillis;
        }

        public Set<UUID> yesVotes() {
            return Collections.unmodifiableSet(yesVotes);
        }

        public Set<UUID> noVotes() {
            return Collections.unmodifiableSet(noVotes);
        }

        public void voteYes(UUID playerId) {
            noVotes.remove(playerId);
            yesVotes.add(playerId);
        }

        public void voteNo(UUID playerId) {
            yesVotes.remove(playerId);
            noVotes.add(playerId);
        }

        public boolean matchesAction(String type, String actionPayload, String permission) {
            return actionType.equals(type)
                    && payload.equals(actionPayload)
                    && permissionKey.equals(permission);
        }

        public GovernanceProposalRecord copy() {
            GovernanceProposalRecord copy = new GovernanceProposalRecord(
                    actionType,
                    payload,
                    summary,
                    permissionKey,
                    governanceModel,
                    proposerId,
                    requiredYesVotes,
                    expiresAtMillis);
            copy.yesVotes.addAll(yesVotes);
            copy.noVotes.addAll(noVotes);
            return copy;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("actionType", actionType);
            tag.putString("payload", payload);
            tag.putString("summary", summary);
            tag.putString("permissionKey", permissionKey);
            tag.putString("governanceModel", governanceModel.serializedName());
            if (proposerId != null) {
                tag.putString("proposerId", proposerId.toString());
            }
            tag.putInt("requiredYesVotes", Math.max(1, requiredYesVotes));
            tag.putLong("expiresAtMillis", expiresAtMillis);
            ListTag yesTags = new ListTag();
            for (UUID id : yesVotes) {
                yesTags.add(StringTag.valueOf(id.toString()));
            }
            tag.put("yesVotes", yesTags);
            ListTag noTags = new ListTag();
            for (UUID id : noVotes) {
                noTags.add(StringTag.valueOf(id.toString()));
            }
            tag.put("noVotes", noTags);
            return tag;
        }

        @Nullable
        public static GovernanceProposalRecord load(CompoundTag tag) {
            if (!tag.contains("actionType", Tag.TAG_STRING)
                    || !tag.contains("payload", Tag.TAG_STRING)
                    || !tag.contains("permissionKey", Tag.TAG_STRING)
                    || !tag.contains("governanceModel", Tag.TAG_STRING)) {
                return null;
            }
            @Nullable GovernanceModel model = GovernanceModel.fromSerializedName(tag.getString("governanceModel"));
            if (model == null) {
                return null;
            }
            String summary = tag.contains("summary", Tag.TAG_STRING)
                    ? tag.getString("summary")
                    : tag.getString("actionType");
            @Nullable UUID proposerId = null;
            if (tag.contains("proposerId", Tag.TAG_STRING)) {
                try {
                    proposerId = UUID.fromString(tag.getString("proposerId"));
                } catch (Exception ignored) {
                }
            }
            GovernanceProposalRecord out = new GovernanceProposalRecord(
                    tag.getString("actionType"),
                    tag.getString("payload"),
                    summary,
                    tag.getString("permissionKey"),
                    model,
                    proposerId,
                    Math.max(1, tag.getInt("requiredYesVotes")),
                    tag.getLong("expiresAtMillis"));
            ListTag yesTags = tag.getList("yesVotes", Tag.TAG_STRING);
            for (Tag entry : yesTags) {
                try {
                    out.yesVotes.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }
            ListTag noTags = tag.getList("noVotes", Tag.TAG_STRING);
            for (Tag entry : noTags) {
                try {
                    out.noVotes.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }
            return out;
        }
    }

    public enum LeadershipContestType {
        ELECTION,
        COUP;

        @Nullable
        public static LeadershipContestType fromSerializedName(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "ELECTION" -> ELECTION;
                case "COUP" -> COUP;
                default -> null;
            };
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final class LeadershipContestRecord {
        private final LeadershipContestType contestType;
        @Nullable
        private final UUID initiatorId;
        @Nullable
        private final UUID coupLeaderId;
        private final long startedAtMillis;
        private final long expiresAtMillis;
        private final Set<UUID> candidates = new HashSet<>();
        private final Map<UUID, UUID> electionVotes = new HashMap<>();
        private final Set<UUID> coupApprovals = new HashSet<>();

        private LeadershipContestRecord(
                LeadershipContestType contestType,
                @Nullable UUID initiatorId,
                @Nullable UUID coupLeaderId,
                long startedAtMillis,
                long expiresAtMillis) {
            this.contestType = contestType == null ? LeadershipContestType.ELECTION : contestType;
            this.initiatorId = initiatorId;
            this.coupLeaderId = coupLeaderId;
            this.startedAtMillis = startedAtMillis;
            this.expiresAtMillis = expiresAtMillis;
        }

        public static LeadershipContestRecord newElection(
                @Nullable UUID initiatorId,
                long startedAtMillis,
                long expiresAtMillis) {
            return new LeadershipContestRecord(
                    LeadershipContestType.ELECTION,
                    initiatorId,
                    null,
                    startedAtMillis,
                    expiresAtMillis);
        }

        public static LeadershipContestRecord newCoup(
                @Nullable UUID initiatorId,
                @Nullable UUID coupLeaderId,
                long startedAtMillis,
                long expiresAtMillis) {
            return new LeadershipContestRecord(
                    LeadershipContestType.COUP,
                    initiatorId,
                    coupLeaderId,
                    startedAtMillis,
                    expiresAtMillis);
        }

        public LeadershipContestType contestType() {
            return contestType;
        }

        @Nullable
        public UUID initiatorId() {
            return initiatorId;
        }

        @Nullable
        public UUID coupLeaderId() {
            return coupLeaderId;
        }

        public long startedAtMillis() {
            return startedAtMillis;
        }

        public long expiresAtMillis() {
            return expiresAtMillis;
        }

        public Set<UUID> candidates() {
            return candidates;
        }

        public Map<UUID, UUID> electionVotes() {
            return electionVotes;
        }

        public Set<UUID> coupApprovals() {
            return coupApprovals;
        }

        public LeadershipContestRecord copy() {
            LeadershipContestRecord copy = new LeadershipContestRecord(
                    contestType,
                    initiatorId,
                    coupLeaderId,
                    startedAtMillis,
                    expiresAtMillis);
            copy.candidates.addAll(candidates);
            copy.electionVotes.putAll(electionVotes);
            copy.coupApprovals.addAll(coupApprovals);
            return copy;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("contestType", contestType.serializedName());
            if (initiatorId != null) {
                tag.putString("initiatorId", initiatorId.toString());
            }
            if (coupLeaderId != null) {
                tag.putString("coupLeaderId", coupLeaderId.toString());
            }
            tag.putLong("startedAtMillis", startedAtMillis);
            tag.putLong("expiresAtMillis", expiresAtMillis);

            ListTag candidateTags = new ListTag();
            for (UUID candidateId : candidates) {
                candidateTags.add(StringTag.valueOf(candidateId.toString()));
            }
            tag.put("candidates", candidateTags);

            ListTag voteTags = new ListTag();
            for (Map.Entry<UUID, UUID> vote : electionVotes.entrySet()) {
                CompoundTag voteTag = new CompoundTag();
                voteTag.putString("voterId", vote.getKey().toString());
                voteTag.putString("candidateId", vote.getValue().toString());
                voteTags.add(voteTag);
            }
            tag.put("electionVotes", voteTags);

            ListTag approvalTags = new ListTag();
            for (UUID voterId : coupApprovals) {
                approvalTags.add(StringTag.valueOf(voterId.toString()));
            }
            tag.put("coupApprovals", approvalTags);
            return tag;
        }

        @Nullable
        public static LeadershipContestRecord load(CompoundTag tag) {
            if (!tag.contains("contestType", Tag.TAG_STRING)) {
                return null;
            }
            @Nullable LeadershipContestType type = LeadershipContestType.fromSerializedName(tag.getString("contestType"));
            if (type == null) {
                return null;
            }

            @Nullable UUID initiator = null;
            if (tag.contains("initiatorId", Tag.TAG_STRING)) {
                try {
                    initiator = UUID.fromString(tag.getString("initiatorId"));
                } catch (Exception ignored) {
                }
            }

            @Nullable UUID coupLeader = null;
            if (tag.contains("coupLeaderId", Tag.TAG_STRING)) {
                try {
                    coupLeader = UUID.fromString(tag.getString("coupLeaderId"));
                } catch (Exception ignored) {
                }
            }

            LeadershipContestRecord out = new LeadershipContestRecord(
                    type,
                    initiator,
                    coupLeader,
                    tag.getLong("startedAtMillis"),
                    tag.getLong("expiresAtMillis"));

            ListTag candidateTags = tag.getList("candidates", Tag.TAG_STRING);
            for (Tag entry : candidateTags) {
                try {
                    out.candidates.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }

            ListTag voteTags = tag.getList("electionVotes", Tag.TAG_COMPOUND);
            for (Tag entry : voteTags) {
                if (!(entry instanceof CompoundTag voteTag)) {
                    continue;
                }
                if (!voteTag.contains("voterId", Tag.TAG_STRING) || !voteTag.contains("candidateId", Tag.TAG_STRING)) {
                    continue;
                }
                try {
                    UUID voterId = UUID.fromString(voteTag.getString("voterId"));
                    UUID candidateId = UUID.fromString(voteTag.getString("candidateId"));
                    out.electionVotes.put(voterId, candidateId);
                } catch (Exception ignored) {
                }
            }

            ListTag approvalTags = tag.getList("coupApprovals", Tag.TAG_STRING);
            for (Tag entry : approvalTags) {
                try {
                    out.coupApprovals.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }
            return out;
        }
    }

    public record LeadershipContestDecision(boolean changed, String message) {
        public static LeadershipContestDecision changed(String message) {
            return new LeadershipContestDecision(true, message);
        }

        public static LeadershipContestDecision denied(String message) {
            return new LeadershipContestDecision(false, message);
        }
    }

    public record CivRoleView(
            String roleId,
            String displayName,
            Set<String> permissions,
            List<UUID> members) {
    }

    public record DiplomacyView(String otherCivilizationId, DiplomacyState state) {
    }

    public record WarCasualtyView(long yourCasualties, long otherCasualties) {
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

    private record DiplomacyPair(String firstCivId, String secondCivId) {
    }

    private static final class WarCasualtyRecord {
        private long firstCasualties;
        private long secondCasualties;
    }

    public static final class CivilizationRecord {
        private final String id;
        private String displayName;
        private long treasuryCents;
        private final Map<String, Long> hubStock = new HashMap<>();
        private final Map<String, PlotRecord> plots = new HashMap<>();
        private final List<String> auditLogs = new ArrayList<>();
        private final Set<UUID> civicManagers = new HashSet<>();
        private final Set<UUID> explosivesExperts = new HashSet<>();
        private final Set<UUID> redstoners = new HashSet<>();
        private final Set<UUID> joinRequests = new HashSet<>();
        private final Set<UUID> invitedPlayers = new HashSet<>();
        private final Map<String, RoleRecord> customRoles = new HashMap<>();
        @Nullable
        private UUID mayorId;
        @Nullable
        private String hubDimension;
        private int hubX;
        private int hubY;
        private int hubZ;
        private String leaderTitle = DEFAULT_LEADER_TITLE;
        private GovernanceModel governanceModel = GovernanceModel.AUTOCRATIC;
        private HubDistributionMode hubDistributionMode = HubDistributionMode.CONTRIBUTION_RATIO;
        private double hubSharedWithdrawRatio = 0.10D;
        private double upkeepRateMultiplier = 1.0D;
        private TaxPaymentMode taxPaymentMode = TaxPaymentMode.KARMA;
        private String taxItemId = "minecraft:gold_nugget";
        private int taxItemCountPerPlot = 1;
        private final Map<String, Integer> hubDailyAllowances = new HashMap<>();
        @Nullable
        private GovernanceProposalRecord governanceProposal;
        @Nullable
        private LeadershipContestRecord leadershipContest;
        private boolean allowIntraCivPvp;
        private boolean starterTownAreaGranted;

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

        public String leaderTitle() {
            return leaderTitle;
        }

        public void setLeaderTitle(String title) {
            this.leaderTitle = sanitizeLeaderTitle(title);
        }

        public GovernanceModel governanceModel() {
            return governanceModel;
        }

        public void setGovernanceModel(GovernanceModel governanceModel) {
            this.governanceModel = governanceModel == null ? GovernanceModel.AUTOCRATIC : governanceModel;
        }

        public HubDistributionMode hubDistributionMode() {
            return hubDistributionMode;
        }

        public void setHubDistributionMode(HubDistributionMode mode) {
            this.hubDistributionMode = mode == null ? HubDistributionMode.CONTRIBUTION_RATIO : mode;
        }

        public double hubSharedWithdrawRatio() {
            return hubSharedWithdrawRatio;
        }

        public void setHubSharedWithdrawRatio(double ratio) {
            this.hubSharedWithdrawRatio = clampUnitRatio(ratio);
        }

        public double upkeepRateMultiplier() {
            return upkeepRateMultiplier;
        }

        public void setUpkeepRateMultiplier(double multiplier) {
            this.upkeepRateMultiplier = clampUpkeepRateMultiplier(multiplier);
        }

        public TaxPaymentMode taxPaymentMode() {
            return taxPaymentMode;
        }

        public void setTaxPaymentMode(@Nullable TaxPaymentMode mode) {
            this.taxPaymentMode = mode == null ? TaxPaymentMode.KARMA : mode;
        }

        public String taxItemId() {
            return taxItemId;
        }

        public void setTaxItemId(String itemId) {
            if (itemId == null || itemId.isBlank()) {
                this.taxItemId = "minecraft:gold_nugget";
                return;
            }
            this.taxItemId = itemId;
        }

        public int taxItemCountPerPlot() {
            return taxItemCountPerPlot;
        }

        public void setTaxItemCountPerPlot(int countPerPlot) {
            this.taxItemCountPerPlot = Math.max(1, countPerPlot);
        }

        public Map<String, Integer> hubDailyAllowances() {
            return hubDailyAllowances;
        }

        @Nullable
        public GovernanceProposalRecord governanceProposal() {
            return governanceProposal;
        }

        public void setGovernanceProposal(@Nullable GovernanceProposalRecord proposal) {
            this.governanceProposal = proposal;
        }

        @Nullable
        public LeadershipContestRecord leadershipContest() {
            return leadershipContest;
        }

        public void setLeadershipContest(@Nullable LeadershipContestRecord contest) {
            this.leadershipContest = contest;
        }

        public boolean allowIntraCivPvp() {
            return allowIntraCivPvp;
        }

        public void setAllowIntraCivPvp(boolean allowed) {
            this.allowIntraCivPvp = allowed;
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

        public Set<UUID> explosivesExperts() {
            return explosivesExperts;
        }

        public Set<UUID> redstoners() {
            return redstoners;
        }

        public Set<UUID> joinRequests() {
            return joinRequests;
        }

        public Set<UUID> invitedPlayers() {
            return invitedPlayers;
        }

        public Map<String, RoleRecord> customRoles() {
            return customRoles;
        }

        @Nullable
        public UUID mayorId() {
            return mayorId;
        }

        public void setMayorId(@Nullable UUID mayorId) {
            this.mayorId = mayorId;
        }

        @Nullable
        public String hubDimension() {
            return hubDimension;
        }

        public int hubX() {
            return hubX;
        }

        public int hubY() {
            return hubY;
        }

        public int hubZ() {
            return hubZ;
        }

        public void setHubLocation(String dimension, int x, int y, int z) {
            this.hubDimension = dimension;
            this.hubX = x;
            this.hubY = y;
            this.hubZ = z;
        }

        public void clearHubLocation() {
            this.hubDimension = null;
            this.hubX = 0;
            this.hubY = 0;
            this.hubZ = 0;
        }

        public boolean starterTownAreaGranted() {
            return starterTownAreaGranted;
        }

        public void setStarterTownAreaGranted(boolean granted) {
            this.starterTownAreaGranted = granted;
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

            ListTag experts = new ListTag();
            for (UUID expert : explosivesExperts) {
                experts.add(StringTag.valueOf(expert.toString()));
            }
            tag.put("explosivesExperts", experts);

            ListTag redstonersTag = new ListTag();
            for (UUID redstoner : redstoners) {
                redstonersTag.add(StringTag.valueOf(redstoner.toString()));
            }
            tag.put("redstoners", redstonersTag);

            ListTag requests = new ListTag();
            for (UUID request : joinRequests) {
                requests.add(StringTag.valueOf(request.toString()));
            }
            tag.put("joinRequests", requests);

            ListTag invites = new ListTag();
            for (UUID invited : invitedPlayers) {
                invites.add(StringTag.valueOf(invited.toString()));
            }
            tag.put("invitedPlayers", invites);

            if (mayorId != null) {
                tag.putString("mayor", mayorId.toString());
            }
            if (hubDimension != null) {
                tag.putString("hubDimension", hubDimension);
                tag.putInt("hubX", hubX);
                tag.putInt("hubY", hubY);
                tag.putInt("hubZ", hubZ);
            }
            tag.putString("leaderTitle", sanitizeLeaderTitle(leaderTitle));
            tag.putString("governanceModel", governanceModel.serializedName());
            tag.putString("hubDistributionMode", hubDistributionMode.serializedName());
            tag.putDouble("hubSharedWithdrawRatio", clampUnitRatio(hubSharedWithdrawRatio));
            tag.putDouble("upkeepRateMultiplier", clampUpkeepRateMultiplier(upkeepRateMultiplier));
            tag.putString("taxPaymentMode", taxPaymentMode.serializedName());
            tag.putString("taxItemId", taxItemId);
            tag.putInt("taxItemCountPerPlot", Math.max(1, taxItemCountPerPlot));
            CompoundTag dailyAllowanceTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : hubDailyAllowances.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    dailyAllowanceTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("hubDailyAllowances", dailyAllowanceTag);
            if (governanceProposal != null) {
                tag.put("governanceProposal", governanceProposal.save());
            }
            if (leadershipContest != null) {
                tag.put("leadershipContest", leadershipContest.save());
            }

            ListTag roleTags = new ListTag();
            for (RoleRecord role : customRoles.values()) {
                roleTags.add(role.save());
            }
            tag.put("customRoles", roleTags);
            tag.putBoolean("allowIntraCivPvp", allowIntraCivPvp);
            tag.putBoolean("starterTownAreaGranted", starterTownAreaGranted);
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

            ListTag experts = tag.getList("explosivesExperts", Tag.TAG_STRING);
            for (Tag entry : experts) {
                try {
                    record.explosivesExperts.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }

            ListTag redstonersTag = tag.getList("redstoners", Tag.TAG_STRING);
            for (Tag entry : redstonersTag) {
                try {
                    record.redstoners.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }

            ListTag requests = tag.getList("joinRequests", Tag.TAG_STRING);
            for (Tag entry : requests) {
                try {
                    record.joinRequests.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }

            ListTag invites = tag.getList("invitedPlayers", Tag.TAG_STRING);
            for (Tag entry : invites) {
                try {
                    record.invitedPlayers.add(UUID.fromString(entry.getAsString()));
                } catch (Exception ignored) {
                }
            }

            if (tag.contains("mayor")) {
                try {
                    record.mayorId = UUID.fromString(tag.getString("mayor"));
                } catch (Exception ignored) {
                }
            }
            if (tag.contains("hubDimension")) {
                record.hubDimension = tag.getString("hubDimension");
                record.hubX = tag.getInt("hubX");
                record.hubY = tag.getInt("hubY");
                record.hubZ = tag.getInt("hubZ");
            }
            if (tag.contains("leaderTitle")) {
                record.leaderTitle = sanitizeLeaderTitle(tag.getString("leaderTitle"));
            }
            if (tag.contains("governanceModel")) {
                @Nullable GovernanceModel parsed = GovernanceModel.fromSerializedName(tag.getString("governanceModel"));
                if (parsed != null) {
                    record.governanceModel = parsed;
                }
            }
            if (tag.contains("hubDistributionMode")) {
                @Nullable HubDistributionMode parsed = HubDistributionMode.fromSerializedName(tag.getString("hubDistributionMode"));
                if (parsed != null) {
                    record.hubDistributionMode = parsed;
                }
            }
            if (tag.contains("hubSharedWithdrawRatio")) {
                record.hubSharedWithdrawRatio = clampUnitRatio(tag.getDouble("hubSharedWithdrawRatio"));
            }
            if (tag.contains("upkeepRateMultiplier")) {
                record.upkeepRateMultiplier = clampUpkeepRateMultiplier(tag.getDouble("upkeepRateMultiplier"));
            }
            if (tag.contains("taxPaymentMode", Tag.TAG_STRING)) {
                @Nullable TaxPaymentMode parsed = TaxPaymentMode.fromSerializedName(tag.getString("taxPaymentMode"));
                if (parsed != null) {
                    record.taxPaymentMode = parsed;
                }
            }
            if (tag.contains("taxItemId", Tag.TAG_STRING)) {
                record.taxItemId = tag.getString("taxItemId");
            }
            if (tag.contains("taxItemCountPerPlot")) {
                record.taxItemCountPerPlot = Math.max(1, tag.getInt("taxItemCountPerPlot"));
            }
            CompoundTag dailyAllowanceTag = tag.getCompound("hubDailyAllowances");
            for (String key : dailyAllowanceTag.getAllKeys()) {
                int value = Math.max(0, dailyAllowanceTag.getInt(key));
                if (value > 0) {
                    record.hubDailyAllowances.put(key, value);
                }
            }
            if (tag.contains("governanceProposal", Tag.TAG_COMPOUND)) {
                @Nullable GovernanceProposalRecord proposal = GovernanceProposalRecord.load(tag.getCompound("governanceProposal"));
                if (proposal != null) {
                    record.governanceProposal = proposal;
                }
            }
            if (tag.contains("leadershipContest", Tag.TAG_COMPOUND)) {
                @Nullable LeadershipContestRecord contest = LeadershipContestRecord.load(tag.getCompound("leadershipContest"));
                if (contest != null) {
                    record.leadershipContest = contest;
                }
            }
            ListTag roleTags = tag.getList("customRoles", Tag.TAG_COMPOUND);
            for (Tag entry : roleTags) {
                if (!(entry instanceof CompoundTag roleTag)) {
                    continue;
                }
                @Nullable RoleRecord role = RoleRecord.load(roleTag);
                if (role == null) {
                    continue;
                }
                record.customRoles.put(role.roleId(), role);
            }
            record.allowIntraCivPvp = tag.getBoolean("allowIntraCivPvp");
            if (tag.contains("starterTownAreaGranted")) {
                record.starterTownAreaGranted = tag.getBoolean("starterTownAreaGranted");
            } else if (record.hubDimension != null) {
                for (PlotRecord plot : record.plots.values()) {
                    if (plot.landClass() == LandClass.CIVIC) {
                        record.starterTownAreaGranted = true;
                        break;
                    }
                }
            }
            return record;
        }

        private static final class RoleRecord {
            private final String roleId;
            private String displayName;
            private final Set<String> permissions = new HashSet<>();
            private final Set<UUID> members = new HashSet<>();

            private RoleRecord(String roleId, String displayName) {
                this.roleId = roleId;
                this.displayName = displayName;
            }

            private String roleId() {
                return roleId;
            }

            private String displayName() {
                return displayName;
            }

            private void setDisplayName(String displayName) {
                this.displayName = displayName;
            }

            private Set<String> permissions() {
                return permissions;
            }

            private Set<UUID> members() {
                return members;
            }

            private CompoundTag save() {
                CompoundTag tag = new CompoundTag();
                tag.putString("roleId", roleId);
                tag.putString("displayName", displayName);
                ListTag permissionTags = new ListTag();
                for (String permission : permissions) {
                    permissionTags.add(StringTag.valueOf(permission));
                }
                tag.put("permissions", permissionTags);
                ListTag memberTags = new ListTag();
                for (UUID memberId : members) {
                    memberTags.add(StringTag.valueOf(memberId.toString()));
                }
                tag.put("members", memberTags);
                return tag;
            }

            @Nullable
            private static RoleRecord load(CompoundTag tag) {
                @Nullable String roleId = normalizeRoleId(tag.getString("roleId"));
                if (roleId == null) {
                    return null;
                }
                String displayName = sanitizeRoleDisplayName(tag.contains("displayName")
                        ? tag.getString("displayName")
                        : roleId);
                RoleRecord record = new RoleRecord(roleId, displayName);

                ListTag permissionTags = tag.getList("permissions", Tag.TAG_STRING);
                for (Tag permissionTag : permissionTags) {
                    @Nullable String permission = normalizePermissionKey(permissionTag.getAsString());
                    if (permission != null) {
                        record.permissions.add(permission);
                    }
                }

                ListTag memberTags = tag.getList("members", Tag.TAG_STRING);
                for (Tag memberTag : memberTags) {
                    try {
                        record.members.add(UUID.fromString(memberTag.getAsString()));
                    } catch (Exception ignored) {
                    }
                }

                return record;
            }
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

                LandClass landClass = LandClass.COMMUNITY;
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
        private static final long DAY_MILLIS = 86_400_000L;
        private long socialCreditCents;
        private int farmerActions;
        private int minerActions;
        private long farmerActionsUpdatedAtMillis;
        private long minerActionsUpdatedAtMillis;
        private int farmerXp;
        private int minerXp;
        private final Map<String, Integer> minerBlockActions = new HashMap<>();
        private int terraformerActions;
        private long terraformerActionsUpdatedAtMillis;
        private int terraformerXp;
        private int lumberjackActions;
        private long lumberjackActionsUpdatedAtMillis;
        private int lumberjackXp;
        private int fisherActions;
        private long fisherActionsUpdatedAtMillis;
        private int fisherXp;
        private int hunterActions;
        private long hunterActionsUpdatedAtMillis;
        private int hunterXp;
        private int warriorActions;
        private long warriorActionsUpdatedAtMillis;
        private int warriorXp;
        private int pendingWarriorHubRegistrations;
        private int explosivesExpertActions;
        private long explosivesExpertActionsUpdatedAtMillis;
        private int explosivesExpertXp;
        private int crafterActions;
        private long crafterActionsUpdatedAtMillis;
        private int crafterXp;
        private int enchanterActions;
        private long enchanterActionsUpdatedAtMillis;
        private int enchanterXp;
        private int brewerActions;
        private long brewerActionsUpdatedAtMillis;
        private int brewerXp;
        private int traderActions;
        private long traderActionsUpdatedAtMillis;
        private int traderXp;
        private int shepherdActions;
        private long shepherdActionsUpdatedAtMillis;
        private int shepherdXp;
        private int explorerActions;
        private long explorerActionsUpdatedAtMillis;
        private int explorerXp;
        private int treasureHunterActions;
        private long treasureHunterActionsUpdatedAtMillis;
        private int treasureHunterXp;
        private int breederActions;
        private long breederActionsUpdatedAtMillis;
        private int breederXp;
        private int smithyActions;
        private long smithyActionsUpdatedAtMillis;
        private int smithyXp;
        private int smelterActions;
        private long smelterActionsUpdatedAtMillis;
        private int smelterXp;
        private int generalXp;
        private long firstSeenAtMillis;
        private final Map<String, HookWindowUsage> hookWindowUsage = new HashMap<>();
        private final Map<String, Integer> hunterMobActions = new HashMap<>();
        private long professionDailyActionDayIndex = -1L;
        private final Map<String, Integer> professionDailyActions = new HashMap<>();
        private long minerDailyBlockActionDayIndex = -1L;
        private final Map<String, Integer> minerDailyBlockActions = new HashMap<>();
        private final Map<String, Integer> lumberjackBlockActions = new HashMap<>();
        private long lumberjackDailyBlockActionDayIndex = -1L;
        private final Map<String, Integer> lumberjackDailyBlockActions = new HashMap<>();
        private final Map<String, Integer> terraformerBlockActions = new HashMap<>();
        private long terraformerDailyBlockActionDayIndex = -1L;
        private final Map<String, Integer> terraformerDailyBlockActions = new HashMap<>();
        private long professionProgressDayIndex = -1L;
        private int generalLevelsGainedToday;
        private final Map<String, Integer> professionLevelsGainedToday = new HashMap<>();
        private long contributionGainDayIndex = -1L;
        private final Map<String, Long> contributionGainTodayCents = new HashMap<>();
        @Nullable
        private Profession focusedProfession;
        private final Map<String, CivAccount> civAccounts = new HashMap<>();

        // Legacy fields retained for migration.
        private final Map<String, Long> contributions = new HashMap<>();
        private final Map<String, Long> personalWithdrawals = new HashMap<>();
        @Nullable
        private Double personalWithdrawRatioOverride;
        @Nullable
        private String ftbClaimModeOverride;

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
            if (target.dailyAllowanceDayIndex == fromAccount.dailyAllowanceDayIndex) {
                for (Map.Entry<String, Long> entry : fromAccount.dailyAllowanceWithdrawals.entrySet()) {
                    long value = Math.max(0L, entry.getValue());
                    if (value > 0L) {
                        target.dailyAllowanceWithdrawals.merge(entry.getKey(), value, Long::sum);
                    }
                }
            } else if (fromAccount.dailyAllowanceDayIndex > target.dailyAllowanceDayIndex) {
                target.dailyAllowanceDayIndex = fromAccount.dailyAllowanceDayIndex;
                target.dailyAllowanceWithdrawals.clear();
                for (Map.Entry<String, Long> entry : fromAccount.dailyAllowanceWithdrawals.entrySet()) {
                    long value = Math.max(0L, entry.getValue());
                    if (value > 0L) {
                        target.dailyAllowanceWithdrawals.put(entry.getKey(), value);
                    }
                }
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

        public long addSocialCreditCents(String civId, long delta) {
            if (delta == 0L) {
                return 0L;
            }
            if (delta < 0L) {
                long before = socialCreditCents(civId);
                setSocialCreditCents(civId, socialCreditCents(civId) + delta);
                long after = socialCreditCents(civId);
                return after - before;
            }
            long allowed = clampContributionGainForToday(civId, delta);
            if (allowed <= 0L) {
                return 0L;
            }
            long before = socialCreditCents(civId);
            setSocialCreditCents(civId, socialCreditCents(civId) + allowed);
            long after = socialCreditCents(civId);
            return after - before;
        }

        public int farmerActions() {
            return farmerActions;
        }

        public void setFarmerActions(int value) {
            int updated = Math.max(0, value);
            if (this.farmerActions != updated) {
                this.farmerActions = updated;
                this.farmerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public int minerActions() {
            return minerActions;
        }

        public void setMinerActions(int value) {
            int updated = Math.max(0, value);
            if (this.minerActions != updated) {
                if (updated <= 0) {
                    minerBlockActions.clear();
                }
                this.minerActions = updated;
                this.minerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public void clearMinerBlockWindow() {
            minerBlockActions.clear();
        }

        public int minerBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            return Math.max(0, minerBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addMinerBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            String key = blockId.toString();
            int current = Math.max(0, minerBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            minerBlockActions.put(key, (int) next);
        }

        public int minerDailyBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            resetMinerDailyBlockActionWindowIfNeeded();
            return Math.max(0, minerDailyBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addMinerDailyBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            resetMinerDailyBlockActionWindowIfNeeded();
            String key = blockId.toString();
            int current = Math.max(0, minerDailyBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            minerDailyBlockActions.put(key, (int) next);
        }

        public void clearLumberjackBlockWindow() {
            lumberjackBlockActions.clear();
        }

        public int lumberjackBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            return Math.max(0, lumberjackBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addLumberjackBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            String key = blockId.toString();
            int current = Math.max(0, lumberjackBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            lumberjackBlockActions.put(key, (int) next);
        }

        public int lumberjackDailyBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            resetLumberjackDailyBlockActionWindowIfNeeded();
            return Math.max(0, lumberjackDailyBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addLumberjackDailyBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            resetLumberjackDailyBlockActionWindowIfNeeded();
            String key = blockId.toString();
            int current = Math.max(0, lumberjackDailyBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            lumberjackDailyBlockActions.put(key, (int) next);
        }

        public void clearTerraformerBlockWindow() {
            terraformerBlockActions.clear();
        }

        public int terraformerBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            return Math.max(0, terraformerBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addTerraformerBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            String key = blockId.toString();
            int current = Math.max(0, terraformerBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            terraformerBlockActions.put(key, (int) next);
        }

        public int terraformerDailyBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            resetTerraformerDailyBlockActionWindowIfNeeded();
            return Math.max(0, terraformerDailyBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addTerraformerDailyBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            resetTerraformerDailyBlockActionWindowIfNeeded();
            String key = blockId.toString();
            int current = Math.max(0, terraformerDailyBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            terraformerDailyBlockActions.put(key, (int) next);
        }

        public int dailyProfessionActionsUsed(Profession profession) {
            if (profession == null || profession == Profession.NONE) {
                return 0;
            }
            resetDailyProfessionActionWindowIfNeeded();
            return Math.max(0, professionDailyActions.getOrDefault(profession.name(), 0));
        }

        public void addDailyProfessionActions(Profession profession, int delta) {
            if (profession == null || profession == Profession.NONE || delta <= 0) {
                return;
            }
            resetDailyProfessionActionWindowIfNeeded();
            String key = profession.name();
            int current = Math.max(0, professionDailyActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            professionDailyActions.put(key, (int) next);
        }

        public long farmerActionsUpdatedAtMillis() {
            return farmerActionsUpdatedAtMillis;
        }

        public long minerActionsUpdatedAtMillis() {
            return minerActionsUpdatedAtMillis;
        }

        public int farmerXp() {
            return farmerXp;
        }

        public int minerXp() {
            return minerXp;
        }

        public int terraformerActions() {
            return terraformerActions;
        }

        public void setTerraformerActions(int value) {
            int updated = Math.max(0, value);
            if (this.terraformerActions != updated) {
                this.terraformerActions = updated;
                this.terraformerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long terraformerActionsUpdatedAtMillis() {
            return terraformerActionsUpdatedAtMillis;
        }

        public int terraformerXp() {
            return terraformerXp;
        }

        public int lumberjackActions() {
            return lumberjackActions;
        }

        public void setLumberjackActions(int value) {
            int updated = Math.max(0, value);
            if (this.lumberjackActions != updated) {
                this.lumberjackActions = updated;
                this.lumberjackActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long lumberjackActionsUpdatedAtMillis() {
            return lumberjackActionsUpdatedAtMillis;
        }

        public int lumberjackXp() {
            return lumberjackXp;
        }

        public int fisherActions() {
            return fisherActions;
        }

        public void setFisherActions(int value) {
            int updated = Math.max(0, value);
            if (this.fisherActions != updated) {
                this.fisherActions = updated;
                this.fisherActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long fisherActionsUpdatedAtMillis() {
            return fisherActionsUpdatedAtMillis;
        }

        public int fisherXp() {
            return fisherXp;
        }

        public int hunterActions() {
            return hunterActions;
        }

        public void setHunterActions(int value) {
            int updated = Math.max(0, value);
            if (this.hunterActions != updated) {
                if (updated < this.hunterActions) {
                    trimHunterMobActions(this.hunterActions - updated);
                }
                this.hunterActions = updated;
                this.hunterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long hunterActionsUpdatedAtMillis() {
            return hunterActionsUpdatedAtMillis;
        }

        public int hunterXp() {
            return hunterXp;
        }

        public int hunterMobActions(ResourceLocation entityId) {
            if (entityId == null) {
                return 0;
            }
            return Math.max(0, hunterMobActions.getOrDefault(entityId.toString(), 0));
        }

        public void addHunterMobActions(ResourceLocation entityId, int delta) {
            if (entityId == null || delta <= 0) {
                return;
            }
            String key = entityId.toString();
            int current = Math.max(0, hunterMobActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            hunterMobActions.put(key, (int) next);
        }

        public void restoreHunterActions(int restoredActions) {
            int refund = Math.min(Math.max(0, restoredActions), hunterActions);
            if (refund <= 0) {
                return;
            }
            setHunterActions(hunterActions - refund);
        }

        private void trimHunterMobActions(int reduction) {
            if (reduction <= 0 || hunterMobActions.isEmpty()) {
                return;
            }
            // Keep per-mob counters in sync with the generic hunter action pool.
            int remaining = reduction;
            List<String> keys = new ArrayList<>(hunterMobActions.keySet());
            keys.sort(String::compareTo);
            for (String key : keys) {
                if (remaining <= 0) {
                    break;
                }
                int used = Math.max(0, hunterMobActions.getOrDefault(key, 0));
                if (used <= 0) {
                    hunterMobActions.remove(key);
                    continue;
                }
                int reduce = Math.min(used, remaining);
                int next = used - reduce;
                if (next <= 0) {
                    hunterMobActions.remove(key);
                } else {
                    hunterMobActions.put(key, next);
                }
                remaining -= reduce;
            }
        }

        public int warriorActions() {
            return warriorActions;
        }

        public void setWarriorActions(int value) {
            int updated = Math.max(0, value);
            if (this.warriorActions != updated) {
                this.warriorActions = updated;
                this.warriorActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long warriorActionsUpdatedAtMillis() {
            return warriorActionsUpdatedAtMillis;
        }

        public int warriorXp() {
            return warriorXp;
        }

        public int pendingWarriorHubRegistrations() {
            return Math.max(0, pendingWarriorHubRegistrations);
        }

        public void addPendingWarriorHubRegistrations(int delta) {
            if (delta <= 0) {
                return;
            }
            pendingWarriorHubRegistrations = Math.max(0, pendingWarriorHubRegistrations + delta);
        }

        public void clearPendingWarriorHubRegistrations() {
            pendingWarriorHubRegistrations = 0;
        }

        public void addWarriorXp(int delta) {
            addProfessionXp(Profession.WARRIOR, delta);
        }

        public int explosivesExpertActions() {
            return explosivesExpertActions;
        }

        public void setExplosivesExpertActions(int value) {
            int updated = Math.max(0, value);
            if (this.explosivesExpertActions != updated) {
                this.explosivesExpertActions = updated;
                this.explosivesExpertActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long explosivesExpertActionsUpdatedAtMillis() {
            return explosivesExpertActionsUpdatedAtMillis;
        }

        public int explosivesExpertXp() {
            return explosivesExpertXp;
        }

        public void addExplosivesExpertXp(int delta) {
            addProfessionXp(Profession.EXPLOSIVES_EXPERT, delta);
        }

        public int crafterActions() {
            return crafterActions;
        }

        public void setCrafterActions(int value) {
            int updated = Math.max(0, value);
            if (this.crafterActions != updated) {
                this.crafterActions = updated;
                this.crafterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long crafterActionsUpdatedAtMillis() {
            return crafterActionsUpdatedAtMillis;
        }

        public int crafterXp() {
            return crafterXp;
        }

        public int enchanterActions() {
            return enchanterActions;
        }

        public void setEnchanterActions(int value) {
            int updated = Math.max(0, value);
            if (this.enchanterActions != updated) {
                this.enchanterActions = updated;
                this.enchanterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long enchanterActionsUpdatedAtMillis() {
            return enchanterActionsUpdatedAtMillis;
        }

        public int enchanterXp() {
            return enchanterXp;
        }

        public int brewerActions() {
            return brewerActions;
        }

        public void setBrewerActions(int value) {
            int updated = Math.max(0, value);
            if (this.brewerActions != updated) {
                this.brewerActions = updated;
                this.brewerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long brewerActionsUpdatedAtMillis() {
            return brewerActionsUpdatedAtMillis;
        }

        public int brewerXp() {
            return brewerXp;
        }

        public int traderActions() {
            return traderActions;
        }

        public void setTraderActions(int value) {
            int updated = Math.max(0, value);
            if (this.traderActions != updated) {
                this.traderActions = updated;
                this.traderActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long traderActionsUpdatedAtMillis() {
            return traderActionsUpdatedAtMillis;
        }

        public int traderXp() {
            return traderXp;
        }

        public int shepherdActions() {
            return shepherdActions;
        }

        public void setShepherdActions(int value) {
            int updated = Math.max(0, value);
            if (this.shepherdActions != updated) {
                this.shepherdActions = updated;
                this.shepherdActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long shepherdActionsUpdatedAtMillis() {
            return shepherdActionsUpdatedAtMillis;
        }

        public int shepherdXp() {
            return shepherdXp;
        }

        public int explorerActions() {
            return explorerActions;
        }

        public void setExplorerActions(int value) {
            int updated = Math.max(0, value);
            if (this.explorerActions != updated) {
                this.explorerActions = updated;
                this.explorerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long explorerActionsUpdatedAtMillis() {
            return explorerActionsUpdatedAtMillis;
        }

        public int explorerXp() {
            return explorerXp;
        }

        public int treasureHunterActions() {
            return treasureHunterActions;
        }

        public void setTreasureHunterActions(int value) {
            int updated = Math.max(0, value);
            if (this.treasureHunterActions != updated) {
                this.treasureHunterActions = updated;
                this.treasureHunterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long treasureHunterActionsUpdatedAtMillis() {
            return treasureHunterActionsUpdatedAtMillis;
        }

        public int treasureHunterXp() {
            return treasureHunterXp;
        }

        public int breederActions() {
            return breederActions;
        }

        public void setBreederActions(int value) {
            int updated = Math.max(0, value);
            if (this.breederActions != updated) {
                this.breederActions = updated;
                this.breederActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long breederActionsUpdatedAtMillis() {
            return breederActionsUpdatedAtMillis;
        }

        public int breederXp() {
            return breederXp;
        }

        public int smithyActions() {
            return smithyActions;
        }

        public void setSmithyActions(int value) {
            int updated = Math.max(0, value);
            if (this.smithyActions != updated) {
                this.smithyActions = updated;
                this.smithyActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long smithyActionsUpdatedAtMillis() {
            return smithyActionsUpdatedAtMillis;
        }

        public int smithyXp() {
            return smithyXp;
        }

        public int smelterActions() {
            return smelterActions;
        }

        public void setSmelterActions(int value) {
            int updated = Math.max(0, value);
            if (this.smelterActions != updated) {
                this.smelterActions = updated;
                this.smelterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long smelterActionsUpdatedAtMillis() {
            return smelterActionsUpdatedAtMillis;
        }

        public int smelterXp() {
            return smelterXp;
        }

        public int actionsForProfession(Profession profession) {
            if (profession == null) {
                return 0;
            }
            return switch (profession) {
                case FARMER -> farmerActions;
                case MINER -> minerActions;
                case TERRAFORMER -> terraformerActions;
                case LUMBERJACK -> lumberjackActions;
                case FISHER -> fisherActions;
                case HUNTER -> hunterActions;
                case WARRIOR -> warriorActions;
                case EXPLOSIVES_EXPERT -> explosivesExpertActions;
                case CRAFTER -> crafterActions;
                case ENCHANTER -> enchanterActions;
                case BREWER -> brewerActions;
                case TRADER -> traderActions;
                case SHEPHERD -> shepherdActions;
                case EXPLORER -> explorerActions;
                case TREASURE_HUNTER -> treasureHunterActions;
                case BREEDER -> breederActions;
                case SMITHY -> smithyActions;
                case SMELTER -> smelterActions;
                case NONE -> 0;
            };
        }

        public void setActionsForProfession(Profession profession, int value) {
            if (profession == null) {
                return;
            }
            switch (profession) {
                case FARMER -> setFarmerActions(value);
                case MINER -> setMinerActions(value);
                case TERRAFORMER -> setTerraformerActions(value);
                case LUMBERJACK -> setLumberjackActions(value);
                case FISHER -> setFisherActions(value);
                case HUNTER -> setHunterActions(value);
                case WARRIOR -> setWarriorActions(value);
                case EXPLOSIVES_EXPERT -> setExplosivesExpertActions(value);
                case CRAFTER -> setCrafterActions(value);
                case ENCHANTER -> setEnchanterActions(value);
                case BREWER -> setBrewerActions(value);
                case TRADER -> setTraderActions(value);
                case SHEPHERD -> setShepherdActions(value);
                case EXPLORER -> setExplorerActions(value);
                case TREASURE_HUNTER -> setTreasureHunterActions(value);
                case BREEDER -> setBreederActions(value);
                case SMITHY -> setSmithyActions(value);
                case SMELTER -> setSmelterActions(value);
                case NONE -> {
                }
            }
        }

        public long actionsUpdatedAtMillisForProfession(Profession profession) {
            if (profession == null) {
                return 0L;
            }
            return switch (profession) {
                case FARMER -> farmerActionsUpdatedAtMillis;
                case MINER -> minerActionsUpdatedAtMillis;
                case TERRAFORMER -> terraformerActionsUpdatedAtMillis;
                case LUMBERJACK -> lumberjackActionsUpdatedAtMillis;
                case FISHER -> fisherActionsUpdatedAtMillis;
                case HUNTER -> hunterActionsUpdatedAtMillis;
                case WARRIOR -> warriorActionsUpdatedAtMillis;
                case EXPLOSIVES_EXPERT -> explosivesExpertActionsUpdatedAtMillis;
                case CRAFTER -> crafterActionsUpdatedAtMillis;
                case ENCHANTER -> enchanterActionsUpdatedAtMillis;
                case BREWER -> brewerActionsUpdatedAtMillis;
                case TRADER -> traderActionsUpdatedAtMillis;
                case SHEPHERD -> shepherdActionsUpdatedAtMillis;
                case EXPLORER -> explorerActionsUpdatedAtMillis;
                case TREASURE_HUNTER -> treasureHunterActionsUpdatedAtMillis;
                case BREEDER -> breederActionsUpdatedAtMillis;
                case SMITHY -> smithyActionsUpdatedAtMillis;
                case SMELTER -> smelterActionsUpdatedAtMillis;
                case NONE -> 0L;
            };
        }

        public int generalXp() {
            return generalXp;
        }

        public void ensureFirstSeenAtMillis(long nowMillis) {
            if (firstSeenAtMillis > 0L) {
                return;
            }
            if (nowMillis <= 0L) {
                return;
            }
            firstSeenAtMillis = nowMillis;
        }

        public long firstSeenAtMillis() {
            return firstSeenAtMillis;
        }

        public long membershipMillis(long nowMillis) {
            if (nowMillis <= 0L || firstSeenAtMillis <= 0L || nowMillis < firstSeenAtMillis) {
                return 0L;
            }
            return nowMillis - firstSeenAtMillis;
        }

        public int hookWindowUsed(String key, long windowStartMillis) {
            if (key == null || key.isBlank()) {
                return 0;
            }
            HookWindowUsage usage = hookWindowUsage.get(key);
            if (usage == null || usage.windowStartMillis != windowStartMillis) {
                return 0;
            }
            return Math.max(0, usage.usedTriggers);
        }

        public void setHookWindowUsed(String key, long windowStartMillis, int usedTriggers) {
            if (key == null || key.isBlank()) {
                return;
            }
            int safeUsed = Math.max(0, usedTriggers);
            if (safeUsed <= 0) {
                hookWindowUsage.remove(key);
                return;
            }
            HookWindowUsage usage = hookWindowUsage.computeIfAbsent(key, ignored -> new HookWindowUsage());
            usage.windowStartMillis = Math.max(0L, windowStartMillis);
            usage.usedTriggers = safeUsed;
        }

        @Nullable
        public Profession focusedProfession() {
            return focusedProfession;
        }

        public void setFocusedProfession(@Nullable Profession profession) {
            if (profession == Profession.NONE) {
                profession = null;
            }
            this.focusedProfession = profession;
        }

        public boolean canProgressProfession(Profession profession) {
            if (profession == Profession.NONE || !RealCivConfig.specializationSingleProfessionLockEnabled()) {
                return true;
            }
            return focusedProfession != null && focusedProfession == profession;
        }

        public void addProfessionXp(Profession profession, int delta) {
            if (profession == null || profession == Profession.NONE || delta <= 0) {
                return;
            }
            if (RealCivConfig.specializationSingleProfessionLockEnabled()
                    && (focusedProfession == null || focusedProfession != profession)) {
                return;
            }
            int startXp = professionXpValue(profession);
            int startLevel = RealCivConfig.professionLevelFromXp(profession, startXp);
            int maxLevelCap = RealCivConfig.professionLevelCap(profession);
            if (startLevel >= maxLevelCap) {
                return;
            }

            int cappedTargetXp = capProfessionXpByDailyLevelGain(profession, startXp, delta, startLevel, maxLevelCap);
            int appliedDelta = Math.max(0, cappedTargetXp - startXp);
            if (appliedDelta <= 0) {
                return;
            }

            setProfessionXpValue(profession, cappedTargetXp);
            applyProfessionXpDecay(profession, appliedDelta);
            int endLevel = RealCivConfig.professionLevelFromXp(profession, professionXpValue(profession));
            recordProfessionLevelGain(profession, Math.max(0, endLevel - startLevel));
        }

        public void addGeneralXp(int delta) {
            if (delta <= 0) {
                return;
            }
            int startXp = generalXp;
            int startLevel = RealCivConfig.generalLevelFromXp(startXp);
            int targetXp = capGeneralXpByDailyLevelGain(startXp, delta, startLevel);
            if (targetXp <= startXp) {
                return;
            }
            generalXp = Math.max(0, targetXp);
            int endLevel = RealCivConfig.generalLevelFromXp(generalXp);
            recordGeneralLevelGain(Math.max(0, endLevel - startLevel));
        }

        private int capProfessionXpByDailyLevelGain(
                Profession profession,
                int startXp,
                int delta,
                int startLevel,
                int maxLevelCap) {
            int targetXp = safeAddPositive(startXp, delta);
            int absoluteMaxXp = maxXpForProfessionLevel(maxLevelCap);
            if (targetXp > absoluteMaxXp) {
                targetXp = absoluteMaxXp;
            }

            int maxDailyLevels = RealCivConfig.maxProfessionLevelGainsPerDay();
            if (maxDailyLevels <= 0) {
                return targetXp;
            }

            rotateProfessionProgressDay();
            String key = profession.name();
            int used = Math.max(0, professionLevelsGainedToday.getOrDefault(key, 0));
            int remaining = Math.max(0, maxDailyLevels - used);
            if (remaining <= 0) {
                return startXp;
            }

            int allowedLevel = Math.min(maxLevelCap, safeAddPositive(startLevel, remaining));
            int maxByDailyLimit = maxXpForProfessionLevel(allowedLevel);
            return Math.min(targetXp, maxByDailyLimit);
        }

        private int capGeneralXpByDailyLevelGain(int startXp, int delta, int startLevel) {
            int targetXp = safeAddPositive(startXp, delta);
            int maxDailyLevels = RealCivConfig.maxGeneralLevelGainsPerDay();
            if (maxDailyLevels <= 0) {
                return targetXp;
            }
            rotateProfessionProgressDay();
            int used = Math.max(0, generalLevelsGainedToday);
            int remaining = Math.max(0, maxDailyLevels - used);
            if (remaining <= 0) {
                return startXp;
            }
            int allowedLevel = safeAddPositive(startLevel, remaining);
            int maxByDailyLimit = maxXpForGeneralLevel(allowedLevel);
            return Math.min(targetXp, maxByDailyLimit);
        }

        private long clampContributionGainForToday(String civIdRaw, long delta) {
            if (delta <= 0L) {
                return 0L;
            }
            long dailyCap = RealCivConfig.maxContributionKarmaGainPerDayCents();
            if (dailyCap <= 0L) {
                return delta;
            }
            rotateContributionGainDay();
            @Nullable String normalized = CivSavedData.normalizeCivId(civIdRaw);
            String civId = normalized == null ? RealCivConfig.defaultCivilizationId() : normalized;
            long used = Math.max(0L, contributionGainTodayCents.getOrDefault(civId, 0L));
            long remaining = Math.max(0L, dailyCap - used);
            if (remaining <= 0L) {
                return 0L;
            }
            long allowed = Math.min(delta, remaining);
            if (allowed <= 0L) {
                return 0L;
            }
            contributionGainTodayCents.put(civId, used + allowed);
            return allowed;
        }

        private void recordProfessionLevelGain(Profession profession, int gained) {
            if (gained <= 0 || profession == null || profession == Profession.NONE) {
                return;
            }
            rotateProfessionProgressDay();
            String key = profession.name();
            int used = Math.max(0, professionLevelsGainedToday.getOrDefault(key, 0));
            professionLevelsGainedToday.put(key, safeAddPositive(used, gained));
        }

        private void recordGeneralLevelGain(int gained) {
            if (gained <= 0) {
                return;
            }
            rotateProfessionProgressDay();
            generalLevelsGainedToday = safeAddPositive(generalLevelsGainedToday, gained);
        }

        private void rotateProfessionProgressDay() {
            long nowDay = currentDayIndex();
            if (professionProgressDayIndex == nowDay) {
                return;
            }
            professionProgressDayIndex = nowDay;
            generalLevelsGainedToday = 0;
            professionLevelsGainedToday.clear();
        }

        private void rotateContributionGainDay() {
            long nowDay = currentDayIndex();
            if (contributionGainDayIndex == nowDay) {
                return;
            }
            contributionGainDayIndex = nowDay;
            contributionGainTodayCents.clear();
        }

        private void resetDailyProfessionActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (professionDailyActionDayIndex == dayIndex) {
                return;
            }
            professionDailyActionDayIndex = dayIndex;
            professionDailyActions.clear();
        }

        private void resetMinerDailyBlockActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (minerDailyBlockActionDayIndex == dayIndex) {
                return;
            }
            minerDailyBlockActionDayIndex = dayIndex;
            minerDailyBlockActions.clear();
        }

        private void resetLumberjackDailyBlockActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (lumberjackDailyBlockActionDayIndex == dayIndex) {
                return;
            }
            lumberjackDailyBlockActionDayIndex = dayIndex;
            lumberjackDailyBlockActions.clear();
        }

        private void resetTerraformerDailyBlockActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (terraformerDailyBlockActionDayIndex == dayIndex) {
                return;
            }
            terraformerDailyBlockActionDayIndex = dayIndex;
            terraformerDailyBlockActions.clear();
        }

        private int maxXpForProfessionLevel(int level) {
            if (level < 0) {
                return 0;
            }
            List<? extends Integer> thresholds = RealCivConfig.PROFESSION_XP_THRESHOLDS.get();
            if (thresholds.isEmpty()) {
                return Integer.MAX_VALUE;
            }
            int nextIndex = safeAddPositive(level, 1);
            if (nextIndex >= thresholds.size()) {
                return Integer.MAX_VALUE;
            }
            Integer next = thresholds.get(nextIndex);
            if (next == null) {
                return Integer.MAX_VALUE;
            }
            int nextThreshold = Math.max(0, next);
            if (nextThreshold <= 0) {
                return 0;
            }
            return nextThreshold - 1;
        }

        private int maxXpForGeneralLevel(int level) {
            if (level < 0) {
                return 0;
            }
            List<? extends Integer> thresholds = RealCivConfig.GENERAL_XP_THRESHOLDS.get();
            if (thresholds.isEmpty()) {
                return Integer.MAX_VALUE;
            }
            int nextIndex = safeAddPositive(level, 1);
            if (nextIndex >= thresholds.size()) {
                return Integer.MAX_VALUE;
            }
            Integer next = thresholds.get(nextIndex);
            if (next == null) {
                return Integer.MAX_VALUE;
            }
            int nextThreshold = Math.max(0, next);
            if (nextThreshold <= 0) {
                return 0;
            }
            return nextThreshold - 1;
        }

        private static long currentDayIndex() {
            return Math.floorDiv(System.currentTimeMillis(), DAY_MILLIS);
        }

        private static int safeAddPositive(int left, int right) {
            long value = (long) left + (long) right;
            if (value <= 0L) {
                return 0;
            }
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) value;
        }

        private int professionXpValue(Profession profession) {
            return switch (profession) {
                case FARMER -> farmerXp;
                case MINER -> minerXp;
                case TERRAFORMER -> terraformerXp;
                case LUMBERJACK -> lumberjackXp;
                case FISHER -> fisherXp;
                case HUNTER -> hunterXp;
                case WARRIOR -> warriorXp;
                case EXPLOSIVES_EXPERT -> explosivesExpertXp;
                case CRAFTER -> crafterXp;
                case ENCHANTER -> enchanterXp;
                case BREWER -> brewerXp;
                case TRADER -> traderXp;
                case SHEPHERD -> shepherdXp;
                case EXPLORER -> explorerXp;
                case TREASURE_HUNTER -> treasureHunterXp;
                case BREEDER -> breederXp;
                case SMITHY -> smithyXp;
                case SMELTER -> smelterXp;
                case NONE -> 0;
            };
        }

        private void setProfessionXpValue(Profession profession, int value) {
            int clamped = Math.max(0, value);
            switch (profession) {
                case FARMER -> farmerXp = clamped;
                case MINER -> minerXp = clamped;
                case TERRAFORMER -> terraformerXp = clamped;
                case LUMBERJACK -> lumberjackXp = clamped;
                case FISHER -> fisherXp = clamped;
                case HUNTER -> hunterXp = clamped;
                case WARRIOR -> warriorXp = clamped;
                case EXPLOSIVES_EXPERT -> explosivesExpertXp = clamped;
                case CRAFTER -> crafterXp = clamped;
                case ENCHANTER -> enchanterXp = clamped;
                case BREWER -> brewerXp = clamped;
                case TRADER -> traderXp = clamped;
                case SHEPHERD -> shepherdXp = clamped;
                case EXPLORER -> explorerXp = clamped;
                case TREASURE_HUNTER -> treasureHunterXp = clamped;
                case BREEDER -> breederXp = clamped;
                case SMITHY -> smithyXp = clamped;
                case SMELTER -> smelterXp = clamped;
                case NONE -> {
                }
            }
        }

        private void applyProfessionXpDecay(Profession gainedProfession, int gainedXp) {
            if (!RealCivConfig.specializationXpDecayEnabled() || gainedXp <= 0) {
                return;
            }
            double rate = RealCivConfig.specializationXpDecayRate();
            if (rate <= 0.0D) {
                return;
            }
            int decayAmount = Math.max(0, (int) Math.round(gainedXp * rate));
            if (decayAmount <= 0) {
                return;
            }
            for (Profession profession : Profession.values()) {
                if (profession == Profession.NONE || profession == gainedProfession) {
                    continue;
                }
                setProfessionXpValue(profession, professionXpValue(profession) - decayAmount);
            }
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

        public long dailyAllowanceWithdrawnCount(String civId, ResourceLocation itemId) {
            CivAccount account = account(civId);
            resetDailyAllowanceWindowIfNeeded(account);
            return account.dailyAllowanceWithdrawals.getOrDefault(itemId.toString(), 0L);
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

        public long remainingDailyAllowance(String civId, ResourceLocation itemId, long dailyLimit) {
            long safeLimit = Math.max(0L, dailyLimit);
            if (safeLimit <= 0L) {
                return 0L;
            }
            long used = dailyAllowanceWithdrawnCount(civId, itemId);
            long remaining = safeLimit - used;
            return Math.max(0L, remaining);
        }

        public void recordDailyAllowanceWithdrawal(String civId, ResourceLocation itemId, long count) {
            if (count <= 0L) {
                return;
            }
            CivAccount account = account(civId);
            resetDailyAllowanceWindowIfNeeded(account);
            account.dailyAllowanceWithdrawals.merge(itemId.toString(), count, Long::sum);
        }

        @Nullable
        public String ftbClaimModeOverride() {
            return ftbClaimModeOverride;
        }

        public void setFtbClaimModeOverride(@Nullable String rawMode) {
            ftbClaimModeOverride = normalizeFtbClaimMode(rawMode);
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
            tag.putLong("farmerActionsUpdatedAtMillis", farmerActionsUpdatedAtMillis);
            tag.putLong("minerActionsUpdatedAtMillis", minerActionsUpdatedAtMillis);
            tag.putInt("farmerXp", farmerXp);
            tag.putInt("minerXp", minerXp);
            CompoundTag minerBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : minerBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    minerBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("minerBlockActions", minerBlockTag);
            CompoundTag lumberjackBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : lumberjackBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    lumberjackBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("lumberjackBlockActions", lumberjackBlockTag);
            CompoundTag terraformerBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : terraformerBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    terraformerBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("terraformerBlockActions", terraformerBlockTag);
            tag.putInt("terraformerActions", terraformerActions);
            tag.putLong("terraformerActionsUpdatedAtMillis", terraformerActionsUpdatedAtMillis);
            tag.putInt("terraformerXp", terraformerXp);
            tag.putInt("lumberjackActions", lumberjackActions);
            tag.putLong("lumberjackActionsUpdatedAtMillis", lumberjackActionsUpdatedAtMillis);
            tag.putInt("lumberjackXp", lumberjackXp);
            tag.putInt("fisherActions", fisherActions);
            tag.putLong("fisherActionsUpdatedAtMillis", fisherActionsUpdatedAtMillis);
            tag.putInt("fisherXp", fisherXp);
            tag.putInt("hunterActions", hunterActions);
            tag.putLong("hunterActionsUpdatedAtMillis", hunterActionsUpdatedAtMillis);
            tag.putInt("hunterXp", hunterXp);
            tag.putInt("warriorActions", warriorActions);
            tag.putLong("warriorActionsUpdatedAtMillis", warriorActionsUpdatedAtMillis);
            tag.putInt("warriorXp", warriorXp);
            tag.putInt("pendingWarriorHubRegistrations", pendingWarriorHubRegistrations);
            tag.putInt("explosivesExpertActions", explosivesExpertActions);
            tag.putLong("explosivesExpertActionsUpdatedAtMillis", explosivesExpertActionsUpdatedAtMillis);
            tag.putInt("explosivesExpertXp", explosivesExpertXp);
            tag.putInt("crafterActions", crafterActions);
            tag.putLong("crafterActionsUpdatedAtMillis", crafterActionsUpdatedAtMillis);
            tag.putInt("crafterXp", crafterXp);
            tag.putInt("enchanterActions", enchanterActions);
            tag.putLong("enchanterActionsUpdatedAtMillis", enchanterActionsUpdatedAtMillis);
            tag.putInt("enchanterXp", enchanterXp);
            tag.putInt("brewerActions", brewerActions);
            tag.putLong("brewerActionsUpdatedAtMillis", brewerActionsUpdatedAtMillis);
            tag.putInt("brewerXp", brewerXp);
            tag.putInt("traderActions", traderActions);
            tag.putLong("traderActionsUpdatedAtMillis", traderActionsUpdatedAtMillis);
            tag.putInt("traderXp", traderXp);
            tag.putInt("shepherdActions", shepherdActions);
            tag.putLong("shepherdActionsUpdatedAtMillis", shepherdActionsUpdatedAtMillis);
            tag.putInt("shepherdXp", shepherdXp);
            tag.putInt("explorerActions", explorerActions);
            tag.putLong("explorerActionsUpdatedAtMillis", explorerActionsUpdatedAtMillis);
            tag.putInt("explorerXp", explorerXp);
            tag.putInt("treasureHunterActions", treasureHunterActions);
            tag.putLong("treasureHunterActionsUpdatedAtMillis", treasureHunterActionsUpdatedAtMillis);
            tag.putInt("treasureHunterXp", treasureHunterXp);
            tag.putInt("breederActions", breederActions);
            tag.putLong("breederActionsUpdatedAtMillis", breederActionsUpdatedAtMillis);
            tag.putInt("breederXp", breederXp);
            tag.putInt("smithyActions", smithyActions);
            tag.putLong("smithyActionsUpdatedAtMillis", smithyActionsUpdatedAtMillis);
            tag.putInt("smithyXp", smithyXp);
            tag.putInt("smelterActions", smelterActions);
            tag.putLong("smelterActionsUpdatedAtMillis", smelterActionsUpdatedAtMillis);
            tag.putInt("smelterXp", smelterXp);
            tag.putInt("generalXp", generalXp);
            if (firstSeenAtMillis > 0L) {
                tag.putLong("firstSeenAtMillis", firstSeenAtMillis);
            }
            if (focusedProfession != null && focusedProfession != Profession.NONE) {
                tag.putString("focusedProfession", focusedProfession.name());
            }

            ListTag hookUsageTag = new ListTag();
            for (Map.Entry<String, HookWindowUsage> entry : hookWindowUsage.entrySet()) {
                String key = entry.getKey();
                HookWindowUsage usage = entry.getValue();
                if (key == null || key.isBlank() || usage == null || usage.usedTriggers <= 0) {
                    continue;
                }
                CompoundTag usageTag = new CompoundTag();
                usageTag.putString("key", key);
                usageTag.putLong("windowStartMillis", Math.max(0L, usage.windowStartMillis));
                usageTag.putInt("used", Math.max(0, usage.usedTriggers));
                hookUsageTag.add(usageTag);
            }
            tag.put("hookWindowUsage", hookUsageTag);

            CompoundTag hunterMobTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : hunterMobActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    hunterMobTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("hunterMobActions", hunterMobTag);

            if (professionDailyActionDayIndex >= 0L) {
                tag.putLong("professionDailyActionDayIndex", professionDailyActionDayIndex);
            }
            CompoundTag professionDailyTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : professionDailyActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    professionDailyTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("professionDailyActions", professionDailyTag);

            if (minerDailyBlockActionDayIndex >= 0L) {
                tag.putLong("minerDailyBlockActionDayIndex", minerDailyBlockActionDayIndex);
            }
            CompoundTag minerDailyBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : minerDailyBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    minerDailyBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("minerDailyBlockActions", minerDailyBlockTag);

            if (professionProgressDayIndex >= 0L) {
                tag.putLong("professionProgressDayIndex", professionProgressDayIndex);
            }
            tag.putInt("generalLevelsGainedToday", Math.max(0, generalLevelsGainedToday));
            CompoundTag professionGainTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : professionLevelsGainedToday.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    professionGainTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("professionLevelsGainedToday", professionGainTag);

            if (contributionGainDayIndex >= 0L) {
                tag.putLong("contributionGainDayIndex", contributionGainDayIndex);
            }
            CompoundTag contributionGainTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : contributionGainTodayCents.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value > 0L) {
                    contributionGainTag.putLong(entry.getKey(), value);
                }
            }
            tag.put("contributionGainTodayCents", contributionGainTag);

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
            if (ftbClaimModeOverride != null) {
                tag.putString("ftbClaimModeOverride", ftbClaimModeOverride);
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
            CompoundTag minerBlockTag = tag.getCompound("minerBlockActions");
            for (String key : minerBlockTag.getAllKeys()) {
                int value = Math.max(0, minerBlockTag.getInt(key));
                if (value > 0) {
                    record.minerBlockActions.put(key, value);
                }
            }
            CompoundTag lumberjackBlockTag = tag.getCompound("lumberjackBlockActions");
            for (String key : lumberjackBlockTag.getAllKeys()) {
                int value = Math.max(0, lumberjackBlockTag.getInt(key));
                if (value > 0) {
                    record.lumberjackBlockActions.put(key, value);
                }
            }
            CompoundTag terraformerBlockTag = tag.getCompound("terraformerBlockActions");
            for (String key : terraformerBlockTag.getAllKeys()) {
                int value = Math.max(0, terraformerBlockTag.getInt(key));
                if (value > 0) {
                    record.terraformerBlockActions.put(key, value);
                }
            }
            record.terraformerActions = Math.max(0, tag.getInt("terraformerActions"));
            record.terraformerXp = Math.max(0, tag.getInt("terraformerXp"));
            record.lumberjackActions = Math.max(0, tag.getInt("lumberjackActions"));
            record.lumberjackXp = Math.max(0, tag.getInt("lumberjackXp"));
            record.fisherActions = Math.max(0, tag.getInt("fisherActions"));
            record.fisherXp = Math.max(0, tag.getInt("fisherXp"));
            record.hunterActions = Math.max(0, tag.getInt("hunterActions"));
            record.hunterXp = Math.max(0, tag.getInt("hunterXp"));
            record.warriorActions = Math.max(0, tag.getInt("warriorActions"));
            record.warriorXp = Math.max(0, tag.getInt("warriorXp"));
            record.pendingWarriorHubRegistrations = Math.max(0, tag.getInt("pendingWarriorHubRegistrations"));
            record.explosivesExpertActions = Math.max(0, tag.getInt("explosivesExpertActions"));
            record.explosivesExpertXp = Math.max(0, tag.getInt("explosivesExpertXp"));
            record.crafterActions = Math.max(0, tag.getInt("crafterActions"));
            record.crafterXp = Math.max(0, tag.getInt("crafterXp"));
            record.enchanterActions = Math.max(0, tag.getInt("enchanterActions"));
            record.enchanterXp = Math.max(0, tag.getInt("enchanterXp"));
            record.brewerActions = Math.max(0, tag.getInt("brewerActions"));
            record.brewerXp = Math.max(0, tag.getInt("brewerXp"));
            record.traderActions = Math.max(0, tag.getInt("traderActions"));
            record.traderXp = Math.max(0, tag.getInt("traderXp"));
            record.shepherdActions = Math.max(0, tag.getInt("shepherdActions"));
            record.shepherdXp = Math.max(0, tag.getInt("shepherdXp"));
            record.explorerActions = Math.max(0, tag.getInt("explorerActions"));
            record.explorerXp = Math.max(0, tag.getInt("explorerXp"));
            record.treasureHunterActions = Math.max(0, tag.getInt("treasureHunterActions"));
            record.treasureHunterXp = Math.max(0, tag.getInt("treasureHunterXp"));
            record.breederActions = Math.max(0, tag.getInt("breederActions"));
            record.breederXp = Math.max(0, tag.getInt("breederXp"));
            record.smithyActions = Math.max(0, tag.getInt("smithyActions"));
            record.smithyXp = Math.max(0, tag.getInt("smithyXp"));
            record.smelterActions = Math.max(0, tag.getInt("smelterActions"));
            record.smelterXp = Math.max(0, tag.getInt("smelterXp"));
            record.generalXp = Math.max(0, tag.getInt("generalXp"));
            if (tag.contains("focusedProfession")) {
                Profession parsed = Profession.fromConfigName(tag.getString("focusedProfession"));
                if (parsed != null && parsed != Profession.NONE) {
                    record.focusedProfession = parsed;
                }
            }

            long loadedAt = System.currentTimeMillis();
            record.firstSeenAtMillis = tag.contains("firstSeenAtMillis")
                    ? Math.max(0L, tag.getLong("firstSeenAtMillis"))
                    : loadedAt;
            if (record.firstSeenAtMillis <= 0L) {
                record.firstSeenAtMillis = loadedAt;
            }
            record.farmerActionsUpdatedAtMillis = tag.contains("farmerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("farmerActionsUpdatedAtMillis"))
                    : (record.farmerActions > 0 ? loadedAt : 0L);
            record.minerActionsUpdatedAtMillis = tag.contains("minerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("minerActionsUpdatedAtMillis"))
                    : (record.minerActions > 0 ? loadedAt : 0L);
            record.terraformerActionsUpdatedAtMillis = tag.contains("terraformerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("terraformerActionsUpdatedAtMillis"))
                    : (record.terraformerActions > 0 ? loadedAt : 0L);
            record.lumberjackActionsUpdatedAtMillis = tag.contains("lumberjackActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("lumberjackActionsUpdatedAtMillis"))
                    : (record.lumberjackActions > 0 ? loadedAt : 0L);
            record.fisherActionsUpdatedAtMillis = tag.contains("fisherActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("fisherActionsUpdatedAtMillis"))
                    : (record.fisherActions > 0 ? loadedAt : 0L);
            record.hunterActionsUpdatedAtMillis = tag.contains("hunterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("hunterActionsUpdatedAtMillis"))
                    : (record.hunterActions > 0 ? loadedAt : 0L);
            record.warriorActionsUpdatedAtMillis = tag.contains("warriorActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("warriorActionsUpdatedAtMillis"))
                    : (record.warriorActions > 0 ? loadedAt : 0L);
            record.explosivesExpertActionsUpdatedAtMillis = tag.contains("explosivesExpertActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("explosivesExpertActionsUpdatedAtMillis"))
                    : (record.explosivesExpertActions > 0 ? loadedAt : 0L);
            record.crafterActionsUpdatedAtMillis = tag.contains("crafterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("crafterActionsUpdatedAtMillis"))
                    : (record.crafterActions > 0 ? loadedAt : 0L);
            record.enchanterActionsUpdatedAtMillis = tag.contains("enchanterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("enchanterActionsUpdatedAtMillis"))
                    : (record.enchanterActions > 0 ? loadedAt : 0L);
            record.brewerActionsUpdatedAtMillis = tag.contains("brewerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("brewerActionsUpdatedAtMillis"))
                    : (record.brewerActions > 0 ? loadedAt : 0L);
            record.traderActionsUpdatedAtMillis = tag.contains("traderActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("traderActionsUpdatedAtMillis"))
                    : (record.traderActions > 0 ? loadedAt : 0L);
            record.shepherdActionsUpdatedAtMillis = tag.contains("shepherdActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("shepherdActionsUpdatedAtMillis"))
                    : (record.shepherdActions > 0 ? loadedAt : 0L);
            record.explorerActionsUpdatedAtMillis = tag.contains("explorerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("explorerActionsUpdatedAtMillis"))
                    : (record.explorerActions > 0 ? loadedAt : 0L);
            record.treasureHunterActionsUpdatedAtMillis = tag.contains("treasureHunterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("treasureHunterActionsUpdatedAtMillis"))
                    : (record.treasureHunterActions > 0 ? loadedAt : 0L);
            record.breederActionsUpdatedAtMillis = tag.contains("breederActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("breederActionsUpdatedAtMillis"))
                    : (record.breederActions > 0 ? loadedAt : 0L);
            record.smithyActionsUpdatedAtMillis = tag.contains("smithyActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("smithyActionsUpdatedAtMillis"))
                    : (record.smithyActions > 0 ? loadedAt : 0L);
            record.smelterActionsUpdatedAtMillis = tag.contains("smelterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("smelterActionsUpdatedAtMillis"))
                    : (record.smelterActions > 0 ? loadedAt : 0L);

            ListTag hookUsageTag = tag.getList("hookWindowUsage", Tag.TAG_COMPOUND);
            for (Tag entry : hookUsageTag) {
                if (!(entry instanceof CompoundTag usageTag)) {
                    continue;
                }
                String key = usageTag.getString("key");
                if (key == null || key.isBlank()) {
                    continue;
                }
                long windowStartMillis = Math.max(0L, usageTag.getLong("windowStartMillis"));
                int used = Math.max(0, usageTag.getInt("used"));
                if (used <= 0) {
                    continue;
                }
                HookWindowUsage usage = new HookWindowUsage();
                usage.windowStartMillis = windowStartMillis;
                usage.usedTriggers = used;
                record.hookWindowUsage.put(key, usage);
            }

            CompoundTag hunterMobTag = tag.getCompound("hunterMobActions");
            for (String key : hunterMobTag.getAllKeys()) {
                int value = Math.max(0, hunterMobTag.getInt(key));
                if (value > 0) {
                    record.hunterMobActions.put(key, value);
                }
            }

            record.professionDailyActionDayIndex = tag.contains("professionDailyActionDayIndex")
                    ? tag.getLong("professionDailyActionDayIndex")
                    : -1L;
            CompoundTag professionDailyTag = tag.getCompound("professionDailyActions");
            for (String key : professionDailyTag.getAllKeys()) {
                int value = Math.max(0, professionDailyTag.getInt(key));
                if (value > 0) {
                    record.professionDailyActions.put(key, value);
                }
            }

            record.minerDailyBlockActionDayIndex = tag.contains("minerDailyBlockActionDayIndex")
                    ? tag.getLong("minerDailyBlockActionDayIndex")
                    : -1L;
            CompoundTag minerDailyBlockTag = tag.getCompound("minerDailyBlockActions");
            for (String key : minerDailyBlockTag.getAllKeys()) {
                int value = Math.max(0, minerDailyBlockTag.getInt(key));
                if (value > 0) {
                    record.minerDailyBlockActions.put(key, value);
                }
            }

            record.professionProgressDayIndex = tag.contains("professionProgressDayIndex")
                    ? tag.getLong("professionProgressDayIndex")
                    : -1L;
            record.generalLevelsGainedToday = Math.max(0, tag.getInt("generalLevelsGainedToday"));
            CompoundTag professionGainTag = tag.getCompound("professionLevelsGainedToday");
            for (String key : professionGainTag.getAllKeys()) {
                int value = Math.max(0, professionGainTag.getInt(key));
                if (value > 0) {
                    record.professionLevelsGainedToday.put(key, value);
                }
            }

            record.contributionGainDayIndex = tag.contains("contributionGainDayIndex")
                    ? tag.getLong("contributionGainDayIndex")
                    : -1L;
            CompoundTag contributionGainTag = tag.getCompound("contributionGainTodayCents");
            for (String key : contributionGainTag.getAllKeys()) {
                long value = Math.max(0L, contributionGainTag.getLong(key));
                if (value > 0L) {
                    record.contributionGainTodayCents.put(key, value);
                }
            }

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
            if (tag.contains("ftbClaimModeOverride")) {
                record.ftbClaimModeOverride = normalizeFtbClaimMode(tag.getString("ftbClaimModeOverride"));
            }
            return record;
        }

        public int levelFor(Profession profession) {
            return switch (profession) {
                case FARMER -> RealCivConfig.professionLevelFromXp(Profession.FARMER, farmerXp());
                case MINER -> RealCivConfig.professionLevelFromXp(Profession.MINER, minerXp());
                case TERRAFORMER -> RealCivConfig.professionLevelFromXp(Profession.TERRAFORMER, terraformerXp());
                case LUMBERJACK -> RealCivConfig.professionLevelFromXp(Profession.LUMBERJACK, lumberjackXp());
                case FISHER -> RealCivConfig.professionLevelFromXp(Profession.FISHER, fisherXp());
                case HUNTER -> RealCivConfig.professionLevelFromXp(Profession.HUNTER, hunterXp());
                case WARRIOR -> RealCivConfig.professionLevelFromXp(Profession.WARRIOR, warriorXp());
                case EXPLOSIVES_EXPERT -> RealCivConfig.professionLevelFromXp(Profession.EXPLOSIVES_EXPERT, explosivesExpertXp());
                case CRAFTER -> RealCivConfig.professionLevelFromXp(Profession.CRAFTER, crafterXp());
                case ENCHANTER -> RealCivConfig.professionLevelFromXp(Profession.ENCHANTER, enchanterXp());
                case BREWER -> RealCivConfig.professionLevelFromXp(Profession.BREWER, brewerXp());
                case TRADER -> RealCivConfig.professionLevelFromXp(Profession.TRADER, traderXp());
                case SHEPHERD -> RealCivConfig.professionLevelFromXp(Profession.SHEPHERD, shepherdXp());
                case EXPLORER -> RealCivConfig.professionLevelFromXp(Profession.EXPLORER, explorerXp());
                case TREASURE_HUNTER -> RealCivConfig.professionLevelFromXp(Profession.TREASURE_HUNTER, treasureHunterXp());
                case BREEDER -> RealCivConfig.professionLevelFromXp(Profession.BREEDER, breederXp());
                case SMITHY -> RealCivConfig.professionLevelFromXp(Profession.SMITHY, smithyXp());
                case SMELTER -> RealCivConfig.professionLevelFromXp(Profession.SMELTER, smelterXp());
                case NONE -> 0;
            };
        }

        public int generalLevel() {
            return RealCivConfig.generalLevelFromXp(generalXp());
        }

        private static double clampRatio(double ratio) {
            return Math.max(0.0D, Math.min(1.0D, ratio));
        }

        private static long currentUtcDayIndex() {
            return System.currentTimeMillis() / DAY_MILLIS;
        }

        private static void resetDailyAllowanceWindowIfNeeded(CivAccount account) {
            long dayIndex = currentUtcDayIndex();
            if (account.dailyAllowanceDayIndex != dayIndex) {
                account.dailyAllowanceDayIndex = dayIndex;
                account.dailyAllowanceWithdrawals.clear();
            }
        }

        @Nullable
        private static String normalizeFtbClaimMode(@Nullable String rawMode) {
            if (rawMode == null) {
                return null;
            }
            String mode = rawMode.trim().toLowerCase(java.util.Locale.ROOT);
            if (mode.isEmpty() || "auto".equals(mode)) {
                return null;
            }
            if ("civic".equals(mode) || "private".equals(mode)) {
                return mode;
            }
            return null;
        }

        private static final class HookWindowUsage {
            private long windowStartMillis;
            private int usedTriggers;
        }
    }

    public static final class CivAccount {
        private long socialCreditCents;
        private final Map<String, Long> contributions = new HashMap<>();
        private final Map<String, Long> personalWithdrawals = new HashMap<>();
        private long dailyAllowanceDayIndex = -1L;
        private final Map<String, Long> dailyAllowanceWithdrawals = new HashMap<>();
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

            if (dailyAllowanceDayIndex >= 0L) {
                tag.putLong("dailyAllowanceDayIndex", dailyAllowanceDayIndex);
            }
            CompoundTag dailyAllowanceTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : dailyAllowanceWithdrawals.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value > 0L) {
                    dailyAllowanceTag.putLong(entry.getKey(), value);
                }
            }
            tag.put("dailyAllowanceWithdrawals", dailyAllowanceTag);

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

            account.dailyAllowanceDayIndex = tag.contains("dailyAllowanceDayIndex")
                    ? tag.getLong("dailyAllowanceDayIndex")
                    : -1L;
            CompoundTag dailyAllowanceTag = tag.getCompound("dailyAllowanceWithdrawals");
            for (String key : dailyAllowanceTag.getAllKeys()) {
                long value = Math.max(0L, dailyAllowanceTag.getLong(key));
                if (value > 0L) {
                    account.dailyAllowanceWithdrawals.put(key, value);
                }
            }

            if (tag.contains("personalWithdrawRatioOverride")) {
                account.personalWithdrawRatioOverride = Math.max(0.0D, Math.min(1.0D, tag.getDouble("personalWithdrawRatioOverride")));
            }
            return account;
        }
    }
}
