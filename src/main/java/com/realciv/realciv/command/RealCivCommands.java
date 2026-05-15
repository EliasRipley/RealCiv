package com.realciv.realciv.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.*;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.integration.RealCivFTBChunksBridge;
import com.realciv.realciv.integration.RealCivFTBChunksMirror;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

public final class RealCivCommands {
    private RealCivCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("realciv")
                .then(Commands.literal("profile")
                        .executes(ctx -> ProfileCommands.showProfile(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> ProfileCommands.showProfile(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("profession")
                        .then(Commands.literal("focus")
                                .then(Commands.literal("show")
                                        .executes(ctx -> ProfessionCommands.professionFocusShow(
                                                ctx.getSource(),
                                                ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> ProfessionCommands.professionFocusShow(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("profession", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        ProfessionCommands.focusableProfessionNames(),
                                                        builder))
                                                .executes(ctx -> ProfessionCommands.professionFocusSetSelf(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "profession")))))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> ProfessionCommands.professionFocusClearSelf(ctx.getSource())))
                                .then(Commands.literal("assign")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("profession", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                ProfessionCommands.focusableProfessionNames(),
                                                                builder))
                                                        .executes(ctx -> ProfessionCommands.professionFocusAssign(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "profession"))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> ProfessionCommands.professionFocusRemove(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("civ")
                        .then(Commands.literal("info")
                                .executes(ctx -> civInfo(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> civInfo(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("list")
                                .executes(ctx -> civList(ctx.getSource())))
                        .then(Commands.literal("title")
                                .then(Commands.literal("show")
                                        .executes(ctx -> civTitleShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> civTitleShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                .executes(ctx -> civTitleSetSelf(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "title"))))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(3))
                                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                                        .executes(ctx -> civTitleSetAdmin(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "civ"),
                                                                StringArgumentType.getString(ctx, "title"))))))
                                .then(Commands.literal("reset")
                                        .executes(ctx -> civTitleResetSelf(ctx.getSource()))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(3))
                                                .executes(ctx -> civTitleResetAdmin(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ"))))))
                        .then(Commands.literal("governance")
                                .then(Commands.literal("show")
                                        .executes(ctx -> civGovernanceShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> civGovernanceShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("model", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of("autocratic", "council", "democratic"),
                                                        builder))
                                                .executes(ctx -> civGovernanceSetSelf(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "model"))))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(3))
                                                .then(Commands.argument("model", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                List.of("autocratic", "council", "democratic"),
                                                                builder))
                                                        .executes(ctx -> civGovernanceSetAdmin(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "civ"),
                                                                StringArgumentType.getString(ctx, "model")))))))
                        .then(Commands.literal("role")
                                .then(Commands.literal("list")
                                        .executes(ctx -> civRoleList(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> civRoleList(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("create")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .executes(ctx -> civRoleCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "role"),
                                                        StringArgumentType.getString(ctx, "role")))
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> civRoleCreate(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("rename")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> civRoleRename(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("delete")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .executes(ctx -> civRoleDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "role")))))
                                .then(Commands.literal("permission")
                                        .then(Commands.literal("list")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .executes(ctx -> civRolePermissionList(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role")))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .then(Commands.argument("permission", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                        CivSavedData.knownRolePermissions(),
                                                                        builder))
                                                                .executes(ctx -> civRolePermissionSet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "role"),
                                                                        StringArgumentType.getString(ctx, "permission"),
                                                                        true)))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .then(Commands.argument("permission", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                        CivSavedData.knownRolePermissions(),
                                                                        builder))
                                                                .executes(ctx -> civRolePermissionSet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "role"),
                                                                        StringArgumentType.getString(ctx, "permission"),
                                                                        false))))))
                                .then(Commands.literal("member")
                                        .then(Commands.literal("list")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .executes(ctx -> civRoleMemberList(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role")))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .then(Commands.argument("player", EntityArgument.player())
                                                                .executes(ctx -> civRoleMemberSet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "role"),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        true)))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .then(Commands.argument("player", EntityArgument.player())
                                                                .executes(ctx -> civRoleMemberSet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "role"),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        false)))))))
                        .then(Commands.literal("found")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> civFound(
                                                ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("create")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> civCreate(
                                                ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("rename")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> civRename(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ"),
                                                        StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("delete")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> civDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("join")
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> civJoin(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("leave")
                                .executes(ctx -> civLeave(ctx.getSource())))
                        .then(Commands.literal("assign")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .executes(ctx -> civAssign(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "civ"))))))
                        .then(Commands.literal("diplomacy")
                                .then(Commands.literal("show")
                                        .executes(ctx -> civDiplomacyShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> civDiplomacyShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("other", StringArgumentType.string())
                                                .then(Commands.argument("state", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                List.of("ally", "neutral", "war"),
                                                                builder))
                                                        .executes(ctx -> civDiplomacySet(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "other"),
                                                                StringArgumentType.getString(ctx, "state")))))
                                        .then(Commands.argument("civA", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(3))
                                                .then(Commands.argument("civB", StringArgumentType.string())
                                                        .then(Commands.argument("state", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                        List.of("ally", "neutral", "war"),
                                                                        builder))
                                                                .executes(ctx -> civDiplomacySetBetween(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "civA"),
                                                                        StringArgumentType.getString(ctx, "civB"),
                                                                        StringArgumentType.getString(ctx, "state"))))))))
                        .then(Commands.literal("pvp")
                                .then(Commands.literal("show")
                                        .executes(ctx -> civFriendlyFireShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> civFriendlyFireShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("friendlyfire")
                                        .then(Commands.literal("on")
                                                .executes(ctx -> civFriendlyFireSet(ctx.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(ctx -> civFriendlyFireSet(ctx.getSource(), false)))))
                        .then(Commands.literal("explosives")
                                .then(Commands.literal("show")
                                        .executes(ctx -> civExplosivesShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> civExplosivesShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> civExplosivesSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> civExplosivesSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false)))))
                        .then(Commands.literal("redstoner")
                                .then(Commands.literal("show")
                                        .executes(ctx -> civRedstonerShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> civRedstonerShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> civRedstonerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> civRedstonerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false)))))
                .then(Commands.literal("town")
                        .then(Commands.literal("info")
                                .executes(ctx -> TownCommands.townInfo(ctx.getSource())))
                        .then(Commands.literal("map")
                                .executes(ctx -> TownCommands.townMap(ctx.getSource(), 4))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> TownCommands.townMap(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "radius")))))
                        .then(Commands.literal("claim")
                                .executes(ctx -> TownCommands.townClaim(ctx.getSource())))
                        .then(Commands.literal("unclaim")
                                .executes(ctx -> TownCommands.townUnclaim(ctx.getSource())))
                        .then(Commands.literal("allot")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> TownCommands.townAllotPrivate(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> TownCommands.townAllotPrivate(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "days")))))))
                .then(Commands.literal("plot")
                        .then(Commands.literal("info")
                                .executes(ctx -> LandCommands.showLandInfo(ctx.getSource())))
                        .then(Commands.literal("claim")
                                .executes(ctx -> PlotCommands.plotClaimSelf(ctx.getSource(), RealCivConfig.LAND_RENT_DAYS.get()))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                        .executes(ctx -> PlotCommands.plotClaimSelf(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("unclaim")
                                .executes(ctx -> PlotCommands.plotUnclaimSelf(ctx.getSource()))))
                .then(Commands.literal("land")
                        .then(Commands.literal("rent")
                                .executes(ctx -> PlotCommands.rentCurrentPlot(ctx.getSource(), RealCivConfig.LAND_RENT_DAYS.get()))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                        .executes(ctx -> PlotCommands.rentCurrentPlot(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("info")
                                .executes(ctx -> LandCommands.showLandInfo(ctx.getSource())))
                        .then(Commands.literal("zone")
                                .then(Commands.argument("class", StringArgumentType.word())
                                        .executes(ctx -> LandCommands.landZoneCurrentPlot(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "class"),
                                                null,
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("owner", EntityArgument.player())
                                                .executes(ctx -> LandCommands.landZoneCurrentPlot(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        EntityArgument.getPlayer(ctx, "owner"),
                                                        RealCivConfig.LAND_RENT_DAYS.get()))
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                        .executes(ctx -> LandCommands.landZoneCurrentPlot(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "class"),
                                                                EntityArgument.getPlayer(ctx, "owner"),
                                                                IntegerArgumentType.getInteger(ctx, "days")))))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> LandCommands.landZoneCurrentPlot(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        null,
                                                        IntegerArgumentType.getInteger(ctx, "days"))))))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> LandCommands.landZoneCurrentPlot(
                                                ctx.getSource(),
                                                "private",
                                                EntityArgument.getPlayer(ctx, "player"),
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> LandCommands.landZoneCurrentPlot(
                                                        ctx.getSource(),
                                                        "private",
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "days"))))))
                        .then(Commands.literal("revoke")
                                .executes(ctx -> LandCommands.landRevokeCurrentPlot(ctx.getSource())))
                        .then(Commands.literal("clear")
                                .executes(ctx -> LandCommands.landRevokeCurrentPlot(ctx.getSource())))
                        .then(Commands.literal("manager")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> LandCommands.landManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> LandCommands.landManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false)))))
                        .then(Commands.literal("wand")
                                .executes(ctx -> LandCommands.landWandGive(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> LandCommands.landSelectionClear(ctx.getSource())))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(3))
                                        .executes(ctx -> LandCommands.landWandGive(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("selection")
                                .then(Commands.literal("info")
                                        .executes(ctx -> LandCommands.landSelectionInfo(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> LandCommands.landSelectionClear(ctx.getSource()))))
                        .then(Commands.literal("zone-selection")
                                .then(Commands.argument("class", StringArgumentType.word())
                                        .executes(ctx -> LandCommands.landZoneSelection(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "class"),
                                                null,
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("owner", EntityArgument.player())
                                                .executes(ctx -> LandCommands.landZoneSelection(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        EntityArgument.getPlayer(ctx, "owner"),
                                                        RealCivConfig.LAND_RENT_DAYS.get()))
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                        .executes(ctx -> LandCommands.landZoneSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "class"),
                                                                EntityArgument.getPlayer(ctx, "owner"),
                                                                IntegerArgumentType.getInteger(ctx, "days")))))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> LandCommands.landZoneSelection(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        null,
                                                        IntegerArgumentType.getInteger(ctx, "days"))))))
                        .then(Commands.literal("clear-selection")
                                .executes(ctx -> LandCommands.landClearSelection(ctx.getSource())))
                        .then(Commands.literal("visualize")
                                .executes(ctx -> LandCommands.landVisualize(ctx.getSource(), RealCivConfig.landWandVisualizeRadiusChunks()))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> LandCommands.landVisualize(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "radius")))))
                        .then(Commands.literal("ftb-mode")
                                .executes(ctx -> LandCommands.landFtbModeShow(ctx.getSource()))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                List.of(
                                                        RealCivFTBChunksBridge.CLAIM_MODE_AUTO,
                                                        RealCivFTBChunksBridge.CLAIM_MODE_CIVIC,
                                                        RealCivFTBChunksBridge.CLAIM_MODE_PRIVATE),
                                                builder))
                                        .executes(ctx -> LandCommands.landFtbModeSet(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.literal("gui")
                                .executes(ctx -> LandCommands.openLandGui(ctx.getSource()))))
                .then(Commands.literal("hub")
                        .then(Commands.literal("open")
                                .executes(ctx -> HubCommands.openHubStockMenu(ctx.getSource())))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> HubCommands.withdrawFromHub(
                                                        ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "item"),
                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                        ctx.getSource().getPlayerOrException()))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> HubCommands.withdrawFromHub(
                                                                ctx.getSource(),
                                                                ResourceLocationArgument.getId(ctx, "item"),
                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                EntityArgument.getPlayer(ctx, "target")))))))
                        .then(Commands.literal("stock")
                                .executes(ctx -> HubCommands.showHubStock(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> HubCommands.showHubStock(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("quota")
                                .executes(ctx -> HubCommands.showHubQuotaSelf(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> HubCommands.showHubQuotaSelf(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page"))))
                                .then(Commands.literal("player")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> HubCommands.showHubQuotaFor(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        1))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> HubCommands.showHubQuotaFor(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                IntegerArgumentType.getInteger(ctx, "page")))))))
                        .then(Commands.literal("distribution")
                                .then(Commands.literal("show")
                                        .executes(ctx -> HubCommands.hubDistributionShow(ctx.getSource())))
                                .then(Commands.literal("mode")
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of("contribution_ratio", "shared_stock_ratio", "daily_allowance"),
                                                        builder))
                                                .executes(ctx -> HubCommands.hubDistributionSetMode(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "mode")))))
                                .then(Commands.literal("shared-ratio")
                                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                .executes(ctx -> HubCommands.hubDistributionSetSharedRatio(
                                                        ctx.getSource(),
                                                        DoubleArgumentType.getDouble(ctx, "percent")))))
                                .then(Commands.literal("allowance")
                                        .then(Commands.literal("list")
                                                .executes(ctx -> HubCommands.hubDistributionAllowanceList(ctx.getSource(), 1))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> HubCommands.hubDistributionAllowanceList(
                                                                ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "page")))))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> HubCommands.hubDistributionAllowanceSet(
                                                                        ctx.getSource(),
                                                                        ResourceLocationArgument.getId(ctx, "item"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"))))))
                                        .then(Commands.literal("clear")
                                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                                        .executes(ctx -> HubCommands.hubDistributionAllowanceClear(
                                                                ctx.getSource(),
                                                                ResourceLocationArgument.getId(ctx, "item")))))
                                        .then(Commands.literal("clearall")
                                                .executes(ctx -> HubCommands.hubDistributionAllowanceClearAll(ctx.getSource())))))
                        .then(Commands.literal("coverage")
                                .requires(source -> source.hasPermission(3))
                                .executes(ctx -> HubCommands.showHubCoverage(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> HubCommands.showHubCoverage(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("logs")
                                .executes(ctx -> HubCommands.showHubLogs(ctx.getSource(), 20))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 200))
                                        .executes(ctx -> HubCommands.showHubLogs(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("export-items")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .executes(ctx -> HubCommands.exportHubItemIds(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "namespace"))))))
                .then(Commands.literal("census")
                        .then(Commands.literal("members")
                                .executes(ctx -> CensusCommands.censusMembers(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> CensusCommands.censusMembers(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("requests")
                                .executes(ctx -> CensusCommands.censusRequests(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> CensusCommands.censusRequests(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("invites")
                                .executes(ctx -> CensusCommands.censusInvites(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> CensusCommands.censusInvites(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> CensusCommands.censusInvitePlayer(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("uninvite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> CensusCommands.censusUninvitePlayer(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("approve")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> CensusCommands.censusApproveRequest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("deny")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> CensusCommands.censusDenyRequest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> CensusCommands.censusRemoveMember(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("manager")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> CensusCommands.censusManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> CensusCommands.censusManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false)))))
                        .then(Commands.literal("mayor")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> CensusCommands.censusMayorSet(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> CensusCommands.censusMayorClear(ctx.getSource())))))
                .then(Commands.literal("tax")
                        .then(Commands.literal("status")
                                .executes(ctx -> TaxCommands.taxStatus(ctx.getSource()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> TaxCommands.taxStatusFor(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("pay")
                                .executes(ctx -> TaxCommands.taxPay(ctx.getSource(), 1))
                                .then(Commands.argument("cycles", IntegerArgumentType.integer(1, 365))
                                        .executes(ctx -> TaxCommands.taxPay(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "cycles"))))))
                .then(Commands.literal("credit")
                        .requires(source -> source.hasPermission(3))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                                                .executes(ctx -> CreditCommands.creditAdd(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount"))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                                                .executes(ctx -> CreditCommands.creditSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount")))))))
                .then(Commands.literal("mayor")
                        .then(Commands.literal("show")
                                .executes(ctx -> mayorShow(ctx.getSource(), null))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> mayorShow(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> mayorSet(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .executes(ctx -> mayorSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "civ"))))))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(3))
                                .executes(ctx -> mayorClear(ctx.getSource(), null))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> mayorClear(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("withdrawrate")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> mayorWithdrawRateShow(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                        .executes(ctx -> mayorWithdrawRateSet(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                DoubleArgumentType.getDouble(ctx, "percent"))))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> mayorWithdrawRateClear(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"))))))
                        .then(Commands.literal("approval")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> mayorApprovalSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> mayorApprovalSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false))))
                                .then(Commands.literal("list")
                                        .executes(ctx -> mayorApprovalList(ctx.getSource())))))));
    }

    private static int civInfo(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        boolean mayor = data.isMayor(civId, target.getUUID());
        String leaderTitle = data.leaderTitle(civId);
        String leaderSuffix = mayor ? " (" + leaderTitle + ")" : "";
        if (source.hasPermission(3)) {
            source.sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " belongs to " + civDisplay(data, civId) + " [" + civId + "]"
                            + leaderSuffix),
                    false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " belongs to " + civDisplay(data, civId)
                            + leaderSuffix),
                    false);
        }
        return 1;
    }

    private static int civList(CommandSourceStack source) {
        CivSavedData data = CivSavedData.get(source.getServer());
        List<String> ids = data.civilizationIdsSorted();
        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No civilizations configured."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Civilizations:"), false);
        boolean showInternalId = source.hasPermission(3);
        for (String civId : ids) {
            CivilizationRecord civ = data.getCivilization(civId);
            String name = civ == null ? civId : civ.displayName();
            int plots = civ == null ? 0 : civ.plots().size();
            if (showInternalId) {
                source.sendSuccess(() -> Component.literal("- " + name + " [" + civId + "] | plots " + plots), false);
            } else {
                source.sendSuccess(() -> Component.literal("- " + name + " | plots " + plots), false);
            }
        }
        return 1;
    }

    private static int civTitleShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civRef == null ? civOfSource(source, data) : resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Leadership title for " + civDisplay(data, civId) + ": " + data.leaderTitle(civId)),
                false);
        return 1;
    }

    private static int civTitleSetSelf(CommandSourceStack source, String titleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can set civilization title."));
            return 0;
        }
        if (!data.setLeaderTitle(civId, titleRaw, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Title may already match."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Leadership title for " + civDisplay(data, civId) + " set to " + data.leaderTitle(civId) + "."),
                true);
        return 1;
    }

    private static int civTitleSetAdmin(CommandSourceStack source, String civRef, String titleRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        if (!data.setLeaderTitle(civId, titleRaw, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Title may already match."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Leadership title for " + civDisplay(data, civId) + " set to " + data.leaderTitle(civId) + "."),
                true);
        return 1;
    }

    private static int civTitleResetSelf(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return civTitleSetSelf(source, "Mayor");
    }

    private static int civTitleResetAdmin(CommandSourceStack source, String civRef) {
        return civTitleSetAdmin(source, civRef, "Mayor");
    }

    private static int civGovernanceShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civRef == null ? civOfSource(source, data) : resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        GovernanceModel model = data.governanceModel(civId);
        source.sendSuccess(() -> Component.literal(
                "Governance model for " + civDisplay(data, civId) + ": " + model.serializedName()),
                false);
        return 1;
    }

    private static int civGovernanceSetSelf(CommandSourceStack source, String modelRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can set governance model."));
            return 0;
        }
        @Nullable GovernanceModel model = GovernanceModel.fromSerializedName(modelRaw);
        if (model == null) {
            source.sendFailure(Component.literal("Unknown governance model. Use autocratic, council, or democratic."));
            return 0;
        }
        if (!data.setGovernanceModel(civId, model, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Governance model already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Governance model for " + civDisplay(data, civId) + " set to " + model.serializedName() + "."),
                true);
        return 1;
    }

    private static int civGovernanceSetAdmin(CommandSourceStack source, String civRef, String modelRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        @Nullable GovernanceModel model = GovernanceModel.fromSerializedName(modelRaw);
        if (model == null) {
            source.sendFailure(Component.literal("Unknown governance model. Use autocratic, council, or democratic."));
            return 0;
        }
        if (!data.setGovernanceModel(civId, model, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Governance model already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Governance model for " + civDisplay(data, civId) + " set to " + model.serializedName() + "."),
                true);
        return 1;
    }

    private static int civRoleList(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civRef == null ? civOfSource(source, data) : resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        List<CivRoleView> roles = data.customRolesSorted(civId);
        if (roles.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No custom roles configured for " + civDisplay(data, civId) + "."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(
                "Custom roles for " + civDisplay(data, civId) + ":"), false);
        for (CivRoleView role : roles) {
            String permissions = role.permissions().isEmpty()
                    ? "-"
                    : String.join(", ", role.permissions().stream().sorted().toList());
            String members = role.members().isEmpty()
                    ? "-"
                    : String.join(", ", role.members().stream()
                            .map(memberId -> playerNameOrShortId(source, memberId))
                            .toList());
            source.sendSuccess(() -> Component.literal(
                    "- " + role.displayName() + " [" + role.roleId() + "] | perms: " + permissions + " | members: " + members), false);
        }
        return 1;
    }

    private static int civRoleCreate(CommandSourceStack source, String roleRaw, String nameRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can create custom roles."));
            return 0;
        }
        @Nullable String roleId = CivSavedData.canonicalRoleId(roleRaw);
        if (roleId == null) {
            source.sendFailure(Component.literal("Invalid role id. Use letters, numbers, spaces, - or _."));
            return 0;
        }
        if (!data.createCustomRole(civId, roleId, nameRaw, actorName(source))) {
            source.sendFailure(Component.literal("Unable to create role. It may already exist."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Created custom role '" + nameRaw + "' [" + roleId + "] for " + civDisplay(data, civId) + "."),
                true);
        return 1;
    }

    private static int civRoleRename(CommandSourceStack source, String roleRaw, String nameRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can rename custom roles."));
            return 0;
        }
        if (!data.renameCustomRole(civId, roleRaw, nameRaw, actorName(source))) {
            source.sendFailure(Component.literal("Role rename failed. Check role id and new name."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Renamed role [" + roleRaw + "] to '" + nameRaw + "'."), true);
        return 1;
    }

    private static int civRoleDelete(CommandSourceStack source, String roleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can delete custom roles."));
            return 0;
        }
        if (!data.deleteCustomRole(civId, roleRaw, actorName(source))) {
            source.sendFailure(Component.literal("Role delete failed. Check role id."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Deleted role [" + roleRaw + "]."), true);
        return 1;
    }

    private static int civRolePermissionList(CommandSourceStack source, String roleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        List<CivRoleView> roles = data.customRolesSorted(civId);
        CivRoleView match = null;
        for (CivRoleView role : roles) {
            if (role.roleId().equals(CivSavedData.canonicalRoleId(roleRaw))) {
                match = role;
                break;
            }
        }
        if (match == null) {
            source.sendFailure(Component.literal("Role not found: " + roleRaw));
            return 0;
        }
        String permissions = match.permissions().isEmpty() ? "-" : String.join(", ", match.permissions());
        CivRoleView resolved = match;
        String permissionText = permissions;
        source.sendSuccess(() -> Component.literal(
                "Permissions for role '" + resolved.displayName() + "' [" + resolved.roleId() + "]: " + permissionText),
                false);
        return 1;
    }

    private static int civRolePermissionSet(CommandSourceStack source, String roleRaw, String permissionRaw, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage role permissions."));
            return 0;
        }
        @Nullable String permission = CivSavedData.canonicalRolePermission(permissionRaw);
        if (permission == null) {
            source.sendFailure(Component.literal("Invalid permission key."));
            return 0;
        }
        if (!data.setCustomRolePermission(civId, roleRaw, permission, allowed, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Check role id and permission key."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Granted " : "Revoked ")
                        + "permission '" + permission + "' for role [" + roleRaw + "]."),
                true);
        return 1;
    }

    private static int civRoleMemberList(CommandSourceStack source, String roleRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        List<CivRoleView> roles = data.customRolesSorted(civId);
        CivRoleView match = null;
        for (CivRoleView role : roles) {
            if (role.roleId().equals(CivSavedData.canonicalRoleId(roleRaw))) {
                match = role;
                break;
            }
        }
        if (match == null) {
            source.sendFailure(Component.literal("Role not found: " + roleRaw));
            return 0;
        }
        String members = match.members().isEmpty()
                ? "-"
                : String.join(", ", match.members().stream()
                        .map(memberId -> playerNameOrShortId(source, memberId))
                        .toList());
        CivRoleView resolved = match;
        String memberText = members;
        source.sendSuccess(() -> Component.literal(
                "Members of role '" + resolved.displayName() + "' [" + resolved.roleId() + "]: " + memberText),
                false);
        return 1;
    }

    private static int civRoleMemberSet(CommandSourceStack source, String roleRaw, ServerPlayer player, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage role members."));
            return 0;
        }
        if (allowed) {
            String targetCiv = data.getOrAssignCivilization(player.getUUID());
            if (!targetCiv.equals(civId)) {
                source.sendFailure(Component.literal("Player must be a member of your civilization."));
                return 0;
            }
        }
        if (!data.setCustomRoleMember(civId, roleRaw, player.getUUID(), allowed, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Check role id and member status."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Added " : "Removed ")
                        + player.getGameProfile().getName() + (allowed ? " to " : " from ")
                        + "role [" + roleRaw + "]."),
                true);
        return 1;
    }

    private static int civCreate(CommandSourceStack source, String nameRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String displayName = nameRaw == null ? "" : nameRaw.trim();
        if (displayName.isEmpty()) {
            source.sendFailure(Component.literal("Civilization name cannot be empty."));
            return 0;
        }
        if (data.findCivilizationIdByDisplayName(displayName) != null) {
            source.sendFailure(Component.literal("A civilization with that name already exists."));
            return 0;
        }

        String id = data.suggestCivilizationId(displayName);
        if (!data.createCivilization(id, displayName, actorName(source))) {
            source.sendFailure(Component.literal("Unable to create civilization. Name or internal id may already exist."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Created civilization '" + displayName + "'. Internal id: " + id + "."), true);
        return 1;
    }

    private static int civFound(CommandSourceStack source, String nameRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer founder = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!canFoundCivilization(source, data, founder)) {
            source.sendFailure(Component.literal(
                    "You are not approved to found a civilization. Ask an admin to run "
                            + "/realciv mayor approval add " + founder.getGameProfile().getName() + "."));
            return 0;
        }

        String displayName = nameRaw == null ? "" : nameRaw.trim();
        if (displayName.isEmpty()) {
            source.sendFailure(Component.literal("Civilization name cannot be empty."));
            return 0;
        }
        if (data.findCivilizationIdByDisplayName(displayName) != null) {
            source.sendFailure(Component.literal("A civilization with that name already exists."));
            return 0;
        }

        String id = data.suggestCivilizationId(displayName);
        if (!data.createCivilization(id, displayName, founder.getGameProfile().getName())) {
            source.sendFailure(Component.literal("Unable to found civilization. Name or internal id may already exist."));
            return 0;
        }

        data.setPlayerCivilization(founder.getUUID(), id, founder.getGameProfile().getName());
        data.setMayor(id, founder.getUUID(), founder.getGameProfile().getName());
        if (RealCivConfig.requireFounderApproval() && !source.hasPermission(3)) {
            data.consumeFounderApproval(founder.getUUID(), founder.getGameProfile().getName());
        }
        grantMayorStarterHub(founder);
        source.sendSuccess(() -> Component.literal(
                "You founded '" + displayName + "' and are now its " + data.leaderTitle(id).toLowerCase(Locale.ROOT) + "."), true);
        return 1;
    }

    private static int civRename(CommandSourceStack source, String civRef, String name) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        if (!data.renameCivilization(civId, name, actorName(source))) {
            source.sendFailure(Component.literal("Unable to rename civilization. Name may already be taken."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Renamed civilization '" + civRef + "' to '" + name + "'."), true);
        return 1;
    }

    private static int civDelete(CommandSourceStack source, String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }

        String defaultCiv = RealCivConfig.defaultCivilizationId();
        if (civId.equals(defaultCiv)) {
            source.sendFailure(Component.literal("Cannot delete the default civilization."));
            return 0;
        }

        DeleteCivilizationResult result = data.deleteCivilization(civId, defaultCiv, actorName(source));
        if (result == null) {
            source.sendFailure(Component.literal("Unable to delete civilization."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Deleted '" + result.deletedDisplayName() + "' [" + result.deletedId() + "]. "
                        + "Reassigned members: " + result.reassignedMembers()
                        + ", migrated accounts: " + result.migratedAccounts()
                        + ", transferred stock entries: " + result.transferredStockEntries()
                        + " (" + result.transferredStockItems() + " items), removed plots: " + result.removedPlots() + "."),
                true);
        return 1;
    }

    private static int civJoin(CommandSourceStack source, String civRef)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        String currentCiv = data.getOrAssignCivilization(player.getUUID());
        if (currentCiv.equals(civId)) {
            source.sendFailure(Component.literal("You are already in " + civDisplay(data, civId) + "."));
            return 0;
        }

        boolean hasInvite = data.hasInvite(civId, player.getUUID());
        boolean canDirectJoin = source.hasPermission(3) || hasInvite;
        if (canDirectJoin) {
            if (!data.setPlayerCivilization(player.getUUID(), civId, actorName(source))) {
                source.sendFailure(Component.literal("Failed to join civilization."));
                return 0;
            }
            String newCiv = data.getOrAssignCivilization(player.getUUID());
            source.sendSuccess(() -> Component.literal(
                    "You are now a citizen of " + civDisplay(data, newCiv) + "."), true);
            return 1;
        }

        boolean created = data.addJoinRequest(civId, player.getUUID(), actorName(source));
        if (!created) {
            source.sendSuccess(() -> Component.literal(
                    "Your join request is already pending for " + civDisplay(data, civId) + "."), false);
            return 1;
        }

        @Nullable UUID mayorId = data.getMayorId(civId);
        if (mayorId != null) {
            ServerPlayer mayorOnline = source.getServer().getPlayerList().getPlayer(mayorId);
            if (mayorOnline != null) {
                mayorOnline.sendSystemMessage(Component.literal(
                        player.getGameProfile().getName() + " requested to join " + civDisplay(data, civId)
                                + ". Use Census Block UI to approve/deny."));
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Join request submitted to " + civDisplay(data, civId)
                        + ". Wait for mayor/manager approval at Census."), true);
        return 1;
    }

    private static int civLeave(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String defaultCiv = RealCivConfig.defaultCivilizationId();
        if (!data.setPlayerCivilization(player.getUUID(), defaultCiv, actorName(source))) {
            source.sendFailure(Component.literal("Unable to leave civilization."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "You are now unaligned (" + civDisplay(data, defaultCiv) + ")."), true);
        return 1;
    }

    private static int civAssign(CommandSourceStack source, ServerPlayer target, String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveCivilizationId(data, civRef);
        if (civId == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civRef));
            return 0;
        }
        if (!data.setPlayerCivilization(target.getUUID(), civId, actorName(source))) {
            source.sendFailure(Component.literal("Failed to assign player to civilization."));
            return 0;
        }
        String newCiv = data.getOrAssignCivilization(target.getUUID());
        source.sendSuccess(() -> Component.literal(
                "Assigned " + target.getGameProfile().getName()
                        + " to " + civDisplay(data, newCiv) + "."),
                true);
        return 1;
    }

    private static int civDiplomacyShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = civOfSource(source, data);
        } else {
            civId = resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Diplomacy for " + civDisplay(data, civId) + " [" + civId + "]"), false);
        source.sendSuccess(() -> Component.literal(
                "Intra-civ PvP (friendly fire): " + (data.allowIntraCivPvp(civId) ? "ENABLED" : "DISABLED")), false);

        List<DiplomacyView> relations = data.nonNeutralDiplomacyEntriesFor(civId);
        if (relations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("All external relations are currently NEUTRAL."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Non-neutral relations:"), false);
        for (DiplomacyView relation : relations) {
            String other = relation.otherCivilizationId();
            source.sendSuccess(() -> Component.literal(
                    "- " + civDisplay(data, other) + " [" + other + "]: " + relation.state().displayName()), false);
        }
        return 1;
    }

    private static int civDiplomacySet(CommandSourceStack source, String otherCivRef, String stateRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = civOfSource(source, data);
        if (!hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY)) {
            source.sendFailure(Component.literal("Only leadership/admin can change diplomacy for your civilization."));
            return 0;
        }

        String otherCiv = resolveCivilizationId(data, otherCivRef);
        if (otherCiv == null) {
            source.sendFailure(Component.literal("Civilization not found: " + otherCivRef));
            return 0;
        }
        if (actorCiv.equals(otherCiv)) {
            source.sendFailure(Component.literal("Use /realciv civ pvp friendlyfire on|off to control same-civ PvP."));
            return 0;
        }

        @Nullable DiplomacyState state = parseDiplomacyState(stateRaw);
        if (state == null) {
            source.sendFailure(Component.literal("Invalid diplomacy state. Use ally, neutral, or war."));
            return 0;
        }

        if (!data.setDiplomacyState(actorCiv, otherCiv, state, actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Diplomacy may already be " + state.displayName() + "."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Set diplomacy between " + civDisplay(data, actorCiv) + " and "
                        + civDisplay(data, otherCiv) + " to " + state.displayName() + "."), true);
        return 1;
    }

    private static int civDiplomacySetBetween(CommandSourceStack source, String civARef, String civBRef, String stateRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civA = resolveCivilizationId(data, civARef);
        if (civA == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civARef));
            return 0;
        }
        String civB = resolveCivilizationId(data, civBRef);
        if (civB == null) {
            source.sendFailure(Component.literal("Civilization not found: " + civBRef));
            return 0;
        }
        if (civA.equals(civB)) {
            source.sendFailure(Component.literal("Cannot set diplomacy between the same civilization."));
            return 0;
        }

        @Nullable DiplomacyState state = parseDiplomacyState(stateRaw);
        if (state == null) {
            source.sendFailure(Component.literal("Invalid diplomacy state. Use ally, neutral, or war."));
            return 0;
        }
        if (!data.setDiplomacyState(civA, civB, state, actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Diplomacy may already be " + state.displayName() + "."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Set diplomacy between " + civDisplay(data, civA) + " and "
                        + civDisplay(data, civB) + " to " + state.displayName() + "."), true);
        return 1;
    }

    private static int civFriendlyFireShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = civOfSource(source, data);
        } else {
            civId = resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.literal(
                "Intra-civ PvP for " + civDisplay(data, civId) + ": "
                        + (data.allowIntraCivPvp(civId) ? "ENABLED" : "DISABLED")), false);
        return 1;
    }

    private static int civFriendlyFireSet(CommandSourceStack source, boolean allowed) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE)) {
            source.sendFailure(Component.literal("Only leadership/admin can change friendly-fire settings."));
            return 0;
        }
        if (!data.setAllowIntraCivPvp(civId, allowed, actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Friendly fire is already " + (allowed ? "enabled" : "disabled") + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Friendly fire for " + civDisplay(data, civId) + " is now " + (allowed ? "ENABLED" : "DISABLED") + "."), true);
        return 1;
    }

    private static int civExplosivesShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = civOfSource(source, data);
        } else {
            civId = resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        int cap = RealCivConfig.maxExplosivesExpertsPerCivilization();
        List<UUID> experts = data.explosivesExpertsSorted(civId);
        source.sendSuccess(() -> Component.literal(
                "Explosives experts for " + civDisplay(data, civId) + " [" + civId + "]: "
                        + experts.size() + "/" + cap),
                false);
        if (cap <= 0) {
            source.sendSuccess(() -> Component.literal(
                    "Role is disabled by server config (civ.maxExplosivesExpertsPerCivilization=0)."), false);
        }
        if (experts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No members are designated as explosives experts."), false);
            return 1;
        }
        for (UUID expert : experts) {
            source.sendSuccess(() -> Component.literal("- " + playerNameOrShortId(source, expert) + " | " + expert), false);
        }
        return 1;
    }

    private static int civExplosivesSet(CommandSourceStack source, ServerPlayer target, boolean allowed) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_EXPLOSIVES)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage explosives experts."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!civId.equals(targetCiv)) {
            source.sendFailure(Component.literal(
                    "Target player is not in your civilization. Current civ: " + civDisplay(data, targetCiv) + "."));
            return 0;
        }

        int cap = RealCivConfig.maxExplosivesExpertsPerCivilization();
        if (allowed) {
            if (cap <= 0) {
                source.sendFailure(Component.literal(
                        "Explosives Expert role is disabled by server config for all civilizations."));
                return 0;
            }
            if (!data.isExplosivesExpert(civId, target.getUUID())
                    && data.explosivesExpertCount(civId) >= cap) {
                source.sendFailure(Component.literal(
                        "Cannot add more explosives experts. Cap reached (" + cap + ")."));
                return 0;
            }
        }

        if (!data.setExplosivesExpert(civId, target.getUUID(), allowed, actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Player is already " + (allowed ? "" : "not ") + "an explosives expert."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                (allowed ? "Designated " : "Removed ")
                        + target.getGameProfile().getName()
                        + (allowed ? " as an" : " from")
                        + " explosives expert for " + civDisplay(data, civId) + "."),
                true);
        return 1;
    }

    private static int civRedstonerShow(CommandSourceStack source, @Nullable String civRef) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId;
        if (civRef == null || civRef.isBlank()) {
            civId = civOfSource(source, data);
        } else {
            civId = resolveCivilizationId(data, civRef);
            if (civId == null) {
                source.sendFailure(Component.literal("Civilization not found: " + civRef));
                return 0;
            }
        }

        int cap = RealCivConfig.maxRedstonersPerCivilization();
        List<UUID> redstoners = data.redstonersSorted(civId);
        source.sendSuccess(() -> Component.literal(
                "Redstoners for " + civDisplay(data, civId) + " [" + civId + "]: "
                        + redstoners.size() + "/" + cap),
                false);
        if (cap <= 0) {
            source.sendSuccess(() -> Component.literal(
                    "Role is disabled by server config (civ.maxRedstonersPerCivilization=0)."), false);
        }
        if (redstoners.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No members are designated as redstoners."), false);
            return 1;
        }
        for (UUID redstoner : redstoners) {
            source.sendSuccess(() -> Component.literal("- " + playerNameOrShortId(source, redstoner) + " | " + redstoner), false);
        }
        return 1;
    }

    private static int civRedstonerSet(CommandSourceStack source, ServerPlayer target, boolean allowed) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_REDSTONERS)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage redstoners."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!civId.equals(targetCiv)) {
            source.sendFailure(Component.literal(
                    "Target player is not in your civilization. Current civ: " + civDisplay(data, targetCiv) + "."));
            return 0;
        }

        int cap = RealCivConfig.maxRedstonersPerCivilization();
        if (allowed) {
            if (cap <= 0) {
                source.sendFailure(Component.literal(
                        "Redstoner role is disabled by server config for all civilizations."));
                return 0;
            }
            if (!data.isRedstoner(civId, target.getUUID())
                    && data.redstonerCount(civId) >= cap) {
                source.sendFailure(Component.literal(
                        "Cannot add more redstoners. Cap reached (" + cap + ")."));
                return 0;
            }
        }

        if (!data.setRedstoner(civId, target.getUUID(), allowed, actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Player is already " + (allowed ? "" : "not ") + "a redstoner."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                (allowed ? "Designated " : "Removed ")
                        + target.getGameProfile().getName()
                        + (allowed ? " as a" : " from")
                        + " redstoner for " + civDisplay(data, civId) + "."),
                true);
        return 1;
    }

    public static void openLandGuiForPlayer(ServerPlayer player, CivSavedData data) {
        if (player.getServer() != null) {
            RealCivFTBChunksMirror.syncAll(player.getServer(), data);
        }
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean mayorOrAdmin = player.hasPermissions(3)
                || data.isMayor(civId, player.getUUID())
                || data.hasCustomRolePermission(civId, player.getUUID(), CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE);
        String effectiveMode = RealCivFTBChunksBridge.effectiveClaimModeLabel(
                mayorOrAdmin,
                data.getOrCreatePlayer(player.getUUID()).ftbClaimModeOverride());

        if (RealCivFTBChunksBridge.tryOpenClaimMap(player)) {
            player.sendSystemMessage(Component.literal(
                    "FTB chunk map opened. RealCiv mode: " + effectiveMode.toUpperCase(Locale.ROOT)
                            + ". Use /realciv land ftb-mode <auto|civic|private> if you have leadership permissions."));
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new LandClaimMenu(containerId, playerInventory, player, data, civId),
                Component.literal("Land Claims")));
        player.sendSystemMessage(Component.literal(
                "Fallback land claim map opened (FTB Chunks map unavailable)."));
    }

    public static long countInventoryItem(ServerPlayer player, Item item) {
        long total = 0L;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return Math.max(0L, total);
    }

    public static void removeInventoryItem(ServerPlayer player, Item item, long count) {
        long remaining = Math.max(0L, count);
        if (remaining <= 0L) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0L) {
                break;
            }
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int remove = (int) Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (remaining <= 0L) {
                break;
            }
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int remove = (int) Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }
    }





    private static int mayorSet(CommandSourceStack source, ServerPlayer player, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveMayorCivId(source, data, civRaw);
        data.setMayor(civId, player.getUUID(), actorName(source));
        grantMayorStarterHub(player);
        String title = data.leaderTitle(civId);
        source.sendSuccess(
                () -> Component.literal(title + " for " + civDisplay(data, civId)
                        + " set to " + player.getGameProfile().getName() + "."),
                true);
        return 1;
    }

    private static int mayorClear(CommandSourceStack source, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveMayorCivId(source, data, civRaw);
        data.setMayor(civId, null, actorName(source));
        String title = data.leaderTitle(civId);
        source.sendSuccess(() -> Component.literal(
                title + " assignment cleared for " + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int mayorShow(CommandSourceStack source, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveMayorCivId(source, data, civRaw);
        String title = data.leaderTitle(civId);
        UUID mayor = data.getMayorId(civId);
        if (mayor == null) {
            source.sendSuccess(() -> Component.literal(
                    "No " + title.toLowerCase(Locale.ROOT) + " is assigned for " + civDisplay(data, civId) + "."), false);
            return 1;
        }

        ServerPlayer online = source.getServer().getPlayerList().getPlayer(mayor);
        String name = online == null ? mayor.toString() : online.getGameProfile().getName();
        source.sendSuccess(() -> Component.literal(
                "Current " + title.toLowerCase(Locale.ROOT) + " for " + civDisplay(data, civId) + ": " + name), false);
        return 1;
    }

    private static int mayorWithdrawRateShow(CommandSourceStack source, ServerPlayer player) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES)) {
            source.sendFailure(Component.literal("Only leadership/admin can view per-player withdraw rates."));
            return 0;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        String rateText = RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId));
        String mode = record.personalWithdrawRatioOverride(civId) == null ? "default" : "override";
        source.sendSuccess(() -> Component.literal(
                "Withdrawal rate for " + player.getGameProfile().getName()
                        + " in " + civDisplay(data, civId) + ": " + rateText + " (" + mode + ")"),
                false);
        if (data.hubDistributionMode(civId) == HubDistributionMode.DAILY_ALLOWANCE) {
            source.sendSuccess(() -> Component.literal(
                    "Note: hub distribution mode is daily_allowance, so withdraw rate does not currently apply."),
                    false);
        }
        return 1;
    }

    private static int mayorWithdrawRateSet(CommandSourceStack source, ServerPlayer player, double percent) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES)) {
            source.sendFailure(Component.literal("Only leadership/admin can set per-player withdraw rates."));
            return 0;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        double ratio = Math.max(0.0D, Math.min(1.0D, percent / 100.0D));
        record.setPersonalWithdrawRatioOverride(civId, ratio);
        data.addAuditLog(
                civId,
                actorName(source) + " set personal withdraw rate for " + player.getGameProfile().getName()
                        + " to " + RealCivUtil.formatPercentFromRatio(ratio),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Set withdrawal rate for " + player.getGameProfile().getName()
                        + " in " + civDisplay(data, civId)
                        + " to " + RealCivUtil.formatPercentFromRatio(ratio) + "."), true);
        if (data.hubDistributionMode(civId) == HubDistributionMode.DAILY_ALLOWANCE) {
            source.sendSuccess(() -> Component.literal(
                    "Current hub mode is daily_allowance; this rate will apply if switched back to contribution_ratio."),
                    false);
        }
        return 1;
    }

    private static int mayorWithdrawRateClear(CommandSourceStack source, ServerPlayer player) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear per-player withdraw rates."));
            return 0;
        }

        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        record.setPersonalWithdrawRatioOverride(civId, null);
        data.addAuditLog(
                civId,
                actorName(source) + " cleared personal withdraw rate override for "
                        + player.getGameProfile().getName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Cleared withdrawal rate override for " + player.getGameProfile().getName()
                        + " in " + civDisplay(data, civId) + "."), true);
        if (data.hubDistributionMode(civId) == HubDistributionMode.DAILY_ALLOWANCE) {
            source.sendSuccess(() -> Component.literal(
                    "Current hub mode is daily_allowance; withdraw rates are inactive until contribution_ratio mode is used."),
                    false);
        }
        return 1;
    }

    private static int mayorApprovalSet(CommandSourceStack source, ServerPlayer player, boolean approved) {
        CivSavedData data = CivSavedData.get(source.getServer());
        data.setFounderApproved(player.getUUID(), approved, actorName(source));
        source.sendSuccess(() -> Component.literal(
                (approved ? "Approved " : "Revoked approval for ")
                        + player.getGameProfile().getName()
                        + (approved ? " to found a civilization." : " as civilization founder.")),
                true);
        return 1;
    }

    private static int mayorApprovalList(CommandSourceStack source) {
        CivSavedData data = CivSavedData.get(source.getServer());
        List<UUID> approved = data.founderApprovalsSorted();
        if (approved.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No founder approvals are currently set."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Founder approvals:"), false);
        for (UUID id : approved) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
            String label = online != null ? online.getGameProfile().getName() + " (" + id + ")" : id.toString();
            source.sendSuccess(() -> Component.literal("- " + label), false);
        }
        return 1;
    }

    public static long nextTownClaimCostCents(int civicChunksOwned) {
        long base = RealCivConfig.townClaimCostCents();
        long extra = RealCivConfig.townClaimCostAddedPerOwnedCents() * Math.max(0, civicChunksOwned);
        return Math.max(0L, base + extra);
    }

    public static boolean ensureClaimDimensionAllowed(CommandSourceStack source, String dimension) {
        if (source.hasPermission(3)) {
            return true;
        }
        if (RealCivConfig.canClaimDimension(dimension)) {
            return true;
        }
        source.sendFailure(Component.literal(
                "Land claiming is disabled in dimension '" + dimension + "' by server policy "
                        + "(" + RealCivConfig.claimDimensionPolicyLabel() + ")."));
        return false;
    }

    public static long nextPrivateClaimCostCents(int privateOwnedByPlayer) {
        long base = RealCivConfig.rentCostCents();
        long extra = RealCivConfig.rentCostAddedPerOwnedPrivateCents() * Math.max(0, privateOwnedByPlayer);
        return Math.max(0L, base + extra);
    }

    public static char mapSymbolForChunk(
            CivSavedData data,
            String civId,
            UUID viewerId,
            String dimension,
            long chunkX,
            long chunkZ,
            long centerX,
            long centerZ) {
        if (chunkX == centerX && chunkZ == centerZ) {
            return '@';
        }
        @Nullable PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup == null) {
            return '.';
        }
        if (!lookup.civilizationId().equals(civId)) {
            return 'x';
        }
        return switch (lookup.plot().landClass()) {
            case CIVIC -> 'C';
            case PRIVATE -> lookup.plot().ownerId() != null && lookup.plot().ownerId().equals(viewerId) ? 'P' : 'p';
            case COMMUNITY -> 'm';
        };
    }

    public static boolean isWithinOrAdjacentToTown(
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

    public static boolean isTownChunk(
            CivSavedData data,
            String civId,
            String dimension,
            long chunkX,
            long chunkZ) {
        @Nullable PlotRecord plot = data.getPlot(civId, dimension, chunkX, chunkZ);
        return plot != null && plot.landClass() == LandClass.CIVIC;
    }

    public static String civOfSource(CommandSourceStack source, CivSavedData data) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return data.getOrAssignCivilization(player.getUUID());
        }
        return RealCivConfig.defaultCivilizationId();
    }

    public static boolean canFoundCivilization(CommandSourceStack source, CivSavedData data, ServerPlayer founder) {
        if (source.hasPermission(3)) {
            return true;
        }
        if (!RealCivConfig.requireFounderApproval()) {
            return true;
        }
        return data.isFounderApproved(founder.getUUID());
    }

    @Nullable
    public static String resolveCivilizationId(CivSavedData data, String civRaw) {
        if (civRaw == null || civRaw.isBlank()) {
            return null;
        }
        CivilizationRecord byId = data.getCivilization(civRaw);
        if (byId != null) {
            return byId.id();
        }
        return data.findCivilizationIdByDisplayName(civRaw);
    }

    public static String resolveMayorCivId(CommandSourceStack source, CivSavedData data, @Nullable String civRaw) {
        if (civRaw == null || civRaw.isBlank()) {
            return civOfSource(source, data);
        }
        String resolved = resolveCivilizationId(data, civRaw);
        return resolved == null ? civOfSource(source, data) : resolved;
    }

    @Nullable
    public static DiplomacyState parseDiplomacyState(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return DiplomacyState.fromSerializedName(raw);
    }

    public static String civDisplay(CivSavedData data, String civId) {
        CivilizationRecord civ = data.getCivilization(civId);
        if (civ == null) {
            return civId;
        }
        return civ.displayName();
    }

    public static String playerNameOrShortId(CommandSourceStack source, UUID playerId) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        String raw = playerId.toString();
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }

    public static boolean hasCivPermission(
            CommandSourceStack source,
            CivSavedData data,
            String civId,
            String permissionKey) {
        return CivPermissionService.hasCivPermission(source, data, civId, permissionKey);
    }

    public static boolean isMayorOrAdmin(CommandSourceStack source, CivSavedData data, String civId) {
        return CivPermissionService.isMayorOrAdmin(source, data, civId);
    }

    public static void grantMayorStarterHub(ServerPlayer player) {
        List<StarterItem> starterItems = List.of(
                new StarterItem(ModBlocks.COMMUNITY_HUB_ITEM.get(), "Community Hub"),
                new StarterItem(ModBlocks.CENSUS_BLOCK_ITEM.get(), "Census Block"),
                new StarterItem(ModBlocks.TAX_BLOCK_ITEM.get(), "Tax Block"),
                new StarterItem(ModBlocks.CIVIC_CONTROL_CONSOLE_ITEM.get(), "Civic Control Console"),
                new StarterItem(ModBlocks.PROFESSION_LEDGER_ITEM.get(), "Profession Ledger"),
                new StarterItem(ModBlocks.WAR_TABLE_ITEM.get(), "Diplomacy Table"),
                new StarterItem(ModBlocks.LAND_WAND.get(), "Land Wand"));

        List<String> granted = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();

        for (StarterItem starterItem : starterItems) {
            if (starterItem.item() == Items.AIR) {
                unavailable.add(starterItem.label());
                continue;
            }
            if (inventoryHasItem(player, starterItem.item())) {
                continue;
            }

            ItemStack stack = new ItemStack(starterItem.item(), 1);
            ItemStack copy = stack.copy();
            boolean added = player.getInventory().add(copy);
            if (!added) {
                player.drop(copy, false);
                dropped.add(starterItem.label());
            } else {
                granted.add(starterItem.label());
            }
        }

        if (!granted.isEmpty() || !dropped.isEmpty()) {
            StringBuilder msg = new StringBuilder("Mayor starter kit update.");
            if (!granted.isEmpty()) {
                msg.append(" Added: ").append(String.join(", ", granted)).append(".");
            }
            if (!dropped.isEmpty()) {
                msg.append(" Dropped near you (inventory full): ").append(String.join(", ", dropped)).append(".");
            }
            player.sendSystemMessage(Component.literal(msg.toString()));
        } else {
            player.sendSystemMessage(Component.literal("Mayor starter kit unchanged (all civic items already present)."));
        }
        if (!unavailable.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Starter kit warning: unavailable registry entries: " + String.join(", ", unavailable) + "."));
        }
    }

    public static boolean inventoryHasItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private record StarterItem(Item item, String label) {
    }

    public static String actorName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getGameProfile().getName();
        }
        return "Console";
    }
}

