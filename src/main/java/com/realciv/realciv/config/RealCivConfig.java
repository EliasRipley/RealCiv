package com.realciv.realciv.config;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class RealCivConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

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

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> LUMBERJACK_LIMITS = BUILDER
            .comment("Lumberjack block-break limits by lumberjack level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.lumberjackLimits",
                    List.of(32, 64, 96, 160, 256, 384),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> HUNTER_LIMITS = BUILDER
            .comment("Hunter kill limits by hunter level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.hunterLimits",
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
                            "ITEM_TAG|realciv:lumberjack_contributions|LUMBERJACK|1.00|2|1",
                            "ITEM_TAG|realciv:hunter_contributions|HUNTER|2.00|3|2",
                            "ITEM_TAG|realciv:crafter_contributions|CRAFTER|1.20|2|1",
                            "BLOCK_TAG|minecraft:mineable/pickaxe|MINER|0.10|1|1",
                            "BLOCK_TAG|minecraft:logs|LUMBERJACK|1.00|2|1",
                            "BLOCK_TAG|minecraft:bamboo_blocks|LUMBERJACK|1.00|2|1"),
                    () -> "",
                    RealCivConfig::isString);

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

    public static final ModConfigSpec.DoubleValue LAND_RENT_COST = BUILDER
            .comment("Social credit cost to rent one chunk plot.")
            .defineInRange("land.rentCost", 100.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.IntValue LAND_RENT_DAYS = BUILDER
            .comment("How many Minecraft days one rent payment grants.")
            .defineInRange("land.rentDays", 7, 1, 10_000);

    public static final ModConfigSpec.DoubleValue LAND_UPKEEP_COST = BUILDER
            .comment("Recurring social credit upkeep cost per private plot per upkeep interval.")
            .defineInRange("land.upkeepCost", 20.0D, 0.0D, 1_000_000.0D);

    public static final ModConfigSpec.IntValue LAND_UPKEEP_INTERVAL_DAYS = BUILDER
            .comment("How many Minecraft days between private-plot upkeep charges.")
            .defineInRange("land.upkeepIntervalDays", 1, 1, 10_000);

    public static final ModConfigSpec.IntValue LAND_UPKEEP_GRACE_DAYS = BUILDER
            .comment("How many Minecraft days private plots can remain unpaid before repossession.")
            .defineInRange("land.upkeepGraceDays", 3, 1, 10_000);

    public static final ModConfigSpec.BooleanValue LAND_BLOCK_UNCLAIMED_BUILDING = BUILDER
            .comment("When true, block placement/breaking is denied in chunks with no zoning record.")
            .define("land.blockUnclaimedBuilding", true);

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CIVILIZATION_ID = BUILDER
            .comment("Civilization id assigned to players that do not currently belong to one.")
            .define("civ.defaultId", "commonwealth");

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CIVILIZATION_NAME = BUILDER
            .comment("Display name used when creating the default civilization automatically.")
            .define("civ.defaultName", "Commonwealth");

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

    public static int lumberjackLimitForLevel(int lumberjackLevel) {
        return RealCivUtil.valueForLevel(lumberjackLevel, LUMBERJACK_LIMITS.get(), 32);
    }

    public static int hunterLimitForLevel(int hunterLevel) {
        return RealCivUtil.valueForLevel(hunterLevel, HUNTER_LIMITS.get(), 1);
    }

    public static int crafterLimitForLevel(int crafterLevel) {
        return RealCivUtil.valueForLevel(crafterLevel, CRAFTER_LIMITS.get(), 64);
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

    public static long rentDurationTicks() {
        return 24_000L * LAND_RENT_DAYS.get();
    }

    public static long upkeepCostCents() {
        return RealCivUtil.creditsToCents(LAND_UPKEEP_COST.get());
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

    public static String defaultCivilizationId() {
        String value = DEFAULT_CIVILIZATION_ID.get();
        if (value == null || value.isBlank()) {
            return "commonwealth";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static String defaultCivilizationName() {
        String value = DEFAULT_CIVILIZATION_NAME.get();
        if (value == null || value.isBlank()) {
            return "Commonwealth";
        }
        return value.trim();
    }

    public static double defaultPersonalWithdrawRatio() {
        return Math.max(0.0D, Math.min(1.0D, DEFAULT_PERSONAL_WITHDRAW_PERCENT.get() / 100.0D));
    }

    public static Map<ResourceLocation, RewardRule> rewardRules() {
        Map<ResourceLocation, RewardRule> rules = new HashMap<>();

        for (String raw : HUB_REWARD_RULES.get()) {
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

            RewardRule rule = new RewardRule(
                    itemId,
                    profession,
                    RealCivUtil.creditsToCents(Math.max(0.0D, credits)),
                    Math.max(0, professionXp),
                    Math.max(0, generalXp));

            rules.put(itemId, rule);
        }

        return rules;
    }

    public static List<TagRewardRule> tagRewardRules() {
        java.util.ArrayList<TagRewardRule> rules = new java.util.ArrayList<>();

        for (String raw : HUB_TAG_REWARD_RULES.get()) {
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

        return java.util.List.copyOf(rules);
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
