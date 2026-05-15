package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.data.PlotLookup;
import com.realciv.realciv.logic.RealCivUtil;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TownCommands {
    private TownCommands() {
    }

    public static int townInfo(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        int civicChunks = data.countPlotsByClass(civId, LandClass.CIVIC);
        int privateChunks = data.countPlotsByClass(civId, LandClass.PRIVATE);
        long nextTownCost = RealCivCommands.nextTownClaimCostCents(civicChunks);
        int ownedPrivate = data.privatePlotCountForOwner(civId, player.getUUID());
        long nextPrivateCost = RealCivCommands.nextPrivateClaimCostCents(ownedPrivate);
        source.sendSuccess(() -> Component.literal(
                "Town info for " + RealCivCommands.civDisplay(data, civId)
                        + " | CIVIC chunks: " + civicChunks
                        + " | PRIVATE chunks: " + privateChunks
                        + " | civ treasury: " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))
                        + " | next CIVIC claim cost: " + RealCivUtil.formatCredits(nextTownCost)
                        + " | your next PRIVATE claim cost: " + RealCivUtil.formatCredits(nextPrivateCost)),
                false);
        return 1;
    }

    public static int townMap(CommandSourceStack source, int radius)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        String dimension = player.serverLevel().dimension().location().toString();
        long centerX = player.chunkPosition().x;
        long centerZ = player.chunkPosition().z;
        int safeRadius = Math.max(1, Math.min(10, radius));

        source.sendSuccess(() -> Component.literal(
                "Town map " + RealCivCommands.civDisplay(data, civId)
                        + " | dim: " + dimension
                        + " | center chunk: [" + centerX + ", " + centerZ + "]"
                        + " | radius: " + safeRadius),
                false);

        for (long z = centerZ - safeRadius; z <= centerZ + safeRadius; z++) {
            StringBuilder row = new StringBuilder();
            row.append(String.format(Locale.ROOT, "z=%d ", z));
            for (long x = centerX - safeRadius; x <= centerX + safeRadius; x++) {
                char symbol = RealCivCommands.mapSymbolForChunk(data, civId, player.getUUID(), dimension, x, z, centerX, centerZ);
                row.append(symbol);
                if (x < centerX + safeRadius) {
                    row.append(' ');
                }
            }
            String line = row.toString();
            source.sendSuccess(() -> Component.literal(line), false);
        }

        source.sendSuccess(() -> Component.literal(
                "Legend: @=you, C=your town(CIVIC), P=your private, p=other member private, m=your COMMUNITY zoning, x=other civ claim, .=wilderness"),
                false);
        source.sendSuccess(() -> Component.literal(
                "Chunk claiming: mayor uses /realciv town claim (CIVIC), citizens use /realciv plot claim (PRIVATE)."),
                false);
        return 1;
    }

    public static int townClaim(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS)) {
            source.sendFailure(Component.literal("Only leadership/admin can expand CIVIC claims."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        if (!RealCivCommands.ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();

        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing != null && !existing.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk is already claimed by civilization '" + existing.civilizationId() + "'."));
            return 0;
        }
        if (existing != null
                && existing.civilizationId().equals(civId)
                && existing.plot().landClass() == LandClass.CIVIC) {
            source.sendFailure(Component.literal("This chunk is already claimed as CIVIC territory."));
            return 0;
        }

        if (data.countPlotsByClass(civId, LandClass.CIVIC) > 0
                && !RealCivCommands.isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
            source.sendFailure(Component.literal(
                    "CIVIC claims must be adjacent to existing CIVIC territory."));
            return 0;
        }

        int civicChunks = data.countPlotsByClass(civId, LandClass.CIVIC);
        long claimCost = RealCivCommands.nextTownClaimCostCents(civicChunks);
        long treasury = data.civTreasuryCents(civId);
        if (treasury < claimCost) {
            source.sendFailure(Component.literal(
                    "Civ treasury has " + RealCivUtil.formatCredits(treasury)
                            + ", need " + RealCivUtil.formatCredits(claimCost) + " for this claim."));
            return 0;
        }

        if (existing != null && !existing.civilizationId().equals(civId) && source.hasPermission(3)) {
            data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        }

        data.addCivTreasuryCents(civId, -claimCost);
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.CIVIC, null, now, 0L);
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " claimed CIVIC chunk " + dimension + "[" + chunkX + "," + chunkZ + "]"
                        + " for " + RealCivUtil.formatCredits(claimCost),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "CIVIC chunk claimed at [" + chunkX + ", " + chunkZ + "] in " + RealCivCommands.civDisplay(data, civId)
                        + ". Civ treasury: " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))),
                true);
        return 1;
    }

    public static int townUnclaim(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS)) {
            source.sendFailure(Component.literal("Only leadership/admin can unclaim CIVIC territory."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing == null) {
            source.sendFailure(Component.literal("This chunk is not claimed."));
            return 0;
        }
        if (!existing.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("You cannot unclaim another civilization's chunk."));
            return 0;
        }
        if (existing.plot().landClass() == LandClass.PRIVATE && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "Use /realciv plot unclaim on PRIVATE plots, or admin override."));
            return 0;
        }

        String civIdRefund = existing.civilizationId();
        long refundAmount = RealCivConfig.townClaimCostCents();
        data.addCivTreasuryCents(civIdRefund, refundAmount);

        data.clearPlot(civIdRefund, dimension, chunkX, chunkZ);
        data.addAuditLog(
                civIdRefund,
                RealCivCommands.actorName(source) + " unclaimed CIVIC chunk " + dimension + "[" + chunkX + "," + chunkZ + "]."
                        + " Refunded " + RealCivUtil.formatCredits(refundAmount) + " to civ treasury.",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "CIVIC chunk unclaimed at [" + chunkX + ", " + chunkZ + "]."
                        + " Refunded " + RealCivUtil.formatCredits(refundAmount) + " to civ treasury."), true);
        return 1;
    }

    public static int townAllotPrivate(CommandSourceStack source, ServerPlayer target, int days)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS)) {
            source.sendFailure(Component.literal("Only leadership/admin can allot PRIVATE plots."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!targetCiv.equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("Target player must belong to your civilization."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        if (!RealCivCommands.ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();
        long paidTicks = Math.max(1L, days) * 24_000L;

        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing == null || !existing.civilizationId().equals(civId)) {
            source.sendFailure(Component.literal(
                    "Mayor allotment must be on a chunk already claimed by your civilization."));
            return 0;
        }
        if (existing.plot().landClass() == LandClass.CIVIC || existing.plot().landClass() == LandClass.COMMUNITY) {
            data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, target.getUUID(), now, paidTicks);
            data.addAuditLog(
                    civId,
                    RealCivCommands.actorName(source) + " allotted PRIVATE plot " + dimension + "[" + chunkX + "," + chunkZ + "]"
                            + " to " + target.getGameProfile().getName(),
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            data.setDirty();
            source.sendSuccess(() -> Component.literal(
                    "Allotted PRIVATE chunk [" + chunkX + ", " + chunkZ + "] to "
                            + target.getGameProfile().getName() + "."), true);
            return 1;
        }

        if (existing.plot().landClass() == LandClass.PRIVATE && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("This chunk is already a PRIVATE plot."));
            return 0;
        }
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, target.getUUID(), now, paidTicks);
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "Reassigned PRIVATE chunk [" + chunkX + ", " + chunkZ + "] to "
                        + target.getGameProfile().getName() + "."), true);
        return 1;
    }
}
