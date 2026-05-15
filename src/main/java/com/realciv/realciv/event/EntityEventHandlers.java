package com.realciv.realciv.event;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.*;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.ProfessionEventHook;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public final class EntityEventHandlers {

    private EntityEventHandlers() {}

    public static void handleAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker) || attacker.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(attacker)) {
            return;
        }

        CivSavedData data = CivSavedData.get(attacker.getServer());
        if (event.getTarget() instanceof EndCrystal && !RealCivEvents.canUseExplosivesExpertAction(attacker, data, true)) {
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
            if (!RealCivEvents.canProgressProfession(attacker, data, Profession.WARRIOR, true)) {
                event.setCanceled(true);
                return;
            }

            boolean defendingHomeLand = isWarriorHomeDefenseExempt(attackerCiv, target, data);
            if (defendingHomeLand) {
                return;
            }
            PlayerRecord record = data.getOrCreatePlayer(attacker.getUUID());
            if (!RealCivEvents.canConsumeDailyActionBudget(attacker, record, Profession.WARRIOR, 1, "fight")) {
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
        if (!RealCivEvents.canProgressProfession(attacker, data, Profession.HUNTER, true)) {
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
        if (!RealCivEvents.canConsumeDailyActionBudget(attacker, record, Profession.HUNTER, 1, "hunt")) {
            event.setCanceled(true);
            return;
        }
        if (record.hunterActions() >= limit) {
            RealCivMessages.deny(
                    attacker,
                    "You can't kill another mob until you've contributed mob loot to the Community Hub. "
                            + "Hunter limit reached (" + limit + ").");
            event.setCanceled(true);
        }
    }

    public static void handleLivingDeath(LivingDeathEvent event) {
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
            if (!RealCivEvents.canProgressProfession(killer, data, Profession.WARRIOR, true)) {
                event.setCanceled(true);
                if (victim.getHealth() <= 0.0F) {
                    victim.setHealth(1.0F);
                }
                return;
            }

            boolean defendingHomeLand = isWarriorHomeDefenseExempt(killerCiv, victim, data);
            PlayerRecord killerRecord = data.getOrCreatePlayer(killer.getUUID());
            if (!defendingHomeLand) {
                if (!RealCivEvents.canConsumeDailyActionBudget(killer, killerRecord, Profession.WARRIOR, 1, "fight")) {
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
                killer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
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
        if (!RealCivEvents.canProgressProfession(killer, data, Profession.HUNTER, true)) {
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
        if (!RealCivEvents.canConsumeDailyActionBudget(killer, record, Profession.HUNTER, 1, "hunt")) {
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

    public static void handleAnimalBreed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ANIMAL_BREED,
                1,
                "You can't breed more animals until you've contributed farm resources to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    public static void handleAnimalTame(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ANIMAL_TAME,
                1,
                "You can't tame more animals until you've contributed related resources to the Community Hub.")) {
            event.setCanceled(true);
        }
    }

    public static void handleEntityInteract(PlayerInteractEvent.EntityInteract event) {
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
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.VILLAGER_INTERACT,
                1,
                "You can't interact with villagers right now until you've contributed to the Community Hub.")) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static void handleEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        if (!RealCivEvents.isShearTool(event.getItemStack())) {
            return;
        }
        if (!(event.getTarget() instanceof IShearable shearable)) {
            return;
        }
        if (!shearable.isShearable(player, event.getItemStack(), player.serverLevel(), event.getTarget().blockPosition())) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.SHEAR_ENTITY,
                1,
                "You can't shear more entities until you've contributed the related materials to the Community Hub.")) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
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

    @Nullable
    private static ServerPlayer getResponsiblePlayer(DamageSource source) {
        @Nullable ServerPlayer fromSource = RealCivEvents.resolvePlayerOwner(source.getEntity());
        if (fromSource != null) {
            return fromSource;
        }

        return RealCivEvents.resolvePlayerOwner(source.getDirectEntity());
    }
}
