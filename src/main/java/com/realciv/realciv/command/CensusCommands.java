package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CensusCommands {
    private CensusCommands() {
    }

    public static int censusMembers(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        List<UUID> members = data.civilizationMembersSorted(civId);
        if (members.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No members are registered in your civilization."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (members.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(members.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Census members for " + RealCivCommands.civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"),
                false);
        for (int i = start; i < end; i++) {
            UUID memberId = members.get(i);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(memberId);
            String name = online == null ? memberId.toString() : online.getGameProfile().getName();
            String role = data.isMayor(civId, memberId)
                    ? "MAYOR"
                    : (data.isCivicManager(civId, memberId) ? "MANAGER" : "CITIZEN");
            source.sendSuccess(() -> Component.literal("- " + name + " | " + role + " | " + memberId), false);
        }
        return 1;
    }

    public static int censusRequests(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can view join requests."));
            return 0;
        }

        List<UUID> requests = data.joinRequestsSorted(civId);
        if (requests.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No join requests are pending."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (requests.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(requests.size(), start + pageSize);
        source.sendSuccess(() -> Component.literal(
                "Join requests for " + RealCivCommands.civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);

        for (int i = start; i < end; i++) {
            UUID id = requests.get(i);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
            String name = online == null ? id.toString() : online.getGameProfile().getName();
            source.sendSuccess(() -> Component.literal("- " + name + " | " + id), false);
        }
        return 1;
    }

    public static int censusInvites(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can view invitations."));
            return 0;
        }

        List<UUID> invites = data.invitedPlayersSorted(civId);
        if (invites.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No invitations are pending."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (invites.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(invites.size(), start + pageSize);
        source.sendSuccess(() -> Component.literal(
                "Invitations for " + RealCivCommands.civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);

        for (int i = start; i < end; i++) {
            UUID id = invites.get(i);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
            String name = online == null ? id.toString() : online.getGameProfile().getName();
            source.sendSuccess(() -> Component.literal("- " + name + " | " + id), false);
        }
        return 1;
    }

    public static int censusInvitePlayer(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can invite players."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (targetCiv.equals(civId)) {
            source.sendFailure(Component.literal(target.getGameProfile().getName() + " is already in your civilization."));
            return 0;
        }
        if (!data.addInvite(civId, target.getUUID(), RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Invite already exists for that player."));
            return 0;
        }
        target.sendSystemMessage(Component.literal(
                "You have been invited to join " + RealCivCommands.civDisplay(data, civId)
                        + ". Use /realciv civ join " + civId + " to accept."));
        source.sendSuccess(() -> Component.literal(
                "Invited " + target.getGameProfile().getName() + " to " + RealCivCommands.civDisplay(data, civId) + "."), true);
        return 1;
    }

    public static int censusUninvitePlayer(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can revoke invites."));
            return 0;
        }
        if (!data.removeInvite(civId, target.getUUID(), RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No invite found for that player."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Revoked invite for " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    public static int censusApproveRequest(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can approve join requests."));
            return 0;
        }
        if (!data.hasJoinRequest(civId, target.getUUID()) && !data.hasInvite(civId, target.getUUID())) {
            source.sendFailure(Component.literal("No pending request/invite for that player."));
            return 0;
        }
        if (!data.setPlayerCivilization(target.getUUID(), civId, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("Failed to approve join."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Approved " + target.getGameProfile().getName() + " into " + RealCivCommands.civDisplay(data, civId) + "."), true);
        target.sendSystemMessage(Component.literal(
                "Your membership in " + RealCivCommands.civDisplay(data, civId) + " was approved."));
        return 1;
    }

    public static int censusDenyRequest(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can deny join requests."));
            return 0;
        }
        boolean removedRequest = data.removeJoinRequest(civId, target.getUUID(), RealCivCommands.actorName(source));
        boolean removedInvite = data.removeInvite(civId, target.getUUID(), RealCivCommands.actorName(source));
        if (!removedRequest && !removedInvite) {
            source.sendFailure(Component.literal("No pending request/invite for that player."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Denied/cleared pending join state for " + target.getGameProfile().getName() + "."), true);
        target.sendSystemMessage(Component.literal(
                "Your join request/invite for " + RealCivCommands.civDisplay(data, civId) + " was declined or revoked."));
        return 1;
    }

    public static int censusRemoveMember(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_POLICE_MEMBERS)) {
            source.sendFailure(Component.literal("Only leadership/admin can remove members."));
            return 0;
        }
        if (actor.getUUID().equals(target.getUUID()) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("Use /realciv civ leave if you want to leave your own civilization."));
            return 0;
        }
        if (!data.removeMemberToDefault(civId, target.getUUID(), RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("That player is not a member of your civilization."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Removed " + target.getGameProfile().getName() + " from " + RealCivCommands.civDisplay(data, civId) + "."), true);
        target.sendSystemMessage(Component.literal(
                "You were removed from " + RealCivCommands.civDisplay(data, civId) + "."));
        return 1;
    }

    public static int censusManagerSet(CommandSourceStack source, ServerPlayer target, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS_ROLES)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage census roles."));
            return 0;
        }

        data.setCivicManager(civId, target.getUUID(), allowed, RealCivCommands.actorName(source));
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Assigned " : "Removed ")
                        + target.getGameProfile().getName() + " as civic manager in "
                        + RealCivCommands.civDisplay(data, civId) + "."), true);
        return 1;
    }

    public static int censusMayorSet(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LEADERSHIP)) {
            source.sendFailure(Component.literal("Only leadership/admin can set mayor through census controls."));
            return 0;
        }

        data.setMayor(civId, target.getUUID(), RealCivCommands.actorName(source));
        RealCivCommands.grantMayorStarterHub(target);
        String title = data.leaderTitle(civId);
        source.sendSuccess(() -> Component.literal(
                "Set " + title.toLowerCase(Locale.ROOT) + " for " + RealCivCommands.civDisplay(data, civId)
                        + " to " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    public static int censusMayorClear(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LEADERSHIP)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear mayor through census controls."));
            return 0;
        }
        data.setMayor(civId, null, RealCivCommands.actorName(source));
        String title = data.leaderTitle(civId);
        source.sendSuccess(() -> Component.literal(
                title + " assignment cleared for " + RealCivCommands.civDisplay(data, civId) + "."), true);
        return 1;
    }
}
