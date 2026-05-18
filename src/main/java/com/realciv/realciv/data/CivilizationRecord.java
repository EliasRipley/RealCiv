package com.realciv.realciv.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class CivilizationRecord {
    static final String DEFAULT_LEADER_TITLE = "Mayor";
    private final String id;
    private String displayName;
    private long treasuryCents;
    private final Map<String, Long> hubStock = new HashMap<>();
    private final Map<String, Long> hubLockedForWar = new HashMap<>();
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
    private final Map<AttributeCategory, CivicAttribute> civicAttributes = new HashMap<>();
    private double hubSharedWithdrawRatio = 0.10D;
    private double upkeepRateMultiplier = 1.0D;
    private String taxItemId = "minecraft:gold_nugget";
    private int taxItemCountPerPlot = 1;
    private final Map<String, Integer> hubDailyAllowances = new HashMap<>();
    @Nullable
    private GovernanceProposalRecord governanceProposal;
    @Nullable
    private LeadershipContestRecord leadershipContest;
    @Nullable
    private UUID ftbTeamId;
    private boolean allowIntraCivPvp;
    private boolean starterTownAreaGranted;

    public CivilizationRecord(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        for (AttributeCategory cat : AttributeCategory.values()) {
            civicAttributes.put(cat, CivicAttribute.defaultFor(cat));
        }
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
        this.leaderTitle = CivSavedData.sanitizeLeaderTitle(title);
    }

    public CivicAttribute civicAttribute(AttributeCategory category) {
        return civicAttributes.getOrDefault(category, CivicAttribute.defaultFor(category));
    }

    public void setCivicAttribute(AttributeCategory category, @Nullable CivicAttribute attribute) {
        if (attribute != null && attribute.category() == category) {
            civicAttributes.put(category, attribute);
        }
    }

    public Map<AttributeCategory, CivicAttribute> civicAttributesCopy() {
        return Map.copyOf(civicAttributes);
    }

    public double hubSharedWithdrawRatio() {
        return hubSharedWithdrawRatio;
    }

    public void setHubSharedWithdrawRatio(double ratio) {
        this.hubSharedWithdrawRatio = CivSavedData.clampUnitRatio(ratio);
    }

    public double upkeepRateMultiplier() {
        return upkeepRateMultiplier;
    }

    public void setUpkeepRateMultiplier(double multiplier) {
        this.upkeepRateMultiplier = CivSavedData.clampUpkeepRateMultiplier(multiplier);
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

    public Map<String, Long> hubLockedForWar() {
        return hubLockedForWar;
    }

    public long availableHubStock(String itemId) {
        long stock = hubStock.getOrDefault(itemId, 0L);
        long locked = hubLockedForWar.getOrDefault(itemId, 0L);
        return Math.max(0L, stock - locked);
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

    void removePlayerFromAllRoles(UUID playerId) {
        for (RoleRecord role : customRoles.values()) {
            role.members.remove(playerId);
        }
    }

    @Nullable
    public UUID mayorId() {
        return mayorId;
    }

    public void setMayorId(@Nullable UUID mayorId) {
        this.mayorId = mayorId;
    }

    @Nullable
    public UUID ftbTeamId() {
        return ftbTeamId;
    }

    public void setFtbTeamId(@Nullable UUID ftbTeamId) {
        this.ftbTeamId = ftbTeamId;
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

        CompoundTag lockedTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : hubLockedForWar.entrySet()) {
            lockedTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
        }
        tag.put("hubLockedForWar", lockedTag);

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
        if (ftbTeamId != null) {
            tag.putString("ftbTeamId", ftbTeamId.toString());
        }
        if (hubDimension != null) {
            tag.putString("hubDimension", hubDimension);
            tag.putInt("hubX", hubX);
            tag.putInt("hubY", hubY);
            tag.putInt("hubZ", hubZ);
        }
        tag.putString("leaderTitle", CivSavedData.sanitizeLeaderTitle(leaderTitle));
        CompoundTag attrTag = new CompoundTag();
        for (Map.Entry<AttributeCategory, CivicAttribute> entry : civicAttributes.entrySet()) {
            attrTag.putString(entry.getKey().serializedName(), entry.getValue().serializedName());
        }
        tag.put("civicAttributes", attrTag);
        tag.putDouble("hubSharedWithdrawRatio", CivSavedData.clampUnitRatio(hubSharedWithdrawRatio));
        tag.putDouble("upkeepRateMultiplier", CivSavedData.clampUpkeepRateMultiplier(upkeepRateMultiplier));
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
        String id = CivSavedData.normalizeCivId(tag.getString("id"));
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

        CompoundTag lockedTag = tag.getCompound("hubLockedForWar");
        for (String key : lockedTag.getAllKeys()) {
            record.hubLockedForWar.put(key, Math.max(0L, lockedTag.getLong(key)));
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
        if (tag.contains("ftbTeamId")) {
            try {
                record.ftbTeamId = UUID.fromString(tag.getString("ftbTeamId"));
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
            record.leaderTitle = CivSavedData.sanitizeLeaderTitle(tag.getString("leaderTitle"));
        }

        // Load civic attributes — try new format first, fall back to old individual fields
        if (tag.contains("civicAttributes", Tag.TAG_COMPOUND)) {
            CompoundTag attrTag = tag.getCompound("civicAttributes");
            for (String catKey : attrTag.getAllKeys()) {
                @Nullable AttributeCategory cat = AttributeCategory.fromSerializedName(catKey);
                if (cat == null) continue;
                @Nullable CivicAttribute attr = CivicAttribute.fromSerializedName(attrTag.getString(catKey));
                if (attr != null) {
                    record.civicAttributes.put(cat, attr);
                }
            }
        } else {
            // Backward compat: migrate old individual enum fields (inline parsing, no enum dependency)
            if (tag.contains("governanceModel")) {
                String raw = tag.getString("governanceModel").trim().toUpperCase(Locale.ROOT);
                @Nullable CivicAttribute attr = switch (raw) {
                    case "AUTOCRATIC", "AUTOCRACY", "AUTO" -> CivicAttribute.DIRECT_RULE;
                    case "COUNCIL", "OLIGARCHY" -> CivicAttribute.COUNCIL_VOTE;
                    case "DEMOCRATIC", "DEMOCRACY", "DEMO" -> CivicAttribute.POPULAR_VOTE;
                    default -> null;
                };
                if (attr != null) record.civicAttributes.put(AttributeCategory.EXECUTIVE, attr);
            }
            if (tag.contains("hubDistributionMode")) {
                String raw = tag.getString("hubDistributionMode").trim().toUpperCase(Locale.ROOT);
                @Nullable CivicAttribute attr = switch (raw) {
                    case "CONTRIBUTION_RATIO", "RATIO", "CONTRIBUTION", "DEFAULT" -> CivicAttribute.CONTRIBUTION_SHARE;
                    case "SHARED_STOCK_RATIO", "SHARED", "GLOBAL", "ALL_GOODS", "STOCK_RATIO" -> CivicAttribute.EQUAL_SHARE;
                    case "DAILY_ALLOWANCE", "ALLOWANCE", "DAILY" -> CivicAttribute.RATIONED;
                    default -> null;
                };
                if (attr != null) record.civicAttributes.put(AttributeCategory.RESOURCE, attr);
            }
            if (tag.contains("taxPaymentMode", Tag.TAG_STRING)) {
                String raw = tag.getString("taxPaymentMode").trim().toUpperCase(Locale.ROOT);
                @Nullable CivicAttribute attr = switch (raw) {
                    case "KARMA", "CREDIT", "CREDITS", "COMMUNITY_KARMA" -> CivicAttribute.KARMA_TAX;
                    case "ITEM", "ITEMS", "GOODS" -> CivicAttribute.GOODS_TAX;
                    default -> null;
                };
                if (attr != null) record.civicAttributes.put(AttributeCategory.TAXATION, attr);
            }
        }

        if (tag.contains("hubSharedWithdrawRatio")) {
            record.hubSharedWithdrawRatio = CivSavedData.clampUnitRatio(tag.getDouble("hubSharedWithdrawRatio"));
        }
        if (tag.contains("upkeepRateMultiplier")) {
            record.upkeepRateMultiplier = CivSavedData.clampUpkeepRateMultiplier(tag.getDouble("upkeepRateMultiplier"));
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

    static final class RoleRecord {
        final String roleId;
        String displayName;
        final Set<String> permissions = new HashSet<>();
        final Set<UUID> members = new HashSet<>();

        RoleRecord(String roleId, String displayName) {
            this.roleId = roleId;
            this.displayName = displayName;
        }

        String roleId() {
            return roleId;
        }

        String displayName() {
            return displayName;
        }

        void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        Set<String> permissions() {
            return permissions;
        }

        Set<UUID> members() {
            return members;
        }

        CompoundTag save() {
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
        static RoleRecord load(CompoundTag tag) {
            @Nullable String roleId = CivSavedData.normalizeRoleId(tag.getString("roleId"));
            if (roleId == null) {
                return null;
            }
            String displayName = CivSavedData.sanitizeRoleDisplayName(tag.contains("displayName")
                    ? tag.getString("displayName")
                    : roleId);
            RoleRecord record = new RoleRecord(roleId, displayName);

            ListTag permissionTags = tag.getList("permissions", Tag.TAG_STRING);
            for (Tag permissionTag : permissionTags) {
                @Nullable String permission = CivSavedData.normalizePermissionKey(permissionTag.getAsString());
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
