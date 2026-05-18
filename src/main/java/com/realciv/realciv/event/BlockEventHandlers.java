package com.realciv.realciv.event;

import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.census.CensusSnapshotBuilder;
import com.realciv.realciv.command.RealCivCommands;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.*;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.hub.CommunityHubDepositContainer;
import com.realciv.realciv.hub.CommunityHubDepositMenu;
import com.realciv.realciv.hub.HubStockSnapshotBuilder;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshotBuilder;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.LandWandService;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.ProfessionEventHook;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.network.RealCivNetwork;
import com.realciv.realciv.tax.TaxSnapshotBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class BlockEventHandlers {
    private static final TagKey<Block> PICKAXE_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/pickaxe"));
    private static final TagKey<Block> SHOVEL_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/shovel"));
    private static final TagKey<Block> BAMBOO_BLOCKS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:bamboo_blocks"));

    private BlockEventHandlers() {
    }

    public static void handleRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState clickedState = event.getLevel().getBlockState(event.getPos());
        // Civic control blocks open custom dashboards; suppress item-use to prevent
        // client ghost stack/desync when right-clicking them with placeable items.
        if (isCivicControlBlock(clickedState)) {
            event.setUseItem(TriState.FALSE);
        }

        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        ItemStack held = event.getItemStack();

        if (held.is(ModBlocks.LAND_WAND.get())) {
            if (player.isShiftKeyDown()) {
                int radius = RealCivConfig.landWandVisualizeRadiusChunks();
                int edges = LandWandService.visualizeNearbyPlots(player, data, radius);
                player.sendSystemMessage(Component.literal(
                        "[RealCiv] Land Wand visualized " + edges
                                + " nearby boundary line(s) within " + radius + " chunks"
                                + " (all distinct nearby claim boundaries)."));
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            // POS1/POS2 SELECTION (DISABLED) - wand in hand does not interfere with block interaction
            // Previously called LandWandService.setPos2(player, event.getPos()) here.
        }

        if (clickedState.is(ModBlocks.COMMUNITY_HUB.get())) {
            if (player.isShiftKeyDown()) {
                openHubStockMenu(event, player, data);
            } else {
                openHubDepositMenu(event, player, data);
            }
            return;
        }

        if (clickedState.is(ModBlocks.CIVIC_CONTROL_CONSOLE.get())) {
            CivControlPanelEvents.openControlPanel(event, player, data);
            return;
        }

        if (clickedState.is(ModBlocks.CENSUS_BLOCK.get())) {
            openCensusPanel(event, player, data);
            return;
        }

        if (clickedState.is(ModBlocks.TAX_BLOCK.get())) {
            openTaxPanel(event, player, data);
            return;
        }

        if (clickedState.is(ModBlocks.PROFESSION_LEDGER.get())) {
            openProfessionLedgerPanel(event, player, data);
            return;
        }

        if (clickedState.is(ModBlocks.WAR_TABLE.get())) {
            openDiplomacyTablePanel(event, player, data);
            return;
        }

        if (isRegulatedExplosiveItem(held) && !canUseExplosivesExpertAction(player, data, true)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof RandomizableContainerBlockEntity container && container.getLootTable() != null) {
            if (!RealCivEvents.canProgressProfession(player, data, Profession.TREASURE_HUNTER, true)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
            int thLevel = record.levelFor(Profession.TREASURE_HUNTER);
            int limit = RealCivConfig.treasureHunterLimitForLevel(thLevel);
            if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.TREASURE_HUNTER, 1, "open chests")) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            if (record.treasureHunterActions() >= limit) {
                RealCivMessages.deny(
                        player,
                        "You can't open more treasure chests until you've returned to the Community Hub. "
                                + "Treasure Hunter limit reached (" + limit + ").");
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            record.setTreasureHunterActions(record.treasureHunterActions() + 1);
            record.addDailyProfessionActions(Profession.TREASURE_HUNTER, 1);
            data.setDirty();
            return;
        }
        if (isAnvilBlock(clickedState) && !RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ANVIL_USE,
                1,
                "You can't use an anvil right now until you've progressed your configured profession gate.")) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (clickedState.is(Blocks.CRAFTING_TABLE) && !RealCivUtil.isBypass(player)) {
            PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
            int crafterLevel = record.levelFor(Profession.CRAFTER);
            int limit = RealCivConfig.crafterLimitForLevel(crafterLevel);
            if (record.crafterActions() >= limit) {
                RealCivMessages.deny(
                        player,
                        "You can't use crafting tables until you've contributed crafted goods to the Community Hub. "
                                + "Crafter limit reached (" + limit + ").");
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.CRAFTER, 1, "craft")) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
        }

        if (isToolLocked(player, event.getItemStack(), data)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static void handleRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (isRegulatedExplosiveItem(event.getItemStack()) && !canUseExplosivesExpertAction(player, data, true)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        if (event.getItemStack().is(Items.FIREWORK_ROCKET) && player.isFallFlying()) {
            String civId = data.getOrAssignCivilization(player.getUUID());
            PlotLookup lookup = data.getPlotAnyCivilization(
                    player.serverLevel().dimension().location().toString(),
                    player.getBlockX() >> 4,
                    player.getBlockZ() >> 4);
            boolean inOwnCivTerritory = lookup != null && civId.equals(lookup.civilizationId());
            if (!inOwnCivTerritory) {
                if (!RealCivEvents.canProgressProfession(player, data, Profession.EXPLORER, true)) {
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
                int explorerLevel = record.levelFor(Profession.EXPLORER);
                int limit = RealCivConfig.explorerLimitForLevel(explorerLevel);
                if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.EXPLORER, 1, "explore")) {
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                if (record.explorerActions() >= limit) {
                    RealCivMessages.deny(
                            player,
                            "You can't use more fireworks until you've returned to the Community Hub. "
                                    + "Explorer limit reached (" + limit + ").");
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                    return;
                }
                record.setExplorerActions(record.explorerActions() + 1);
                record.addDailyProfessionActions(Profession.EXPLORER, 1);
                data.setDirty();
            }
            return;
        }
        if (event.getItemStack().is(ModBlocks.LAND_WAND.get())) {
            if (player.isShiftKeyDown()) {
                int radius = RealCivConfig.landWandVisualizeRadiusChunks();
                int edges = LandWandService.visualizeNearbyPlots(player, data, radius);
                player.sendSystemMessage(Component.literal(
                        "[RealCiv] Land Wand visualized " + edges
                                + " nearby boundary line(s) within " + radius + " chunks"
                                + " (all distinct nearby claim boundaries)."));
            } else {
                RealCivCommands.openLandGuiForPlayer(player, data);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (isToolLocked(player, event.getItemStack(), data)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static void handleLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        // POS1/POS2 SELECTION (DISABLED) - left-click with wand performs normal interaction
        // Previously called LandWandService.setPos1(player, event.getPos()) here.

        CivSavedData data = CivSavedData.get(player.getServer());
        if (isToolLocked(player, event.getItemStack(), data)) {
            event.setCanceled(true);
        }
    }

    public static void handleBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }

        BlockState placedBlock = event.getPlacedBlock();
        CivSavedData data = CivSavedData.get(player.getServer());
        long now = RealCivEvents.currentGameTime(player);
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean placingCommunityHub = placedBlock.is(ModBlocks.COMMUNITY_HUB.get());
        boolean canPlaceCivicControlBlocks = canRecoverCivicControlBlock(player, data, civId);

        if (isCivicControlBlock(placedBlock) && !canPlaceCivicControlBlocks) {
            RealCivMessages.deny(
                    player,
                    "Only civilization leadership can place civic control blocks.");
            event.setCanceled(true);
            return;
        }

        if ((placedBlock.is(Blocks.TNT) || placedBlock.is(Blocks.RESPAWN_ANCHOR))
                && !canUseExplosivesExpertAction(player, data, true)) {
            event.setCanceled(true);
            return;
        }
        if (isRegulatedRedstoneBlock(placedBlock) && !canUseRedstonerRole(player, data, true)) {
            event.setCanceled(true);
            return;
        }
        if (placedBlock.is(Blocks.SCAFFOLDING) && !RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.PLACE_SCAFFOLDING,
                1,
                "You can't place more scaffolding until you've contributed building materials to the Community Hub.")) {
            event.setCanceled(true);
            return;
        }

        boolean firstTownHubPlacement = false;
        if (placingCommunityHub) {
            @Nullable HubLocation existingHub = data.getHubLocation(civId);
            if (existingHub != null
                    && (!existingHub.dimension().equals(level.dimension().location().toString())
                    || existingHub.x() != event.getPos().getX()
                    || existingHub.y() != event.getPos().getY()
                    || existingHub.z() != event.getPos().getZ())) {
                RealCivMessages.deny(
                        player,
                        "Your civilization already has a Community Hub. Break/move the current hub first.");
                event.setCanceled(true);
                return;
            }

            if (!data.hasStarterTownAreaGranted(civId)) {
                if (!claimStarterTownAreaFromHub(player, data, civId, level, event.getPos(), now)) {
                    event.setCanceled(true);
                    return;
                }
                firstTownHubPlacement = true;
            }
            data.setHubLocation(
                    civId,
                    level.dimension().location().toString(),
                    event.getPos().getX(),
                    event.getPos().getY(),
                    event.getPos().getZ());
            data.addAuditLog(
                    civId,
                    player.getGameProfile().getName() + " placed Community Hub at "
                            + level.dimension().location() + "[" + event.getPos().getX() + ","
                            + event.getPos().getY() + "," + event.getPos().getZ() + "]",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            data.setDirty();
        }

        if (!firstTownHubPlacement && !canBuildInChunk(player, level, event.getPos(), data, now)) {
            PlotLookup lookup = data.getPlotAnyCivilization(
                    level.dimension().location().toString(),
                    event.getPos().getX() >> 4,
                    event.getPos().getZ() >> 4);
            if (lookup == null) {
                RealCivMessages.deny(
                        player,
                        "You can't place blocks in wilderness. Build in your civilization's COMMUNITY, CIVIC, or PRIVATE land.");
            } else {
                RealCivMessages.deny(
                        player,
                        "You can't build on this " + lookup.plot().landClass().name().toLowerCase(java.util.Locale.ROOT)
                                + " plot in civilization '" + lookup.civilizationId() + "'.");
            }
            event.setCanceled(true);
            player.inventoryMenu.broadcastFullState();
            return;
        }

        if (placedBlock.is(BlockTags.CROPS)) {
            if (!RealCivEvents.canProgressProfession(player, data, Profession.FARMER, true)) {
                event.setCanceled(true);
                return;
            }
            PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
            int farmerLevel = record.levelFor(Profession.FARMER);
            int limit = RealCivConfig.farmerLimitForLevel(farmerLevel);

            if (record.farmerActions() >= limit) {
                RealCivMessages.deny(
                        player,
                        "You can't place more crops right now. Farmer limit reached (" + limit
                                + "). Contribute harvested crops to the Community Hub to reset your farming limit.");
                event.setCanceled(true);
                return;
            }
            if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.FARMER, 1, "plant")) {
                event.setCanceled(true);
                return;
            }

            record.setFarmerActions(record.farmerActions() + 1);
            record.addDailyProfessionActions(Profession.FARMER, 1);
            data.setDirty();
        }
    }

    public static void handleBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        long now = RealCivEvents.currentGameTime(player);
        boolean authorizedHubMove = false;
        @Nullable String authorizedHubCiv = null;
        String dimension = level.dimension().location().toString();

        if (state.is(ModBlocks.COMMUNITY_HUB.get())) {
            String hubOwnerCiv = data.findCivilizationIdByHubPosition(
                    dimension,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
            if (hubOwnerCiv != null) {
                if (!canRecoverCivicControlBlock(player, data, hubOwnerCiv)) {
                    RealCivMessages.deny(player, "Only civilization leadership can move this Community Hub.");
                    event.setCanceled(true);
                    return;
                }
                authorizedHubMove = true;
                authorizedHubCiv = hubOwnerCiv;
            } else if (!player.hasPermissions(3)) {
                String civId = data.getOrAssignCivilization(player.getUUID());
                if (!canRecoverCivicControlBlock(player, data, civId)) {
                    RealCivMessages.deny(player, "Community Hub is protected.");
                    event.setCanceled(true);
                    return;
                }
            }
        } else if (isProtectedCivicControlBlock(state) && !player.hasPermissions(3)) {
            String civId = data.getOrAssignCivilization(player.getUUID());
            if (!canRecoverCivicControlBlock(player, data, civId)) {
                RealCivMessages.deny(player, "Only civilization leadership can recover civic control blocks.");
                event.setCanceled(true);
                return;
            }
        }

        if (!authorizedHubMove && !canBreakInChunk(player, level, pos, data, now)) {
            PlotLookup lookup = data.getPlotAnyCivilization(
                    dimension,
                    pos.getX() >> 4,
                    pos.getZ() >> 4);
            if (lookup == null) {
                if (RealCivConfig.blockUnclaimedBuilding()) {
                    RealCivMessages.deny(
                            player,
                            "You can't break blocks here. Wilderness breaking is disabled by server configuration.");
                } else {
                    RealCivMessages.deny(player, "You can't break blocks here. This chunk is not legally zoned.");
                }
            } else {
                RealCivMessages.deny(
                        player,
                        "You can't break blocks on this "
                                + lookup.plot().landClass().name().toLowerCase(java.util.Locale.ROOT)
                                + " plot in civilization '" + lookup.civilizationId() + "'.");
            }
            event.setCanceled(true);
            return;
        }

        if (authorizedHubMove && authorizedHubCiv != null) {
            data.clearHubLocation(authorizedHubCiv);
            data.addAuditLog(
                    authorizedHubCiv,
                    player.getGameProfile().getName() + " removed Community Hub from "
                            + dimension + "[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
        }

        if (isToolLocked(player, player.getMainHandItem(), data)) {
            event.setCanceled(true);
            return;
        }

        if (!player.isCreative()) {
            BreakProfession breakProfession = breakProfessionFor(state);
            if (breakProfession == BreakProfession.NONE) {
                return;
            }
            Profession profession = professionForBreakProfession(breakProfession);
            if (profession != Profession.NONE && !RealCivEvents.canProgressProfession(player, data, profession, true)) {
                event.setCanceled(true);
                return;
            }
            int actionCost = actionCostForState(state);

            PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
            if (breakProfession == BreakProfession.LUMBERJACK) {
                int levelValue = record.levelFor(Profession.LUMBERJACK);
                int limit = RealCivConfig.lumberjackLimitForLevel(levelValue);
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                int blockCap = RealCivConfig.lumberjackBlockActionCapForLevel(blockId, levelValue);
                if (blockCap > 0) {
                    int used = record.lumberjackBlockActions(blockId);
                    if (used + 1 > blockCap) {
                        RealCivMessages.deny(
                                player,
                                "Lumberjack block cap reached for " + blockId + " (" + used + "/" + blockCap + "). "
                                        + "Contribute wood at the Community Hub to reset your lumberjack window.");
                        event.setCanceled(true);
                        return;
                    }
                }
                int dailyBlockCap = RealCivConfig.lumberjackDailyBlockActionCapForLevel(blockId, levelValue);
                if (dailyBlockCap > 0) {
                    int dailyUsed = record.lumberjackDailyBlockActions(blockId);
                    if (dailyUsed + 1 > dailyBlockCap) {
                        RealCivMessages.deny(
                                player,
                                "Daily lumberjack cap reached for " + blockId + " (" + dailyUsed + "/" + dailyBlockCap + ").");
                        event.setCanceled(true);
                        return;
                    }
                }
                if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.LUMBERJACK, actionCost, "chop")) {
                    event.setCanceled(true);
                    return;
                }
                if (record.lumberjackActions() + actionCost > limit) {
                    RealCivMessages.deny(
                            player,
                            "You can't chop more wood until you've contributed wood to the Community Hub. "
                                    + "Lumberjack limit reached (" + record.lumberjackActions() + "/" + limit
                                    + ", this block costs " + actionCost + ").");
                    event.setCanceled(true);
                }
            } else if (breakProfession == BreakProfession.MINER) {
                int levelValue = record.levelFor(Profession.MINER);
                int limit = RealCivConfig.minerLimitForLevel(levelValue);
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                int blockCap = RealCivConfig.minerBlockActionCapForLevel(blockId, levelValue);
                if (blockCap > 0) {
                    int used = record.minerBlockActions(blockId);
                    if (used + 1 > blockCap) {
                        RealCivMessages.deny(
                                player,
                                "Miner block cap reached for " + blockId + " (" + used + "/" + blockCap + "). "
                                        + "Contribute mining output at the Community Hub to reset your miner window.");
                        event.setCanceled(true);
                        return;
                    }
                }
                int dailyBlockCap = RealCivConfig.minerDailyBlockActionCapForLevel(blockId, levelValue);
                if (dailyBlockCap > 0) {
                    int dailyUsed = record.minerDailyBlockActions(blockId);
                    if (dailyUsed + 1 > dailyBlockCap) {
                        RealCivMessages.deny(
                                player,
                                "Daily miner cap reached for " + blockId + " (" + dailyUsed + "/" + dailyBlockCap + ").");
                        event.setCanceled(true);
                        return;
                    }
                }
                if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.MINER, actionCost, "mine")) {
                    event.setCanceled(true);
                    return;
                }
                if (record.minerActions() + actionCost > limit) {
                    RealCivMessages.deny(
                            player,
                            "You can't mine more until you've returned your blocks to the Community Hub. "
                                    + "Miner limit reached (" + record.minerActions() + "/" + limit
                                    + ", this block costs " + actionCost + ").");
                    event.setCanceled(true);
                }
            } else if (breakProfession == BreakProfession.TERRAFORMER) {
                int levelValue = record.levelFor(Profession.TERRAFORMER);
                int limit = RealCivConfig.terraformerLimitForLevel(levelValue);
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                int blockCap = RealCivConfig.terraformerBlockActionCapForLevel(blockId, levelValue);
                if (blockCap > 0) {
                    int used = record.terraformerBlockActions(blockId);
                    if (used + 1 > blockCap) {
                        RealCivMessages.deny(
                                player,
                                "Terraformer block cap reached for " + blockId + " (" + used + "/" + blockCap + "). "
                                        + "Contribute earth materials at the Community Hub to reset your terraformer window.");
                        event.setCanceled(true);
                        return;
                    }
                }
                int dailyBlockCap = RealCivConfig.terraformerDailyBlockActionCapForLevel(blockId, levelValue);
                if (dailyBlockCap > 0) {
                    int dailyUsed = record.terraformerDailyBlockActions(blockId);
                    if (dailyUsed + 1 > dailyBlockCap) {
                        RealCivMessages.deny(
                                player,
                                "Daily terraformer cap reached for " + blockId + " (" + dailyUsed + "/" + dailyBlockCap + ").");
                        event.setCanceled(true);
                        return;
                    }
                }
                if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.TERRAFORMER, actionCost, "terraform")) {
                    event.setCanceled(true);
                    return;
                }
                if (record.terraformerActions() + actionCost > limit) {
                    RealCivMessages.deny(
                            player,
                            "You can't terraform more blocks until you've contributed earth materials to the Community Hub. "
                                    + "Terraformer limit reached (" + record.terraformerActions() + "/" + limit
                                    + ", this block costs " + actionCost + ").");
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void handleBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || player.getServer() == null || player.isCreative()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        BlockState state = event.getState();
        if (isCivicControlBlock(state)) {
            CivSavedData data = CivSavedData.get(player.getServer());
            @Nullable ItemStack recoveredStack = recoveredCivicBlockStack(state);
            if (recoveredStack == null) {
                return;
            }

            if (!player.hasPermissions(3)) {
                String dimension = player.level().dimension().location().toString();
                BlockPos pos = event.getPos();
                @Nullable String ownerCiv;
                if (state.is(ModBlocks.COMMUNITY_HUB.get())) {
                    ownerCiv = data.findCivilizationIdByHubPosition(dimension, pos.getX(), pos.getY(), pos.getZ());
                } else {
                    @Nullable PlotLookup lookup = data.getPlotAnyCivilization(
                            dimension,
                            pos.getX() >> 4,
                            pos.getZ() >> 4);
                    ownerCiv = lookup == null ? data.getOrAssignCivilization(player.getUUID()) : lookup.civilizationId();
                }
                if (ownerCiv == null || !canRecoverCivicControlBlock(player, data, ownerCiv)) {
                    event.getDrops().clear();
                    return;
                }
            }

            if (event.getDrops().isEmpty()) {
                ItemEntity blockEntity = new ItemEntity(
                        event.getLevel(),
                        event.getPos().getX(),
                        event.getPos().getY(),
                        event.getPos().getZ(),
                        recoveredStack.copy());
                event.getDrops().add(blockEntity);
            }
            player.sendSystemMessage(Component.literal(
                    recoveredStack.getHoverName().getString() + " recovered. You can place it again."));
            return;
        }

        BreakProfession breakProfession = breakProfessionFor(state);
        if (breakProfession == BreakProfession.NONE) {
            return;
        }
        int actionCost = actionCostForState(state);

        CivSavedData data = CivSavedData.get(player.getServer());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (breakProfession == BreakProfession.LUMBERJACK) {
            record.setLumberjackActions(record.lumberjackActions() + actionCost);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            record.addLumberjackBlockActions(blockId, 1);
            record.addLumberjackDailyBlockActions(blockId, 1);
            record.addDailyProfessionActions(Profession.LUMBERJACK, actionCost);
        } else if (breakProfession == BreakProfession.MINER) {
            record.setMinerActions(record.minerActions() + actionCost);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            record.addMinerBlockActions(blockId, 1);
            record.addMinerDailyBlockActions(blockId, 1);
            record.addDailyProfessionActions(Profession.MINER, actionCost);
        } else if (breakProfession == BreakProfession.TERRAFORMER) {
            record.setTerraformerActions(record.terraformerActions() + actionCost);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            record.addTerraformerBlockActions(blockId, 1);
            record.addTerraformerDailyBlockActions(blockId, 1);
            record.addDailyProfessionActions(Profession.TERRAFORMER, actionCost);
        }
        data.setDirty();
    }

    public static void handleBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        @Nullable ProfessionEventHook hook = hookForToolAbility(event.getItemAbility());
        if (hook == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                hook,
                1,
                "You can't perform more tool modifications right now until you've contributed to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    public static void handleFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.FARMLAND_TRAMPLE,
                1,
                "You can't trample more farmland right now until you've contributed to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    public static void handleUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
            return;
        }

        ItemStack held = event.getItemStack();

        if (held.getItem() instanceof BlockItem) {
            CivSavedData data = CivSavedData.get(player.getServer());
            long now = RealCivEvents.currentGameTime(player);

            boolean skipCheck = held.is(ModBlocks.COMMUNITY_HUB_ITEM.get())
                    && !data.hasStarterTownAreaGranted(data.getOrAssignCivilization(player.getUUID()));
            if (!skipCheck) {
                BlockPos checkPos = event.getFace() != null
                        ? event.getPos().relative(event.getFace())
                        : event.getPos();
                if (!canBuildInChunk(player, event.getLevel(), checkPos, data, now)) {
                    PlotLookup lookup = data.getPlotAnyCivilization(
                            event.getLevel().dimension().location().toString(),
                            checkPos.getX() >> 4,
                            checkPos.getZ() >> 4);
                    if (lookup == null) {
                        RealCivMessages.deny(
                                player,
                                "You can't place blocks in wilderness. Build in your civilization's COMMUNITY, CIVIC, or PRIVATE land.");
                    } else {
                        RealCivMessages.deny(
                                player,
                                "You can't build on this " + lookup.plot().landClass().name().toLowerCase(Locale.ROOT)
                                        + " plot in civilization '" + lookup.civilizationId() + "'.");
                    }
                    event.cancelWithResult(ItemInteractionResult.FAIL);
                    player.inventoryMenu.broadcastFullState();
                    return;
                }
            }
        }

        if (!RealCivEvents.isShearTool(held)) {
            return;
        }

        BlockState targetState = event.getLevel().getBlockState(event.getPos());
        if (targetState.getToolModifiedState(event.getUseOnContext(), ItemAbilities.SHEARS_HARVEST, true) != null) {
            return;
        }
        if (!(targetState.getBlock() instanceof IShearable shearable)) {
            return;
        }
        if (!shearable.isShearable(player, held, event.getLevel(), event.getPos())) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.SHEAR_BLOCK,
                1,
                "You can't shear more blocks/plants until you've contributed the related materials to the Community Hub.")) {
            event.cancelWithResult(ItemInteractionResult.FAIL);
        }
    }

    public static void handleBonemealUse(BonemealEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(player) || !event.isValidBonemealTarget()) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.BONEMEAL_USE,
                1,
                "You can't use more bonemeal right now until you've contributed farming resources to the Community Hub.")) {
            event.setSuccessful(false);
        }
    }

    public static void handleExplosionStart(ExplosionEvent.Start event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Explosion explosion = event.getExplosion();
        @Nullable Entity direct = explosion.getDirectSourceEntity();
        if (!isRegulatedExplosionSource(direct)) {
            return;
        }

        @Nullable ServerPlayer actor = responsiblePlayerForExplosion(explosion);
        if (actor == null || actor.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(actor)) {
            return;
        }

        CivSavedData data = CivSavedData.get(actor.getServer());
        if (!canUseExplosivesExpertAction(actor, data, true)) {
            event.setCanceled(true);
            return;
        }

        PlayerRecord record = data.getOrCreatePlayer(actor.getUUID());
        record.setExplosivesExpertActions(record.explosivesExpertActions() + 1);
        record.addDailyProfessionActions(Profession.EXPLOSIVES_EXPERT, 1);
        data.setDirty();
    }

    public static void handleExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide() || event.getLevel().getServer() == null) {
            return;
        }

        Level level = event.getLevel();
        Explosion explosion = event.getExplosion();
        @Nullable ServerPlayer actor = responsiblePlayerForExplosion(explosion);
        boolean actorBypass = actor != null && RealCivUtil.isBypass(actor);
        CivSavedData data = CivSavedData.get(level.getServer());

        String actorCiv = actor == null ? null : data.getOrAssignCivilization(actor.getUUID());
        java.util.Iterator<BlockPos> iterator = event.getAffectedBlocks().iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            @Nullable PlotLookup lookup = data.getPlotAnyCivilization(
                    level.dimension().location().toString(),
                    pos.getX() >> 4,
                    pos.getZ() >> 4);
            if (lookup == null) {
                continue;
            }
            if (actorBypass) {
                continue;
            }
            if (actor == null || actorCiv == null) {
                if (RealCivConfig.blockNonPlayerExplosionDamageInClaims()) {
                    iterator.remove();
                }
                continue;
            }

            String targetCiv = lookup.civilizationId();
            if (actorCiv.equals(targetCiv)) {
                if (!data.canBreakOnPlot(targetCiv, lookup.plot(), actor.getUUID(), false)) {
                    iterator.remove();
                }
                continue;
            }

            if (data.diplomacyState(actorCiv, targetCiv) != DiplomacyState.WAR) {
                iterator.remove();
            }
        }
    }

    private static void openHubDepositMenu(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        registerPendingWarriorHubProgress(player, data, civId);
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, playerInventory, p) ->
                                new CommunityHubDepositMenu(
                                        containerId, playerInventory, new CommunityHubDepositContainer(civId)),
                        Component.literal("Community Hub Deposit")),
                buffer -> buffer.writeUtf(civId, 128));

        player.sendSystemMessage(Component.literal(
                "Deposit mode for civilization '" + civId + "'. "
                        + "Place accepted stacks then close the window to contribute."));
        if (civId.equals(RealCivConfig.defaultCivilizationId())) {
            player.sendSystemMessage(Component.literal(
                    "Tip: create your own civilization with /realciv civ found <name>."));
        }
        player.sendSystemMessage(Component.literal(
                "Sneak + right click the Hub to open stock/withdraw pages."));
        player.sendSystemMessage(Component.literal(
                "Credits: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + " | Farmer " + record.farmerActions() + "/" + RealCivConfig.farmerLimitForLevel(record.levelFor(Profession.FARMER))
                        + " | Miner " + record.minerActions() + "/" + RealCivConfig.minerLimitForLevel(record.levelFor(Profession.MINER))
                        + " | Terraformer " + record.terraformerActions() + "/" + RealCivConfig.terraformerLimitForLevel(record.levelFor(Profession.TERRAFORMER))
                        + " | Lumberjack " + record.lumberjackActions() + "/" + RealCivConfig.lumberjackLimitForLevel(record.levelFor(Profession.LUMBERJACK))
                        + " | Fisher " + record.fisherActions() + "/" + RealCivConfig.fisherLimitForLevel(record.levelFor(Profession.FISHER))
                        + " | Hunter " + record.hunterActions() + "/" + RealCivConfig.hunterLimitForLevel(record.levelFor(Profession.HUNTER))
                        + " | Warrior " + record.warriorActions() + "/" + RealCivConfig.warriorLimitForLevel(record.levelFor(Profession.WARRIOR))
                        + " | Explosives " + record.explosivesExpertActions() + "/" + RealCivConfig.explosivesExpertLimitForLevel(record.levelFor(Profession.EXPLOSIVES_EXPERT))
                        + " | Crafter " + record.crafterActions() + "/" + RealCivConfig.crafterLimitForLevel(record.levelFor(Profession.CRAFTER))
                        + " | Enchanter " + record.enchanterActions() + "/" + RealCivConfig.enchanterLimitForLevel(record.levelFor(Profession.ENCHANTER))
                        + " | Brewer " + record.brewerActions() + "/" + RealCivConfig.brewerLimitForLevel(record.levelFor(Profession.BREWER))
                        + " | Trader " + record.traderActions() + "/" + RealCivConfig.traderLimitForLevel(record.levelFor(Profession.TRADER))));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openHubStockMenu(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        registerPendingWarriorHubProgress(player, data, civId);
        boolean canManagePolicy = CivPermissionService.hasCivPermission(
                player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);

        var snapshot = HubStockSnapshotBuilder.build(player, data, civId, canManagePolicy, 0);
        PacketDistributor.sendToPlayer(player, new com.realciv.realciv.network.RealCivPayloads.OpenHubStockPayload(snapshot));
        player.sendSystemMessage(Component.literal("Hub stock/withdraw opened for '" + civId + "'."));
        if (canManagePolicy) {
            player.sendSystemMessage(Component.literal("Leadership policy controls enabled."));
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openCensusPanel(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean canManage = CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)
                || data.isCivicManager(civId, player.getUUID());
        var snapshot = CensusSnapshotBuilder.build(player, data, civId, canManage, 0);
        PacketDistributor.sendToPlayer(player, new com.realciv.realciv.network.RealCivPayloads.OpenCensusPayload(snapshot));
        player.sendSystemMessage(Component.literal(
                "Census opened for " + RealCivUtil.civilizationDisplayName(data, civId) + "."));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openTaxPanel(
            PlayerInteractEvent.RightClickBlock event,
            ServerPlayer player,
            CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        var snapshot = TaxSnapshotBuilder.build(player, data, civId, 0);
        PacketDistributor.sendToPlayer(player, new com.realciv.realciv.network.RealCivPayloads.OpenTaxPayload(snapshot));
        player.sendSystemMessage(Component.literal("Tax Office opened for " + RealCivUtil.civilizationDisplayName(data, civId) + "."));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openProfessionLedgerPanel(
            PlayerInteractEvent.RightClickBlock event,
            ServerPlayer player,
            CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        var snapshot = ProfessionLedgerSnapshotBuilder.build(player, data, civId);
        PacketDistributor.sendToPlayer(player, new com.realciv.realciv.network.RealCivPayloads.OpenProfessionLedgerPayload(snapshot));
        player.sendSystemMessage(Component.literal("Profession Ledger opened."));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openDiplomacyTablePanel(
            PlayerInteractEvent.RightClickBlock event,
            ServerPlayer player,
            CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        var snapshot = RealCivNetwork.buildDiplomacySnapshotForPlayer(player, data, civId, 0);
        PacketDistributor.sendToPlayer(player, new com.realciv.realciv.network.RealCivPayloads.OpenDiplomacyPayload(snapshot));
        player.sendSystemMessage(Component.literal("Diplomacy Table opened."));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean claimStarterTownAreaFromHub(
            ServerPlayer player,
            CivSavedData data,
            String civId,
            Level level,
            BlockPos hubPos,
            long now) {
        int sideBlocks = RealCivConfig.hubStarterAreaBlocks();
        int half = sideBlocks / 2;
        int minX = hubPos.getX() - half;
        int minZ = hubPos.getZ() - half;
        int maxX = minX + sideBlocks - 1;
        int maxZ = minZ + sideBlocks - 1;
        long minChunkX = minX >> 4;
        long maxChunkX = maxX >> 4;
        long minChunkZ = minZ >> 4;
        long maxChunkZ = maxZ >> 4;
        String dimension = level.dimension().location().toString();

        if (!player.hasPermissions(3) && !RealCivConfig.canClaimDimension(dimension)) {
            RealCivMessages.deny(
                    player,
                    "Cannot seed starter town area here. Land claiming is disabled in dimension '" + dimension
                            + "' by server policy (" + RealCivConfig.claimDimensionPolicyLabel() + ").");
            return false;
        }

        for (long chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (long chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                @Nullable PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
                if (lookup != null && !lookup.civilizationId().equals(civId)) {
                    RealCivMessages.deny(
                            player,
                            "Cannot place first Community Hub here. Starter area overlaps civilization '"
                                    + lookup.civilizationId() + "' at chunk [" + chunkX + ", " + chunkZ + "].");
                    return false;
                }
            }
        }

        int claimed = 0;
        for (long chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (long chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.CIVIC, null, now, 0L);
                claimed++;
            }
        }
        data.addAuditLog(
                civId,
                player.getGameProfile().getName() + " seeded starter town area from Community Hub at "
                        + dimension + "[" + hubPos.getX() + "," + hubPos.getY() + "," + hubPos.getZ() + "]"
                        + " | side=" + sideBlocks + " blocks | claimed chunks=" + claimed,
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setStarterTownAreaGranted(civId, true);
        player.sendSystemMessage(Component.literal(
                "Starter town land claimed: " + claimed + " CIVIC chunk(s) around the Community Hub."));
        return true;
    }

    private static void registerPendingWarriorHubProgress(ServerPlayer player, CivSavedData data, String civId) {
        if (!RealCivConfig.warriorRequireHubRegistration() || RealCivUtil.isBypass(player)) {
            return;
        }
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int pendingKills = record.pendingWarriorHubRegistrations();
        if (pendingKills <= 0) {
            return;
        }

        int warriorXpGain = RealCivEvents.safeMultiply(RealCivConfig.warriorXpPerPlayerKill(), pendingKills);
        int generalXpGain = RealCivEvents.safeMultiply(RealCivConfig.warriorGeneralXpPerPlayerKill(), pendingKills);
        if (warriorXpGain > 0) {
            record.addWarriorXp(warriorXpGain);
        }
        if (generalXpGain > 0) {
            record.addGeneralXp(generalXpGain);
        }
        record.clearPendingWarriorHubRegistrations();
        data.addAuditLog(
                civId,
                player.getGameProfile().getName() + " registered " + pendingKills
                        + " warrior kill(s) at the Community Hub.",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        player.sendSystemMessage(Component.literal(
                "Community Hub registered " + pendingKills + " warrior kill(s). "
                        + "+" + warriorXpGain + " Warrior XP, +" + generalXpGain + " General XP."));
    }

    private static boolean isRegulatedExplosiveItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return RealCivConfig.regulatedExplosiveItems().contains(itemId);
    }

    private static boolean isRegulatedRedstoneBlock(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return RealCivConfig.regulatedRedstoneBlocks().contains(blockId);
    }

    private static boolean isAnvilBlock(BlockState state) {
        return state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL);
    }

    private static boolean isRegulatedExplosionSource(@Nullable Entity source) {
        return source instanceof PrimedTnt
                || source instanceof MinecartTNT
                || source instanceof EndCrystal
                || source instanceof WitherSkull
                || source instanceof WitherBoss;
    }

    private static Profession professionForBreakProfession(BreakProfession breakProfession) {
        return switch (breakProfession) {
            case MINER -> Profession.MINER;
            case TERRAFORMER -> Profession.TERRAFORMER;
            case LUMBERJACK -> Profession.LUMBERJACK;
            case NONE -> Profession.NONE;
        };
    }

    @Nullable
    private static ServerPlayer responsiblePlayerForExplosion(Explosion explosion) {
        @Nullable ServerPlayer indirect = RealCivEvents.resolvePlayerOwner(explosion.getIndirectSourceEntity());
        if (indirect != null) {
            return indirect;
        }
        return RealCivEvents.resolvePlayerOwner(explosion.getDirectSourceEntity());
    }

    private static BreakProfession breakProfessionFor(BlockState state) {
        if (state.isAir()
                || state.is(BlockTags.CROPS)
                || isCivicControlBlock(state)) {
            return BreakProfession.NONE;
        }
        if (state.is(BlockTags.LOGS) || state.is(BAMBOO_BLOCKS_TAG)) {
            return BreakProfession.LUMBERJACK;
        }
        if (state.is(PICKAXE_MINEABLE_TAG)) {
            return BreakProfession.MINER;
        }
        if (state.is(SHOVEL_MINEABLE_TAG)) {
            return BreakProfession.TERRAFORMER;
        }
        return BreakProfession.NONE;
    }

    private static int actionCostForState(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        int configured = RealCivConfig.breakActionCostOverrides().getOrDefault(blockId, 1);
        return Math.max(1, configured);
    }

    @Nullable
    private static ProfessionEventHook hookForToolAbility(ItemAbility ability) {
        String abilityName = ability.name();
        return switch (abilityName) {
            case "axe_strip" -> ProfessionEventHook.TOOL_STRIP_LOG;
            case "shears_harvest", "shears_carve", "shears_trim" -> ProfessionEventHook.SHEAR_BLOCK;
            case "till" -> ProfessionEventHook.TOOL_TILL_SOIL;
            case "shovel_flatten" -> ProfessionEventHook.TOOL_FLATTEN_PATH;
            case "shovel_douse" -> ProfessionEventHook.TOOL_DOUSE_CAMPFIRE;
            case "axe_scrape" -> ProfessionEventHook.TOOL_SCRAPE_COPPER;
            case "axe_wax_off" -> ProfessionEventHook.TOOL_WAX_OFF;
            default -> null;
        };
    }

    private static boolean canBuildInChunk(ServerPlayer player, Level level, BlockPos pos, CivSavedData data, long gameTime) {
        if (RealCivUtil.isBypass(player)) {
            return true;
        }

        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(
                level.dimension().location().toString(),
                pos.getX() >> 4,
                pos.getZ() >> 4);
        if (lookup == null) {
            return false;
        }
        return data.canBuildOnPlot(lookup.civilizationId(), lookup.plot(), player.getUUID(), false);
    }

    private static boolean canBreakInChunk(ServerPlayer player, Level level, BlockPos pos, CivSavedData data, long gameTime) {
        if (RealCivUtil.isBypass(player)) {
            return true;
        }

        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(
                level.dimension().location().toString(),
                pos.getX() >> 4,
                pos.getZ() >> 4);
        if (lookup == null) {
            return !RealCivConfig.blockUnclaimedBuilding();
        }
        return data.canBreakOnPlot(lookup.civilizationId(), lookup.plot(), player.getUUID(), false);
    }

    private static boolean isCivicControlBlock(BlockState state) {
        return state.is(ModBlocks.COMMUNITY_HUB.get())
                || state.is(ModBlocks.CENSUS_BLOCK.get())
                || state.is(ModBlocks.TAX_BLOCK.get())
                || state.is(ModBlocks.CIVIC_CONTROL_CONSOLE.get())
                || state.is(ModBlocks.PROFESSION_LEDGER.get())
                || state.is(ModBlocks.WAR_TABLE.get());
    }

    private static boolean isProtectedCivicControlBlock(BlockState state) {
        return state.is(ModBlocks.CENSUS_BLOCK.get())
                || state.is(ModBlocks.TAX_BLOCK.get())
                || state.is(ModBlocks.CIVIC_CONTROL_CONSOLE.get())
                || state.is(ModBlocks.PROFESSION_LEDGER.get())
                || state.is(ModBlocks.WAR_TABLE.get());
    }

    private static boolean canRecoverCivicControlBlock(ServerPlayer player, CivSavedData data, String civId) {
        if (player.hasPermissions(3) || RealCivUtil.isBypass(player)) {
            return true;
        }
        return CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LEADERSHIP)
                || CivPermissionService.hasCivPermission(player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)
                || data.isCivicManager(civId, player.getUUID());
    }

    @Nullable
    private static ItemStack recoveredCivicBlockStack(BlockState state) {
        if (state.is(ModBlocks.COMMUNITY_HUB.get())) {
            return ModBlocks.COMMUNITY_HUB_ITEM.get().getDefaultInstance();
        }
        if (state.is(ModBlocks.CENSUS_BLOCK.get())) {
            return ModBlocks.CENSUS_BLOCK_ITEM.get().getDefaultInstance();
        }
        if (state.is(ModBlocks.TAX_BLOCK.get())) {
            return ModBlocks.TAX_BLOCK_ITEM.get().getDefaultInstance();
        }
        if (state.is(ModBlocks.CIVIC_CONTROL_CONSOLE.get())) {
            return ModBlocks.CIVIC_CONTROL_CONSOLE_ITEM.get().getDefaultInstance();
        }
        if (state.is(ModBlocks.PROFESSION_LEDGER.get())) {
            return ModBlocks.PROFESSION_LEDGER_ITEM.get().getDefaultInstance();
        }
        if (state.is(ModBlocks.WAR_TABLE.get())) {
            return ModBlocks.WAR_TABLE_ITEM.get().getDefaultInstance();
        }
        return null;
    }

    private static boolean canUseExplosivesExpertAction(ServerPlayer player, CivSavedData data, boolean sendMessage) {
        if (RealCivUtil.isBypass(player)) {
            return true;
        }

        String civId = data.getOrAssignCivilization(player.getUUID());
        int maxExperts = RealCivConfig.maxExplosivesExpertsPerCivilization();
        if (maxExperts <= 0) {
            if (sendMessage) {
                RealCivMessages.deny(
                        player,
                        "Explosives are disabled by server configuration (max experts per civilization is 0).");
            }
            return false;
        }
        if (!data.isExplosivesExpert(civId, player.getUUID())) {
            if (sendMessage) {
                RealCivMessages.deny(
                        player,
                        "You are not designated as an Explosives Expert for your civilization. "
                                + "Ask your mayor to run /realciv civ explosives add <player>.");
            }
            return false;
        }
        if (!RealCivEvents.canProgressProfession(player, data, Profession.EXPLOSIVES_EXPERT, sendMessage)) {
            return false;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int level = record.levelFor(Profession.EXPLOSIVES_EXPERT);
        int limit = RealCivConfig.explosivesExpertLimitForLevel(level);
        if (record.explosivesExpertActions() >= limit) {
            if (sendMessage) {
                RealCivMessages.deny(
                        player,
                        "Explosives Expert limit reached (" + limit + "). "
                                + "Wait for timed recovery or level progression before more explosive actions.");
            }
            return false;
        }
        if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.EXPLOSIVES_EXPERT, 1, "use explosives", sendMessage)) {
            return false;
        }
        return true;
    }

    private static boolean canUseRedstonerRole(ServerPlayer player, CivSavedData data, boolean sendMessage) {
        if (RealCivUtil.isBypass(player)) {
            return true;
        }

        String civId = data.getOrAssignCivilization(player.getUUID());
        int maxRedstoners = RealCivConfig.maxRedstonersPerCivilization();
        if (maxRedstoners <= 0) {
            if (sendMessage) {
                RealCivMessages.deny(
                        player,
                        "Regulated redstone placement is disabled by server configuration (max redstoners per civilization is 0).");
            }
            return false;
        }
        if (!data.isRedstoner(civId, player.getUUID())) {
            if (sendMessage) {
                RealCivMessages.deny(
                        player,
                        "You are not designated as a Redstoner for your civilization. "
                                + "Ask your mayor to run /realciv civ redstoner add <player>.");
            }
            return false;
        }
        return true;
    }

    private static boolean isToolLocked(ServerPlayer player, ItemStack itemStack, CivSavedData data) {
        if (itemStack.isEmpty() || RealCivUtil.isBypass(player)) {
            return false;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (RealCivConfig.professionToolLevelGatesEnabled()) {
            @Nullable Profession requiredProfession = RealCivUtil.professionForTieredTool(itemStack);
            if (requiredProfession != null && requiredProfession != Profession.NONE) {
                int requiredProfessionLevel = RealCivConfig.requiredProfessionLevelForToolTier(
                        requiredProfession,
                        RealCivUtil.toolTier(itemStack));
                if (requiredProfessionLevel > 0) {
                    int currentProfessionLevel = record.levelFor(requiredProfession);
                    if (currentProfessionLevel < requiredProfessionLevel) {
                        RealCivMessages.deny(
                                player,
                                "Tool locked. " + RealCivEvents.readableProfessionName(requiredProfession)
                                        + " level " + requiredProfessionLevel + " is required for "
                                        + RealCivUtil.toolTierLabel(itemStack) + " tier tools (you are level "
                                        + currentProfessionLevel + ").");
                        return true;
                    }
                }
            }
        }

        if (RealCivConfig.generalToolLevelGatesEnabled()) {
            int requiredLevel = RealCivUtil.requiredGeneralLevelForTool(itemStack);
            if (requiredLevel > 0) {
                int generalLevel = record.generalLevel();
                if (generalLevel < requiredLevel) {
                    RealCivMessages.deny(
                            player,
                            "Tool locked. " + RealCivUtil.requiredToolLevelName(itemStack)
                                    + " tier requires general level " + requiredLevel + " (you are level " + generalLevel + ").");
                    return true;
                }
            }
        }
        return false;
    }

    private enum BreakProfession {
        NONE,
        MINER,
        TERRAFORMER,
        LUMBERJACK
    }
}
