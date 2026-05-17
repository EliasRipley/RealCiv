package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.*;
import com.realciv.realciv.data.LandClass;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class LandWandService {
    // --- POS1/POS2 SELECTION SYSTEM (DISABLED) ---
    // The pos1/pos2 chunk selection system is disabled. Right-clicking the Land Wand
    // in the air opens the FTB claim map instead. Uncomment to re-enable selection-based
    // bulk zoning via wand.
    //
    // private static final Map<UUID, SelectionState> SELECTIONS = new ConcurrentHashMap<>();
    // public static void setPos1(ServerPlayer player, BlockPos pos) { ... }
    // public static void setPos2(ServerPlayer player, BlockPos pos) { ... }
    // public static void clearSelection(ServerPlayer player) { ... }
    // public static boolean hasSelection(ServerPlayer player) { ... }
    // public static ChunkSelection selectionForCurrentDimension(...) { ... }
    // private static void sendSelectionMessage(...) { ... }
    // public record ChunkSelection(...) { ... }
    // private record ChunkPoint(...) { ... }
    // private static final class SelectionState { ... }
    // ------------------------------------------------

    private LandWandService() {
    }

    public static int visualizeNearbyPlots(ServerPlayer player, CivSavedData data, int radiusChunks) {
        ServerLevel level = player.serverLevel();
        String dimension = level.dimension().location().toString();
        long centerX = player.chunkPosition().x;
        long centerZ = player.chunkPosition().z;
        double y = player.getY() + 0.25D;
        Map<Long, java.util.SortedSet<Long>> horizontalSegments = new java.util.HashMap<>();
        Map<Long, java.util.SortedSet<Long>> verticalSegments = new java.util.HashMap<>();

        int radius = Math.max(0, radiusChunks);
        for (long chunkX = centerX - radius; chunkX <= centerX + radius; chunkX++) {
            for (long chunkZ = centerZ - radius; chunkZ <= centerZ + radius; chunkZ++) {
                @Nullable PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
                if (lookup == null) {
                    continue;
                }

                @Nullable PlotLookup north = data.getPlotAnyCivilization(dimension, chunkX, chunkZ - 1);
                @Nullable PlotLookup south = data.getPlotAnyCivilization(dimension, chunkX, chunkZ + 1);
                @Nullable PlotLookup west = data.getPlotAnyCivilization(dimension, chunkX - 1, chunkZ);
                @Nullable PlotLookup east = data.getPlotAnyCivilization(dimension, chunkX + 1, chunkZ);

                ParticleOptions particle = particleFor(lookup.plot().landClass());
                if (!sameBoundaryGroup(lookup, north)) {
                    drawNorthEdge(level, chunkX, chunkZ, y, particle);
                    addHorizontalSegment(horizontalSegments, chunkZ, chunkX);
                }
                if (!sameBoundaryGroup(lookup, south)) {
                    drawSouthEdge(level, chunkX, chunkZ, y, particle);
                    addHorizontalSegment(horizontalSegments, chunkZ + 1L, chunkX);
                }
                if (!sameBoundaryGroup(lookup, west)) {
                    drawWestEdge(level, chunkX, chunkZ, y, particle);
                    addVerticalSegment(verticalSegments, chunkX, chunkZ);
                }
                if (!sameBoundaryGroup(lookup, east)) {
                    drawEastEdge(level, chunkX, chunkZ, y, particle);
                    addVerticalSegment(verticalSegments, chunkX + 1L, chunkZ);
                }
            }
        }
        return countMergedLines(horizontalSegments) + countMergedLines(verticalSegments);
    }

    public static int visualizeSelection(ServerPlayer player) {
        // POS1/POS2 SELECTION (DISABLED) - always returns 0
        return 0;
    }

    private static void drawNorthEdge(ServerLevel level, long chunkX, long chunkZ, double y, ParticleOptions particle) {
        long worldX = chunkX << 4;
        long worldZ = chunkZ << 4;
        for (int offset = 0; offset <= 16; offset += 2) {
            level.sendParticles(
                    particle,
                    worldX + offset + 0.5D,
                    y,
                    worldZ + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static void drawSouthEdge(ServerLevel level, long chunkX, long chunkZ, double y, ParticleOptions particle) {
        long worldX = chunkX << 4;
        long worldZ = chunkZ << 4;
        for (int offset = 0; offset <= 16; offset += 2) {
            level.sendParticles(
                    particle,
                    worldX + offset + 0.5D,
                    y,
                    worldZ + 16.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static void drawWestEdge(ServerLevel level, long chunkX, long chunkZ, double y, ParticleOptions particle) {
        long worldX = chunkX << 4;
        long worldZ = chunkZ << 4;
        for (int offset = 0; offset <= 16; offset += 2) {
            level.sendParticles(
                    particle,
                    worldX + 0.5D,
                    y,
                    worldZ + offset + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static void drawEastEdge(ServerLevel level, long chunkX, long chunkZ, double y, ParticleOptions particle) {
        long worldX = chunkX << 4;
        long worldZ = chunkZ << 4;
        for (int offset = 0; offset <= 16; offset += 2) {
            level.sendParticles(
                    particle,
                    worldX + 16.5D,
                    y,
                    worldZ + offset + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static boolean sameBoundaryGroup(@Nullable PlotLookup left, @Nullable PlotLookup right) {
        if (left == null || right == null) {
            return false;
        }
        if (!left.civilizationId().equals(right.civilizationId())) {
            return false;
        }
        if (left.plot().landClass() != right.plot().landClass()) {
            return false;
        }
        return Objects.equals(left.plot().ownerId(), right.plot().ownerId());
    }

    private static void addHorizontalSegment(Map<Long, java.util.SortedSet<Long>> segments, long zLine, long xStart) {
        segments.computeIfAbsent(zLine, ignored -> new java.util.TreeSet<>()).add(xStart);
    }

    private static void addVerticalSegment(Map<Long, java.util.SortedSet<Long>> segments, long xLine, long zStart) {
        segments.computeIfAbsent(xLine, ignored -> new java.util.TreeSet<>()).add(zStart);
    }

    private static int countMergedLines(Map<Long, java.util.SortedSet<Long>> segments) {
        int lines = 0;
        for (java.util.SortedSet<Long> starts : segments.values()) {
            Long prev = null;
            for (Long start : starts) {
                if (prev == null || start != prev + 1L) {
                    lines++;
                }
                prev = start;
            }
        }
        return lines;
    }

    private static ParticleOptions particleFor(LandClass landClass) {
        return switch (landClass) {
            case COMMUNITY -> ParticleTypes.HAPPY_VILLAGER;
            case CIVIC -> ParticleTypes.END_ROD;
            case PRIVATE -> ParticleTypes.FLAME;
        };
    }

    // DISABLED: private static void sendSelectionMessage(ServerPlayer player, SelectionState state) { ... }
    // DISABLED: public record ChunkSelection(...) { ... }
    // DISABLED: private record ChunkPoint(...) { ... }
    // DISABLED: private static final class SelectionState { ... }
}
