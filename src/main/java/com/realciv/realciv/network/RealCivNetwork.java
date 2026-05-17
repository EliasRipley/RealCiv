package com.realciv.realciv.network;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.census.CensusSnapshot;
import com.realciv.realciv.census.CensusSnapshotBuilder;
import com.realciv.realciv.client.ModernCensusScreen;
import com.realciv.realciv.client.ModernCivControlPanelScreen;
import com.realciv.realciv.client.ModernDiplomacyScreen;
import com.realciv.realciv.client.ModernHubStockScreen;
import com.realciv.realciv.client.ModernProfessionLedgerScreen;
import com.realciv.realciv.client.ModernRationEditorScreen;
import com.realciv.realciv.client.ModernTaxScreen;
import com.realciv.realciv.client.RealCivScreen;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.data.*;
import com.realciv.realciv.diplomacy.DiplomacySnapshot;
import com.realciv.realciv.diplomacy.DiplomacySnapshotBuilder;
import com.realciv.realciv.hub.HubStockSnapshot;
import com.realciv.realciv.hub.HubStockSnapshotBuilder;
import com.realciv.realciv.hub.HubRationSnapshot;
import com.realciv.realciv.hub.HubRationSnapshotBuilder;
import com.realciv.realciv.hub.RationDraftContainer;
import com.realciv.realciv.hub.RationDraftMenu;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshot;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.panel.CivControlPanelSnapshotBuilder;
import com.realciv.realciv.panel.CivGovernanceWorkflowService;
import com.realciv.realciv.tax.TaxSnapshot;
import com.realciv.realciv.tax.TaxSnapshotBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
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
    private static final Map<UUID, Integer> rationPages = new HashMap<>();

    private RealCivNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(RealCivPayloads.OpenTaxPayload.TYPE, RealCivPayloads.OpenTaxPayload.STREAM_CODEC, RealCivNetwork::handleOpenTax);
        registrar.playToClient(RealCivPayloads.OpenDiplomacyPayload.TYPE, RealCivPayloads.OpenDiplomacyPayload.STREAM_CODEC, RealCivNetwork::handleOpenDiplomacy);
        registrar.playToClient(RealCivPayloads.OpenProfessionLedgerPayload.TYPE, RealCivPayloads.OpenProfessionLedgerPayload.STREAM_CODEC, RealCivNetwork::handleOpenProfession);
        registrar.playToClient(RealCivPayloads.OpenCensusPayload.TYPE, RealCivPayloads.OpenCensusPayload.STREAM_CODEC, RealCivNetwork::handleOpenCensus);
        registrar.playToClient(RealCivPayloads.OpenControlPanelPayload.TYPE, RealCivPayloads.OpenControlPanelPayload.STREAM_CODEC, RealCivNetwork::handleOpenControlPanel);
        registrar.playToClient(RealCivPayloads.OpenHubStockPayload.TYPE, RealCivPayloads.OpenHubStockPayload.STREAM_CODEC, RealCivNetwork::handleOpenHubStock);
        registrar.playToClient(RealCivPayloads.OpenRationEditorPayload.TYPE, RealCivPayloads.OpenRationEditorPayload.STREAM_CODEC, RealCivNetwork::handleOpenRationEditor);

        registrar.playToClient(RealCivPayloads.ForceMapRefreshPayload.TYPE, RealCivPayloads.ForceMapRefreshPayload.STREAM_CODEC, RealCivNetwork::handleForceMapRefresh);
        registrar.playToServer(RealCivPayloads.RealCivActionPayload.TYPE, RealCivPayloads.RealCivActionPayload.STREAM_CODEC, RealCivNetwork::handleAction);
        registrar.playToServer(RealCivPayloads.SetTaxItemPayload.TYPE, RealCivPayloads.SetTaxItemPayload.STREAM_CODEC, RealCivNetwork::handleSetTaxItem);
        registrar.playToServer(RealCivPayloads.SetTaxItemCountPayload.TYPE, RealCivPayloads.SetTaxItemCountPayload.STREAM_CODEC, RealCivNetwork::handleSetTaxItemCount);
        registrar.playToServer(RealCivPayloads.SetHubAllowancePayload.TYPE, RealCivPayloads.SetHubAllowancePayload.STREAM_CODEC, RealCivNetwork::handleSetHubAllowance);
        registrar.playToServer(RealCivPayloads.SetHubAllowanceBatchPayload.TYPE, RealCivPayloads.SetHubAllowanceBatchPayload.STREAM_CODEC, RealCivNetwork::handleSetHubAllowanceBatch);
    }

    private static void handleOpenTax(RealCivPayloads.OpenTaxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            openOrRefreshRealCivScreen(
                    ModernTaxScreen.class,
                    () -> new ModernTaxScreen(payload.snapshot()),
                    screen -> screen.refresh(payload.snapshot()));
        });
    }

    private static void handleOpenDiplomacy(RealCivPayloads.OpenDiplomacyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            openOrRefreshRealCivScreen(
                    ModernDiplomacyScreen.class,
                    () -> new ModernDiplomacyScreen(payload.snapshot()),
                    screen -> screen.refresh(payload.snapshot()));
        });
    }

    private static void handleOpenProfession(RealCivPayloads.OpenProfessionLedgerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            openOrRefreshRealCivScreen(
                    ModernProfessionLedgerScreen.class,
                    () -> new ModernProfessionLedgerScreen(payload.snapshot()),
                    screen -> screen.refresh(payload.snapshot()));
        });
    }

    private static void handleOpenCensus(RealCivPayloads.OpenCensusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            openOrRefreshRealCivScreen(
                    ModernCensusScreen.class,
                    () -> new ModernCensusScreen(payload.snapshot()),
                    screen -> screen.refresh(payload.snapshot()));
        });
    }

    private static void handleOpenControlPanel(RealCivPayloads.OpenControlPanelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            openOrRefreshRealCivScreen(
                    ModernCivControlPanelScreen.class,
                    () -> new ModernCivControlPanelScreen(payload.snapshot()),
                    screen -> screen.refresh(payload.snapshot()));
        });
    }

    private static void handleOpenHubStock(RealCivPayloads.OpenHubStockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            openOrRefreshRealCivScreen(
                    ModernHubStockScreen.class,
                    () -> new ModernHubStockScreen(payload.snapshot()),
                    screen -> screen.refresh(payload.snapshot()));
        });
    }

    private static void handleOpenRationEditor(RealCivPayloads.OpenRationEditorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            openOrRefreshRealCivScreen(
                    ModernRationEditorScreen.class,
                    () -> new ModernRationEditorScreen(payload.snapshot()),
                    screen -> screen.refresh(payload.snapshot()));
        });
    }

    private static <T extends RealCivScreen> void openOrRefreshRealCivScreen(
            Class<T> screenType, Supplier<T> screenFactory, Consumer<T> refresher) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ScreenWrapper wrapper && wrapper.getGui() instanceof RealCivScreen openScreen) {
            if (screenType.isInstance(openScreen)) {
                refresher.accept(screenType.cast(openScreen));
                return;
            }

            // Replace RealCiv screens in-place so ESC closes straight to gameplay
            // instead of walking a stack of previously-opened block screens.
            openScreen.closeGui(false);
        }
        screenFactory.get().openGui();
    }

    private static void handleForceMapRefresh(RealCivPayloads.ForceMapRefreshPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            MapManager.getInstance().ifPresent(mm -> {
                MapDimension mapDim = mm.getDimension(payload.dimensionKey());
                if (mapDim == null) return;

                XZ regionPos = XZ.regionFromChunk(payload.chunkX(), payload.chunkZ());
                MapRegion region = mapDim.getRegion(regionPos);
                if (region == null) return;

                region.update(false);
                NativeImage regionImage = region.getRenderedMapImage();
                if (regionImage == null) return;

                // Directly update the minimap texture tile for this chunk.
                // This is needed because when ChunkScreen is open the normal
                // minimap render loop (which rebuilds the minimap texture)
                // does not run, so the claim map shows stale borders.
                int pcx = mc.player.chunkPosition().x;
                int pcz = mc.player.chunkPosition().z;
                int mx = payload.chunkX() - (pcx - FTBChunks.TILE_OFFSET);
                int mz = payload.chunkZ() - (pcz - FTBChunks.TILE_OFFSET);

                if (mx >= 0 && mx < FTBChunks.TILES && mz >= 0 && mz < FTBChunks.TILES) {
                    int texId = FTBChunksClient.INSTANCE.getMinimapTextureId();
                    if (texId != -1) {
                        RenderSystem.bindTexture(texId);
                        regionImage.upload(0, mx * 16, mz * 16,
                                (payload.chunkX() & 31) * 16, (payload.chunkZ() & 31) * 16,
                                16, 16, false, false, false, false);
                    }
                }

                region.getRenderedMapImageTextureId();
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
            case RealCivPayloads.SCREEN_RATION_EDITOR -> handleRationEditorAction(player, data, civId, payload.actionId());
        }
    }

    @Nullable
    private static String resolveCivId(ServerPlayer player) {
        if (player.getServer() == null) return null;
        return CivSavedData.get(player.getServer()).getOrAssignCivilization(player.getUUID());
    }

    private static void sendTaxScreen(ServerPlayer player, CivSavedData data, String civId) {
        int page = taxPages.getOrDefault(player.getUUID(), 0);
        TaxSnapshot snap = TaxSnapshotBuilder.build(player, data, civId, page);
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenTaxPayload(snap));
    }

    private static void openRationDraftMenu(ServerPlayer player, CivSavedData data, String civId) {
        List<RationDraftMenu.AllowancePreviewEntry> allowancePreview = data.hubDailyAllowanceEntriesSorted(civId).stream()
                .limit(48)
                .map(entry -> new RationDraftMenu.AllowancePreviewEntry(
                        entry.getKey(),
                        Math.max(0, entry.getValue())))
                .toList();
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, playerInventory, p) ->
                                new RationDraftMenu(
                                        containerId,
                                        playerInventory,
                                        new RationDraftContainer(civId),
                                        allowancePreview),
                        Component.literal("Ration Draft Editor")),
                buffer -> {
                    buffer.writeUtf(civId, 128);
                    buffer.writeVarInt(allowancePreview.size());
                    for (RationDraftMenu.AllowancePreviewEntry entry : allowancePreview) {
                        buffer.writeUtf(entry.itemId(), 128);
                        buffer.writeVarInt(entry.dailyAllowance());
                    }
                });
        player.sendSystemMessage(Component.literal(
                "Ration draft opened for '" + civId + "'. Stage items here; stack size sets daily allowance. "
                        + "Items are returned when you close."));
    }

    private static void sendRationEditorScreen(ServerPlayer player, CivSavedData data, String civId) {
        boolean canManage = CivPermissionService.hasCivPermission(
                player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);
        int page = rationPages.getOrDefault(player.getUUID(), 0);
        HubRationSnapshot snap = HubRationSnapshotBuilder.build(player, data, civId, canManage, page);
        rationPages.put(player.getUUID(), snap.page());
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenRationEditorPayload(snap));
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
                int nextPage = Math.min(p, TaxSnapshotBuilder.build(player, data, civId, 0).totalMemberPages() - 1);
                taxPages.put(player.getUUID(), nextPage);
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

    private static void handleSetHubAllowance(RealCivPayloads.SetHubAllowancePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String civId = resolveCivId(player);
            if (civId == null) {
                return;
            }
            CivSavedData data = CivSavedData.get(player.getServer());
            if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
                RealCivMessages.deny(player, "You do not have permission to manage hub daily allowances.");
                sendRationEditorScreen(player, data, civId);
                return;
            }

            ResourceLocation itemId = ResourceLocation.parse(payload.itemId());
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                RealCivMessages.deny(player, "Unknown item: " + payload.itemId());
                sendRationEditorScreen(player, data, civId);
                return;
            }

            int safeCount = Math.max(0, payload.dailyCount());
            if (data.setHubDailyAllowanceLimit(civId, itemId, safeCount, player.getGameProfile().getName())) {
                String action = safeCount <= 0 ? "cleared" : ("set to " + safeCount + "/day");
                player.sendSystemMessage(Component.literal("Allowance for " + itemId + " " + action + "."));
            }
            sendRationEditorScreen(player, data, civId);
        });
    }

    private static void handleSetTaxItemCount(RealCivPayloads.SetTaxItemCountPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String civId = resolveCivId(player);
            if (civId == null) {
                return;
            }
            CivSavedData data = CivSavedData.get(player.getServer());
            if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP)) {
                RealCivMessages.deny(player, "You do not have permission to manage upkeep settings.");
                sendTaxScreen(player, data, civId);
                return;
            }

            int safeCount = Math.max(1, Math.min(9999, payload.itemCount()));
            if (data.setTaxItemRule(civId, data.taxItemId(civId), safeCount, player.getGameProfile().getName())) {
                player.sendSystemMessage(Component.literal("Item count: " + safeCount));
            }
            sendTaxScreen(player, data, civId);
        });
    }

    private static void handleSetHubAllowanceBatch(RealCivPayloads.SetHubAllowanceBatchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String civId = resolveCivId(player);
            if (civId == null) {
                return;
            }
            CivSavedData data = CivSavedData.get(player.getServer());
            if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
                RealCivMessages.deny(player, "You do not have permission to manage hub daily allowances.");
                sendRationEditorScreen(player, data, civId);
                return;
            }

            int pairCount = Math.min(payload.itemIds().size(), payload.dailyCounts().size());
            if (pairCount > 256) {
                pairCount = 256;
            }

            Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
            int invalidIds = 0;
            for (int i = 0; i < pairCount; i++) {
                String rawId = payload.itemIds().get(i);
                ResourceLocation itemId;
                try {
                    itemId = ResourceLocation.parse(rawId);
                } catch (Exception ignored) {
                    invalidIds++;
                    continue;
                }
                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                    invalidIds++;
                    continue;
                }
                int safeCount = Math.max(0, Math.min(9999, payload.dailyCounts().get(i)));
                normalized.put(itemId, safeCount);
            }

            String actor = player.getGameProfile().getName();
            int changed = 0;
            if (payload.replaceExisting()) {
                changed += data.clearAllHubDailyAllowanceLimits(civId, actor);
            }
            for (Map.Entry<ResourceLocation, Integer> entry : normalized.entrySet()) {
                if (data.setHubDailyAllowanceLimit(civId, entry.getKey(), entry.getValue(), actor)) {
                    changed++;
                }
            }

            if (invalidIds > 0) {
                RealCivMessages.deny(player, "Skipped " + invalidIds + " invalid ration item entr" + (invalidIds == 1 ? "y." : "ies."));
            }
            player.sendSystemMessage(Component.literal(
                    "Ration draft applied: " + normalized.size() + " item(s), " + changed + " change(s)."));
            sendRationEditorScreen(player, data, civId);
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
        if (!canManage && actionId != ModernCensusScreen.ACTION_PREV_PAGE && actionId != ModernCensusScreen.ACTION_NEXT_PAGE) {
            RealCivMessages.deny(player, "Only leadership or civic managers can manage census actions.");
            int page = currentCensusPage(player);
            CensusSnapshot snap = CensusSnapshotBuilder.build(player, data, civId, false, page);
            PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenCensusPayload(snap));
            return;
        }

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

        int totalMemberPages = Math.max(1, (data.civilizationMembersSorted(civId).size()
                + CensusSnapshotBuilder.MEMBERS_PER_PAGE - 1) / CensusSnapshotBuilder.MEMBERS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalMemberPages - 1));
        censusPages.put(player.getUUID(), page);

        if (actionId >= 100 && actionId < 200) {
            int row = actionId - 100;
            UUID memberId = memberAtRow(data, civId, page * CensusSnapshotBuilder.MEMBERS_PER_PAGE + row);
            if (memberId != null) data.removeMemberToDefault(civId, memberId, player.getGameProfile().getName());
        } else if (actionId >= 200 && actionId < 300) {
            int row = actionId - 200;
            UUID target = memberAtRow(data, civId, page * CensusSnapshotBuilder.MEMBERS_PER_PAGE + row);
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
    private static UUID memberAtRow(CivSavedData data, String civId, int row) {
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
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
                    RealCivMessages.deny(player, "You do not have permission to manage governance.");
                    return;
                }
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
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
                    RealCivMessages.deny(player, "You do not have permission to manage hub distribution policy.");
                    return;
                }
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
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE)) {
                    RealCivMessages.deny(player, "You do not have permission to toggle friendly PvP.");
                    return;
                }
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
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
                    RealCivMessages.deny(player, "You do not have permission to create roles.");
                    return;
                }
                int count = data.customRolesSorted(civId).size() + 1;
                String roleId = "custom_role_" + count;
                String displayName = "Custom Role " + count;
                if (data.createCustomRole(civId, roleId, displayName, name)) {
                    player.sendSystemMessage(Component.literal("Created role: " + displayName + ". Use /realciv role to configure."));
                } else {
                    RealCivMessages.deny(player, "Could not create role (may already exist).");
                }
            }
            case ModernCivControlPanelScreen.ACTION_OPEN_RATION_EDITOR -> {
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
                    RealCivMessages.deny(player, "You do not have permission to manage ration allowances.");
                    return;
                }
                openRationDraftMenu(player, data, civId);
                return;
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
                if (!CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
                    RealCivMessages.deny(player, "You do not have permission to change civic policies.");
                    return;
                }
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

    private static void handleRationEditorAction(ServerPlayer player, CivSavedData data, String civId, int actionId) {
        boolean canManage = CivPermissionService.hasCivPermission(
                player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);

        int page = rationPages.getOrDefault(player.getUUID(), 0);
        if (actionId == ModernRationEditorScreen.ACTION_PREV_PAGE) {
            page = Math.max(0, page - 1);
            rationPages.put(player.getUUID(), page);
        } else if (actionId == ModernRationEditorScreen.ACTION_NEXT_PAGE) {
            page = page + 1;
            rationPages.put(player.getUUID(), page);
        }

        HubRationSnapshot pageSnapshot = HubRationSnapshotBuilder.build(player, data, civId, canManage, page);
        rationPages.put(player.getUUID(), pageSnapshot.page());

        boolean isRowAction = actionId >= ModernRationEditorScreen.ACTION_ALLOWANCE_UP
                && actionId < ModernRationEditorScreen.ACTION_ALLOWANCE_CLEAR + 1000;
        if (isRowAction && !canManage) {
            RealCivMessages.deny(player, "You do not have permission to manage ration entries.");
            sendRationEditorScreen(player, data, civId);
            return;
        }

        if (actionId >= ModernRationEditorScreen.ACTION_ALLOWANCE_UP
                && actionId < ModernRationEditorScreen.ACTION_ALLOWANCE_DOWN) {
            int row = actionId - ModernRationEditorScreen.ACTION_ALLOWANCE_UP;
            adjustRationAllowanceRow(player, data, civId, pageSnapshot, row, 1);
        } else if (actionId >= ModernRationEditorScreen.ACTION_ALLOWANCE_DOWN
                && actionId < ModernRationEditorScreen.ACTION_ALLOWANCE_CLEAR) {
            int row = actionId - ModernRationEditorScreen.ACTION_ALLOWANCE_DOWN;
            adjustRationAllowanceRow(player, data, civId, pageSnapshot, row, -1);
        } else if (actionId >= ModernRationEditorScreen.ACTION_ALLOWANCE_CLEAR
                && actionId < ModernRationEditorScreen.ACTION_ALLOWANCE_CLEAR + 1000) {
            int row = actionId - ModernRationEditorScreen.ACTION_ALLOWANCE_CLEAR;
            setRationAllowanceRow(player, data, civId, pageSnapshot, row, 0);
        }

        sendRationEditorScreen(player, data, civId);
    }

    private static void adjustRationAllowanceRow(ServerPlayer player, CivSavedData data, String civId,
                                                 HubRationSnapshot snapshot, int rowIndex, int delta) {
        if (rowIndex < 0 || rowIndex >= snapshot.entries().size()) {
            return;
        }
        String itemKey = snapshot.entries().get(rowIndex).itemId();
        ResourceLocation itemId = ResourceLocation.parse(itemKey);
        int current = data.hubDailyAllowanceLimit(civId, itemId);
        int next = Math.max(0, current + delta);
        setRationAllowance(player, data, civId, itemId, next);
    }

    private static void setRationAllowanceRow(ServerPlayer player, CivSavedData data, String civId,
                                              HubRationSnapshot snapshot, int rowIndex, int count) {
        if (rowIndex < 0 || rowIndex >= snapshot.entries().size()) {
            return;
        }
        String itemKey = snapshot.entries().get(rowIndex).itemId();
        ResourceLocation itemId = ResourceLocation.parse(itemKey);
        setRationAllowance(player, data, civId, itemId, count);
    }

    private static void setRationAllowance(ServerPlayer player, CivSavedData data, String civId,
                                           ResourceLocation itemId, int count) {
        int safeCount = Math.max(0, count);
        if (data.setHubDailyAllowanceLimit(civId, itemId, safeCount, player.getGameProfile().getName())) {
            if (safeCount <= 0) {
                player.sendSystemMessage(Component.literal("Cleared daily allowance for " + itemId + "."));
            } else {
                player.sendSystemMessage(Component.literal("Allowance for " + itemId + ": " + safeCount + "/day"));
            }
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
            int index = page * HubStockSnapshotBuilder.ROWS_PER_PAGE + (actionId - 1000);
            performWithdrawal(player, data, civId, index, 1);
        } else if (actionId >= 2000 && actionId < 3000) {
            int index = page * HubStockSnapshotBuilder.ROWS_PER_PAGE + (actionId - 2000);
            performWithdrawal(player, data, civId, index, 64);
        } else if (actionId >= 3000 && actionId < 4000 && canManage) {
            int index = page * HubStockSnapshotBuilder.ROWS_PER_PAGE + (actionId - 3000);
            adjustAllowance(player, data, civId, index, 1);
        } else if (actionId >= 4000 && actionId < 5000 && canManage) {
            int index = page * HubStockSnapshotBuilder.ROWS_PER_PAGE + (actionId - 4000);
            adjustAllowance(player, data, civId, index, -1);
        } else if ((actionId >= 3000 && actionId < 5000) && !canManage) {
            RealCivMessages.deny(player, "You do not have permission to adjust hub daily allowances.");
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
