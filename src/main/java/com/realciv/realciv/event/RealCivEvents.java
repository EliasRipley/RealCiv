package com.realciv.realciv.event;

import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.census.CensusMenu;
import com.realciv.realciv.command.RealCivCommands;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.logic.CarryCapService;
import com.realciv.realciv.logic.CraftingLimitService;
import com.realciv.realciv.logic.LandWandService;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.ProfessionEventHook;
import com.realciv.realciv.logic.ProfessionEventHookRule;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.hub.CommunityHubDepositContainer;
import com.realciv.realciv.hub.CommunityHubStockMenu;
import com.realciv.realciv.integration.RealCivFTBChunksMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.StatAwardEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

public final class RealCivEvents {
    private static final java.lang.reflect.Field RESULT_SLOT_REMOVE_COUNT_FIELD = findResultSlotRemoveCountField();
    private static final TagKey<Block> PICKAXE_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/pickaxe"));
    private static final TagKey<Block> SHOVEL_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/shovel"));
    private static final TagKey<Block> BAMBOO_BLOCKS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:bamboo_blocks"));
    private static final long UPKEEP_TICK_INTERVAL = 200L;
    private static final long TERRITORY_CHECK_INTERVAL = 10L;
    private static long lastUpkeepTick = Long.MIN_VALUE;
    private static final Map<UUID, TerritoryState> LAST_TERRITORY = new HashMap<>();
    private static final List<Profession> ACTION_TRACKED_PROFESSIONS = List.of(
            Profession.FARMER,
            Profession.MINER,
            Profession.TERRAFORMER,
            Profession.LUMBERJACK,
            Profession.FISHER,
            Profession.HUNTER,
            Profession.WARRIOR,
            Profession.EXPLOSIVES_EXPERT,
            Profession.CRAFTER);

    private RealCivEvents() {
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        long now = currentGameTime(player);
        data.processUpkeep(now);

        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (record.firstSeenAtMillis() <= 0L) {
            record.ensureFirstSeenAtMillis(System.currentTimeMillis());
            data.setDirty();
        }
        applyStaleActionTimeoutReset(player, data, record, System.currentTimeMillis());

        player.sendSystemMessage(Component.literal("RealCiv profile loaded."));
        player.sendSystemMessage(Component.literal(
                "Civilization: " + civId
                        + " | Credits: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + " | General Level: " + record.generalLevel()
                        + " | Farmer: " + record.levelFor(Profession.FARMER)
                        + " | Miner: " + record.levelFor(Profession.MINER)
                        + " | Terraformer: " + record.levelFor(Profession.TERRAFORMER)
                        + " | Lumberjack: " + record.levelFor(Profession.LUMBERJACK)
                        + " | Fisher: " + record.levelFor(Profession.FISHER)
                        + " | Hunter: " + record.levelFor(Profession.HUNTER)
                        + " | Warrior: " + record.levelFor(Profession.WARRIOR)
                        + " | Explosives: " + record.levelFor(Profession.EXPLOSIVES_EXPERT)
                        + " | Crafter: " + record.levelFor(Profession.CRAFTER)));
        if (RealCivConfig.specializationSingleProfessionLockEnabled()) {
            @Nullable Profession focus = record.focusedProfession();
            if (focus == null) {
                player.sendSystemMessage(Component.literal(
                        "Specialization lock is active. Set your focus with /realciv profession focus set <profession>."));
            } else {
                player.sendSystemMessage(Component.literal(
                        "Specialization focus: " + focus.name().toLowerCase(java.util.Locale.ROOT) + "."));
            }
        }
        if (civId.equals(RealCivConfig.defaultCivilizationId())) {
            if (RealCivConfig.requireFounderApproval()) {
                player.sendSystemMessage(Component.literal(
                        "You are in the default civilization. Ask an admin for founder approval before using /realciv civ found <name>."));
            } else {
                    player.sendSystemMessage(Component.literal(
                            "You are in the default civilization. Use /realciv civ found <name> to create your own civilization."));
            }
        }
        if (RealCivUtil.isBypass(player)) {
            player.sendSystemMessage(Component.literal(
                    "[RealCiv] Admin bypass is ACTIVE for your account. Limits and land restrictions will not apply."));
        }
        RealCivFTBChunksMirror.syncCivilization(player.getServer(), data, civId);
        LAST_TERRITORY.remove(player.getUUID());
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LAST_TERRITORY.remove(player.getUUID());
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }

        double ratio = RealCivConfig.deathActionRefundRatio();
        if (ratio <= 0.0D) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int totalRefund = 0;
        for (Profession profession : ACTION_TRACKED_PROFESSIONS) {
            int current = record.actionsForProfession(profession);
            int refund = refundActions(current, ratio);
            if (refund > 0) {
                record.setActionsForProfession(profession, current - refund);
                totalRefund += refund;
            }
        }

        if (totalRefund <= 0) {
            return;
        }

        data.setDirty();

