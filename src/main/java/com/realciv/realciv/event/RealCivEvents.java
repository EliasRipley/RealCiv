package com.realciv.realciv.event;

import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CarryCapService;
import com.realciv.realciv.logic.CraftingLimitService;
import com.realciv.realciv.logic.LandWandService;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.hub.CommunityHubDepositContainer;
import com.realciv.realciv.hub.CommunityHubStockMenu;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
    private static final TagKey<Block> BAMBOO_BLOCKS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:bamboo_blocks"));
    private static final long UPKEEP_TICK_INTERVAL = 200L;
    private static long lastUpkeepTick = Long.MIN_VALUE;

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

        player.sendSystemMessage(Component.literal("RealCiv profile loaded."));
        player.sendSystemMessage(Component.literal(
                "Civilization: " + civId
                        + " | Credits: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + " | General Level: " + record.generalLevel()
                        + " | Farmer: " + record.levelFor(Profession.FARMER)
                        + " | Miner: " + record.levelFor(Profession.MINER)
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
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().overworld() == null) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        if (lastUpkeepTick != Long.MIN_VALUE && now - lastUpkeepTick < UPKEEP_TICK_INTERVAL) {
            return;
        }
        lastUpkeepTick = now;
        CivSavedData.get(event.getServer()).processUpkeep(now);
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
                                + " nearby boundary edge(s) within " + radius + " chunks."
                                + (selectedEdges > 0 ? " Selection edges: " + selectedEdges + "." : "")));
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
            int radius = RealCivConfig.landWandVisualizeRadiusChunks();
            int edges = LandWandService.visualizeNearbyPlots(player, data, radius);
            int selectedEdges = LandWandService.visualizeSelection(player);
            player.sendSystemMessage(Component.literal(
                    "[RealCiv] Land Wand visualized " + edges
                            + " nearby boundary edge(s) within " + radius + " chunks."
                            + (selectedEdges > 0 ? " Selection edges: " + selectedEdges + "." : "")));
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

        if ((placedBlock.is(ModBlocks.COMMUNITY_HUB.get())
                || placedBlock.is(ModBlocks.CENSUS_BLOCK.get())
                || placedBlock.is(ModBlocks.TAX_BLOCK.get()))
                && !player.hasPermissions(3)) {
            String civId = data.getOrAssignCivilization(player.getUUID());
            if (!data.isMayor(civId, player.getUUID())) {
                RealCivMessages.deny(
                        player,
                        "Only your civilization mayor (or admins) can place civic control blocks (Hub/Census/Tax).");
                event.setCanceled(true);
                return;
            }
        }

        if (!canBuildInChunk(player, level, event.getPos(), data, now)) {
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

        if ((state.is(ModBlocks.COMMUNITY_HUB.get())
                || state.is(ModBlocks.CENSUS_BLOCK.get())
                || state.is(ModBlocks.TAX_BLOCK.get()))
                && !player.hasPermissions(3)) {
            String civId = data.getOrAssignCivilization(player.getUUID());
            if (!data.isMayor(civId, player.getUUID())) {
                RealCivMessages.deny(player, "Civic control blocks (Hub/Census/Tax) are protected.");
                event.setCanceled(true);
                return;
            }
        }

        if (!canBreakInChunk(player, level, pos, data, now)) {
            CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(
                    level.dimension().location().toString(),
                    pos.getX() >> 4,
                    pos.getZ() >> 4);
            if (lookup == null) {
                RealCivMessages.deny(player, "You can't break blocks here. This chunk is not legally zoned.");
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

        if (isToolLocked(player, player.getMainHandItem(), data)) {
            event.setCanceled(true);
            return;
        }

        if (!player.isCreative()) {
            BreakProfession breakProfession = breakProfessionFor(state);
            if (breakProfession == BreakProfession.NONE) {
                return;
            }

            CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
            if (breakProfession == BreakProfession.LUMBERJACK) {
                int levelValue = record.levelFor(Profession.LUMBERJACK);
                int limit = RealCivConfig.lumberjackLimitForLevel(levelValue);
                if (record.lumberjackActions() >= limit) {
                    RealCivMessages.deny(
                            player,
                            "You can't chop more wood until you've contributed wood to the Community Hub. "
                                    + "Lumberjack limit reached (" + limit + ").");
                    event.setCanceled(true);
                }
            } else if (breakProfession == BreakProfession.MINER) {
                int levelValue = record.levelFor(Profession.MINER);
                int limit = RealCivConfig.minerLimitForLevel(levelValue);
                if (record.minerActions() >= limit) {
                    RealCivMessages.deny(
                            player,
                            "You can't mine more until you've returned your blocks to the Community Hub. "
                                    + "Miner limit reached (" + limit + ").");
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

        CivSavedData data = CivSavedData.get(player.getServer());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (breakProfession == BreakProfession.LUMBERJACK) {
            record.setLumberjackActions(record.lumberjackActions() + 1);
        } else if (breakProfession == BreakProfession.MINER) {
            record.setMinerActions(record.minerActions() + 1);
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
                        + " | Lumberjack " + record.lumberjackActions() + "/" + RealCivConfig.lumberjackLimitForLevel(record.levelFor(Profession.LUMBERJACK))
                        + " | Hunter " + record.hunterActions() + "/" + RealCivConfig.hunterLimitForLevel(record.levelFor(Profession.HUNTER))
                        + " | Crafter " + record.crafterActions() + "/" + RealCivConfig.crafterLimitForLevel(record.levelFor(Profession.CRAFTER))));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void openHubStockMenu(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean privileged = player.hasPermissions(3) || RealCivUtil.isBypass(player) || data.isMayor(civId, player.getUUID());

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
        List<UUID> members = data.civilizationMembersSorted(civId);
        int managers = 0;
        for (UUID member : members) {
            if (data.isCivicManager(civId, member)) {
                managers++;
            }
        }
        boolean mayorOrAdmin = player.hasPermissions(3) || RealCivUtil.isBypass(player) || data.isMayor(civId, player.getUUID());

        player.sendSystemMessage(Component.literal(
                "Census: civ='" + civId + "' | members=" + members.size() + " | managers=" + managers));
        if (mayorOrAdmin) {
            player.sendSystemMessage(Component.literal(
                    "Mayor controls: /realciv census members, /realciv census manager add <player>, "
                            + "/realciv census manager remove <player>, /realciv census mayor <player>"));
        } else {
            player.sendSystemMessage(Component.literal(
                    "Citizen view: /realciv census members"));
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
            return true;
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
        return BreakProfession.NONE;
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
        LUMBERJACK
    }
}
