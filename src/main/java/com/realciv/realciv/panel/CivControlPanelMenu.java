package com.realciv.realciv.panel;

import com.realciv.realciv.ModMenus;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CivPermissionService;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CivControlPanelMenu extends AbstractContainerMenu {
    private static final int DAILY_ALLOWANCE_STEP = 1;
    private static final ResourceLocation BREAD_ID = ResourceLocation.parse("minecraft:bread");
    private static final ResourceLocation GOLD_NUGGET_ID = ResourceLocation.parse("minecraft:gold_nugget");

    private final Snapshot snapshot;
    @Nullable
    private final ServerPlayer serverViewer;
    @Nullable
    private final CivSavedData serverData;
    @Nullable
    private final String serverCivId;

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
            case "allowance_delta" -> {
                String[] parts = action.payload().split("\\|", 2);
                if (parts.length != 2) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] Invalid allowance payload: " + action.payload()));
                    return;
                }
                ResourceLocation itemId;
                try {
                    itemId = ResourceLocation.parse(parts[0]);
                } catch (Exception ex) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] Invalid item id: " + parts[0]));
                    return;
                }
                int delta;
                try {
                    delta = Integer.parseInt(parts[1]);
                } catch (Exception ex) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] Invalid allowance delta: " + parts[1]));
                    return;
                }
                int current = serverData.hubDailyAllowanceLimit(serverCivId, itemId);
                int next = Math.max(0, current + delta);
                boolean changed = serverData.setHubDailyAllowanceLimit(serverCivId, itemId, next, actorName);
                actor.sendSystemMessage(Component.literal("[RealCiv] Daily allowance for " + itemId
                        + " is now " + next + "/day" + (changed ? "." : " (unchanged).")));
            }
            case "allowance_clear_all" -> {
                int cleared = serverData.clearAllHubDailyAllowanceLimits(serverCivId, actorName);
                actor.sendSystemMessage(Component.literal(
                        "[RealCiv] Cleared " + cleared + " daily allowance entr" + (cleared == 1 ? "y." : "ies.")));
            }
            case "hook_rules_template" -> {
                RealCivConfig.PROFESSION_EVENT_HOOK_RULES.set(
                        PolicyRuleListUtil.addUnique(RealCivConfig.PROFESSION_EVENT_HOOK_RULES.get(), action.payload()));
                RealCivConfig.invalidateExternalRuleFileCache();
                RealCivConfig.SPEC.save();
                actor.sendSystemMessage(Component.literal("[RealCiv] Added hook rule template: " + action.payload()));
            }
            case "hook_rules_remove_last" -> {
                if (RealCivConfig.PROFESSION_EVENT_HOOK_RULES.get().isEmpty()) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] No hook rules to remove."));
                    return;
                }
                List<String> existing = PolicyRuleListUtil.removeLast(RealCivConfig.PROFESSION_EVENT_HOOK_RULES.get());
                RealCivConfig.PROFESSION_EVENT_HOOK_RULES.set(existing);
                RealCivConfig.invalidateExternalRuleFileCache();
                RealCivConfig.SPEC.save();
                actor.sendSystemMessage(Component.literal("[RealCiv] Removed last hook rule entry."));
            }
            case "hunter_caps_template" -> {
                RealCivConfig.HUNTER_MOB_ACTION_CAPS.set(
                        PolicyRuleListUtil.addUnique(RealCivConfig.HUNTER_MOB_ACTION_CAPS.get(), action.payload()));
                RealCivConfig.invalidateExternalRuleFileCache();
                RealCivConfig.SPEC.save();
                actor.sendSystemMessage(Component.literal("[RealCiv] Added hunter mob cap template: " + action.payload()));
            }
            case "hunter_caps_remove_last" -> {
                if (RealCivConfig.HUNTER_MOB_ACTION_CAPS.get().isEmpty()) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] No hunter mob caps to remove."));
                    return;
                }
                List<String> existing = PolicyRuleListUtil.removeLast(RealCivConfig.HUNTER_MOB_ACTION_CAPS.get());
                RealCivConfig.HUNTER_MOB_ACTION_CAPS.set(existing);
                RealCivConfig.invalidateExternalRuleFileCache();
                RealCivConfig.SPEC.save();
                actor.sendSystemMessage(Component.literal("[RealCiv] Removed last hunter cap rule entry."));
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
                CivSavedData.HubDistributionMode next = data.hubDistributionMode(civId) == CivSavedData.HubDistributionMode.CONTRIBUTION_RATIO
                        ? CivSavedData.HubDistributionMode.DAILY_ALLOWANCE
                        : CivSavedData.HubDistributionMode.CONTRIBUTION_RATIO;
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
            case CivControlPanelActionIds.ALLOWANCE_BREAD_PLUS -> dailyAllowanceAction(actor, BREAD_ID, DAILY_ALLOWANCE_STEP);
            case CivControlPanelActionIds.ALLOWANCE_BREAD_MINUS -> dailyAllowanceAction(actor, BREAD_ID, -DAILY_ALLOWANCE_STEP);
            case CivControlPanelActionIds.ALLOWANCE_GOLD_PLUS -> dailyAllowanceAction(actor, GOLD_NUGGET_ID, DAILY_ALLOWANCE_STEP);
            case CivControlPanelActionIds.ALLOWANCE_GOLD_MINUS -> dailyAllowanceAction(actor, GOLD_NUGGET_ID, -DAILY_ALLOWANCE_STEP);
            case CivControlPanelActionIds.ALLOWANCE_CLEAR_ALL -> {
                if (!CivPermissionService.hasCivPermission(actor, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
                    actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage daily allowances."));
                    yield null;
                }
                yield new CivGovernanceWorkflowService.PanelAction(
                        "allowance_clear_all",
                        "all",
                        "Clear all hub daily allowances",
                        CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);
            }
            case CivControlPanelActionIds.HOOK_ADD_ANIMAL_BREED -> hookTemplateAction(
                    actor,
                    "ANIMAL_BREED|FARMER|1|min_profession_level=1|profession_xp=5|general_xp=1");
            case CivControlPanelActionIds.HOOK_ADD_VILLAGER_TRADE -> hookTemplateAction(
                    actor,
                    "VILLAGER_TRADE|CRAFTER|1|min_profession_level=1|profession_xp=6|general_xp=2");
            case CivControlPanelActionIds.HOOK_ADD_SHEAR_ENTITY -> hookTemplateAction(
                    actor,
                    "SHEAR_ENTITY|FARMER|1|min_profession_level=1|profession_xp=4|general_xp=1");
            case CivControlPanelActionIds.HOOK_REMOVE_LAST -> hookRemoveLastAction(actor);
            case CivControlPanelActionIds.HUNTER_CAP_ADD_ZOMBIE -> hunterCapTemplateAction(
                    actor,
                    "minecraft:zombie|2,4,6,8,10,12,16");
            case CivControlPanelActionIds.HUNTER_CAP_ADD_ENDER_DRAGON -> hunterCapTemplateAction(
                    actor,
                    "minecraft:ender_dragon|0,0,0,0,0,0,1");
            case CivControlPanelActionIds.HUNTER_CAP_REMOVE_LAST -> hunterCapRemoveLastAction(actor);
            default -> null;
        };
    }

    private @Nullable CivGovernanceWorkflowService.PanelAction dailyAllowanceAction(ServerPlayer actor, ResourceLocation itemId, int delta) {
        if (!CivPermissionService.hasCivPermission(actor, serverData, serverCivId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage daily allowances."));
            return null;
        }
        return new CivGovernanceWorkflowService.PanelAction(
                "allowance_delta",
                itemId + "|" + delta,
                (delta >= 0 ? "Increase " : "Decrease ") + itemId + " daily allowance by " + Math.abs(delta),
                CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);
    }

    private @Nullable CivGovernanceWorkflowService.PanelAction hookTemplateAction(ServerPlayer actor, String ruleTemplate) {
        if (!CivPermissionService.hasCivPermission(actor, serverData, serverCivId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage profession hook policy templates."));
            return null;
        }
        return new CivGovernanceWorkflowService.PanelAction(
                "hook_rules_template",
                ruleTemplate,
                "Add hook rule template: " + ruleTemplate,
                CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
    }

    private @Nullable CivGovernanceWorkflowService.PanelAction hookRemoveLastAction(ServerPlayer actor) {
        if (!CivPermissionService.hasCivPermission(actor, serverData, serverCivId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage profession hook policy templates."));
            return null;
        }
        return new CivGovernanceWorkflowService.PanelAction(
                "hook_rules_remove_last",
                "last",
                "Remove last hook rule template",
                CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
    }

    private @Nullable CivGovernanceWorkflowService.PanelAction hunterCapTemplateAction(ServerPlayer actor, String capTemplate) {
        if (!CivPermissionService.hasCivPermission(actor, serverData, serverCivId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage hunter selector policies."));
            return null;
        }
        return new CivGovernanceWorkflowService.PanelAction(
                "hunter_caps_template",
                capTemplate,
                "Add hunter cap template: " + capTemplate,
                CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
    }

    private @Nullable CivGovernanceWorkflowService.PanelAction hunterCapRemoveLastAction(ServerPlayer actor) {
        if (!CivPermissionService.hasCivPermission(actor, serverData, serverCivId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            actor.sendSystemMessage(Component.literal("[RealCiv] You cannot manage hunter selector policies."));
            return null;
        }
        return new CivGovernanceWorkflowService.PanelAction(
                "hunter_caps_remove_last",
                "last",
                "Remove last hunter cap template",
                CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
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
            boolean canManageHubDistribution) {
        private static final int MAX_ID_LEN = 64;
        private static final int MAX_NAME_LEN = 96;
        private static final int MAX_TITLE_LEN = 128;

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
                    buffer.readBoolean());
        }
    }
}
