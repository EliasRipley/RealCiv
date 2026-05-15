package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CraftingLimitService {
    private CraftingLimitService() {
    }

    public static boolean canTakeCraftingResult(ServerPlayer player, ItemStack resultStack) {
        if (resultStack.isEmpty() || RealCivUtil.isBypass(player) || player.getServer() == null) {
            return true;
        }

        CivSavedData data = CivSavedData.get(player.getServer());
        if (!CarryCapService.canAcquireForCraft(player, data, resultStack, Math.max(1, resultStack.getCount()))) {
            return false;
        }
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (RealCivConfig.specializationSingleProfessionLockEnabled()
                && !record.canProgressProfession(Profession.CRAFTER)) {
            return false;
        }
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int resultCount = Math.max(1, resultStack.getCount());

        int globalLimit = RealCivConfig.crafterLimitForLevel(crafterLevel);
        if (record.crafterActions() + resultCount > globalLimit) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(resultStack.getItem());
        int itemCap = RealCivConfig.crafterItemActionCapForLevel(itemId, crafterLevel);
        if (itemCap > 0 && record.crafterItemActions(itemId) + resultCount > itemCap) {
            return false;
        }
        int dailyItemCap = RealCivConfig.crafterDailyItemActionCapForLevel(itemId, crafterLevel);
        if (dailyItemCap > 0 && record.crafterDailyItemActions(itemId) + resultCount > dailyItemCap) {
            return false;
        }

        return true;
    }

    public static void notifyCraftDenied(ServerPlayer player, ItemStack resultStack) {
        if (player.getServer() == null) {
            return;
        }
        CivSavedData data = CivSavedData.get(player.getServer());
        if (RealCivConfig.carryCapCraftEnabled()
                && resultStack != null
                && !resultStack.isEmpty()
                && !CarryCapService.canAcquireForCraft(player, data, resultStack, Math.max(1, resultStack.getCount()))) {
            return;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int resultCount = resultStack != null && !resultStack.isEmpty() ? Math.max(1, resultStack.getCount()) : 1;

        if (RealCivConfig.specializationSingleProfessionLockEnabled()
                && !record.canProgressProfession(Profession.CRAFTER)) {
            Profession focus = record.focusedProfession();
            if (focus == null) {
                RealCivMessages.deny(
                        player,
                        "Specialization lock is enabled. Choose your profession focus with "
                                + "/realciv profession focus set <profession>.");
            } else {
                RealCivMessages.deny(
                        player,
                        "Your profession focus is " + focus.name().toLowerCase(java.util.Locale.ROOT)
                                + ". You cannot progress crafter while focused elsewhere.");
            }
            return;
        }

        if (!resultStack.isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(resultStack.getItem());
            int itemCap = RealCivConfig.crafterItemActionCapForLevel(itemId, crafterLevel);
            if (itemCap > 0) {
                int used = record.crafterItemActions(itemId);
                if (used + resultCount > itemCap) {
                    RealCivMessages.deny(
                            player,
                            "Crafter item cap reached for " + itemId.getPath()
                                    + " (" + used + "/" + itemCap + "). "
                                    + "Contribute crafted goods at the Community Hub to reset your crafter window.");
                    return;
                }
            }
            int dailyItemCap = RealCivConfig.crafterDailyItemActionCapForLevel(itemId, crafterLevel);
            if (dailyItemCap > 0) {
                int used = record.crafterDailyItemActions(itemId);
                if (used + resultCount > dailyItemCap) {
                    RealCivMessages.deny(
                            player,
                            "Daily crafter cap reached for " + itemId.getPath()
                                    + " (" + used + "/" + dailyItemCap + ").");
                    return;
                }
            }
        }

        int globalLimit = RealCivConfig.crafterLimitForLevel(crafterLevel);
        RealCivMessages.deny(
                player,
                "You can't craft more until you've contributed crafted goods to the Community Hub. "
                        + "Crafter limit reached (" + record.crafterActions() + "/" + globalLimit + ").");
    }

    public static void notifyCraftDenied(ServerPlayer player) {
        notifyCraftDenied(player, ItemStack.EMPTY);
    }
}
