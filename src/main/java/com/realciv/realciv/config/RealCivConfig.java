package com.realciv.realciv.config;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.logic.ItemResetRule;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import com.realciv.realciv.logic.TagResetRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class RealCivConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final List<Integer> LEGACY_LUMBERJACK_LIMITS = List.of(32, 64, 96, 160, 256, 384);
    private static final List<Integer> CURRENT_LUMBERJACK_LIMITS = List.of(8, 16, 32, 64, 96, 128);
    private static final String LEGACY_DEFAULT_CIV_ID = "commonwealth";
    private static final String LEGACY_DEFAULT_CIV_NAME = "Commonwealth";

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> FARMER_LIMITS = BUILDER
            .comment("Farmer action limits by farmer level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.farmerLimits",
                    List.of(8, 16, 32, 64, 96, 128),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MINER_LIMITS = BUILDER
            .comment("Miner action limits by miner level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.minerLimits",
                    List.of(40, 80, 120, 180, 240, 320),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TERRAFORMER_LIMITS = BUILDER
            .comment("Terraformer block-break limits by terraformer level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.terraformerLimits",
                    List.of(40, 80, 120, 180, 240, 320),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> LUMBERJACK_LIMITS = BUILDER
            .comment("Lumberjack block-break limits by lumberjack level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.lumberjackLimits",
                    List.of(8, 16, 32, 64, 96, 128),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> HUNTER_LIMITS = BUILDER
            .comment("Hunter kill limits by hunter level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.hunterLimits",
                    List.of(1, 2, 4, 6, 10, 16),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> WARRIOR_LIMITS = BUILDER
            .comment("Warrior player-kill limits by warrior level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.warriorLimits",
                    List.of(1, 2, 4, 6, 10, 20, 40),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> EXPLOSIVES_EXPERT_LIMITS = BUILDER
            .comment("Explosives Expert action limits by level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.explosivesExpertLimits",
                    List.of(1, 2, 4, 6, 10, 16),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> CRAFTER_LIMITS = BUILDER
            .comment("Crafter output-item limits by crafter level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.crafterLimits",
                    List.of(64, 128, 256, 512, 1024, 2048),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> PROFESSION_XP_THRESHOLDS = BUILDER
            .comment("XP thresholds for profession levels. Index = level.")
            .defineListAllowEmpty(
                    "progression.professionXpThresholds",
                    List.of(0, 100, 250, 500, 900, 1400, 2200),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GENERAL_XP_THRESHOLDS = BUILDER
            .comment("XP thresholds for general level. Index = level.")
            .defineListAllowEmpty(
                    "progression.generalXpThresholds",
                    List.of(0, 40, 120, 260, 480, 760, 1120, 1600, 2200),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.DoubleValue DEATH_ACTION_REFUND_PERCENT = BUILDER
            .comment("Percent of all profession action counters refunded on death to avoid deadlocks from lost resources.")
            .defineInRange("progression.deathActionRefundPercent", 100.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue STALE_ACTION_RESET_ENABLED = BUILDER
            .comment("When true, profession action counters auto-reset to 0 after prolonged inactivity to prevent deadlocks.")
            .define("progression.staleActionResetEnabled", true);

    public static final ModConfigSpec.IntValue STALE_ACTION_RESET_MINUTES = BUILDER
            .comment("Real-time minutes after last profession action update before that profession counter is auto-reset to 0.")
            .defineInRange("progression.staleActionResetMinutes", 120, 1, 60 * 24 * 30);

    public static final ModConfigSpec.IntValue WARRIOR_XP_PER_PLAYER_KILL = BUILDER
            .comment("Warrior profession XP awarded instantly per enemy player kill.")
            .defineInRange("progression.warriorXpPerPlayerKill", 150, 0, 100_000);

    public static final ModConfigSpec.IntValue WARRIOR_GENERAL_XP_PER_PLAYER_KILL = BUILDER
            .comment("General XP awarded instantly per enemy player kill that counts toward warrior progression.")
            .defineInRange("progression.warriorGeneralXpPerPlayerKill", 10, 0, 100_000);

    public static final ModConfigSpec.IntValue EXPLOSIVES_EXPERT_XP_PER_USE = BUILDER
            .comment("Explosives Expert profession XP awarded instantly per valid explosive action.")
            .defineInRange("progression.explosivesExpertXpPerUse", 120, 0, 100_000);

    public static final ModConfigSpec.IntValue EXPLOSIVES_EXPERT_GENERAL_XP_PER_USE = BUILDER
            .comment("General XP awarded instantly per valid explosive action.")
            .defineInRange("progression.explosivesExpertGeneralXpPerUse", 8, 0, 100_000);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUB_REWARD_RULES = BUILDER
            .comment("Accepted hub item rewards. Format: item_id|profession|credits|profession_xp|general_xp")
            .defineListAllowEmpty(
                    "hub.rewardRules",
                    List.of(
                            "minecraft:wheat|FARMER|1.0|2|1",
                            "minecraft:wheat_seeds|FARMER|0.2|1|0",
                            "minecraft:carrot|FARMER|1.2|2|1",
                            "minecraft:potato|FARMER|1.2|2|1",
                            "minecraft:beetroot|FARMER|1.1|2|1",
                            "minecraft:beetroot_seeds|FARMER|0.2|1|0",
                            "minecraft:melon_slice|FARMER|0.8|1|1",
                            "minecraft:pumpkin|FARMER|1.5|2|1",
                            "minecraft:nether_wart|FARMER|1.4|2|1",
                            "minecraft:sugar_cane|FARMER|0.7|1|1",
                            "minecraft:sweet_berries|FARMER|0.9|1|1",
                            "minecraft:cocoa_beans|FARMER|0.8|1|1",
                            "minecraft:cobblestone|MINER|0.1|1|1",
                            "minecraft:stone|MINER|0.1|1|1",
                            "minecraft:deepslate|MINER|0.2|1|1",
                            "minecraft:coal|MINER|1.5|3|2",
                            "minecraft:raw_iron|MINER|4.0|5|3",
                            "minecraft:raw_copper|MINER|2.0|3|2",
                            "minecraft:raw_gold|MINER|5.0|6|4",
                            "minecraft:diamond|MINER|8.0|8|4",
                            "minecraft:emerald|MINER|8.0|8|4",
                            "minecraft:redstone|MINER|1.5|2|1",
                            "minecraft:lapis_lazuli|MINER|2.0|3|2",
                            "minecraft:quartz|MINER|2.0|3|2",
                            "minecraft:amethyst_shard|MINER|2.5|3|2",
                            "minecraft:netherite_scrap|MINER|20.0|20|10",
                            "minecraft:iron_ore|MINER|5.0|6|4",
                            "minecraft:deepslate_iron_ore|MINER|5.0|6|4",
                            "minecraft:copper_ore|MINER|3.0|4|2",
                            "minecraft:deepslate_copper_ore|MINER|3.0|4|2",
                            "minecraft:gold_ore|MINER|6.0|7|4",
                            "minecraft:deepslate_gold_ore|MINER|6.0|7|4",
                            "minecraft:coal_ore|MINER|2.0|3|2",
                            "minecraft:deepslate_coal_ore|MINER|2.0|3|2",
                            "minecraft:diamond_ore|MINER|10.0|10|5",
                            "minecraft:deepslate_diamond_ore|MINER|10.0|10|5",
                            "minecraft:emerald_ore|MINER|10.0|10|5",
                            "minecraft:deepslate_emerald_ore|MINER|10.0|10|5",
                            "minecraft:redstone_ore|MINER|3.0|4|2",
                            "minecraft:deepslate_redstone_ore|MINER|3.0|4|2",
                            "minecraft:lapis_ore|MINER|4.0|5|3",
                            "minecraft:deepslate_lapis_ore|MINER|4.0|5|3",
                            "minecraft:nether_quartz_ore|MINER|3.0|4|2",
                            "minecraft:nether_gold_ore|MINER|3.0|4|2",
                            "minecraft:ancient_debris|MINER|30.0|24|12",
                            "minecraft:dirt|TERRAFORMER|0.15|1|1",
                            "minecraft:coarse_dirt|TERRAFORMER|0.2|1|1",
                            "minecraft:rooted_dirt|TERRAFORMER|0.3|2|1",
                            "minecraft:grass_block|TERRAFORMER|0.2|1|1",
                            "minecraft:podzol|TERRAFORMER|0.3|2|1",
                            "minecraft:mycelium|TERRAFORMER|0.4|2|1",
                            "minecraft:sand|TERRAFORMER|0.25|1|1",
                            "minecraft:red_sand|TERRAFORMER|0.25|1|1",
                            "minecraft:gravel|TERRAFORMER|0.3|2|1",
                            "minecraft:clay|TERRAFORMER|0.5|2|1",
                            "minecraft:clay_ball|TERRAFORMER|0.4|2|1",
                            "minecraft:soul_sand|TERRAFORMER|0.5|2|1",
                            "minecraft:soul_soil|TERRAFORMER|0.5|2|1",
                            "minecraft:mud|TERRAFORMER|0.4|2|1",
                            "minecraft:muddy_mangrove_roots|TERRAFORMER|0.5|2|1",
                            "minecraft:snow_block|TERRAFORMER|0.2|1|1",
                            "minecraft:oak_log|LUMBERJACK|1.0|2|1",
                            "minecraft:spruce_log|LUMBERJACK|1.0|2|1",
                            "minecraft:birch_log|LUMBERJACK|1.0|2|1",
                            "minecraft:jungle_log|LUMBERJACK|1.0|2|1",
                            "minecraft:acacia_log|LUMBERJACK|1.0|2|1",
                            "minecraft:dark_oak_log|LUMBERJACK|1.0|2|1",
                            "minecraft:cherry_log|LUMBERJACK|1.2|2|1",
                            "minecraft:mangrove_log|LUMBERJACK|1.2|2|1",
                            "minecraft:crimson_stem|LUMBERJACK|1.2|2|1",
                            "minecraft:warped_stem|LUMBERJACK|1.2|2|1",
                            "minecraft:oak_planks|LUMBERJACK|0.3|1|1",
                            "minecraft:bamboo_block|LUMBERJACK|1.0|2|1",
                            "minecraft:stripped_bamboo_block|LUMBERJACK|1.0|2|1",
                            "minecraft:rotten_flesh|HUNTER|0.8|2|1",
                            "minecraft:bone|HUNTER|1.5|2|1",
                            "minecraft:string|HUNTER|1.5|2|1",
                            "minecraft:gunpowder|HUNTER|2.0|3|2",
                            "minecraft:spider_eye|HUNTER|2.0|3|2",
                            "minecraft:ender_pearl|HUNTER|6.0|6|3",
                            "minecraft:blaze_rod|HUNTER|6.0|6|3",
                            "minecraft:magma_cream|HUNTER|4.0|4|2",
                            "minecraft:ghast_tear|HUNTER|8.0|8|4",
                            "minecraft:slime_ball|HUNTER|2.5|3|2",
                            "minecraft:prismarine_shard|HUNTER|2.0|3|2",
                            "minecraft:prismarine_crystals|HUNTER|3.0|4|2",
                            "minecraft:leather|HUNTER|1.0|2|1",
                            "minecraft:feather|HUNTER|1.0|2|1",
                            "minecraft:rabbit_hide|HUNTER|1.0|2|1",
                            "minecraft:rabbit_foot|HUNTER|4.0|4|2",
                            "minecraft:porkchop|HUNTER|1.0|2|1",
                            "minecraft:beef|HUNTER|1.0|2|1",
                            "minecraft:chicken|HUNTER|1.0|2|1",
                            "minecraft:mutton|HUNTER|1.0|2|1",
                            "minecraft:rabbit|HUNTER|1.0|2|1",
                            "minecraft:bread|CRAFTER|1.2|2|1",
                            "minecraft:torch|CRAFTER|0.2|1|1",
                            "minecraft:crafting_table|CRAFTER|0.8|2|1",
                            "minecraft:chest|CRAFTER|1.0|2|1",
                            "minecraft:furnace|CRAFTER|1.5|3|2",
                            "minecraft:stone_pickaxe|CRAFTER|3.0|4|2",
                            "minecraft:iron_pickaxe|CRAFTER|8.0|8|4"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUB_TAG_REWARD_RULES = BUILDER
            .comment("Tag-backed hub reward rules. Format: selector_type|tag_id|profession|credits|profession_xp|general_xp")
            .comment("selector_type: BLOCK_TAG or ITEM_TAG")
            .defineListAllowEmpty(
                    "hub.tagRewardRules",
                    List.of(
                            "ITEM_TAG|realciv:farmer_contributions|FARMER|1.00|2|1",
                            "ITEM_TAG|realciv:miner_contributions|MINER|2.00|3|2",
                            "ITEM_TAG|realciv:terraformer_contributions|TERRAFORMER|0.40|2|1",
                            "ITEM_TAG|realciv:lumberjack_contributions|LUMBERJACK|1.00|2|1",
                            "ITEM_TAG|realciv:hunter_contributions|HUNTER|2.00|3|2",
                            "ITEM_TAG|realciv:crafter_contributions|CRAFTER|1.20|2|1",
                            "BLOCK_TAG|minecraft:mineable/pickaxe|MINER|0.10|1|1",
                            "BLOCK_TAG|minecraft:mineable/shovel|TERRAFORMER|0.15|1|1",
                            "BLOCK_TAG|minecraft:logs|LUMBERJACK|1.00|2|1",
                            "BLOCK_TAG|minecraft:bamboo_blocks|LUMBERJACK|1.00|2|1"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUB_TAG_RESET_RULES = BUILDER
            .comment("Tag-backed action reset rules. Format: selector_type|tag_id|profession|actions_per_item")
            .comment("selector_type: BLOCK_TAG or ITEM_TAG")
            .defineListAllowEmpty(
                    "hub.tagResetRules",
                    List.of(
                            "ITEM_TAG|realciv:farmer_reset_items|FARMER|1.0",
                            "ITEM_TAG|realciv:miner_reset_items|MINER|1.0",
                            "ITEM_TAG|realciv:terraformer_reset_items|TERRAFORMER|1.0",
                            "ITEM_TAG|realciv:lumberjack_reset_items|LUMBERJACK|1.0",
                            "ITEM_TAG|realciv:hunter_reset_items|HUNTER|1.0",
                            "ITEM_TAG|realciv:crafter_reset_items|CRAFTER|1.0"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.BooleanValue HUB_USE_PROFESSION_RULE_FILES = BUILDER
            .comment("When true, hub rules are read from per-profession text files under hub.professionRuleDirectory.")
            .comment("If files are missing, RealCiv will generate starter files from current legacy hub.* rule lists.")
            .define("hub.useProfessionRuleFiles", true);

    public static final ModConfigSpec.ConfigValue<String> HUB_PROFESSION_RULE_DIRECTORY = BUILDER
            .comment("Directory under config/ for per-profession hub rule files.")
            .comment("Default produces files like config/realciv/hub/farmer_rewards.txt and farmer_resets.txt.")
            .define("hub.professionRuleDirectory", "realciv/hub");

    public static final ModConfigSpec.DoubleValue DEFAULT_PERSONAL_WITHDRAW_PERCENT = BUILDER
            .comment("Default percent of a player's own contributed item count they can withdraw from the hub.")
            .defineInRange("hub.defaultPersonalWithdrawalPercent", 10.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.IntValue WOOD_TOOL_LEVEL = BUILDER
            .comment("Minimum general level for wooden and golden tiered tools.")
            .defineInRange("tools.requiredLevel.wood", 0, 0, 1000);

    public static final ModConfigSpec.IntValue STONE_TOOL_LEVEL = BUILDER
            .comment("Minimum general level for stone tiered tools.")
            .defineInRange("tools.requiredLevel.stone", 1, 0, 1000);

    public static final ModConfigSpec.IntValue IRON_TOOL_LEVEL = BUILDER
            .comment("Minimum general level for iron tiered tools.")
            .defineInRange("tools.requiredLevel.iron", 3, 0, 1000);

    public static final ModConfigSpec.IntValue DIAMOND_TOOL_LEVEL = BUILDER
            .comment("Minimum general level for diamond tiered tools.")
            .defineInRange("tools.requiredLevel.diamond", 5, 0, 1000);

    public static final ModConfigSpec.IntValue NETHERITE_TOOL_LEVEL = BUILDER
            .comment("Minimum general level for netherite tiered tools.")
            .defineInRange("tools.requiredLevel.netherite", 8, 0, 1000);

    public static final ModConfigSpec.IntValue UNKNOWN_TIER_TOOL_LEVEL = BUILDER
            .comment("Fallback required level for custom tiered tools not matching vanilla tiers.")
            .defineInRange("tools.requiredLevel.unknownTier", 8, 0, 1000);

    public static final ModConfigSpec.BooleanValue CARRY_CAP_PICKUP_ENABLED = BUILDER
            .comment("When true, players cannot pick up profession-tracked items above configured carry caps.")
            .define("carryCap.pickupEnabled", false);

    public static final ModConfigSpec.BooleanValue CARRY_CAP_CRAFT_ENABLED = BUILDER
            .comment("When true, players cannot take crafting output that would exceed configured carry caps.")
            .define("carryCap.craftEnabled", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CARRY_CAP_PROFESSION_MULTIPLIERS = BUILDER
            .comment("Carry-cap multipliers by profession. Format: profession|multiplier")
            .comment("Example: FARMER|1.0 means cap equals farmer action limit for current level.")
            .defineListAllowEmpty(
                    "carryCap.professionMultipliers",
                    List.of(
                            "FARMER|1.0",
                            "MINER|1.0",
                            "TERRAFORMER|1.0",
                            "LUMBERJACK|1.0",
                            "HUNTER|1.0",
                            "WARRIOR|1.0",
                            "EXPLOSIVES_EXPERT|1.0",
                            "CRAFTER|1.0"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CARRY_CAP_ITEM_MAX_OVERRIDES = BUILDER
            .comment("Optional per-item carry cap overrides. Format: item_id|max_count")
            .comment("If an item override exists, it takes precedence over profession multiplier caps.")
            .defineListAllowEmpty(
                    "carryCap.itemMaxOverrides",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BREAK_ACTION_COST_OVERRIDES = BUILDER
            .comment("Optional per-block action costs for block-break limits. Format: block_id|cost")
            .comment("Example: minecraft:coal_ore|2 means breaking one coal ore uses 2 profession actions.")
            .defineListAllowEmpty(
                    "profession.breakActionCostOverrides",
                    List.of(
                            "minecraft:coal_ore|2",
                            "minecraft:deepslate_coal_ore|2",
                            "minecraft:diamond_ore|3",
                            "minecraft:deepslate_diamond_ore|3",
                            "minecraft:ancient_debris|4"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.DoubleValue LAND_RENT_COST = BUILDER
            .comment("Contribution karma cost to rent one chunk plot.")
            .defineInRange("land.rentCost", 100.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.DoubleValue LAND_RENT_COST_ADDED_PER_OWNED_PRIVATE = BUILDER
            .comment("Extra contribution karma cost added per already-owned private plot when claiming another private plot.")
            .defineInRange("land.rentCostAddedPerOwnedPrivate", 20.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.DoubleValue LAND_TOWN_CLAIM_COST = BUILDER
            .comment("Civilization treasury contribution karma cost to claim one town (CIVIC) chunk.")
            .defineInRange("land.townClaimCost", 150.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.DoubleValue LAND_TOWN_CLAIM_COST_ADDED_PER_OWNED = BUILDER
            .comment("Extra treasury cost added per already-owned CIVIC town chunk when expanding town land.")
            .defineInRange("land.townClaimCostAddedPerOwned", 30.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.IntValue LAND_HUB_STARTER_AREA_BLOCKS = BUILDER
            .comment("Square starter town area side length in blocks, auto-claimed as CIVIC when first Community Hub is placed.")
            .defineInRange("land.hubStarterAreaBlocks", 50, 1, 1024);

    public static final ModConfigSpec.IntValue LAND_RENT_DAYS = BUILDER
            .comment("How many Minecraft days one rent payment grants.")
            .defineInRange("land.rentDays", 7, 1, 10_000);

    public static final ModConfigSpec.DoubleValue CIV_TREASURY_DEPOSIT_PERCENT = BUILDER
            .comment("Percent of hub contribution-karma reward mirrored into civilization treasury on deposit.")
            .defineInRange("economy.civTreasuryDepositPercent", 100.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue HUB_WITHDRAW_CREDIT_PENALTY_PERCENT = BUILDER
            .comment("Percent of the item's deposit credit value to deduct from recipient contribution karma on hub withdrawal.")
            .defineInRange("economy.hubWithdrawCreditPenaltyPercent", 100.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue LAND_UPKEEP_COST = BUILDER
            .comment("Recurring contribution karma upkeep cost per private plot per upkeep interval.")
            .defineInRange("land.upkeepCost", 20.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.IntValue LAND_UPKEEP_INTERVAL_DAYS = BUILDER
            .comment("How many Minecraft days between private-plot upkeep charges.")
            .defineInRange("land.upkeepIntervalDays", 1, 1, 10_000);

    public static final ModConfigSpec.IntValue LAND_UPKEEP_GRACE_DAYS = BUILDER
            .comment("How many Minecraft days private plots can remain unpaid before repossession.")
            .defineInRange("land.upkeepGraceDays", 3, 1, 10_000);

    public static final ModConfigSpec.BooleanValue LAND_BLOCK_UNCLAIMED_BUILDING = BUILDER
            .comment("When true, breaking is denied in wilderness/unzoned chunks. Placement is always denied there.")
            .define("land.blockUnclaimedBuilding", false);

    public static final ModConfigSpec.IntValue LAND_WAND_VISUALIZE_RADIUS_CHUNKS = BUILDER
            .comment("How many chunks around the player the land wand will visualize.")
            .defineInRange("land.wandVisualizeRadiusChunks", 6, 1, 64);

    public static final ModConfigSpec.IntValue LAND_WAND_MAX_SELECTION_CHUNKS = BUILDER
            .comment("Maximum chunk count that can be zoned/cleared in one land-wand selection action.")
            .defineInRange("land.wandMaxSelectionChunks", 256, 1, 10_000);

    public static final ModConfigSpec.ConfigValue<String> LAND_FTB_MAYOR_DEFAULT_CLAIM_MODE = BUILDER
            .comment("Default FTB map claim mode when mayor/admin personal mode is AUTO. Valid: civic, private.")
            .define("land.ftbMayorDefaultClaimMode", "civic");

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CIVILIZATION_ID = BUILDER
            .comment("Civilization id assigned to players that do not currently belong to one.")
            .define("civ.defaultId", "unaligned");

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CIVILIZATION_NAME = BUILDER
            .comment("Display name used when creating the default civilization automatically.")
            .define("civ.defaultName", "Unaligned");

    public static final ModConfigSpec.IntValue MAX_EXPLOSIVES_EXPERTS_PER_CIV = BUILDER
            .comment("Maximum number of designated explosives experts per civilization. Set to 0 to disable the role.")
            .defineInRange("civ.maxExplosivesExpertsPerCivilization", 1, 0, 256);

    public static final ModConfigSpec.BooleanValue REQUIRE_FOUNDER_APPROVAL = BUILDER
            .comment("When true, only approved players (or admins) can found new civilizations.")
            .define("civ.requireFounderApproval", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXPLOSIVES_RESTRICTED_ITEMS = BUILDER
            .comment("Items treated as regulated explosives. Players need Explosives Expert authorization and progression to use them.")
            .defineListAllowEmpty(
                    "explosives.restrictedItems",
                    List.of(
                            "minecraft:tnt",
                            "minecraft:end_crystal",
                            "minecraft:tnt_minecart",
                            "minecraft:wither_skeleton_skull",
                            "minecraft:respawn_anchor"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.BooleanValue EXPLOSIVES_BLOCK_NON_PLAYER_DAMAGE_IN_CLAIMS = BUILDER
            .comment("When true, explosions without an accountable player cannot damage claimed civilization land.")
            .define("explosives.blockNonPlayerDamageInClaims", true);

    public static final ModConfigSpec.BooleanValue ADMIN_BYPASS_RESTRICTIONS = BUILDER
            .comment("When true, players with high operator permission bypass RealCiv restrictions.")
            .define("admin.bypassRestrictions", false);

    public static final ModConfigSpec.IntValue MAX_AUDIT_LOGS = BUILDER
            .comment("Maximum number of audit log entries to retain in saved data.")
            .defineInRange("admin.maxAuditLogs", 500, 50, 20_000);

    public static final ModConfigSpec.IntValue DENY_MESSAGE_COOLDOWN_TICKS = BUILDER
            .comment("Per-player cooldown for repeated denial messages.")
            .defineInRange("ui.denyMessageCooldownTicks", 30, 1, 1200);

    public static final ModConfigSpec.IntValue HUB_STOCK_LIST_LIMIT = BUILDER
            .comment("How many stock entries are shown by /realciv hub stock.")
            .defineInRange("ui.hubStockListLimit", 12, 1, 200);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private RealCivConfig() {
    }

    private static boolean isNonNegativeInteger(Object value) {
        return value instanceof Integer integer && integer >= 0;
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }

    public static int farmerLimitForLevel(int farmerLevel) {
        return RealCivUtil.valueForLevel(farmerLevel, FARMER_LIMITS.get(), 8);
    }

    public static int minerLimitForLevel(int minerLevel) {
        return RealCivUtil.valueForLevel(minerLevel, MINER_LIMITS.get(), 40);
    }

    public static int terraformerLimitForLevel(int terraformerLevel) {
        return RealCivUtil.valueForLevel(terraformerLevel, TERRAFORMER_LIMITS.get(), 40);
    }

    public static int lumberjackLimitForLevel(int lumberjackLevel) {
        return RealCivUtil.valueForLevel(lumberjackLevel, LUMBERJACK_LIMITS.get(), 8);
    }

    public static int hunterLimitForLevel(int hunterLevel) {
        return RealCivUtil.valueForLevel(hunterLevel, HUNTER_LIMITS.get(), 1);
    }

    public static int warriorLimitForLevel(int warriorLevel) {
        return RealCivUtil.valueForLevel(warriorLevel, WARRIOR_LIMITS.get(), 1);
    }

    public static int explosivesExpertLimitForLevel(int explosivesLevel) {
        return RealCivUtil.valueForLevel(explosivesLevel, EXPLOSIVES_EXPERT_LIMITS.get(), 1);
    }

    public static int crafterLimitForLevel(int crafterLevel) {
        return RealCivUtil.valueForLevel(crafterLevel, CRAFTER_LIMITS.get(), 64);
    }

    public static int warriorXpPerPlayerKill() {
        return Math.max(0, WARRIOR_XP_PER_PLAYER_KILL.get());
    }

    public static int warriorGeneralXpPerPlayerKill() {
        return Math.max(0, WARRIOR_GENERAL_XP_PER_PLAYER_KILL.get());
    }

    public static int explosivesExpertXpPerUse() {
        return Math.max(0, EXPLOSIVES_EXPERT_XP_PER_USE.get());
    }

    public static int explosivesExpertGeneralXpPerUse() {
        return Math.max(0, EXPLOSIVES_EXPERT_GENERAL_XP_PER_USE.get());
    }

    public static int professionLevelFromXp(int xp) {
        return RealCivUtil.levelFromThresholds(xp, PROFESSION_XP_THRESHOLDS.get());
    }

    public static int generalLevelFromXp(int xp) {
        return RealCivUtil.levelFromThresholds(xp, GENERAL_XP_THRESHOLDS.get());
    }

    public static long rentCostCents() {
        return RealCivUtil.creditsToCents(LAND_RENT_COST.get());
    }

    public static long rentCostAddedPerOwnedPrivateCents() {
        return RealCivUtil.creditsToCents(LAND_RENT_COST_ADDED_PER_OWNED_PRIVATE.get());
    }

    public static long townClaimCostCents() {
        return RealCivUtil.creditsToCents(LAND_TOWN_CLAIM_COST.get());
    }

    public static long townClaimCostAddedPerOwnedCents() {
        return RealCivUtil.creditsToCents(LAND_TOWN_CLAIM_COST_ADDED_PER_OWNED.get());
    }

    public static int hubStarterAreaBlocks() {
        return Math.max(1, LAND_HUB_STARTER_AREA_BLOCKS.get());
    }

    public static long rentDurationTicks() {
        return 24_000L * LAND_RENT_DAYS.get();
    }

    public static long upkeepCostCents() {
        return RealCivUtil.creditsToCents(LAND_UPKEEP_COST.get());
    }

    public static double hubWithdrawCreditPenaltyRatio() {
        return Math.max(0.0D, HUB_WITHDRAW_CREDIT_PENALTY_PERCENT.get() / 100.0D);
    }

    public static double civTreasuryDepositRatio() {
        return Math.max(0.0D, Math.min(1.0D, CIV_TREASURY_DEPOSIT_PERCENT.get() / 100.0D));
    }

    public static long upkeepIntervalTicks() {
        return 24_000L * LAND_UPKEEP_INTERVAL_DAYS.get();
    }

    public static long upkeepGraceTicks() {
        return 24_000L * LAND_UPKEEP_GRACE_DAYS.get();
    }

    public static boolean blockUnclaimedBuilding() {
        return LAND_BLOCK_UNCLAIMED_BUILDING.get();
    }

    public static int landWandVisualizeRadiusChunks() {
        return Math.max(1, LAND_WAND_VISUALIZE_RADIUS_CHUNKS.get());
    }

    public static int landWandMaxSelectionChunks() {
        return Math.max(1, LAND_WAND_MAX_SELECTION_CHUNKS.get());
    }

    public static String ftbMayorDefaultClaimMode() {
        String raw = LAND_FTB_MAYOR_DEFAULT_CLAIM_MODE.get();
        if (raw == null) {
            return "civic";
        }
        String mode = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if ("private".equals(mode)) {
            return "private";
        }
        return "civic";
    }

    public static String defaultCivilizationId() {
        String value = DEFAULT_CIVILIZATION_ID.get();
        if (value == null || value.isBlank()) {
            return "unaligned";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static String defaultCivilizationName() {
        String value = DEFAULT_CIVILIZATION_NAME.get();
        if (value == null || value.isBlank()) {
            return "Unaligned";
        }
        return value.trim();
    }

    public static boolean adminBypassRestrictions() {
        return ADMIN_BYPASS_RESTRICTIONS.get();
    }

    public static boolean requireFounderApproval() {
        return REQUIRE_FOUNDER_APPROVAL.get();
    }

    public static int maxExplosivesExpertsPerCivilization() {
        return Math.max(0, MAX_EXPLOSIVES_EXPERTS_PER_CIV.get());
    }

    public static boolean blockNonPlayerExplosionDamageInClaims() {
        return EXPLOSIVES_BLOCK_NON_PLAYER_DAMAGE_IN_CLAIMS.get();
    }

    public static Set<ResourceLocation> regulatedExplosiveItems() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String raw : EXPLOSIVES_RESTRICTED_ITEMS.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                out.add(ResourceLocation.parse(line));
            } catch (Exception ignored) {
            }
        }
        return Set.copyOf(out);
    }

    public static boolean carryCapPickupEnabled() {
        return CARRY_CAP_PICKUP_ENABLED.get();
    }

    public static boolean carryCapCraftEnabled() {
        return CARRY_CAP_CRAFT_ENABLED.get();
    }

    public static Map<Profession, Double> carryCapProfessionMultipliers() {
        Map<Profession, Double> multipliers = new HashMap<>();
        multipliers.put(Profession.FARMER, 1.0D);
        multipliers.put(Profession.MINER, 1.0D);
        multipliers.put(Profession.TERRAFORMER, 1.0D);
        multipliers.put(Profession.LUMBERJACK, 1.0D);
        multipliers.put(Profession.HUNTER, 1.0D);
        multipliers.put(Profession.WARRIOR, 1.0D);
        multipliers.put(Profession.EXPLOSIVES_EXPERT, 1.0D);
        multipliers.put(Profession.CRAFTER, 1.0D);

        for (String raw : CARRY_CAP_PROFESSION_MULTIPLIERS.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 2) {
                continue;
            }

            Profession profession = Profession.fromConfigName(parts[0].trim());
            if (profession == null || profession == Profession.NONE) {
                continue;
            }

            Double parsed = tryParseDouble(parts[1].trim());
            if (parsed == null) {
                continue;
            }
            multipliers.put(profession, Math.max(0.0D, parsed));
        }

        return multipliers;
    }

    public static Map<ResourceLocation, Integer> carryCapItemMaxOverrides() {
        Map<ResourceLocation, Integer> overrides = new HashMap<>();
        for (String raw : CARRY_CAP_ITEM_MAX_OVERRIDES.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 2) {
                continue;
            }

            ResourceLocation itemId;
            try {
                itemId = ResourceLocation.parse(parts[0].trim());
            } catch (Exception ex) {
                continue;
            }

            Integer maxCount = tryParseInt(parts[1].trim());
            if (maxCount == null) {
                continue;
            }
            overrides.put(itemId, Math.max(0, maxCount));
        }
        return overrides;
    }

    public static Map<ResourceLocation, Integer> breakActionCostOverrides() {
        Map<ResourceLocation, Integer> overrides = new HashMap<>();
        for (String raw : BREAK_ACTION_COST_OVERRIDES.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 2) {
                continue;
            }

            ResourceLocation blockId;
            try {
                blockId = ResourceLocation.parse(parts[0].trim());
            } catch (Exception ex) {
                continue;
            }

            Integer cost = tryParseInt(parts[1].trim());
            if (cost == null) {
                continue;
            }
            overrides.put(blockId, Math.max(1, cost));
        }
        return overrides;
    }

    public static boolean migrateLegacyCommonConfigIfNeeded() {
        boolean changed = false;
        List<Integer> current = sanitizeIntegerList(LUMBERJACK_LIMITS.get());
        if (current.equals(LEGACY_LUMBERJACK_LIMITS)) {
            LUMBERJACK_LIMITS.set(CURRENT_LUMBERJACK_LIMITS);
            RealCivMod.LOGGER.info(
                    "Migrated legacy lumberjack limits {} -> {} for RealCiv common config.",
                    LEGACY_LUMBERJACK_LIMITS,
                    CURRENT_LUMBERJACK_LIMITS);
            changed = true;
        }

        String configuredCivId = DEFAULT_CIVILIZATION_ID.get();
        if (configuredCivId != null && configuredCivId.equalsIgnoreCase(LEGACY_DEFAULT_CIV_ID)) {
            DEFAULT_CIVILIZATION_ID.set("unaligned");
            RealCivMod.LOGGER.info("Migrated legacy default civilization id '{}' -> 'unaligned'.", configuredCivId);
            changed = true;
        }

        String configuredCivName = DEFAULT_CIVILIZATION_NAME.get();
        if (configuredCivName != null && configuredCivName.trim().equals(LEGACY_DEFAULT_CIV_NAME)) {
            DEFAULT_CIVILIZATION_NAME.set("Unaligned");
            RealCivMod.LOGGER.info("Migrated legacy default civilization name '{}' -> 'Unaligned'.", configuredCivName);
            changed = true;
        }

        if (LAND_BLOCK_UNCLAIMED_BUILDING.get() && changed) {
            LAND_BLOCK_UNCLAIMED_BUILDING.set(false);
            RealCivMod.LOGGER.info("Migrated legacy land.blockUnclaimedBuilding true -> false.");
        }

        if (changed) {
            SPEC.save();
        }
        return changed;
    }

    private static List<Integer> sanitizeIntegerList(List<? extends Integer> raw) {
        List<Integer> values = new ArrayList<>(raw.size());
        for (Integer value : raw) {
            values.add(value == null ? 0 : value);
        }
        return values;
    }

    public static double defaultPersonalWithdrawRatio() {
        return Math.max(0.0D, Math.min(1.0D, DEFAULT_PERSONAL_WITHDRAW_PERCENT.get() / 100.0D));
    }

    public static double deathActionRefundRatio() {
        return Math.max(0.0D, Math.min(1.0D, DEATH_ACTION_REFUND_PERCENT.get() / 100.0D));
    }

    public static boolean staleActionResetEnabled() {
        return STALE_ACTION_RESET_ENABLED.get();
    }

    public static long staleActionResetMillis() {
        long minutes = Math.max(1L, STALE_ACTION_RESET_MINUTES.get());
        return minutes * 60_000L;
    }

    public static boolean useProfessionRuleFiles() {
        return HUB_USE_PROFESSION_RULE_FILES.get();
    }

    public static String hubProfessionRuleDirectory() {
        String configured = HUB_PROFESSION_RULE_DIRECTORY.get();
        if (configured == null || configured.isBlank()) {
            return "realciv/hub";
        }
        return configured.trim();
    }

    public static void invalidateExternalRuleFileCache() {
        ProfessionRuleFileLoader.invalidateCache();
    }

    public static Map<ResourceLocation, RewardRule> rewardRules() {
        @Nullable ProfessionRuleFileLoader.LoadedHubRules external = externalHubRulesOrNull();
        if (external != null) {
            Map<ResourceLocation, RewardRule> rules = new HashMap<>();
            for (Map.Entry<ResourceLocation, ProfessionRuleFileLoader.ParsedRewardEntry> entry : external.exactRewardEntries().entrySet()) {
                ProfessionRuleFileLoader.ParsedRewardEntry parsed = entry.getValue();
                rules.put(
                        entry.getKey(),
                        new RewardRule(
                                entry.getKey(),
                                parsed.profession(),
                                RealCivUtil.creditsToCents(parsed.creditsPerItem()),
                                parsed.professionXpPerItem(),
                                parsed.generalXpPerItem()));
            }
            return rules;
        }
        return parseLegacyRewardRules(HUB_REWARD_RULES.get());
    }

    public static List<TagRewardRule> tagRewardRules() {
        @Nullable ProfessionRuleFileLoader.LoadedHubRules external = externalHubRulesOrNull();
        if (external != null) {
            return external.tagRewardRules();
        }
        return parseLegacyTagRewardRules(HUB_TAG_REWARD_RULES.get());
    }

    public static Map<ResourceLocation, ItemResetRule> itemResetRules() {
        @Nullable ProfessionRuleFileLoader.LoadedHubRules external = externalHubRulesOrNull();
        if (external != null) {
            return external.itemResetRules();
        }
        return Map.of();
    }

    public static List<TagResetRule> tagResetRules() {
        @Nullable ProfessionRuleFileLoader.LoadedHubRules external = externalHubRulesOrNull();
        if (external != null) {
            return external.tagResetRules();
        }
        return parseLegacyTagResetRules(HUB_TAG_RESET_RULES.get());
    }

    @Nullable
    private static ProfessionRuleFileLoader.LoadedHubRules externalHubRulesOrNull() {
        if (!useProfessionRuleFiles()) {
            return null;
        }
        return ProfessionRuleFileLoader.loadFromConfiguredFiles(
                hubProfessionRuleDirectory(),
                HUB_REWARD_RULES.get(),
                HUB_TAG_REWARD_RULES.get(),
                HUB_TAG_RESET_RULES.get());
    }

    private static Map<ResourceLocation, RewardRule> parseLegacyRewardRules(List<? extends String> lines) {
        Map<ResourceLocation, RewardRule> rules = new HashMap<>();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }

            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 5) {
                RealCivMod.LOGGER.warn("Skipping invalid hub reward rule (expected 5 fields): {}", line);
                continue;
            }

            ResourceLocation itemId;
            try {
                itemId = ResourceLocation.parse(parts[0].trim());
            } catch (Exception ex) {
                RealCivMod.LOGGER.warn("Skipping reward rule with invalid item id '{}': {}", parts[0], line);
                continue;
            }

            Profession profession = Profession.fromConfigName(parts[1].trim());
            if (profession == null) {
                RealCivMod.LOGGER.warn("Skipping reward rule with invalid profession '{}': {}", parts[1], line);
                continue;
            }

            Double credits = tryParseDouble(parts[2].trim());
            Integer professionXp = tryParseInt(parts[3].trim());
            Integer generalXp = tryParseInt(parts[4].trim());
            if (credits == null || professionXp == null || generalXp == null) {
                RealCivMod.LOGGER.warn("Skipping reward rule with invalid numeric values: {}", line);
                continue;
            }

            rules.put(
                    itemId,
                    new RewardRule(
                            itemId,
                            profession,
                            RealCivUtil.creditsToCents(Math.max(0.0D, credits)),
                            Math.max(0, professionXp),
                            Math.max(0, generalXp)));
        }
        return rules;
    }

    private static List<TagRewardRule> parseLegacyTagRewardRules(List<? extends String> lines) {
        ArrayList<TagRewardRule> rules = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }

            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 6) {
                RealCivMod.LOGGER.warn("Skipping invalid hub tag reward rule (expected 6 fields): {}", line);
                continue;
            }

            TagRewardRule.SelectorType selectorType = TagRewardRule.SelectorType.fromConfig(parts[0].trim());
            if (selectorType == null) {
                RealCivMod.LOGGER.warn("Skipping hub tag reward rule with invalid selector '{}': {}", parts[0], line);
                continue;
            }

            ResourceLocation tagId;
            try {
                tagId = ResourceLocation.parse(parts[1].trim());
            } catch (Exception ex) {
                RealCivMod.LOGGER.warn("Skipping hub tag reward rule with invalid tag id '{}': {}", parts[1], line);
                continue;
            }

            Profession profession = Profession.fromConfigName(parts[2].trim());
            if (profession == null) {
                RealCivMod.LOGGER.warn("Skipping hub tag reward rule with invalid profession '{}': {}", parts[2], line);
                continue;
            }

            Double credits = tryParseDouble(parts[3].trim());
            Integer professionXp = tryParseInt(parts[4].trim());
            Integer generalXp = tryParseInt(parts[5].trim());
            if (credits == null || professionXp == null || generalXp == null) {
                RealCivMod.LOGGER.warn("Skipping hub tag reward rule with invalid numeric values: {}", line);
                continue;
            }

            rules.add(new TagRewardRule(
                    selectorType,
                    tagId,
                    profession,
                    RealCivUtil.creditsToCents(Math.max(0.0D, credits)),
                    Math.max(0, professionXp),
                    Math.max(0, generalXp)));
        }
        return List.copyOf(rules);
    }

    private static List<TagResetRule> parseLegacyTagResetRules(List<? extends String> lines) {
        ArrayList<TagResetRule> rules = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }

            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 4) {
                RealCivMod.LOGGER.warn("Skipping invalid hub tag reset rule (expected 4 fields): {}", line);
                continue;
            }

            TagRewardRule.SelectorType selectorType = TagRewardRule.SelectorType.fromConfig(parts[0].trim());
            if (selectorType == null) {
                RealCivMod.LOGGER.warn("Skipping hub tag reset rule with invalid selector '{}': {}", parts[0], line);
                continue;
            }

            ResourceLocation tagId;
            try {
                tagId = ResourceLocation.parse(parts[1].trim());
            } catch (Exception ex) {
                RealCivMod.LOGGER.warn("Skipping hub tag reset rule with invalid tag id '{}': {}", parts[1], line);
                continue;
            }

            Profession profession = Profession.fromConfigName(parts[2].trim());
            if (profession == null || profession == Profession.NONE) {
                RealCivMod.LOGGER.warn("Skipping hub tag reset rule with invalid profession '{}': {}", parts[2], line);
                continue;
            }

            Double actionsPerItem = tryParseDouble(parts[3].trim());
            if (actionsPerItem == null) {
                RealCivMod.LOGGER.warn("Skipping hub tag reset rule with invalid actions_per_item '{}': {}", parts[3], line);
                continue;
            }

            rules.add(new TagResetRule(
                    selectorType,
                    tagId,
                    profession,
                    Math.max(0.0D, actionsPerItem)));
        }
        return List.copyOf(rules);
    }

    @Nullable
    private static Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
