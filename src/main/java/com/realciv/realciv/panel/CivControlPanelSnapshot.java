package com.realciv.realciv.panel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record CivControlPanelSnapshot(
        String civilizationId,
        String civilizationName,
        String leaderTitle,
        String executiveAttribute,
        String successionAttribute,
        String resourceAttribute,
        String taxationAttribute,
        String membershipAttribute,
        String landAttribute,
        String claimDimensionPolicy,
        String playerRole,
        String pendingProposalSummary,
        int pendingProposalYesVotes,
        int pendingProposalRequiredYesVotes,
        int memberCount,
        int managerCount,
        int customRoleCount,
        int joinRequestCount,
        int inviteCount,
        int plotTotalCount,
        int plotCivicCount,
        int plotPrivateCount,
        int plotCommunityCount,
        int hubStockEntryCount,
        int hubDailyAllowanceCount,
        int professionEventHookRuleCount,
        int hunterMobActionCapCount,
        long civTreasuryCents,
        long playerContributionKarmaCents,
        long playerContributedItemsTotal,
        long maxContributionKarmaGainPerDayCents,
        boolean governanceApprovalWorkflowEnabled,
        boolean warriorRequireHubRegistration,
        boolean warriorHomeDefenseNoActionCost,
        boolean allowIntraCivPvp,
        boolean canManageGovernance,
        boolean canManageCensus,
        boolean canManageHubDistribution,
        String leadershipContestType,
        String leadershipContestSummary,
        String leadershipCoupLeaderName,
        int leadershipCandidateCount,
        int leadershipElectionVoteCount,
        int leadershipCoupApprovalCount,
        int leadershipCoupRequiredApprovals,
        long leadershipContestEndsAtMillis,
        List<String> leadershipCandidateEntries) {

    private static final int MAX_ID_LEN = 64;
    private static final int MAX_NAME_LEN = 96;
    private static final int MAX_TITLE_LEN = 128;
    private static final int MAX_SUMMARY_LEN = 256;
    private static final int MAX_CANDIDATE_ENTRY_LEN = 160;

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(civilizationId, MAX_ID_LEN);
        buffer.writeUtf(civilizationName, MAX_NAME_LEN);
        buffer.writeUtf(leaderTitle, MAX_TITLE_LEN);
        buffer.writeUtf(executiveAttribute, MAX_TITLE_LEN);
        buffer.writeUtf(successionAttribute, MAX_TITLE_LEN);
        buffer.writeUtf(resourceAttribute, MAX_TITLE_LEN);
        buffer.writeUtf(taxationAttribute, MAX_TITLE_LEN);
        buffer.writeUtf(membershipAttribute, MAX_TITLE_LEN);
        buffer.writeUtf(landAttribute, MAX_TITLE_LEN);
        buffer.writeUtf(claimDimensionPolicy, MAX_TITLE_LEN);
        buffer.writeUtf(playerRole, MAX_TITLE_LEN);
        buffer.writeUtf(pendingProposalSummary, MAX_TITLE_LEN);
        buffer.writeInt(pendingProposalYesVotes);
        buffer.writeInt(pendingProposalRequiredYesVotes);
        buffer.writeInt(memberCount);
        buffer.writeInt(managerCount);
        buffer.writeInt(customRoleCount);
        buffer.writeInt(joinRequestCount);
        buffer.writeInt(inviteCount);
        buffer.writeInt(plotTotalCount);
        buffer.writeInt(plotCivicCount);
        buffer.writeInt(plotPrivateCount);
        buffer.writeInt(plotCommunityCount);
        buffer.writeInt(hubStockEntryCount);
        buffer.writeInt(hubDailyAllowanceCount);
        buffer.writeInt(professionEventHookRuleCount);
        buffer.writeInt(hunterMobActionCapCount);
        buffer.writeLong(civTreasuryCents);
        buffer.writeLong(playerContributionKarmaCents);
        buffer.writeLong(playerContributedItemsTotal);
        buffer.writeLong(maxContributionKarmaGainPerDayCents);
        buffer.writeBoolean(governanceApprovalWorkflowEnabled);
        buffer.writeBoolean(warriorRequireHubRegistration);
        buffer.writeBoolean(warriorHomeDefenseNoActionCost);
        buffer.writeBoolean(allowIntraCivPvp);
        buffer.writeBoolean(canManageGovernance);
        buffer.writeBoolean(canManageCensus);
        buffer.writeBoolean(canManageHubDistribution);
        buffer.writeUtf(leadershipContestType, MAX_TITLE_LEN);
        buffer.writeUtf(leadershipContestSummary, MAX_SUMMARY_LEN);
        buffer.writeUtf(leadershipCoupLeaderName, MAX_TITLE_LEN);
        buffer.writeInt(leadershipCandidateCount);
        buffer.writeInt(leadershipElectionVoteCount);
        buffer.writeInt(leadershipCoupApprovalCount);
        buffer.writeInt(leadershipCoupRequiredApprovals);
        buffer.writeLong(leadershipContestEndsAtMillis);
        List<String> entries = leadershipCandidateEntries == null ? List.of() : leadershipCandidateEntries;
        buffer.writeInt(entries.size());
        for (String entry : entries) {
            buffer.writeUtf(entry == null ? "" : entry, MAX_CANDIDATE_ENTRY_LEN);
        }
    }

    public static CivControlPanelSnapshot read(RegistryFriendlyByteBuf buffer) {
        return new CivControlPanelSnapshot(
                buffer.readUtf(MAX_ID_LEN),
                buffer.readUtf(MAX_NAME_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readUtf(MAX_SUMMARY_LEN),
                buffer.readUtf(MAX_TITLE_LEN),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readLong(),
                readCandidateEntries(buffer));
    }

    private static List<String> readCandidateEntries(RegistryFriendlyByteBuf buffer) {
        int declaredSize = Math.max(0, Math.min(32, buffer.readInt()));
        List<String> entries = new ArrayList<>(declaredSize);
        for (int i = 0; i < declaredSize; i++) {
            entries.add(buffer.readUtf(MAX_CANDIDATE_ENTRY_LEN));
        }
        return List.copyOf(entries);
    }
}
