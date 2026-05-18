package com.realciv.realciv.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
                                .executes(ctx -> CivCommands.civInfo(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> CivCommands.civInfo(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("list")
                                .executes(ctx -> CivCommands.civList(ctx.getSource())))
                        .then(Commands.literal("title")
                                .then(Commands.literal("show")
                                        .executes(ctx -> CivCommands.civTitleShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civTitleShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                .executes(ctx -> CivCommands.civTitleSetSelf(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "title"))))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(3))
                                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                                        .executes(ctx -> CivCommands.civTitleSetAdmin(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "civ"),
                                                                StringArgumentType.getString(ctx, "title"))))))
                                .then(Commands.literal("reset")
                                        .executes(ctx -> CivCommands.civTitleResetSelf(ctx.getSource()))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(3))
                                                .executes(ctx -> CivCommands.civTitleResetAdmin(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ"))))))
                        .then(Commands.literal("governance")
                                .then(Commands.literal("show")
                                        .executes(ctx -> CivCommands.civGovernanceShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civGovernanceShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("category", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of("executive", "succession", "resource", "taxation", "membership", "land"),
                                                        builder))
                                                .then(Commands.argument("attribute", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                List.of("direct_rule", "council_vote", "popular_vote",
                                                                        "appointed", "election", "coup",
                                                                        "contribution_share", "equal_share", "rationed",
                                                                        "karma_tax", "goods_tax", "exempt",
                                                                        "open", "invite_only", "application",
                                                                        "open_claim", "leader_claim", "taxed_claim"),
                                                                builder))
                                                        .executes(ctx -> CivCommands.civAttributeSetSelf(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "category"),
                                                                StringArgumentType.getString(ctx, "attribute")))))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(3))
                                                .then(Commands.argument("category", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                List.of("executive", "succession", "resource", "taxation", "membership", "land"),
                                                                builder))
                                                        .then(Commands.argument("attribute", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                        List.of("direct_rule", "council_vote", "popular_vote",
                                                                                "appointed", "election", "coup",
                                                                                "contribution_share", "equal_share", "rationed",
                                                                                "karma_tax", "goods_tax", "exempt",
                                                                                "open", "invite_only", "application",
                                                                                "open_claim", "leader_claim", "taxed_claim"),
                                                                        builder))
                                                                .executes(ctx -> CivCommands.civAttributeSetAdmin(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "civ"),
                                                                        StringArgumentType.getString(ctx, "category"),
                                                                        StringArgumentType.getString(ctx, "attribute"))))))))
                        .then(Commands.literal("role")
                                .then(Commands.literal("list")
                                        .executes(ctx -> CivCommands.civRoleList(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civRoleList(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("create")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .executes(ctx -> CivCommands.civRoleCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "role"),
                                                        StringArgumentType.getString(ctx, "role")))
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> CivCommands.civRoleCreate(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("rename")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> CivCommands.civRoleRename(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role"),
                                                                StringArgumentType.getString(ctx, "name"))))))
                                .then(Commands.literal("delete")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .executes(ctx -> CivCommands.civRoleDelete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "role")))))
                                .then(Commands.literal("permission")
                                        .then(Commands.literal("list")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .executes(ctx -> CivCommands.civRolePermissionList(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role")))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .then(Commands.argument("permission", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                        CivSavedData.knownRolePermissions(),
                                                                        builder))
                                                                .executes(ctx -> CivCommands.civRolePermissionSet(
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
                                                                .executes(ctx -> CivCommands.civRolePermissionSet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "role"),
                                                                        StringArgumentType.getString(ctx, "permission"),
                                                                        false))))))
                                .then(Commands.literal("member")
                                        .then(Commands.literal("list")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .executes(ctx -> CivCommands.civRoleMemberList(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "role")))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .then(Commands.argument("player", EntityArgument.player())
                                                                .executes(ctx -> CivCommands.civRoleMemberSet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "role"),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        true)))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("role", StringArgumentType.word())
                                                        .then(Commands.argument("player", EntityArgument.player())
                                                                .executes(ctx -> CivCommands.civRoleMemberSet(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "role"),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        false)))))))
                        .then(Commands.literal("found")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> CivCommands.civFound(
                                                ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("create")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> CivCommands.civCreate(
                                                ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("rename")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> CivCommands.civRename(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ"),
                                                        StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("delete")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> CivCommands.civDelete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("join")
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> CivCommands.civJoin(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("leave")
                                .executes(ctx -> CivCommands.civLeave(ctx.getSource())))
                        .then(Commands.literal("assign")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .executes(ctx -> CivCommands.civAssign(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "civ"))))))
                        .then(Commands.literal("diplomacy")
                                .then(Commands.literal("show")
                                        .executes(ctx -> CivCommands.civDiplomacyShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civDiplomacyShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("other", StringArgumentType.string())
                                                .then(Commands.argument("state", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                List.of("ally", "neutral", "war"),
                                                                builder))
                                                        .executes(ctx -> CivCommands.civDiplomacySet(
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
                                                                .executes(ctx -> CivCommands.civDiplomacySetBetween(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "civA"),
                                                                        StringArgumentType.getString(ctx, "civB"),
                                                                        StringArgumentType.getString(ctx, "state"))))))))
                                .then(Commands.literal("accept")
                                        .then(Commands.argument("other", StringArgumentType.string())
                                                .executes(ctx -> CivCommands.civDiplomacyAccept(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "other")))))
                                .then(Commands.literal("reject")
                                        .then(Commands.argument("other", StringArgumentType.string())
                                                .executes(ctx -> CivCommands.civDiplomacyReject(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "other")))))
                        .then(Commands.literal("war")
                                .then(Commands.literal("show")
                                        .executes(ctx -> CivCommands.civWarShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civWarShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("declare")
                                        .then(Commands.literal("destruction")
                                                .then(Commands.argument("other", StringArgumentType.string())
                                                        .executes(ctx -> CivCommands.civWarDeclare(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "other"),
                                                                "destruction",
                                                                RealCivConfig.defaultWarPvpKillTarget(),
                                                                false,
                                                                false))
                                                        .then(Commands.argument("submission", BoolArgumentType.bool())
                                                                .executes(ctx -> CivCommands.civWarDeclare(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "other"),
                                                                        "destruction",
                                                                        RealCivConfig.defaultWarPvpKillTarget(),
                                                                        BoolArgumentType.getBool(ctx, "submission"),
                                                                        false))
                                                                .then(Commands.argument("land", BoolArgumentType.bool())
                                                                        .executes(ctx -> CivCommands.civWarDeclare(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "other"),
                                                                                "destruction",
                                                                                RealCivConfig.defaultWarPvpKillTarget(),
                                                                                BoolArgumentType.getBool(ctx, "submission"),
                                                                                BoolArgumentType.getBool(ctx, "land")))))))
                                        .then(Commands.literal("pvp")
                                                .then(Commands.argument("other", StringArgumentType.string())
                                                        .then(Commands.argument("killTarget", IntegerArgumentType.integer(1, 100_000))
                                                                .executes(ctx -> CivCommands.civWarDeclare(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "other"),
                                                                        "pvp",
                                                                        IntegerArgumentType.getInteger(ctx, "killTarget"),
                                                                        false,
                                                                        false))
                                                                .then(Commands.argument("submission", BoolArgumentType.bool())
                                                                        .executes(ctx -> CivCommands.civWarDeclare(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "other"),
                                                                                "pvp",
                                                                                IntegerArgumentType.getInteger(ctx, "killTarget"),
                                                                                BoolArgumentType.getBool(ctx, "submission"),
                                                                                false))
                                                                        .then(Commands.argument("land", BoolArgumentType.bool())
                                                                                .executes(ctx -> CivCommands.civWarDeclare(
                                                                                        ctx.getSource(),
                                                                                        StringArgumentType.getString(ctx, "other"),
                                                                                        "pvp",
                                                                                        IntegerArgumentType.getInteger(ctx, "killTarget"),
                                                                                        BoolArgumentType.getBool(ctx, "submission"),
                                                                                        BoolArgumentType.getBool(ctx, "land")))))))))
                                .then(Commands.literal("accept")
                                        .then(Commands.argument("other", StringArgumentType.string())
                                                .executes(ctx -> CivCommands.civWarAccept(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "other")))))
                                .then(Commands.literal("reject")
                                        .then(Commands.argument("other", StringArgumentType.string())
                                                .executes(ctx -> CivCommands.civWarReject(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "other")))))
                                .then(Commands.literal("resign")
                                        .then(Commands.argument("other", StringArgumentType.string())
                                                .executes(ctx -> CivCommands.civWarResign(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "other"))))))
                        .then(Commands.literal("pvp")
                                .then(Commands.literal("show")
                                        .executes(ctx -> CivCommands.civFriendlyFireShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civFriendlyFireShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("friendlyfire")
                                        .then(Commands.literal("on")
                                                .executes(ctx -> CivCommands.civFriendlyFireSet(ctx.getSource(), true)))
                                        .then(Commands.literal("off")
                                                .executes(ctx -> CivCommands.civFriendlyFireSet(ctx.getSource(), false)))))
                        .then(Commands.literal("explosives")
                                .then(Commands.literal("show")
                                        .executes(ctx -> CivCommands.civExplosivesShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civExplosivesShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> CivCommands.civExplosivesSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> CivCommands.civExplosivesSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false)))))
                        .then(Commands.literal("redstoner")
                                .then(Commands.literal("show")
                                        .executes(ctx -> CivCommands.civRedstonerShow(ctx.getSource(), null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> CivCommands.civRedstonerShow(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "civ")))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> CivCommands.civRedstonerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> CivCommands.civRedstonerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false))))))
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
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(3))
                                        .executes(ctx -> LandCommands.landWandGive(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
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
                                                        List.of("contribution_share", "equal_share", "rationed"),
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
                                .executes(ctx -> MayorCommands.mayorShow(ctx.getSource(), null))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> MayorCommands.mayorShow(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> MayorCommands.mayorSet(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                null))
                                        .then(Commands.argument("civ", StringArgumentType.string())
                                                .executes(ctx -> MayorCommands.mayorSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "civ"))))))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(3))
                                .executes(ctx -> MayorCommands.mayorClear(ctx.getSource(), null))
                                .then(Commands.argument("civ", StringArgumentType.string())
                                        .executes(ctx -> MayorCommands.mayorClear(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "civ")))))
                        .then(Commands.literal("withdrawrate")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> MayorCommands.mayorWithdrawRateShow(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                        .executes(ctx -> MayorCommands.mayorWithdrawRateSet(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                DoubleArgumentType.getDouble(ctx, "percent"))))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> MayorCommands.mayorWithdrawRateClear(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"))))))
                        .then(Commands.literal("approval")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> MayorCommands.mayorApprovalSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> MayorCommands.mayorApprovalSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false))))
                                .then(Commands.literal("list")
                                        .executes(ctx -> MayorCommands.mayorApprovalList(ctx.getSource()))))));
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







    public static long nextTownClaimCostCents(int civicChunksOwned) {
        return RealCivConfig.nextTownClaimCostCents(civicChunksOwned);
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

