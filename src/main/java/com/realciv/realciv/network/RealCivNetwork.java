package com.realciv.realciv.network;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.census.CensusSnapshot;
import com.realciv.realciv.census.CensusSnapshotBuilder;
import com.realciv.realciv.client.ModernCensusScreen;
import com.realciv.realciv.client.ModernCivControlPanelScreen;
import com.realciv.realciv.client.ModernDiplomacyScreen;
import com.realciv.realciv.client.ModernHubStockScreen;
import com.realciv.realciv.client.ModernProfessionLedgerScreen;
import com.realciv.realciv.client.ModernTaxScreen;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.data.*;
import com.realciv.realciv.diplomacy.DiplomacySnapshot;
import com.realciv.realciv.diplomacy.DiplomacySnapshotBuilder;
import com.realciv.realciv.hub.HubStockSnapshot;
import com.realciv.realciv.hub.HubStockSnapshotBuilder;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshot;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.panel.CivControlPanelSnapshotBuilder;
import com.realciv.realciv.panel.CivGovernanceWorkflowService;
import com.realciv.realciv.tax.TaxSnapshot;
import com.realciv.realciv.tax.TaxSnapshotBuilder;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.math.XZ;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.jetbrains.annotations.Nullable;

public final class RealCivNetwork {
    private static final Map<UUID, Integer> taxPages = new HashMap<>();
    private static final Map<UUID, Integer> diplomacyPages = new HashMap<>();
    private static final Map<UUID, Integer> hubStockPages = new HashMap<>();
    private static final Map<UUID, Integer> censusPages = new HashMap<>();

    private RealCivNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(RealCivPayloads.OpenTaxPayload.TYPE, RealCivPayloads.OpenTaxPayload.STREAM_CODEC, RealCivNetwork::handleOpenTax);
        registrar.playToClient(RealCivPayloads.OpenDiplomacyPayload.TYPE, RealCivPayloads.OpenDiplomacyPayload.STREAM_CODEC, RealCivNetwork::handleOpenDiplomacy);
        registrar.playToClient(RealCivPayloads.OpenProfessionLedgerPayload.TYPE, RealCivPayloads.OpenProfessionLedgerPayload.STREAM_CODEC, RealCivNetwork::handleOpenProfession);
        registrar.playToClient(RealCivPayloads.OpenCensusPayload.TYPE, RealCivPayloads.OpenCensusPayload.STREAM_CODEC, RealCivNetwork::handleOpenCensus);
        registrar.playToClient(RealCivPayloads.OpenControlPanelPayload.TYPE, RealCivPayloads.OpenControlPanelPayload.STREAM_CODEC, RealCivNetwork::handleOpenControlPanel);
        registrar.playToClient(RealCivPayloads.OpenHubStockPayload.TYPE, RealCivPayloads.OpenHubStockPayload.STREAM_CODEC, RealCivNetwork::handleOpenHubStock);

