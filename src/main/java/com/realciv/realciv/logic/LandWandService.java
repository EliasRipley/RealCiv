package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class LandWandService {
    private static final Map<UUID, SelectionState> SELECTIONS = new ConcurrentHashMap<>();

    private LandWandService() {
    }

    public static void setPos1(ServerPlayer player, BlockPos pos) {
        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = pos.getX() >> 4;
        long chunkZ = pos.getZ() >> 4;
        SelectionState state = SELECTIONS.computeIfAbsent(player.getUUID(), ignored -> new SelectionState());
        state.setPos1(dimension, chunkX, chunkZ);
        sendSelectionMessage(player, state);
    }

    public static void setPos2(ServerPlayer player, BlockPos pos) {
        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = pos.getX() >> 4;
        long chunkZ = pos.getZ() >> 4;
        SelectionState state = SELECTIONS.computeIfAbsent(player.getUUID(), ignored -> new SelectionState());
        state.setPos2(dimension, chunkX, chunkZ);
        sendSelectionMessage(player, state);
    }

    public static void clearSelection(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("[RealCiv] Land wand selection cleared."));
    }

    public static boolean hasSelection(ServerPlayer player) {
        SelectionState state = SELECTIONS.get(player.getUUID());
        return state != null && state.isComplete();
    }

    @Nullable
    public static ChunkSelection selectionForCurrentDimension(ServerPlayer player) {
        SelectionState state = SELECTIONS.get(player.getUUID());
        if (state == null || !state.isComplete()) {
            return null;
        }
        String currentDimension = player.serverLevel().dimension().location().toString();
        return state.toSelection(currentDimension);
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
                @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
                if (lookup == null) {
                    continue;
                }

                @Nullable CivSavedData.PlotLookup north = data.getPlotAnyCivilization(dimension, chunkX, chunkZ - 1);
                @Nullable CivSavedData.PlotLookup south = data.getPlotAnyCivilization(dimension, chunkX, chunkZ + 1);
                @Nullable CivSavedData.PlotLookup west = data.getPlotAnyCivilization(dimension, chunkX - 1, chunkZ);
                @Nullable CivSavedData.PlotLookup east = data.getPlotAnyCivilization(dimension, chunkX + 1, chunkZ);

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
        ChunkSelection selection = selectionForCurrentDimension(player);
        if (selection == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        double y = player.getY() + 0.45D;
        ParticleOptions particle = ParticleTypes.END_ROD;

        long minBlockX = selection.minChunkX() << 4;
        long maxBlockX = (selection.maxChunkX() + 1L) << 4;
        long minBlockZ = selection.minChunkZ() << 4;
        long maxBlockZ = (selection.maxChunkZ() + 1L) << 4;

        drawHorizontalBoundary(level, minBlockX, maxBlockX, minBlockZ, y, particle);
        drawHorizontalBoundary(level, minBlockX, maxBlockX, maxBlockZ, y, particle);
        drawVerticalBoundary(level, minBlockZ, maxBlockZ, minBlockX, y, particle);
        drawVerticalBoundary(level, minBlockZ, maxBlockZ, maxBlockX, y, particle);

        return 4;
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

    private static void drawHorizontalBoundary(
            ServerLevel level,
            long minBlockX,
            long maxBlockX,
            long fixedBlockZ,
            double y,
            ParticleOptions particle) {
        for (long x = minBlockX; x <= maxBlockX; x += 2L) {
            level.sendParticles(
                    particle,
                    x + 0.5D,
                    y,
                    fixedBlockZ + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static void drawVerticalBoundary(
            ServerLevel level,
            long minBlockZ,
            long maxBlockZ,
            long fixedBlockX,
            double y,
            ParticleOptions particle) {
        for (long z = minBlockZ; z <= maxBlockZ; z += 2L) {
            level.sendParticles(
                    particle,
                    fixedBlockX + 0.5D,
                    y,
                    z + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static boolean sameBoundaryGroup(@Nullable CivSavedData.PlotLookup left, @Nullable CivSavedData.PlotLookup right) {
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
            case PUBLIC -> ParticleTypes.HAPPY_VILLAGER;
            case CIVIC -> ParticleTypes.END_ROD;
            case PRIVATE -> ParticleTypes.FLAME;
        };
    }

    private static void sendSelectionMessage(ServerPlayer player, SelectionState state) {
        ChunkPoint pos1 = state.pos1();
        ChunkPoint pos2 = state.pos2();
        if (pos1 == null) {
            return;
        }

        if (pos2 == null) {
            player.sendSystemMessage(Component.literal(
                    "[RealCiv] Wand pos1 set: chunk [" + pos1.chunkX() + ", " + pos1.chunkZ() + "] in " + state.dimension()
                            + ". Right-click another chunk with the wand to set pos2."));
            return;
        }

        long minX = Math.min(pos1.chunkX(), pos2.chunkX());
        long maxX = Math.max(pos1.chunkX(), pos2.chunkX());
        long minZ = Math.min(pos1.chunkZ(), pos2.chunkZ());
        long maxZ = Math.max(pos1.chunkZ(), pos2.chunkZ());
        long count = ((maxX - minX) + 1L) * ((maxZ - minZ) + 1L);
        player.sendSystemMessage(Component.literal(
                "[RealCiv] Wand selection: " + count + " chunk(s) | X " + minX + ".." + maxX
                        + " | Z " + minZ + ".." + maxZ + " | dimension " + state.dimension()));
    }

    public record ChunkSelection(String dimension, long minChunkX, long maxChunkX, long minChunkZ, long maxChunkZ, long chunkCount) {
    }

    private record ChunkPoint(long chunkX, long chunkZ) {
    }

    private static final class SelectionState {
        @Nullable
        private String dimension;
        @Nullable
        private ChunkPoint pos1;
        @Nullable
        private ChunkPoint pos2;

        @Nullable
        public ChunkPoint pos1() {
            return pos1;
        }

        @Nullable
        public ChunkPoint pos2() {
            return pos2;
        }

        @Nullable
        public String dimension() {
            return dimension;
        }

        public void setPos1(String dimension, long chunkX, long chunkZ) {
            if (!Objects.equals(this.dimension, dimension)) {
                this.pos2 = null;
            }
            this.dimension = dimension;
            this.pos1 = new ChunkPoint(chunkX, chunkZ);
        }

        public void setPos2(String dimension, long chunkX, long chunkZ) {
            if (!Objects.equals(this.dimension, dimension)) {
                this.pos1 = null;
            }
            this.dimension = dimension;
            this.pos2 = new ChunkPoint(chunkX, chunkZ);
        }

        public boolean isComplete() {
            return dimension != null && pos1 != null && pos2 != null;
        }

        @Nullable
        public ChunkSelection toSelection(String currentDimension) {
            if (!isComplete() || !Objects.equals(dimension, currentDimension)) {
                return null;
            }

            long minX = Math.min(pos1.chunkX(), pos2.chunkX());
            long maxX = Math.max(pos1.chunkX(), pos2.chunkX());
            long minZ = Math.min(pos1.chunkZ(), pos2.chunkZ());
            long maxZ = Math.max(pos1.chunkZ(), pos2.chunkZ());
            long count = ((maxX - minX) + 1L) * ((maxZ - minZ) + 1L);
            return new ChunkSelection(dimension, minX, maxX, minZ, maxZ, count);
        }
    }
}
