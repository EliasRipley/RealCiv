package com.realciv.realciv.logic;

import com.realciv.realciv.data.CivSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class CivPermissionService {
    private CivPermissionService() {
    }

    public static boolean hasCivPermission(ServerPlayer player, CivSavedData data, String civId, String permissionKey) {
        if (player.hasPermissions(3) || RealCivUtil.isBypass(player) || data.isMayor(civId, player.getUUID())) {
            return true;
        }
        return data.hasCustomRolePermission(civId, player.getUUID(), permissionKey);
    }

    public static boolean hasCivPermission(CommandSourceStack source, CivSavedData data, String civId, String permissionKey) {
        if (source.hasPermission(3)) {
            return true;
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            return hasCivPermission(player, data, civId, permissionKey);
        }
        return false;
    }

    public static boolean isMayorOrAdmin(CommandSourceStack source, CivSavedData data, String civId) {
        if (source.hasPermission(3)) {
            return true;
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            return data.isMayor(civId, player.getUUID());
        }
        return false;
    }
}
