package com.realciv.realciv.command;

import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class MayorCommands {
    private MayorCommands() {
    }

    public static int mayorSet(CommandSourceStack source, ServerPlayer player, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveMayorCivId(source, data, civRaw);
        data.setMayor(civId, player.getUUID(), RealCivCommands.actorName(source));
        RealCivCommands.grantMayorStarterHub(player);
        String title = data.leaderTitle(civId);
        source.sendSuccess(
                () -> Component.literal(title + " for " + RealCivCommands.civDisplay(data, civId)
                        + " set to " + player.getGameProfile().getName() + "."),
                true);
        return 1;
    }

    public static int mayorClear(CommandSourceStack source, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveMayorCivId(source, data, civRaw);
        data.setMayor(civId, null, RealCivCommands.actorName(source));
        String title = data.leaderTitle(civId);
        source.sendSuccess(() -> Component.literal(
                title + " assignment cleared for " + RealCivCommands.civDisplay(data, civId) + "."), true);
        return 1;
    }

    public static int mayorShow(CommandSourceStack source, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.resolveMayorCivId(source, data, civRaw);
        String title = data.leaderTitle(civId);
        UUID mayor = data.getMayorId(civId);
        if (mayor == null) {
            source.sendSuccess(() -> Component.literal(
                    "No " + title.toLowerCase(Locale.ROOT) + " is assigned for " + RealCivCommands.civDisplay(data, civId) + "."), false);
            return 1;
        }

        ServerPlayer online = source.getServer().getPlayerList().getPlayer(mayor);
        String name = online == null ? mayor.toString() : online.getGameProfile().getName();
        source.sendSuccess(() -> Component.literal(
                "Current " + title.toLowerCase(Locale.ROOT) + " for " + RealCivCommands.civDisplay(data, civId) + ": " + name), false);
        return 1;
    }

    public static int mayorWithdrawRateShow(CommandSourceStack source, ServerPlayer player) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES)) {
            source.sendFailure(Component.literal("Only leadership/admin can view per-player withdraw rates."));
            return 0;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        String rateText = RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId));
        String mode = record.personalWithdrawRatioOverride(civId) == null ? "default" : "override";
        source.sendSuccess(() -> Component.literal(
                "Withdrawal rate for " + player.getGameProfile().getName()
                        + " in " + RealCivCommands.civDisplay(data, civId) + ": " + rateText + " (" + mode + ")"),
                false);
        if (data.civicAttribute(civId, AttributeCategory.RESOURCE) == CivicAttribute.RATIONED) {
            source.sendSuccess(() -> Component.literal(
                    "Note: hub distribution mode is daily_allowance, so withdraw rate does not currently apply."),
                    false);
        }
        return 1;
    }

    public static int mayorWithdrawRateSet(CommandSourceStack source, ServerPlayer player, double percent) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES)) {
            source.sendFailure(Component.literal("Only leadership/admin can set per-player withdraw rates."));
            return 0;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        double ratio = Math.max(0.0D, Math.min(1.0D, percent / 100.0D));
        record.setPersonalWithdrawRatioOverride(civId, ratio);
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " set personal withdraw rate for " + player.getGameProfile().getName()
                        + " to " + RealCivUtil.formatPercentFromRatio(ratio),
                com.realciv.realciv.config.RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Set withdrawal rate for " + player.getGameProfile().getName()
                        + " in " + RealCivCommands.civDisplay(data, civId)
                        + " to " + RealCivUtil.formatPercentFromRatio(ratio) + "."), true);
        if (data.civicAttribute(civId, AttributeCategory.RESOURCE) == CivicAttribute.RATIONED) {
            source.sendSuccess(() -> Component.literal(
                    "Current hub mode is daily_allowance; this rate will apply if switched back to contribution_ratio."),
                    false);
        }
        return 1;
    }

    public static int mayorWithdrawRateClear(CommandSourceStack source, ServerPlayer player) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear per-player withdraw rates."));
            return 0;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        record.setPersonalWithdrawRatioOverride(civId, null);
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " cleared personal withdraw rate override for "
                        + player.getGameProfile().getName(),
                com.realciv.realciv.config.RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Cleared withdrawal rate override for " + player.getGameProfile().getName()
                        + " in " + RealCivCommands.civDisplay(data, civId) + "."), true);
        if (data.civicAttribute(civId, AttributeCategory.RESOURCE) == CivicAttribute.RATIONED) {
            source.sendSuccess(() -> Component.literal(
                    "Current hub mode is daily_allowance; withdraw rates are inactive until contribution_ratio mode is used."),
                    false);
        }
        return 1;
    }

    public static int mayorApprovalSet(CommandSourceStack source, ServerPlayer player, boolean approved) {
        CivSavedData data = CivSavedData.get(source.getServer());
        data.setFounderApproved(player.getUUID(), approved, RealCivCommands.actorName(source));
        source.sendSuccess(() -> Component.literal(
                (approved ? "Approved " : "Revoked approval for ")
                        + player.getGameProfile().getName()
                        + (approved ? " to found a civilization." : " as civilization founder.")),
                true);
        return 1;
    }

    public static int mayorApprovalList(CommandSourceStack source) {
        CivSavedData data = CivSavedData.get(source.getServer());
        List<UUID> approved = data.founderApprovalsSorted();
        if (approved.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No founder approvals are currently set."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Founder approvals:"), false);
        for (UUID id : approved) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
            String label = online != null ? online.getGameProfile().getName() + " (" + id + ")" : id.toString();
            source.sendSuccess(() -> Component.literal("- " + label), false);
        }
        return 1;
    }
}
