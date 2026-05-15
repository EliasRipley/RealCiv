package com.realciv.realciv.event;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.integration.RealCivFTBChunksMirror;
import com.realciv.realciv.logic.CarryCapService;
import com.realciv.realciv.logic.CraftingLimitService;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.ProfessionEventHook;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.StatAwardEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import org.jetbrains.annotations.Nullable;

public final class PlayerEventHandlers {
    private static final java.lang.reflect.Field RESULT_SLOT_REMOVE_COUNT_FIELD = findResultSlotRemoveCountField();

    private PlayerEventHandlers() {
    }

    public static void handlePlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        long now = RealCivEvents.currentGameTime(player);
        data.processUpkeep(now);

        String civId = data.getOrAssignCivilization(player.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (record.firstSeenAtMillis() <= 0L) {
            record.ensureFirstSeenAtMillis(System.currentTimeMillis());
            data.setDirty();
        }
        RealCivEvents.applyStaleActionTimeoutReset(player, data, record, System.currentTimeMillis());

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
                        + " | Crafter: " + record.levelFor(Profession.CRAFTER)
                        + " | Enchanter: " + record.levelFor(Profession.ENCHANTER)
                        + " | Brewer: " + record.levelFor(Profession.BREWER)
                        + " | Trader: " + record.levelFor(Profession.TRADER)
                        + " | Shepherd: " + record.levelFor(Profession.SHEPHERD)
                        + " | Breeder: " + record.levelFor(Profession.BREEDER)
                        + " | Smithy: " + record.levelFor(Profession.SMITHY)
                        + " | Smelter: " + record.levelFor(Profession.SMELTER)));
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
        if (RealCivConfig.warriorRequireHubRegistration() && record.pendingWarriorHubRegistrations() > 0) {
            player.sendSystemMessage(Component.literal(
                    "You have " + record.pendingWarriorHubRegistrations()
                            + " unregistered warrior kill(s). Visit your Community Hub to claim kill XP."));
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
        RealCivEvents.LAST_TERRITORY.remove(player.getUUID());
    }

    public static void handlePlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RealCivEvents.LAST_TERRITORY.remove(player.getUUID());
    }

    public static void handlePlayerClone(PlayerEvent.Clone event) {
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
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int totalRefund = 0;
        for (Profession profession : RealCivEvents.ACTION_TRACKED_PROFESSIONS) {
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

    public static void handleItemPickupPre(ItemEntityPickupEvent.Pre event) {
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

    public static void handleItemCrafted(PlayerEvent.ItemCraftedEvent event) {
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
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int craftedCount = resolveCraftedCount(event, crafted);
        int current = record.crafterActions();

        int limit = RealCivConfig.crafterLimitForLevel(crafterLevel);
        if (current >= limit) {
            CraftingLimitService.notifyCraftDenied(player, crafted);
            return;
        }
        int applied = Math.min(Math.max(0, limit - current), craftedCount);

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(crafted.getItem());
        int itemCap = RealCivConfig.crafterItemActionCapForLevel(itemId, crafterLevel);
        if (itemCap > 0) {
            int used = record.crafterItemActions(itemId);
            if (used >= itemCap) {
                CraftingLimitService.notifyCraftDenied(player, crafted);
                return;
            }
            applied = Math.min(applied, Math.max(0, itemCap - used));
        }

        int dailyItemCap = RealCivConfig.crafterDailyItemActionCapForLevel(itemId, crafterLevel);
        if (dailyItemCap > 0) {
            int used = record.crafterDailyItemActions(itemId);
            if (used >= dailyItemCap) {
                CraftingLimitService.notifyCraftDenied(player, crafted);
                return;
            }
            applied = Math.min(applied, Math.max(0, dailyItemCap - used));
        }

        int dailyCap = RealCivConfig.dailyActionCapForLevel(Profession.CRAFTER, crafterLevel);
        if (dailyCap > 0) {
            int dailyUsed = record.dailyProfessionActionsUsed(Profession.CRAFTER);
            int dailyRemaining = Math.max(0, dailyCap - dailyUsed);
            applied = Math.min(applied, dailyRemaining);
        }

        if (applied > 0) {
            record.setCrafterActions(current + applied);
            record.addCrafterItemActions(itemId, applied);
            record.addCrafterDailyItemActions(itemId, applied);
            record.addDailyProfessionActions(Profession.CRAFTER, applied);
        }
        data.setDirty();

        if (craftedCount > applied) {
            CraftingLimitService.notifyCraftDenied(player, crafted);
        }
    }

    public static void handleItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        ItemStack smelted = event.getSmelting();
        int triggerCount = smelted.isEmpty() ? 1 : Math.max(1, smelted.getCount());
        CivSavedData data = CivSavedData.get(player.getServer());
        RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ITEM_SMELT,
                triggerCount,
                "You can't smelt additional items right now until your configured progression gate is met.");
    }

    public static void handleItemEnchanted(PlayerEnchantItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        int triggerCount = Math.max(1, event.getEnchantments().size());
        CivSavedData data = CivSavedData.get(player.getServer());
        RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ITEM_ENCHANT,
                triggerCount,
                "You can't apply additional enchantments right now until your configured progression gate is met.");
    }

    public static void handlePotionBrewed(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        ItemStack brewed = event.getStack();
        int triggerCount = brewed.isEmpty() ? 1 : Math.max(1, brewed.getCount());
        CivSavedData data = CivSavedData.get(player.getServer());
        RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.POTION_BREW,
                triggerCount,
                "You can't brew additional potions right now until your configured progression gate is met.");
    }

    public static void handleItemToss(ItemTossEvent event) {
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
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.ITEM_TOSS,
                triggerCount,
                "You can't toss additional items right now until your configured progression gate is met.")) {
            event.setCanceled(true);
        }
    }

    public static void handleStatAward(StatAwardEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        int triggerCount = Math.max(1, event.getValue());
        String statId = event.getStat().toString();
        CivSavedData data = CivSavedData.get(player.getServer());
        if (!RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.STAT_AWARD,
                triggerCount,
                "You can't progress additional tracked stats right now until your configured progression gate is met.",
                statId)) {
            event.setCanceled(true);
        }
    }

    public static void handleItemFished(ItemFishedEvent event) {
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
        if (!RealCivEvents.canProgressProfession(player, data, Profession.FISHER, true)) {
            event.setCanceled(true);
            return;
        }
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int fisherLevel = record.levelFor(Profession.FISHER);
        int limit = RealCivConfig.fisherLimitForLevel(fisherLevel);
        if (!RealCivEvents.canConsumeDailyActionBudget(player, record, Profession.FISHER, fishCaught, "fish")) {
            event.setCanceled(true);
            return;
        }
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
        record.addDailyProfessionActions(Profession.FISHER, fishCaught);
        data.setDirty();
    }

    public static void handleVillagerTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        CivSavedData data = CivSavedData.get(player.getServer());
        RealCivEvents.tryConsumeConfiguredHookActions(
                player,
                data,
                ProfessionEventHook.VILLAGER_TRADE,
                1,
                "You can't complete additional villager trades right now until your configured progression gate is met.");
    }

    public static void handleAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null || player.serverLevel().isClientSide()) {
            return;
        }
        if (RealCivUtil.isBypass(player)) {
            return;
        }
        CivSavedData data = CivSavedData.get(player.getServer());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        ItemStack right = event.getRight();
        ItemStack left = event.getLeft();
        ItemStack output = event.getOutput();

        if (right.isEmpty()) {
            RealCivEvents.tryConsumeConfiguredHookActions(
                    player, data, ProfessionEventHook.ANVIL_RENAME, 1,
                    "You can't rename items until you progress your Smithy level.");
            return;
        }

        if (right.is(Items.ENCHANTED_BOOK)
                || (!left.isEmpty() && left.isEnchanted() && right.isEnchanted())) {
            RealCivEvents.tryConsumeConfiguredHookActions(
                    player, data, ProfessionEventHook.ANVIL_COMBINE_ENCHANT, 1,
                    "You can't combine enchantments until you progress your Smithy level.");
            return;
        }

        if (!output.isEmpty()) {
            String tierKey = resolveRepairTierKey(output);
            if (!tierKey.isEmpty()) {
                int requiredLevel = RealCivConfig.smithyRepairTierRequirement(tierKey);
                int smithyLevel = record.levelFor(Profession.SMITHY);
                if (smithyLevel < requiredLevel) {
                    RealCivMessages.deny(player,
                            "You need Smithy level " + requiredLevel + " to repair "
                                    + tierKey.toLowerCase(Locale.ROOT)
                                    + " items (you are level " + smithyLevel + ").");
                    return;
                }
            }
        }

        RealCivEvents.tryConsumeConfiguredHookActions(
                player, data, ProfessionEventHook.ANVIL_REPAIR_TOOL, 1,
                "You can't repair that item yet. Progress your Smithy level.");
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

    private static String resolveRepairTierKey(ItemStack stack) {
        if (stack.isEmpty()) return "";
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof TieredItem tiered) {
            net.minecraft.world.item.Tier tier = tiered.getTier();
            if (tier instanceof net.minecraft.world.item.Tiers knownTier) {
                return knownTier.name();
            }
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        if (path.startsWith("leather_")) return "LEATHER";
        if (path.startsWith("chainmail_")) return "CHAINMAIL";
        if (path.startsWith("turtle_")) return "TURTLE";
        if (path.startsWith("iron_")) return "IRON";
        if (path.startsWith("golden_") || path.startsWith("gold_")) return "GOLD";
        if (path.startsWith("diamond_")) return "DIAMOND";
        if (path.startsWith("netherite_")) return "NETHERITE";
        if (path.equals("shears") || path.equals("shield") || path.equals("elytra")
                || path.equals("fishing_rod") || path.equals("flint_and_steel")
                || path.contains("horse_armor") || path.contains("wolf_armor")) {
            return "DEFAULT";
        }
        return "";
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
}