        registrar.playToClient(RealCivPayloads.ForceMapRefreshPayload.TYPE, RealCivPayloads.ForceMapRefreshPayload.STREAM_CODEC, RealCivNetwork::handleForceMapRefresh);
        registrar.playToServer(RealCivPayloads.RealCivActionPayload.TYPE, RealCivPayloads.RealCivActionPayload.STREAM_CODEC, RealCivNetwork::handleAction);
        registrar.playToServer(RealCivPayloads.SetTaxItemPayload.TYPE, RealCivPayloads.SetTaxItemPayload.STREAM_CODEC, RealCivNetwork::handleSetTaxItem);
    }

    private static void handleOpenTax(RealCivPayloads.OpenTaxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> new ModernTaxScreen(payload.snapshot()).openGui());
    }

    private static void handleOpenDiplomacy(RealCivPayloads.OpenDiplomacyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> new ModernDiplomacyScreen(payload.snapshot()).openGui());
    }

    private static void handleOpenProfession(RealCivPayloads.OpenProfessionLedgerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> new ModernProfessionLedgerScreen(payload.snapshot()).openGui());
    }

    private static void handleOpenCensus(RealCivPayloads.OpenCensusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> new ModernCensusScreen(payload.snapshot()).openGui());
    }

    private static void handleOpenControlPanel(RealCivPayloads.OpenControlPanelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> new ModernCivControlPanelScreen(payload.snapshot()).openGui());
    }

    private static void handleOpenHubStock(RealCivPayloads.OpenHubStockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> new ModernHubStockScreen(payload.snapshot()).openGui());
    }

    private static void handleForceMapRefresh(RealCivPayloads.ForceMapRefreshPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            ResourceKey<Level> dimKey = payload.dimensionKey();
            MapManager.getInstance().ifPresent(mm -> {
                MapDimension mapDim = mm.getDimension(dimKey);
                XZ regionPos = XZ.regionFromChunk(payload.chunkX(), payload.chunkZ());
                MapRegion region = mapDim.getRegion(regionPos);
                if (region != null) {
                    region.update(false);
                    region.getRenderedMapImage();
                }
            });
            FTBChunksClient.INSTANCE.scheduleMinimapUpdate();
        });
    }

    private static void handleAction(RealCivPayloads.RealCivActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        String civId = resolveCivId(player);
        if (civId == null) return;
        CivSavedData data = CivSavedData.get(player.getServer());

        switch (payload.screenType()) {
            case RealCivPayloads.SCREEN_TAX -> handleTaxAction(player, data, civId, payload.actionId());
            case RealCivPayloads.SCREEN_DIPLOMACY -> handleDiplomacyAction(player, data, civId, payload.actionId());
            case RealCivPayloads.SCREEN_CENSUS -> handleCensusAction(player, data, civId, payload.actionId());
            case RealCivPayloads.SCREEN_CONTROL_PANEL -> handleControlPanelAction(player, data, civId, payload.actionId());
            case RealCivPayloads.SCREEN_HUB_STOCK -> handleHubStockAction(player, data, civId, payload.actionId());
        }
    }

    @Nullable
    private static String resolveCivId(ServerPlayer player) {
        if (player.getServer() == null) return null;
        return CivSavedData.get(player.getServer()).getOrAssignCivilization(player.getUUID());
    }

    private static void sendTaxScreen(ServerPlayer player, CivSavedData data, String civId) {
        TaxSnapshot snap = TaxSnapshotBuilder.build(player, data, civId);
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenTaxPayload(snap));
    }

    private static void handleTaxAction(ServerPlayer player, CivSavedData data, String civId, int actionId) {
        boolean canManage = CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP);

        switch (actionId) {
            case ModernTaxScreen.ACTION_PAY_1 -> payTax(player, data, civId, 1);
            case ModernTaxScreen.ACTION_PAY_5 -> payTax(player, data, civId, 5);
            case ModernTaxScreen.ACTION_PAY_25 -> payTax(player, data, civId, 25);
            case ModernTaxScreen.ACTION_RATE_DOWN -> { if (canManage) adjustRate(player, data, civId, -0.10D); }
            case ModernTaxScreen.ACTION_RATE_UP -> { if (canManage) adjustRate(player, data, civId, 0.10D); }
            case ModernTaxScreen.ACTION_MODE_TOGGLE -> { if (canManage) toggleMode(player, data, civId); }
            case ModernTaxScreen.ACTION_ITEM_CYCLE -> { if (canManage) cycleItem(player, data, civId); }
            case ModernTaxScreen.ACTION_SET_TAX_ITEM -> {
                // Handled via SetTaxItemPayload instead — no inventory interaction
            }
            case ModernTaxScreen.ACTION_ITEM_COUNT_DOWN -> { if (canManage) adjustItemCount(player, data, civId, -1); }
            case ModernTaxScreen.ACTION_ITEM_COUNT_UP -> { if (canManage) adjustItemCount(player, data, civId, 1); }
            case ModernTaxScreen.ACTION_PREV_PAGE -> {
                int p = taxPages.getOrDefault(player.getUUID(), 0) - 1;
                taxPages.put(player.getUUID(), Math.max(0, p));
                sendTaxScreen(player, data, civId);
                return;
            }
            case ModernTaxScreen.ACTION_NEXT_PAGE -> {
                int p = taxPages.getOrDefault(player.getUUID(), 0) + 1;
                TaxSnapshot current = TaxSnapshotBuilder.build(player, data, civId);
                taxPages.put(player.getUUID(), Math.min(p, current.totalMemberPages() - 1));
                sendTaxScreen(player, data, civId);
                return;
            }
        }
        sendTaxScreen(player, data, civId);
    }

    private static void handleSetTaxItem(RealCivPayloads.SetTaxItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String civId = resolveCivId(player);
            if (civId == null) return;
            CivSavedData data = CivSavedData.get(player.getServer());
            if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP)) return;

            ResourceLocation itemId = ResourceLocation.parse(payload.itemId());
            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                data.setTaxItemRule(civId, itemId, data.taxItemCountPerPlot(civId), player.getGameProfile().getName());
                sendTaxScreen(player, data, civId);
            }
        });
    }

    private static void payTax(ServerPlayer player, CivSavedData data, String civId, int cycles) {
        int ownedPlots = data.privatePlotCountForOwner(civId, player.getUUID());
        if (ownedPlots <= 0) {
            RealCivMessages.deny(player, "You do not own private plots.");
            return;
        }
        boolean isKarma = data.isKarmaTax(civId);
        long cost = isKarma
                ? data.upkeepCostPerPlotCents(civId) * ownedPlots * cycles
                : data.taxItemCostPerPlotCurrentRate(civId) * ownedPlots * cycles;
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        if (isKarma) {
            long balance = record.socialCreditCents(civId);
            if (balance < cost) {
                RealCivMessages.deny(player, "Insufficient karma. Need " + cost + ", have " + balance + ".");
                return;
            }
            record.addSocialCreditCents(civId, -cost);
            data.addCivTreasuryCents(civId, cost);
        } else {
            ResourceLocation itemId = data.taxItemId(civId);
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) { RealCivMessages.deny(player, "Tax item invalid."); return; }
            long available = countItem(player, item);
            if (available < cost) { RealCivMessages.deny(player, "Need " + cost + "x " + itemId + ", have " + available + "."); return; }
            removeItem(player, item, cost);
            data.addToHubStock(civId, itemId, cost, player.getGameProfile().getName());
        }
        long now = player.getServer() != null && player.getServer().overworld() != null
                ? player.getServer().overworld().getGameTime() : 0L;
        int affected = data.prepayPrivatePlotUpkeep(civId, player.getUUID(), cycles, now, player.getGameProfile().getName());
        if (affected > 0) {
            player.sendSystemMessage(Component.literal("Paid " + cycles + " cycle(s) for " + affected + " plot(s)."));
        }
        data.setDirty();
    }

    private static void adjustRate(ServerPlayer player, CivSavedData data, String civId, double delta) {
        double current = data.upkeepRateMultiplier(civId);
        if (data.setUpkeepRateMultiplier(civId, current + delta, player.getGameProfile().getName())) {
            player.sendSystemMessage(Component.literal("Rate: " + String.format("%.2fx", data.upkeepRateMultiplier(civId))));
        }
    }

    private static void toggleMode(ServerPlayer player, CivSavedData data, String civId) {
        CivicAttribute current = data.civicAttribute(civId, AttributeCategory.TAXATION);
        CivicAttribute next = current == CivicAttribute.KARMA_TAX
                ? CivicAttribute.GOODS_TAX : CivicAttribute.KARMA_TAX;
        if (data.setCivicAttribute(civId, AttributeCategory.TAXATION, next, player.getGameProfile().getName())) {
            player.sendSystemMessage(Component.literal("Tax: " + next.displayName()));
        }
    }

    private static void cycleItem(ServerPlayer player, CivSavedData data, String civId) {
        ResourceLocation current = data.taxItemId(civId);
        List<ResourceLocation> options = List.of(
                ResourceLocation.parse("minecraft:gold_nugget"),
                ResourceLocation.parse("minecraft:iron_ingot"),
                ResourceLocation.parse("minecraft:emerald"));
        int idx = options.indexOf(current);
        ResourceLocation next = options.get((idx + 1) % options.size());
        data.setTaxItemRule(civId, next, data.taxItemCountPerPlot(civId), player.getGameProfile().getName());
        player.sendSystemMessage(Component.literal("Tax item: " + next));
    }

    private static void adjustItemCount(ServerPlayer player, CivSavedData data, String civId, int delta) {
        int next = Math.max(1, data.taxItemCountPerPlot(civId) + delta);
        data.setTaxItemRule(civId, data.taxItemId(civId), next, player.getGameProfile().getName());
        player.sendSystemMessage(Component.literal("Item count: " + next));
    }

    private static long countItem(ServerPlayer player, Item item) {
        long total = 0;
        for (var stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) total += stack.getCount();
        }
        for (var stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    private static void removeItem(ServerPlayer player, Item item, long count) {
        long remaining = count;
        for (var stack : player.getInventory().items) {
            if (remaining <= 0) break;
            if (!stack.isEmpty() && stack.getItem() == item) {
                int remove = (int) Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
        for (var stack : player.getInventory().offhand) {
            if (remaining <= 0) break;
            if (!stack.isEmpty() && stack.getItem() == item) {
                int remove = (int) Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
    }

    private static void handleDiplomacyAction(ServerPlayer player, CivSavedData data, String civId, int actionId) {
        boolean canManage = CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY);

        if (actionId == ModernDiplomacyScreen.ACTION_PREV_PAGE) {
            int p = diplomacyPages.getOrDefault(player.getUUID(), 0) - 1;
            diplomacyPages.put(player.getUUID(), Math.max(0, p));
        } else if (actionId == ModernDiplomacyScreen.ACTION_NEXT_PAGE) {
            int p = diplomacyPages.getOrDefault(player.getUUID(), 0) + 1;
            DiplomacySnapshot s = DiplomacySnapshotBuilder.build(player, data, civId, p);
            diplomacyPages.put(player.getUUID(), Math.min(p, s.totalPages() - 1));
        } else if (actionId >= ModernDiplomacyScreen.ACTION_CYCLE_RELATION * 100 && canManage) {
            int index = actionId - ModernDiplomacyScreen.ACTION_CYCLE_RELATION * 100;
            int page = diplomacyPages.getOrDefault(player.getUUID(), 0);
            DiplomacySnapshot s = DiplomacySnapshotBuilder.build(player, data, civId, page);
            if (index >= 0 && index < s.relations().size()) {
                String otherId = s.relations().get(index).otherCivId();
                DiplomacyState current = data.diplomacyState(civId, otherId);
                DiplomacyState next = switch (current) {
                    case NEUTRAL -> DiplomacyState.ALLY;
                    case ALLY -> DiplomacyState.WAR;
                    case WAR -> DiplomacyState.NEUTRAL;
                };
                data.setDiplomacyState(civId, otherId, next, player.getGameProfile().getName());
                player.sendSystemMessage(Component.literal("Diplomacy with " + otherId + " is now " + next.displayName()));
                s = DiplomacySnapshotBuilder.build(player, data, civId, page);
            }
        }

        int page = diplomacyPages.getOrDefault(player.getUUID(), 0);
        DiplomacySnapshot updated = DiplomacySnapshotBuilder.build(player, data, civId, page);
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenDiplomacyPayload(updated));
    }

    private static void handleCensusAction(ServerPlayer player, CivSavedData data, String civId, int actionId) {
        boolean canManage = CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)
                || data.isCivicManager(civId, player.getUUID());
        if (!canManage && actionId != ModernCensusScreen.ACTION_PREV_PAGE && actionId != ModernCensusScreen.ACTION_NEXT_PAGE) return;

        int page;
        if (actionId == ModernCensusScreen.ACTION_PREV_PAGE) {
            page = Math.max(0, currentCensusPage(player) - 1);
            censusPages.put(player.getUUID(), page);
        } else if (actionId == ModernCensusScreen.ACTION_NEXT_PAGE) {
            page = currentCensusPage(player) + 1;
            censusPages.put(player.getUUID(), page);
        } else {
            page = currentCensusPage(player);
        }

        if (actionId >= 100 && actionId < 200) {
            int row = actionId - 100;
            UUID memberId = memberAtRow(player, data, civId, row);
            if (memberId != null) data.removeMemberToDefault(civId, memberId, player.getGameProfile().getName());
        } else if (actionId >= 200 && actionId < 300) {
            int row = actionId - 200;
            UUID target = memberAtRow(player, data, civId, row);
            if (target != null) data.setCivicManager(civId, target, !data.isCivicManager(civId, target), player.getGameProfile().getName());
        } else if (actionId >= 300 && actionId < 400) {
            int row = actionId - 300;
            UUID target = requestAtRow(data, civId, row);
            if (target != null) data.setPlayerCivilization(target, civId, player.getGameProfile().getName());
        } else if (actionId >= 400 && actionId < 500) {
            int row = actionId - 400;
            UUID target = requestAtRow(data, civId, row);
            if (target != null) data.removeJoinRequest(civId, target, player.getGameProfile().getName());
        } else if (actionId >= 500 && actionId < 600) {
            int row = actionId - 500;
            UUID inviteTarget = inviteAtRow(data, civId, row);
            if (inviteTarget != null) data.removeInvite(civId, inviteTarget, player.getGameProfile().getName());
        }

        CensusSnapshot snap = CensusSnapshotBuilder.build(player, data, civId, canManage, page);
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenCensusPayload(snap));
    }

    private static int currentCensusPage(ServerPlayer player) { return censusPages.getOrDefault(player.getUUID(), 0); }

    @Nullable
    private static UUID memberAtRow(ServerPlayer player, CivSavedData data, String civId, int row) {
        var members = data.civilizationMembersSorted(civId);
        return row >= 0 && row < members.size() ? members.get(row) : null;
    }

    @Nullable
    private static UUID requestAtRow(CivSavedData data, String civId, int row) {
        var requests = data.joinRequestsSorted(civId);
        return row >= 0 && row < requests.size() ? requests.get(row) : null;
    }

    @Nullable
    private static UUID inviteAtRow(CivSavedData data, String civId, int row) {
        var invites = data.invitedPlayersSorted(civId);
        return row >= 0 && row < invites.size() ? invites.get(row) : null;
    }

    private static void handleControlPanelAction(ServerPlayer player, CivSavedData data, String civId, int actionId) {
        String name = player.getGameProfile().getName();

        switch (actionId) {
            case ModernCivControlPanelScreen.ACTION_GOVERNANCE_CYCLE -> {
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) return;
                CivicAttribute current = data.civicAttribute(civId, AttributeCategory.EXECUTIVE);
                CivicAttribute next = switch (current) {
                    case DIRECT_RULE -> CivicAttribute.COUNCIL_VOTE;
                    case COUNCIL_VOTE -> CivicAttribute.POPULAR_VOTE;
                    default -> CivicAttribute.DIRECT_RULE;
                };
                data.setCivicAttribute(civId, AttributeCategory.EXECUTIVE, next, name);
                player.sendSystemMessage(Component.literal("Executive: " + next.displayName()));
            }
            case ModernCivControlPanelScreen.ACTION_DISTRIBUTION_TOGGLE -> {
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) return;
                CivicAttribute current = data.civicAttribute(civId, AttributeCategory.RESOURCE);
                CivicAttribute next = switch (current) {
                    case CONTRIBUTION_SHARE -> CivicAttribute.EQUAL_SHARE;
                    case EQUAL_SHARE -> CivicAttribute.RATIONED;
                    default -> CivicAttribute.CONTRIBUTION_SHARE;
                };
                data.setCivicAttribute(civId, AttributeCategory.RESOURCE, next, name);
                player.sendSystemMessage(Component.literal("Resource: " + next.displayName()));
            }
            case ModernCivControlPanelScreen.ACTION_FRIENDLY_FIRE_TOGGLE -> {
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE)) return;
                boolean next = !data.allowIntraCivPvp(civId);
                data.setAllowIntraCivPvp(civId, next, name);
                player.sendSystemMessage(Component.literal("Friendly PvP: " + (next ? "enabled" : "disabled")));
            }
            case ModernCivControlPanelScreen.ACTION_PROPOSAL_YES -> {
                var d = CivGovernanceWorkflowService.vote(player, data, civId, true);
                player.sendSystemMessage(Component.literal("[RealCiv] " + d.message()));
                if (d.actionToApply() != null) applyAction(player, data, civId, d.actionToApply());
            }
            case ModernCivControlPanelScreen.ACTION_PROPOSAL_NO -> {
                var d = CivGovernanceWorkflowService.vote(player, data, civId, false);
                player.sendSystemMessage(Component.literal("[RealCiv] " + d.message()));
                if (d.actionToApply() != null) applyAction(player, data, civId, d.actionToApply());
            }
            case ModernCivControlPanelScreen.ACTION_START_ELECTION -> {
                var d = data.startLeadershipElection(civId, player.getUUID(), name);
                player.sendSystemMessage(Component.literal("[RealCiv] " + d.message()));
            }
            case ModernCivControlPanelScreen.ACTION_JOIN_ELECTION -> {
                var d = data.joinLeadershipElectionCandidate(civId, player.getUUID(), name);
                player.sendSystemMessage(Component.literal("[RealCiv] " + d.message()));
            }
            case ModernCivControlPanelScreen.ACTION_START_COUP -> {
                var d = data.startLeadershipCoup(civId, player.getUUID(), player.getUUID(), name);
                player.sendSystemMessage(Component.literal("[RealCiv] " + d.message()));
            }
            case ModernCivControlPanelScreen.ACTION_APPROVE_COUP -> {
                var d = data.approveLeadershipCoup(civId, player.getUUID(), name);
                player.sendSystemMessage(Component.literal("[RealCiv] " + d.message()));
            }
            case ModernCivControlPanelScreen.ACTION_ROLES_CREATE -> {
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) return;
                int count = data.customRolesSorted(civId).size() + 1;
                String roleId = "custom_role_" + count;
                String displayName = "Custom Role " + count;
                if (data.createCustomRole(civId, roleId, displayName, name)) {
                    player.sendSystemMessage(Component.literal("Created role: " + displayName + ". Use /realciv role to configure."));
                } else {
                    RealCivMessages.deny(player, "Could not create role (may already exist).");
                }
            }
            case ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 1,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 2, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 3,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 4, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 5,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 6, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 7,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 8, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 9,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 10, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 11,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 12, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 13,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 14, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 15,
                 ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 16, ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE + 17 -> {
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) return;
                int attrOrdinal = actionId - ModernCivControlPanelScreen.ACTION_SET_ATTRIBUTE;
                CivicAttribute[] attrValues = CivicAttribute.values();
                if (attrOrdinal >= 0 && attrOrdinal < attrValues.length) {
                    CivicAttribute attr = attrValues[attrOrdinal];
                    data.setCivicAttribute(civId, attr.category(), attr, name);
                    player.sendSystemMessage(Component.literal(attr.category().displayName() + ": " + attr.displayName()));
                }
            }
            case ModernCivControlPanelScreen.ACTION_VOTE_CANDIDATE_1, ModernCivControlPanelScreen.ACTION_VOTE_CANDIDATE_2,
                 ModernCivControlPanelScreen.ACTION_VOTE_CANDIDATE_3, ModernCivControlPanelScreen.ACTION_VOTE_CANDIDATE_4,
                 ModernCivControlPanelScreen.ACTION_VOTE_CANDIDATE_5 -> {
                int idx = actionId - ModernCivControlPanelScreen.ACTION_VOTE_CANDIDATE_1;
                var contest = data.leadershipContest(civId);
                if (contest != null && idx >= 0 && idx < contest.candidates().size()) {
                    @Nullable UUID candidateId = contest.candidates().stream().skip(idx).findFirst().orElse(null);
                    if (candidateId != null) {
                        var d = data.voteLeadershipElectionCandidate(civId, player.getUUID(), candidateId, name);
                        player.sendSystemMessage(Component.literal("[RealCiv] " + d.message()));
                    }
                }
            }
        }

        var snap = CivControlPanelSnapshotBuilder.build(player, data, civId);
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenControlPanelPayload(snap));
    }

    private static void applyAction(ServerPlayer player, CivSavedData data, String civId, CivGovernanceWorkflowService.PanelAction action) {
        switch (action.type()) {
            case "executive_attribute" -> {
                var attr = CivicAttribute.fromSerializedName(action.payload());
                if (attr != null) data.setCivicAttribute(civId, AttributeCategory.EXECUTIVE, attr, player.getGameProfile().getName());
            }
            case "resource_attribute" -> {
                var attr = CivicAttribute.fromSerializedName(action.payload());
                if (attr != null) data.setCivicAttribute(civId, AttributeCategory.RESOURCE, attr, player.getGameProfile().getName());
            }
            case "friendly_fire" -> data.setAllowIntraCivPvp(civId, Boolean.parseBoolean(action.payload()), player.getGameProfile().getName());
        }
    }

    private static void handleHubStockAction(ServerPlayer player, CivSavedData data, String civId, int actionId) {
        boolean canManage = CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);
        int page = hubStockPages.getOrDefault(player.getUUID(), 0);

        if (actionId == ModernHubStockScreen.ACTION_PREV_PAGE) {
            hubStockPages.put(player.getUUID(), Math.max(0, page - 1));
        } else if (actionId == ModernHubStockScreen.ACTION_NEXT_PAGE) {
            HubStockSnapshot s = HubStockSnapshotBuilder.build(player, data, civId, canManage, page + 1);
            hubStockPages.put(player.getUUID(), Math.min(page + 1, Math.max(0, s.totalPages() - 1)));
        } else if (actionId >= 1000 && actionId < 2000) {
            int index = actionId - 1000;
            performWithdrawal(player, data, civId, index, 1);
        } else if (actionId >= 2000 && actionId < 3000) {
            int index = actionId - 2000;
            performWithdrawal(player, data, civId, index, 64);
        } else if (actionId >= 3000 && actionId < 4000 && canManage) {
            int index = actionId - 3000;
            adjustAllowance(player, data, civId, index, 1);
        } else if (actionId >= 4000 && actionId < 5000 && canManage) {
            int index = actionId - 4000;
            adjustAllowance(player, data, civId, index, -1);
        }

        page = hubStockPages.getOrDefault(player.getUUID(), 0);
        HubStockSnapshot snap = HubStockSnapshotBuilder.build(player, data, civId, canManage, page);
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenHubStockPayload(snap));
    }

    private static void adjustAllowance(ServerPlayer player, CivSavedData data, String civId, int index, int delta) {
        List<Map.Entry<String, Long>> entries = data.getHubStockEntriesSorted(civId);
        if (index < 0 || index >= entries.size()) return;
        String itemKey = entries.get(index).getKey();
        ResourceLocation itemId = ResourceLocation.parse(itemKey);
        int current = data.hubDailyAllowanceLimit(civId, itemId);
        int next = Math.max(0, current + delta);
        if (data.setHubDailyAllowanceLimit(civId, itemId, next, player.getGameProfile().getName())) {
            player.sendSystemMessage(Component.literal("Allowance for " + itemKey + ": " + next + "/day"));
        }
    }

    private static void performWithdrawal(ServerPlayer player, CivSavedData data, String civId, int index, int count) {
        List<Map.Entry<String, Long>> entries = data.getHubStockEntriesSorted(civId);
        if (index < 0 || index >= entries.size()) return;
        String itemKey = entries.get(index).getKey();
        ResourceLocation itemId = ResourceLocation.parse(itemKey);
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) return;

        if (!data.tryWithdrawFromHub(civId, itemId, count)) {
            RealCivMessages.deny(player, "Cannot withdraw that item (check quota/allowance).");
            data.setDirty();
            return;
        }
        ItemStack stack = new net.minecraft.world.item.ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendSystemMessage(Component.literal("Withdrew " + count + "x " + itemId));
        data.setDirty();
    }
}
