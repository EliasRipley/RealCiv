package com.realciv.realciv.event;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.panel.CivControlPanelMenu;
import com.realciv.realciv.panel.CivGovernanceWorkflowService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public final class CivControlPanelEvents {
    private CivControlPanelEvents() {
    }

    public static void openControlPanel(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivControlPanelMenu.Snapshot snapshot = buildSnapshot(player, data, civId);
        String title = "Control Panel: " + civilizationDisplayName(data, civId);
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, playerInventory, p) ->
                                new CivControlPanelMenu(containerId, playerInventory, snapshot, player, data, civId),
                        Component.literal(title)),
                snapshot::write);
        player.sendSystemMessage(Component.literal(
                "Control Panel opened for " + civilizationDisplayName(data, civId)
                        + ". This panel includes governance controls plus leadership election/coup actions."));
        player.sendSystemMessage(Component.literal(
                "Tip: use Census for membership, Tax Office for upkeep, Profession Ledger for leveling, and Diplomacy Table for relations."));
        player.sendSystemMessage(Component.literal(
                "Role management tip: when no election is active, the vote-row actions are used for role create/select/member/permission controls."));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static CivControlPanelMenu.Snapshot buildSnapshot(ServerPlayer player, CivSavedData data, String civId) {
        // Keep snapshot generation deterministic: it must mirror server authority so clients never author policy state.
        CivSavedData.PlayerRecord playerRecord = data.getOrCreatePlayer(player.getUUID());
        List<UUID> members = data.civilizationMembersSorted(civId);
        int managerCount = 0;
        for (UUID memberId : members) {
            if (data.isCivicManager(civId, memberId)) {
                managerCount++;
            }
        }

        int civicPlots = data.countPlotsByClass(civId, LandClass.CIVIC);
        int privatePlots = data.countPlotsByClass(civId, LandClass.PRIVATE);
        int communityPlots = data.countPlotsByClass(civId, LandClass.COMMUNITY);
        int totalPlots = data.plotCount(civId);

        boolean canManageGovernance = CivPermissionService.hasCivPermission(
                player,
                data,
                civId,
                CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
        boolean canManageCensus = CivPermissionService.hasCivPermission(
                player,
                data,
                civId,
                CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)
                || data.isCivicManager(civId, player.getUUID());
        boolean canManageHubDistribution = CivPermissionService.hasCivPermission(
                player,
                data,
                civId,
                CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);

        String playerRole = resolveRoleLabel(data, civId, player.getUUID());
        String leaderTitle = data.leaderTitle(civId);
        String governance = humanizeSerialized(data.governanceModel(civId).serializedName());
        String distribution = humanizeSerialized(data.hubDistributionMode(civId).serializedName());
        String claimPolicy = RealCivConfig.claimDimensionPolicyLabel();

        @Nullable CivGovernanceWorkflowService.ProposalView proposal = CivGovernanceWorkflowService.proposalView(data, civId);
        String proposalSummary = proposal == null ? "No pending proposal" : proposal.summary();
        int proposalYes = proposal == null ? 0 : proposal.yesVotes();
        int proposalRequired = proposal == null ? 0 : proposal.requiredYesVotes();

        String leadershipContestType = "None";
        String leadershipContestSummary = "No active leadership contest";
        String leadershipCoupLeaderName = "-";
        int leadershipCandidateCount = 0;
        int leadershipElectionVoteCount = 0;
        int leadershipCoupApprovalCount = 0;
        int leadershipCoupRequiredApprovals = 0;
        long leadershipContestEndsAtMillis = 0L;
        List<String> leadershipCandidateEntries = List.of();

        @Nullable CivSavedData.LeadershipContestRecord leadershipContest = data.leadershipContest(civId);
        if (leadershipContest != null) {
            leadershipContestEndsAtMillis = Math.max(0L, leadershipContest.expiresAtMillis());
            Set<UUID> memberSet = new HashSet<>(members);
            if (leadershipContest.contestType() == CivSavedData.LeadershipContestType.ELECTION) {
                leadershipContestType = "Election";
                Map<UUID, Integer> votesByCandidate = new HashMap<>();
                int validVotes = 0;
                for (Map.Entry<UUID, UUID> vote : leadershipContest.electionVotes().entrySet()) {
                    UUID voterId = vote.getKey();
                    UUID candidateId = vote.getValue();
                    if (!memberSet.contains(voterId)) {
                        continue;
                    }
                    if (!memberSet.contains(candidateId) || !leadershipContest.candidates().contains(candidateId)) {
                        continue;
                    }
                    validVotes++;
                    votesByCandidate.merge(candidateId, 1, Integer::sum);
                }
                leadershipElectionVoteCount = validVotes;

                List<UUID> candidates = leadershipContest.candidates().stream()
                        .filter(memberSet::contains)
                        .sorted(java.util.Comparator.comparing(UUID::toString))
                        .toList();
                leadershipCandidateCount = candidates.size();

                List<String> candidateRows = new ArrayList<>();
                int maxEntries = Math.min(5, candidates.size());
                for (int index = 0; index < maxEntries; index++) {
                    UUID candidateId = candidates.get(index);
                    int votes = votesByCandidate.getOrDefault(candidateId, 0);
                    String label = playerDisplayName(player, candidateId)
                            + " (" + votes + " vote" + (votes == 1 ? "" : "s") + ")";
                    candidateRows.add(candidateId + "|" + label);
                }
                leadershipCandidateEntries = List.copyOf(candidateRows);
                leadershipContestSummary = "Election in progress: "
                        + leadershipCandidateCount + " candidate(s), "
                        + leadershipElectionVoteCount + "/" + members.size() + " vote(s) cast.";
            } else {
                leadershipContestType = "Coup";
                int approvals = 0;
                for (UUID voterId : leadershipContest.coupApprovals()) {
                    if (memberSet.contains(voterId)) {
                        approvals++;
                    }
                }
                leadershipCoupApprovalCount = approvals;
                leadershipCoupRequiredApprovals = Math.max(1, (int) Math.ceil(Math.max(1, members.size()) * 0.51D));
                @Nullable UUID coupLeaderId = leadershipContest.coupLeaderId();
                if (coupLeaderId != null) {
                    leadershipCoupLeaderName = playerDisplayName(player, coupLeaderId);
                }
                leadershipContestSummary = "Coup in progress: "
                        + approvals + "/" + leadershipCoupRequiredApprovals + " approval(s).";
            }
        }

        return new CivControlPanelMenu.Snapshot(
                civId,
                civilizationDisplayName(data, civId),
                leaderTitle,
                governance,
                distribution,
                claimPolicy,
                playerRole,
                proposalSummary,
                proposalYes,
                proposalRequired,
                members.size(),
                managerCount,
                data.customRolesSorted(civId).size(),
                data.joinRequestsSorted(civId).size(),
                data.invitedPlayersSorted(civId).size(),
                totalPlots,
                civicPlots,
                privatePlots,
                communityPlots,
                data.getHubStockEntriesSorted(civId).size(),
                data.hubDailyAllowanceEntriesSorted(civId).size(),
                RealCivConfig.PROFESSION_EVENT_HOOK_RULES.get().size(),
                RealCivConfig.HUNTER_MOB_ACTION_CAPS.get().size(),
                data.civTreasuryCents(civId),
                playerRecord.socialCreditCents(civId),
                totalContributions(playerRecord, civId),
                RealCivConfig.maxContributionKarmaGainPerDayCents(),
                RealCivConfig.governanceApprovalWorkflowEnabled(),
                RealCivConfig.warriorRequireHubRegistration(),
                RealCivConfig.warriorHomeDefenseNoActionCost(),
                data.allowIntraCivPvp(civId),
                canManageGovernance,
                canManageCensus,
                canManageHubDistribution,
                leadershipContestType,
                leadershipContestSummary,
                leadershipCoupLeaderName,
                leadershipCandidateCount,
                leadershipElectionVoteCount,
                leadershipCoupApprovalCount,
                leadershipCoupRequiredApprovals,
                leadershipContestEndsAtMillis,
                leadershipCandidateEntries);
    }

    private static long totalContributions(CivSavedData.PlayerRecord record, String civId) {
        long sum = 0L;
        for (long value : record.contributions(civId).values()) {
            sum += Math.max(0L, value);
        }
        return sum;
    }

    private static String resolveRoleLabel(CivSavedData data, String civId, UUID playerId) {
        if (data.isMayor(civId, playerId)) {
            return data.leaderTitle(civId);
        }
        if (data.isCivicManager(civId, playerId)) {
            return "Civic Manager";
        }
        return "Citizen";
    }

    private static String humanizeSerialized(String serialized) {
        String raw = serialized == null ? "" : serialized.trim();
        if (raw.isEmpty()) {
            return "-";
        }
        String[] words = raw.split("_");
        StringBuilder out = new StringBuilder(raw.length() + words.length * 2);
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            if (words[i].isEmpty()) {
                continue;
            }
            out.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) {
                out.append(words[i].substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }

    private static String civilizationDisplayName(CivSavedData data, String civId) {
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
        if (civ == null) {
            return civId;
        }
        return civ.displayName();
    }

    private static String playerDisplayName(ServerPlayer viewer, UUID playerId) {
        if (viewer.getServer() != null) {
            @Nullable ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(playerId);
            if (online != null) {
                return online.getGameProfile().getName();
            }
        }
        String raw = playerId.toString();
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }
}
