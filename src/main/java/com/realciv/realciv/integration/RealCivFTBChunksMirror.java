package com.realciv.realciv.integration;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.data.CivSavedData;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.data.AbstractTeam;
import dev.ftb.mods.ftbteams.data.AbstractTeamBase;
import dev.ftb.mods.ftbteams.data.PlayerTeam;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class RealCivFTBChunksMirror {
    private static final String TEAM_DESCRIPTION_PREFIX = "RealCiv civilization team:";

    private RealCivFTBChunksMirror() {
    }

    public static boolean ftbApisReady() {
        return FTBChunksAPI.api().isManagerLoaded() && FTBTeamsAPI.api().isManagerLoaded();
    }

    public static void syncAll(MinecraftServer server, CivSavedData data) {
        if (!ftbApisReady()) {
            return;
        }

        for (String civId : data.civilizationIdsSorted()) {
            syncCivilization(server, data, civId);
        }
    }

    public static void syncCivilization(MinecraftServer server, CivSavedData data, String civIdRaw) {
        if (!ftbApisReady()) {
            return;
        }

        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civIdRaw);
        if (civ == null) {
            return;
        }

        @Nullable Team civTeam = ensureCivTeam(server, civ);
        if (civTeam == null) {
            return;
        }

        syncCivilizationMembership(server, data, civ.id(), civTeam);
        syncCivilizationClaims(server, data, civ.id(), civTeam);
    }

    public static void syncPlotChange(
            MinecraftServer server,
            CivSavedData data,
            String civIdRaw,
            String dimension,
            long chunkX,
            long chunkZ,
            boolean claimed) {
        if (!ftbApisReady()) {
            return;
        }

        if (!claimed) {
            clearClaimAt(server, dimension, chunkX, chunkZ);
            return;
        }

        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civIdRaw);
        if (civ == null) {
            return;
        }
        @Nullable Team civTeam = ensureCivTeam(server, civ);
        if (civTeam == null) {
            return;
        }
        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ChunkTeamData teamData = manager.getOrCreateData(civTeam);
        ensureTeamClaimCapacity(teamData, civ.plots().size());
        ensureClaimOwnedByTeam(server, teamData, civTeam, dimension, chunkX, chunkZ);
    }

    public static void clearClaimAt(MinecraftServer server, String dimension, long chunkX, long chunkZ) {
        if (!ftbApisReady()) {
            return;
        }

        @Nullable ChunkDimPos pos = toChunkPos(dimension, chunkX, chunkZ);
        if (pos == null) {
            return;
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        @Nullable ClaimedChunk existing = manager.getChunk(pos);
        if (existing == null) {
            return;
        }

        CommandSourceStack source = syncSource(server);
        RealCivFTBChunksBridge.runInternalSync(() -> existing.unclaim(source, true));
    }

    private static void syncCivilizationMembership(MinecraftServer server, CivSavedData data, String civId, Team civTeam) {
        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        if (!(civTeam instanceof AbstractTeam civAbstract) || !(civTeam instanceof AbstractTeamBase civBase)) {
            return;
        }

        Set<UUID> members = new HashSet<>(data.civilizationMembersSorted(civId));
        for (UUID memberId : members) {
            assignPlayerToCivTeam(data, civId, teamManager, civAbstract, civBase, memberId);
        }

        for (UUID teamMember : new HashSet<>(civBase.getMembers())) {
            if (members.contains(teamMember)) {
                continue;
            }
            removePlayerFromCivTeam(teamManager, civAbstract, civBase, teamMember);
        }

        civBase.markDirty();
        teamManager.markDirty();
        syncTeamPackets(teamManager, civAbstract, members);
    }

    private static void assignPlayerToCivTeam(
            CivSavedData data,
            String civId,
            TeamManager teamManager,
            AbstractTeam civAbstract,
            AbstractTeamBase civBase,
            UUID playerId) {
        @Nullable Team rawPersonal = teamManager.getPlayerTeamForPlayerID(playerId).orElse(null);
        if (!(rawPersonal instanceof PlayerTeam personalTeam)) {
            return;
        }

        @Nullable AbstractTeam previousEffective = personalTeam.getEffectiveTeam();
        if (previousEffective instanceof AbstractTeamBase previousBase && previousBase != civBase) {
            previousBase.removeMember(playerId);
            previousBase.markDirty();
        }

        TeamRank rank = data.isMayor(civId, playerId)
                ? TeamRank.OWNER
                : (data.isCivicManager(civId, playerId) ? TeamRank.OFFICER : TeamRank.MEMBER);
        civBase.addMember(playerId, rank);
        civBase.markDirty();

        if (previousEffective == personalTeam) {
            personalTeam.removeMember(playerId);
        }
        personalTeam.setEffectiveTeam(civAbstract);
        personalTeam.markDirty();
        personalTeam.updatePresence();
    }

    private static void removePlayerFromCivTeam(
            TeamManager teamManager,
            AbstractTeam civAbstract,
            AbstractTeamBase civBase,
            UUID playerId) {
        civBase.removeMember(playerId);
        civBase.markDirty();

        @Nullable Team rawPersonal = teamManager.getPlayerTeamForPlayerID(playerId).orElse(null);
        if (!(rawPersonal instanceof PlayerTeam personalTeam)) {
            return;
        }

        if (personalTeam.getEffectiveTeam() != civAbstract) {
            return;
        }

        personalTeam.addMember(playerId, TeamRank.OWNER);
        personalTeam.setEffectiveTeam(personalTeam);
        personalTeam.markDirty();
        personalTeam.updatePresence();
    }

    private static void syncTeamPackets(TeamManager teamManager, AbstractTeam civTeam, Set<UUID> members) {
        if (!(teamManager instanceof TeamManagerImpl impl)) {
            return;
        }

        impl.syncToAll(civTeam);
        for (UUID memberId : members) {
            @Nullable ServerPlayer online = impl.getServer().getPlayerList().getPlayer(memberId);
            if (online != null) {
                impl.syncAllToPlayer(online, civTeam);
            }
        }
    }

    private static void syncCivilizationClaims(MinecraftServer server, CivSavedData data, String civId, Team civTeam) {
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
        if (civ == null) {
            return;
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ChunkTeamData teamData = manager.getOrCreateData(civTeam);
        int desiredCount = civ.plots().size();
        ensureTeamClaimCapacity(teamData, desiredCount);

        Set<String> desiredKeys = new HashSet<>();
        for (CivSavedData.PlotRecord plot : civ.plots().values()) {
            desiredKeys.add(plot.plotKey());
            ensureClaimOwnedByTeam(server, teamData, civTeam, plot.dimension(), plot.chunkX(), plot.chunkZ());
        }

        CommandSourceStack source = syncSource(server);
        for (ClaimedChunk claimedChunk : new ArrayList<>(teamData.getClaimedChunks())) {
            String key = toPlotKey(claimedChunk.getPos());
            if (desiredKeys.contains(key)) {
                continue;
            }
            RealCivFTBChunksBridge.runInternalSync(() -> claimedChunk.unclaim(source, true));
        }
    }

    private static void ensureClaimOwnedByTeam(
            MinecraftServer server,
            ChunkTeamData teamData,
            Team team,
            String dimension,
            long chunkX,
            long chunkZ) {
        @Nullable ChunkDimPos pos = toChunkPos(dimension, chunkX, chunkZ);
        if (pos == null) {
            return;
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        @Nullable ClaimedChunk existing = manager.getChunk(pos);
        if (existing != null && existing.getTeamData().getTeam().getTeamId().equals(team.getTeamId())) {
            return;
        }

        CommandSourceStack source = syncSource(server);
        RealCivFTBChunksBridge.runInternalSync(() -> {
            if (existing != null) {
                existing.unclaim(source, true);
            }
            ClaimResult claimResult = teamData.claim(source, pos, false);
            if (!claimResult.isSuccess()) {
                ensureTeamClaimCapacity(teamData, teamData.getClaimedChunks().size() + 1);
                claimResult = teamData.claim(source, pos, false);
            }
            if (!claimResult.isSuccess()) {
                RealCivMod.LOGGER.warn(
                        "FTB mirror claim failed at {} for team {}: {}",
                        pos,
                        team.getTeamId(),
                        claimResult.getResultId());
            }
        });
    }

    private static void ensureTeamClaimCapacity(ChunkTeamData teamData, int targetClaimCount) {
        int minClaims = Math.max(16, targetClaimCount + 16);
        int currentMax = teamData.getMaxClaimChunks();
        if (currentMax >= minClaims) {
            return;
        }
        int delta = minClaims - currentMax;
        teamData.setExtraClaimChunks(teamData.getExtraClaimChunks() + delta);
    }

    @Nullable
    private static Team ensureCivTeam(MinecraftServer server, CivSavedData.CivilizationRecord civ) {
        TeamManager manager = FTBTeamsAPI.api().getManager();
        UUID teamId = civTeamId(civ.id());

        @Nullable Team team = manager.getTeamByID(teamId).orElse(null);
        if (team == null) {
            String creationName = createServerTeamName(civ.displayName(), civ.id());
            try {
                team = manager.createServerTeam(
                        syncSource(server),
                        creationName,
                        TEAM_DESCRIPTION_PREFIX + " " + civ.id(),
                        colorForCiv(civ.id()),
                        teamId);
            } catch (CommandSyntaxException exception) {
                RealCivMod.LOGGER.warn(
                        "Failed to create FTB server team for civ {} ({})",
                        civ.id(),
                        civ.displayName(),
                        exception);
                return null;
            }
        }

        String expectedName = civ.displayName();
        String expectedDescription = TEAM_DESCRIPTION_PREFIX + " " + civ.id();
        boolean changed = false;
        if (!expectedName.equals(team.getProperty(TeamProperties.DISPLAY_NAME))) {
            team.setProperty(TeamProperties.DISPLAY_NAME, expectedName);
            changed = true;
        }
        if (!expectedDescription.equals(team.getProperty(TeamProperties.DESCRIPTION))) {
            team.setProperty(TeamProperties.DESCRIPTION, expectedDescription);
            changed = true;
        }
        if (changed) {
            team.markDirty();
            manager.markDirty();
            if (manager instanceof TeamManagerImpl impl && team instanceof AbstractTeam abstractTeam) {
                impl.syncToAll(abstractTeam);
            }
        }
        return team;
    }

    private static String toPlotKey(ChunkDimPos pos) {
        return pos.dimension().location() + "|" + pos.x() + "|" + pos.z();
    }

    @Nullable
    private static ChunkDimPos toChunkPos(String dimension, long chunkX, long chunkZ) {
        int x;
        int z;
        try {
            x = Math.toIntExact(chunkX);
            z = Math.toIntExact(chunkZ);
        } catch (ArithmeticException exception) {
            RealCivMod.LOGGER.warn("Chunk position outside int range for FTB mirror: {} {}", chunkX, chunkZ);
            return null;
        }

        ResourceLocation location;
        try {
            location = ResourceLocation.parse(dimension);
        } catch (Exception exception) {
            RealCivMod.LOGGER.warn("Invalid dimension id for FTB mirror: {}", dimension, exception);
            return null;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, location);
        return new ChunkDimPos(dimensionKey, x, z);
    }

    private static CommandSourceStack syncSource(MinecraftServer server) {
        return server.createCommandSourceStack().withPermission(4);
    }

    private static UUID civTeamId(String civId) {
        return UUID.nameUUIDFromBytes(("realciv:ftb-team:" + civId).getBytes(StandardCharsets.UTF_8));
    }

    private static Color4I colorForCiv(String civId) {
        int hash = civId.hashCode();
        int red = 80 + Math.floorMod(hash, 120);
        int green = 80 + Math.floorMod(hash >> 8, 120);
        int blue = 80 + Math.floorMod(hash >> 16, 120);
        return Color4I.rgb(red, green, blue);
    }

    private static String createServerTeamName(String displayName, String civId) {
        String trimmed = displayName == null ? "" : displayName.trim();
        if (trimmed.length() >= 3) {
            return trimmed;
        }
        String fallback = ("Civ " + civId).trim();
        return fallback.length() >= 3 ? fallback : "RealCiv-" + civId;
    }
}

