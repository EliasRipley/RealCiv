package com.realciv.realciv.event;

import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.census.CensusSnapshotBuilder;
import com.realciv.realciv.command.RealCivCommands;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.*;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.logic.CarryCapService;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.CraftingLimitService;
import com.realciv.realciv.logic.LandWandService;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.ProfessionEventHook;
import com.realciv.realciv.logic.ProfessionEventHookRule;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.diplomacy.DiplomacySnapshotBuilder;
import com.realciv.realciv.hub.CommunityHubDepositContainer;
import com.realciv.realciv.hub.CommunityHubDepositMenu;
import com.realciv.realciv.hub.HubStockSnapshotBuilder;
import com.realciv.realciv.integration.RealCivFTBChunksMirror;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshotBuilder;
import com.realciv.realciv.tax.TaxSnapshotBuilder;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
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
    private static final long UPKEEP_TICK_INTERVAL = 200L;
    private static final long TERRITORY_CHECK_INTERVAL = 10L;
    private static long lastUpkeepTick = Long.MIN_VALUE;
    public static final Map<UUID, TerritoryState> LAST_TERRITORY = new HashMap<>();
    public static final List<Profession> ACTION_TRACKED_PROFESSIONS = List.of(
            Profession.FARMER,
            Profession.MINER,
            Profession.TERRAFORMER,
            Profession.LUMBERJACK,
            Profession.FISHER,
            Profession.HUNTER,
            Profession.WARRIOR,
            Profession.EXPLOSIVES_EXPERT,
            Profession.CRAFTER,
            Profession.ENCHANTER,
            Profession.BREWER,
            Profession.TRADER,
            Profession.SHEPHERD,
            Profession.EXPLORER,
            Profession.TREASURE_HUNTER,
            Profession.BREEDER,
            Profession.SMITHY,
            Profession.SMELTER);

    private RealCivEvents() {
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEventHandlers.handlePlayerLogin(event);
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEventHandlers.handlePlayerLogout(event);
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerEventHandlers.handlePlayerClone(event);
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
        List<String> resolvedLeadership = data.resolveLeadershipContestsSystem();
        if (!resolvedLeadership.isEmpty()) {
            for (String entry : resolvedLeadership) {
                String civId = entry;
                String message = entry;
                int split = entry.indexOf(':');
                if (split >= 0) {
                    civId = entry.substring(0, split).trim();
                    message = entry.substring(split + 1).trim();
                }
                Component out = Component.literal("[RealCiv] Leadership update (" + civId + "): " + message);
                for (ServerPlayer online : event.getServer().getPlayerList().getPlayers()) {
                    if (online.hasPermissions(3) || data.isCivilizationMember(civId, online.getUUID())) {
                        online.sendSystemMessage(out);
                    }
                }
            }
        }
        if (RealCivConfig.staleActionResetEnabled()) {
            long nowMillis = System.currentTimeMillis();
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
                applyStaleActionTimeoutReset(player, data, record, nowMillis);
            }
        }
    }

    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        PlayerEventHandlers.handleItemPickupPre(event);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockEventHandlers.handleRightClickBlock(event);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        BlockEventHandlers.handleRightClickItem(event);
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        BlockEventHandlers.handleLeftClickBlock(event);
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        BlockEventHandlers.handleBlockPlace(event);
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockEventHandlers.handleBlockBreak(event);
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        BlockEventHandlers.handleBlockDrops(event);
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

            boolean defendingHomeLand = isWarriorHomeDefenseExempt(attackerCiv, target, data);
            if (defendingHomeLand) {
                return;
            }
            PlayerRecord record = data.getOrCreatePlayer(attacker.getUUID());
            if (!canConsumeDailyActionBudget(attacker, record, Profession.WARRIOR, 1, "fight")) {
                event.setCanceled(true);
                return;
            }
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

        PlayerRecord record = data.getOrCreatePlayer(attacker.getUUID());
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int limit = RealCivConfig.hunterLimitForLevel(hunterLevel);
        if (isHunterMobCapReached(attacker, record, (Mob) event.getTarget(), hunterLevel)) {
            event.setCanceled(true);
            return;
        }
        if (!canConsumeDailyActionBudget(attacker, record, Profession.HUNTER, 1, "hunt")) {
            event.setCanceled(true);
            return;
        }
        if (record.hunterActions() >= limit) {
            RealCivMessages.deny(
                    attacker,
                    "You can't kill another mob until you've contributed mob loot to the Community Hub. "
                            + "Hunter limit reached (" + limit + ").");
            event.setCanceled(true);
            return;
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

            boolean defendingHomeLand = isWarriorHomeDefenseExempt(killerCiv, victim, data);
            PlayerRecord killerRecord = data.getOrCreatePlayer(killer.getUUID());
            if (!defendingHomeLand) {
                if (!canConsumeDailyActionBudget(killer, killerRecord, Profession.WARRIOR, 1, "fight")) {
                    event.setCanceled(true);
                    if (victim.getHealth() <= 0.0F) {
                        victim.setHealth(1.0F);
                    }
                    return;
                }
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
            }

            if (!defendingHomeLand) {
                killerRecord.setWarriorActions(killerRecord.warriorActions() + 1);
                killerRecord.addDailyProfessionActions(Profession.WARRIOR, 1);
            }
            if (RealCivConfig.warriorRequireHubRegistration()) {
                killerRecord.addPendingWarriorHubRegistrations(1);
                killer.sendSystemMessage(Component.literal(
                        "Warrior kill recorded. Return to your Community Hub to register this kill for Warrior XP."));
            } else {
                killerRecord.addWarriorXp(RealCivConfig.warriorXpPerPlayerKill());
                killerRecord.addGeneralXp(RealCivConfig.warriorGeneralXpPerPlayerKill());
            }
            data.recordWarCasualty(killerCiv, victimCiv);
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
        PlayerRecord record = data.getOrCreatePlayer(killer.getUUID());
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int limit = RealCivConfig.hunterLimitForLevel(hunterLevel);
        if (isHunterMobCapReached(killer, record, mob, hunterLevel)) {
            event.setCanceled(true);
            if (mob.getHealth() <= 0.0F) {
                mob.setHealth(1.0F);
            }
            return;
        }
        if (!canConsumeDailyActionBudget(killer, record, Profession.HUNTER, 1, "hunt")) {
            event.setCanceled(true);
            if (mob.getHealth() <= 0.0F) {
                mob.setHealth(1.0F);
            }
            return;
        }
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
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        record.addHunterMobActions(mobId, 1);
        record.addDailyProfessionActions(Profession.HUNTER, 1);
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

    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        BlockEventHandlers.handleBlockToolModification(event);
    }

    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        BlockEventHandlers.handleFarmlandTrample(event);
    }

    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        BlockEventHandlers.handleUseItemOnBlock(event);
    }

    public static void onBonemealUse(BonemealEvent event) {
        BlockEventHandlers.handleBonemealUse(event);
    }

    public static void onVillagerTrade(TradeWithVillagerEvent event) {
        PlayerEventHandlers.handleVillagerTrade(event);
    }

    public static void onAnvilRepair(AnvilRepairEvent event) {
        PlayerEventHandlers.handleAnvilRepair(event);
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        PlayerEventHandlers.handleItemCrafted(event);
    }

    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        PlayerEventHandlers.handleItemSmelted(event);
    }

    public static void onItemEnchanted(PlayerEnchantItemEvent event) {
        PlayerEventHandlers.handleItemEnchanted(event);
    }

    public static void onPotionBrewed(PlayerBrewedPotionEvent event) {
        PlayerEventHandlers.handlePotionBrewed(event);
    }

    public static void onItemToss(ItemTossEvent event) {
        PlayerEventHandlers.handleItemToss(event);
    }

    public static void onStatAward(StatAwardEvent event) {
        PlayerEventHandlers.handleStatAward(event);
    }

    public static void onItemFished(ItemFishedEvent event) {
        PlayerEventHandlers.handleItemFished(event);
    }

    public static void onExplosionStart(ExplosionEvent.Start event) {
        BlockEventHandlers.handleExplosionStart(event);
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        BlockEventHandlers.handleExplosionDetonate(event);
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

        DiplomacyState relation = data.diplomacyState(attackerCiv, targetCiv);
        if (relation == DiplomacyState.WAR) {
            return true;
        }
        if (relation == DiplomacyState.ALLY) {
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
                && data.diplomacyState(attackerCiv, targetCiv) == DiplomacyState.WAR;
    }

    private static boolean isWarriorHomeDefenseExempt(String defenderCivId, ServerPlayer enemy, CivSavedData data) {
        if (!RealCivConfig.warriorHomeDefenseNoActionCost()) {
            return false;
        }
        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(
                enemy.serverLevel().dimension().location().toString(),
                enemy.getBlockX() >> 4,
                enemy.getBlockZ() >> 4);
        return lookup != null && defenderCivId.equals(lookup.civilizationId());
    }

    private static void notifyWarriorLimitReached(ServerPlayer player, int limit) {
        RealCivMessages.deny(
                player,
                "Warrior limit reached (" + limit + "). "
                        + "You cannot kill another enemy player until your warrior cap resets.");
    }

    private static boolean isHunterMobCapReached(
            ServerPlayer player,
            PlayerRecord record,
            Mob mob,
            int hunterLevel) {
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        int requiredLevel = RealCivConfig.hunterRequiredLevelForMob(mobId);
        if (requiredLevel > 0 && hunterLevel < requiredLevel) {
            RealCivMessages.deny(
                    player,
                    "Hunter level " + requiredLevel + " is required to kill " + mobId
                            + " (you are level " + hunterLevel + ").");
            return true;
        }
        int mobCap = RealCivConfig.hunterMobActionCapForLevel(mobId, hunterLevel);
        if (mobCap <= 0) {
            return false;
        }
        int used = record.hunterMobActions(mobId);
        if (used < mobCap) {
            return false;
        }
        RealCivMessages.deny(
                player,
                "Hunter cap reached for " + mobId + " (" + used + "/" + mobCap + "). "
                        + "Contribute mob loot to the Community Hub to reset actions.");
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

        String civName = RealCivUtil.civilizationDisplayName(data, current.civilizationId());
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
        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup == null) {
            return TerritoryState.wilderness();
        }
        return TerritoryState.claimed(lookup.civilizationId(), lookup.plot().landClass(), lookup.plot().ownerId());
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

    @Nullable
    public static ServerPlayer resolvePlayerOwner(@Nullable Entity entity) {
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
    public static boolean tryConsumeConfiguredHookActions(
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

    public static boolean tryConsumeConfiguredHookActions(
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

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
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
            int dailyCap = RealCivConfig.dailyActionCapForLevel(profession, level);
            if (dailyCap > 0) {
                int dailyUsed = record.dailyProfessionActionsUsed(profession);
                if (dailyUsed + pending.actionCost() > dailyCap) {
                    String message = formatHookDenyMessage(
                            pending.sourceRule(),
                            hook,
                            dailyUsed,
                            dailyCap,
                            pending.actionCost(),
                            0,
                            0,
                            0L,
                            0,
                            0,
                            0,
                            defaultDenyMessage,
                            "Daily " + readableProfessionName(profession).toLowerCase(Locale.ROOT)
                                    + " cap reached (" + dailyUsed + "/" + dailyCap + ").");
                    RealCivMessages.deny(player, message);
                    return false;
                }
            }
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
            int actionCost = entry.getValue().actionCost();
            record.setActionsForProfession(profession, current + actionCost);
            record.addDailyProfessionActions(profession, actionCost);
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

    public static int safeMultiply(int left, int right) {
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

    public static boolean isShearTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.canPerformAction(ItemAbilities.SHEARS_HARVEST)
                || stack.canPerformAction(ItemAbilities.SHEARS_TRIM)
                || stack.canPerformAction(ItemAbilities.SHEARS_CARVE)
                || stack.canPerformAction(ItemAbilities.SHEARS_REMOVE_ARMOR)
                || stack.canPerformAction(ItemAbilities.SHEARS_DISARM);
    }

    public static String readableProfessionName(Profession profession) {
        String raw = profession.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        if (raw.isEmpty()) {
            return "Profession";
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    public static boolean canProgressProfession(
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
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
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
        if (!canConsumeDailyActionBudget(player, record, Profession.EXPLOSIVES_EXPERT, 1, "use explosives", sendMessage)) {
            return false;
        }
        return true;
    }

    public static boolean canConsumeDailyActionBudget(
            ServerPlayer player,
            PlayerRecord record,
            Profession profession,
            int actionCost,
            String activityVerb) {
        return canConsumeDailyActionBudget(player, record, profession, actionCost, activityVerb, true);
    }

    public static boolean canConsumeDailyActionBudget(
            ServerPlayer player,
            PlayerRecord record,
            Profession profession,
            int actionCost,
            String activityVerb,
            boolean sendMessage) {
        if (profession == null || profession == Profession.NONE) {
            return true;
        }
        int safeCost = Math.max(0, actionCost);
        if (safeCost <= 0) {
            return true;
        }
        int level = record.levelFor(profession);
        int dailyCap = RealCivConfig.dailyActionCapForLevel(profession, level);
        if (dailyCap <= 0) {
            return true;
        }
        int used = record.dailyProfessionActionsUsed(profession);
        if (used + safeCost <= dailyCap) {
            return true;
        }
        if (sendMessage) {
            RealCivMessages.deny(
                    player,
                    "You can't " + activityVerb + " more today. "
                            + readableProfessionName(profession) + " daily cap reached (" + used + "/" + dailyCap + ").");
        }
        return false;
    }

    public static long currentGameTime(ServerPlayer player) {
        if (player.getServer() == null || player.getServer().overworld() == null) {
            return 0L;
        }
        return player.getServer().overworld().getGameTime();
    }

    public static void applyStaleActionTimeoutReset(
            ServerPlayer player,
            CivSavedData data,
            PlayerRecord record,
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

    public static int staleResetValue(int current, long lastUpdatedMillis, long nowMillis, long staleMillis) {
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
        @Nullable ServerPlayer fromSource = resolvePlayerOwner(source.getEntity());
        if (fromSource != null) {
            return fromSource;
        }

        return resolvePlayerOwner(source.getDirectEntity());
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

}
