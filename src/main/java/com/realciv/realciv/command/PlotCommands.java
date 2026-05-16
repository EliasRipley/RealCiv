package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.data.PlotLookup;
import com.realciv.realciv.data.PlotRecord;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class PlotCommands {
    private PlotCommands() {
    }

    public static int plotClaimSelf(CommandSourceStack source, int days)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        String dimension = player.serverLevel().dimension().location().toString();
        if (!RealCivCommands.ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();
        long paidTicks = Math.max(1L, days) * 24_000L;

        if (!RealCivCommands.isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
            source.sendFailure(Component.literal(
                    "PRIVATE plots must be adjacent to your civilization's CIVIC territory."));
            return 0;
        }

        CivicAttribute landAttr = data.civicAttribute(civId, AttributeCategory.LAND);
        if (landAttr == CivicAttribute.LEADER_CLAIM && !source.hasPermission(3)
                && !data.isCivicManager(civId, player.getUUID())
                && !player.getUUID().equals(data.getMayorId(civId))) {
            source.sendFailure(Component.literal("Only leadership can claim private plots (Land: Leader Claim)."));
            return 0;
        }

        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing != null && !existing.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("That chunk belongs to another civilization."));
            return 0;
        }
        if (existing != null
                && existing.civilizationId().equals(civId)
                && existing.plot().landClass() == LandClass.PRIVATE
                && existing.plot().ownerId() != null
                && !existing.plot().ownerId().equals(player.getUUID())
                && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("That PRIVATE plot is owned by another player."));
            return 0;
        }
        if (existing != null
                && existing.civilizationId().equals(civId)
                && existing.plot().landClass() == LandClass.CIVIC
                && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk is CIVIC territory. Ask your mayor to allot it with /realciv town allot <player>."));
            return 0;
        }

        int ownedPrivate = data.privatePlotCountForOwner(civId, player.getUUID());
        long cost = RealCivCommands.nextPrivateClaimCostCents(ownedPrivate);
        if (record.socialCreditCents(civId) < cost) {
            source.sendFailure(Component.literal(
                    "You need " + RealCivUtil.formatCredits(cost) + " karma, you have "
                            + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."));
            return 0;
        }

        if (existing != null && !existing.civilizationId().equals(civId) && source.hasPermission(3)) {
            data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        }

        record.addSocialCreditCents(civId, -cost);
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, player.getUUID(), now, paidTicks);
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " claimed PRIVATE plot " + dimension + "[" + chunkX + "," + chunkZ + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "PRIVATE chunk claimed at [" + chunkX + ", " + chunkZ + "]. Cost: "
                        + RealCivUtil.formatCredits(cost)
                        + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))),
                true);
        return 1;
    }

    public static int plotUnclaimSelf(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());

        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing == null) {
            source.sendFailure(Component.literal("This chunk is not claimed."));
            return 0;
        }
        if (!existing.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("You cannot unclaim another civilization's chunk."));
            return 0;
        }
        if (existing.plot().landClass() != LandClass.PRIVATE) {
            source.sendFailure(Component.literal("This is not a PRIVATE plot."));
            return 0;
        }
        if (existing.plot().ownerId() != null
                && !existing.plot().ownerId().equals(player.getUUID())
                && !RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)
                && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("Only owner/leadership/admin can unclaim this PRIVATE plot."));
            return 0;
        }

        String civIdRefund = existing.civilizationId();
        long refundAmount = RealCivConfig.rentCostCents();
        PlayerRecord refundRecord = data.getOrCreatePlayer(player.getUUID());
        refundRecord.addSocialCreditCents(civIdRefund, refundAmount);

        data.clearPlot(civIdRefund, dimension, chunkX, chunkZ);
        data.addAuditLog(
                civIdRefund,
                RealCivCommands.actorName(source) + " unclaimed PRIVATE plot " + dimension + "[" + chunkX + "," + chunkZ + "]."
                        + " Refunded " + RealCivUtil.formatCredits(refundAmount) + " karma to "
                        + player.getGameProfile().getName() + ".",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "PRIVATE plot unclaimed at [" + chunkX + ", " + chunkZ + "]."
                        + " Refunded " + RealCivUtil.formatCredits(refundAmount) + " karma."), true);
        return 1;
    }

    public static int rentCurrentPlot(CommandSourceStack source, int days)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        String dimension = player.serverLevel().dimension().location().toString();
        if (!RealCivCommands.ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();
        long paidTicks = Math.max(1L, days) * 24_000L;

        if (!RealCivCommands.isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
            source.sendFailure(Component.literal(
                    "PRIVATE plots must be adjacent to your civilization's CIVIC territory."));
            return 0;
        }

        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup != null && !lookup.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk is claimed by civilization '" + lookup.civilizationId() + "' and cannot be rented here."));
            return 0;
        }

        int ownedPrivate = data.privatePlotCountForOwner(civId, player.getUUID());
        long rentCost = RealCivCommands.nextPrivateClaimCostCents(ownedPrivate);
        if (record.socialCreditCents(civId) < rentCost) {
            source.sendFailure(Component.literal(
                    "You need " + RealCivUtil.formatCredits(rentCost) + " karma for this plot, you have "
                            + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."));
            return 0;
        }

        if (lookup != null && lookup.civilizationId().equals(civId)) {
            PlotRecord plot = lookup.plot();
            if (plot.landClass() == LandClass.CIVIC && !source.hasPermission(3)) {
                source.sendFailure(Component.literal(
                        "This chunk is CIVIC territory. Ask your mayor to allot it with /realciv town allot <player>."));
                return 0;
            }
            if (plot.landClass() == LandClass.PRIVATE
                    && plot.ownerId() != null
                    && !plot.ownerId().equals(player.getUUID())
                    && !source.hasPermission(3)) {
                source.sendFailure(Component.literal("This PRIVATE plot is owned by another player."));
                return 0;
            }
            if (plot.landClass() == LandClass.PRIVATE && player.getUUID().equals(plot.ownerId())) {
                long next = Math.max(now, plot.nextUpkeepTick()) + paidTicks;
                plot.setOwnerId(player.getUUID());
                plot.setDelinquentSinceTick(-1L);
                plot.setNextUpkeepTick(next);
                record.addSocialCreditCents(civId, -rentCost);
                data.addAuditLog(
                        civId,
                        RealCivCommands.actorName(source) + " renewed PRIVATE plot " + dimension + "[" + chunkX + "," + chunkZ + "]"
                                + " until upkeep tick " + next,
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                data.setDirty();
                source.sendSuccess(() -> Component.literal(
                        "Plot renewed. Cost: " + RealCivUtil.formatCredits(rentCost)
                                + " | Next upkeep tick: " + next + " | Balance: "
                                + RealCivUtil.formatCredits(record.socialCreditCents(civId))),
                        false);
                return 1;
            }
        }

        record.addSocialCreditCents(civId, -rentCost);
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, player.getUUID(), now, paidTicks);
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " rented chunk " + dimension + "[" + chunkX + "," + chunkZ + "] as PRIVATE"
                        + " for " + days + " day(s).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "PRIVATE plot rented in " + RealCivCommands.civDisplay(data, civId)
                        + " at [" + chunkX + ", " + chunkZ + "] for " + days + " day(s). "
                        + "Cost: " + RealCivUtil.formatCredits(rentCost)
                        + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))),
                false);
        return 1;
    }
}
