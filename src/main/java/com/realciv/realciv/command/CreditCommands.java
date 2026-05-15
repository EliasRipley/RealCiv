package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
}
