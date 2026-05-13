package com.realciv.realciv.panel;

import com.realciv.realciv.ModMenus;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CivPermissionService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CivControlPanelMenu extends AbstractContainerMenu {
    private final Snapshot snapshot;
    @Nullable
    private final ServerPlayer serverViewer;
    @Nullable
    private final CivSavedData serverData;
    @Nullable
    private final String serverCivId;
    private int roleCursor;
    private int memberCursor;
    private int permissionCursor;

    public CivControlPanelMenu(int containerId, Inventory playerInventory, Snapshot snapshot) {
        this(containerId, playerInventory, snapshot, null, null, null);
    }

    public CivControlPanelMenu(
            int containerId,
            Inventory playerInventory,
            Snapshot snapshot,
            @Nullable ServerPlayer serverViewer,
            @Nullable CivSavedData serverData,
            @Nullable String serverCivId) {
        super(ModMenus.CIV_CONTROL_PANEL.get(), containerId);
        this.snapshot = snapshot;
        this.serverViewer = serverViewer;
        this.serverData = serverData;
        this.serverCivId = serverCivId;
    }

    public static CivControlPanelMenu fromBuffer(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        return new CivControlPanelMenu(containerId, playerInventory, Snapshot.read(buffer));
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverData == null || serverCivId == null || serverViewer == null) {
            return false;
        }
        if (!serverPlayer.getUUID().equals(serverViewer.getUUID())) {
            return false;
        }

        switch (id) {
            case CivControlPanelActionIds.PROPOSAL_VOTE_YES -> {
                applyProposalVote(serverPlayer, true);
                return true;
            }
            case CivControlPanelActionIds.PROPOSAL_VOTE_NO -> {
                applyProposalVote(serverPlayer, false);
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_START_ELECTION -> {
                applyLeadershipStartElection(serverPlayer);
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_JOIN_ELECTION -> {
                if (isElectionActive()) {
                    applyLeadershipJoinElection(serverPlayer);
                } else {
                    applyRoleToggleMember(serverPlayer);
                }
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_START_COUP_SELF -> {
                applyLeadershipStartCoupSelf(serverPlayer);
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_APPROVE_COUP -> {
                if (isCoupActive()) {
                    applyLeadershipApproveCoup(serverPlayer);
                } else {
                    applyRoleTogglePermission(serverPlayer);
                }
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_1 -> {
                if (isElectionActive()) {
                    applyLeadershipVoteCandidate(serverPlayer, 0);
                } else {
                    applyRoleCreate(serverPlayer);
                }
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_2 -> {
                if (isElectionActive()) {
                    applyLeadershipVoteCandidate(serverPlayer, 1);
                } else {
                    applyRoleSelectNext(serverPlayer);
                }
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_3 -> {
                if (isElectionActive()) {
                    applyLeadershipVoteCandidate(serverPlayer, 2);
                } else {
                    applyRoleSelectMemberNext(serverPlayer);
                }
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_4 -> {
                if (isElectionActive()) {
                    applyLeadershipVoteCandidate(serverPlayer, 3);
                } else {
                    applyRoleSelectPermissionNext(serverPlayer);
                }
                return true;
            }
            case CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_5 -> {
                if (isElectionActive()) {
                    applyLeadershipVoteCandidate(serverPlayer, 4);
                } else {
                    applyRoleToggleMember(serverPlayer);
                }
                return true;
            }
            default -> {
            }
        }

        @Nullable CivGovernanceWorkflowService.PanelAction action = panelActionForButton(serverPlayer, id);
        if (action == null) {
            return false;
        }
        CivGovernanceWorkflowService.Decision decision =
                CivGovernanceWorkflowService.requestAction(serverPlayer, serverData, serverCivId, action);
        if (decision.actionToApply() != null) {
            applyPanelAction(serverPlayer, decision.actionToApply());
        }
        serverPlayer.sendSystemMessage(Component.literal("[RealCiv] " + decision.message()));
        return true;
    }

    private void applyProposalVote(ServerPlayer serverPlayer, boolean yes) {
        CivGovernanceWorkflowService.Decision decision =
                CivGovernanceWorkflowService.vote(serverPlayer, serverData, serverCivId, yes);
        if (decision.actionToApply() != null) {
            applyPanelAction(serverPlayer, decision.actionToApply());
        }
        serverPlayer.sendSystemMessage(Component.literal("[RealCiv] " + decision.message()));
    }

    private void applyLeadershipStartElection(ServerPlayer actor) {
        CivSavedData.LeadershipContestDecision decision = serverData.startLeadershipElection(
                serverCivId,
                actor.getUUID(),
                actor.getGameProfile().getName());
        actor.sendSystemMessage(Component.literal("[RealCiv] " + decision.message()));
    }

    private void applyLeadershipJoinElection(ServerPlayer actor) {
        CivSavedData.LeadershipContestDecision decision = serverData.joinLeadershipElectionCandidate(
                serverCivId,
                actor.getUUID(),
                actor.getGameProfile().getName());
        actor.sendSystemMessage(Component.literal("[RealCiv] " + decision.message()));
    }

    private void applyLeadershipStartCoupSelf(ServerPlayer actor) {
        CivSavedData.LeadershipContestDecision decision = serverData.startLeadershipCoup(
                serverCivId,
                actor.getUUID(),
                actor.getUUID(),
                actor.getGameProfile().getName());
        actor.sendSystemMessage(Component.literal("[RealCiv] " + decision.message()));
    }

    private void applyLeadershipApproveCoup(ServerPlayer actor) {
        CivSavedData.LeadershipContestDecision decision = serverData.approveLeadershipCoup(
                serverCivId,
                actor.getUUID(),
                actor.getGameProfile().getName());
        actor.sendSystemMessage(Component.literal("[RealCiv] " + decision.message()));
    }

    private void applyLeadershipVoteCandidate(ServerPlayer actor, int candidateIndex) {
        @Nullable UUID candidateId = candidateIdForSnapshotIndex(candidateIndex);
        if (candidateId == null) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No candidate is assigned to that vote slot."));
            return;
        }
        CivSavedData.LeadershipContestDecision decision = serverData.voteLeadershipElectionCandidate(
                serverCivId,
                actor.getUUID(),
                candidateId,
                actor.getGameProfile().getName());
        actor.sendSystemMessage(Component.literal("[RealCiv] " + decision.message()));
    }

    private boolean isElectionActive() {
        if (serverData == null || serverCivId == null) {
            return false;
        }
        @Nullable CivSavedData.LeadershipContestRecord contest = serverData.leadershipContest(serverCivId);
        return contest != null && contest.contestType() == CivSavedData.LeadershipContestType.ELECTION;
    }

    private boolean isCoupActive() {
        if (serverData == null || serverCivId == null) {
            return false;
        }
        @Nullable CivSavedData.LeadershipContestRecord contest = serverData.leadershipContest(serverCivId);
        return contest != null && contest.contestType() == CivSavedData.LeadershipContestType.COUP;
    }

    private boolean ensureRoleManagementPermission(ServerPlayer actor) {
        if (serverData == null || serverCivId == null) {
            return false;
        }
        if (!CivPermissionService.hasCivPermission(
                actor,
                serverData,
                serverCivId,
                CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS_ROLES)) {
            actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage civ roles."));
            return false;
        }
        return true;
    }

    private List<CivSavedData.CivRoleView> customRoles() {
        if (serverData == null || serverCivId == null) {
            return List.of();
        }
        return serverData.customRolesSorted(serverCivId);
    }

    @Nullable
    private CivSavedData.CivRoleView selectedRole() {
        List<CivSavedData.CivRoleView> roles = customRoles();
        if (roles.isEmpty()) {
            return null;
        }
        roleCursor = Math.floorMod(roleCursor, roles.size());
        return roles.get(roleCursor);
    }

    private List<UUID> civilizationMembers() {
        if (serverData == null || serverCivId == null) {
            return List.of();
        }
        return serverData.civilizationMembersSorted(serverCivId);
    }

    @Nullable
    private UUID selectedMemberId() {
        List<UUID> members = civilizationMembers();
        if (members.isEmpty()) {
            return null;
        }
        memberCursor = Math.floorMod(memberCursor, members.size());
        return members.get(memberCursor);
    }

    private String selectedPermissionKey() {
        List<String> permissions = CivSavedData.knownRolePermissions();
        if (permissions.isEmpty()) {
            return "";
        }
        permissionCursor = Math.floorMod(permissionCursor, permissions.size());
        return permissions.get(permissionCursor);
    }

    private String playerLabel(UUID playerId) {
        if (serverViewer.getServer() != null) {
            @Nullable ServerPlayer online = serverViewer.getServer().getPlayerList().getPlayer(playerId);
            if (online != null) {
                return online.getGameProfile().getName();
            }
        }
        String raw = playerId.toString();
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }

    private void applyRoleCreate(ServerPlayer actor) {
        if (!ensureRoleManagementPermission(actor) || serverData == null || serverCivId == null) {
            return;
        }
        String createdRoleId = null;
        String createdRoleName = null;
        for (int i = 1; i <= 99; i++) {
            String roleId = String.format(java.util.Locale.ROOT, "role_%02d", i);
            if (serverData.customRoleExists(serverCivId, roleId)) {
                continue;
            }
            String roleName = "Role " + String.format(java.util.Locale.ROOT, "%02d", i);
            if (serverData.createCustomRole(serverCivId, roleId, roleName, actor.getGameProfile().getName())) {
                createdRoleId = roleId;
                createdRoleName = roleName;
                break;
            }
        }
        if (createdRoleId == null) {
            actor.sendSystemMessage(Component.literal(
                    "[RealCiv] Could not create a new role (all default role slots are already used)."));
            return;
        }
        List<CivSavedData.CivRoleView> roles = customRoles();
        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).roleId().equals(createdRoleId)) {
                roleCursor = i;
                break;
            }
        }
        actor.sendSystemMessage(Component.literal(
                "[RealCiv] Created role '" + createdRoleName + "' [" + createdRoleId + "]."));
    }

    private void applyRoleSelectNext(ServerPlayer actor) {
        if (!ensureRoleManagementPermission(actor)) {
            return;
        }
        List<CivSavedData.CivRoleView> roles = customRoles();
        if (roles.isEmpty()) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No custom roles exist yet. Use role-create action first."));
            return;
        }
        roleCursor = Math.floorMod(roleCursor + 1, roles.size());
        CivSavedData.CivRoleView selected = roles.get(roleCursor);
        actor.sendSystemMessage(Component.literal(
                "[RealCiv] Selected role: '" + selected.displayName() + "' [" + selected.roleId() + "]"
                        + " | members " + selected.members().size()
                        + " | permissions " + selected.permissions().size()));
    }

    private void applyRoleSelectMemberNext(ServerPlayer actor) {
        if (!ensureRoleManagementPermission(actor)) {
            return;
        }
        List<UUID> members = civilizationMembers();
        if (members.isEmpty()) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No civilization members are available."));
            return;
        }
        memberCursor = Math.floorMod(memberCursor + 1, members.size());
        UUID memberId = members.get(memberCursor);
        actor.sendSystemMessage(Component.literal(
                "[RealCiv] Selected member: " + playerLabel(memberId)));
    }

    private void applyRoleSelectPermissionNext(ServerPlayer actor) {
        if (!ensureRoleManagementPermission(actor)) {
            return;
        }
        List<String> permissions = CivSavedData.knownRolePermissions();
        if (permissions.isEmpty()) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No known role permissions are registered."));
            return;
        }
        permissionCursor = Math.floorMod(permissionCursor + 1, permissions.size());
        actor.sendSystemMessage(Component.literal(
                "[RealCiv] Selected permission: " + selectedPermissionKey()));
    }

    private void applyRoleToggleMember(ServerPlayer actor) {
        if (!ensureRoleManagementPermission(actor) || serverData == null || serverCivId == null) {
            return;
        }
        @Nullable CivSavedData.CivRoleView role = selectedRole();
        if (role == null) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No role selected. Create/select a role first."));
            return;
        }
        @Nullable UUID memberId = selectedMemberId();
        if (memberId == null) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No member selected."));
            return;
        }
        boolean currentlyAssigned = role.members().contains(memberId);
        if (!serverData.setCustomRoleMember(
                serverCivId,
                role.roleId(),
                memberId,
                !currentlyAssigned,
                actor.getGameProfile().getName())) {
            actor.sendSystemMessage(Component.literal("[RealCiv] Role membership did not change."));
            return;
        }
        actor.sendSystemMessage(Component.literal(
                "[RealCiv] " + (currentlyAssigned ? "Removed " : "Assigned ")
                        + playerLabel(memberId)
                        + (currentlyAssigned ? " from " : " to ")
                        + "role '" + role.displayName() + "'."));
    }

    private void applyRoleTogglePermission(ServerPlayer actor) {
        if (!ensureRoleManagementPermission(actor) || serverData == null || serverCivId == null) {
            return;
        }
        @Nullable CivSavedData.CivRoleView role = selectedRole();
        if (role == null) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No role selected. Create/select a role first."));
            return;
        }
        String permission = selectedPermissionKey();
        if (permission.isBlank()) {
            actor.sendSystemMessage(Component.literal("[RealCiv] No permission selected."));
            return;
        }
        boolean currentlyAllowed = role.permissions().contains(permission);
        if (!serverData.setCustomRolePermission(
                serverCivId,
                role.roleId(),
                permission,
                !currentlyAllowed,
                actor.getGameProfile().getName())) {
            actor.sendSystemMessage(Component.literal("[RealCiv] Role permission did not change."));
            return;
        }
        actor.sendSystemMessage(Component.literal(
                "[RealCiv] " + (currentlyAllowed ? "Revoked " : "Granted ")
                        + "permission '" + permission
                        + "' " + (currentlyAllowed ? "from " : "to ")
                        + "role '" + role.displayName() + "'."));
    }

    @Nullable
    private UUID candidateIdForSnapshotIndex(int candidateIndex) {
        List<String> entries = snapshot.leadershipCandidateEntries();
        if (candidateIndex < 0 || candidateIndex >= entries.size()) {
            return null;
        }
        String entry = entries.get(candidateIndex);
        int split = entry.indexOf('|');
        String rawId = split < 0 ? entry : entry.substring(0, split);
        try {
            return UUID.fromString(rawId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyPanelAction(ServerPlayer actor, CivGovernanceWorkflowService.PanelAction action) {
        String actorName = actor.getGameProfile().getName();
        switch (action.type()) {
            case "governance_model" -> {
                @Nullable CivSavedData.GovernanceModel model = CivSavedData.GovernanceModel.fromSerializedName(action.payload());
                if (model == null) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] Invalid governance model payload: " + action.payload()));
                    return;
                }
                boolean changed = serverData.setGovernanceModel(serverCivId, model, actorName);
                actor.sendSystemMessage(Component.literal("[RealCiv] Governance model "
                        + (changed ? "updated to " + model.serializedName() : "already set to " + model.serializedName()) + "."));
            }
            case "distribution_mode" -> {
                @Nullable CivSavedData.HubDistributionMode mode = CivSavedData.HubDistributionMode.fromSerializedName(action.payload());
                if (mode == null) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] Invalid distribution mode payload: " + action.payload()));
                    return;
                }
                boolean changed = serverData.setHubDistributionMode(serverCivId, mode, actorName);
                actor.sendSystemMessage(Component.literal("[RealCiv] Hub distribution mode "
                        + (changed ? "updated to " + mode.serializedName() : "already set to " + mode.serializedName()) + "."));
            }
            case "friendly_fire" -> {
                boolean enabled = Boolean.parseBoolean(action.payload());
                boolean changed = serverData.setAllowIntraCivPvp(serverCivId, enabled, actorName);
                actor.sendSystemMessage(Component.literal("[RealCiv] Intra-civ PvP "
                        + (changed ? (enabled ? "enabled." : "disabled.") : "already in that state.")));
            }
            default -> actor.sendSystemMessage(Component.literal("[RealCiv] Unsupported panel action: " + action.type()));
        }
    }

    private @Nullable CivGovernanceWorkflowService.PanelAction panelActionForButton(ServerPlayer actor, int id) {
        String civId = serverCivId;
        CivSavedData data = serverData;
        if (civId == null || data == null) {
            return null;
        }
        return switch (id) {
            case CivControlPanelActionIds.GOVERNANCE_CYCLE -> {
                if (!CivPermissionService.hasCivPermission(actor, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage governance."));
                    yield null;
                }
                CivSavedData.GovernanceModel current = data.governanceModel(civId);
                CivSavedData.GovernanceModel next = switch (current) {
                    case AUTOCRATIC -> CivSavedData.GovernanceModel.COUNCIL;
                    case COUNCIL -> CivSavedData.GovernanceModel.DEMOCRATIC;
                    case DEMOCRATIC -> CivSavedData.GovernanceModel.AUTOCRATIC;
                };
                yield new CivGovernanceWorkflowService.PanelAction(
                        "governance_model",
                        next.serializedName(),
                        "Set governance model to " + next.serializedName(),
                        CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
            }
            case CivControlPanelActionIds.DISTRIBUTION_TOGGLE -> {
                if (!CivPermissionService.hasCivPermission(actor, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage hub distribution."));
                    yield null;
                }
                CivSavedData.HubDistributionMode current = data.hubDistributionMode(civId);
                CivSavedData.HubDistributionMode next = switch (current) {
                    case CONTRIBUTION_RATIO -> CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO;
                    case SHARED_STOCK_RATIO -> CivSavedData.HubDistributionMode.DAILY_ALLOWANCE;
                    case DAILY_ALLOWANCE -> CivSavedData.HubDistributionMode.CONTRIBUTION_RATIO;
                };
                yield new CivGovernanceWorkflowService.PanelAction(
                        "distribution_mode",
                        next.serializedName(),
                        "Set hub distribution mode to " + next.serializedName(),
                        CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);
            }
            case CivControlPanelActionIds.FRIENDLY_FIRE_TOGGLE -> {
                if (!CivPermissionService.hasCivPermission(actor, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE)) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage friendly-fire policy."));
                    yield null;
                }
                boolean next = !data.allowIntraCivPvp(civId);
                yield new CivGovernanceWorkflowService.PanelAction(
                        "friendly_fire",
                        Boolean.toString(next),
                        "Set intra-civ PvP to " + (next ? "enabled" : "disabled"),
                        CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE);
            }
            default -> null;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public record Snapshot(
            String civilizationId,
            String civilizationName,
            String leaderTitle,
            String governanceModel,
            String hubDistributionMode,
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
            int professionHookRuleCount,
            int hunterMobActionCapRuleCount,
            long civTreasuryCents,
            long playerContributionKarmaCents,
            long playerContributedItemsTotal,
            long maxContributionKarmaGainPerDayCents,
            boolean governanceApprovalWorkflowEnabled,
            boolean warriorHubRegistrationRequired,
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
            buffer.writeUtf(governanceModel, MAX_TITLE_LEN);
            buffer.writeUtf(hubDistributionMode, MAX_TITLE_LEN);
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
            buffer.writeInt(professionHookRuleCount);
            buffer.writeInt(hunterMobActionCapRuleCount);
            buffer.writeLong(civTreasuryCents);
            buffer.writeLong(playerContributionKarmaCents);
            buffer.writeLong(playerContributedItemsTotal);
            buffer.writeLong(maxContributionKarmaGainPerDayCents);
            buffer.writeBoolean(governanceApprovalWorkflowEnabled);
            buffer.writeBoolean(warriorHubRegistrationRequired);
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

        public static Snapshot read(RegistryFriendlyByteBuf buffer) {
            return new Snapshot(
                    buffer.readUtf(MAX_ID_LEN),
                    buffer.readUtf(MAX_NAME_LEN),
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
}
