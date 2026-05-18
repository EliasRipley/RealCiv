package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class CreditCommands {
    private CreditCommands() {
    }

    public static int creditAdd(CommandSourceStack source, ServerPlayer player, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        long cents = RealCivUtil.creditsToCents(amount);
        long applied = record.addSocialCreditCents(civId, cents);
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " added " + RealCivUtil.formatCredits(applied)
                        + " contribution karma to " + player.getGameProfile().getName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String capSuffix = "";
        if (cents > 0 && applied < cents) {
            capSuffix = " (daily gain cap applied)";
        }
        final String finalCapSuffix = capSuffix;
        source.sendSuccess(() -> Component.literal(
                "Added " + RealCivUtil.formatCredits(applied)
                        + " credits to " + player.getGameProfile().getName()
                        + " in " + RealCivCommands.civDisplay(data, civId)
                        + ". New balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + finalCapSuffix),
                true);
        return 1;
    }

    public static int creditReduce(CommandSourceStack source, ServerPlayer player, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        long cents = RealCivUtil.creditsToCents(amount);
        long before = record.socialCreditCents(civId);
        long applied = record.addSocialCreditCents(civId, -cents);
        long removed = Math.max(0L, before - record.socialCreditCents(civId));
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " reduced contribution karma of "
                        + player.getGameProfile().getName()
                        + " by " + RealCivUtil.formatCredits(removed),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String clampSuffix = "";
        if (applied != -cents) {
            clampSuffix = " (clamped at 0)";
        }
        final String finalClampSuffix = clampSuffix;
        source.sendSuccess(() -> Component.literal(
                "Reduced " + player.getGameProfile().getName()
                        + " by " + RealCivUtil.formatCredits(removed)
                        + " in " + RealCivCommands.civDisplay(data, civId)
                        + ". New balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + finalClampSuffix),
                true);
        return 1;
    }

    public static int creditSet(CommandSourceStack source, ServerPlayer player, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        long cents = RealCivUtil.creditsToCents(amount);
        record.setSocialCreditCents(civId, cents);
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " set contribution karma of " + player.getGameProfile().getName()
                        + " to " + RealCivUtil.formatCredits(cents),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName()
                        + " balance in " + RealCivCommands.civDisplay(data, civId)
                        + " to " + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."),
                true);
        return 1;
    }

    public static int collectiveAdd(CommandSourceStack source, String civRef, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        @Nullable String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        long cents = RealCivUtil.creditsToCents(amount);
        long before = data.civTreasuryCents(civId);
        data.addCivTreasuryCents(civId, cents);
        long after = data.civTreasuryCents(civId);
        long applied = Math.max(0L, after - before);

        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " added "
                        + RealCivUtil.formatCredits(applied)
                        + " to civ collective contribution karma",
                RealCivConfig.MAX_AUDIT_LOGS.get());

        source.sendSuccess(() -> Component.literal(
                "Added " + RealCivUtil.formatCredits(applied)
                        + " to " + RealCivCommands.civDisplay(data, civId)
                        + ". New collective contribution karma: " + RealCivUtil.formatCredits(after) + "."),
                true);
        return 1;
    }

    public static int collectiveReduce(CommandSourceStack source, String civRef, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        @Nullable String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        long cents = RealCivUtil.creditsToCents(amount);
        long before = data.civTreasuryCents(civId);
        data.addCivTreasuryCents(civId, -cents);
        long after = data.civTreasuryCents(civId);
        long removed = Math.max(0L, before - after);

        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " reduced civ collective contribution karma by "
                        + RealCivUtil.formatCredits(removed),
                RealCivConfig.MAX_AUDIT_LOGS.get());

        String clampSuffix = removed < cents ? " (clamped at 0)" : "";
        source.sendSuccess(() -> Component.literal(
                "Reduced " + RealCivCommands.civDisplay(data, civId)
                        + " by " + RealCivUtil.formatCredits(removed)
                        + ". New collective contribution karma: " + RealCivUtil.formatCredits(after) + "."
                        + clampSuffix),
                true);
        return 1;
    }

    public static int collectiveSet(CommandSourceStack source, String civRef, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        @Nullable String civId = RealCivCommands.resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        long target = RealCivUtil.creditsToCents(amount);
        long before = data.civTreasuryCents(civId);
        data.addCivTreasuryCents(civId, target - before);
        long after = data.civTreasuryCents(civId);

        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " set civ collective contribution karma to "
                        + RealCivUtil.formatCredits(after),
                RealCivConfig.MAX_AUDIT_LOGS.get());

        source.sendSuccess(() -> Component.literal(
                "Set " + RealCivCommands.civDisplay(data, civId)
                        + " collective contribution karma to " + RealCivUtil.formatCredits(after) + "."),
                true);
        return 1;
    }
}
