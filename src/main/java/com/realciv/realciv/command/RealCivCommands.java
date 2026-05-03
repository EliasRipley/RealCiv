package com.realciv.realciv.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.hub.CommunityHubStockMenu;
import com.realciv.realciv.logic.HubRewardResolver;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import org.jetbrains.annotations.Nullable;

public final class RealCivCommands {
    private static final TagKey<Block> PICKAXE_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/pickaxe"));
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
                        .then(Commands.literal("create")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> civCreate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "id")))
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> civCreate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("rename")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> civRename(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("join")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> civJoin(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("assign")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> civAssign(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "id")))))))
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
                                                        false))))))
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
                                                IntegerArgumentType.getInteger(ctx, "count"))))))
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
                                .then(Commands.argument("civ", StringArgumentType.word())
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
                                        .then(Commands.argument("civ", StringArgumentType.word())
                                                .executes(ctx -> mayorSet(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "civ"))))))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(3))
                                .executes(ctx -> mayorClear(ctx.getSource(), null))
                                .then(Commands.argument("civ", StringArgumentType.word())
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
                                                        EntityArgument.getPlayer(ctx, "player"))))))));
    }

    private static int showProfile(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(target.getUUID());

        int farmerLevel = record.levelFor(Profession.FARMER);
        int minerLevel = record.levelFor(Profession.MINER);
        int lumberjackLevel = record.levelFor(Profession.LUMBERJACK);
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int generalLevel = record.generalLevel();

        source.sendSuccess(() -> Component.literal("RealCiv profile for " + target.getGameProfile().getName()), false);
        source.sendSuccess(() -> Component.literal("Civilization: " + civDisplay(data, civId) + " [" + civId + "]"), false);
        source.sendSuccess(() -> Component.literal(
                "Credits: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + " | General Level: " + generalLevel + " (" + record.generalXp() + " XP)"), false);
        source.sendSuccess(() -> Component.literal(
                "Farmer L" + farmerLevel + " | actions " + record.farmerActions() + "/"
                        + RealCivConfig.farmerLimitForLevel(farmerLevel)
                        + " | XP " + record.farmerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Miner L" + minerLevel + " | actions " + record.minerActions() + "/"
                        + RealCivConfig.minerLimitForLevel(minerLevel)
                        + " | XP " + record.minerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Lumberjack L" + lumberjackLevel + " | actions " + record.lumberjackActions() + "/"
                        + RealCivConfig.lumberjackLimitForLevel(lumberjackLevel)
                        + " | XP " + record.lumberjackXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Hunter L" + hunterLevel + " | kills " + record.hunterActions() + "/"
                        + RealCivConfig.hunterLimitForLevel(hunterLevel)
                        + " | XP " + record.hunterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Crafter L" + crafterLevel + " | crafted " + record.crafterActions() + "/"
                        + RealCivConfig.crafterLimitForLevel(crafterLevel)
                        + " | XP " + record.crafterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Hub personal withdrawal rate: "
                        + RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId))
                        + (record.personalWithdrawRatioOverride(civId) == null ? " (default)" : " (mayor override)")),
                false);
        return 1;
    }

    private static int civInfo(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        boolean mayor = data.isMayor(civId, target.getUUID());
        source.sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " belongs to " + civDisplay(data, civId) + " [" + civId + "]"
                        + (mayor ? " (Mayor)" : "")),
                false);
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
        for (String civId : ids) {
            CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
            String name = civ == null ? civId : civ.displayName();
            int plots = civ == null ? 0 : civ.plots().size();
            source.sendSuccess(() -> Component.literal("- " + name + " [" + civId + "] | plots " + plots), false);
        }
        return 1;
    }

    private static int civCreate(CommandSourceStack source, String id, String name) {
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.createCivilization(id, name, actorName(source))) {
            source.sendFailure(Component.literal("Unable to create civilization. It may already exist or id is invalid."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Created civilization '" + name + "' [" + id.toLowerCase(Locale.ROOT) + "]."), true);
        return 1;
    }

    private static int civRename(CommandSourceStack source, String id, String name) {
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.renameCivilization(id, name, actorName(source))) {
            source.sendFailure(Component.literal("Civilization not found: " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Renamed civilization [" + id + "] to '" + name + "'."), true);
        return 1;
    }

    private static int civJoin(CommandSourceStack source, String id)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.setPlayerCivilization(player.getUUID(), id, actorName(source))) {
            source.sendFailure(Component.literal("Civilization not found: " + id));
            return 0;
        }
        String newCiv = data.getOrAssignCivilization(player.getUUID());
        source.sendSuccess(() -> Component.literal(
                "You are now a citizen of " + civDisplay(data, newCiv) + " [" + newCiv + "]."), true);
        return 1;
    }

    private static int civAssign(CommandSourceStack source, ServerPlayer target, String id) {
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.setPlayerCivilization(target.getUUID(), id, actorName(source))) {
            source.sendFailure(Component.literal("Civilization not found: " + id));
            return 0;
        }
        String newCiv = data.getOrAssignCivilization(target.getUUID());
        source.sendSuccess(() -> Component.literal(
                "Assigned " + target.getGameProfile().getName()
                        + " to " + civDisplay(data, newCiv) + " [" + newCiv + "]."),
                true);
        return 1;
    }

    private static int rentCurrentPlot(CommandSourceStack source, int days)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        long rentCost = RealCivConfig.rentCostCents();
        if (record.socialCreditCents(civId) < rentCost) {
            source.sendFailure(Component.literal(
                    "Not enough social credit for " + civDisplay(data, civId) + ". Need "
                            + RealCivUtil.formatCredits(rentCost)
                            + ", you have " + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + "."));
            return 0;
        }

        String dimension = player.serverLevel().dimension().location().toString();
        long chunkX = player.chunkPosition().x;
        long chunkZ = player.chunkPosition().z;
        long now = source.getServer().overworld().getGameTime();
        long paidTicks = Math.max(1L, days) * 24_000L;

        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup != null && !lookup.civilizationId().equals(civId) && !source.hasPermission(3)) {
            source.sendFailure(Component.literal(
                    "This chunk is zoned under civilization '" + lookup.civilizationId() + "' and cannot be rented here."));
            return 0;
        }

        if (lookup != null && lookup.civilizationId().equals(civId)) {
            CivSavedData.PlotRecord plot = lookup.plot();
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
                        "Plot renewed. Next upkeep tick: " + next + " | Balance: "
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
                        + "Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))),
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
            source.sendSuccess(() -> Component.literal(
                    "Chunk [" + chunkX + ", " + chunkZ + "] in " + dimension + " is unzoned."), false);
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
        if (!isMayorOrAdmin(source, data, actorCiv)) {
            source.sendFailure(Component.literal("Only mayor/admin can zone plots for this civilization."));
            return 0;
        }

        LandClass landClass = LandClass.fromConfig(landClassRaw);
        if (landClass == null) {
            source.sendFailure(Component.literal("Invalid land class. Use: public, civic, private."));
            return 0;
        }

        String dimension = actor.serverLevel().dimension().location().toString();
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
        if (!isMayorOrAdmin(source, data, actorCiv)) {
            source.sendFailure(Component.literal("Only mayor/admin can clear plot zoning."));
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
        if (!isMayorOrAdmin(source, data, civId)) {
            source.sendFailure(Component.literal("Only mayor/admin can manage civic managers."));
            return 0;
        }

        data.setCivicManager(civId, target.getUUID(), allowed, actorName(source));
        source.sendSuccess(() -> Component.literal(
                (allowed ? "Added " : "Removed ")
                        + target.getGameProfile().getName() + " as civic manager for "
                        + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int openHubStockMenu(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean privileged = isMayorOrAdmin(source, data, civId);

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) ->
                        new CommunityHubStockMenu(containerId, playerInventory, player, data, civId, privileged),
                Component.literal("Community Hub Stock")));
        player.sendSystemMessage(Component.literal(
                "Hub stock page opened for " + civDisplay(data, civId)
                        + ". Left click=stack, Right click=1 item, Shift click=4 stacks."));
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
        boolean privileged = isMayorOrAdmin(source, data, civId);
        ServerPlayer requester = source.getEntity() instanceof ServerPlayer player ? player : null;

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            source.sendFailure(Component.literal("Unknown item: " + itemId));
            return 0;
        }

        long remainingAllowance = -1L;
        CivSavedData.PlayerRecord requesterRecord = null;

        if (!privileged) {
            if (requester == null) {
                source.sendFailure(Component.literal("Only players can use personal hub withdrawals."));
                return 0;
            }
            if (!requester.getUUID().equals(target.getUUID())) {
                source.sendFailure(Component.literal("You can only withdraw to yourself unless you are mayor/admin."));
                return 0;
            }

            requesterRecord = data.getOrCreatePlayer(requester.getUUID());
            remainingAllowance = requesterRecord.remainingPersonalWithdraw(civId, itemId);
            if (remainingAllowance <= 0L) {
                source.sendFailure(Component.literal(
                        "No personal withdrawal allowance left for " + itemId
                                + ". Contribute more to increase your quota."));
                return 0;
            }
            if (count > remainingAllowance) {
                source.sendFailure(Component.literal(
                        "You can withdraw at most " + remainingAllowance + "x " + itemId
                                + " right now (personal quota)."));
                return 0;
            }
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

        if (!privileged && requesterRecord != null) {
            requesterRecord.recordPersonalWithdrawal(civId, itemId, count);
            data.setDirty();
        }

        String actor = actorName(source);
        if (privileged) {
            data.addAuditLog(
                    civId,
                    actor + " withdrew " + count + "x " + itemId + " for " + target.getGameProfile().getName(),
                    RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            long newRemaining = requesterRecord == null ? 0L : requesterRecord.remainingPersonalWithdraw(civId, itemId);
            data.addAuditLog(
                    civId,
                    actor + " withdrew " + count + "x " + itemId
                            + " from personal quota (remaining allowance: " + newRemaining + ")",
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            source.sendSuccess(() -> Component.literal(
                    "Personal quota remaining for " + itemId + ": " + newRemaining), false);
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
            if (!isMayorOrAdmin(source, data, targetCiv) && !source.hasPermission(3)) {
                source.sendFailure(Component.literal("You can only inspect your own quota unless you are mayor/admin."));
                return 0;
            }
        }
        return showHubQuota(source, target, page);
    }

    private static int showHubQuota(CommandSourceStack source, ServerPlayer target, int page) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(target.getUUID());

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
        int lumberBlocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            totalBlocks++;
            if (block.defaultBlockState().is(PICKAXE_MINEABLE_TAG)) {
                minerBlocks++;
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
        final int summaryLumberBlocks = lumberBlocks;
        final int summaryTotalBlocks = totalBlocks;

        source.sendSuccess(() -> Component.literal(
                "Hub coverage audit:"
                        + " exact rules=" + exactRules.size()
                        + ", tag rules=" + tagRules.size()
                        + ", items covered=" + summaryCoveredItems + "/" + summaryTotalItems
                        + ", blocks(miner)=" + summaryMinerBlocks + "/" + summaryTotalBlocks
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

    private static int showHubLogs(CommandSourceStack source, int count) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!isMayorOrAdmin(source, data, civId)) {
            source.sendFailure(Component.literal("Only the mayor or admins can inspect hub logs."));
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
        record.addSocialCreditCents(civId, cents);
        data.addAuditLog(
                civId,
                actorName(source) + " added " + RealCivUtil.formatCredits(cents)
                        + " social credit to " + player.getGameProfile().getName(),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Added " + RealCivUtil.formatCredits(cents)
                        + " credits to " + player.getGameProfile().getName()
                        + " in " + civDisplay(data, civId)
                        + ". New balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))),
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
                actorName(source) + " set social credit of " + player.getGameProfile().getName()
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
        source.sendSuccess(
                () -> Component.literal("Mayor for " + civDisplay(data, civId)
                        + " set to " + player.getGameProfile().getName() + "."),
                true);
        return 1;
    }

    private static int mayorClear(CommandSourceStack source, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveMayorCivId(source, data, civRaw);
        data.setMayor(civId, null, actorName(source));
        source.sendSuccess(() -> Component.literal(
                "Mayor assignment cleared for " + civDisplay(data, civId) + "."), true);
        return 1;
    }

    private static int mayorShow(CommandSourceStack source, @Nullable String civRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = resolveMayorCivId(source, data, civRaw);
        UUID mayor = data.getMayorId(civId);
        if (mayor == null) {
            source.sendSuccess(() -> Component.literal(
                    "No mayor is assigned for " + civDisplay(data, civId) + "."), false);
            return 1;
        }

        ServerPlayer online = source.getServer().getPlayerList().getPlayer(mayor);
        String name = online == null ? mayor.toString() : online.getGameProfile().getName();
        source.sendSuccess(() -> Component.literal(
                "Current mayor for " + civDisplay(data, civId) + ": " + name), false);
        return 1;
    }

    private static int mayorWithdrawRateShow(CommandSourceStack source, ServerPlayer player) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!isMayorOrAdmin(source, data, civId)) {
            source.sendFailure(Component.literal("Only mayor/admin can view per-player withdraw rates."));
            return 0;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        String rateText = RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId));
        String mode = record.personalWithdrawRatioOverride(civId) == null ? "default" : "override";
        source.sendSuccess(() -> Component.literal(
                "Withdrawal rate for " + player.getGameProfile().getName()
                        + " in " + civDisplay(data, civId) + ": " + rateText + " (" + mode + ")"),
                false);
        return 1;
    }

    private static int mayorWithdrawRateSet(CommandSourceStack source, ServerPlayer player, double percent) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!isMayorOrAdmin(source, data, civId)) {
            source.sendFailure(Component.literal("Only mayor/admin can set per-player withdraw rates."));
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
        return 1;
    }

    private static int mayorWithdrawRateClear(CommandSourceStack source, ServerPlayer player) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = civOfSource(source, data);
        if (!isMayorOrAdmin(source, data, civId)) {
            source.sendFailure(Component.literal("Only mayor/admin can clear per-player withdraw rates."));
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
        return 1;
    }

    private static String civOfSource(CommandSourceStack source, CivSavedData data) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return data.getOrAssignCivilization(player.getUUID());
        }
        return RealCivConfig.defaultCivilizationId();
    }

    private static String resolveMayorCivId(CommandSourceStack source, CivSavedData data, @Nullable String civRaw) {
        if (civRaw == null || civRaw.isBlank()) {
            return civOfSource(source, data);
        }
        CivSavedData.CivilizationRecord civ = data.getCivilization(civRaw);
        return civ == null ? civOfSource(source, data) : civ.id();
    }

    private static String civDisplay(CivSavedData data, String civId) {
        CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
        if (civ == null) {
            return civId;
        }
        return civ.displayName();
    }

    private static boolean isMayorOrAdmin(CommandSourceStack source, CivSavedData data, String civId) {
        if (source.hasPermission(3)) {
            return true;
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            return data.isMayor(civId, player.getUUID());
        }
        return false;
    }

    private static String actorName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getGameProfile().getName();
        }
        return "Console";
    }
}