        player.sendSystemMessage(Component.literal(
                "Death recovery refunded " + totalRefund + " profession action(s) total. "
                        + "Refund rate: " + RealCivUtil.formatPercentFromRatio(ratio) + "."));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().overworld() == null) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        if (now % TERRITORY_CHECK_INTERVAL == 0L) {
            CivSavedData data = CivSavedData.get(event.getServer());
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                sendTerritoryTransitionMessage(player, data);
            }
        }
        if (lastUpkeepTick != Long.MIN_VALUE && now - lastUpkeepTick < UPKEEP_TICK_INTERVAL) {
            return;
        }
        lastUpkeepTick = now;
        CivSavedData data = CivSavedData.get(event.getServer());
        data.processUpkeep(now);
        if (RealCivConfig.staleActionResetEnabled()) {
            long nowMillis = System.currentTimeMillis();
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
                applyStaleActionTimeoutReset(player, data, record, nowMillis);
            }
        }
    }

    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        ItemStack incoming = event.getItemEntity().getItem();
        if (incoming.isEmpty()) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!CarryCapService.canAcquireForPickup(player, data, incoming)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        BlockState clickedState = event.getLevel().getBlockState(event.getPos());
        ItemStack held = event.getItemStack();

        if (held.is(ModBlocks.LAND_WAND.get())) {
            if (player.isShiftKeyDown()) {
                int radius = RealCivConfig.landWandVisualizeRadiusChunks();
                int edges = LandWandService.visualizeNearbyPlots(player, data, radius);
                int selectedEdges = LandWandService.visualizeSelection(player);
                player.sendSystemMessage(Component.literal(
                        "[RealCiv] Land Wand visualized " + edges
                                + " nearby boundary line(s) within " + radius + " chunks"
                                + " (all distinct nearby claim boundaries)."
                                + (selectedEdges > 0 ? " Selection boundary lines: " + selectedEdges + "." : "")));
            } else {
                LandWandService.setPos2(player, event.getPos());
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (clickedState.is(ModBlocks.COMMUNITY_HUB.get())) {
            if (player.isShiftKeyDown()) {
                openHubStockMenu(event, player, data);
            } else {
                openHubDepositMenu(event, player, data);
            }
            return;
        }

        if (clickedState.is(ModBlocks.CENSUS_BLOCK.get())) {
            openCensusPanel(event, player, data);
            return;
        }

        if (clickedState.is(ModBlocks.TAX_BLOCK.get())) {
            openTaxPanel(event, player, data, player.isShiftKeyDown());
            return;
        }

        if (isRegulatedExplosiveItem(held) && !canUseExplosivesExpertAction(player, data, true)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        if (isAnvilBlock(clickedState) && !tryConsumeConfiguredHookActions(
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
            CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
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
        }

        if (isToolLocked(player, event.getItemStack(), data)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
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
        if (event.getItemStack().is(ModBlocks.LAND_WAND.get())) {
            if (player.isShiftKeyDown()) {
                int radius = RealCivConfig.landWandVisualizeRadiusChunks();
                int edges = LandWandService.visualizeNearbyPlots(player, data, radius);
                int selectedEdges = LandWandService.visualizeSelection(player);
                player.sendSystemMessage(Component.literal(
                        "[RealCiv] Land Wand visualized " + edges
                                + " nearby boundary line(s) within " + radius + " chunks"
                                + " (all distinct nearby claim boundaries)."
                                + (selectedEdges > 0 ? " Selection boundary lines: " + selectedEdges + "." : "")));
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

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        if (event.getItemStack().is(ModBlocks.LAND_WAND.get())) {
            LandWandService.setPos1(player, event.getPos());
            event.setCanceled(true);
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (isToolLocked(player, event.getItemStack(), data)) {
            event.setCanceled(true);
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }

        BlockState placedBlock = event.getPlacedBlock();
        CivSavedData data = CivSavedData.get(player.getServer());
        long now = currentGameTime(player);
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean placingCommunityHub = placedBlock.is(ModBlocks.COMMUNITY_HUB.get());
        boolean mayorOrAdmin = player.hasPermissions(3) || data.isMayor(civId, player.getUUID());

        if ((placedBlock.is(ModBlocks.COMMUNITY_HUB.get())
                || placedBlock.is(ModBlocks.CENSUS_BLOCK.get())
                || placedBlock.is(ModBlocks.TAX_BLOCK.get()))
                && !mayorOrAdmin) {
            RealCivMessages.deny(
                    player,
                    "Only your civilization mayor (or admins) can place civic control blocks (Hub/Census/Tax).");
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
        if (placedBlock.is(Blocks.SCAFFOLDING) && !tryConsumeConfiguredHookActions(
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
            @Nullable CivSavedData.HubLocation existingHub = data.getHubLocation(civId);
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
            CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(
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
            return;
        }

        if (placedBlock.is(BlockTags.CROPS)) {
            if (!canProgressProfession(player, data, Profession.FARMER, true)) {
                event.setCanceled(true);
                return;
            }
            CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
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

            record.setFarmerActions(record.farmerActions() + 1);
            data.setDirty();
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        long now = currentGameTime(player);
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
                if (!player.hasPermissions(3) && !data.isMayor(hubOwnerCiv, player.getUUID())) {
                    RealCivMessages.deny(player, "Only the owning civilization mayor can move this Community Hub.");
                    event.setCanceled(true);
                    return;
                }
                authorizedHubMove = true;
                authorizedHubCiv = hubOwnerCiv;
            } else if (!player.hasPermissions(3)) {
                String civId = data.getOrAssignCivilization(player.getUUID());
                if (!data.isMayor(civId, player.getUUID())) {
                    RealCivMessages.deny(player, "Community Hub is protected.");
                    event.setCanceled(true);
                    return;
                }
            }
        } else if ((state.is(ModBlocks.CENSUS_BLOCK.get()) || state.is(ModBlocks.TAX_BLOCK.get()))
                && !player.hasPermissions(3)) {
            String civId = data.getOrAssignCivilization(player.getUUID());
            if (!data.isMayor(civId, player.getUUID())) {
                RealCivMessages.deny(player, "Civic control blocks (Hub/Census/Tax) are protected.");
                event.setCanceled(true);
                return;
            }
        }

        if (!authorizedHubMove && !canBreakInChunk(player, level, pos, data, now)) {
            CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(
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
            if (profession != Profession.NONE && !canProgressProfession(player, data, profession, true)) {
                event.setCanceled(true);
                return;
            }
            int actionCost = actionCostForState(state);

            CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
            if (breakProfession == BreakProfession.LUMBERJACK) {
                int levelValue = record.levelFor(Profession.LUMBERJACK);
                int limit = RealCivConfig.lumberjackLimitForLevel(levelValue);
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

    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || player.getServer() == null || player.isCreative()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        BlockState state = event.getState();
        BreakProfession breakProfession = breakProfessionFor(state);
        if (breakProfession == BreakProfession.NONE) {
            return;
        }
        int actionCost = actionCostForState(state);

        CivSavedData data = CivSavedData.get(player.getServer());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (breakProfession == BreakProfession.LUMBERJACK) {
            record.setLumberjackActions(record.lumberjackActions() + actionCost);
        } else if (breakProfession == BreakProfession.MINER) {
            record.setMinerActions(record.minerActions() + actionCost);
        } else if (breakProfession == BreakProfession.TERRAFORMER) {
            record.setTerraformerActions(record.terraformerActions() + actionCost);
        }
        data.setDirty();
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker) || attacker.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(attacker)) {
            return;
        }

        CivSavedData data = CivSavedData.get(attacker.getServer());
        if (event.getTarget() instanceof EndCrystal && !canUseExplosivesExpertAction(attacker, data, true)) {
            event.setCanceled(true);
            return;
        }

        if (event.getTarget() instanceof ServerPlayer target) {
            if (attacker.getUUID().equals(target.getUUID())) {
                return;
            }

            String attackerCiv = data.getOrAssignCivilization(attacker.getUUID());
            String targetCiv = data.getOrAssignCivilization(target.getUUID());
            if (!isPlayerCombatAllowed(attacker, target, data, attackerCiv, targetCiv)) {
                event.setCanceled(true);
                return;
            }
            if (!shouldCountWarriorProgress(attackerCiv, targetCiv, data)) {
                return;
            }
            if (!canProgressProfession(attacker, data, Profession.WARRIOR, true)) {
                event.setCanceled(true);
                return;
            }

            CivSavedData.PlayerRecord record = data.getOrCreatePlayer(attacker.getUUID());
            int warriorLevel = record.levelFor(Profession.WARRIOR);
            int limit = RealCivConfig.warriorLimitForLevel(warriorLevel);
            if (record.warriorActions() >= limit) {
                notifyWarriorLimitReached(attacker, limit);
                event.setCanceled(true);
            }
            return;
        }

        if (!(event.getTarget() instanceof Mob)) {
            return;
        }
        if (!canProgressProfession(attacker, data, Profession.HUNTER, true)) {
            event.setCanceled(true);
            return;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(attacker.getUUID());
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int limit = RealCivConfig.hunterLimitForLevel(hunterLevel);
        if (record.hunterActions() >= limit) {
            RealCivMessages.deny(
                    attacker,
                    "You can't kill another mob until you've contributed mob loot to the Community Hub. "
                            + "Hunter limit reached (" + limit + ").");
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && !victim.level().isClientSide()) {
            ServerPlayer killer = getResponsiblePlayer(event.getSource());
            if (killer == null || killer.getServer() == null || killer.getUUID().equals(victim.getUUID()) || RealCivUtil.isBypass(killer)) {
                return;
            }

            CivSavedData data = CivSavedData.get(killer.getServer());
            String killerCiv = data.getOrAssignCivilization(killer.getUUID());
            String victimCiv = data.getOrAssignCivilization(victim.getUUID());
            if (!isPlayerCombatAllowed(killer, victim, data, killerCiv, victimCiv)) {
                event.setCanceled(true);
                if (victim.getHealth() <= 0.0F) {
                    victim.setHealth(1.0F);
                }
                return;
            }
            if (!shouldCountWarriorProgress(killerCiv, victimCiv, data)) {
                return;
            }
            if (!canProgressProfession(killer, data, Profession.WARRIOR, true)) {
                event.setCanceled(true);
                if (victim.getHealth() <= 0.0F) {
                    victim.setHealth(1.0F);
                }
                return;
            }

            CivSavedData.PlayerRecord killerRecord = data.getOrCreatePlayer(killer.getUUID());
            int warriorLevel = killerRecord.levelFor(Profession.WARRIOR);
            int limit = RealCivConfig.warriorLimitForLevel(warriorLevel);
            if (killerRecord.warriorActions() >= limit) {
                notifyWarriorLimitReached(killer, limit);
                event.setCanceled(true);
                if (victim.getHealth() <= 0.0F) {
                    victim.setHealth(1.0F);
                }
                return;
            }

            killerRecord.setWarriorActions(killerRecord.warriorActions() + 1);
            killerRecord.addWarriorXp(RealCivConfig.warriorXpPerPlayerKill());
            killerRecord.addGeneralXp(RealCivConfig.warriorGeneralXpPerPlayerKill());
            data.setDirty();
            return;
        }

        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }

        ServerPlayer killer = getResponsiblePlayer(event.getSource());
        if (killer == null || killer.getServer() == null || RealCivUtil.isBypass(killer)) {
            return;
        }

        CivSavedData data = CivSavedData.get(killer.getServer());
        if (!canProgressProfession(killer, data, Profession.HUNTER, true)) {
            event.setCanceled(true);
            if (mob.getHealth() <= 0.0F) {
                mob.setHealth(1.0F);
            }
            return;
        }
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(killer.getUUID());
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int limit = RealCivConfig.hunterLimitForLevel(hunterLevel);
        if (record.hunterActions() >= limit) {
            RealCivMessages.deny(
                    killer,
                    "You can't kill another mob until you've contributed mob loot to the Community Hub. "
                            + "Hunter limit reached (" + limit + ").");
            event.setCanceled(true);
            if (mob.getHealth() <= 0.0F) {
                mob.setHealth(1.0F);
            }
            return;
        }

        record.setHunterActions(record.hunterActions() + 1);
        data.setDirty();
    }

    /**
     * Optional profession hook: animal breeding can consume profession action budget.
     */
    public static void onAnimalBreed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ANIMAL_BREED,
                1,
                "You can't breed more animals until you've contributed farm resources to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    /**
     * Optional profession hook: animal taming can consume profession action budget.
     */
    public static void onAnimalTame(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ANIMAL_TAME,
                1,
                "You can't tame more animals until you've contributed related resources to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    /**
     * Optional profession hook: villager interaction can be gated before opening trade flows.
     */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        if (!(event.getTarget() instanceof AbstractVillager)) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.VILLAGER_INTERACT,
                1,
                "You can't interact with villagers right now until you've contributed to the Community Hub.")) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    /**
     * Optional profession hook: entity shearing can be limited before vanilla entity interaction runs.
     */
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        if (!isShearTool(event.getItemStack())) {
            return;
        }
        if (!(event.getTarget() instanceof IShearable shearable)) {
            return;
        }
        if (!shearable.isShearable(player, event.getItemStack(), player.serverLevel(), event.getTarget().blockPosition())) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.SHEAR_ENTITY,
                1,
                "You can't shear more entities until you've contributed the related materials to the Community Hub.")) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    /**
     * Optional profession hook: tool-based block modifications (strip/till/path/scrape/wax off).
     */
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
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
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                hook,
                1,
                "You can't perform more tool modifications right now until you've contributed to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    /**
     * Optional profession hook: farmland trampling can be limited or blocked.
     */
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
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
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.FARMLAND_TRAMPLE,
                1,
                "You can't trample more farmland right now until you've contributed to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    /**
     * Optional profession hook: block shearing can be limited.
     */
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
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
        if (!isShearTool(event.getItemStack())) {
            return;
        }

        BlockState targetState = event.getLevel().getBlockState(event.getPos());
        // If this path can be represented as a tool modification, we let BlockToolModificationEvent own it.
        if (targetState.getToolModifiedState(event.getUseOnContext(), ItemAbilities.SHEARS_HARVEST, true) != null) {
            return;
        }
        if (!(targetState.getBlock() instanceof IShearable shearable)) {
            return;
        }
        if (!shearable.isShearable(player, event.getItemStack(), event.getLevel(), event.getPos())) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.SHEAR_BLOCK,
                1,
                "You can't shear more blocks/plants until you've contributed the related materials to the Community Hub.")) {
            event.cancelWithResult(ItemInteractionResult.FAIL);
        }
    }

    /**
     * Optional profession hook: bone meal usage can consume profession action budget.
     */
    public static void onBonemealUse(BonemealEvent event) {
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
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.BONEMEAL_USE,
                1,
                "You can't use more bonemeal right now until you've contributed farming resources to the Community Hub.")) {
            event.setSuccessful(false);
        }
    }

    /**
     * Optional profession hook: completed villager trades can be counted for quotas, XP, and action costs.
     * This event is not cancellable in NeoForge, so it is best used for accounting policies.
     */
    public static void onVillagerTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        CivSavedData data = CivSavedData.get(player.getServer());
        tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.VILLAGER_TRADE,
                1,
                "You can't complete additional villager trades right now until your configured progression gate is met.");
    }

    /**
     * Optional profession hook: anvil result taken.
     * This event is post-action in vanilla flow and is best used for accounting/XP.
     */
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        CivSavedData data = CivSavedData.get(player.getServer());
        tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ANVIL_REPAIR,
                1,
                "You can't take more anvil outputs right now until your configured progression gate is met.");
    }

    /**
     * Optional profession hook: smelting output collected by player.
     */
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        ItemStack smelted = event.getSmelting();
        int triggerCount = smelted.isEmpty() ? 1 : Math.max(1, smelted.getCount());
        CivSavedData data = CivSavedData.get(player.getServer());
        tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ITEM_SMELT,
                triggerCount,
                "You can't smelt additional items right now until your configured progression gate is met.");
    }

    /**
     * Optional profession hook: enchanting completed by player.
     */
    public static void onItemEnchanted(PlayerEnchantItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        int triggerCount = Math.max(1, event.getEnchantments().size());
        CivSavedData data = CivSavedData.get(player.getServer());
        tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ITEM_ENCHANT,
                triggerCount,
                "You can't apply additional enchantments right now until your configured progression gate is met.");
    }

    /**
     * Optional profession hook: brewed potion taken from stand by player.
     */
    public static void onPotionBrewed(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        ItemStack brewed = event.getStack();
        int triggerCount = brewed.isEmpty() ? 1 : Math.max(1, brewed.getCount());
        CivSavedData data = CivSavedData.get(player.getServer());
        tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.POTION_BREW,
                triggerCount,
                "You can't brew additional potions right now until your configured progression gate is met.");
    }

    /**
     * Optional profession hook: player tosses items into the world.
     */
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        int triggerCount = 1;
        ItemStack tossed = event.getEntity().getItem();
        if (!tossed.isEmpty()) {
            triggerCount = Math.max(1, tossed.getCount());
        }
        CivSavedData data = CivSavedData.get(player.getServer());
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ITEM_TOSS,
                triggerCount,
                "You can't toss additional items right now until your configured progression gate is met.")) {
            event.setCanceled(true);
        }
    }

    /**
     * Optional profession hook: stat progression/counters from vanilla stat awards.
     */
    public static void onStatAward(StatAwardEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        int triggerCount = Math.max(1, event.getValue());
        String statId = event.getStat().toString();
        CivSavedData data = CivSavedData.get(player.getServer());
        if (!tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.STAT_AWARD,
                triggerCount,
                "You can't progress additional tracked stats right now until your configured progression gate is met.",
                statId)) {
            event.setCanceled(true);
        }
    }

    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        int fishCaught = 0;
        for (ItemStack drop : event.getDrops()) {
            if (!drop.isEmpty() && drop.is(ItemTags.FISHES)) {
                fishCaught += Math.max(0, drop.getCount());
            }
        }
        if (fishCaught <= 0) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!canProgressProfession(player, data, Profession.FISHER, true)) {
            event.setCanceled(true);
            return;
        }
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int fisherLevel = record.levelFor(Profession.FISHER);
        int limit = RealCivConfig.fisherLimitForLevel(fisherLevel);
        if (record.fisherActions() + fishCaught > limit) {
            RealCivMessages.deny(
                    player,
                    "You can't catch more fish until you've contributed fish to the Community Hub. "
                            + "Fisher limit reached (" + record.fisherActions() + "/" + limit
                            + ", this catch would add " + fishCaught + ").");
            event.setCanceled(true);
            return;
        }

        record.setFisherActions(record.fisherActions() + fishCaught);
        data.setDirty();
    }

    public static void onExplosionStart(ExplosionEvent.Start event) {
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

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(actor.getUUID());
        record.setExplosivesExpertActions(record.explosivesExpertActions() + 1);
        record.addExplosivesExpertXp(RealCivConfig.explosivesExpertXpPerUse());
        record.addGeneralXp(RealCivConfig.explosivesExpertGeneralXpPerUse());
        data.setDirty();
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
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
            @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(
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

            if (data.diplomacyState(actorCiv, targetCiv) != CivSavedData.DiplomacyState.WAR) {
                iterator.remove();
            }
        }
    }

    private static boolean isPlayerCombatAllowed(
            ServerPlayer attacker,
            ServerPlayer target,
            CivSavedData data,
            String attackerCiv,
            String targetCiv) {
        if (attackerCiv.equals(targetCiv)) {
            if (data.allowIntraCivPvp(attackerCiv)) {
                return true;
            }
            RealCivMessages.deny(
                    attacker,
                    "Friendly fire is disabled for your civilization. "
                            + "Your mayor can enable it with /realciv civ pvp friendlyfire on.");
            return false;
        }

        CivSavedData.DiplomacyState relation = data.diplomacyState(attackerCiv, targetCiv);
        if (relation == CivSavedData.DiplomacyState.WAR) {
            return true;
        }
        if (relation == CivSavedData.DiplomacyState.ALLY) {
            RealCivMessages.deny(
                    attacker,
                    "You cannot attack allied civilization members. "
                            + "Ask your mayor to adjust diplomacy first.");
            return false;
        }

        RealCivMessages.deny(
                attacker,
                "You can only attack players from civilizations your civilization is at war with.");
        return false;
    }

    private static boolean shouldCountWarriorProgress(String attackerCiv, String targetCiv, CivSavedData data) {
        return !attackerCiv.equals(targetCiv)
                && data.diplomacyState(attackerCiv, targetCiv) == CivSavedData.DiplomacyState.WAR;
    }

    private static void notifyWarriorLimitReached(ServerPlayer player, int limit) {
        RealCivMessages.deny(
                player,
                "Warrior limit reached (" + limit + "). "
                        + "You cannot kill another enemy player until your warrior cap resets.");
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int limit = RealCivConfig.crafterLimitForLevel(crafterLevel);
        int craftedCount = resolveCraftedCount(event, crafted);
        int current = record.crafterActions();

        if (current >= limit) {
            CraftingLimitService.notifyCraftDenied(player, crafted);
            return;
        }

        int allowed = Math.max(0, limit - current);
        int applied = Math.min(allowed, craftedCount);
        record.setCrafterActions(current + applied);
        data.setDirty();

        if (craftedCount > applied) {
            CraftingLimitService.notifyCraftDenied(player, crafted);
        }
    }

    private static void openHubDepositMenu(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> ChestMenu.sixRows(
                        containerId,
                        playerInventory,
                        new CommunityHubDepositContainer(civId)),
                Component.literal("Community Hub Deposit")));

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
                        + " | Crafter " + record.crafterActions() + "/" + RealCivConfig.crafterLimitForLevel(record.levelFor(Profession.CRAFTER))));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openHubStockMenu(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean privileged = player.hasPermissions(3) || RealCivUtil.isBypass(player);

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) ->
                        new CommunityHubStockMenu(containerId, playerInventory, player, data, civId, privileged),
                Component.literal("Community Hub Stock")));
        player.sendSystemMessage(Component.literal(
                "Stock/withdraw mode for civilization '" + civId + "'. "
                        + "Left click: 1 stack, Right click: 1 item, Shift click: 4 stacks."));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openCensusPanel(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean canManage = player.hasPermissions(3)
                || RealCivUtil.isBypass(player)
                || data.isMayor(civId, player.getUUID())
                || data.isCivicManager(civId, player.getUUID());
        String title = "Census: " + civilizationDisplayName(data, civId);
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CensusMenu(containerId, playerInventory, player, data, civId, canManage),
                Component.literal(title)));
        player.sendSystemMessage(Component.literal(
                "Census opened for " + civilizationDisplayName(data, civId)
                        + ". Use left/right click actions inside the menu to manage members, requests, and invites."));
        if (canManage) {
            player.sendSystemMessage(Component.literal(
                    "Tip: /realciv census invite <player> sends invites, and requests can be approved/denied in the menu."));
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openTaxPanel(
            PlayerInteractEvent.RightClickBlock event,
            ServerPlayer player,
            CivSavedData data,
            boolean payOneCycleNow) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        int ownedPlots = data.privatePlotCountForOwner(civId, player.getUUID());
        if (ownedPlots <= 0) {
            RealCivMessages.deny(
                    player,
                    "You do not own any private plots in this civilization, so no upkeep tax is due.");
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        long upkeepPerPlot = RealCivConfig.upkeepCostCents();
        long cycleCost = upkeepPerPlot * ownedPlots;
        int delinquent = data.delinquentPrivatePlotCountForOwner(civId, player.getUUID());
        long nextTick = data.earliestPrivatePlotUpkeepTick(civId, player.getUUID());

        if (payOneCycleNow) {
            payPrivateUpkeepCycles(player, data, civId, 1, ownedPlots, cycleCost);
        } else {
            player.sendSystemMessage(Component.literal(
                    "Tax Office: private plots=" + ownedPlots
                            + " | delinquent=" + delinquent
                            + " | next upkeep tick=" + nextTick));
            player.sendSystemMessage(Component.literal(
                    "Cycle cost: " + RealCivUtil.formatCredits(cycleCost)
                            + " | Your balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))));
            player.sendSystemMessage(Component.literal(
                    "Sneak + right-click to prepay 1 upkeep cycle now, or run /realciv tax pay <cycles>."));
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void payPrivateUpkeepCycles(
            ServerPlayer player,
            CivSavedData data,
            String civId,
            int cycles,
            int ownedPlots,
            long cycleCost) {
        int safeCycles = Math.max(1, cycles);
        long totalCost = cycleCost * safeCycles;
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        long balance = record.socialCreditCents(civId);
        if (balance < totalCost) {
            RealCivMessages.deny(
                    player,
                    "Insufficient contribution karma for upkeep prepayment. Need "
                            + RealCivUtil.formatCredits(totalCost)
                            + ", you have " + RealCivUtil.formatCredits(balance) + ".");
            return;
        }

        long now = currentGameTime(player);
        int affectedPlots = data.prepayPrivatePlotUpkeep(civId, player.getUUID(), safeCycles, now, player.getGameProfile().getName());
        if (affectedPlots <= 0) {
            RealCivMessages.deny(player, "No private plots were eligible for upkeep prepayment.");
            return;
        }

        record.addSocialCreditCents(civId, -totalCost);
        data.addCivTreasuryCents(civId, totalCost);
        data.addAuditLog(
                civId,
                player.getGameProfile().getName() + " paid " + RealCivUtil.formatCredits(totalCost)
                        + " upkeep tax via Tax Block for " + affectedPlots + " plot(s)"
                        + " across " + safeCycles + " cycle(s).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        player.sendSystemMessage(Component.literal(
                "Upkeep tax paid for " + affectedPlots + " plot(s). Cost: "
                        + RealCivUtil.formatCredits(totalCost)
                        + " | New balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))));
        player.sendSystemMessage(Component.literal(
                "Civ collective contribution karma (" + civId + "): "
                        + RealCivUtil.formatCredits(data.civTreasuryCents(civId))));
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

        for (long chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (long chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
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

    private static void sendTerritoryTransitionMessage(ServerPlayer player, CivSavedData data) {
        TerritoryState current = territoryStateForPlayer(player, data);
        TerritoryState previous = LAST_TERRITORY.put(player.getUUID(), current);
        if (previous != null && previous.sameAs(current)) {
            return;
        }

        if (current.isWilderness()) {
            player.sendSystemMessage(Component.literal("You've entered true public wilderness."));
            return;
        }

        String civName = civilizationDisplayName(data, current.civilizationId());
        if (current.landClass() == LandClass.PRIVATE) {
            if (current.ownerId() != null && current.ownerId().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal(
                        "You've now entered your private plot in " + civName + "."));
            } else {
                player.sendSystemMessage(Component.literal(
                        "You've now entered " + ownerName(player, current.ownerId()) + "'s private plot in " + civName + "."));
            }
            return;
        }

        if (current.landClass() == LandClass.CIVIC) {
            player.sendSystemMessage(Component.literal(
                    "You've now entered " + civName + "'s territory."));
            return;
        }

        player.sendSystemMessage(Component.literal(
                "You've now entered community land of " + civName + "."));
    }

    private static TerritoryState territoryStateForPlayer(ServerPlayer player, CivSavedData data) {
        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup == null) {
            return TerritoryState.wilderness();
        }
        return TerritoryState.claimed(lookup.civilizationId(), lookup.plot().landClass(), lookup.plot().ownerId());
    }

    private static String civilizationDisplayName(CivSavedData data, @Nullable String civId) {
        if (civId == null) {
            return "Unknown Civilization";
        }
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
        return civ == null ? civId : civ.displayName();
    }

    private static String ownerName(ServerPlayer viewer, @Nullable UUID ownerId) {
        if (ownerId == null) {
            return "Unknown";
        }
        ServerPlayer online = viewer.getServer() == null ? null : viewer.getServer().getPlayerList().getPlayer(ownerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        String raw = ownerId.toString();
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }

    private static boolean canBuildInChunk(ServerPlayer player, Level level, BlockPos pos, CivSavedData data, long gameTime) {
        if (RealCivUtil.isBypass(player)) {
            return true;
        }

        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(
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

        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(
                level.dimension().location().toString(),
                pos.getX() >> 4,
                pos.getZ() >> 4);
        if (lookup == null) {
            return !RealCivConfig.blockUnclaimedBuilding();
        }
        return data.canBreakOnPlot(lookup.civilizationId(), lookup.plot(), player.getUUID(), false);
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
        @Nullable ServerPlayer indirect = resolvePlayerOwner(explosion.getIndirectSourceEntity());
        if (indirect != null) {
            return indirect;
        }
        return resolvePlayerOwner(explosion.getDirectSourceEntity());
    }

    @Nullable
    private static ServerPlayer resolvePlayerOwner(@Nullable Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }
        if (entity instanceof TraceableEntity traceable && traceable.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }
        return null;
    }

    /**
     * Applies all configured event hook rules for the hook (atomically) against profession action counters.
     */
    private static boolean tryConsumeConfiguredHookActions(
            ServerPlayer player,
            CivSavedData data,
            ProfessionEventHook hook,
            int triggerCount,
            String defaultDenyMessage) {
        return tryConsumeConfiguredHookActions(
                player,
                data,
                hook,
                triggerCount,
                defaultDenyMessage,
                null);
    }

    private static boolean tryConsumeConfiguredHookActions(
            ServerPlayer player,
            CivSavedData data,
            ProfessionEventHook hook,
            int triggerCount,
            String defaultDenyMessage,
            @Nullable String contextToken) {
        if (triggerCount <= 0) {
            return true;
        }
        int safeTriggerCount = Math.max(1, triggerCount);
        List<ProfessionEventHookRule> matchingRules = configuredHookRules(hook);
        if (matchingRules.isEmpty()) {
            return true;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        long nowMillis = System.currentTimeMillis();
        Map<Profession, PendingHookCharge> pendingByProfession = new HashMap<>();
        Map<Profession, Integer> pendingProfessionXpByProfession = new HashMap<>();
        Map<String, PendingWindowUsage> pendingWindowUsageByRule = new HashMap<>();
        int pendingGeneralXp = 0;
        boolean anyRuleApplied = false;
        boolean mutated = false;

        for (ProfessionEventHookRule rule : matchingRules) {
            if (!matchesRuleContext(rule, contextToken)) {
                continue;
            }
            Profession profession = rule.profession();
            if (profession == Profession.NONE) {
                continue;
            }
            if (!canProgressProfession(player, data, profession, true)) {
                return false;
            }

            int professionLevel = record.levelFor(profession);
            if (professionLevel < rule.minProfessionLevel()) {
                RealCivMessages.deny(
                        player,
                        formatHookDenyMessage(
                                rule,
                                hook,
                                professionLevel,
                                rule.minProfessionLevel(),
                                0,
                                rule.minProfessionLevel(),
                                0,
                                0L,
                                0,
                                0,
                                0,
                                defaultDenyMessage,
                                readableProfessionName(profession)
                                        + " level " + rule.minProfessionLevel()
                                        + " required (you are level " + professionLevel + ")."));
                return false;
            }
            int generalLevel = record.generalLevel();
            if (generalLevel < rule.minGeneralLevel()) {
                RealCivMessages.deny(
                        player,
                        formatHookDenyMessage(
                                rule,
                                hook,
                                generalLevel,
                                rule.minGeneralLevel(),
                                0,
                                0,
                                rule.minGeneralLevel(),
                                0L,
                                0,
                                0,
                                0,
                                defaultDenyMessage,
                                "General level " + rule.minGeneralLevel()
                                        + " required (you are level " + generalLevel + ")."));
                return false;
            }

            if (rule.minMembershipMillis() > 0L) {
                if (record.firstSeenAtMillis() <= 0L) {
                    record.ensureFirstSeenAtMillis(nowMillis);
                    mutated = true;
                    data.setDirty();
                }
                long membershipMillis = record.membershipMillis(nowMillis);
                if (membershipMillis < rule.minMembershipMillis()) {
                    long requiredHours = Math.max(1L, rule.minMembershipMillis() / 3_600_000L);
                    long currentHours = Math.max(0L, membershipMillis / 3_600_000L);
                    RealCivMessages.deny(
                            player,
                            formatHookDenyMessage(
                                    rule,
                                    hook,
                                    (int) Math.min(Integer.MAX_VALUE, currentHours),
                                    (int) Math.min(Integer.MAX_VALUE, requiredHours),
                                    0,
                                    0,
                                    0,
                                    rule.minMembershipMillis(),
                                    0,
                                    0,
                                    0,
                                    defaultDenyMessage,
                                    "Server membership time required: " + requiredHours
                                            + "h (you currently have " + currentHours + "h)."));
                    return false;
                }
            }

            if (rule.hasWindowQuota()) {
                long windowStartMillis = resolveWindowStartMillis(nowMillis, rule.windowSeconds());
                String ruleKey = rule.windowCounterKey();
                int persistedUsed = record.hookWindowUsed(ruleKey, windowStartMillis);
                PendingWindowUsage pendingUsage = pendingWindowUsageByRule.get(ruleKey);
                int pendingAlready = pendingUsage == null ? 0 : pendingUsage.addedTriggers();
                int usedAfterCurrentEvent = persistedUsed + pendingAlready + safeTriggerCount;
                if (usedAfterCurrentEvent > rule.maxTriggersPerWindow()) {
                    RealCivMessages.deny(
                            player,
                            formatHookDenyMessage(
                                    rule,
                                    hook,
                                    persistedUsed + pendingAlready,
                                    rule.maxTriggersPerWindow(),
                                    0,
                                    0,
                                    0,
                                    0L,
                                    persistedUsed + pendingAlready,
                                    rule.maxTriggersPerWindow(),
                                    rule.windowSeconds(),
                                    defaultDenyMessage,
                                    "Hook quota reached for this window ("
                                            + (persistedUsed + pendingAlready) + "/" + rule.maxTriggersPerWindow() + ")."));
                    return false;
                }
                if (pendingUsage == null) {
                    pendingWindowUsageByRule.put(
                            ruleKey,
                            new PendingWindowUsage(
                                    rule,
                                    windowStartMillis,
                                    safeTriggerCount,
                                    persistedUsed));
                } else {
                    pendingWindowUsageByRule.put(
                            ruleKey,
                            new PendingWindowUsage(
                                    pendingUsage.sourceRule(),
                                    pendingUsage.windowStartMillis(),
                                    pendingUsage.addedTriggers() + safeTriggerCount,
                                    pendingUsage.persistedUsed()));
                }
            }

            int addedCost = safeMultiply(rule.actionCost(), safeTriggerCount);
            if (addedCost > 0) {
                PendingHookCharge existing = pendingByProfession.get(profession);
                if (existing == null) {
                    pendingByProfession.put(profession, new PendingHookCharge(rule, addedCost));
                } else {
                    pendingByProfession.put(
                            profession,
                            new PendingHookCharge(existing.sourceRule(), existing.actionCost() + addedCost));
                }
            }

            int professionXpGain = safeMultiply(rule.professionXpPerTrigger(), safeTriggerCount);
            if (professionXpGain > 0) {
                pendingProfessionXpByProfession.merge(profession, professionXpGain, Integer::sum);
            }
            int generalXpGain = safeMultiply(rule.generalXpPerTrigger(), safeTriggerCount);
            if (generalXpGain > 0) {
                pendingGeneralXp = safeAdd(pendingGeneralXp, generalXpGain);
            }
            anyRuleApplied = true;
        }

        if (!anyRuleApplied) {
            if (mutated) {
                data.setDirty();
            }
            return true;
        }

        for (Map.Entry<Profession, PendingHookCharge> entry : pendingByProfession.entrySet()) {
            Profession profession = entry.getKey();
            PendingHookCharge pending = entry.getValue();
            int level = record.levelFor(profession);
            int limit = RealCivConfig.limitForProfession(profession, level);
            int current = record.actionsForProfession(profession);
            if (current + pending.actionCost() > limit) {
                String message = formatHookDenyMessage(
                        pending.sourceRule(),
                        hook,
                        current,
                        limit,
                        pending.actionCost(),
                        0,
                        0,
                        0L,
                        0,
                        0,
                        0,
                        defaultDenyMessage,
                        null);
                RealCivMessages.deny(player, message);
                return false;
            }
        }

        for (Map.Entry<Profession, PendingHookCharge> entry : pendingByProfession.entrySet()) {
            Profession profession = entry.getKey();
            int current = record.actionsForProfession(profession);
            record.setActionsForProfession(profession, current + entry.getValue().actionCost());
            mutated = true;
        }

        for (Map.Entry<String, PendingWindowUsage> entry : pendingWindowUsageByRule.entrySet()) {
            PendingWindowUsage pendingUsage = entry.getValue();
            int totalUsed = safeAdd(pendingUsage.persistedUsed(), pendingUsage.addedTriggers());
            record.setHookWindowUsed(entry.getKey(), pendingUsage.windowStartMillis(), totalUsed);
            mutated = true;
        }

        for (Map.Entry<Profession, Integer> xpEntry : pendingProfessionXpByProfession.entrySet()) {
            int xp = Math.max(0, xpEntry.getValue());
            if (xp <= 0) {
                continue;
            }
            record.addProfessionXp(xpEntry.getKey(), xp);
            mutated = true;
        }
        if (pendingGeneralXp > 0) {
            record.addGeneralXp(pendingGeneralXp);
            mutated = true;
        }

        if (mutated) {
            data.setDirty();
        }
        return true;
    }

    private static List<ProfessionEventHookRule> configuredHookRules(ProfessionEventHook hook) {
        ArrayList<ProfessionEventHookRule> matches = new ArrayList<>();
        for (ProfessionEventHookRule rule : RealCivConfig.professionEventHookRules()) {
            if (rule.hook() == hook) {
                matches.add(rule);
            }
        }
        return matches;
    }

    private record PendingHookCharge(ProfessionEventHookRule sourceRule, int actionCost) {
    }

    private record PendingWindowUsage(
            ProfessionEventHookRule sourceRule,
            long windowStartMillis,
            int addedTriggers,
            int persistedUsed) {
    }

    private static boolean matchesRuleContext(ProfessionEventHookRule rule, @Nullable String contextToken) {
        @Nullable String statPrefix = rule.statPrefixFilter();
        if (statPrefix == null || statPrefix.isBlank()) {
            return true;
        }
        if (contextToken == null || contextToken.isBlank()) {
            return false;
        }
        return contextToken.toLowerCase(Locale.ROOT).startsWith(statPrefix.toLowerCase(Locale.ROOT));
    }

    private static long resolveWindowStartMillis(long nowMillis, int windowSeconds) {
        if (windowSeconds <= 0) {
            return 0L;
        }
        long windowMillis = Math.max(1L, windowSeconds) * 1_000L;
        if (windowMillis <= 0L) {
            return 0L;
        }
        return Math.floorDiv(nowMillis, windowMillis) * windowMillis;
    }

    private static int safeMultiply(int left, int right) {
        long result = (long) left * (long) right;
        if (result <= 0L) {
            return 0;
        }
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) result;
    }

    private static int safeAdd(int left, int right) {
        long result = (long) left + (long) right;
        if (result <= 0L) {
            return 0;
        }
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) result;
    }

    private static String formatHookDenyMessage(
            ProfessionEventHookRule rule,
            ProfessionEventHook hook,
            int current,
            int limit,
            int actionCost,
            int requiredProfessionLevel,
            int requiredGeneralLevel,
            long requiredMembershipMillis,
            int windowUsed,
            int windowLimit,
            int windowSeconds,
            String fallback,
            @Nullable String fallbackDetail) {
        String template = rule.denyMessageOverride();
        if (template == null || template.isBlank()) {
            if (fallbackDetail == null || fallbackDetail.isBlank()) {
                return fallback + " "
                        + readableProfessionName(rule.profession()) + " limit reached ("
                        + current + "/" + limit + ", this action costs " + actionCost + ").";
            }
            return fallback + " " + fallbackDetail;
        }
        return template
                .replace("%hook%", hook.name().toLowerCase(Locale.ROOT))
                .replace("%profession%", readableProfessionName(rule.profession()))
                .replace("%current%", Integer.toString(current))
                .replace("%limit%", Integer.toString(limit))
                .replace("%cost%", Integer.toString(actionCost))
                .replace("%required_profession_level%", Integer.toString(requiredProfessionLevel))
                .replace("%required_general_level%", Integer.toString(requiredGeneralLevel))
                .replace("%required_membership_hours%", Long.toString(Math.max(0L, requiredMembershipMillis / 3_600_000L)))
                .replace("%window_used%", Integer.toString(windowUsed))
                .replace("%window_limit%", Integer.toString(windowLimit))
                .replace("%window_seconds%", Integer.toString(windowSeconds))
                .replace("%detail%", fallbackDetail == null ? "" : fallbackDetail);
    }

    private static boolean isShearTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.canPerformAction(ItemAbilities.SHEARS_HARVEST)
                || stack.canPerformAction(ItemAbilities.SHEARS_TRIM)
                || stack.canPerformAction(ItemAbilities.SHEARS_CARVE)
                || stack.canPerformAction(ItemAbilities.SHEARS_REMOVE_ARMOR)
                || stack.canPerformAction(ItemAbilities.SHEARS_DISARM);
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

    private static String readableProfessionName(Profession profession) {
        String raw = profession.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        if (raw.isEmpty()) {
            return "Profession";
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static boolean canProgressProfession(
            ServerPlayer player,
            CivSavedData data,
            Profession profession,
            boolean sendMessage) {
        if (profession == Profession.NONE || RealCivUtil.isBypass(player)) {
            return true;
        }
        if (!RealCivConfig.specializationSingleProfessionLockEnabled()) {
            return true;
        }
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        @Nullable Profession focused = record.focusedProfession();
        if (focused == null) {
            if (sendMessage) {
                RealCivMessages.deny(
                        player,
                        "Specialization lock is enabled. Choose your profession focus with "
                                + "/realciv profession focus set <profession>.");
            }
            return false;
        }
        if (focused != profession) {
            if (sendMessage) {
                RealCivMessages.deny(
                        player,
                        "Your profession focus is " + focused.name().toLowerCase(java.util.Locale.ROOT)
                                + ". You cannot progress "
                                + profession.name().toLowerCase(java.util.Locale.ROOT)
                                + " while focused elsewhere.");
            }
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
        if (!canProgressProfession(player, data, Profession.EXPLOSIVES_EXPERT, sendMessage)) {
            return false;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
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
        return true;
    }

    private static boolean isToolLocked(ServerPlayer player, ItemStack itemStack, CivSavedData data) {
        if (itemStack.isEmpty() || RealCivUtil.isBypass(player)) {
            return false;
        }

        int requiredLevel = RealCivUtil.requiredGeneralLevelForTool(itemStack);
        if (requiredLevel <= 0) {
            return false;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int generalLevel = record.generalLevel();
        if (generalLevel >= requiredLevel) {
            return false;
        }

        RealCivMessages.deny(
                player,
                "Tool locked. " + RealCivUtil.requiredToolLevelName(itemStack)
                        + " tier requires general level " + requiredLevel + " (you are level " + generalLevel + ").");
        return true;
    }

    private static long currentGameTime(ServerPlayer player) {
        if (player.getServer() == null || player.getServer().overworld() == null) {
            return 0L;
        }
        return player.getServer().overworld().getGameTime();
    }

    private static BreakProfession breakProfessionFor(BlockState state) {
        if (state.isAir()
                || state.is(BlockTags.CROPS)
                || state.is(ModBlocks.COMMUNITY_HUB.get())
                || state.is(ModBlocks.CENSUS_BLOCK.get())
                || state.is(ModBlocks.TAX_BLOCK.get())) {
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

    private static int refundActions(int spentActions, double ratio) {
        int current = Math.max(0, spentActions);
        if (current <= 0 || ratio <= 0.0D) {
            return 0;
        }
        int refunded = (int) Math.floor(current * ratio);
        if (refunded <= 0) {
            refunded = 1;
        }
        return Math.min(current, refunded);
    }

    private static void applyStaleActionTimeoutReset(
            ServerPlayer player,
            CivSavedData data,
            CivSavedData.PlayerRecord record,
            long nowMillis) {
        if (!RealCivConfig.staleActionResetEnabled()) {
            return;
        }
        long staleMillis = RealCivConfig.staleActionResetMillis();
        if (staleMillis <= 0L) {
            return;
        }

        int totalReset = 0;
        for (Profession profession : ACTION_TRACKED_PROFESSIONS) {
            int current = record.actionsForProfession(profession);
            int reset = staleResetValue(
                    current,
                    record.actionsUpdatedAtMillisForProfession(profession),
                    nowMillis,
                    staleMillis);
            if (reset > 0) {
                record.setActionsForProfession(profession, 0);
                totalReset += reset;
            }
        }

        if (totalReset <= 0) {
            return;
        }
        data.setDirty();

        player.sendSystemMessage(Component.literal(
                "Timed recovery reset " + totalReset + " stale profession action(s) after "
                        + (staleMillis / 60_000L) + " minute(s) without progression reset."));
    }

    private static int staleResetValue(int current, long lastUpdatedMillis, long nowMillis, long staleMillis) {
        if (current <= 0 || staleMillis <= 0L) {
            return 0;
        }
        if (lastUpdatedMillis <= 0L) {
            return 0;
        }
        if (nowMillis < lastUpdatedMillis) {
            return 0;
        }
        return (nowMillis - lastUpdatedMillis) >= staleMillis ? current : 0;
    }

    @Nullable
    private static ServerPlayer getResponsiblePlayer(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof ServerPlayer player) {
            return player;
        }

        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof ServerPlayer player) {
            return player;
        }
        if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }

        return null;
    }

    private record TerritoryState(
            @Nullable String civilizationId,
            @Nullable LandClass landClass,
            @Nullable UUID ownerId) {
        private static TerritoryState wilderness() {
            return new TerritoryState(null, null, null);
        }

        private static TerritoryState claimed(String civilizationId, LandClass landClass, @Nullable UUID ownerId) {
            return new TerritoryState(civilizationId, landClass, ownerId);
        }

        private boolean isWilderness() {
            return civilizationId == null;
        }

        private boolean sameAs(TerritoryState other) {
            if (other == null) {
                return false;
            }
            if (civilizationId == null ? other.civilizationId != null : !civilizationId.equals(other.civilizationId)) {
                return false;
            }
            if (landClass != other.landClass) {
                return false;
            }
            return ownerId == null ? other.ownerId == null : ownerId.equals(other.ownerId);
        }
    }

    private static int resolveCraftedCount(PlayerEvent.ItemCraftedEvent event, ItemStack crafted) {
        if (!crafted.isEmpty() && crafted.getCount() > 0) {
            return crafted.getCount();
        }

        if (RESULT_SLOT_REMOVE_COUNT_FIELD != null && event.getEntity().containerMenu != null) {
            if (!event.getEntity().containerMenu.slots.isEmpty()
                    && event.getEntity().containerMenu.slots.getFirst() instanceof ResultSlot resultSlot) {
                try {
                    Object value = RESULT_SLOT_REMOVE_COUNT_FIELD.get(resultSlot);
                    if (value instanceof Integer removeCount && removeCount > 0) {
                        return removeCount;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }

        return 1;
    }

    @Nullable
    private static java.lang.reflect.Field findResultSlotRemoveCountField() {
        try {
            java.lang.reflect.Field field = ResultSlot.class.getDeclaredField("removeCount");
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) {
            return null;
        }
    }

    private enum BreakProfession {
        NONE,
        MINER,
        TERRAFORMER,
        LUMBERJACK
    }
}
