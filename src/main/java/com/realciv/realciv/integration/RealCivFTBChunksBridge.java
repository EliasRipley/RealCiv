package com.realciv.realciv.integration;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.*;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivUtil;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.OpenClaimGUIPacket;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class RealCivFTBChunksBridge {
    private static final String CLAIM_DENIED_TRANSLATION_KEY = "realciv.ftbchunks.claim_denied";
    private static final String UNCLAIM_DENIED_TRANSLATION_KEY = "realciv.ftbchunks.unclaim_denied";
    public static final String CLAIM_MODE_AUTO = "auto";
    public static final String CLAIM_MODE_CIVIC = "civic";
    public static final String CLAIM_MODE_PRIVATE = "private";

    private static final ThreadLocal<Boolean> INTERNAL_UNCLAIM = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> CLAIM_HANDLED_INTERNALLY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> INTERNAL_SYNC_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static boolean registered;

    private RealCivFTBChunksBridge() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ClaimedChunkEvent.BEFORE_CLAIM.register(RealCivFTBChunksBridge::beforeClaim);
        ClaimedChunkEvent.BEFORE_UNCLAIM.register(RealCivFTBChunksBridge::beforeUnclaim);
        ClaimedChunkEvent.AFTER_CLAIM.register(RealCivFTBChunksBridge::afterClaim);
        ClaimedChunkEvent.AFTER_UNCLAIM.register(RealCivFTBChunksBridge::afterUnclaim);
        registered = true;
        RealCivMod.LOGGER.info("RealCiv FTB Chunks bridge registered.");
    }

    public static boolean tryOpenClaimMap(ServerPlayer player) {
        try {
            Optional<Team> team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player);
            if (team.isEmpty()) {
                player.sendSystemMessage(Component.literal(
                        "FTB claim map context is unavailable for your current team state. "
                                + "Opening the RealCiv land map instead."));
                return false;
            }
            NetworkManager.sendToPlayer(player, new OpenClaimGUIPacket(team.get().getTeamId()));
            return true;
        } catch (Throwable throwable) {
            RealCivMod.LOGGER.warn("Failed to open FTB claim map for {}.", player.getGameProfile().getName(), throwable);
            return false;
        }
    }

    public static String normalizeClaimModeOrAuto(@Nullable String rawMode) {
        if (rawMode == null) {
            return CLAIM_MODE_AUTO;
        }
        String mode = rawMode.trim().toLowerCase(java.util.Locale.ROOT);
        if (mode.isEmpty() || CLAIM_MODE_AUTO.equals(mode)) {
            return CLAIM_MODE_AUTO;
        }
        if (CLAIM_MODE_CIVIC.equals(mode)) {
            return CLAIM_MODE_CIVIC;
        }
        if (CLAIM_MODE_PRIVATE.equals(mode)) {
            return CLAIM_MODE_PRIVATE;
        }
        return CLAIM_MODE_AUTO;
    }

    public static String effectiveClaimModeLabel(boolean mayorOrAdmin, @Nullable String rawOverrideMode) {
        if (!mayorOrAdmin) {
            return CLAIM_MODE_PRIVATE;
        }
        String normalized = normalizeClaimModeOrAuto(rawOverrideMode);
        if (!CLAIM_MODE_AUTO.equals(normalized)) {
            return normalized;
        }
        return RealCivConfig.ftbMayorDefaultClaimMode();
    }

    public static void runInternalSync(Runnable runnable) {
        runInternalSync(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T runInternalSync(Supplier<T> supplier) {
        int previous = INTERNAL_SYNC_DEPTH.get();
        INTERNAL_SYNC_DEPTH.set(previous + 1);
        try {
            return supplier.get();
        } finally {
            INTERNAL_SYNC_DEPTH.set(previous);
        }
    }

    private static boolean isInternalSync() {
        return INTERNAL_SYNC_DEPTH.get() > 0;
    }

    private static CompoundEventResult<ClaimResult> beforeClaim(CommandSourceStack source, ClaimedChunk chunk) {
        if (isInternalSync()) {
            return CompoundEventResult.pass();
        }
        ClaimDecision decision = validateClaim(source, chunk);
        if (decision.allowed()) {
            CLAIM_HANDLED_INTERNALLY.set(true);
            applyClaim(source, decision);
            return CompoundEventResult.interruptFalse(ClaimResult.success());
        }
        sendClaimFailure(source, decision);
        return CompoundEventResult.interruptFalse(ClaimResult.customProblem(CLAIM_DENIED_TRANSLATION_KEY));
    }

    private static CompoundEventResult<ClaimResult> beforeUnclaim(CommandSourceStack source, ClaimedChunk chunk) {
        if (isInternalSync()) {
            return CompoundEventResult.pass();
        }
        if (INTERNAL_UNCLAIM.get()) {
            return CompoundEventResult.pass();
        }
        UnclaimDecision decision = validateUnclaim(source, chunk);
        if (decision.allowed()) {
            return CompoundEventResult.pass();
        }
        sendClaimFailure(source, decision);
        return CompoundEventResult.interruptFalse(ClaimResult.customProblem(UNCLAIM_DENIED_TRANSLATION_KEY));
    }

    private static void afterClaim(CommandSourceStack source, ClaimedChunk chunk) {
        if (isInternalSync()) {
            return;
        }
        if (CLAIM_HANDLED_INTERNALLY.get()) {
            CLAIM_HANDLED_INTERNALLY.set(false);
            return;
        }
        ClaimDecision decision = validateClaim(source, chunk);
        if (!decision.allowed()) {
            sendClaimFailure(source, decision);
            unclaimInternally(chunk, source);
            return;
        }
        applyClaim(source, decision);
    }

    private static void afterUnclaim(CommandSourceStack source, ClaimedChunk chunk) {
        if (isInternalSync()) {
            return;
        }
        if (INTERNAL_UNCLAIM.get()) {
            return;
        }
        if (source.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(source.getServer());
        String dimension = chunk.getPos().dimension().location().toString();
        long chunkX = chunk.getPos().x();
        long chunkZ = chunk.getPos().z();
        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup == null) {
            return;
        }

        LandClass landClass = lookup.plot().landClass();
        UUID ownerId = lookup.plot().ownerId();
        String civId = lookup.civilizationId();

        data.clearPlot(civId, dimension, chunkX, chunkZ);

        if (landClass == LandClass.CIVIC) {
            long refund = RealCivConfig.townClaimCostCents();
            data.addCivTreasuryCents(civId, refund);
            data.addAuditLog(
                    civId,
                    actorName(source) + " unclaimed CIVIC chunk " + dimension + "[" + chunkX + "," + chunkZ
                            + "] via FTB Chunks map. Refunded " + RealCivUtil.formatCredits(refund) + " to civ treasury.",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
        } else if (landClass == LandClass.PRIVATE && ownerId != null) {
            long refund = RealCivConfig.rentCostCents();
            data.getOrCreatePlayer(ownerId).addSocialCreditCents(civId, refund);
            data.addAuditLog(
                    civId,
                    actorName(source) + " unclaimed PRIVATE chunk " + dimension + "[" + chunkX + "," + chunkZ
                            + "] via FTB Chunks map. Refunded " + RealCivUtil.formatCredits(refund) + " to player "
                            + ownerId + ".",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            data.addAuditLog(
                    civId,
                    actorName(source) + " unclaimed chunk " + dimension + "[" + chunkX + "," + chunkZ + "] via FTB Chunks map.",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
        }

        data.setDirty();

        if (source.getEntity() instanceof ServerPlayer player) {
            StringBuilder msg = new StringBuilder();
            msg.append("Chunk unclaimed at [").append(chunkX).append(", ").append(chunkZ).append("].");
            if (landClass == LandClass.CIVIC) {
                long refund = RealCivConfig.townClaimCostCents();
                msg.append(" Refunded ").append(RealCivUtil.formatCredits(refund)).append(" to civ treasury.");
            } else if (landClass == LandClass.PRIVATE && ownerId != null) {
                long refund = RealCivConfig.rentCostCents();
                msg.append(" Refunded ").append(RealCivUtil.formatCredits(refund)).append(" karma to you.");
            }
            player.sendSystemMessage(Component.literal(msg.toString()));
        }
    }

    private static ClaimDecision validateClaim(CommandSourceStack source, ClaimedChunk chunk) {
        if (source.getServer() == null || source.getServer().overworld() == null) {
            return ClaimDecision.denied("Server is not ready for land claim checks.");
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return ClaimDecision.denied("Only players can claim land from the FTB map.");
        }

        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        String dimension = chunk.getPos().dimension().location().toString();
        long chunkX = chunk.getPos().x();
        long chunkZ = chunk.getPos().z();
        long now = source.getServer().overworld().getGameTime();

        if (!source.hasPermission(3) && !RealCivConfig.canClaimDimension(dimension)) {
            return ClaimDecision.denied(
                    "Land claiming is disabled in dimension '" + dimension + "' by server policy "
                            + "(" + RealCivConfig.claimDimensionPolicyLabel() + ").");
        }

        boolean mayorOrAdmin = canManageFtbMode(source, data, civId);
        String mode = effectiveClaimModeLabel(
                mayorOrAdmin,
                data.getOrCreatePlayer(player.getUUID()).ftbClaimModeOverride());
        LandClass targetClass = CLAIM_MODE_PRIVATE.equals(mode) ? LandClass.PRIVATE : LandClass.CIVIC;
        long paidTicks = targetClass == LandClass.PRIVATE ? Math.max(1L, RealCivConfig.LAND_RENT_DAYS.get()) * 24_000L : 0L;
        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);

        if (existing != null && !existing.civilizationId().equals(civId) && !source.hasPermission(3)) {
            return ClaimDecision.denied(
                    "This chunk is already claimed by civilization '" + existing.civilizationId() + "'.");
        }

        if (targetClass == LandClass.CIVIC) {
            if (!canManageTownClaims(source, data, civId)) {
                return ClaimDecision.denied("Only leadership/admin can expand CIVIC claims.");
            }

            int civicChunks = data.countPlotsByClass(civId, LandClass.CIVIC);
            if (civicChunks > 0 && !isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
                return ClaimDecision.denied("CIVIC claims must be adjacent to existing CIVIC territory.");
            }

            long claimCost = nextTownClaimCostCents(civicChunks);
            long treasury = data.civTreasuryCents(civId);
            if (treasury < claimCost) {
                return ClaimDecision.denied(
                        "Civ treasury has " + RealCivUtil.formatCredits(treasury)
                                + ", need " + RealCivUtil.formatCredits(claimCost) + " for this claim.");
            }

            return ClaimDecision.allowed(player, civId, targetClass, dimension, chunkX, chunkZ, now, paidTicks, claimCost);
        }

        if (!isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
            return ClaimDecision.denied("PRIVATE plots must be adjacent to your civilization's CIVIC territory.");
        }

        if (existing != null
                && existing.civilizationId().equals(civId)
                && existing.plot().landClass() == LandClass.PRIVATE
                && existing.plot().ownerId() != null
                && !existing.plot().ownerId().equals(player.getUUID())
                && !source.hasPermission(3)) {
            return ClaimDecision.denied("This PRIVATE plot is already owned by another player.");
        }
        if (existing != null
                && existing.civilizationId().equals(civId)
                && existing.plot().landClass() == LandClass.CIVIC
                && !source.hasPermission(3)) {
            return ClaimDecision.denied(
                    "This chunk is CIVIC territory. Ask your mayor to allot it as a PRIVATE plot.");
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        int ownedPrivate = data.privatePlotCountForOwner(civId, player.getUUID());
        long claimCost = nextPrivateClaimCostCents(ownedPrivate);
        if (record.socialCreditCents(civId) < claimCost) {
            return ClaimDecision.denied(
                    "You need " + RealCivUtil.formatCredits(claimCost) + " karma, you have "
                            + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + ".");
        }

        return ClaimDecision.allowed(player, civId, targetClass, dimension, chunkX, chunkZ, now, paidTicks, claimCost);
    }

    private static UnclaimDecision validateUnclaim(CommandSourceStack source, ClaimedChunk chunk) {
        if (source.getServer() == null) {
            return UnclaimDecision.permit();
        }

        CivSavedData data = CivSavedData.get(source.getServer());
        String dimension = chunk.getPos().dimension().location().toString();
        long chunkX = chunk.getPos().x();
        long chunkZ = chunk.getPos().z();
        @Nullable PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);

        if (existing == null) {
            return UnclaimDecision.permit();
        }
        if (source.hasPermission(3)) {
            return UnclaimDecision.permit();
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return UnclaimDecision.denied("Only players can unclaim land from the FTB map.");
        }
        if (!existing.civilizationId().equals(data.getOrAssignCivilization(player.getUUID()))) {
            return UnclaimDecision.denied("You cannot unclaim another civilization's claimed chunk.");
        }

        if (existing.plot().landClass() == LandClass.PRIVATE) {
            UUID ownerId = existing.plot().ownerId();
            if (ownerId != null
                    && !ownerId.equals(player.getUUID())
                    && !canManageLandZoning(source, data, existing.civilizationId())) {
                return UnclaimDecision.denied("Only owner/leadership/admin can unclaim this PRIVATE plot.");
            }
            return UnclaimDecision.permit();
        }

        if (!canManageTownClaims(source, data, existing.civilizationId())) {
            return UnclaimDecision.denied("Only leadership/admin can unclaim CIVIC territory.");
        }
        return UnclaimDecision.permit();
    }

    private static void applyClaim(CommandSourceStack source, ClaimDecision decision) {
        if (source.getServer() == null || source.getServer().overworld() == null || decision.player() == null) {
            return;
        }

        ServerPlayer player = decision.player();
        CivSavedData data = CivSavedData.get(source.getServer());
        @Nullable PlotLookup existing = data.getPlotAnyCivilization(
                decision.dimension(),
                decision.chunkX(),
                decision.chunkZ());

        if (existing != null
                && !existing.civilizationId().equals(decision.civId())
                && source.hasPermission(3)) {
            data.clearPlot(existing.civilizationId(), decision.dimension(), decision.chunkX(), decision.chunkZ());
        }

        if (decision.targetClass() == LandClass.CIVIC) {
            data.addCivTreasuryCents(decision.civId(), -decision.claimCostCents());
            data.setPlot(
                    decision.civId(),
                    decision.dimension(),
                    decision.chunkX(),
                    decision.chunkZ(),
                    LandClass.CIVIC,
                    null,
                    decision.nowGameTick(),
                    0L);
            data.addAuditLog(
                    decision.civId(),
                    actorName(source) + " claimed CIVIC chunk " + decision.dimension()
                            + "[" + decision.chunkX() + "," + decision.chunkZ() + "] via FTB map"
                            + " for " + RealCivUtil.formatCredits(decision.claimCostCents()),
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            data.setDirty();

            player.sendSystemMessage(Component.literal(
                    "CIVIC chunk claimed at [" + decision.chunkX() + ", " + decision.chunkZ() + "]."
                            + " Civ treasury: "
                            + RealCivUtil.formatCredits(data.civTreasuryCents(decision.civId())) + "."));
            return;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        record.addSocialCreditCents(decision.civId(), -decision.claimCostCents());
        data.setPlot(
                decision.civId(),
                decision.dimension(),
                decision.chunkX(),
                decision.chunkZ(),
                LandClass.PRIVATE,
                player.getUUID(),
                decision.nowGameTick(),
                decision.initialPaidTicks());
        data.addAuditLog(
                decision.civId(),
                actorName(source) + " claimed PRIVATE chunk " + decision.dimension()
                        + "[" + decision.chunkX() + "," + decision.chunkZ() + "] via FTB map"
                        + " for " + RealCivUtil.formatCredits(decision.claimCostCents()),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        player.sendSystemMessage(Component.literal(
                "PRIVATE chunk claimed at [" + decision.chunkX() + ", " + decision.chunkZ() + "]. Cost: "
                        + RealCivUtil.formatCredits(decision.claimCostCents())
                        + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(decision.civId()))));
    }

    private static void unclaimInternally(ClaimedChunk chunk, CommandSourceStack source) {
        boolean previous = INTERNAL_UNCLAIM.get();
        INTERNAL_UNCLAIM.set(true);
        try {
            chunk.unclaim(source, true);
        } catch (Exception exception) {
            RealCivMod.LOGGER.warn("Failed to rollback invalid FTB chunk claim at {}.", chunk.getPos(), exception);
        } finally {
            INTERNAL_UNCLAIM.set(previous);
        }
    }

    private static void sendClaimFailure(CommandSourceStack source, DenialReason denial) {
        String message = denial.message();
        if (message == null || message.isBlank()) {
            return;
        }
        source.sendFailure(Component.literal(message));
    }

    private static boolean canManageFtbMode(CommandSourceStack source, CivSavedData data, String civId) {
        return CivPermissionService.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE);
    }

    private static boolean canManageTownClaims(CommandSourceStack source, CivSavedData data, String civId) {
        return CivPermissionService.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS);
    }

    private static boolean canManageLandZoning(CommandSourceStack source, CivSavedData data, String civId) {
        return CivPermissionService.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING);
    }

    private static boolean isWithinOrAdjacentToTown(
            CivSavedData data,
            String civId,
            String dimension,
            long chunkX,
            long chunkZ) {
        if (isTownChunk(data, civId, dimension, chunkX, chunkZ)) {
            return true;
        }
        if (isTownChunk(data, civId, dimension, chunkX + 1, chunkZ)) {
            return true;
        }
        if (isTownChunk(data, civId, dimension, chunkX - 1, chunkZ)) {
            return true;
        }
        if (isTownChunk(data, civId, dimension, chunkX, chunkZ + 1)) {
            return true;
        }
        return isTownChunk(data, civId, dimension, chunkX, chunkZ - 1);
    }

    private static boolean isTownChunk(CivSavedData data, String civId, String dimension, long chunkX, long chunkZ) {
        @Nullable PlotRecord plot = data.getPlot(civId, dimension, chunkX, chunkZ);
        return plot != null && plot.landClass() == LandClass.CIVIC;
    }

    private static long nextTownClaimCostCents(int civicChunksOwned) {
        long base = RealCivConfig.townClaimCostCents();
        long extra = RealCivConfig.townClaimCostAddedPerOwnedCents() * Math.max(0, civicChunksOwned);
        return Math.max(0L, base + extra);
    }

    private static long nextPrivateClaimCostCents(int privateOwnedByPlayer) {
        long base = RealCivConfig.rentCostCents();
        long extra = RealCivConfig.rentCostAddedPerOwnedPrivateCents() * Math.max(0, privateOwnedByPlayer);
        return Math.max(0L, base + extra);
    }

    private static String actorName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getGameProfile().getName();
        }
        return source.getTextName();
    }

    private interface DenialReason {
        @Nullable
        String message();
    }

    private record ClaimDecision(
            boolean allowed,
            @Nullable String message,
            @Nullable ServerPlayer player,
            @Nullable String civId,
            @Nullable LandClass targetClass,
            @Nullable String dimension,
            long chunkX,
            long chunkZ,
            long nowGameTick,
            long initialPaidTicks,
            long claimCostCents) implements DenialReason {
        private static ClaimDecision denied(String message) {
            return new ClaimDecision(false, message, null, null, null, null, 0L, 0L, 0L, 0L, 0L);
        }

        private static ClaimDecision allowed(
                ServerPlayer player,
                String civId,
                LandClass targetClass,
                String dimension,
                long chunkX,
                long chunkZ,
                long nowGameTick,
                long initialPaidTicks,
                long claimCostCents) {
            return new ClaimDecision(
                    true,
                    null,
                    player,
                    civId,
                    targetClass,
                    dimension,
                    chunkX,
                    chunkZ,
                    nowGameTick,
                    initialPaidTicks,
                    claimCostCents);
        }
    }

    private record UnclaimDecision(boolean allowed, @Nullable String message) implements DenialReason {
        private static UnclaimDecision permit() {
            return new UnclaimDecision(true, null);
        }

        private static UnclaimDecision denied(String message) {
            return new UnclaimDecision(false, message);
        }
    }
}
