package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CarryCapService {
    private CarryCapService() {
    }

    public static boolean canAcquireForPickup(ServerPlayer player, CivSavedData data, ItemStack incoming) {
        return canAcquire(player, data, incoming, incoming.getCount(), AcquireMode.PICKUP);
    }

    public static boolean canAcquireForCraft(ServerPlayer player, CivSavedData data, ItemStack incoming, int amount) {
        return canAcquire(player, data, incoming, amount, AcquireMode.CRAFT);
    }

    public static boolean canAcquire(
            ServerPlayer player,
            CivSavedData data,
            ItemStack incoming,
            int incomingCount,
            AcquireMode mode) {
        if (incoming.isEmpty() || incomingCount <= 0 || RealCivUtil.isBypass(player)) {
            return true;
        }
        if (mode == AcquireMode.PICKUP && !RealCivConfig.carryCapPickupEnabled()) {
            return true;
        }
        if (mode == AcquireMode.CRAFT && !RealCivConfig.carryCapCraftEnabled()) {
            return true;
        }

        Map<ResourceLocation, RewardRule> exactRules = RealCivConfig.rewardRules();
        List<TagRewardRule> tagRules = RealCivConfig.tagRewardRules();
        Map<ResourceLocation, Profession> professionCache = new HashMap<>();
        Map<Profession, Double> multipliers = RealCivConfig.carryCapProfessionMultipliers();
        Map<ResourceLocation, Integer> itemOverrides = RealCivConfig.carryCapItemMaxOverrides();

        CapCheck capCheck = evaluateCap(player, data, incoming, incomingCount, exactRules, tagRules, professionCache, multipliers, itemOverrides);
        if (capCheck == null || capCheck.allowed()) {
            return true;
        }

        String action = mode == AcquireMode.PICKUP ? "pick up" : "take crafted output";
        String scope = capCheck.itemScoped()
                ? "item cap for " + capCheck.itemId()
                : capCheck.profession().name().toLowerCase(java.util.Locale.ROOT) + " carry cap";
        RealCivMessages.deny(
                player,
                "You can't " + action + " more right now. " + scope + " reached ("
                        + capCheck.currentCount() + "/" + capCheck.maxAllowed()
                        + "). Contribute to the Community Hub to reset progression limits.");
        return false;
    }

    @Nullable
    private static CapCheck evaluateCap(
            ServerPlayer player,
            CivSavedData data,
            ItemStack incoming,
            int incomingCount,
            Map<ResourceLocation, RewardRule> exactRules,
            List<TagRewardRule> tagRules,
            Map<ResourceLocation, Profession> professionCache,
            Map<Profession, Double> multipliers,
            Map<ResourceLocation, Integer> itemOverrides) {
        @Nullable Profession profession = resolveProfession(incoming, exactRules, tagRules, professionCache);
        if (profession == null || profession == Profession.NONE) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(incoming.getItem());
        @Nullable Integer itemCapOverride = itemOverrides.get(itemId);
        if (itemCapOverride != null) {
            int maxAllowed = Math.max(0, itemCapOverride);
            int current = countItemInInventory(player.getInventory(), itemId);
            int projected = current + Math.max(0, incomingCount);
            return new CapCheck(projected <= maxAllowed, true, profession, itemId, current, projected, maxAllowed);
        }

        int baseLimit = baseLimitForProfession(data.getOrCreatePlayer(player.getUUID()), profession);
        double multiplier = Math.max(0.0D, multipliers.getOrDefault(profession, 1.0D));
        int maxAllowed = (int) Math.floor(baseLimit * multiplier);
        int current = countProfessionInInventory(player.getInventory(), profession, exactRules, tagRules, professionCache);
        int projected = current + Math.max(0, incomingCount);
        return new CapCheck(projected <= maxAllowed, false, profession, itemId, current, projected, maxAllowed);
    }

    private static int baseLimitForProfession(PlayerRecord record, Profession profession) {
        return switch (profession) {
            case FARMER -> RealCivConfig.farmerLimitForLevel(record.levelFor(Profession.FARMER));
            case MINER -> RealCivConfig.minerLimitForLevel(record.levelFor(Profession.MINER));
            case TERRAFORMER -> RealCivConfig.terraformerLimitForLevel(record.levelFor(Profession.TERRAFORMER));
            case LUMBERJACK -> RealCivConfig.lumberjackLimitForLevel(record.levelFor(Profession.LUMBERJACK));
            case FISHER -> RealCivConfig.fisherLimitForLevel(record.levelFor(Profession.FISHER));
            case HUNTER -> RealCivConfig.hunterLimitForLevel(record.levelFor(Profession.HUNTER));
            case WARRIOR -> RealCivConfig.warriorLimitForLevel(record.levelFor(Profession.WARRIOR));
            case EXPLOSIVES_EXPERT -> RealCivConfig.explosivesExpertLimitForLevel(record.levelFor(Profession.EXPLOSIVES_EXPERT));
            case CRAFTER -> RealCivConfig.crafterLimitForLevel(record.levelFor(Profession.CRAFTER));
            case ENCHANTER -> RealCivConfig.enchanterLimitForLevel(record.levelFor(Profession.ENCHANTER));
            case BREWER -> RealCivConfig.brewerLimitForLevel(record.levelFor(Profession.BREWER));
            case TRADER -> RealCivConfig.traderLimitForLevel(record.levelFor(Profession.TRADER));
            case SHEPHERD -> RealCivConfig.shepherdLimitForLevel(record.levelFor(Profession.SHEPHERD));
            case EXPLORER -> RealCivConfig.explorerLimitForLevel(record.levelFor(Profession.EXPLORER));
            case TREASURE_HUNTER -> RealCivConfig.treasureHunterLimitForLevel(record.levelFor(Profession.TREASURE_HUNTER));
            case BREEDER -> RealCivConfig.breederLimitForLevel(record.levelFor(Profession.BREEDER));
            case SMITHY -> RealCivConfig.smithyLimitForLevel(record.levelFor(Profession.SMITHY));
            case SMELTER -> RealCivConfig.smelterLimitForLevel(record.levelFor(Profession.SMELTER));
            case NONE -> 0;
        };
    }

    private static int countItemInInventory(Inventory inventory, ResourceLocation itemId) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (stackId.equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int countProfessionInInventory(
            Inventory inventory,
            Profession targetProfession,
            Map<ResourceLocation, RewardRule> exactRules,
            List<TagRewardRule> tagRules,
            Map<ResourceLocation, Profession> professionCache) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            @Nullable Profession profession = resolveProfession(stack, exactRules, tagRules, professionCache);
            if (profession == targetProfession) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Nullable
    private static Profession resolveProfession(
            ItemStack stack,
            Map<ResourceLocation, RewardRule> exactRules,
            List<TagRewardRule> tagRules,
            Map<ResourceLocation, Profession> cache) {
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Profession cached = cache.get(itemId);
        if (cached != null) {
            return cached == Profession.NONE ? null : cached;
        }

        @Nullable RewardRule direct = exactRules.get(itemId);
        if (direct != null) {
            Profession profession = direct.profession();
            cache.put(itemId, profession == Profession.NONE ? Profession.NONE : profession);
            return profession == Profession.NONE ? null : profession;
        }

        for (TagRewardRule tagRule : tagRules) {
            if (HubRewardResolver.matchesTagRule(stack, tagRule)) {
                Profession profession = tagRule.profession();
                cache.put(itemId, profession == Profession.NONE ? Profession.NONE : profession);
                return profession == Profession.NONE ? null : profession;
            }
        }

        cache.put(itemId, Profession.NONE);
        return null;
    }

    public enum AcquireMode {
        PICKUP,
        CRAFT
    }

    private record CapCheck(
            boolean allowed,
            boolean itemScoped,
            Profession profession,
            ResourceLocation itemId,
            int currentCount,
            int projectedCount,
            int maxAllowed) {
    }
}
