package com.realciv.realciv.command;

import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.data.PlotLookup;
import com.realciv.realciv.data.PlotRecord;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.integration.RealCivFTBChunksBridge;
import com.realciv.realciv.logic.LandWandService;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class LandCommands {
    private LandCommands() {
    }

    public static int showLandInfo(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());

        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();

        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup == null) {
            String wildernessRule = RealCivConfig.blockUnclaimedBuilding()
                    ? "break denied by config, building denied"
                    : "break allowed, building denied";
            source.sendSuccess(() -> Component.literal(
                    "Chunk [" + chunkX + ", " + chunkZ + "] in " + dimension
                            + " is wilderness/unzoned (" + wildernessRule + ")."), false);
            return 1;
        }

        PlotRecord plot = lookup.plot();
        String owner = plot.ownerId() == null ? "none" : plot.ownerId().toString();
        long ticksToUpkeep = Math.max(0L, plot.nextUpkeepTick() - now);
        double daysToUpkeep = ticksToUpkeep / 24_000.0D;
        String delinquent = plot.delinquentSinceTick() < 0L ? "no" : "yes (since tick " + plot.delinquentSinceTick() + ")";
        boolean canBuild = data.canBuildOnPlot(lookup.civilizationId(), plot, player.getUUID(), RealCivUtil.isBypass(player));
        boolean canBreak = data.canBreakOnPlot(lookup.civilizationId(), plot, player.getUUID(), RealCivUtil.isBypass(player));

        source.sendSuccess(() -> Component.literal(
                "Chunk [" + chunkX + ", " + chunkZ + "] in " + dimension
                        + " | Civ: " + RealCivCommands.civDisplay(data, lookup.civilizationId()) + " [" + lookup.civilizationId() + "]"
                        + " | Class: " + plot.landClass().name()
                        + " | Owner: " + owner),
                false);
        source.sendSuccess(() -> Component.literal(
                "Upkeep in ~" + String.format(Locale.ROOT, "%.2f", daysToUpkeep)
                        + " days | Delinquent: " + delinquent
                        + " | You can build: " + canBuild + " | You can break: " + canBreak),
                false);
        return 1;
    }

    public static int landZoneCurrentPlot(
            CommandSourceStack source,
            String landClassRaw,
            @Nullable ServerPlayer owner,
            int prepayDays) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CivSavedData data = CivSavedData.get(source.getServer());
        ServerPlayer actor = source.getPlayerOrException();
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
            source.sendFailure(Component.literal("Only leadership/admin can zone plots for this civilization."));
            return 0;
        }

        LandClass landClass = LandClass.fromConfig(landClassRaw);
        if (landClass == null) {
            source.sendFailure(Component.literal("Invalid land class. Use: community, civic, private (public also works)."));
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
        if (existing != null && !existing.civilizationId().equals(actorCiv) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk belongs to another civilization (" + existing.civilizationId()
                            + "). Admin privileges required to override."));
            return 0;
        }
        if (existing != null && !existing.civilizationId().equals(actorCiv) && source.hasPermission(3)) {
            data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        }

        UUID ownerId = null;
        long paidTicks = 0L;
        if (landClass == LandClass.PRIVATE) {
            ownerId = owner == null ? actor.getUUID() : owner.getUUID();
            paidTicks = Math.max(1L, prepayDays) * 24_000L;
        }

        data.setPlot(actorCiv, dimension, chunkX, chunkZ, landClass, ownerId, now, paidTicks);
        data.addAuditLog(
                actorCiv,
                RealCivCommands.actorName(source) + " zoned " + dimension + "[" + chunkX + "," + chunkZ + "] as " + landClass
                        + (ownerId == null ? "" : " owner=" + ownerId),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String ownerText = ownerId == null ? "none" : ownerId.toString();
        source.sendSuccess(() -> Component.literal(
                "Zoned chunk [" + chunkX + ", " + chunkZ + "] in " + RealCivCommands.civDisplay(data, actorCiv)
                        + " as " + landClass + " | owner: " + ownerText),
                true);
        return 1;
    }

    public static int landRevokeCurrentPlot(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CivSavedData data = CivSavedData.get(source.getServer());
        ServerPlayer actor = source.getPlayerOrException();
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear plot zoning."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing == null) {
            source.sendFailure(Component.literal("This chunk is already unzoned."));
            return 0;
        }
        if (!existing.civilizationId().equals(actorCiv) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk belongs to civilization '" + existing.civilizationId()
                            + "'. Admin privileges required to clear it."));
            return 0;
        }

        data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        data.addAuditLog(
                existing.civilizationId(),
                RealCivCommands.actorName(source) + " cleared zoning at " + dimension + "[" + chunkX + "," + chunkZ + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "Cleared zoning for chunk [" + chunkX + ", " + chunkZ + "] in " + dimension + "."), true);
        return 1;
    }

    public static int landManagerSet(CommandSourceStack source, ServerPlayer target, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_MANAGERS)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage civic managers."));
            return 0;
        }

        data.setCivicManager(civId, target.getUUID(), allowed, RealCivCommands.actorName(source));
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Added " : "Removed ")
                        + target.getGameProfile().getName() + " as civic manager for "
                        + RealCivCommands.civDisplay(data, civId) + "."), true);
        return 1;
    }

    public static int landWandGive(CommandSourceStack source, ServerPlayer target) {
        ItemStack wand = new ItemStack(ModBlocks.LAND_WAND.get(), 1);
        boolean added = target.getInventory().add(wand);
        if (!added) {
            target.drop(wand, false);
        }
        source.sendSuccess(() -> Component.literal(
                "Granted Land Wand to " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    public static int landSelectionInfo(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        @Nullable LandWandService.ChunkSelection selection = LandWandService.selectionForCurrentDimension(player);
        if (selection == null) {
            source.sendFailure(Component.literal(
                    "No complete wand selection in this dimension. Use Land Wand left-click for pos1 and right-click for pos2."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Land selection: dimension=" + selection.dimension()
                        + " | X " + selection.minChunkX() + ".." + selection.maxChunkX()
                        + " | Z " + selection.minChunkZ() + ".." + selection.maxChunkZ()
                        + " | chunks=" + selection.chunkCount()), false);
        source.sendSuccess(() -> Component.literal(
                "Configured selection max: " + RealCivConfig.landWandMaxSelectionChunks() + " chunk(s)."), false);
        return 1;
    }

    public static int landSelectionClear(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandWandService.clearSelection(player);
        source.sendSuccess(() -> Component.literal("Land wand selection cleared."), false);
        return 1;
    }

    public static int landVisualize(CommandSourceStack source, int radius)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        int safeRadius = Math.max(1, Math.min(64, radius));
        int boundaryLines = LandWandService.visualizeNearbyPlots(player, data, safeRadius);
        int selectionLines = LandWandService.visualizeSelection(player);
        source.sendSuccess(() -> Component.literal(
                "Visualized " + boundaryLines + " land boundary line(s) within " + safeRadius + " chunks"
                        + " (all distinct nearby claim boundaries)."
                        + (selectionLines > 0 ? " Selection boundary lines: " + selectionLines + "." : "")),
                false);
        return 1;
    }

    public static int openLandGui(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        RealCivCommands.openLandGuiForPlayer(player, data);
        return 1;
    }

    public static int landFtbModeShow(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean mayorOrAdmin = RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE);
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        String storedMode = RealCivFTBChunksBridge.normalizeClaimModeOrAuto(record.ftbClaimModeOverride());
        String effectiveMode = RealCivFTBChunksBridge.effectiveClaimModeLabel(mayorOrAdmin, record.ftbClaimModeOverride());

        if (!mayorOrAdmin) {
            source.sendSuccess(() -> Component.literal(
                    "FTB map claim mode: PRIVATE (non-leadership players always claim PRIVATE plots)."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(
                "FTB map claim mode for " + RealCivCommands.civDisplay(data, civId) + ": stored="
                        + storedMode.toUpperCase(Locale.ROOT)
                        + ", effective=" + effectiveMode.toUpperCase(Locale.ROOT)
                        + " (default when AUTO: " + RealCivConfig.ftbMayorDefaultClaimMode().toUpperCase(Locale.ROOT) + ")."),
                false);
        source.sendSuccess(() -> Component.literal(
                "Set with: /realciv land ftb-mode <auto|civic|private>"), false);
        return 1;
    }

    public static int landFtbModeSet(CommandSourceStack source, String rawMode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean mayorOrAdmin = RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE);
        if (!mayorOrAdmin) {
            source.sendFailure(Component.literal(
                    "Only leadership/admin can change FTB map claim mode. Non-leaders always claim PRIVATE plots."));
            return 0;
        }

        String parsed = parseFtbClaimModeArgument(rawMode);
        if (parsed == null) {
            source.sendFailure(Component.literal(
                    "Invalid mode. Use one of: auto, civic, private."));
            return 0;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (RealCivFTBChunksBridge.CLAIM_MODE_AUTO.equals(parsed)) {
            record.setFtbClaimModeOverride(null);
        } else {
            record.setFtbClaimModeOverride(parsed);
        }

        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " set FTB map claim mode override to " + parsed.toUpperCase(Locale.ROOT),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String storedMode = RealCivFTBChunksBridge.normalizeClaimModeOrAuto(record.ftbClaimModeOverride());
        String effectiveMode = RealCivFTBChunksBridge.effectiveClaimModeLabel(true, record.ftbClaimModeOverride());
        source.sendSuccess(() -> Component.literal(
                "FTB map mode updated: stored=" + storedMode.toUpperCase(Locale.ROOT)
                        + ", effective=" + effectiveMode.toUpperCase(Locale.ROOT) + "."),
                true);
        return 1;
    }

    @Nullable
    public static String parseFtbClaimModeArgument(String rawMode) {
        if (rawMode == null) {
            return null;
        }
        String mode = rawMode.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "auto", "default" -> RealCivFTBChunksBridge.CLAIM_MODE_AUTO;
            case "civic", "town" -> RealCivFTBChunksBridge.CLAIM_MODE_CIVIC;
            case "private", "plot" -> RealCivFTBChunksBridge.CLAIM_MODE_PRIVATE;
            default -> null;
        };
    }

    public static int landZoneSelection(
            CommandSourceStack source,
            String landClassRaw,
            @Nullable ServerPlayer owner,
            int prepayDays) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
            source.sendFailure(Component.literal("Only leadership/admin can zone selected areas for this civilization."));
            return 0;
        }

        LandClass landClass = LandClass.fromConfig(landClassRaw);
        if (landClass == null) {
            source.sendFailure(Component.literal("Invalid land class. Use: community, civic, private (public also works)."));
            return 0;
        }

        @Nullable LandWandService.ChunkSelection selection = LandWandService.selectionForCurrentDimension(actor);
        if (selection == null) {
            source.sendFailure(Component.literal(
                    "No complete wand selection in this dimension. Use the Land Wand first."));
            return 0;
        }

        if (selection.chunkCount() > RealCivConfig.landWandMaxSelectionChunks()) {
            source.sendFailure(Component.literal(
                    "Selection is too large (" + selection.chunkCount() + " chunks). Max allowed: "
                            + RealCivConfig.landWandMaxSelectionChunks() + "."));
            return 0;
        }

        long now = source.getServer().overworld().getGameTime();
        String dimension = selection.dimension();
        if (!RealCivCommands.ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        for (long chunkX = selection.minChunkX(); chunkX <= selection.maxChunkX(); chunkX++) {
            for (long chunkZ = selection.minChunkZ(); chunkZ <= selection.maxChunkZ(); chunkZ++) {
                @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
                if (existing != null
                        && !existing.civilizationId().equals(actorCiv)
                        && !source.hasPermission(3)) {
                    source.sendFailure(Component.literal(
                            "Selection contains chunk [" + chunkX + ", " + chunkZ + "] already zoned by civilization '"
                                    + existing.civilizationId() + "'. Admin privileges required to override."));
                    return 0;
                }
            }
        }

        UUID ownerId = null;
        long paidTicks = 0L;
        if (landClass == LandClass.PRIVATE) {
            ownerId = owner == null ? actor.getUUID() : owner.getUUID();
            paidTicks = Math.max(1L, prepayDays) * 24_000L;
        }

        int affected = 0;
        for (long chunkX = selection.minChunkX(); chunkX <= selection.maxChunkX(); chunkX++) {
            for (long chunkZ = selection.minChunkZ(); chunkZ <= selection.maxChunkZ(); chunkZ++) {
                @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
                if (existing != null && !existing.civilizationId().equals(actorCiv) && source.hasPermission(3)) {
                    data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
                }
                data.setPlot(actorCiv, dimension, chunkX, chunkZ, landClass, ownerId, now, paidTicks);
                affected++;
            }
        }

        data.addAuditLog(
                actorCiv,
                RealCivCommands.actorName(source) + " zoned selection " + dimension
                        + " X[" + selection.minChunkX() + ".." + selection.maxChunkX() + "]"
                        + " Z[" + selection.minChunkZ() + ".." + selection.maxChunkZ() + "] as " + landClass
                        + (ownerId == null ? "" : " owner=" + ownerId),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String ownerText = ownerId == null ? "none" : ownerId.toString();
        final int finalAffected = affected;
        final String zonedMessage =
                "Zoned " + finalAffected + " chunk(s) in " + RealCivCommands.civDisplay(data, actorCiv)
                        + " as " + landClass + " | owner: " + ownerText;
        source.sendSuccess(() -> Component.literal(zonedMessage), true);
        return 1;
    }

    public static int landClearSelection(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear selected land zoning."));
            return 0;
        }

        @Nullable LandWandService.ChunkSelection selection = LandWandService.selectionForCurrentDimension(actor);
        if (selection == null) {
            source.sendFailure(Component.literal(
                    "No complete wand selection in this dimension. Use the Land Wand first."));
            return 0;
        }

        if (selection.chunkCount() > RealCivConfig.landWandMaxSelectionChunks()) {
            source.sendFailure(Component.literal(
                    "Selection is too large (" + selection.chunkCount() + " chunks). Max allowed: "
                            + RealCivConfig.landWandMaxSelectionChunks() + "."));
            return 0;
        }

        int cleared = 0;
        int skipped = 0;
        for (long chunkX = selection.minChunkX(); chunkX <= selection.maxChunkX(); chunkX++) {
            for (long chunkZ = selection.minChunkZ(); chunkZ <= selection.maxChunkZ(); chunkZ++) {
                @Nullable PlotLookup existing = data.getPlotAnyCivilization(selection.dimension(), chunkX, chunkZ);
                if (existing == null) {
                    continue;
                }
                if (!existing.civilizationId().equals(actorCiv) && !source.hasPermission(3)) {
                    skipped++;
                    continue;
                }
                if (data.clearPlot(existing.civilizationId(), selection.dimension(), chunkX, chunkZ)) {
                    data.addAuditLog(
                            existing.civilizationId(),
                            RealCivCommands.actorName(source) + " cleared zoning at " + selection.dimension()
                                    + "[" + chunkX + "," + chunkZ + "]",
                            RealCivConfig.MAX_AUDIT_LOGS.get());
                    cleared++;
                }
            }
        }
        data.setDirty();

        if (cleared == 0) {
            source.sendFailure(Component.literal(
                    "No zoned chunks were cleared in selection."
                            + (skipped > 0 ? " Skipped " + skipped + " chunk(s) owned by other civilizations." : "")));
            return 0;
        }

        final int finalCleared = cleared;
        final int finalSkipped = skipped;
        final String clearedMessage =
                "Cleared zoning for " + finalCleared + " chunk(s)."
                        + (finalSkipped > 0 ? " Skipped " + finalSkipped + " chunk(s) owned by other civilizations." : "");
        source.sendSuccess(() -> Component.literal(clearedMessage), true);
        return 1;
    }
}
