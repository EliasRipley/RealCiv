package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class TaxCommands {
    private TaxCommands() {
    }

    public static int taxStatus(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        showTaxStatus(source, actor);
        return 1;
    }

    public static int taxStatusFor(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!actor.getUUID().equals(target.getUUID())
                && (!actorCiv.equals(targetCiv)
                || !RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP))) {
            source.sendFailure(Component.literal("Only leadership/admin can inspect another member's tax status."));
            return 0;
        }
        showTaxStatus(source, target);
        return 1;
    }

    public static void showTaxStatus(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        boolean isKarma = data.isKarmaTax(civId);
        ResourceLocation taxItemId = data.taxItemId(civId);

        int ownedPlots = data.privatePlotCountForOwner(civId, target.getUUID());
        int delinquentPlots = data.delinquentPrivatePlotCountForOwner(civId, target.getUUID());
        long nextUpkeepTick = data.earliestPrivatePlotUpkeepTick(civId, target.getUUID());
        long cycleCost = data.upkeepCostPerPlotCents(civId) * ownedPlots;
        long cycleItemCost = data.taxItemCostPerPlotCurrentRate(civId) * ownedPlots;

        source.sendSuccess(() -> Component.literal(
                "Tax status for " + target.getGameProfile().getName() + " in " + RealCivCommands.civDisplay(data, civId) + ":"), false);
        source.sendSuccess(() -> Component.literal(
                "Private plots: " + ownedPlots + " | Delinquent: " + delinquentPlots + " | Next upkeep tick: " + nextUpkeepTick), false);
        if (isKarma) {
            source.sendSuccess(() -> Component.literal(
                    "Mode: karma | Cycle cost: " + RealCivUtil.formatCredits(cycleCost)
                            + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                            + " | Civ collective contribution karma: " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))),
                    false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Mode: item | Cycle cost: " + cycleItemCost + "x " + taxItemId
                            + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                            + " | Civ collective contribution karma: " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))),
                    false);
        }
    }

    public static int taxPay(CommandSourceStack source, int cycles)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean isKarma = data.isKarmaTax(civId);
        ResourceLocation taxItemId = data.taxItemId(civId);
        int safeCycles = Math.max(1, cycles);
        int ownedPlots = data.privatePlotCountForOwner(civId, player.getUUID());
        if (ownedPlots <= 0) {
            source.sendFailure(Component.literal(
                    "You do not own private plots in " + RealCivCommands.civDisplay(data, civId) + "."));
            return 0;
        }

        long cycleCost = data.upkeepCostPerPlotCents(civId) * ownedPlots;
        long cycleItemCost = data.taxItemCostPerPlotCurrentRate(civId) * ownedPlots;
        long totalCost = cycleCost * safeCycles;
        long totalItemCost = cycleItemCost * safeCycles;
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (isKarma) {
            if (record.socialCreditCents(civId) < totalCost) {
                source.sendFailure(Component.literal(
                        "Insufficient contribution karma. Need " + RealCivUtil.formatCredits(totalCost)
                                + ", you have " + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."));
                return 0;
            }
        } else {
            Item taxItem = BuiltInRegistries.ITEM.getOptional(taxItemId).orElse(Items.AIR);
            if (taxItem == Items.AIR) {
                source.sendFailure(Component.literal(
                        "Tax item is invalid for " + RealCivCommands.civDisplay(data, civId) + "."));
                return 0;
            }
            long available = RealCivCommands.countInventoryItem(player, taxItem);
            if (available < totalItemCost) {
                source.sendFailure(Component.literal(
                        "Insufficient tax items. Need " + totalItemCost + "x " + taxItemId
                                + ", you have " + available + "."));
                return 0;
            }
        }

        long now = source.getServer().overworld().getGameTime();
        int affected = data.prepayPrivatePlotUpkeep(civId, player.getUUID(), safeCycles, now, RealCivCommands.actorName(source));
        if (affected <= 0) {
            source.sendFailure(Component.literal("No private plots were eligible for upkeep prepayment."));
            return 0;
        }

        if (isKarma) {
            record.addSocialCreditCents(civId, -totalCost);
            data.addCivTreasuryCents(civId, totalCost);
        } else {
            Item taxItem = BuiltInRegistries.ITEM.getOptional(taxItemId).orElse(Items.AIR);
            RealCivCommands.removeInventoryItem(player, taxItem, totalItemCost);
            data.addToHubStock(civId, taxItemId, totalItemCost, RealCivCommands.actorName(source));
        }
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " paid upkeep tax "
                        + (isKarma
                        ? RealCivUtil.formatCredits(totalCost) + " karma"
                        : totalItemCost + "x " + taxItemId)
                        + " for " + affected + " private plot(s) across " + safeCycles + " cycle(s).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        if (isKarma) {
            source.sendSuccess(() -> Component.literal(
                    "Paid " + RealCivUtil.formatCredits(totalCost)
                            + " upkeep tax for " + affected + " private plot(s). New balance: "
                            + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                            + " | Civ collective contribution karma: " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))),
                    true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Paid " + totalItemCost + "x " + taxItemId
                            + " upkeep tax for " + affected + " private plot(s)."),
                    true);
        }
        return 1;
    }
}
