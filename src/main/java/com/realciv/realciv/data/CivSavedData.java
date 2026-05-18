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
    static final String DEFAULT_LEADER_TITLE = "Mayor";
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
    private final Map<String, DiplomacyRequestRecord> diplomacyRequests = new HashMap<>();
    private final Map<String, ActiveWarRecord> activeWars = new HashMap<>();
    private final Map<String, WarCasualtyRecord> warCasualties = new HashMap<>();
    private final Map<String, String> vassalOverlord = new HashMap<>();
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

        ListTag diplomacyRequestTags = tag.getList("diplomacyRequests", Tag.TAG_COMPOUND);
        for (Tag entry : diplomacyRequestTags) {
            if (!(entry instanceof CompoundTag requestTag)) {
                continue;
            }
            String requesterCiv = normalizeCivId(requestTag.getString("requesterCiv"));
            String responderCiv = normalizeCivId(requestTag.getString("responderCiv"));
            DiplomacyState requestedState = DiplomacyState.fromSerializedName(requestTag.getString("requestedState"));
            if (requesterCiv == null || responderCiv == null || requestedState == null || requestedState == DiplomacyState.NEUTRAL) {
                continue;
            }
            String key = diplomacyRequestKey(requesterCiv, responderCiv);
            if (key == null) {
                continue;
            }
            DiplomacyRequestRecord request = new DiplomacyRequestRecord();
            request.requesterCivId = requesterCiv;
            request.responderCivId = responderCiv;
            request.requestedState = requestedState;
            request.createdAtMillis = Math.max(0L, requestTag.getLong("createdAtMillis"));
            if (requestedState == DiplomacyState.WAR) {
                request.warType = WarType.fromSerializedName(requestTag.getString("warType"));
                if (request.warType == null) {
                    request.warType = WarType.DESTRUCTION;
                }
                int configuredTarget = requestTag.contains("pvpKillTarget")
                        ? Math.max(1, requestTag.getInt("pvpKillTarget"))
                        : RealCivConfig.defaultWarPvpKillTarget();
                request.pvpKillTarget = configuredTarget;
                request.warOfSubmission = requestTag.getBoolean("warOfSubmission");
                request.warOfLand = requestTag.getBoolean("warOfLand");
            }
            data.diplomacyRequests.put(key, request);
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

        ListTag activeWarTags = tag.getList("activeWars", Tag.TAG_COMPOUND);
        for (Tag entry : activeWarTags) {
            if (!(entry instanceof CompoundTag warTag)) {
                continue;
            }
            String civA = normalizeCivId(warTag.getString("civA"));
            String civB = normalizeCivId(warTag.getString("civB"));
            WarType warType = WarType.fromSerializedName(warTag.getString("warType"));
            if (civA == null || civB == null || civA.equals(civB) || warType == null) {
                continue;
            }
            String key = diplomacyKey(civA, civB);
            if (key == null) {
                continue;
            }
            ActiveWarRecord war = new ActiveWarRecord();
            war.warType = warType;
            war.pvpKillTarget = Math.max(1, warTag.getInt("pvpKillTarget"));
            if (warType == WarType.DESTRUCTION) {
                war.pvpKillTarget = Math.max(1, RealCivConfig.defaultWarPvpKillTarget());
            }
            war.warOfSubmission = warTag.getBoolean("warOfSubmission");
            war.warOfLand = warTag.getBoolean("warOfLand");
            war.startedAtMillis = Math.max(0L, warTag.getLong("startedAtMillis"));
            war.declaredByCivId = normalizeCivId(warTag.getString("declaredByCivId"));
            data.activeWars.put(key, war);
        }

        CompoundTag vassalTag = tag.getCompound("vassalOverlord");
        for (String vassalIdRaw : vassalTag.getAllKeys()) {
            String vassalId = normalizeCivId(vassalIdRaw);
            String overlordId = normalizeCivId(vassalTag.getString(vassalIdRaw));
            if (vassalId == null || overlordId == null || vassalId.equals(overlordId)) {
                continue;
            }
            if (!data.civilizations.containsKey(vassalId) || !data.civilizations.containsKey(overlordId)) {
                continue;
            }
            data.vassalOverlord.put(vassalId, overlordId);
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

        ListTag diplomacyRequestTags = new ListTag();
        for (DiplomacyRequestRecord request : diplomacyRequests.values()) {
            if (request == null
                    || request.requesterCivId == null
                    || request.responderCivId == null
                    || request.requestedState == null
                    || request.requestedState == DiplomacyState.NEUTRAL) {
                continue;
            }
            String key = diplomacyRequestKey(request.requesterCivId, request.responderCivId);
            if (key == null) {
                continue;
            }
            CompoundTag requestTag = new CompoundTag();
            requestTag.putString("requesterCiv", request.requesterCivId);
            requestTag.putString("responderCiv", request.responderCivId);
            requestTag.putString("requestedState", request.requestedState.serializedName());
            requestTag.putLong("createdAtMillis", Math.max(0L, request.createdAtMillis));
            if (request.requestedState == DiplomacyState.WAR) {
                WarType requestWarType = request.warType == null ? WarType.DESTRUCTION : request.warType;
                requestTag.putString("warType", requestWarType.serializedName());
                requestTag.putInt("pvpKillTarget", Math.max(1, request.pvpKillTarget));
                requestTag.putBoolean("warOfSubmission", request.warOfSubmission);
                requestTag.putBoolean("warOfLand", request.warOfLand);
            }
            diplomacyRequestTags.add(requestTag);
        }
        tag.put("diplomacyRequests", diplomacyRequestTags);

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

        ListTag activeWarTags = new ListTag();
        for (Map.Entry<String, ActiveWarRecord> entry : activeWars.entrySet()) {
            DiplomacyPair pair = diplomacyPairFromKey(entry.getKey());
            if (pair == null) {
                continue;
            }
            ActiveWarRecord war = entry.getValue();
            if (war == null || war.warType == null) {
                continue;
            }
            CompoundTag warTag = new CompoundTag();
            warTag.putString("civA", pair.firstCivId());
            warTag.putString("civB", pair.secondCivId());
            warTag.putString("warType", war.warType.serializedName());
            warTag.putInt("pvpKillTarget", Math.max(1, war.pvpKillTarget));
            warTag.putBoolean("warOfSubmission", war.warOfSubmission);
            warTag.putBoolean("warOfLand", war.warOfLand);
            warTag.putLong("startedAtMillis", Math.max(0L, war.startedAtMillis));
            if (war.declaredByCivId != null) {
                warTag.putString("declaredByCivId", war.declaredByCivId);
            }
            activeWarTags.add(warTag);
        }
        tag.put("activeWars", activeWarTags);

        CompoundTag vassalTag = new CompoundTag();
        for (Map.Entry<String, String> entry : vassalOverlord.entrySet()) {
            String vassalId = normalizeCivId(entry.getKey());
            String overlordId = normalizeCivId(entry.getValue());
            if (vassalId == null || overlordId == null || vassalId.equals(overlordId)) {
                continue;
            }
            vassalTag.putString(vassalId, overlordId);
        }
        tag.put("vassalOverlord", vassalTag);

        return tag;
    }

    @Nullable
    static String normalizeCivId(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String id = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return id.isEmpty() ? null : id;
    }

    @Nullable
    static String normalizeRoleId(@Nullable String raw) {
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

    static String sanitizeRoleDisplayName(@Nullable String raw) {
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
    static String normalizePermissionKey(@Nullable String raw) {
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
        if (out.isEmpty()) {
            return null;
        }
        String normalized = out.toString();
        return KNOWN_ROLE_PERMISSIONS.contains(normalized) ? normalized : null;
    }

    static String sanitizeLeaderTitle(@Nullable String raw) {
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

    static double clampUpkeepRateMultiplier(double multiplier) {
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
            return 1.0D;
        }
        return Math.max(0.25D, Math.min(5.0D, multiplier));
    }

    static double clampUnitRatio(double ratio) {
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
    private static String diplomacyRequestKey(String requesterCivRaw, String responderCivRaw) {
        String requesterCiv = normalizeCivId(requesterCivRaw);
        String responderCiv = normalizeCivId(responderCivRaw);
        if (requesterCiv == null || responderCiv == null || requesterCiv.equals(responderCiv)) {
            return null;
        }
        return requesterCiv + "->" + responderCiv;
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
        removeDiplomacyRequestsForCivilization(civId);
        removeActiveWarsForCivilization(civId);
        removeWarCasualtiesForCivilization(civId);
        removeVassalLinksForCivilization(civId);

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
        long available = civ.availableHubStock(key);
        if (available < amount) {
            return false;
        }
        long current = civ.hubStock().getOrDefault(key, 0L);
        long remaining = current - amount;
        if (remaining <= 0L) {
            civ.hubStock().remove(key);
        } else {
            civ.hubStock().put(key, remaining);
        }
        setDirty();
        return true;
    }

    private void lockHubStock(String civIdRaw, String itemId, long amount) {
        if (amount <= 0L || itemId == null || itemId.isBlank()) {
            return;
        }
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.hubLockedForWar().merge(itemId, amount, Long::sum);
        setDirty();
    }

    private void unlockHubStock(String civIdRaw, String itemId, long amount) {
        if (amount <= 0L || itemId == null || itemId.isBlank()) {
            return;
        }
        CivilizationRecord civ = getOrCreateCivilization(civIdRaw);
        civ.hubLockedForWar().merge(itemId, -amount, (old, delta) -> {
            long result = old + delta;
            return result <= 0L ? null : result;
        });
        setDirty();
    }

    public long getAvailableHubStock(String civIdRaw, ResourceLocation itemId) {
        return getOrCreateCivilization(civIdRaw).availableHubStock(itemId.toString());
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
                record.clearCrafterItemWindow();
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

        if (RealCivConfig.hubDepositGeneralXpEnabled()) {
            record.addGeneralXp(rewardRule.generalXpPerItem() * itemCount);
        }
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
        if (civicAttribute(civIdRaw, AttributeCategory.TAXATION) == CivicAttribute.EXEMPT) {
            return 0L;
        }
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

    public CivicAttribute civicAttribute(String civIdRaw, AttributeCategory category) {
        return getOrCreateCivilization(civIdRaw).civicAttribute(category);
    }

    public boolean setCivicAttribute(String civIdRaw, AttributeCategory category, @Nullable CivicAttribute attribute, String actorName) {
        CivilizationRecord civ = getCivilization(civIdRaw);
        if (civ == null || attribute == null || attribute.category() != category) {
            return false;
        }
        if (civ.civicAttribute(category) == attribute) {
            return false;
        }
        civ.setCivicAttribute(category, attribute);
        // Executive attribute changes can alter voter eligibility/quorum math, so discard any in-flight proposal
        if (category == AttributeCategory.EXECUTIVE) {
            civ.setGovernanceProposal(null);
        }
        addAuditLog(
                civ.id(),
                actorName + " set " + category.displayName() + " to " + attribute.displayName(),
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
        CivicAttribute succession = civicAttribute(civ.id(), AttributeCategory.SUCCESSION);
        if (succession != CivicAttribute.ELECTION) {
            return LeadershipContestDecision.denied("This civilization does not allow elections (Succession: " + succession.displayName() + ").");
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
        CivicAttribute succession = civicAttribute(civ.id(), AttributeCategory.SUCCESSION);
        if (succession != CivicAttribute.ELECTION) {
            return LeadershipContestDecision.denied("This civilization does not allow elections (Succession: " + succession.displayName() + ").");
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
        CivicAttribute succession = civicAttribute(civ.id(), AttributeCategory.SUCCESSION);
        if (succession != CivicAttribute.ELECTION) {
            return LeadershipContestDecision.denied("This civilization does not allow elections (Succession: " + succession.displayName() + ").");
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
        CivicAttribute succession = civicAttribute(civ.id(), AttributeCategory.SUCCESSION);
        if (succession != CivicAttribute.COUP) {
            return LeadershipContestDecision.denied("This civilization does not allow coups (Succession: " + succession.displayName() + ").");
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
        CivicAttribute succession = civicAttribute(civ.id(), AttributeCategory.SUCCESSION);
        if (succession != CivicAttribute.COUP) {
            return LeadershipContestDecision.denied("This civilization does not allow coups (Succession: " + succession.displayName() + ").");
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

    public boolean isKarmaTax(String civIdRaw) {
        return civicAttribute(civIdRaw, AttributeCategory.TAXATION) == CivicAttribute.KARMA_TAX;
    }

    public boolean isGoodsTax(String civIdRaw) {
        return civicAttribute(civIdRaw, AttributeCategory.TAXATION) == CivicAttribute.GOODS_TAX;
    }

    public boolean isTaxExempt(String civIdRaw) {
        return civicAttribute(civIdRaw, AttributeCategory.TAXATION) == CivicAttribute.EXEMPT;
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
        if (civicAttribute(civIdRaw, AttributeCategory.TAXATION) == CivicAttribute.EXEMPT) {
            return 0L;
        }
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

    @Nullable
    public DiplomacyRequestView diplomacyRequest(String requesterCivRaw, String responderCivRaw) {
        String key = diplomacyRequestKey(requesterCivRaw, responderCivRaw);
        if (key == null) {
            return null;
        }
        DiplomacyRequestRecord request = diplomacyRequests.get(key);
        return toDiplomacyRequestView(request);
    }

    public List<DiplomacyRequestView> incomingDiplomacyRequestsFor(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return List.of();
        }
        List<DiplomacyRequestView> out = new ArrayList<>();
        for (DiplomacyRequestRecord request : diplomacyRequests.values()) {
            if (request == null || !civId.equals(request.responderCivId)) {
                continue;
            }
            DiplomacyRequestView view = toDiplomacyRequestView(request);
            if (view != null) {
                out.add(view);
            }
        }
        out.sort(Comparator
                .comparingLong(DiplomacyRequestView::createdAtMillis)
                .thenComparing(DiplomacyRequestView::requesterCivilizationId));
        return out;
    }

    public List<DiplomacyRequestView> outgoingDiplomacyRequestsFor(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return List.of();
        }
        List<DiplomacyRequestView> out = new ArrayList<>();
        for (DiplomacyRequestRecord request : diplomacyRequests.values()) {
            if (request == null || !civId.equals(request.requesterCivId)) {
                continue;
            }
            DiplomacyRequestView view = toDiplomacyRequestView(request);
            if (view != null) {
                out.add(view);
            }
        }
        out.sort(Comparator
                .comparingLong(DiplomacyRequestView::createdAtMillis)
                .thenComparing(DiplomacyRequestView::responderCivilizationId));
        return out;
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
        clearDiplomacyRequestsBetween(civA, civB);
        if (state == DiplomacyState.WAR) {
            startWarInternal(
                    civA,
                    civB,
                    WarType.DESTRUCTION,
                    RealCivConfig.defaultWarPvpKillTarget(),
                    false,
                    false,
                    false,
                    null,
                    0L,
                    civA);
        }

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

    public DiplomacyRequestResult proposeDiplomacyStateChange(
            String requesterCivRaw,
            String responderCivRaw,
            DiplomacyState requestedState,
            String actorName) {
        if (requestedState == DiplomacyState.WAR) {
            return proposeWarDeclaration(
                    requesterCivRaw,
                    responderCivRaw,
                    WarType.DESTRUCTION,
                    RealCivConfig.defaultWarPvpKillTarget(),
                    false,
                    false,
                    false,
                    null,
                    0L,
                    actorName);
        }
        return proposeDiplomacyStateChangeInternal(
                requesterCivRaw,
                responderCivRaw,
                requestedState,
                null,
                0,
                false,
                false,
                false,
                null,
                0L,
                actorName);
    }

    public DiplomacyRequestResult proposeWarDeclaration(
            String requesterCivRaw,
            String responderCivRaw,
            @Nullable WarType warTypeRaw,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            String actorName) {
        return proposeWarDeclaration(
                requesterCivRaw,
                responderCivRaw,
                warTypeRaw,
                pvpKillTarget,
                warOfSubmission,
                warOfLand,
                false,
                null,
                0L,
                actorName);
    }

    public DiplomacyRequestResult proposeWarDeclaration(
            String requesterCivRaw,
            String responderCivRaw,
            @Nullable WarType warTypeRaw,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmount,
            String actorName) {
        int safeKillTarget = Math.max(1, pvpKillTarget);
        WarType safeWarType = warTypeRaw == null ? WarType.DESTRUCTION : warTypeRaw;
        if (warResourceGamble) {
            if (warGambleItemId == null || warGambleItemId.isBlank() || warGambleAmount <= 0L) {
                return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
            }
            long available = getOrCreateCivilization(requesterCivRaw).availableHubStock(warGambleItemId);
            if (available < warGambleAmount) {
                return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
            }
        }
        return proposeDiplomacyStateChangeInternal(
                requesterCivRaw,
                responderCivRaw,
                DiplomacyState.WAR,
                safeWarType,
                safeKillTarget,
                warOfSubmission,
                warOfLand,
                warResourceGamble,
                warGambleItemId,
                warGambleAmount,
                actorName);
    }

    private DiplomacyRequestResult proposeDiplomacyStateChangeInternal(
            String requesterCivRaw,
            String responderCivRaw,
            DiplomacyState requestedState,
            @Nullable WarType warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmount,
            String actorName) {
        String requesterCiv = normalizeCivId(requesterCivRaw);
        String responderCiv = normalizeCivId(responderCivRaw);
        if (requesterCiv == null
                || responderCiv == null
                || requesterCiv.equals(responderCiv)
                || requestedState == null) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
        }
        CivilizationRecord requester = getCivilization(requesterCiv);
        CivilizationRecord responder = getCivilization(responderCiv);
        if (requester == null || responder == null) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
        }

        if (requestedState == DiplomacyState.NEUTRAL) {
            DiplomacyState current = diplomacyState(requesterCiv, responderCiv);
            if (current == DiplomacyState.NEUTRAL) {
                return new DiplomacyRequestResult(DiplomacyRequestResultType.NO_CHANGE_ALREADY_SET, DiplomacyState.NEUTRAL);
            }
            setDiplomacyStateInternal(requesterCiv, responderCiv, DiplomacyState.NEUTRAL);
            clearDiplomacyRequestsBetween(requesterCiv, responderCiv);
            clearWarStateBetween(requesterCiv, responderCiv);
            clearVassalLinkBetween(requesterCiv, responderCiv);
            addAuditLog(
                    requester.id(),
                    actorName + " set diplomacy with " + responder.displayName() + " [" + responder.id() + "] to NEUTRAL",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            addAuditLog(
                    responder.id(),
                    actorName + " set diplomacy with " + requester.displayName() + " [" + requester.id() + "] to NEUTRAL",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            setDirty();
            return new DiplomacyRequestResult(DiplomacyRequestResultType.STATE_SET, DiplomacyState.NEUTRAL);
        }

        DiplomacyState current = diplomacyState(requesterCiv, responderCiv);
        if (current == requestedState) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.NO_CHANGE_ALREADY_SET, requestedState);
        }

        String incomingKey = diplomacyRequestKey(responderCiv, requesterCiv);
        DiplomacyRequestRecord incoming = incomingKey == null ? null : diplomacyRequests.get(incomingKey);
        if (incoming != null && incoming.requestedState == requestedState) {
            if (incoming.warResourceGamble && incoming.warGambleItemId != null && incoming.warGambleAmount > 0L) {
                long currentAvailable = getOrCreateCivilization(requesterCiv).availableHubStock(incoming.warGambleItemId);
                if (currentAvailable < incoming.warGambleAmount) {
                    return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
                }
                lockHubStock(requesterCiv, incoming.warGambleItemId, incoming.warGambleAmount);
            }
            setDiplomacyStateInternal(requesterCiv, responderCiv, requestedState);
            clearDiplomacyRequestsBetween(requesterCiv, responderCiv);
            if (requestedState == DiplomacyState.WAR) {
                startWarInternal(requesterCiv, responderCiv, incoming, responderCiv);
            } else {
                clearWarStateBetween(requesterCiv, responderCiv);
            }
            addAuditLog(
                    requester.id(),
                    actorName + " accepted a " + requestedState.displayName() + " request from "
                            + responder.displayName() + " [" + responder.id() + "]",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            addAuditLog(
                    responder.id(),
                    actorName + " accepted your " + requestedState.displayName() + " request for "
                            + requester.displayName() + " [" + requester.id() + "]",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            setDirty();
            return new DiplomacyRequestResult(DiplomacyRequestResultType.REQUEST_ACCEPTED, requestedState);
        }

        String outgoingKey = diplomacyRequestKey(requesterCiv, responderCiv);
        if (outgoingKey == null) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, requestedState);
        }
        DiplomacyRequestRecord existing = diplomacyRequests.get(outgoingKey);
        if (existing != null && existing.requestedState == requestedState) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.REQUEST_ALREADY_PENDING, requestedState);
        }

        DiplomacyRequestRecord request = new DiplomacyRequestRecord();
        request.requesterCivId = requesterCiv;
        request.responderCivId = responderCiv;
        request.requestedState = requestedState;
        request.createdAtMillis = System.currentTimeMillis();
        if (requestedState == DiplomacyState.WAR) {
            request.warType = warType == null ? WarType.DESTRUCTION : warType;
            request.pvpKillTarget = Math.max(1, pvpKillTarget);
            request.warOfSubmission = warOfSubmission;
            request.warOfLand = warOfLand;
            request.warResourceGamble = warResourceGamble;
            request.warGambleItemId = warGambleItemId;
            request.warGambleAmount = Math.max(0L, warGambleAmount);
            if (warResourceGamble && warGambleItemId != null && warGambleAmount > 0L) {
                lockHubStock(requesterCiv, warGambleItemId, warGambleAmount);
            }
        }
        diplomacyRequests.put(outgoingKey, request);
        addAuditLog(
                requester.id(),
                actorName + " sent a " + requestedState.displayName() + " request to "
                        + responder.displayName() + " [" + responder.id() + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        addAuditLog(
                responder.id(),
                actorName + " received a " + requestedState.displayName() + " request from "
                        + requester.displayName() + " [" + requester.id() + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return new DiplomacyRequestResult(
                existing == null ? DiplomacyRequestResultType.REQUEST_SENT : DiplomacyRequestResultType.REQUEST_UPDATED,
                requestedState);
    }

    public DiplomacyRequestResult respondToDiplomacyRequest(
            String responderCivRaw,
            String requesterCivRaw,
            boolean accept,
            String actorName) {
        String responderCiv = normalizeCivId(responderCivRaw);
        String requesterCiv = normalizeCivId(requesterCivRaw);
        if (requesterCiv == null || responderCiv == null || requesterCiv.equals(responderCiv)) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
        }
        CivilizationRecord requester = getCivilization(requesterCiv);
        CivilizationRecord responder = getCivilization(responderCiv);
        if (requester == null || responder == null) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
        }
        String key = diplomacyRequestKey(requesterCiv, responderCiv);
        DiplomacyRequestRecord request = key == null ? null : diplomacyRequests.get(key);
        if (request == null || request.requestedState == null || request.requestedState == DiplomacyState.NEUTRAL) {
            return new DiplomacyRequestResult(DiplomacyRequestResultType.NO_PENDING_REQUEST, DiplomacyState.NEUTRAL);
        }
        DiplomacyState requestedState = request.requestedState;

        if (accept) {
            if (request.warResourceGamble && request.warGambleItemId != null && request.warGambleAmount > 0L) {
                long responderAvailable = responder.availableHubStock(request.warGambleItemId);
                if (responderAvailable < request.warGambleAmount) {
                    return new DiplomacyRequestResult(DiplomacyRequestResultType.INVALID, DiplomacyState.NEUTRAL);
                }
                lockHubStock(responderCiv, request.warGambleItemId, request.warGambleAmount);
            }
            setDiplomacyStateInternal(requesterCiv, responderCiv, requestedState);
            clearDiplomacyRequestsBetween(requesterCiv, responderCiv);
            if (requestedState == DiplomacyState.WAR) {
                startWarInternal(requesterCiv, responderCiv, request, requesterCiv);
            } else {
                clearWarStateBetween(requesterCiv, responderCiv);
            }
            addAuditLog(
                    responder.id(),
                    actorName + " accepted a " + requestedState.displayName() + " request from "
                            + requester.displayName() + " [" + requester.id() + "]",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            addAuditLog(
                    requester.id(),
                    actorName + " accepted your " + requestedState.displayName() + " request for "
                            + responder.displayName() + " [" + responder.id() + "]",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            setDirty();
            return new DiplomacyRequestResult(DiplomacyRequestResultType.REQUEST_ACCEPTED, requestedState);
        }

        if (request.warResourceGamble && request.warGambleItemId != null && request.warGambleAmount > 0L) {
            unlockHubStock(requesterCiv, request.warGambleItemId, request.warGambleAmount);
        }
        diplomacyRequests.remove(key);
        addAuditLog(
                responder.id(),
                actorName + " rejected a " + requestedState.displayName() + " request from "
                        + requester.displayName() + " [" + requester.id() + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        addAuditLog(
                requester.id(),
                actorName + " rejected your " + requestedState.displayName() + " request for "
                        + responder.displayName() + " [" + responder.id() + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        setDirty();
        return new DiplomacyRequestResult(DiplomacyRequestResultType.REQUEST_REJECTED, requestedState);
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
        boolean firstSideHit = false;
        boolean secondSideHit = false;
        if (victimCiv.equals(pair.firstCivId())) {
            record.firstCasualties = Math.min(Long.MAX_VALUE, record.firstCasualties + 1L);
            firstSideHit = true;
        } else if (victimCiv.equals(pair.secondCivId())) {
            record.secondCasualties = Math.min(Long.MAX_VALUE, record.secondCasualties + 1L);
            secondSideHit = true;
        }
        ActiveWarRecord war = activeWars.get(key);
        if (war != null && war.warType == WarType.PVP) {
            int target = Math.max(1, war.pvpKillTarget);
            if (firstSideHit && record.firstCasualties >= target) {
                concludeWarWithWinner(
                        pair.secondCivId(),
                        pair.firstCivId(),
                        war,
                        "PvP kill target reached");
                return;
            }
            if (secondSideHit && record.secondCasualties >= target) {
                concludeWarWithWinner(
                        pair.firstCivId(),
                        pair.secondCivId(),
                        war,
                        "PvP kill target reached");
                return;
            }
        }
        setDirty();
    }

    @Nullable
    public ActiveWarView activeWarBetween(String civAraw, String civBraw) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB)) {
            return null;
        }
        String key = diplomacyKey(civA, civB);
        if (key == null) {
            return null;
        }
        if (diplomacyState(civA, civB) != DiplomacyState.WAR) {
            return null;
        }
        ActiveWarRecord war = activeWars.get(key);
        if (war == null || war.warType == null) {
            return null;
        }
        return toActiveWarView(key, war);
    }

    public List<ActiveWarView> activeWarsFor(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return List.of();
        }
        List<ActiveWarView> out = new ArrayList<>();
        for (Map.Entry<String, ActiveWarRecord> entry : activeWars.entrySet()) {
            DiplomacyPair pair = diplomacyPairFromKey(entry.getKey());
            ActiveWarRecord war = entry.getValue();
            if (pair == null || war == null || war.warType == null) {
                continue;
            }
            if (!civId.equals(pair.firstCivId()) && !civId.equals(pair.secondCivId())) {
                continue;
            }
            if (diplomacyState(pair.firstCivId(), pair.secondCivId()) != DiplomacyState.WAR) {
                continue;
            }
            ActiveWarView view = toActiveWarView(entry.getKey(), war);
            if (view != null) {
                out.add(view);
            }
        }
        out.sort(Comparator
                .comparingLong(ActiveWarView::startedAtMillis)
                .thenComparing(ActiveWarView::firstCivilizationId)
                .thenComparing(ActiveWarView::secondCivilizationId));
        return out;
    }

    @Nullable
    public String overlordOf(String vassalCivRaw) {
        String vassalCiv = normalizeCivId(vassalCivRaw);
        if (vassalCiv == null) {
            return null;
        }
        String overlordCiv = normalizeCivId(vassalOverlord.get(vassalCiv));
        if (overlordCiv == null || !civilizations.containsKey(overlordCiv)) {
            return null;
        }
        return overlordCiv;
    }

    public boolean isOverlordOf(String overlordCivRaw, String vassalCivRaw) {
        String overlordCiv = normalizeCivId(overlordCivRaw);
        String vassalCiv = normalizeCivId(vassalCivRaw);
        if (overlordCiv == null || vassalCiv == null || overlordCiv.equals(vassalCiv)) {
            return false;
        }
        return overlordCiv.equals(overlordOf(vassalCiv));
    }

    public List<String> vassalsOf(String overlordCivRaw) {
        String overlordCiv = normalizeCivId(overlordCivRaw);
        if (overlordCiv == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : vassalOverlord.entrySet()) {
            String vassalCiv = normalizeCivId(entry.getKey());
            String mappedOverlord = normalizeCivId(entry.getValue());
            if (vassalCiv == null || mappedOverlord == null || vassalCiv.equals(mappedOverlord)) {
                continue;
            }
            if (overlordCiv.equals(mappedOverlord) && civilizations.containsKey(vassalCiv)) {
                out.add(vassalCiv);
            }
        }
        out.sort(String::compareTo);
        return out;
    }

    public WarResignResult resignWar(String loserCivRaw, String winnerCivRaw, String actorName) {
        String loserCiv = normalizeCivId(loserCivRaw);
        String winnerCiv = normalizeCivId(winnerCivRaw);
        if (loserCiv == null || winnerCiv == null || loserCiv.equals(winnerCiv)) {
            return new WarResignResult(WarResignResultType.INVALID, null);
        }
        if (diplomacyState(loserCiv, winnerCiv) != DiplomacyState.WAR) {
            return new WarResignResult(WarResignResultType.NOT_AT_WAR, null);
        }
        String key = diplomacyKey(loserCiv, winnerCiv);
        ActiveWarRecord war = key == null ? null : activeWars.get(key);
        if (war == null) {
            war = new ActiveWarRecord();
            war.warType = WarType.DESTRUCTION;
            war.pvpKillTarget = Math.max(1, RealCivConfig.defaultWarPvpKillTarget());
        }
        WarOutcomeView outcome = concludeWarWithWinner(
                winnerCiv,
                loserCiv,
                war,
                actorName + " accepted surrender");
        return new WarResignResult(WarResignResultType.RESIGNED, outcome);
    }

    private void startWarInternal(
            String civAraw,
            String civBraw,
            DiplomacyRequestRecord request,
            @Nullable String declaredByCivRaw) {
        WarType warType = request.warType == null ? WarType.DESTRUCTION : request.warType;
        int killTarget = Math.max(1, request.pvpKillTarget);
        startWarInternal(
                civAraw,
                civBraw,
                warType,
                killTarget,
                request.warOfSubmission,
                request.warOfLand,
                request.warResourceGamble,
                request.warGambleItemId,
                request.warGambleAmount,
                declaredByCivRaw);
    }

    private void startWarInternal(
            String civAraw,
            String civBraw,
            WarType warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmount,
            @Nullable String declaredByCivRaw) {
        String key = diplomacyKey(civAraw, civBraw);
        if (key == null) {
            return;
        }
        ActiveWarRecord war = new ActiveWarRecord();
        war.warType = warType == null ? WarType.DESTRUCTION : warType;
        war.pvpKillTarget = Math.max(1, pvpKillTarget);
        war.warOfSubmission = warOfSubmission;
        war.warOfLand = warOfLand;
        war.warResourceGamble = warResourceGamble;
        war.warGambleItemId = warGambleItemId;
        war.warGambleAmount = Math.max(0L, warGambleAmount);
        war.startedAtMillis = System.currentTimeMillis();
        war.declaredByCivId = normalizeCivId(declaredByCivRaw);
        activeWars.put(key, war);
        warCasualties.remove(key);
        clearVassalLinkBetween(civAraw, civBraw);
    }

    private void clearWarStateBetween(String civAraw, String civBraw) {
        String key = diplomacyKey(civAraw, civBraw);
        if (key == null) {
            return;
        }
        ActiveWarRecord war = activeWars.get(key);
        if (war != null && war.warResourceGamble && war.warGambleItemId != null && war.warGambleAmount > 0L) {
            unlockHubStock(civAraw, war.warGambleItemId, war.warGambleAmount);
            unlockHubStock(civBraw, war.warGambleItemId, war.warGambleAmount);
        }
        activeWars.remove(key);
        warCasualties.remove(key);
    }

    private void clearVassalLinkBetween(String civAraw, String civBraw) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB)) {
            return;
        }
        if (civB.equals(vassalOverlord.get(civA))) {
            vassalOverlord.remove(civA);
        }
        if (civA.equals(vassalOverlord.get(civB))) {
            vassalOverlord.remove(civB);
        }
    }

    private void setVassalOverlord(String vassalCivRaw, String overlordCivRaw) {
        String vassalCiv = normalizeCivId(vassalCivRaw);
        String overlordCiv = normalizeCivId(overlordCivRaw);
        if (vassalCiv == null || overlordCiv == null || vassalCiv.equals(overlordCiv)) {
            return;
        }
        vassalOverlord.remove(overlordCiv);
        vassalOverlord.put(vassalCiv, overlordCiv);
    }

    @Nullable
    private ActiveWarView toActiveWarView(String key, ActiveWarRecord war) {
        DiplomacyPair pair = diplomacyPairFromKey(key);
        if (pair == null || war.warType == null) {
            return null;
        }
        WarCasualtyRecord casualty = warCasualties.get(key);
        long firstCasualties = casualty == null ? 0L : Math.max(0L, casualty.firstCasualties);
        long secondCasualties = casualty == null ? 0L : Math.max(0L, casualty.secondCasualties);
        return new ActiveWarView(
                pair.firstCivId(),
                pair.secondCivId(),
                war.warType,
                Math.max(1, war.pvpKillTarget),
                war.warOfSubmission,
                war.warOfLand,
                war.warResourceGamble,
                war.warGambleItemId,
                Math.max(0L, war.warGambleAmount),
                Math.max(0L, war.startedAtMillis),
                war.declaredByCivId,
                firstCasualties,
                secondCasualties);
    }

    @Nullable
    private WarOutcomeView concludeWarWithWinner(
            String winnerCivRaw,
            String loserCivRaw,
            ActiveWarRecord war,
            String reason) {
        String winnerCiv = normalizeCivId(winnerCivRaw);
        String loserCiv = normalizeCivId(loserCivRaw);
        if (winnerCiv == null || loserCiv == null || winnerCiv.equals(loserCiv)) {
            return null;
        }
        CivilizationRecord winner = getCivilization(winnerCiv);
        CivilizationRecord loser = getCivilization(loserCiv);
        if (winner == null || loser == null) {
            return null;
        }
        WarType warType = war.warType == null ? WarType.DESTRUCTION : war.warType;
        boolean warOfSubmission = war.warOfSubmission;
        boolean warOfLand = war.warOfLand;

        clearDiplomacyRequestsBetween(winnerCiv, loserCiv);
        clearWarStateBetween(winnerCiv, loserCiv);

        if (warOfSubmission) {
            setVassalOverlord(loserCiv, winnerCiv);
            setDiplomacyStateInternal(winnerCiv, loserCiv, DiplomacyState.ALLY);
        } else {
            clearVassalLinkBetween(winnerCiv, loserCiv);
            setDiplomacyStateInternal(winnerCiv, loserCiv, DiplomacyState.NEUTRAL);
        }

        int transferredPlotCount = 0;
        if (warOfLand) {
            transferredPlotCount = transferAllPlots(loser.id(), winner.id());
        }

        long gambleTransferred = 0L;
        if (war.warResourceGamble && war.warGambleItemId != null && war.warGambleAmount > 0L) {
            String gambleItem = war.warGambleItemId;
            long gambleAmount = war.warGambleAmount;
            long loserLocked = loser.hubLockedForWar().getOrDefault(gambleItem, 0L);
            long actualTransfer = Math.min(loserLocked, gambleAmount);
            if (actualTransfer > 0L) {
                winner.hubStock().merge(gambleItem, actualTransfer, Long::sum);
                loser.hubStock().merge(gambleItem, -actualTransfer, (old, delta) -> {
                    long result = old + delta;
                    return result <= 0L ? null : result;
                });
                gambleTransferred = actualTransfer;
            }
            unlockHubStock(winnerCiv, gambleItem, gambleAmount);
            unlockHubStock(loserCiv, gambleItem, gambleAmount);
        }

        addAuditLog(
                winner.id(),
                "War ended against " + loser.displayName() + " [" + loser.id() + "]"
                        + " | winner: " + winner.displayName()
                        + " | type: " + warType.displayName()
                        + " | submission: " + (warOfSubmission ? "yes" : "no")
                        + " | land takeover: " + (warOfLand ? ("yes (" + transferredPlotCount + " plots)") : "no")
                        + (war.warResourceGamble ? (" | resource gamble: " + gambleTransferred + "x " + war.warGambleItemId) : "")
                        + " | reason: " + reason,
                RealCivConfig.MAX_AUDIT_LOGS.get());
        addAuditLog(
                loser.id(),
                "War ended against " + winner.displayName() + " [" + winner.id() + "]"
                        + " | winner: " + winner.displayName()
                        + " | type: " + warType.displayName()
                        + " | submission: " + (warOfSubmission ? "yes" : "no")
                        + " | land takeover: " + (warOfLand ? ("yes (" + transferredPlotCount + " plots)") : "no")
                        + (war.warResourceGamble ? (" | resource gamble: " + gambleTransferred + "x " + war.warGambleItemId) : "")
                        + " | reason: " + reason,
                RealCivConfig.MAX_AUDIT_LOGS.get());

        if (attachedServer != null) {
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, winner.id());
            RealCivFTBChunksMirror.syncCivilization(attachedServer, this, loser.id());
        }
        setDirty();
        return new WarOutcomeView(
                winner.id(),
                loser.id(),
                warType,
                Math.max(1, war.pvpKillTarget),
                warOfSubmission,
                warOfLand,
                transferredPlotCount,
                war.warResourceGamble,
                war.warGambleItemId,
                gambleTransferred);
    }

    private int transferAllPlots(String fromCivRaw, String toCivRaw) {
        CivilizationRecord from = getCivilization(fromCivRaw);
        CivilizationRecord to = getCivilization(toCivRaw);
        if (from == null || to == null || from.id().equals(to.id())) {
            return 0;
        }
        int transferred = 0;
        Iterator<Map.Entry<String, PlotRecord>> iterator = from.plots().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PlotRecord> entry = iterator.next();
            PlotRecord plot = entry.getValue();
            if (plot == null) {
                iterator.remove();
                continue;
            }
            if (plot.landClass() == LandClass.PRIVATE) {
                plot.setLandClass(LandClass.COMMUNITY);
                plot.setOwnerId(null);
                plot.setDelinquentSinceTick(-1L);
            }
            if (!to.plots().containsKey(entry.getKey())) {
                to.plots().put(entry.getKey(), plot);
                transferred++;
            }
            iterator.remove();
        }
        return transferred;
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

    @Nullable
    private static DiplomacyRequestView toDiplomacyRequestView(@Nullable DiplomacyRequestRecord request) {
        if (request == null
                || request.requesterCivId == null
                || request.responderCivId == null
                || request.requestedState == null
                || request.requestedState == DiplomacyState.NEUTRAL) {
            return null;
        }
        return new DiplomacyRequestView(
                request.requesterCivId,
                request.responderCivId,
                request.requestedState,
                Math.max(0L, request.createdAtMillis),
                request.requestedState == DiplomacyState.WAR ? request.warType : null,
                request.requestedState == DiplomacyState.WAR ? Math.max(1, request.pvpKillTarget) : 0,
                request.requestedState == DiplomacyState.WAR && request.warOfSubmission,
                request.requestedState == DiplomacyState.WAR && request.warOfLand,
                request.requestedState == DiplomacyState.WAR && request.warResourceGamble,
                request.requestedState == DiplomacyState.WAR ? request.warGambleItemId : null,
                request.requestedState == DiplomacyState.WAR ? Math.max(0L, request.warGambleAmount) : 0L);
    }

    private void clearDiplomacyRequestsBetween(String civAraw, String civBraw) {
        String civA = normalizeCivId(civAraw);
        String civB = normalizeCivId(civBraw);
        if (civA == null || civB == null || civA.equals(civB)) {
            return;
        }
        String aToB = diplomacyRequestKey(civA, civB);
        if (aToB != null) {
            DiplomacyRequestRecord request = diplomacyRequests.get(aToB);
            if (request != null && request.warResourceGamble && request.warGambleItemId != null && request.warGambleAmount > 0L) {
                unlockHubStock(request.requesterCivId, request.warGambleItemId, request.warGambleAmount);
            }
            diplomacyRequests.remove(aToB);
        }
        String bToA = diplomacyRequestKey(civB, civA);
        if (bToA != null) {
            DiplomacyRequestRecord request = diplomacyRequests.get(bToA);
            if (request != null && request.warResourceGamble && request.warGambleItemId != null && request.warGambleAmount > 0L) {
                unlockHubStock(request.requesterCivId, request.warGambleItemId, request.warGambleAmount);
            }
            diplomacyRequests.remove(bToA);
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

    private int removeDiplomacyRequestsForCivilization(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return 0;
        }
        int removed = 0;
        Iterator<Map.Entry<String, DiplomacyRequestRecord>> iterator = diplomacyRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DiplomacyRequestRecord> entry = iterator.next();
            DiplomacyRequestRecord request = entry.getValue();
            if (request == null
                    || civId.equals(normalizeCivId(request.requesterCivId))
                    || civId.equals(normalizeCivId(request.responderCivId))) {
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

    private int removeActiveWarsForCivilization(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return 0;
        }
        int removed = 0;
        Iterator<Map.Entry<String, ActiveWarRecord>> iterator = activeWars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ActiveWarRecord> entry = iterator.next();
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

    private int removeVassalLinksForCivilization(String civIdRaw) {
        String civId = normalizeCivId(civIdRaw);
        if (civId == null) {
            return 0;
        }
        int removed = 0;
        if (vassalOverlord.remove(civId) != null) {
            removed++;
        }
        Iterator<Map.Entry<String, String>> iterator = vassalOverlord.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (civId.equals(normalizeCivId(entry.getValue()))) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private static void removePlayerFromAllCustomRoles(CivilizationRecord civ, UUID playerId) {
        civ.removePlayerFromAllRoles(playerId);
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

    private boolean crossCivBuildBreakAllowed(String actorCivRaw, String targetCivRaw) {
        String actorCiv = normalizeCivId(actorCivRaw);
        String targetCiv = normalizeCivId(targetCivRaw);
        if (actorCiv == null || targetCiv == null || actorCiv.equals(targetCiv)) {
            return false;
        }
        if (isOverlordOf(actorCiv, targetCiv)) {
            return true;
        }
        if (isOverlordOf(targetCiv, actorCiv)) {
            return false;
        }
        DiplomacyState relationState = diplomacyState(actorCiv, targetCiv);
        return switch (relationState) {
            case ALLY -> RealCivConfig.allowAllyCivBuildBreak();
            case WAR -> RealCivConfig.allowWarCivBuildBreak() && activeWarAllowsLandDestruction(actorCiv, targetCiv);
            case NEUTRAL -> RealCivConfig.allowNeutralCivBuildBreak();
        };
    }

    private boolean activeWarAllowsLandDestruction(String civAraw, String civBraw) {
        String key = diplomacyKey(civAraw, civBraw);
        if (key == null) {
            return false;
        }
        ActiveWarRecord war = activeWars.get(key);
        if (war == null || war.warType == null) {
            // Legacy wars had no typed record; treat as destruction for compatibility.
            return true;
        }
        return war.warType == WarType.DESTRUCTION;
    }

    public boolean canBuildOnPlot(String civIdRaw, PlotRecord plot, UUID playerId, boolean bypass) {
        if (bypass) {
            return true;
        }
        @Nullable String targetCiv = normalizeCivId(civIdRaw);
        @Nullable String actorCiv = normalizeCivId(playerCivilization.get(playerId));
        if (targetCiv != null
                && actorCiv != null
                && !targetCiv.equals(actorCiv)
                && crossCivBuildBreakAllowed(actorCiv, targetCiv)) {
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
        @Nullable String targetCiv = normalizeCivId(civIdRaw);
        @Nullable String actorCiv = normalizeCivId(playerCivilization.get(playerId));
        if (targetCiv != null
                && actorCiv != null
                && !targetCiv.equals(actorCiv)
                && crossCivBuildBreakAllowed(actorCiv, targetCiv)) {
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




    public record DiplomacyRequestView(
            String requesterCivilizationId,
            String responderCivilizationId,
            DiplomacyState requestedState,
            long createdAtMillis,
            @Nullable WarType warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmount) {
    }

    public record ActiveWarView(
            String firstCivilizationId,
            String secondCivilizationId,
            WarType warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmount,
            long startedAtMillis,
            @Nullable String declaredByCivilizationId,
            long firstCivilizationCasualties,
            long secondCivilizationCasualties) {
    }

    public record WarOutcomeView(
            String winnerCivilizationId,
            String loserCivilizationId,
            WarType warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            int transferredPlotCount,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmountTransferred) {
    }

    public enum WarResignResultType {
        INVALID,
        NOT_AT_WAR,
        RESIGNED
    }

    public record WarResignResult(WarResignResultType type, @Nullable WarOutcomeView outcome) {
    }

    public enum DiplomacyRequestResultType {
        INVALID,
        NO_CHANGE_ALREADY_SET,
        REQUEST_ALREADY_PENDING,
        REQUEST_SENT,
        REQUEST_UPDATED,
        REQUEST_ACCEPTED,
        REQUEST_REJECTED,
        STATE_SET,
        NO_PENDING_REQUEST
    }

    public record DiplomacyRequestResult(DiplomacyRequestResultType type, DiplomacyState requestedState) {
    }

    private static final class DiplomacyRequestRecord {
        @Nullable
        private String requesterCivId;
        @Nullable
        private String responderCivId;
        @Nullable
        private DiplomacyState requestedState;
        private long createdAtMillis;
        @Nullable
        private WarType warType;
        private int pvpKillTarget;
        private boolean warOfSubmission;
        private boolean warOfLand;
        private boolean warResourceGamble;
        @Nullable
        private String warGambleItemId;
        private long warGambleAmount;
    }

    private record DiplomacyPair(String firstCivId, String secondCivId) {
    }

    private static final class ActiveWarRecord {
        @Nullable
        private WarType warType;
        private int pvpKillTarget;
        private boolean warOfSubmission;
        private boolean warOfLand;
        private boolean warResourceGamble;
        @Nullable
        private String warGambleItemId;
        private long warGambleAmount;
        private long startedAtMillis;
        @Nullable
        private String declaredByCivId;
    }

    private static final class WarCasualtyRecord {
        private long firstCasualties;
        private long secondCasualties;
    }





    // PlayerRecord has been extracted to its own file.
    // The players map still lives in this class.


}
