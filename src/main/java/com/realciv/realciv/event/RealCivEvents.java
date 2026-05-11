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
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.hub.CommunityHubDepositContainer;
import com.realciv.realciv.hub.CommunityHubStockMenu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.util.TriState;
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
                        + " | Hunter: " + record.levelFor(Profession.HUNTER)
                        + " | Crafter: " + record.levelFor(Profession.CRAFTER)));
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
        int farmerRefund = refundActions(record.farmerActions(), ratio);
        int minerRefund = refundActions(record.minerActions(), ratio);
        int terraformerRefund = refundActions(record.terraformerActions(), ratio);
        int lumberjackRefund = refundActions(record.lumberjackActions(), ratio);
        int hunterRefund = refundActions(record.hunterActions(), ratio);
        int crafterRefund = refundActions(record.crafterActions(), ratio);

        if (farmerRefund <= 0 && minerRefund <= 0 && terraformerRefund <= 0
                && lumberjackRefund <= 0 && hunterRefund <= 0 && crafterRefund <= 0) {
            return;
        }

        record.setFarmerActions(record.farmerActions() - farmerRefund);
        record.setMinerActions(record.minerActions() - minerRefund);
        record.setTerraformerActions(record.terraformerActions() - terraformerRefund);
        record.setLumberjackActions(record.lumberjackActions() - lumberjackRefund);
        record.setHunterActions(record.hunterActions() - hunterRefund);
        record.setCrafterActions(record.crafterActions() - crafterRefund);
        data.setDirty();

        int totalRefund = farmerRefund + minerRefund + terraformerRefund + lumberjackRefund + hunterRefund + crafterRefund;
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

            if (data.countPlotsByClass(civId, LandClass.CIVIC) <= 0) {
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
                        "You can't place blocks here. Public/wilderness chunks are break-only. "
                                + "Build in CIVIC (with permission) or PRIVATE land.");
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
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        if (!(event.getTarget() instanceof Mob) || RealCivUtil.isBypass(player)) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int limit = RealCivConfig.hunterLimitForLevel(hunterLevel);
        if (record.hunterActions() >= limit) {
            RealCivMessages.deny(
                    player,
                    "You can't kill another mob until you've contributed mob loot to the Community Hub. "
                            + "Hunter limit reached (" + limit + ").");
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }

        ServerPlayer killer = getResponsiblePlayer(event.getSource());
        if (killer == null || killer.getServer() == null || RealCivUtil.isBypass(killer)) {
            return;
        }

        CivSavedData data = CivSavedData.get(killer.getServer());
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
                        + " | Hunter " + record.hunterActions() + "/" + RealCivConfig.hunterLimitForLevel(record.levelFor(Profession.HUNTER))
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
                    "Insufficient social credit for upkeep prepayment. Need "
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
                "Civ treasury (" + civId + "): " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))));
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
            player.sendSystemMessage(Component.literal("You've entered public wilderness."));
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
                "You've now entered public land of " + civName + "."));
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

        int farmerReset = staleResetValue(record.farmerActions(), record.farmerActionsUpdatedAtMillis(), nowMillis, staleMillis);
        int minerReset = staleResetValue(record.minerActions(), record.minerActionsUpdatedAtMillis(), nowMillis, staleMillis);
        int terraformerReset = staleResetValue(record.terraformerActions(), record.terraformerActionsUpdatedAtMillis(), nowMillis, staleMillis);
        int lumberjackReset = staleResetValue(record.lumberjackActions(), record.lumberjackActionsUpdatedAtMillis(), nowMillis, staleMillis);
        int hunterReset = staleResetValue(record.hunterActions(), record.hunterActionsUpdatedAtMillis(), nowMillis, staleMillis);
        int crafterReset = staleResetValue(record.crafterActions(), record.crafterActionsUpdatedAtMillis(), nowMillis, staleMillis);

        if (farmerReset <= 0 && minerReset <= 0 && terraformerReset <= 0
                && lumberjackReset <= 0 && hunterReset <= 0 && crafterReset <= 0) {
            return;
        }

        if (farmerReset > 0) {
            record.setFarmerActions(0);
        }
        if (minerReset > 0) {
            record.setMinerActions(0);
        }
        if (terraformerReset > 0) {
            record.setTerraformerActions(0);
        }
        if (lumberjackReset > 0) {
            record.setLumberjackActions(0);
        }
        if (hunterReset > 0) {
            record.setHunterActions(0);
        }
        if (crafterReset > 0) {
            record.setCrafterActions(0);
        }
        data.setDirty();

        int totalReset = farmerReset + minerReset + terraformerReset + lumberjackReset + hunterReset + crafterReset;
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
