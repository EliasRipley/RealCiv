package com.realciv.realciv.event;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.panel.CivControlPanelMenu;
import com.realciv.realciv.panel.CivGovernanceWorkflowService;
import java.util.List;
import java.util.Locale;
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
                        + ". This panel includes write actions, governance approval paths, and policy templates."));
        player.sendSystemMessage(Component.literal(
                "Tip: left click Census for member management, shift-right click for policy dashboard."));

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
                canManageHubDistribution);
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
}
