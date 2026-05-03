package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
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
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int limit = RealCivConfig.crafterLimitForLevel(crafterLevel);
        int resultCount = Math.max(1, resultStack.getCount());
        return record.crafterActions() + resultCount <= limit;
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
        int limit = RealCivConfig.crafterLimitForLevel(crafterLevel);
        RealCivMessages.deny(
                player,
                "You can't craft more until you've contributed crafted goods to the Community Hub. "
                        + "Crafter limit reached (" + limit + ").");
    }

    public static void notifyCraftDenied(ServerPlayer player) {
        notifyCraftDenied(player, ItemStack.EMPTY);
    }
}
