package com.realciv.realciv.event;

import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CraftingLimitService;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.hub.CommunityHubDepositContainer;
import com.realciv.realciv.hub.CommunityHubStockMenu;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        BlockState clickedState = event.getLevel().getBlockState(event.getPos());

        if (clickedState.is(ModBlocks.COMMUNITY_HUB.get())) {
            if (player.isShiftKeyDown()) {
                openHubStockMenu(event, player, data);
            } else {
                openHubDepositMenu(event, player, data);
            }
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

        if (placedBlock.is(ModBlocks.COMMUNITY_HUB.get()) && !RealCivUtil.isBypass(player)) {
            RealCivMessages.deny(player, "Only admins can place Community Hubs.");
            event.setCanceled(true);
            return;
        }

        if (!canBuildInChunk(player, level, event.getPos(), data, now)) {
            CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(
                    level.dimension().location().toString(),
                    event.getPos().getX() >> 4,
                    event.getPos().getZ() >> 4);
            if (lookup == null) {
                RealCivMessages.deny(player, "You can't place blocks here. This chunk is not legally zoned.");
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

        if (state.is(ModBlocks.COMMUNITY_HUB.get()) && !RealCivUtil.isBypass(player)) {
            RealCivMessages.deny(player, "Community Hub blocks are protected.");
            event.setCanceled(true);
            return;
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
            CraftingLimitService.notifyCraftDenied(player);
            return;
        }

        int allowed = Math.max(0, limit - current);
        int applied = Math.min(allowed, craftedCount);
        record.setCrafterActions(current + applied);
        data.setDirty();

        if (craftedCount > applied) {
            CraftingLimitService.notifyCraftDenied(player);
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
        boolean privileged = RealCivUtil.isBypass(player) || data.isMayor(civId, player.getUUID());

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

    private static boolean canBuildInChunk(ServerPlayer player, Level level, BlockPos pos, CivSavedData data, long gameTime) {
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
        if (state.isAir() || state.is(BlockTags.CROPS) || state.is(ModBlocks.COMMUNITY_HUB.get())) {
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
