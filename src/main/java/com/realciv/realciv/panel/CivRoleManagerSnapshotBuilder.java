package com.realciv.realciv.panel;

import com.realciv.realciv.data.CivRoleView;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class CivRoleManagerSnapshotBuilder {
    public static final int ROLES_PER_PAGE = 8;

    private CivRoleManagerSnapshotBuilder() {
    }

    public static CivRoleManagerSnapshot build(
            ServerPlayer viewer,
            CivSavedData data,
            String civId,
            int requestedPage,
            @Nullable String requestedRoleId) {
        boolean canManageGovernance = CivPermissionService.hasCivPermission(
                viewer, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
        List<CivRoleView> allRoles = data.customRolesSorted(civId);
        int totalRoles = allRoles.size();
        int rolePageCount = Math.max(1, (totalRoles + ROLES_PER_PAGE - 1) / ROLES_PER_PAGE);
        int rolePage = Math.max(0, Math.min(requestedPage, rolePageCount - 1));

        int start = rolePage * ROLES_PER_PAGE;
        int end = Math.min(totalRoles, start + ROLES_PER_PAGE);
        List<CivRoleView> pageRoles = allRoles.subList(start, end);

        @Nullable CivRoleView selectedRole = resolveSelectedRole(allRoles, pageRoles, requestedRoleId);
        String selectedRoleId = selectedRole == null ? "" : selectedRole.roleId();
        String selectedRoleDisplayName = selectedRole == null ? "-" : selectedRole.displayName();
        int selectedRoleMemberCount = selectedRole == null ? 0 : selectedRole.members().size();
        int selectedRolePermissionCount = selectedRole == null ? 0 : selectedRole.permissions().size();

        List<CivRoleManagerSnapshot.RoleRow> roles = new ArrayList<>(pageRoles.size());
        for (CivRoleView role : pageRoles) {
            roles.add(new CivRoleManagerSnapshot.RoleRow(
                    role.roleId(),
                    role.displayName(),
                    role.members().size(),
                    role.permissions().size(),
                    role.roleId().equals(selectedRoleId)));
        }

        List<String> knownPermissions = CivSavedData.knownRolePermissions();
        List<CivRoleManagerSnapshot.PermissionRow> permissionRows = new ArrayList<>(knownPermissions.size());
        for (int i = 0; i < knownPermissions.size(); i++) {
            String key = knownPermissions.get(i);
            boolean granted = selectedRole != null && selectedRole.permissions().contains(key);
            permissionRows.add(new CivRoleManagerSnapshot.PermissionRow(
                    i,
                    key,
                    permissionLabel(key),
                    granted));
        }

        List<String> selectedMembers = new ArrayList<>();
        if (selectedRole != null) {
            for (UUID memberId : selectedRole.members()) {
                selectedMembers.add(RealCivUtil.playerDisplayName(viewer, memberId));
                if (selectedMembers.size() >= CivRoleManagerSnapshot.MAX_MEMBERS) {
                    break;
                }
            }
        }

        return new CivRoleManagerSnapshot(
                RealCivUtil.civilizationDisplayName(data, civId),
                roleLabel(data, civId, viewer.getUUID()),
                canManageGovernance,
                rolePage,
                rolePageCount,
                totalRoles,
                selectedRoleId,
                selectedRoleDisplayName,
                selectedRoleMemberCount,
                selectedRolePermissionCount,
                List.copyOf(roles),
                List.copyOf(permissionRows),
                List.copyOf(selectedMembers));
    }

    @Nullable
    private static CivRoleView resolveSelectedRole(
            List<CivRoleView> allRoles,
            List<CivRoleView> pageRoles,
            @Nullable String requestedRoleId) {
        if (requestedRoleId != null && !requestedRoleId.isBlank()) {
            for (CivRoleView role : allRoles) {
                if (role.roleId().equals(requestedRoleId)) {
                    return role;
                }
            }
        }
        if (!pageRoles.isEmpty()) {
            return pageRoles.get(0);
        }
        return null;
    }

    private static String roleLabel(CivSavedData data, String civId, UUID playerId) {
        if (data.isMayor(civId, playerId)) {
            return data.leaderTitle(civId);
        }
        if (data.isCivicManager(civId, playerId)) {
            return "Civic Manager";
        }
        return "Citizen";
    }

    private static String permissionLabel(String permissionKey) {
        return switch (permissionKey) {
            case CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY -> "Manage diplomacy";
            case CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE -> "Manage friendly fire";
            case CivSavedData.ROLE_PERMISSION_MANAGE_PROFESSION_FOCUS -> "Manage profession focus";
            case CivSavedData.ROLE_PERMISSION_MANAGE_EXPLOSIVES -> "Manage explosives";
            case CivSavedData.ROLE_PERMISSION_MANAGE_REDSTONERS -> "Manage redstoners";
            case CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS -> "Manage town claims";
            case CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING -> "Manage land zoning";
            case CivSavedData.ROLE_PERMISSION_MANAGE_LAND_MANAGERS -> "Manage land managers";
            case CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE -> "Manage FTB mode";
            case CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS -> "Manage census";
            case CivSavedData.ROLE_PERMISSION_POLICE_MEMBERS -> "Police members";
            case CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS_ROLES -> "Manage census roles";
            case CivSavedData.ROLE_PERMISSION_MANAGE_LEADERSHIP -> "Manage leadership";
            case CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES -> "Manage withdraw rates";
            case CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION -> "Manage hub distribution";
            case CivSavedData.ROLE_PERMISSION_MANAGE_HUB_WITHDRAWALS -> "Manage hub withdrawals";
            case CivSavedData.ROLE_PERMISSION_VIEW_HUB_LOGS -> "View hub logs";
            case CivSavedData.ROLE_PERMISSION_VIEW_HUB_QUOTAS -> "View hub quotas";
            case CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP -> "Manage upkeep";
            case CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE -> "Manage governance";
            default -> permissionKey.replace('_', ' ');
        };
    }
}
