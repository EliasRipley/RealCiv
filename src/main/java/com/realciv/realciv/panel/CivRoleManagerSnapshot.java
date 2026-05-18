package com.realciv.realciv.panel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record CivRoleManagerSnapshot(
        String civDisplayName,
        String playerRole,
        boolean canManageGovernance,
        int rolePage,
        int rolePageCount,
        int totalRoles,
        String selectedRoleId,
        String selectedRoleDisplayName,
        int selectedRoleMemberCount,
        int selectedRolePermissionCount,
        List<RoleRow> roles,
        List<PermissionRow> permissions,
        List<String> selectedMembers) {

    public static final int MAX_CIV_NAME = 128;
    public static final int MAX_ROLE_ID = 64;
    public static final int MAX_ROLE_NAME = 64;
    public static final int MAX_ROLE_LABEL = 64;
    public static final int MAX_PERMISSION_KEY = 64;
    public static final int MAX_PERMISSION_LABEL = 96;
    public static final int MAX_MEMBER_NAME = 64;
    public static final int MAX_ROLES = 12;
    public static final int MAX_PERMISSIONS = 32;
    public static final int MAX_MEMBERS = 24;

    public record RoleRow(
            String roleId,
            String displayName,
            int memberCount,
            int permissionCount,
            boolean selected) {
    }

    public record PermissionRow(
            int permissionIndex,
            String permissionKey,
            String permissionLabel,
            boolean granted) {
    }

    public static CivRoleManagerSnapshot empty() {
        return new CivRoleManagerSnapshot(
                "",
                "",
                false,
                0,
                1,
                0,
                "",
                "",
                0,
                0,
                List.of(),
                List.of(),
                List.of());
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(civDisplayName, MAX_CIV_NAME);
        buffer.writeUtf(playerRole, MAX_ROLE_LABEL);
        buffer.writeBoolean(canManageGovernance);
        buffer.writeVarInt(rolePage);
        buffer.writeVarInt(rolePageCount);
        buffer.writeVarInt(totalRoles);
        buffer.writeUtf(selectedRoleId, MAX_ROLE_ID);
        buffer.writeUtf(selectedRoleDisplayName, MAX_ROLE_NAME);
        buffer.writeVarInt(selectedRoleMemberCount);
        buffer.writeVarInt(selectedRolePermissionCount);

        int roleCount = Math.min(MAX_ROLES, roles.size());
        buffer.writeVarInt(roleCount);
        for (int i = 0; i < roleCount; i++) {
            RoleRow row = roles.get(i);
            buffer.writeUtf(row.roleId(), MAX_ROLE_ID);
            buffer.writeUtf(row.displayName(), MAX_ROLE_NAME);
            buffer.writeVarInt(row.memberCount());
            buffer.writeVarInt(row.permissionCount());
            buffer.writeBoolean(row.selected());
        }

        int permissionCount = Math.min(MAX_PERMISSIONS, permissions.size());
        buffer.writeVarInt(permissionCount);
        for (int i = 0; i < permissionCount; i++) {
            PermissionRow row = permissions.get(i);
            buffer.writeVarInt(row.permissionIndex());
            buffer.writeUtf(row.permissionKey(), MAX_PERMISSION_KEY);
            buffer.writeUtf(row.permissionLabel(), MAX_PERMISSION_LABEL);
            buffer.writeBoolean(row.granted());
        }

        int memberCount = Math.min(MAX_MEMBERS, selectedMembers.size());
        buffer.writeVarInt(memberCount);
        for (int i = 0; i < memberCount; i++) {
            buffer.writeUtf(selectedMembers.get(i), MAX_MEMBER_NAME);
        }
    }

    public static CivRoleManagerSnapshot read(RegistryFriendlyByteBuf buffer) {
        String civDisplayName = buffer.readUtf(MAX_CIV_NAME);
        String playerRole = buffer.readUtf(MAX_ROLE_LABEL);
        boolean canManageGovernance = buffer.readBoolean();
        int rolePage = buffer.readVarInt();
        int rolePageCount = buffer.readVarInt();
        int totalRoles = buffer.readVarInt();
        String selectedRoleId = buffer.readUtf(MAX_ROLE_ID);
        String selectedRoleDisplayName = buffer.readUtf(MAX_ROLE_NAME);
        int selectedRoleMemberCount = buffer.readVarInt();
        int selectedRolePermissionCount = buffer.readVarInt();

        int roleCount = Math.min(MAX_ROLES, Math.max(0, buffer.readVarInt()));
        List<RoleRow> roles = new ArrayList<>(roleCount);
        for (int i = 0; i < roleCount; i++) {
            roles.add(new RoleRow(
                    buffer.readUtf(MAX_ROLE_ID),
                    buffer.readUtf(MAX_ROLE_NAME),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean()));
        }

        int permissionCount = Math.min(MAX_PERMISSIONS, Math.max(0, buffer.readVarInt()));
        List<PermissionRow> permissions = new ArrayList<>(permissionCount);
        for (int i = 0; i < permissionCount; i++) {
            permissions.add(new PermissionRow(
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_PERMISSION_KEY),
                    buffer.readUtf(MAX_PERMISSION_LABEL),
                    buffer.readBoolean()));
        }

        int memberCount = Math.min(MAX_MEMBERS, Math.max(0, buffer.readVarInt()));
        List<String> selectedMembers = new ArrayList<>(memberCount);
        for (int i = 0; i < memberCount; i++) {
            selectedMembers.add(buffer.readUtf(MAX_MEMBER_NAME));
        }

        return new CivRoleManagerSnapshot(
                civDisplayName,
                playerRole,
                canManageGovernance,
                rolePage,
                rolePageCount,
                totalRoles,
                selectedRoleId,
                selectedRoleDisplayName,
                selectedRoleMemberCount,
                selectedRolePermissionCount,
                List.copyOf(roles),
                List.copyOf(permissions),
                List.copyOf(selectedMembers));
    }
}
