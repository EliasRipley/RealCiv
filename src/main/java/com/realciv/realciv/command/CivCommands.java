package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivRoleView;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.data.CivilizationRecord;
import com.realciv.realciv.data.DeleteCivilizationResult;
import com.realciv.realciv.data.DiplomacyState;
import com.realciv.realciv.data.DiplomacyView;
import com.realciv.realciv.data.WarType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class CivCommands {
    private CivCommands() {
    }

    public static int civInfo(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        boolean mayor = data.isMayor(civId, target.getUUID());
        String leaderTitle = data.leaderTitle(civId);
        String leaderSuffix = mayor ? " (" + leaderTitle + ")" : "";
        if (source.hasPermission(3)) {
            source.sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " belongs to " + RealCivCommands.civDisplay(data, civId) + " [" + civId + "]"
                            + leaderSuffix),
                    false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " belongs to " + RealCivCommands.civDisplay(data, civId)
                            + leaderSuffix),
                    false);
        }
        return 1;
    }

    public static int civList(CommandSourceStack source) {
        CivSavedData data = CivSavedData.get(source.getServer());
        List<String> ids = data.civilizationIdsSorted();
        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No civilizations configured."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Civilizations:"), false);
        boolean showInternalId = source.hasPermission(3);
        for (String civId : ids) {
            CivilizationRecord civ = data.getCivilization(civId);
            String name = civ == null ? civId : civ.displayName();
            int plots = civ == null ? 0 : civ.plots().size();
            if (showInternalId) {
                source.sendSuccess(() -> Component.literal("- " + name + " [" + civId + "] | plots " + plots), false);
            } else {
                source.sendSuccess(() -> Component.literal("- " + name + " | plots " + plots), false);
            }
        }
        return 1;
    }

    public static int civTitleShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civRef == null ? RealCivCommands.civOfSource(source, data) : RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Leadership title for " + RealCivCommands.civDisplay(data, civId) + ": " + data.leaderTitle(civId)),
                false);
        return 1;
    }

    public static int civTitleSetSelf(CommandSourceStack source, String titleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can set civilization title."));
            return 0;
        }
        if (!data.setLeaderTitle(civId, titleRaw, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Title may already match."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Leadership title for " + RealCivCommands.civDisplay(data, civId) + " set to " + data.leaderTitle(civId) + "."),
                true);
        return 1;
    }

    public static int civTitleSetAdmin(CommandSourceStack source, String civRef, String titleRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        if (!data.setLeaderTitle(civId, titleRaw, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Title may already match."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Leadership title for " + RealCivCommands.civDisplay(data, civId) + " set to " + data.leaderTitle(civId) + "."),
                true);
        return 1;
    }

    public static int civTitleResetSelf(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return civTitleSetSelf(source, "Mayor");
    }

    public static int civTitleResetAdmin(CommandSourceStack source, String civRef) {
        return civTitleSetAdmin(source, civRef, "Mayor");
    }

    public static int civGovernanceShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civRef == null ? RealCivCommands.civOfSource(source, data) : RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Civic attributes for ").append(RealCivCommands.civDisplay(data, civId)).append(":");
        for (AttributeCategory cat : AttributeCategory.values()) {
            CivicAttribute attr = data.civicAttribute(civId, cat);
            sb.append("\n  ").append(cat.displayName()).append(": ").append(attr.displayName());
        }
        String text = sb.toString();
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    public static int civAttributeSetSelf(CommandSourceStack source, String categoryRaw, String attributeRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can set civic attributes."));
            return 0;
        }
        @Nullable AttributeCategory cat = AttributeCategory.fromSerializedName(categoryRaw);
        if (cat == null) {
            source.sendFailure(Component.literal("Unknown category. Use executive, succession, resource, taxation, membership, or land."));
            return 0;
        }
        @Nullable CivicAttribute attr = CivicAttribute.fromSerializedName(attributeRaw);
        if (attr == null || attr.category() != cat) {
            source.sendFailure(Component.literal("Unknown attribute for category '" + cat.displayName()
                    + "'. Check /realciv civ governance for options."));
            return 0;
        }
        if (!data.setCivicAttribute(civId, cat, attr, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Attribute already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                cat.displayName() + " for " + RealCivCommands.civDisplay(data, civId)
                        + " set to " + attr.displayName() + "."), true);
        return 1;
    }

    public static int civAttributeSetAdmin(CommandSourceStack source, String civRef, String categoryRaw, String attributeRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        @Nullable AttributeCategory cat = AttributeCategory.fromSerializedName(categoryRaw);
        if (cat == null) {
            source.sendFailure(Component.literal("Unknown category. Use executive, succession, resource, taxation, membership, or land."));
            return 0;
        }
        @Nullable CivicAttribute attr = CivicAttribute.fromSerializedName(attributeRaw);
        if (attr == null || attr.category() != cat) {
            source.sendFailure(Component.literal("Unknown attribute for category '" + cat.displayName()
                    + "'. Check /realciv civ governance for options."));
            return 0;
        }
        if (!data.setCivicAttribute(civId, cat, attr, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Attribute already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                cat.displayName() + " for " + RealCivCommands.civDisplay(data, civId)
                        + " set to " + attr.displayName() + "."), true);
        return 1;
    }

    public static int civRoleList(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civRef == null ? RealCivCommands.civOfSource(source, data) : RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        List<CivRoleView> roles = data.customRolesSorted(civId);
        if (roles.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No custom roles configured for " + RealCivCommands.civDisplay(data, civId) + "."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(
                "Custom roles for " + RealCivCommands.civDisplay(data, civId) + ":"), false);
        for (CivRoleView role : roles) {
            String permissions = role.permissions().isEmpty()
                    ? "-"
                    : String.join(", ", role.permissions().stream().sorted().toList());
            String members = role.members().isEmpty()
                    ? "-"
                    : String.join(", ", role.members().stream()
                            .map(memberId -> RealCivCommands.playerNameOrShortId(source, memberId))
                            .toList());
            source.sendSuccess(() -> Component.literal(
                    "- " + role.displayName() + " [" + role.roleId() + "] | perms: " + permissions + " | members: " + members), false);
        }
        return 1;
    }

    public static int civRoleCreate(CommandSourceStack source, String roleRaw, String nameRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can create custom roles."));
            return 0;
        }
        @Nullable String roleId = CivSavedData.canonicalRoleId(roleRaw);
        if (roleId == null) {
            source.sendFailure(Component.literal("Invalid role id. Use letters, numbers, spaces, - or _."));
            return 0;
        }
        if (!data.createCustomRole(civId, roleId, nameRaw, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Unable to create role. It may already exist."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Created custom role '" + nameRaw + "' [" + roleId + "] for " + RealCivCommands.civDisplay(data, civId) + "."),
                true);
        return 1;
    }

    public static int civRoleRename(CommandSourceStack source, String roleRaw, String nameRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can rename custom roles."));
            return 0;
        }
        String actorName = RealCivCommands.actorName(source);

        @Nullable String customRoleId = resolveCustomRoleId(data, civId, roleRaw);
        if (customRoleId != null) {
            if (!data.renameCustomRole(civId, customRoleId, nameRaw, actorName)) {
                source.sendFailure(Component.literal("Role rename failed. New display name may already match."));
                return 0;
            }
            String resolvedRoleId = customRoleId;
            source.sendSuccess(() -> Component.literal(
                    "Renamed role [" + resolvedRoleId + "] to '" + nameRaw + "' for "
                            + RealCivCommands.civDisplay(data, civId) + "."),
                    true);
            return 1;
        }

        if (isLeaderTitleAlias(data, civId, roleRaw)) {
            if (!data.setLeaderTitle(civId, nameRaw, actorName)) {
                source.sendFailure(Component.literal("Leader title rename failed. New title may already match."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal(
                    "Leadership title for " + RealCivCommands.civDisplay(data, civId)
                            + " set to '" + data.leaderTitle(civId) + "'."), true);
            return 1;
        }

        source.sendFailure(Component.literal(
                "Role rename failed. Use /realciv civ role list for role ids, or /realciv civ title set <name> to rename leadership title."));
        return 0;
    }

    public static int civRoleDelete(CommandSourceStack source, String roleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can delete custom roles."));
            return 0;
        }
        if (!data.deleteCustomRole(civId, roleRaw, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Role delete failed. Check role id."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Deleted role [" + roleRaw + "]."), true);
        return 1;
    }

    @Nullable
    private static String resolveCustomRoleId(CivSavedData data, String civId, String roleRaw) {
        @Nullable String canonical = CivSavedData.canonicalRoleId(roleRaw);
        if (canonical != null && data.customRoleExists(civId, canonical)) {
            return canonical;
        }
        String requested = roleRaw == null ? "" : roleRaw.trim();
        if (requested.isEmpty()) {
            return null;
        }
        for (CivRoleView role : data.customRolesSorted(civId)) {
            if (role.displayName().equalsIgnoreCase(requested)) {
                return role.roleId();
            }
        }
        return null;
    }

    private static boolean isLeaderTitleAlias(CivSavedData data, String civId, String roleRaw) {
        @Nullable String requested = CivSavedData.canonicalRoleId(roleRaw);
        if (requested == null) {
            return false;
        }
        @Nullable String currentTitle = CivSavedData.canonicalRoleId(data.leaderTitle(civId));
        if (currentTitle != null && currentTitle.equals(requested)) {
            return true;
        }
        return "mayor".equals(requested) || "leader".equals(requested);
    }

    public static int civRolePermissionList(CommandSourceStack source, String roleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        List<CivRoleView> roles = data.customRolesSorted(civId);
        CivRoleView match = null;
        for (CivRoleView role : roles) {
            if (role.roleId().equals(CivSavedData.canonicalRoleId(roleRaw))) {
                match = role;
                break;
            }
        }
        if (match == null) {
            source.sendFailure(Component.literal("Role not found: " + roleRaw));
            return 0;
        }
        String permissions = match.permissions().isEmpty() ? "-" : String.join(", ", match.permissions());
        CivRoleView resolved = match;
        String permissionText = permissions;
        source.sendSuccess(() -> Component.literal(
                "Permissions for role '" + resolved.displayName() + "' [" + resolved.roleId() + "]: " + permissionText),
                false);
        return 1;
    }

    public static int civRolePermissionSet(CommandSourceStack source, String roleRaw, String permissionRaw, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage role permissions."));
            return 0;
        }
        @Nullable String permission = CivSavedData.canonicalRolePermission(permissionRaw);
        if (permission == null) {
            source.sendFailure(Component.literal("Invalid permission key."));
            return 0;
        }
        if (!data.setCustomRolePermission(civId, roleRaw, permission, allowed, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Check role id and permission key."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Granted " : "Revoked ")
                        + "permission '" + permission + "' for role [" + roleRaw + "]."),
                true);
        return 1;
    }

    public static int civRoleMemberList(CommandSourceStack source, String roleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        List<CivRoleView> roles = data.customRolesSorted(civId);
        CivRoleView match = null;
        for (CivRoleView role : roles) {
            if (role.roleId().equals(CivSavedData.canonicalRoleId(roleRaw))) {
                match = role;
                break;
            }
        }
        if (match == null) {
            source.sendFailure(Component.literal("Role not found: " + roleRaw));
            return 0;
        }
        String members = match.members().isEmpty()
                ? "-"
                : String.join(", ", match.members().stream()
                        .map(memberId -> RealCivCommands.playerNameOrShortId(source, memberId))
                        .toList());
        CivRoleView resolved = match;
        String memberText = members;
        source.sendSuccess(() -> Component.literal(
                "Members of role '" + resolved.displayName() + "' [" + resolved.roleId() + "]: " + memberText),
                false);
        return 1;
    }

    public static int civRoleMemberSet(CommandSourceStack source, String roleRaw, ServerPlayer player, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage role members."));
            return 0;
        }
        if (allowed) {
            String targetCiv = data.getOrAssignCivilization(player.getUUID());
            if (!targetCiv.equals(civId)) {
                source.sendFailure(Component.literal("Player must be a member of your civilization."));
                return 0;
            }
        }
        if (!data.setCustomRoleMember(civId, roleRaw, player.getUUID(), allowed, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Check role id and member status."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Added " : "Removed ")
                        + player.getGameProfile().getName() + (allowed ? " to " : " from ")
                        + "role [" + roleRaw + "]."),
                true);
        return 1;
    }

    public static int civCreate(CommandSourceStack source, String nameRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String displayName = nameRaw == null ? "" : nameRaw.trim();
        if (displayName.isEmpty()) {
            source.sendFailure(Component.literal("Civilization name cannot be empty."));
            return 0;
        }
        if (data.findCivilizationIdByDisplayName(displayName) != null) {
            source.sendFailure(Component.literal("A civilization with that name already exists."));
            return 0;
        }

        String id = data.suggestCivilizationId(displayName);
        if (!data.createCivilization(id, displayName, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Unable to create civilization. Name or internal id may already exist."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Created civilization '" + displayName + "'. Internal id: " + id + "."), true);
        return 1;
    }

    public static int civFound(CommandSourceStack source, String nameRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer founder = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!RealCivCommands.canFoundCivilization(source, data, founder)) {
            source.sendFailure(Component.literal(
                    "You are not approved to found a civilization. Ask an admin to run "
                            + "/realciv mayor approval add " + founder.getGameProfile().getName() + "."));
            return 0;
        }

        String displayName = nameRaw == null ? "" : nameRaw.trim();
        if (displayName.isEmpty()) {
            source.sendFailure(Component.literal("Civilization name cannot be empty."));
            return 0;
        }
        if (data.findCivilizationIdByDisplayName(displayName) != null) {
            source.sendFailure(Component.literal("A civilization with that name already exists."));
            return 0;
        }

        String id = data.suggestCivilizationId(displayName);
        if (!data.createCivilization(id, displayName, founder.getGameProfile().getName())) {
            source.sendFailure(Component.literal("Unable to found civilization. Name or internal id may already exist."));
            return 0;
        }

        data.setPlayerCivilization(founder.getUUID(), id, founder.getGameProfile().getName());
        data.setMayor(id, founder.getUUID(), founder.getGameProfile().getName());
        if (RealCivConfig.requireFounderApproval() && !source.hasPermission(3)) {
            data.consumeFounderApproval(founder.getUUID(), founder.getGameProfile().getName());
        }
        RealCivCommands.grantMayorStarterHub(founder);
        source.sendSuccess(() -> Component.literal(
                "You founded '" + displayName + "' and are now its " + data.leaderTitle(id).toLowerCase(Locale.ROOT) + "."), true);
        return 1;
    }

    public static int civRename(CommandSourceStack source, String civRef, String name) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        if (!data.renameCivilization(civId, name, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Unable to rename civilization. Name may already be taken."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Renamed civilization '" + civRef + "' to '" + name + "'."), true);
        return 1;
    }

    public static int civDelete(CommandSourceStack source, String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }

        String defaultCiv = RealCivConfig.defaultCivilizationId();
        if (civId.equals(defaultCiv)) {
            source.sendFailure(Component.literal("Cannot delete the default civilization."));
            return 0;
        }

        DeleteCivilizationResult result = data.deleteCivilization(civId, defaultCiv, RealCivCommands.actorName(source));
        if (result == null) {
            source.sendFailure(Component.literal("Unable to delete civilization."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Deleted '" + result.deletedDisplayName() + "' [" + result.deletedId() + "]. "
                        + "Reassigned members: " + result.reassignedMembers()
                        + ", migrated accounts: " + result.migratedAccounts()
                        + ", transferred stock entries: " + result.transferredStockEntries()
                        + " (" + result.transferredStockItems() + " items), removed plots: " + result.removedPlots() + "."),
                true);
        return 1;
    }

    public static int civJoin(CommandSourceStack source, String civRef)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        String currentCiv = data.getOrAssignCivilization(player.getUUID());
        if (currentCiv.equals(civId)) {
            source.sendFailure(Component.literal("You are already in " + RealCivCommands.civDisplay(data, civId) + "."));
            return 0;
        }

        boolean hasInvite = data.hasInvite(civId, player.getUUID());
        CivicAttribute membership = data.civicAttribute(civId, AttributeCategory.MEMBERSHIP);
        boolean canDirectJoin = switch (membership) {
            case OPEN -> true;
            case INVITE_ONLY -> source.hasPermission(3) || hasInvite;
            case APPLICATION -> source.hasPermission(3);
            default -> false;
        };
        if (canDirectJoin) {
            if (!data.setPlayerCivilization(player.getUUID(), civId, RealCivCommands.actorName(source))) {
                source.sendFailure(Component.literal("Failed to join civilization."));
                return 0;
            }
            String newCiv = data.getOrAssignCivilization(player.getUUID());
            source.sendSuccess(() -> Component.literal(
                    "You are now a citizen of " + RealCivCommands.civDisplay(data, newCiv) + "."), true);
            return 1;
        }

        boolean created = data.addJoinRequest(civId, player.getUUID(), RealCivCommands.actorName(source));
        if (!created) {
            source.sendSuccess(() -> Component.literal(
                    "Your join request is already pending for " + RealCivCommands.civDisplay(data, civId) + "."), false);
            return 1;
        }

        @Nullable UUID mayorId = data.getMayorId(civId);
        if (mayorId != null) {
            ServerPlayer mayorOnline = source.getServer().getPlayerList().getPlayer(mayorId);
            if (mayorOnline != null) {
                mayorOnline.sendSystemMessage(Component.literal(
                        player.getGameProfile().getName() + " requested to join " + RealCivCommands.civDisplay(data, civId)
                                + ". Use Census Block UI to approve/deny."));
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Join request submitted to " + RealCivCommands.civDisplay(data, civId)
                        + ". Wait for mayor/manager approval at Census."), true);
        return 1;
    }

    public static int civLeave(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String defaultCiv = RealCivConfig.defaultCivilizationId();
        if (!data.setPlayerCivilization(player.getUUID(), defaultCiv, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Unable to leave civilization."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "You are now unaligned (" + RealCivCommands.civDisplay(data, defaultCiv) + ")."), true);
        return 1;
    }

    public static int civAssign(CommandSourceStack source, ServerPlayer target, String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        if (!data.setPlayerCivilization(target.getUUID(), civId, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Failed to assign player to civilization."));
            return 0;
        }
        String newCiv = data.getOrAssignCivilization(target.getUUID());
        source.sendSuccess(() -> Component.literal(
                "Assigned " + target.getGameProfile().getName()
                        + " to " + RealCivCommands.civDisplay(data, newCiv) + "."),
                true);
        return 1;
    }

    public static int civDiplomacyShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = RealCivCommands.civOfSource(source, data);
        } else {
            civId = RealCivCommands.resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Diplomacy for " + RealCivCommands.civDisplay(data, civId) + " [" + civId + "]"), false);
        source.sendSuccess(() -> Component.literal(
                "Intra-civ PvP (friendly fire): " + (data.allowIntraCivPvp(civId) ? "ENABLED" : "DISABLED")), false);

        List<DiplomacyView> relations = data.nonNeutralDiplomacyEntriesFor(civId);
        if (relations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("All external relations are currently NEUTRAL."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Non-neutral relations:"), false);
            for (DiplomacyView relation : relations) {
                String other = relation.otherCivilizationId();
                source.sendSuccess(() -> Component.literal(
                        "- " + RealCivCommands.civDisplay(data, other) + " [" + other + "]: " + relation.state().displayName()), false);
            }
        }

        List<CivSavedData.DiplomacyRequestView> incoming = data.incomingDiplomacyRequestsFor(civId);
        if (incoming.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Incoming requests: none."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Incoming requests:"), false);
            for (CivSavedData.DiplomacyRequestView request : incoming) {
                String fromCiv = request.requesterCivilizationId();
                String detail = formatWarRequestSummary(request);
                source.sendSuccess(() -> Component.literal(
                        "- from " + RealCivCommands.civDisplay(data, fromCiv) + " [" + fromCiv + "]: "
                                + request.requestedState().displayName() + detail),
                        false);
            }
        }

        List<CivSavedData.DiplomacyRequestView> outgoing = data.outgoingDiplomacyRequestsFor(civId);
        if (outgoing.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Outgoing requests: none."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Outgoing requests:"), false);
            for (CivSavedData.DiplomacyRequestView request : outgoing) {
                String toCiv = request.responderCivilizationId();
                String detail = formatWarRequestSummary(request);
                source.sendSuccess(() -> Component.literal(
                        "- to " + RealCivCommands.civDisplay(data, toCiv) + " [" + toCiv + "]: "
                                + request.requestedState().displayName() + detail),
                        false);
            }
        }

        List<CivSavedData.ActiveWarView> activeWars = data.activeWarsFor(civId);
        if (activeWars.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Active wars: none."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Active wars:"), false);
            for (CivSavedData.ActiveWarView war : activeWars) {
                boolean civIsFirst = civId.equals(war.firstCivilizationId());
                String otherCiv = civIsFirst ? war.secondCivilizationId() : war.firstCivilizationId();
                long ourLosses = civIsFirst ? war.firstCivilizationCasualties() : war.secondCivilizationCasualties();
                long theirLosses = civIsFirst ? war.secondCivilizationCasualties() : war.firstCivilizationCasualties();
                String mode = war.warType() == WarType.PVP
                        ? "PVP target " + war.pvpKillTarget()
                        : "DESTRUCTION";
                String options = "submission:" + (war.warOfSubmission() ? "on" : "off")
                        + ", land:" + (war.warOfLand() ? "on" : "off");
                source.sendSuccess(() -> Component.literal(
                        "- vs " + RealCivCommands.civDisplay(data, otherCiv) + " [" + otherCiv + "]"
                                + " | " + mode
                                + " | losses us:" + ourLosses + " them:" + theirLosses
                                + " | " + options),
                        false);
            }
        }

        @Nullable String overlord = data.overlordOf(civId);
        if (overlord != null) {
            source.sendSuccess(() -> Component.literal(
                    "Vassal status: this civilization is a vassal of "
                            + RealCivCommands.civDisplay(data, overlord) + " [" + overlord + "]."),
                    false);
        }
        List<String> vassals = data.vassalsOf(civId);
        if (!vassals.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Vassals under this civilization:"), false);
            for (String vassalCiv : vassals) {
                source.sendSuccess(() -> Component.literal(
                        "- " + RealCivCommands.civDisplay(data, vassalCiv) + " [" + vassalCiv + "]"),
                        false);
            }
        }
        return 1;
    }

    public static int civWarShow(CommandSourceStack source, @Nullable String civRef) {
        return civDiplomacyShow(source, civRef);
    }

    private static String formatWarRequestSummary(CivSavedData.DiplomacyRequestView request) {
        if (request.requestedState() != DiplomacyState.WAR) {
            return "";
        }
        WarType warType = request.warType() == null ? WarType.DESTRUCTION : request.warType();
        return " | " + formatWarSummary(
                warType,
                Math.max(1, request.pvpKillTarget()),
                request.warOfSubmission(),
                request.warOfLand());
    }

    private static String formatWarSummary(
            WarType warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand) {
        return formatWarSummary(warType, pvpKillTarget, warOfSubmission, warOfLand, false, null, 0L);
    }

    private static String formatWarSummary(
            WarType warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmount) {
        String base = warType == WarType.PVP
                ? "PVP (target " + Math.max(1, pvpKillTarget) + ")"
                : "DESTRUCTION";
        String gamble = warResourceGamble && warGambleItemId != null && warGambleAmount > 0L
                ? ", gamble:" + warGambleAmount + "x " + warGambleItemId
                : "";
        return base
                + ", submission:" + (warOfSubmission ? "on" : "off")
                + ", land:" + (warOfLand ? "on" : "off")
                + gamble;
    }

    public static int civDiplomacySet(CommandSourceStack source, String otherCivRef, String stateRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY)) {
            source.sendFailure(Component.literal("Only leadership/admin can change diplomacy for your civilization."));
            return 0;
        }

        String otherCiv = RealCivCommands.resolveCivilizationId(data, otherCivRef);
        if (otherCiv == null) {
            source.sendFailure(Component.literal("Civilization not found: " + otherCivRef));
            return 0;
        }
        if (actorCiv.equals(otherCiv)) {
            source.sendFailure(Component.literal("Use /realciv civ pvp friendlyfire on|off to control same-civ PvP."));
            return 0;
        }

        @Nullable DiplomacyState state = RealCivCommands.parseDiplomacyState(stateRaw);
        if (state == null) {
            source.sendFailure(Component.literal("Invalid diplomacy state. Use ally, neutral, or war."));
            return 0;
        }

        CivSavedData.DiplomacyRequestResult result = data.proposeDiplomacyStateChange(
                actorCiv,
                otherCiv,
                state,
                RealCivCommands.actorName(source));
        CivSavedData.DiplomacyRequestResultType type = result.type();
        String actorDisplay = RealCivCommands.civDisplay(data, actorCiv);
        String otherDisplay = RealCivCommands.civDisplay(data, otherCiv);

        if (type == CivSavedData.DiplomacyRequestResultType.STATE_SET) {
            source.sendSuccess(() -> Component.literal(
                    "Set diplomacy between " + actorDisplay + " and "
                            + otherDisplay + " to " + result.requestedState().displayName() + "."),
                    true);
            return 1;
        }
        if (type == CivSavedData.DiplomacyRequestResultType.REQUEST_ACCEPTED) {
            source.sendSuccess(() -> Component.literal(
                    "Accepted incoming " + result.requestedState().displayName() + " request from "
                            + otherDisplay + ". Diplomacy is now " + result.requestedState().displayName() + "."),
                    true);
            return 1;
        }
        if (type == CivSavedData.DiplomacyRequestResultType.REQUEST_SENT
                || type == CivSavedData.DiplomacyRequestResultType.REQUEST_UPDATED) {
            source.sendSuccess(() -> Component.literal(
                    "Sent " + result.requestedState().displayName() + " request to " + otherDisplay
                            + ". The other civilization can accept or reject it."),
                    true);
            return 1;
        }
        if (type == CivSavedData.DiplomacyRequestResultType.REQUEST_ALREADY_PENDING) {
            source.sendFailure(Component.literal(
                    result.requestedState().displayName() + " request to " + otherDisplay + " is already pending."));
            return 0;
        }
        if (type == CivSavedData.DiplomacyRequestResultType.NO_CHANGE_ALREADY_SET) {
            source.sendFailure(Component.literal(
                    "No change made. Diplomacy is already " + result.requestedState().displayName() + "."));
            return 0;
        }

        source.sendFailure(Component.literal("Unable to process diplomacy request."));
        return 0;
    }

    public static int civWarDeclare(
            CommandSourceStack source,
            String otherCivRef,
            String warTypeRaw,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand) {
        return civWarDeclare(source, otherCivRef, warTypeRaw, pvpKillTarget,
                warOfSubmission, warOfLand, false, null, 0L);
    }

    public static int civWarDeclare(
            CommandSourceStack source,
            String otherCivRef,
            String warTypeRaw,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand,
            boolean warResourceGamble,
            @Nullable String warGambleItemId,
            long warGambleAmount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY)) {
            source.sendFailure(Component.literal("Only leadership/admin can declare war for your civilization."));
            return 0;
        }
        String otherCiv = RealCivCommands.resolveCivilizationId(data, otherCivRef);
        if (otherCiv == null) {
            source.sendFailure(Component.literal("Civilization not found: " + otherCivRef));
            return 0;
        }
        if (actorCiv.equals(otherCiv)) {
            source.sendFailure(Component.literal("Cannot declare war on your own civilization."));
            return 0;
        }
        @Nullable WarType warType = WarType.fromSerializedName(warTypeRaw);
        if (warType == null) {
            source.sendFailure(Component.literal("Invalid war type. Use destruction or pvp."));
            return 0;
        }
        int safeTarget = warType == WarType.PVP
                ? Math.max(1, pvpKillTarget)
                : RealCivConfig.defaultWarPvpKillTarget();
        CivSavedData.DiplomacyRequestResult result = data.proposeWarDeclaration(
                actorCiv,
                otherCiv,
                warType,
                safeTarget,
                warOfSubmission,
                warOfLand,
                warResourceGamble,
                warGambleItemId,
                warGambleAmount,
                RealCivCommands.actorName(source));

        String otherDisplay = RealCivCommands.civDisplay(data, otherCiv);
        String warSummary = formatWarSummary(warType, safeTarget, warOfSubmission, warOfLand, warResourceGamble, warGambleItemId, warGambleAmount);
        if (result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_SENT
                || result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_UPDATED) {
            source.sendSuccess(() -> Component.literal(
                    "Sent WAR request to " + otherDisplay + " [" + otherCiv + "]: " + warSummary
                            + ". The other civilization can accept or reject it."),
                    true);
            return 1;
        }
        if (result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_ACCEPTED) {
            source.sendSuccess(() -> Component.literal(
                    "War accepted with " + otherDisplay + " [" + otherCiv + "]: " + warSummary + "."),
                    true);
            return 1;
        }
        if (result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_ALREADY_PENDING) {
            source.sendFailure(Component.literal(
                    "A WAR request to " + otherDisplay + " is already pending."));
            return 0;
        }
        if (result.type() == CivSavedData.DiplomacyRequestResultType.NO_CHANGE_ALREADY_SET) {
            source.sendFailure(Component.literal("No change made. You are already at war with " + otherDisplay + "."));
            return 0;
        }
        source.sendFailure(Component.literal("Unable to declare war right now."));
        return 0;
    }

    public static int civWarAccept(CommandSourceStack source, String otherCivRef) {
        return civWarRespond(source, otherCivRef, true);
    }

    public static int civWarReject(CommandSourceStack source, String otherCivRef) {
        return civWarRespond(source, otherCivRef, false);
    }

    private static int civWarRespond(CommandSourceStack source, String otherCivRef, boolean accept) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY)) {
            source.sendFailure(Component.literal("Only leadership/admin can respond to war declarations."));
            return 0;
        }
        String otherCiv = RealCivCommands.resolveCivilizationId(data, otherCivRef);
        if (otherCiv == null) {
            source.sendFailure(Component.literal("Civilization not found: " + otherCivRef));
            return 0;
        }
        if (actorCiv.equals(otherCiv)) {
            source.sendFailure(Component.literal("Cannot respond to your own civilization."));
            return 0;
        }

        @Nullable CivSavedData.DiplomacyRequestView pending = data.diplomacyRequest(otherCiv, actorCiv);
        if (pending == null || pending.requestedState() != DiplomacyState.WAR) {
            source.sendFailure(Component.literal(
                    "No pending war declaration from " + RealCivCommands.civDisplay(data, otherCiv) + "."));
            return 0;
        }

        CivSavedData.DiplomacyRequestResult result = data.respondToDiplomacyRequest(
                actorCiv,
                otherCiv,
                accept,
                RealCivCommands.actorName(source));
        String otherDisplay = RealCivCommands.civDisplay(data, otherCiv);
        if (result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_ACCEPTED) {
            WarType warType = pending.warType() == null ? WarType.DESTRUCTION : pending.warType();
            String warSummary = formatWarSummary(
                    warType,
                    Math.max(1, pending.pvpKillTarget()),
                    pending.warOfSubmission(),
                    pending.warOfLand());
            source.sendSuccess(() -> Component.literal(
                    "Accepted war declaration from " + otherDisplay + ": " + warSummary + "."),
                    true);
            return 1;
        }
        if (result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_REJECTED) {
            source.sendSuccess(() -> Component.literal(
                    "Rejected war declaration from " + otherDisplay + "."),
                    true);
            return 1;
        }
        source.sendFailure(Component.literal("Unable to respond to war declaration."));
        return 0;
    }

    public static int civWarResign(CommandSourceStack source, String otherCivRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY)) {
            source.sendFailure(Component.literal("Only leadership/admin can resign from war."));
            return 0;
        }
        String otherCiv = RealCivCommands.resolveCivilizationId(data, otherCivRef);
        if (otherCiv == null) {
            source.sendFailure(Component.literal("Civilization not found: " + otherCivRef));
            return 0;
        }
        if (actorCiv.equals(otherCiv)) {
            source.sendFailure(Component.literal("Cannot resign a war against your own civilization."));
            return 0;
        }
        CivSavedData.WarResignResult result = data.resignWar(actorCiv, otherCiv, RealCivCommands.actorName(source));
        if (result.type() == CivSavedData.WarResignResultType.NOT_AT_WAR) {
            source.sendFailure(Component.literal(
                    "Your civilization is not currently at war with " + RealCivCommands.civDisplay(data, otherCiv) + "."));
            return 0;
        }
        if (result.type() == CivSavedData.WarResignResultType.INVALID) {
            source.sendFailure(Component.literal("Unable to process resignation."));
            return 0;
        }
        @Nullable CivSavedData.WarOutcomeView outcome = result.outcome();
        if (outcome == null) {
            source.sendSuccess(() -> Component.literal("War resignation accepted."), true);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(
                "War ended. Winner: "
                        + RealCivCommands.civDisplay(data, outcome.winnerCivilizationId())
                        + " | Loser: " + RealCivCommands.civDisplay(data, outcome.loserCivilizationId())
                        + " | " + formatWarSummary(
                                outcome.warType(),
                                outcome.pvpKillTarget(),
                                outcome.warOfSubmission(),
                                outcome.warOfLand())
                        + (outcome.warOfLand() ? " | transferred plots: " + outcome.transferredPlotCount() : "")),
                true);
        return 1;
    }

    public static int civDiplomacyAccept(CommandSourceStack source, String otherCivRef) {
        return civDiplomacyRespond(source, otherCivRef, true);
    }

    public static int civDiplomacyReject(CommandSourceStack source, String otherCivRef) {
        return civDiplomacyRespond(source, otherCivRef, false);
    }

    private static int civDiplomacyRespond(CommandSourceStack source, String otherCivRef, boolean accept) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY)) {
            source.sendFailure(Component.literal("Only leadership/admin can respond to diplomacy requests."));
            return 0;
        }
        String otherCiv = RealCivCommands.resolveCivilizationId(data, otherCivRef);
        if (otherCiv == null) {
            source.sendFailure(Component.literal("Civilization not found: " + otherCivRef));
            return 0;
        }
        if (actorCiv.equals(otherCiv)) {
            source.sendFailure(Component.literal("Cannot respond to your own civilization."));
            return 0;
        }

        CivSavedData.DiplomacyRequestResult result = data.respondToDiplomacyRequest(
                actorCiv,
                otherCiv,
                accept,
                RealCivCommands.actorName(source));
        String otherDisplay = RealCivCommands.civDisplay(data, otherCiv);
        if (result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_ACCEPTED) {
            source.sendSuccess(() -> Component.literal(
                    "Accepted " + result.requestedState().displayName() + " request from "
                            + otherDisplay + ". Diplomacy is now " + result.requestedState().displayName() + "."),
                    true);
            return 1;
        }
        if (result.type() == CivSavedData.DiplomacyRequestResultType.REQUEST_REJECTED) {
            source.sendSuccess(() -> Component.literal(
                    "Rejected " + result.requestedState().displayName() + " request from " + otherDisplay + "."),
                    true);
            return 1;
        }
        if (result.type() == CivSavedData.DiplomacyRequestResultType.NO_PENDING_REQUEST) {
            source.sendFailure(Component.literal(
                    "No pending diplomacy request from " + otherDisplay + " to your civilization."));
            return 0;
        }
        source.sendFailure(Component.literal("Unable to respond to diplomacy request."));
        return 0;
    }

    public static int civDiplomacySetBetween(CommandSourceStack source, String civARef, String civBRef, String stateRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civA = RealCivCommands.resolveCivilizationId(data, civARef);
        if (civA == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civARef));
            return 0;
        }
        String civB = RealCivCommands.resolveCivilizationId(data, civBRef);
        if (civB == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civBRef));
            return 0;
        }
        if (civA.equals(civB)) {
            source.sendFailure(Component.literal("Cannot set diplomacy between the same civilization."));
            return 0;
        }

        @Nullable DiplomacyState state = RealCivCommands.parseDiplomacyState(stateRaw);
        if (state == null) {
            source.sendFailure(Component.literal("Invalid diplomacy state. Use ally, neutral, or war."));
            return 0;
        }
        if (!data.setDiplomacyState(civA, civB, state, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Diplomacy may already be " + state.displayName() + "."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Set diplomacy between " + RealCivCommands.civDisplay(data, civA) + " and "
                        + RealCivCommands.civDisplay(data, civB) + " to " + state.displayName() + "."), true);
        return 1;
    }

    public static int civFriendlyFireShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = RealCivCommands.civOfSource(source, data);
        } else {
            civId = RealCivCommands.resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Intra-civ PvP for " + RealCivCommands.civDisplay(data, civId) + ": "
                        + (data.allowIntraCivPvp(civId) ? "ENABLED" : "DISABLED")), false);
        return 1;
    }

    public static int civFriendlyFireSet(CommandSourceStack source, boolean allowed) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE)) {
            source.sendFailure(Component.literal("Only leadership/admin can change friendly-fire settings."));
            return 0;
        }
        if (!data.setAllowIntraCivPvp(civId, allowed, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Friendly fire is already " + (allowed ? "enabled" : "disabled") + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Friendly fire for " + RealCivCommands.civDisplay(data, civId) + " is now " + (allowed ? "ENABLED" : "DISABLED") + "."), true);
        return 1;
    }

    public static int civExplosivesShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = RealCivCommands.civOfSource(source, data);
        } else {
            civId = RealCivCommands.resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        int cap = RealCivConfig.maxExplosivesExpertsPerCivilization();
        List<UUID> experts = data.explosivesExpertsSorted(civId);
        source.sendSuccess(() -> Component.literal(
                "Explosives experts for " + RealCivCommands.civDisplay(data, civId) + " [" + civId + "]: "
                        + experts.size() + "/" + cap),
                false);
        if (cap <= 0) {
            source.sendSuccess(() -> Component.literal(
                    "Role is disabled by server config (civ.maxExplosivesExpertsPerCivilization=0)."), false);
        }
        if (experts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No members are designated as explosives experts."), false);
            return 1;
        }
        for (UUID expert : experts) {
            source.sendSuccess(() -> Component.literal("- " + RealCivCommands.playerNameOrShortId(source, expert) + " | " + expert), false);
        }
        return 1;
    }

    public static int civExplosivesSet(CommandSourceStack source, ServerPlayer target, boolean allowed) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_EXPLOSIVES)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage explosives experts."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!civId.equals(targetCiv)) {
            source.sendFailure(Component.literal(
                    "Target player is not in your civilization. Current civ: " + RealCivCommands.civDisplay(data, targetCiv) + "."));
            return 0;
        }

        int cap = RealCivConfig.maxExplosivesExpertsPerCivilization();
        if (allowed) {
            if (cap <= 0) {
                source.sendFailure(Component.literal(
                        "Explosives Expert role is disabled by server config for all civilizations."));
                return 0;
            }
            if (!data.isExplosivesExpert(civId, target.getUUID())
                    && data.explosivesExpertCount(civId) >= cap) {
                source.sendFailure(Component.literal(
                        "Cannot add more explosives experts. Cap reached (" + cap + ")."));
                return 0;
            }
        }

        if (!data.setExplosivesExpert(civId, target.getUUID(), allowed, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Player is already " + (allowed ? "" : "not ") + "an explosives expert."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                (allowed ? "Designated " : "Removed ")
                        + target.getGameProfile().getName()
                        + (allowed ? " as an" : " from")
                        + " explosives expert for " + RealCivCommands.civDisplay(data, civId) + "."),
                true);
        return 1;
    }

    public static int civRedstonerShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = RealCivCommands.civOfSource(source, data);
        } else {
            civId = RealCivCommands.resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        int cap = RealCivConfig.maxRedstonersPerCivilization();
        List<UUID> redstoners = data.redstonersSorted(civId);
        source.sendSuccess(() -> Component.literal(
                "Redstoners for " + RealCivCommands.civDisplay(data, civId) + " [" + civId + "]: "
                        + redstoners.size() + "/" + cap),
                false);
        if (cap <= 0) {
            source.sendSuccess(() -> Component.literal(
                    "Role is disabled by server config (civ.maxRedstonersPerCivilization=0)."), false);
        }
        if (redstoners.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No members are designated as redstoners."), false);
            return 1;
        }
        for (UUID redstoner : redstoners) {
            source.sendSuccess(() -> Component.literal("- " + RealCivCommands.playerNameOrShortId(source, redstoner) + " | " + redstoner), false);
        }
        return 1;
    }

    public static int civRedstonerSet(CommandSourceStack source, ServerPlayer target, boolean allowed) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_REDSTONERS)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage redstoners."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!civId.equals(targetCiv)) {
            source.sendFailure(Component.literal(
                    "Target player is not in your civilization. Current civ: " + RealCivCommands.civDisplay(data, targetCiv) + "."));
            return 0;
        }

        int cap = RealCivConfig.maxRedstonersPerCivilization();
        if (allowed) {
            if (cap <= 0) {
                source.sendFailure(Component.literal(
                        "Redstoner role is disabled by server config for all civilizations."));
                return 0;
            }
            if (!data.isRedstoner(civId, target.getUUID())
                    && data.redstonerCount(civId) >= cap) {
                source.sendFailure(Component.literal(
                        "Cannot add more redstoners. Cap reached (" + cap + ")."));
                return 0;
            }
        }

        if (!data.setRedstoner(civId, target.getUUID(), allowed, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Player is already " + (allowed ? "" : "not ") + "a redstoner."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                (allowed ? "Designated " : "Removed ")
                        + target.getGameProfile().getName()
                        + (allowed ? " as a" : " from")
                        + " redstoner for " + RealCivCommands.civDisplay(data, civId) + "."),
                true);
        return 1;
    }
}
