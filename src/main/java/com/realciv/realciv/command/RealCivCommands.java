package com.realciv.realciv.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.realciv.realciv.ModBlocks;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.hub.CommunityHubStockMenu;
import com.realciv.realciv.integration.RealCivFTBChunksBridge;
import com.realciv.realciv.integration.RealCivFTBChunksMirror;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.HubRewardResolver;
import com.realciv.realciv.logic.LandWandService;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

public final class RealCivCommands {
    private static final TagKey<Block> PICKAXE_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/pickaxe"));
    private static final TagKey<Block> SHOVEL_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/shovel"));
    private static final TagKey<Block> BAMBOO_BLOCKS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:bamboo_blocks"));

    private RealCivCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("realciv")
                .then(Commands.literal("profile")
                        .executes(ctx -> showProfile(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> showProfile(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("profession")
                        .then(Commands.literal("focus")
                                .then(Commands.literal("show")
                                        .executes(ctx -> professionFocusShow(
                                                ctx.getSource(),
                                                ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> professionFocusShow(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("profession", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        focusableProfessionNames(),
                                                        builder))
                                                .executes(ctx -> professionFocusSetSelf(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "profession")))))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> professionFocusClearSelf(ctx.getSource())))
                                .then(Commands.literal("assign")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("profession", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                focusableProfessionNames(),
                                                                builder))
                                                        .executes(ctx -> professionFocusAssign(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "profession"))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> professionFocusRemove(
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
                                .executes(ctx -> townInfo(ctx.getSource())))
                        .then(Commands.literal("map")
                                .executes(ctx -> townMap(ctx.getSource(), 4))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> townMap(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "radius")))))
                        .then(Commands.literal("claim")
                                .executes(ctx -> townClaim(ctx.getSource())))
                        .then(Commands.literal("unclaim")
                                .executes(ctx -> townUnclaim(ctx.getSource())))
                        .then(Commands.literal("allot")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> townAllotPrivate(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> townAllotPrivate(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "days")))))))
                .then(Commands.literal("plot")
                        .then(Commands.literal("info")
                                .executes(ctx -> showLandInfo(ctx.getSource())))
                        .then(Commands.literal("claim")
                                .executes(ctx -> plotClaimSelf(ctx.getSource(), RealCivConfig.LAND_RENT_DAYS.get()))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                        .executes(ctx -> plotClaimSelf(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("unclaim")
                                .executes(ctx -> plotUnclaimSelf(ctx.getSource()))))
                .then(Commands.literal("land")
                        .then(Commands.literal("rent")
                                .executes(ctx -> rentCurrentPlot(ctx.getSource(), RealCivConfig.LAND_RENT_DAYS.get()))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                        .executes(ctx -> rentCurrentPlot(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("info")
                                .executes(ctx -> showLandInfo(ctx.getSource())))
                        .then(Commands.literal("zone")
                                .then(Commands.argument("class", StringArgumentType.word())
                                        .executes(ctx -> landZoneCurrentPlot(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "class"),
                                                null,
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("owner", EntityArgument.player())
                                                .executes(ctx -> landZoneCurrentPlot(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        EntityArgument.getPlayer(ctx, "owner"),
                                                        RealCivConfig.LAND_RENT_DAYS.get()))
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                        .executes(ctx -> landZoneCurrentPlot(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "class"),
                                                                EntityArgument.getPlayer(ctx, "owner"),
                                                                IntegerArgumentType.getInteger(ctx, "days")))))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> landZoneCurrentPlot(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        null,
                                                        IntegerArgumentType.getInteger(ctx, "days"))))))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> landZoneCurrentPlot(
                                                ctx.getSource(),
                                                "private",
                                                EntityArgument.getPlayer(ctx, "player"),
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> landZoneCurrentPlot(
                                                        ctx.getSource(),
                                                        "private",
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "days"))))))
                        .then(Commands.literal("revoke")
                                .executes(ctx -> landRevokeCurrentPlot(ctx.getSource())))
                        .then(Commands.literal("clear")
                                .executes(ctx -> landRevokeCurrentPlot(ctx.getSource())))
                        .then(Commands.literal("manager")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> landManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> landManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false)))))
                        .then(Commands.literal("wand")
                                .executes(ctx -> landWandGive(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> landSelectionClear(ctx.getSource())))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(3))
                                        .executes(ctx -> landWandGive(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("selection")
                                .then(Commands.literal("info")
                                        .executes(ctx -> landSelectionInfo(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> landSelectionClear(ctx.getSource()))))
                        .then(Commands.literal("zone-selection")
                                .then(Commands.argument("class", StringArgumentType.word())
                                        .executes(ctx -> landZoneSelection(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "class"),
                                                null,
                                                RealCivConfig.LAND_RENT_DAYS.get()))
                                        .then(Commands.argument("owner", EntityArgument.player())
                                                .executes(ctx -> landZoneSelection(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        EntityArgument.getPlayer(ctx, "owner"),
                                                        RealCivConfig.LAND_RENT_DAYS.get()))
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                        .executes(ctx -> landZoneSelection(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "class"),
                                                                EntityArgument.getPlayer(ctx, "owner"),
                                                                IntegerArgumentType.getInteger(ctx, "days")))))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10_000))
                                                .executes(ctx -> landZoneSelection(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "class"),
                                                        null,
                                                        IntegerArgumentType.getInteger(ctx, "days"))))))
                        .then(Commands.literal("clear-selection")
                                .executes(ctx -> landClearSelection(ctx.getSource())))
                        .then(Commands.literal("visualize")
                                .executes(ctx -> landVisualize(ctx.getSource(), RealCivConfig.landWandVisualizeRadiusChunks()))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> landVisualize(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "radius")))))
                        .then(Commands.literal("ftb-mode")
                                .executes(ctx -> landFtbModeShow(ctx.getSource()))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                List.of(
                                                        RealCivFTBChunksBridge.CLAIM_MODE_AUTO,
                                                        RealCivFTBChunksBridge.CLAIM_MODE_CIVIC,
                                                        RealCivFTBChunksBridge.CLAIM_MODE_PRIVATE),
                                                builder))
                                        .executes(ctx -> landFtbModeSet(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.literal("gui")
                                .executes(ctx -> openLandGui(ctx.getSource()))))
                .then(Commands.literal("hub")
                        .then(Commands.literal("open")
                                .executes(ctx -> openHubStockMenu(ctx.getSource())))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(ctx -> withdrawFromHub(
                                                        ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "item"),
                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                        ctx.getSource().getPlayerOrException()))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> withdrawFromHub(
                                                                ctx.getSource(),
                                                                ResourceLocationArgument.getId(ctx, "item"),
                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                EntityArgument.getPlayer(ctx, "target")))))))
                        .then(Commands.literal("stock")
                                .executes(ctx -> showHubStock(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> showHubStock(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("quota")
                                .executes(ctx -> showHubQuotaSelf(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> showHubQuotaSelf(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page"))))
                                .then(Commands.literal("player")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> showHubQuotaFor(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        1))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> showHubQuotaFor(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                IntegerArgumentType.getInteger(ctx, "page")))))))
                        .then(Commands.literal("distribution")
                                .then(Commands.literal("show")
                                        .executes(ctx -> hubDistributionShow(ctx.getSource())))
                                .then(Commands.literal("mode")
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        List.of("contribution_ratio", "shared_stock_ratio", "daily_allowance"),
                                                        builder))
                                                .executes(ctx -> hubDistributionSetMode(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "mode")))))
                                .then(Commands.literal("shared-ratio")
                                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                .executes(ctx -> hubDistributionSetSharedRatio(
                                                        ctx.getSource(),
                                                        DoubleArgumentType.getDouble(ctx, "percent")))))
                                .then(Commands.literal("allowance")
                                        .then(Commands.literal("list")
                                                .executes(ctx -> hubDistributionAllowanceList(ctx.getSource(), 1))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> hubDistributionAllowanceList(
                                                                ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "page")))))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> hubDistributionAllowanceSet(
                                                                        ctx.getSource(),
                                                                        ResourceLocationArgument.getId(ctx, "item"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"))))))
                                        .then(Commands.literal("clear")
                                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                                        .executes(ctx -> hubDistributionAllowanceClear(
                                                                ctx.getSource(),
                                                                ResourceLocationArgument.getId(ctx, "item")))))
                                        .then(Commands.literal("clearall")
                                                .executes(ctx -> hubDistributionAllowanceClearAll(ctx.getSource())))))
                        .then(Commands.literal("coverage")
                                .requires(source -> source.hasPermission(3))
                                .executes(ctx -> showHubCoverage(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> showHubCoverage(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("logs")
                                .executes(ctx -> showHubLogs(ctx.getSource(), 20))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 200))
                                        .executes(ctx -> showHubLogs(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("export-items")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .executes(ctx -> exportHubItemIds(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "namespace"))))))
                .then(Commands.literal("census")
                        .then(Commands.literal("members")
                                .executes(ctx -> censusMembers(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> censusMembers(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("requests")
                                .executes(ctx -> censusRequests(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> censusRequests(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("invites")
                                .executes(ctx -> censusInvites(ctx.getSource(), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> censusInvites(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "page")))))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> censusInvitePlayer(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("uninvite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> censusUninvitePlayer(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("approve")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> censusApproveRequest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("deny")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> censusDenyRequest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> censusRemoveMember(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("manager")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> censusManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> censusManagerSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        false)))))
                        .then(Commands.literal("mayor")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> censusMayorSet(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> censusMayorClear(ctx.getSource())))))
                .then(Commands.literal("tax")
                        .then(Commands.literal("status")
                                .executes(ctx -> taxStatus(ctx.getSource()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> taxStatusFor(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("pay")
                                .executes(ctx -> taxPay(ctx.getSource(), 1))
                                .then(Commands.argument("cycles", IntegerArgumentType.integer(1, 365))
                                        .executes(ctx -> taxPay(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "cycles"))))))
                .then(Commands.literal("credit")
                        .requires(source -> source.hasPermission(3))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                                                .executes(ctx -> creditAdd(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount"))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                                                .executes(ctx -> creditSet(
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

    private static int showProfile(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(target.getUUID());

        int farmerLevel = record.levelFor(Profession.FARMER);
        int minerLevel = record.levelFor(Profession.MINER);
        int terraformerLevel = record.levelFor(Profession.TERRAFORMER);
        int lumberjackLevel = record.levelFor(Profession.LUMBERJACK);
        int fisherLevel = record.levelFor(Profession.FISHER);
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int warriorLevel = record.levelFor(Profession.WARRIOR);
        int explosivesLevel = record.levelFor(Profession.EXPLOSIVES_EXPERT);
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int enchanterLevel = record.levelFor(Profession.ENCHANTER);
        int brewerLevel = record.levelFor(Profession.BREWER);
        int traderLevel = record.levelFor(Profession.TRADER);
        int generalLevel = record.generalLevel();

        source.sendSuccess(() -> Component.literal("RealCiv profile for " + target.getGameProfile().getName()), false);
        if (source.hasPermission(3)) {
            source.sendSuccess(() -> Component.literal("Civilization: " + civDisplay(data, civId) + " [" + civId + "]"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Civilization: " + civDisplay(data, civId)), false);
        }
        source.sendSuccess(() -> Component.literal(
                "Credits: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + " | General Level: " + generalLevel + " (" + record.generalXp() + " XP)"), false);
        @Nullable Profession focus = record.focusedProfession();
        source.sendSuccess(() -> Component.literal(
                "Focus: " + (focus == null ? "none" : focus.name())
                        + " | specialization lock: " + (RealCivConfig.specializationSingleProfessionLockEnabled() ? "ON" : "OFF")
                        + " | XP decay: " + (RealCivConfig.specializationXpDecayEnabled() ? "ON" : "OFF")
                        + " (" + String.format(Locale.ROOT, "%.2f", RealCivConfig.specializationXpDecayRate()) + "x)"),
                false);
        source.sendSuccess(() -> Component.literal(
                "Farmer L" + farmerLevel + " | actions " + record.farmerActions() + "/"
                        + RealCivConfig.farmerLimitForLevel(farmerLevel)
                        + " | XP " + record.farmerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Miner L" + minerLevel + " | actions " + record.minerActions() + "/"
                        + RealCivConfig.minerLimitForLevel(minerLevel)
                        + " | XP " + record.minerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Terraformer L" + terraformerLevel + " | actions " + record.terraformerActions() + "/"
                        + RealCivConfig.terraformerLimitForLevel(terraformerLevel)
                        + " | XP " + record.terraformerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Lumberjack L" + lumberjackLevel + " | actions " + record.lumberjackActions() + "/"
                        + RealCivConfig.lumberjackLimitForLevel(lumberjackLevel)
                        + " | XP " + record.lumberjackXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Fisher L" + fisherLevel + " | catches " + record.fisherActions() + "/"
                        + RealCivConfig.fisherLimitForLevel(fisherLevel)
                        + " | XP " + record.fisherXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Hunter L" + hunterLevel + " | kills " + record.hunterActions() + "/"
                        + RealCivConfig.hunterLimitForLevel(hunterLevel)
                        + " | XP " + record.hunterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Warrior L" + warriorLevel + " | player kills " + record.warriorActions() + "/"
                        + RealCivConfig.warriorLimitForLevel(warriorLevel)
                        + " | XP " + record.warriorXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Explosives Expert L" + explosivesLevel + " | actions " + record.explosivesExpertActions() + "/"
                        + RealCivConfig.explosivesExpertLimitForLevel(explosivesLevel)
                        + " | XP " + record.explosivesExpertXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Crafter L" + crafterLevel + " | crafted " + record.crafterActions() + "/"
                        + RealCivConfig.crafterLimitForLevel(crafterLevel)
                        + " | XP " + record.crafterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Enchanter L" + enchanterLevel + " | actions " + record.enchanterActions() + "/"
                        + RealCivConfig.enchanterLimitForLevel(enchanterLevel)
                        + " | XP " + record.enchanterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Brewer L" + brewerLevel + " | actions " + record.brewerActions() + "/"
                        + RealCivConfig.brewerLimitForLevel(brewerLevel)
                        + " | XP " + record.brewerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Trader L" + traderLevel + " | actions " + record.traderActions() + "/"
                        + RealCivConfig.traderLimitForLevel(traderLevel)
                        + " | XP " + record.traderXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Hub personal withdrawal rate: "
                        + RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId))
                        + (record.personalWithdrawRatioOverride(civId) == null ? " (default)" : " (mayor override)")),
                false);
        return 1;
    }

    private static int professionFocusShow(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CivSavedData data = CivSavedData.get(source.getServer());
        ServerPlayer actor = source.getPlayer();
        if (actor != null
                && !actor.getUUID().equals(target.getUUID())
                && !canManageProfessionFocus(source, data, target)) {
            source.sendFailure(Component.literal("Only leadership/admin can view another player's profession focus."));
            return 0;
        }
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        @Nullable Profession focus = record.focusedProfession();
        source.sendSuccess(() -> Component.literal(
                "Profession focus for " + target.getGameProfile().getName() + ": "
                        + (focus == null ? "none" : focus.name())),
                false);
        source.sendSuccess(() -> Component.literal(
                "specialization.singleProfessionLockEnabled="
                        + RealCivConfig.specializationSingleProfessionLockEnabled()
                        + " | specialization.xpDecayEnabled=" + RealCivConfig.specializationXpDecayEnabled()
                        + " | specialization.xpDecayRate="
                        + String.format(Locale.ROOT, "%.2f", RealCivConfig.specializationXpDecayRate())),
                false);
        return 1;
    }

    private static int professionFocusSetSelf(CommandSourceStack source, String professionRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.setPlayerFocusProfession(player.getUUID(), profession, actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Focus is already " + profession.name() + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Set profession focus to " + profession.name() + "."), true);
        return 1;
    }

    private static int professionFocusClearSelf(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.setPlayerFocusProfession(player.getUUID(), null, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Focus is already cleared."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Cleared profession focus."), true);
        return 1;
    }

    private static int professionFocusAssign(CommandSourceStack source, ServerPlayer target, String professionRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!canManageProfessionFocus(source, data, target)) {
            source.sendFailure(Component.literal("Only leadership/admin can assign another player's profession focus."));
            return 0;
        }
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        if (!data.setPlayerFocusProfession(target.getUUID(), profession, actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Focus is already " + profession.name() + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Set profession focus for " + target.getGameProfile().getName() + " to " + profession.name() + "."),
                true);
        return 1;
    }

    private static int professionFocusRemove(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!canManageProfessionFocus(source, data, target)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear another player's profession focus."));
            return 0;
        }
        if (!data.setPlayerFocusProfession(target.getUUID(), null, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Focus is already cleared."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Cleared profession focus for " + target.getGameProfile().getName() + "."),
                true);
        return 1;
    }

    private static boolean canManageProfessionFocus(CommandSourceStack source, CivSavedData data, ServerPlayer target) {
        if (source.hasPermission(3)) {
            return true;
        }
        ServerPlayer actor = source.getPlayer();
        if (actor == null) {
            return false;
        }
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        return actorCiv.equals(targetCiv)
                && hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_PROFESSION_FOCUS);
    }

    @Nullable
    private static Profession parseFocusableProfession(@Nullable String raw) {
        Profession parsed = Profession.fromConfigName(raw);
        if (parsed == null || parsed == Profession.NONE) {
            return null;
        }
        return parsed;
    }

    private static List<String> focusableProfessionNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            if (profession != Profession.NONE) {
                names.add(profession.name().toLowerCase(Locale.ROOT));
            }
        }
        return names;
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
            CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
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
        CivSavedData.GovernanceModel model = data.governanceModel(civId);
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
        @Nullable CivSavedData.GovernanceModel model = CivSavedData.GovernanceModel.fromSerializedName(modelRaw);
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
        @Nullable CivSavedData.GovernanceModel model = CivSavedData.GovernanceModel.fromSerializedName(modelRaw);
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
        List<CivSavedData.CivRoleView> roles = data.customRolesSorted(civId);
        if (roles.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No custom roles configured for " + civDisplay(data, civId) + "."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(
                "Custom roles for " + civDisplay(data, civId) + ":"), false);
        for (CivSavedData.CivRoleView role : roles) {
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
        List<CivSavedData.CivRoleView> roles = data.customRolesSorted(civId);
        CivSavedData.CivRoleView match = null;
        for (CivSavedData.CivRoleView role : roles) {
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
        CivSavedData.CivRoleView resolved = match;
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
        List<CivSavedData.CivRoleView> roles = data.customRolesSorted(civId);
        CivSavedData.CivRoleView match = null;
        for (CivSavedData.CivRoleView role : roles) {
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
        CivSavedData.CivRoleView resolved = match;
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

        CivSavedData.DeleteCivilizationResult result = data.deleteCivilization(civId, defaultCiv, actorName(source));
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

        List<CivSavedData.DiplomacyView> relations = data.nonNeutralDiplomacyEntriesFor(civId);
        if (relations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("All external relations are currently NEUTRAL."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Non-neutral relations:"), false);
        for (CivSavedData.DiplomacyView relation : relations) {
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

        @Nullable CivSavedData.DiplomacyState state = parseDiplomacyState(stateRaw);
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

        @Nullable CivSavedData.DiplomacyState state = parseDiplomacyState(stateRaw);
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

    private static int townInfo(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        int civicChunks = data.countPlotsByClass(civId, LandClass.CIVIC);
        int privateChunks = data.countPlotsByClass(civId, LandClass.PRIVATE);
        long nextTownCost = nextTownClaimCostCents(civicChunks);
        int ownedPrivate = data.privatePlotCountForOwner(civId, player.getUUID());
        long nextPrivateCost = nextPrivateClaimCostCents(ownedPrivate);
        source.sendSuccess(() -> Component.literal(
                "Town info for " + civDisplay(data, civId)
                        + " | civic chunks: " + civicChunks
                        + " | private chunks: " + privateChunks
                        + " | collective contribution karma: " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))
                        + " | next town claim cost: " + RealCivUtil.formatCredits(nextTownCost)
                        + " | your next private claim cost: " + RealCivUtil.formatCredits(nextPrivateCost)),
                false);
        return 1;
    }

    private static int townMap(CommandSourceStack source, int radius)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        String dimension = player.serverLevel().dimension().location().toString();
        long centerX = player.chunkPosition().x;
        long centerZ = player.chunkPosition().z;
        int safeRadius = Math.max(1, Math.min(10, radius));

        source.sendSuccess(() -> Component.literal(
                "Town map " + civDisplay(data, civId)
                        + " | dim: " + dimension
                        + " | center chunk: [" + centerX + ", " + centerZ + "]"
                        + " | radius: " + safeRadius),
                false);

        for (long z = centerZ - safeRadius; z <= centerZ + safeRadius; z++) {
            StringBuilder row = new StringBuilder();
            row.append(String.format(Locale.ROOT, "z=%d ", z));
            for (long x = centerX - safeRadius; x <= centerX + safeRadius; x++) {
                char symbol = mapSymbolForChunk(data, civId, player.getUUID(), dimension, x, z, centerX, centerZ);
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
                "Chunk claiming: mayor uses /realciv town claim, citizens use /realciv plot claim."),
                false);
        return 1;
    }

    private static int townClaim(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS)) {
            source.sendFailure(Component.literal("Only leadership/admin can expand town claims."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        if (!ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();

        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing != null && !existing.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk is already claimed by civilization '" + existing.civilizationId() + "'."));
            return 0;
        }
        if (existing != null
                && existing.civilizationId().equals(civId)
                && existing.plot().landClass() == LandClass.CIVIC) {
            source.sendFailure(Component.literal("This chunk is already a town (CIVIC) claim."));
            return 0;
        }

        if (data.countPlotsByClass(civId, LandClass.CIVIC) > 0
                && !isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
            source.sendFailure(Component.literal(
                    "Town claims must be within or adjacent to existing town land."));
            return 0;
        }

        int civicChunks = data.countPlotsByClass(civId, LandClass.CIVIC);
        long claimCost = nextTownClaimCostCents(civicChunks);
        long treasury = data.civTreasuryCents(civId);
        if (treasury < claimCost) {
            source.sendFailure(Component.literal(
                    "Not enough collective contribution karma. Need " + RealCivUtil.formatCredits(claimCost)
                            + ", civ has " + RealCivUtil.formatCredits(treasury) + "."));
            return 0;
        }

        if (existing != null && !existing.civilizationId().equals(civId) && source.hasPermission(3)) {
            data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        }

        data.addCivTreasuryCents(civId, -claimCost);
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.CIVIC, null, now, 0L);
        data.addAuditLog(
                civId,
                actorName(source) + " claimed town chunk " + dimension + "[" + chunkX + "," + chunkZ + "]"
                        + " for " + RealCivUtil.formatCredits(claimCost),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Town chunk claimed at [" + chunkX + ", " + chunkZ + "] in " + civDisplay(data, civId)
                        + ". Collective contribution karma now: " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))),
                true);
        return 1;
    }

    private static int townUnclaim(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS)) {
            source.sendFailure(Component.literal("Only leadership/admin can unclaim town chunks."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
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
                    "Use /realciv plot unclaim on private plots, or admin override."));
            return 0;
        }

        data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        data.addAuditLog(
                existing.civilizationId(),
                actorName(source) + " unclaimed chunk " + dimension + "[" + chunkX + "," + chunkZ + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "Unclaimed chunk [" + chunkX + ", " + chunkZ + "]."), true);
        return 1;
    }

    private static int townAllotPrivate(CommandSourceStack source, ServerPlayer target, int days)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS)) {
            source.sendFailure(Component.literal("Only leadership/admin can allot private town plots."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!targetCiv.equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("Target player must belong to your civilization."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        if (!ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();
        long paidTicks = Math.max(1L, days) * 24_000L;

        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing == null || !existing.civilizationId().equals(civId)) {
            source.sendFailure(Component.literal(
                    "Mayor allotment must be on a chunk already claimed by your civilization."));
            return 0;
        }
        if (existing.plot().landClass() == LandClass.CIVIC || existing.plot().landClass() == LandClass.COMMUNITY) {
            data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, target.getUUID(), now, paidTicks);
            data.addAuditLog(
                    civId,
                    actorName(source) + " allotted private plot " + dimension + "[" + chunkX + "," + chunkZ + "]"
                            + " to " + target.getGameProfile().getName(),
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            data.setDirty();
            source.sendSuccess(() -> Component.literal(
                    "Allotted private chunk [" + chunkX + ", " + chunkZ + "] to "
                            + target.getGameProfile().getName() + "."), true);
            return 1;
        }

        if (existing.plot().landClass() == LandClass.PRIVATE && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("This chunk is already private."));
            return 0;
        }
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, target.getUUID(), now, paidTicks);
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "Reassigned private chunk [" + chunkX + ", " + chunkZ + "] to "
                        + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int plotClaimSelf(CommandSourceStack source, int days)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        String dimension = player.serverLevel().dimension().location().toString();
        if (!ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();
        long paidTicks = Math.max(1L, days) * 24_000L;

        if (!isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
            source.sendFailure(Component.literal(
                    "Private land must be within or adjacent to your town's CIVIC claims."));
            return 0;
        }

        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
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
            source.sendFailure(Component.literal("That private plot is owned by another player."));
            return 0;
        }
        if (existing != null
                && existing.civilizationId().equals(civId)
                && existing.plot().landClass() == LandClass.CIVIC
                && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk is CIVIC town land. Ask your mayor to allot it with /realciv town allot <player>."));
            return 0;
        }

        int ownedPrivate = data.privatePlotCountForOwner(civId, player.getUUID());
        long cost = nextPrivateClaimCostCents(ownedPrivate);
        if (record.socialCreditCents(civId) < cost) {
            source.sendFailure(Component.literal(
                    "Not enough contribution karma. Need " + RealCivUtil.formatCredits(cost)
                            + ", you have " + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."));
            return 0;
        }

        if (existing != null && !existing.civilizationId().equals(civId) && source.hasPermission(3)) {
            data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        }

        record.addSocialCreditCents(civId, -cost);
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, player.getUUID(), now, paidTicks);
        data.addAuditLog(
                civId,
                actorName(source) + " claimed private plot " + dimension + "[" + chunkX + "," + chunkZ + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Private chunk claimed at [" + chunkX + ", " + chunkZ + "]. Cost: "
                        + RealCivUtil.formatCredits(cost)
                        + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))),
                true);
        return 1;
    }

    private static int plotUnclaimSelf(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());

        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing == null) {
            source.sendFailure(Component.literal("This chunk is not claimed."));
            return 0;
        }
        if (!existing.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("You cannot unclaim another civilization's chunk."));
            return 0;
        }
        if (existing.plot().landClass() != LandClass.PRIVATE) {
            source.sendFailure(Component.literal("This is not a private plot."));
            return 0;
        }
        if (existing.plot().ownerId() != null
                && !existing.plot().ownerId().equals(player.getUUID())
                && !hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)
                && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("Only owner/leadership/admin can unclaim this private plot."));
            return 0;
        }

        data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        data.addAuditLog(
                existing.civilizationId(),
                actorName(source) + " unclaimed private plot " + dimension + "[" + chunkX + "," + chunkZ + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "Private plot unclaimed at [" + chunkX + ", " + chunkZ + "]."), true);
        return 1;
    }

    private static int rentCurrentPlot(CommandSourceStack source, int days)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        String dimension = player.serverLevel().dimension().location().toString();
        if (!ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();
        long paidTicks = Math.max(1L, days) * 24_000L;

        if (!isWithinOrAdjacentToTown(data, civId, dimension, chunkX, chunkZ)) {
            source.sendFailure(Component.literal(
                    "Private land must be within or adjacent to your town's CIVIC claims."));
            return 0;
        }

        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup != null && !lookup.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk is zoned under civilization '" + lookup.civilizationId() + "' and cannot be rented here."));
            return 0;
        }

        int ownedPrivate = data.privatePlotCountForOwner(civId, player.getUUID());
        long rentCost = nextPrivateClaimCostCents(ownedPrivate);
        if (record.socialCreditCents(civId) < rentCost) {
            source.sendFailure(Component.literal(
                    "Not enough contribution karma for " + civDisplay(data, civId) + ". Need "
                            + RealCivUtil.formatCredits(rentCost)
                            + ", you have " + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."));
            return 0;
        }

        if (lookup != null && lookup.civilizationId().equals(civId)) {
            CivSavedData.PlotRecord plot = lookup.plot();
            if (plot.landClass() == LandClass.CIVIC && !source.hasPermission(3)) {
                source.sendFailure(Component.literal(
                        "This chunk is CIVIC town land. Ask your mayor to allot it with /realciv town allot <player>."));
                return 0;
            }
            if (plot.landClass() == LandClass.PRIVATE
                    && plot.ownerId() != null
                    && !plot.ownerId().equals(player.getUUID())
                    && !source.hasPermission(3)) {
                source.sendFailure(Component.literal("This private plot is owned by another player."));
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
                        actorName(source) + " renewed private plot " + dimension + "[" + chunkX + "," + chunkZ + "]"
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
                actorName(source) + " rented chunk " + dimension + "[" + chunkX + "," + chunkZ + "] as PRIVATE"
                        + " for " + days + " day(s).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Private plot rented in " + civDisplay(data, civId)
                        + " at [" + chunkX + ", " + chunkZ + "] for " + days + " day(s). "
                        + "Cost: " + RealCivUtil.formatCredits(rentCost)
                        + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))),
                false);
        return 1;
    }

    private static int showLandInfo(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());

        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();

        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup == null) {
            String wildernessRule = RealCivConfig.blockUnclaimedBuilding()
                    ? "break denied by config, building denied"
                    : "break allowed, building denied";
            source.sendSuccess(() -> Component.literal(
                    "Chunk [" + chunkX + ", " + chunkZ + "] in " + dimension
                            + " is wilderness/unzoned (" + wildernessRule + ")."), false);
            return 1;
        }

        CivSavedData.PlotRecord plot = lookup.plot();
        String owner = plot.ownerId() == null ? "none" : plot.ownerId().toString();
        long ticksToUpkeep = Math.max(0L, plot.nextUpkeepTick() - now);
        double daysToUpkeep = ticksToUpkeep / 24_000.0D;
        String delinquent = plot.delinquentSinceTick() < 0L ? "no" : "yes (since tick " + plot.delinquentSinceTick() + ")";
        boolean canBuild = data.canBuildOnPlot(lookup.civilizationId(), plot, player.getUUID(), RealCivUtil.isBypass(player));
        boolean canBreak = data.canBreakOnPlot(lookup.civilizationId(), plot, player.getUUID(), RealCivUtil.isBypass(player));

        source.sendSuccess(() -> Component.literal(
                "Chunk [" + chunkX + ", " + chunkZ + "] in " + dimension
                        + " | Civ: " + civDisplay(data, lookup.civilizationId()) + " [" + lookup.civilizationId() + "]"
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

    private static int landZoneCurrentPlot(
            CommandSourceStack source,
            String landClassRaw,
            @Nullable ServerPlayer owner,
            int prepayDays) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CivSavedData data = CivSavedData.get(source.getServer());
        ServerPlayer actor = source.getPlayerOrException();
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
            source.sendFailure(Component.literal("Only leadership/admin can zone plots for this civilization."));
            return 0;
        }

        LandClass landClass = LandClass.fromConfig(landClassRaw);
        if (landClass == null) {
            source.sendFailure(Component.literal("Invalid land class. Use: community, civic, private (public also works)."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        if (!ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();

        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
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
                actorName(source) + " zoned " + dimension + "[" + chunkX + "," + chunkZ + "] as " + landClass
                        + (ownerId == null ? "" : " owner=" + ownerId),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String ownerText = ownerId == null ? "none" : ownerId.toString();
        source.sendSuccess(() -> Component.literal(
                "Zoned chunk [" + chunkX + ", " + chunkZ + "] in " + civDisplay(data, actorCiv)
                        + " as " + landClass + " | owner: " + ownerText),
                true);
        return 1;
    }

    private static int landRevokeCurrentPlot(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CivSavedData data = CivSavedData.get(source.getServer());
        ServerPlayer actor = source.getPlayerOrException();
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear plot zoning."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
        long chunkX = actor.chunkPosition().x;
        long chunkZ = actor.chunkPosition().z;
        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
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
                actorName(source) + " cleared zoning at " + dimension + "[" + chunkX + "," + chunkZ + "]",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        source.sendSuccess(() -> Component.literal(
                "Cleared zoning for chunk [" + chunkX + ", " + chunkZ + "] in " + dimension + "."), true);
        return 1;
    }

    private static int landManagerSet(CommandSourceStack source, ServerPlayer target, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_MANAGERS)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage civic managers."));
            return 0;
        }

        data.setCivicManager(civId, target.getUUID(), allowed, actorName(source));
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Added " : "Removed ")
                        + target.getGameProfile().getName() + " as civic manager for "
                        + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int landWandGive(CommandSourceStack source, ServerPlayer target) {
        ItemStack wand = new ItemStack(ModBlocks.LAND_WAND.get(), 1);
        boolean added = target.getInventory().add(wand);
        if (!added) {
            target.drop(wand, false);
        }
        source.sendSuccess(() -> Component.literal(
                "Granted Land Wand to " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int landSelectionInfo(CommandSourceStack source)
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

    private static int landSelectionClear(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandWandService.clearSelection(player);
        source.sendSuccess(() -> Component.literal("Land wand selection cleared."), false);
        return 1;
    }

    private static int landVisualize(CommandSourceStack source, int radius)
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

    private static int openLandGui(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        openLandGuiForPlayer(player, data);
        return 1;
    }

    private static int landFtbModeShow(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean mayorOrAdmin = hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE);
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        String storedMode = RealCivFTBChunksBridge.normalizeClaimModeOrAuto(record.ftbClaimModeOverride());
        String effectiveMode = RealCivFTBChunksBridge.effectiveClaimModeLabel(mayorOrAdmin, record.ftbClaimModeOverride());

        if (!mayorOrAdmin) {
            source.sendSuccess(() -> Component.literal(
                    "FTB map claim mode: PRIVATE (non-leadership players always claim PRIVATE plots)."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(
                "FTB map claim mode for " + civDisplay(data, civId) + ": stored="
                        + storedMode.toUpperCase(Locale.ROOT)
                        + ", effective=" + effectiveMode.toUpperCase(Locale.ROOT)
                        + " (default when AUTO: " + RealCivConfig.ftbMayorDefaultClaimMode().toUpperCase(Locale.ROOT) + ")."),
                false);
        source.sendSuccess(() -> Component.literal(
                "Set with: /realciv land ftb-mode <auto|civic|private>"), false);
        return 1;
    }

    private static int landFtbModeSet(CommandSourceStack source, String rawMode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean mayorOrAdmin = hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE);
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

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (RealCivFTBChunksBridge.CLAIM_MODE_AUTO.equals(parsed)) {
            record.setFtbClaimModeOverride(null);
        } else {
            record.setFtbClaimModeOverride(parsed);
        }

        data.addAuditLog(
                civId,
                actorName(source) + " set FTB map claim mode override to " + parsed.toUpperCase(Locale.ROOT),
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
    private static String parseFtbClaimModeArgument(String rawMode) {
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

    private static int landZoneSelection(
            CommandSourceStack source,
            String landClassRaw,
            @Nullable ServerPlayer owner,
            int prepayDays) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
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
        if (!ensureClaimDimensionAllowed(source, dimension)) {
            return 0;
        }
        for (long chunkX = selection.minChunkX(); chunkX <= selection.maxChunkX(); chunkX++) {
            for (long chunkZ = selection.minChunkZ(); chunkZ <= selection.maxChunkZ(); chunkZ++) {
                @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
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
                @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
                if (existing != null && !existing.civilizationId().equals(actorCiv) && source.hasPermission(3)) {
                    data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
                }
                data.setPlot(actorCiv, dimension, chunkX, chunkZ, landClass, ownerId, now, paidTicks);
                affected++;
            }
        }

        data.addAuditLog(
                actorCiv,
                actorName(source) + " zoned selection " + dimension
                        + " X[" + selection.minChunkX() + ".." + selection.maxChunkX() + "]"
                        + " Z[" + selection.minChunkZ() + ".." + selection.maxChunkZ() + "] as " + landClass
                        + (ownerId == null ? "" : " owner=" + ownerId),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String ownerText = ownerId == null ? "none" : ownerId.toString();
        final int finalAffected = affected;
        final String zonedMessage =
                "Zoned " + finalAffected + " chunk(s) in " + civDisplay(data, actorCiv)
                        + " as " + landClass + " | owner: " + ownerText;
        source.sendSuccess(() -> Component.literal(zonedMessage), true);
        return 1;
    }

    private static int landClearSelection(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING)) {
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
                @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(selection.dimension(), chunkX, chunkZ);
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
                            actorName(source) + " cleared zoning at " + selection.dimension()
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

    private static int censusMembers(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        List<UUID> members = data.civilizationMembersSorted(civId);
        if (members.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No members are registered in your civilization."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (members.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(members.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Census members for " + civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"),
                false);
        for (int i = start; i < end; i++) {
            UUID memberId = members.get(i);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(memberId);
            String name = online == null ? memberId.toString() : online.getGameProfile().getName();
            String role = data.isMayor(civId, memberId)
                    ? "MAYOR"
                    : (data.isCivicManager(civId, memberId) ? "MANAGER" : "CITIZEN");
            source.sendSuccess(() -> Component.literal("- " + name + " | " + role + " | " + memberId), false);
        }
        return 1;
    }

    private static int censusRequests(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can view join requests."));
            return 0;
        }

        List<UUID> requests = data.joinRequestsSorted(civId);
        if (requests.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No join requests are pending."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (requests.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(requests.size(), start + pageSize);
        source.sendSuccess(() -> Component.literal(
                "Join requests for " + civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);

        for (int i = start; i < end; i++) {
            UUID id = requests.get(i);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
            String name = online == null ? id.toString() : online.getGameProfile().getName();
            source.sendSuccess(() -> Component.literal("- " + name + " | " + id), false);
        }
        return 1;
    }

    private static int censusInvites(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can view invitations."));
            return 0;
        }

        List<UUID> invites = data.invitedPlayersSorted(civId);
        if (invites.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No invitations are pending."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (invites.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(invites.size(), start + pageSize);
        source.sendSuccess(() -> Component.literal(
                "Invitations for " + civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);

        for (int i = start; i < end; i++) {
            UUID id = invites.get(i);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
            String name = online == null ? id.toString() : online.getGameProfile().getName();
            source.sendSuccess(() -> Component.literal("- " + name + " | " + id), false);
        }
        return 1;
    }

    private static int censusInvitePlayer(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can invite players."));
            return 0;
        }

        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (targetCiv.equals(civId)) {
            source.sendFailure(Component.literal(target.getGameProfile().getName() + " is already in your civilization."));
            return 0;
        }
        if (!data.addInvite(civId, target.getUUID(), actorName(source))) {
            source.sendFailure(Component.literal("Invite already exists for that player."));
            return 0;
        }
        target.sendSystemMessage(Component.literal(
                "You have been invited to join " + civDisplay(data, civId)
                        + ". Use /realciv civ join " + civId + " to accept."));
        source.sendSuccess(() -> Component.literal(
                "Invited " + target.getGameProfile().getName() + " to " + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int censusUninvitePlayer(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can revoke invites."));
            return 0;
        }
        if (!data.removeInvite(civId, target.getUUID(), actorName(source))) {
            source.sendFailure(Component.literal("No invite found for that player."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Revoked invite for " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int censusApproveRequest(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can approve join requests."));
            return 0;
        }
        if (!data.hasJoinRequest(civId, target.getUUID()) && !data.hasInvite(civId, target.getUUID())) {
            source.sendFailure(Component.literal("No pending request/invite for that player."));
            return 0;
        }
        if (!data.setPlayerCivilization(target.getUUID(), civId, actorName(source))) {
            source.sendFailure(Component.literal("Failed to approve join."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Approved " + target.getGameProfile().getName() + " into " + civDisplay(data, civId) + "."), true);
        target.sendSystemMessage(Component.literal(
                "Your membership in " + civDisplay(data, civId) + " was approved."));
        return 1;
    }

    private static int censusDenyRequest(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS)) {
            source.sendFailure(Component.literal("Only leadership/admin can deny join requests."));
            return 0;
        }
        boolean removedRequest = data.removeJoinRequest(civId, target.getUUID(), actorName(source));
        boolean removedInvite = data.removeInvite(civId, target.getUUID(), actorName(source));
        if (!removedRequest && !removedInvite) {
            source.sendFailure(Component.literal("No pending request/invite for that player."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Denied/cleared pending join state for " + target.getGameProfile().getName() + "."), true);
        target.sendSystemMessage(Component.literal(
                "Your join request/invite for " + civDisplay(data, civId) + " was declined or revoked."));
        return 1;
    }

    private static int censusRemoveMember(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_POLICE_MEMBERS)) {
            source.sendFailure(Component.literal("Only leadership/admin can remove members."));
            return 0;
        }
        if (actor.getUUID().equals(target.getUUID()) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal("Use /realciv civ leave if you want to leave your own civilization."));
            return 0;
        }
        if (!data.removeMemberToDefault(civId, target.getUUID(), actorName(source))) {
            source.sendFailure(Component.literal("That player is not a member of your civilization."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Removed " + target.getGameProfile().getName() + " from " + civDisplay(data, civId) + "."), true);
        target.sendSystemMessage(Component.literal(
                "You were removed from " + civDisplay(data, civId) + "."));
        return 1;
    }

    private static int censusManagerSet(CommandSourceStack source, ServerPlayer target, boolean allowed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS_ROLES)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage census roles."));
            return 0;
        }

        data.setCivicManager(civId, target.getUUID(), allowed, actorName(source));
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Assigned " : "Removed ")
                        + target.getGameProfile().getName() + " as civic manager in "
                        + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int censusMayorSet(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LEADERSHIP)) {
            source.sendFailure(Component.literal("Only leadership/admin can set mayor through census controls."));
            return 0;
        }

        data.setMayor(civId, target.getUUID(), actorName(source));
        grantMayorStarterHub(target);
        String title = data.leaderTitle(civId);
        source.sendSuccess(() -> Component.literal(
                "Set " + title.toLowerCase(Locale.ROOT) + " for " + civDisplay(data, civId)
                        + " to " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int censusMayorClear(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_LEADERSHIP)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear mayor through census controls."));
            return 0;
        }
        data.setMayor(civId, null, actorName(source));
        String title = data.leaderTitle(civId);
        source.sendSuccess(() -> Component.literal(
                title + " assignment cleared for " + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int taxStatus(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        showTaxStatus(source, actor);
        return 1;
    }

    private static int taxStatusFor(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        if (!actor.getUUID().equals(target.getUUID())
                && (!actorCiv.equals(targetCiv)
                || !hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP))) {
            source.sendFailure(Component.literal("Only leadership/admin can inspect another member's tax status."));
            return 0;
        }
        showTaxStatus(source, target);
        return 1;
    }

    private static void showTaxStatus(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        CivSavedData.TaxPaymentMode paymentMode = data.taxPaymentMode(civId);
        ResourceLocation taxItemId = data.taxItemId(civId);

        int ownedPlots = data.privatePlotCountForOwner(civId, target.getUUID());
        int delinquentPlots = data.delinquentPrivatePlotCountForOwner(civId, target.getUUID());
        long nextUpkeepTick = data.earliestPrivatePlotUpkeepTick(civId, target.getUUID());
        long cycleCost = data.upkeepCostPerPlotCents(civId) * ownedPlots;
        long cycleItemCost = data.taxItemCostPerPlotCurrentRate(civId) * ownedPlots;

        source.sendSuccess(() -> Component.literal(
                "Tax status for " + target.getGameProfile().getName() + " in " + civDisplay(data, civId) + ":"), false);
        source.sendSuccess(() -> Component.literal(
                "Private plots: " + ownedPlots + " | Delinquent: " + delinquentPlots + " | Next upkeep tick: " + nextUpkeepTick), false);
        if (paymentMode == CivSavedData.TaxPaymentMode.KARMA) {
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

    private static int taxPay(CommandSourceStack source, int cycles)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.TaxPaymentMode paymentMode = data.taxPaymentMode(civId);
        ResourceLocation taxItemId = data.taxItemId(civId);
        int safeCycles = Math.max(1, cycles);
        int ownedPlots = data.privatePlotCountForOwner(civId, player.getUUID());
        if (ownedPlots <= 0) {
            source.sendFailure(Component.literal(
                    "You do not own private plots in " + civDisplay(data, civId) + "."));
            return 0;
        }

        long cycleCost = data.upkeepCostPerPlotCents(civId) * ownedPlots;
        long cycleItemCost = data.taxItemCostPerPlotCurrentRate(civId) * ownedPlots;
        long totalCost = cycleCost * safeCycles;
        long totalItemCost = cycleItemCost * safeCycles;
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        if (paymentMode == CivSavedData.TaxPaymentMode.KARMA) {
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
                        "Tax item is invalid for " + civDisplay(data, civId) + "."));
                return 0;
            }
            long available = countInventoryItem(player, taxItem);
            if (available < totalItemCost) {
                source.sendFailure(Component.literal(
                        "Insufficient tax items. Need " + totalItemCost + "x " + taxItemId
                                + ", you have " + available + "."));
                return 0;
            }
        }

        long now = source.getServer().overworld().getGameTime();
        int affected = data.prepayPrivatePlotUpkeep(civId, player.getUUID(), safeCycles, now, actorName(source));
        if (affected <= 0) {
            source.sendFailure(Component.literal("No private plots were eligible for upkeep prepayment."));
            return 0;
        }

        if (paymentMode == CivSavedData.TaxPaymentMode.KARMA) {
            record.addSocialCreditCents(civId, -totalCost);
            data.addCivTreasuryCents(civId, totalCost);
        } else {
            Item taxItem = BuiltInRegistries.ITEM.getOptional(taxItemId).orElse(Items.AIR);
            removeInventoryItem(player, taxItem, totalItemCost);
            data.addToHubStock(civId, taxItemId, totalItemCost, actorName(source));
        }
        data.addAuditLog(
                civId,
                actorName(source) + " paid upkeep tax "
                        + (paymentMode == CivSavedData.TaxPaymentMode.KARMA
                        ? RealCivUtil.formatCredits(totalCost) + " karma"
                        : totalItemCost + "x " + taxItemId)
                        + " for " + affected + " private plot(s) across " + safeCycles + " cycle(s).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        if (paymentMode == CivSavedData.TaxPaymentMode.KARMA) {
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

    private static long countInventoryItem(ServerPlayer player, Item item) {
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

    private static void removeInventoryItem(ServerPlayer player, Item item, long count) {
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

    private static int openHubStockMenu(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean privileged = source.hasPermission(3) || RealCivUtil.isBypass(player);
        boolean canManagePolicy = hasCivPermission(
                source,
                data,
                civId,
                CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) ->
                        new CommunityHubStockMenu(
                                containerId,
                                playerInventory,
                                player,
                                data,
                                civId,
                                privileged,
                                canManagePolicy),
                Component.literal("Community Hub Stock")));
        player.sendSystemMessage(Component.literal(
                "Hub stock page opened for " + civDisplay(data, civId)
                        + ". Left click=stack, Right click=1 item, Shift click=4 stacks."));
        if (canManagePolicy) {
            player.sendSystemMessage(Component.literal(
                    "Leadership controls enabled in top row: policy mode, shared ratio, and daily allowances."));
        }
        return 1;
    }

    private static int withdrawFromHub(
            CommandSourceStack source,
            ResourceLocation itemId,
            int count,
            ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = source.getEntity() instanceof ServerPlayer player
                ? data.getOrAssignCivilization(player.getUUID())
                : RealCivConfig.defaultCivilizationId();
        ServerPlayer requester = source.getEntity() instanceof ServerPlayer player ? player : null;
        boolean canTargetOthers = hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_WITHDRAWALS);
        boolean canBypassQuota = source.hasPermission(3) || (requester != null && RealCivUtil.isBypass(requester));

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            source.sendFailure(Component.literal("Unknown item: " + itemId));
            return 0;
        }

        if (!source.hasPermission(3)) {
            String targetCiv = data.getOrAssignCivilization(target.getUUID());
            if (!targetCiv.equals(civId)) {
                source.sendFailure(Component.literal(
                        "Target must belong to your civilization unless you are admin."));
                return 0;
            }
        }

        CivSavedData.PlayerRecord quotaRecord = data.getOrCreatePlayer(target.getUUID());
        CivSavedData.HubDistributionMode distributionMode = data.hubDistributionMode(civId);
        long activeModeLimit = 0L;
        if (!canBypassQuota) {
            if (requester == null) {
                source.sendFailure(Component.literal("Only players can use personal hub withdrawals."));
                return 0;
            }
            if (!canTargetOthers && !requester.getUUID().equals(target.getUUID())) {
                source.sendFailure(Component.literal("You can only withdraw to yourself unless you are leadership/admin."));
                return 0;
            }

            long remainingAllowance;
            if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
                activeModeLimit = data.hubDailyAllowanceLimit(civId, itemId);
                if (activeModeLimit <= 0L) {
                    source.sendFailure(Component.literal(
                            "No daily allowance is configured for " + itemId + " in "
                                    + civDisplay(data, civId) + "."));
                    return 0;
                }
                remainingAllowance = quotaRecord.remainingDailyAllowance(civId, itemId, activeModeLimit);
            } else if (distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                activeModeLimit = data.hubSharedStockDailyLimit(civId, itemId);
                remainingAllowance = quotaRecord.remainingDailyAllowance(civId, itemId, activeModeLimit);
            } else {
                remainingAllowance = quotaRecord.remainingPersonalWithdraw(civId, itemId);
            }
            if (remainingAllowance <= 0L) {
                if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
                    source.sendFailure(Component.literal(
                            "No daily allowance remaining for " + itemId + " for "
                                    + target.getGameProfile().getName() + "."));
                } else if (distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                    source.sendFailure(Component.literal(
                            "No shared-stock allowance remaining for " + itemId + " for "
                                    + target.getGameProfile().getName()
                                    + " today (ratio "
                                    + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + ")."));
                } else {
                    source.sendFailure(Component.literal(
                            "No personal withdrawal allowance left for " + itemId + " for "
                                    + target.getGameProfile().getName()
                                    + ". Contribute more to increase quota."));
                }
                return 0;
            }
            if (count > remainingAllowance) {
                if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
                    source.sendFailure(Component.literal(
                            "You can withdraw at most " + remainingAllowance + "x " + itemId + " for "
                                    + target.getGameProfile().getName() + " right now (daily allowance)."));
                } else if (distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                    source.sendFailure(Component.literal(
                            "You can withdraw at most " + remainingAllowance + "x " + itemId + " for "
                                    + target.getGameProfile().getName() + " right now (shared-stock allowance)."));
                } else {
                    source.sendFailure(Component.literal(
                            "You can withdraw at most " + remainingAllowance + "x " + itemId + " for "
                                    + target.getGameProfile().getName() + " right now (personal quota)."));
                }
                return 0;
            }
        }

        long penaltyCents = 0L;
        double penaltyRatio = RealCivConfig.hubWithdrawCreditPenaltyRatio();
        if (penaltyRatio > 0.0D) {
            RewardRule rule = HubRewardResolver.resolveEffectiveRewardRule(new ItemStack(item, 1));
            if (rule != null) {
                penaltyCents = Math.round(rule.creditsPerItemCents() * count * penaltyRatio);
            }
        }
        if (penaltyCents > 0L && quotaRecord.socialCreditCents(civId) < penaltyCents) {
            source.sendFailure(Component.literal(
                    target.getGameProfile().getName() + " needs "
                            + RealCivUtil.formatCredits(penaltyCents)
                            + " contribution karma for this withdrawal's credit penalty, but has "
                            + RealCivUtil.formatCredits(quotaRecord.socialCreditCents(civId)) + "."));
            return 0;
        }

        if (!data.tryWithdrawFromHub(civId, itemId, count)) {
            source.sendFailure(Component.literal(
                    "Hub stock in " + civDisplay(data, civId) + " does not contain enough of " + itemId + "."));
            return 0;
        }

        int remaining = count;
        while (remaining > 0) {
            int stackSize = Math.min(item.getDefaultMaxStackSize(), remaining);
            ItemStack stack = new ItemStack(item, stackSize);
            if (!target.getInventory().add(stack)) {
                target.drop(stack, false);
            }
            remaining -= stackSize;
        }

        long newRemaining = 0L;
        if (!canBypassQuota) {
            if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE
                    || distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                quotaRecord.recordDailyAllowanceWithdrawal(civId, itemId, count);
                newRemaining = quotaRecord.remainingDailyAllowance(civId, itemId, activeModeLimit);
            } else {
                quotaRecord.recordPersonalWithdrawal(civId, itemId, count);
                newRemaining = quotaRecord.remainingPersonalWithdraw(civId, itemId);
            }
        }
        if (penaltyCents > 0L) {
            quotaRecord.addSocialCreditCents(civId, -penaltyCents);
        }
        data.setDirty();

        String actor = actorName(source);
        if (canBypassQuota) {
            data.addAuditLog(
                    civId,
                    actor + " withdrew " + count + "x " + itemId + " for " + target.getGameProfile().getName(),
                    RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
                long finalNewRemaining = newRemaining;
                long finalConfiguredDailyLimit = activeModeLimit;
                data.addAuditLog(
                        civId,
                        actor + " withdrew " + count + "x " + itemId
                                + " from " + target.getGameProfile().getName()
                                + " daily allowance (remaining today: " + finalNewRemaining
                                + "/" + finalConfiguredDailyLimit + ")",
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                source.sendSuccess(() -> Component.literal(
                        "Daily allowance remaining for " + target.getGameProfile().getName()
                                + " on " + itemId + ": " + finalNewRemaining + "/" + finalConfiguredDailyLimit),
                        false);
            } else if (distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                long finalNewRemaining = newRemaining;
                long finalSharedLimit = activeModeLimit;
                data.addAuditLog(
                        civId,
                        actor + " withdrew " + count + "x " + itemId
                                + " from " + target.getGameProfile().getName()
                                + " shared-stock allowance (remaining today: " + finalNewRemaining
                                + "/" + finalSharedLimit + ", ratio "
                                + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + ")",
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                source.sendSuccess(() -> Component.literal(
                        "Shared-stock allowance remaining for " + target.getGameProfile().getName()
                                + " on " + itemId + ": " + finalNewRemaining + "/" + finalSharedLimit),
                        false);
            } else {
                long finalNewRemaining = newRemaining;
                data.addAuditLog(
                        civId,
                        actor + " withdrew " + count + "x " + itemId
                                + " from " + target.getGameProfile().getName()
                                + " personal quota (remaining allowance: " + finalNewRemaining + ")",
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                source.sendSuccess(() -> Component.literal(
                        "Personal quota remaining for " + target.getGameProfile().getName()
                                + " on " + itemId + ": " + finalNewRemaining), false);
            }
        }
        if (penaltyCents > 0L) {
            long appliedPenalty = penaltyCents;
            source.sendSuccess(() -> Component.literal(
                    "Withdrawal credit penalty applied to " + target.getGameProfile().getName()
                            + ": -" + RealCivUtil.formatCredits(appliedPenalty)
                            + " contribution karma."), false);
        }

        source.sendSuccess(() -> Component.literal(
                "Withdrew " + count + "x " + itemId + " to " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int showHubStock(CommandSourceStack source, int page) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        List<Map.Entry<String, Long>> entries = data.getHubStockEntriesSorted(civId);
        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "Community Hub stock for " + civDisplay(data, civId) + " is empty."), false);
            return 1;
        }

        int totalPages = (entries.size() + pageSize - 1) / pageSize;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(entries.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Community Hub stock for " + civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);
        for (int i = start; i < end; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            source.sendSuccess(() -> Component.literal("- " + entry.getKey() + ": " + entry.getValue()), false);
        }
        return 1;
    }

    private static int showHubQuotaSelf(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return showHubQuota(source, player, page);
    }

    private static int showHubQuotaFor(CommandSourceStack source, ServerPlayer target, int page) {
        if (!(source.getEntity() instanceof ServerPlayer requester) || !requester.getUUID().equals(target.getUUID())) {
            CivSavedData data = CivSavedData.get(source.getServer());
            String targetCiv = data.getOrAssignCivilization(target.getUUID());
            if (!hasCivPermission(source, data, targetCiv, CivSavedData.ROLE_PERMISSION_VIEW_HUB_QUOTAS)
                    && !source.hasPermission(3)) {
                source.sendFailure(Component.literal("You can only inspect your own quota unless you are leadership/admin."));
                return 0;
            }
        }
        return showHubQuota(source, target, page);
    }

    private static int showHubQuota(CommandSourceStack source, ServerPlayer target, int page) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        CivSavedData.HubDistributionMode mode = data.hubDistributionMode(civId);

        if (mode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
            List<Map.Entry<String, Integer>> entries = data.hubDailyAllowanceEntriesSorted(civId);
            if (entries.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                        "Hub quota for " + civDisplay(data, civId)
                                + " is in daily_allowance mode, but no item limits are configured yet."),
                        false);
                return 1;
            }

            int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
            int totalPages = (entries.size() + pageSize - 1) / pageSize;
            int safePage = Math.max(1, Math.min(page, totalPages));
            int start = (safePage - 1) * pageSize;
            int end = Math.min(entries.size(), start + pageSize);

            source.sendSuccess(() -> Component.literal(
                    "Hub quota for " + target.getGameProfile().getName()
                            + " in " + civDisplay(data, civId)
                            + " (mode daily_allowance, page " + safePage + "/" + totalPages + "):"),
                    false);

            for (int i = start; i < end; i++) {
                Map.Entry<String, Integer> entry = entries.get(i);
                String itemKey = entry.getKey();
                int limit = Math.max(0, entry.getValue());
                long withdrawn = 0L;
                long remaining = 0L;
                try {
                    ResourceLocation parsed = ResourceLocation.parse(itemKey);
                    withdrawn = record.dailyAllowanceWithdrawnCount(civId, parsed);
                    remaining = record.remainingDailyAllowance(civId, parsed, limit);
                } catch (Exception ignored) {
                }
                long safeWithdrawn = Math.max(0L, withdrawn);
                long safeRemaining = Math.max(0L, remaining);
                source.sendSuccess(() -> Component.literal(
                        "- " + itemKey
                                + " | withdrawn today " + safeWithdrawn
                                + " | remaining today " + safeRemaining + "/" + limit),
                        false);
            }
            return 1;
        }

        if (mode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
            List<Map.Entry<String, Long>> stockEntries = data.getHubStockEntriesSorted(civId);
            if (stockEntries.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                        "Hub quota for " + civDisplay(data, civId)
                                + " is in shared_stock_ratio mode, but stock is empty."),
                        false);
                return 1;
            }

            int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
            int totalPages = (stockEntries.size() + pageSize - 1) / pageSize;
            int safePage = Math.max(1, Math.min(page, totalPages));
            int start = (safePage - 1) * pageSize;
            int end = Math.min(stockEntries.size(), start + pageSize);

            source.sendSuccess(() -> Component.literal(
                    "Hub quota for " + target.getGameProfile().getName()
                            + " in " + civDisplay(data, civId)
                            + " (mode shared_stock_ratio @ "
                            + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId))
                            + ", page " + safePage + "/" + totalPages + "):"),
                    false);

            for (int i = start; i < end; i++) {
                Map.Entry<String, Long> entry = stockEntries.get(i);
                String itemKey = entry.getKey();
                long stock = Math.max(0L, entry.getValue());
                long limit = 0L;
                long withdrawn = 0L;
                long remaining = 0L;
                try {
                    ResourceLocation parsed = ResourceLocation.parse(itemKey);
                    limit = data.hubSharedStockDailyLimit(civId, parsed);
                    withdrawn = record.dailyAllowanceWithdrawnCount(civId, parsed);
                    remaining = record.remainingDailyAllowance(civId, parsed, limit);
                } catch (Exception ignored) {
                }
                long safeLimit = Math.max(0L, limit);
                long safeWithdrawn = Math.max(0L, withdrawn);
                long safeRemaining = Math.max(0L, remaining);
                source.sendSuccess(() -> Component.literal(
                        "- " + itemKey
                                + " | stock " + stock
                                + " | shared allowance " + safeRemaining + "/" + safeLimit
                                + " | withdrawn today " + safeWithdrawn),
                        false);
            }
            return 1;
        }

        List<Map.Entry<String, Long>> entries = record.contributions(civId).entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0L)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .toList();

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " has not contributed tracked items in "
                            + civDisplay(data, civId) + " yet."),
                    false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = (entries.size() + pageSize - 1) / pageSize;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(entries.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Hub quota for " + target.getGameProfile().getName()
                        + " in " + civDisplay(data, civId)
                        + " (rate " + RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId))
                        + ", page " + safePage + "/" + totalPages + "):"),
                false);

        for (int i = start; i < end; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            String itemKey = entry.getKey();
            long contributed = Math.max(0L, entry.getValue());
            long withdrawn = 0L;
            long limit = 0L;
            long remaining = 0L;

            try {
                ResourceLocation parsed = ResourceLocation.parse(itemKey);
                withdrawn = record.personalWithdrawnCount(civId, parsed);
                limit = record.personalWithdrawLimit(civId, parsed);
                remaining = record.remainingPersonalWithdraw(civId, parsed);
            } catch (Exception ignored) {
            }

            long safeWithdrawn = Math.max(0L, withdrawn);
            long safeLimit = Math.max(0L, limit);
            long safeRemaining = Math.max(0L, remaining);
            source.sendSuccess(() -> Component.literal(
                    "- " + itemKey
                            + " | contributed " + contributed
                            + " | withdrawn " + safeWithdrawn
                            + " | limit " + safeLimit
                            + " | remaining " + safeRemaining),
                    false);
        }
        return 1;
    }

    private static int hubDistributionShow(CommandSourceStack source) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        CivSavedData.HubDistributionMode mode = data.hubDistributionMode(civId);
        source.sendSuccess(() -> Component.literal(
                "Hub distribution mode for " + civDisplay(data, civId) + ": " + mode.serializedName()),
                false);

        if (mode == CivSavedData.HubDistributionMode.CONTRIBUTION_RATIO) {
            source.sendSuccess(() -> Component.literal(
                    "Contribution-ratio mode uses each player's contribution quota and personal withdraw rate."),
                    false);
            return 1;
        }
        if (mode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
            source.sendSuccess(() -> Component.literal(
                    "Shared-stock ratio mode gives each member a daily allowance per item based on current stock and shared ratio."),
                    false);
            source.sendSuccess(() -> Component.literal(
                    "Current shared ratio: " + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + "."),
                    false);
            return 1;
        }

        List<Map.Entry<String, Integer>> allowances = data.hubDailyAllowanceEntriesSorted(civId);
        if (allowances.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No daily item allowances configured yet. Use /realciv hub distribution allowance set <item> <count>."),
                    false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(
                "Configured daily allowance entries: " + allowances.size() + " item(s)."), false);
        int preview = Math.min(10, allowances.size());
        for (int i = 0; i < preview; i++) {
            Map.Entry<String, Integer> entry = allowances.get(i);
            source.sendSuccess(() -> Component.literal(
                    "- " + entry.getKey() + ": " + entry.getValue() + "/day"),
                    false);
        }
        if (allowances.size() > preview) {
            source.sendSuccess(() -> Component.literal(
                    "Use /realciv hub distribution allowance list for the full list."),
                    false);
        }
        return 1;
    }

    private static int hubDistributionSetMode(CommandSourceStack source, String modeRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage hub distribution mode."));
            return 0;
        }
        @Nullable CivSavedData.HubDistributionMode mode = CivSavedData.HubDistributionMode.fromSerializedName(modeRaw);
        if (mode == null) {
            source.sendFailure(Component.literal(
                    "Unknown mode. Use contribution_ratio, shared_stock_ratio, or daily_allowance."));
            return 0;
        }
        if (!data.setHubDistributionMode(civId, mode, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Hub distribution mode already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Hub distribution mode for " + civDisplay(data, civId)
                        + " set to " + mode.serializedName() + "."), true);
        return 1;
    }

    private static int hubDistributionSetSharedRatio(CommandSourceStack source, double percent)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage hub shared-stock ratio."));
            return 0;
        }
        double ratio = Math.max(0.0D, Math.min(1.0D, percent / 100.0D));
        if (!data.setHubSharedWithdrawRatio(civId, ratio, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Shared-stock ratio already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Hub shared-stock ratio for " + civDisplay(data, civId)
                        + " set to " + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + "."),
                true);
        return 1;
    }

    private static int hubDistributionAllowanceSet(CommandSourceStack source, ResourceLocation itemId, int count)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage daily hub allowances."));
            return 0;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            source.sendFailure(Component.literal("Unknown item: " + itemId));
            return 0;
        }

        if (!data.setHubDailyAllowanceLimit(civId, itemId, count, actorName(source))) {
            source.sendFailure(Component.literal("No change made. Daily allowance may already match."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Set daily allowance for " + itemId + " in " + civDisplay(data, civId)
                        + " to " + count + "/day."), true);
        return 1;
    }

    private static int hubDistributionAllowanceClear(CommandSourceStack source, ResourceLocation itemId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage daily hub allowances."));
            return 0;
        }

        if (!data.setHubDailyAllowanceLimit(civId, itemId, 0, actorName(source))) {
            source.sendFailure(Component.literal("No change made. No daily allowance existed for " + itemId + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Cleared daily allowance for " + itemId + " in " + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int hubDistributionAllowanceClearAll(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage daily hub allowances."));
            return 0;
        }
        int cleared = data.clearAllHubDailyAllowanceLimits(civId, actorName(source));
        if (cleared <= 0) {
            source.sendFailure(Component.literal("No daily allowance entries were configured."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Cleared " + cleared + " daily allowance entr" + (cleared == 1 ? "y" : "ies")
                        + " for " + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int hubDistributionAllowanceList(CommandSourceStack source, int page) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        List<Map.Entry<String, Integer>> allowances = data.hubDailyAllowanceEntriesSorted(civId);
        if (allowances.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No daily allowance entries configured for " + civDisplay(data, civId) + "."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = (allowances.size() + pageSize - 1) / pageSize;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(allowances.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Hub daily allowance entries for " + civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);
        for (int i = start; i < end; i++) {
            Map.Entry<String, Integer> entry = allowances.get(i);
            source.sendSuccess(() -> Component.literal(
                    "- " + entry.getKey() + ": " + entry.getValue() + "/day"), false);
        }
        return 1;
    }

    private static int showHubCoverage(CommandSourceStack source, int page) {
        Map<ResourceLocation, RewardRule> exactRules = RealCivConfig.rewardRules();
        List<TagRewardRule> tagRules = RealCivConfig.tagRewardRules();

        Map<Profession, Integer> coveredByProfession = new HashMap<>();
        List<String> unmatched = new java.util.ArrayList<>();
        int totalItems = 0;
        int coveredItems = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            totalItems++;
            ItemStack stack = new ItemStack(item);
            RewardRule rule = HubRewardResolver.resolveEffectiveRewardRule(stack);
            if (rule == null) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                unmatched.add(id.toString());
                continue;
            }

            coveredItems++;
            coveredByProfession.merge(rule.profession(), 1, Integer::sum);
        }

        int totalBlocks = 0;
        int minerBlocks = 0;
        int terraformerBlocks = 0;
        int lumberBlocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            totalBlocks++;
            if (block.defaultBlockState().is(PICKAXE_MINEABLE_TAG)) {
                minerBlocks++;
            }
            if (block.defaultBlockState().is(SHOVEL_MINEABLE_TAG)) {
                terraformerBlocks++;
            }
            if (block.defaultBlockState().is(BlockTags.LOGS) || block.defaultBlockState().is(BAMBOO_BLOCKS_TAG)) {
                lumberBlocks++;
            }
        }

        unmatched.sort(String::compareTo);
        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (unmatched.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(unmatched.size(), start + pageSize);
        final int summaryCoveredItems = coveredItems;
        final int summaryTotalItems = totalItems;
        final int summaryMinerBlocks = minerBlocks;
        final int summaryTerraformerBlocks = terraformerBlocks;
        final int summaryLumberBlocks = lumberBlocks;
        final int summaryTotalBlocks = totalBlocks;

        source.sendSuccess(() -> Component.literal(
                "Hub coverage audit:"
                        + " exact rules=" + exactRules.size()
                        + ", tag rules=" + tagRules.size()
                        + ", items covered=" + summaryCoveredItems + "/" + summaryTotalItems
                        + ", blocks(miner)=" + summaryMinerBlocks + "/" + summaryTotalBlocks
                        + ", blocks(terraformer)=" + summaryTerraformerBlocks + "/" + summaryTotalBlocks
                        + ", blocks(lumberjack)=" + summaryLumberBlocks + "/" + summaryTotalBlocks),
                false);

        for (Profession profession : Profession.values()) {
            int count = coveredByProfession.getOrDefault(profession, 0);
            source.sendSuccess(() -> Component.literal("- " + profession.name() + ": " + count + " item(s)"), false);
        }

        if (unmatched.isEmpty()) {
            source.sendSuccess(() -> Component.literal("All non-air items are covered by hub reward rules."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(
                "Unmatched items (page " + safePage + "/" + totalPages + ", total " + unmatched.size() + "):"),
                false);
        for (int i = start; i < end; i++) {
            String entry = unmatched.get(i);
            source.sendSuccess(() -> Component.literal("- " + entry), false);
        }
        return 1;
    }

    private static int exportHubItemIds(CommandSourceStack source, String namespaceRaw) {
        @Nullable String namespace = sanitizeNamespace(namespaceRaw);
        if (namespace == null) {
            source.sendFailure(Component.literal(
                    "Invalid namespace. Use lowercase namespace characters only: a-z, 0-9, _, -, ."));
            return 0;
        }

        List<String> itemIds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (namespace.equals(id.getNamespace())) {
                itemIds.add(id.toString());
            }
        }
        itemIds.sort(String::compareTo);

        if (itemIds.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No registered non-air items found for namespace '" + namespace + "'."));
            return 0;
        }

        Path exportDir = FMLPaths.CONFIGDIR.get().resolve("realciv").resolve("exports");
        Path exportFile = exportDir.resolve(namespace + "_items.txt");

        List<String> lines = new ArrayList<>();
        lines.add("# RealCiv Item Export");
        lines.add("# namespace: " + namespace);
        lines.add("# generated_utc: " + Instant.now());
        lines.add("# total_items: " + itemIds.size());
        lines.add("# format: one item id per line");
        lines.add("");
        lines.addAll(itemIds);

        try {
            Files.createDirectories(exportDir);
            Files.write(
                    exportFile,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException ex) {
            source.sendFailure(Component.literal(
                    "Failed to write export file '" + exportFile + "': " + ex.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Exported " + itemIds.size() + " item id(s) for namespace '" + namespace + "' to "
                        + exportFile.toAbsolutePath()), true);
        return 1;
    }

    private static int showHubLogs(CommandSourceStack source, int count) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_VIEW_HUB_LOGS)) {
            source.sendFailure(Component.literal("Only leadership/admin can inspect hub logs."));
            return 0;
        }

        List<String> logs = data.getRecentAuditLogs(civId, count);
        if (logs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No civic logs available."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(
                "Recent civic logs for " + civDisplay(data, civId) + ":"), false);
        for (String entry : logs) {
            source.sendSuccess(() -> Component.literal(entry), false);
        }
        return 1;
    }

    private static int creditAdd(CommandSourceStack source, ServerPlayer player, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        long cents = RealCivUtil.creditsToCents(amount);
        long applied = record.addSocialCreditCents(civId, cents);
        data.addAuditLog(
                civId,
                actorName(source) + " added " + RealCivUtil.formatCredits(applied)
                        + " contribution karma to " + player.getGameProfile().getName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String capSuffix = "";
        if (cents > 0 && applied < cents) {
            capSuffix = " (daily gain cap applied)";
        }
        final String finalCapSuffix = capSuffix;
        source.sendSuccess(() -> Component.literal(
                "Added " + RealCivUtil.formatCredits(applied)
                        + " credits to " + player.getGameProfile().getName()
                        + " in " + civDisplay(data, civId)
                        + ". New balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + finalCapSuffix),
                true);
        return 1;
    }

    private static int creditSet(CommandSourceStack source, ServerPlayer player, double amount) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        long cents = RealCivUtil.creditsToCents(amount);
        record.setSocialCreditCents(civId, cents);
        data.addAuditLog(
                civId,
                actorName(source) + " set contribution karma of " + player.getGameProfile().getName()
                        + " to " + RealCivUtil.formatCredits(cents),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName()
                        + " balance in " + civDisplay(data, civId)
                        + " to " + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."),
                true);
        return 1;
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

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        String rateText = RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId));
        String mode = record.personalWithdrawRatioOverride(civId) == null ? "default" : "override";
        source.sendSuccess(() -> Component.literal(
                "Withdrawal rate for " + player.getGameProfile().getName()
                        + " in " + civDisplay(data, civId) + ": " + rateText + " (" + mode + ")"),
                false);
        if (data.hubDistributionMode(civId) == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
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

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
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
        if (data.hubDistributionMode(civId) == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
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

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
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
        if (data.hubDistributionMode(civId) == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
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

    private static long nextTownClaimCostCents(int civicChunksOwned) {
        long base = RealCivConfig.townClaimCostCents();
        long extra = RealCivConfig.townClaimCostAddedPerOwnedCents() * Math.max(0, civicChunksOwned);
        return Math.max(0L, base + extra);
    }

    private static boolean ensureClaimDimensionAllowed(CommandSourceStack source, String dimension) {
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

    private static long nextPrivateClaimCostCents(int privateOwnedByPlayer) {
        long base = RealCivConfig.rentCostCents();
        long extra = RealCivConfig.rentCostAddedPerOwnedPrivateCents() * Math.max(0, privateOwnedByPlayer);
        return Math.max(0L, base + extra);
    }

    private static char mapSymbolForChunk(
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
        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
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

    private static boolean isTownChunk(
            CivSavedData data,
            String civId,
            String dimension,
            long chunkX,
            long chunkZ) {
        @Nullable CivSavedData.PlotRecord plot = data.getPlot(civId, dimension, chunkX, chunkZ);
        return plot != null && plot.landClass() == LandClass.CIVIC;
    }

    private static String civOfSource(CommandSourceStack source, CivSavedData data) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return data.getOrAssignCivilization(player.getUUID());
        }
        return RealCivConfig.defaultCivilizationId();
    }

    private static boolean canFoundCivilization(CommandSourceStack source, CivSavedData data, ServerPlayer founder) {
        if (source.hasPermission(3)) {
            return true;
        }
        if (!RealCivConfig.requireFounderApproval()) {
            return true;
        }
        return data.isFounderApproved(founder.getUUID());
    }

    @Nullable
    private static String resolveCivilizationId(CivSavedData data, String civRaw) {
        if (civRaw == null || civRaw.isBlank()) {
            return null;
        }
        CivSavedData.CivilizationRecord byId = data.getCivilization(civRaw);
        if (byId != null) {
            return byId.id();
        }
        return data.findCivilizationIdByDisplayName(civRaw);
    }

    private static String resolveMayorCivId(CommandSourceStack source, CivSavedData data, @Nullable String civRaw) {
        if (civRaw == null || civRaw.isBlank()) {
            return civOfSource(source, data);
        }
        String resolved = resolveCivilizationId(data, civRaw);
        return resolved == null ? civOfSource(source, data) : resolved;
    }

    @Nullable
    private static CivSavedData.DiplomacyState parseDiplomacyState(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return CivSavedData.DiplomacyState.fromSerializedName(raw);
    }

    private static String civDisplay(CivSavedData data, String civId) {
        CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
        if (civ == null) {
            return civId;
        }
        return civ.displayName();
    }

    private static String playerNameOrShortId(CommandSourceStack source, UUID playerId) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        String raw = playerId.toString();
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }

    private static boolean hasCivPermission(
            CommandSourceStack source,
            CivSavedData data,
            String civId,
            String permissionKey) {
        return CivPermissionService.hasCivPermission(source, data, civId, permissionKey);
    }

    private static boolean isMayorOrAdmin(CommandSourceStack source, CivSavedData data, String civId) {
        return CivPermissionService.isMayorOrAdmin(source, data, civId);
    }

    private static void grantMayorStarterHub(ServerPlayer player) {
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

    private static boolean inventoryHasItem(ServerPlayer player, Item item) {
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

    private static String actorName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getGameProfile().getName();
        }
        return "Console";
    }

    @Nullable
    private static String sanitizeNamespace(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean allowed = (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_'
                    || ch == '-'
                    || ch == '.';
            if (!allowed) {
                return null;
            }
        }
        return value;
    }
}

