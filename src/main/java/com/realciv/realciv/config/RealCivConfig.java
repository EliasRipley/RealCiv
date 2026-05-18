package com.realciv.realciv.config;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.logic.ItemResetRule;
import com.realciv.realciv.logic.ProfessionEventHook;
import com.realciv.realciv.logic.ProfessionEventHookRule;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import com.realciv.realciv.logic.TagResetRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class RealCivConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    static final List<Integer> LEGACY_LUMBERJACK_LIMITS = List.of(32, 64, 96, 160, 256, 384);
    static final List<Integer> CURRENT_LUMBERJACK_LIMITS = List.of(8, 16, 32, 64, 96, 128);
    static final String LEGACY_DEFAULT_CIV_ID = "commonwealth";
    static final String LEGACY_DEFAULT_CIV_NAME = "Commonwealth";
    @Nullable
    static List<ProfessionEventHookRule> cachedProfessionEventHookRules;

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
            .comment("Lumberjack block-break and strip limits by lumberjack level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.lumberjackLimits",
                    List.of(10, 16, 22, 28, 34, 40),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> FISHER_LIMITS = BUILDER
            .comment("Fisher catch limits by fisher level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.fisherLimits",
                    List.of(8, 16, 24, 40, 64, 96),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> HUNTER_LIMITS = BUILDER
            .comment("Hunter kill limits by hunter level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.hunterLimits",
                    List.of(1, 2, 4, 6, 10, 16),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUNTER_MOB_ACTION_CAPS = BUILDER
            .comment("Optional per-mob Hunter action caps by hunter level.")
            .comment("Format: entity_id|cap0,cap1,cap2,...")
            .comment("Cap value <= 0 means no cap for that level. Missing level values reuse the last provided value.")
            .comment("Example: minecraft:ender_dragon|0,0,0,0,0,0,1")
            .defineListAllowEmpty(
                    "profession.hunterMobActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUNTER_MOB_MIN_LEVELS = BUILDER
            .comment("Optional per-mob minimum Hunter level requirements.")
            .comment("Format: entity_id|required_level")
            .comment("Example: minecraft:warden|40")
            .defineListAllowEmpty(
                    "profession.hunterMobMinLevels",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

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

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> ENCHANTER_LIMITS = BUILDER
            .comment("Enchanter action limits by enchanter level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.enchanterLimits",
                    List.of(1, 2, 3, 4, 5, 6),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> BREWER_LIMITS = BUILDER
            .comment("Brewer action limits by brewer level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.brewerLimits",
                    List.of(1, 2, 3, 4, 5, 6),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TRADER_LIMITS = BUILDER
            .comment("Trader action limits by trader level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.traderLimits",
                    List.of(1, 2, 3, 4, 5, 6),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.BooleanValue PROFESSION_USE_LINEAR_LIMIT_FORMULAS = BUILDER
            .comment("When true, profession action caps use base + (perLevel * level) values below.")
            .comment("When false, legacy profession.*Limits lists are used instead.")
            .define("profession.useLinearLimitFormulas", true);

    public static final ModConfigSpec.IntValue FARMER_LIMIT_BASE = BUILDER
            .comment("Farmer level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.farmerLimitBase", 4, 0, 1_000_000);
    public static final ModConfigSpec.IntValue FARMER_LIMIT_PER_LEVEL = BUILDER
            .comment("Farmer action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.farmerLimitPerLevel", 4, 0, 1_000_000);

    public static final ModConfigSpec.IntValue MINER_LIMIT_BASE = BUILDER
            .comment("Miner level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.minerLimitBase", 40, 0, 1_000_000);
    public static final ModConfigSpec.IntValue MINER_LIMIT_PER_LEVEL = BUILDER
            .comment("Miner action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.minerLimitPerLevel", 10, 0, 1_000_000);

    public static final ModConfigSpec.IntValue TERRAFORMER_LIMIT_BASE = BUILDER
            .comment("Terraformer level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.terraformerLimitBase", 40, 0, 1_000_000);
    public static final ModConfigSpec.IntValue TERRAFORMER_LIMIT_PER_LEVEL = BUILDER
            .comment("Terraformer action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.terraformerLimitPerLevel", 10, 0, 1_000_000);

    public static final ModConfigSpec.IntValue LUMBERJACK_LIMIT_BASE = BUILDER
            .comment("Lumberjack level 0 action cap when profession.useLinearLimitFormulas=true (logs+stripping combined).")
            .defineInRange("profession.lumberjackLimitBase", 10, 0, 1_000_000);
    public static final ModConfigSpec.IntValue LUMBERJACK_LIMIT_PER_LEVEL = BUILDER
            .comment("Lumberjack action cap increase per level when profession.useLinearLimitFormulas=true (logs+stripping combined).")
            .defineInRange("profession.lumberjackLimitPerLevel", 6, 0, 1_000_000);

    public static final ModConfigSpec.IntValue FISHER_LIMIT_BASE = BUILDER
            .comment("Fisher level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.fisherLimitBase", 4, 0, 1_000_000);
    public static final ModConfigSpec.IntValue FISHER_LIMIT_PER_LEVEL = BUILDER
            .comment("Fisher action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.fisherLimitPerLevel", 4, 0, 1_000_000);

    public static final ModConfigSpec.IntValue HUNTER_LIMIT_BASE = BUILDER
            .comment("Hunter level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.hunterLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue HUNTER_LIMIT_PER_LEVEL = BUILDER
            .comment("Hunter action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.hunterLimitPerLevel", 2, 0, 1_000_000);

    public static final ModConfigSpec.IntValue WARRIOR_LIMIT_BASE = BUILDER
            .comment("Warrior level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.warriorLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue WARRIOR_LIMIT_PER_LEVEL = BUILDER
            .comment("Warrior action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.warriorLimitPerLevel", 2, 0, 1_000_000);

    public static final ModConfigSpec.IntValue EXPLOSIVES_EXPERT_LIMIT_BASE = BUILDER
            .comment("Explosives Expert level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.explosivesExpertLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue EXPLOSIVES_EXPERT_LIMIT_PER_LEVEL = BUILDER
            .comment("Explosives Expert action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.explosivesExpertLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.IntValue CRAFTER_LIMIT_BASE = BUILDER
            .comment("Crafter level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.crafterLimitBase", 64, 0, 1_000_000);
    public static final ModConfigSpec.IntValue CRAFTER_LIMIT_PER_LEVEL = BUILDER
            .comment("Crafter action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.crafterLimitPerLevel", 64, 0, 1_000_000);

    public static final ModConfigSpec.IntValue ENCHANTER_LIMIT_BASE = BUILDER
            .comment("Enchanter level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.enchanterLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue ENCHANTER_LIMIT_PER_LEVEL = BUILDER
            .comment("Enchanter action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.enchanterLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.IntValue BREWER_LIMIT_BASE = BUILDER
            .comment("Brewer level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.brewerLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue BREWER_LIMIT_PER_LEVEL = BUILDER
            .comment("Brewer action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.brewerLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.IntValue TRADER_LIMIT_BASE = BUILDER
            .comment("Trader level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.traderLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue TRADER_LIMIT_PER_LEVEL = BUILDER
            .comment("Trader action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.traderLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SHEPHERD_LIMITS = BUILDER
            .comment("Shepherd shear limits by shepherd level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.shepherdLimits",
                    List.of(2, 6, 10, 14, 18, 22),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> EXPLORER_LIMITS = BUILDER
            .comment("Explorer rocket-use limits by explorer level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.explorerLimits",
                    List.of(1, 2, 3, 4, 5, 6),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TREASURE_HUNTER_LIMITS = BUILDER
            .comment("Treasure Hunter chest-open limits by level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.treasureHunterLimits",
                    List.of(1, 2, 3, 4, 5, 6),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> BREEDER_LIMITS = BUILDER
            .comment("Breeder feed/breed limits by breeder level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.breederLimits",
                    List.of(2, 6, 10, 14, 18, 22),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SMITHY_LIMITS = BUILDER
            .comment("Smithy anvil-use limits by smithy level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.smithyLimits",
                    List.of(1, 2, 3, 4, 5, 6),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SMELTER_LIMITS = BUILDER
            .comment("Smelter furnace-output limits by smelter level index (level 0 = first value).")
            .defineListAllowEmpty(
                    "profession.smelterLimits",
                    List.of(1, 2, 3, 4, 5, 6),
                    () -> 0,
                    RealCivConfig::isNonNegativeInteger);

    public static final ModConfigSpec.IntValue SHEPHERD_LIMIT_BASE = BUILDER
            .comment("Shepherd level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.shepherdLimitBase", 2, 0, 1_000_000);
    public static final ModConfigSpec.IntValue SHEPHERD_LIMIT_PER_LEVEL = BUILDER
            .comment("Shepherd action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.shepherdLimitPerLevel", 4, 0, 1_000_000);

    public static final ModConfigSpec.IntValue EXPLORER_LIMIT_BASE = BUILDER
            .comment("Explorer level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.explorerLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue EXPLORER_LIMIT_PER_LEVEL = BUILDER
            .comment("Explorer action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.explorerLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.IntValue TREASURE_HUNTER_LIMIT_BASE = BUILDER
            .comment("Treasure Hunter level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.treasureHunterLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue TREASURE_HUNTER_LIMIT_PER_LEVEL = BUILDER
            .comment("Treasure Hunter action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.treasureHunterLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.IntValue BREEDER_LIMIT_BASE = BUILDER
            .comment("Breeder level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.breederLimitBase", 2, 0, 1_000_000);
    public static final ModConfigSpec.IntValue BREEDER_LIMIT_PER_LEVEL = BUILDER
            .comment("Breeder action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.breederLimitPerLevel", 4, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SMITHY_LIMIT_BASE = BUILDER
            .comment("Smithy level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.smithyLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue SMITHY_LIMIT_PER_LEVEL = BUILDER
            .comment("Smithy action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.smithyLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SMELTER_LIMIT_BASE = BUILDER
            .comment("Smelter level 0 action cap when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.smelterLimitBase", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue SMELTER_LIMIT_PER_LEVEL = BUILDER
            .comment("Smelter action cap increase per level when profession.useLinearLimitFormulas=true.")
            .defineInRange("profession.smelterLimitPerLevel", 1, 0, 1_000_000);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_DAILY_ACTION_CAPS = BUILDER
            .comment("Optional daily action caps by profession and level.")
            .comment("Format: PROFESSION|cap0,cap1,cap2,...  (cap <= 0 disables cap for that level)")
            .comment("Example: MINER|0,0,128,192")
            .defineListAllowEmpty(
                    "profession.dailyActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MINER_BLOCK_ACTION_CAPS = BUILDER
            .comment("Optional miner per-block action-window caps by miner level.")
            .comment("Format: block_id|cap0,cap1,cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:iron_ore|2,4,6")
            .defineListAllowEmpty(
                    "profession.minerBlockActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MINER_DAILY_BLOCK_ACTION_CAPS = BUILDER
            .comment("Optional miner per-block daily caps by miner level.")
            .comment("Format: block_id|cap0,cap1,cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:iron_ore|2,4,6")
            .defineListAllowEmpty(
                    "profession.minerDailyBlockActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LUMBERJACK_BLOCK_ACTION_CAPS = BUILDER
            .comment("Optional lumberjack per-resource window caps by lumberjack level.")
            .comment("Format: block_id|cap0,cap1,cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:oak_log|4,8,12")
            .defineListAllowEmpty(
                    "profession.lumberjackBlockActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LUMBERJACK_DAILY_BLOCK_ACTION_CAPS = BUILDER
            .comment("Optional lumberjack per-resource daily caps by lumberjack level.")
            .comment("Format: block_id|cap0,cap1,cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:oak_log|16,32,64")
            .defineListAllowEmpty(
                    "profession.lumberjackDailyBlockActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TERRAFORMER_BLOCK_ACTION_CAPS = BUILDER
            .comment("Optional terraformer per-block window caps by terraformer level.")
            .comment("Format: block_id|cap0,cap1,cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:dirt|10,20,30")
            .defineListAllowEmpty(
                    "profession.terraformerBlockActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TERRAFORMER_DAILY_BLOCK_ACTION_CAPS = BUILDER
            .comment("Optional terraformer per-block daily caps by terraformer level.")
            .comment("Format: block_id|cap0,cap1,cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:dirt|40,80,120")
            .defineListAllowEmpty(
                    "profession.terraformerDailyBlockActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CRAFTER_ITEM_ACTION_CAPS = BUILDER
            .comment("Optional crafter per-item action-window caps by crafter level.")
            .comment("Format: item_id|cap0,cap1,cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:furnace|8,16,32,64")
            .defineListAllowEmpty(
                    "profession.crafterItemActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CRAFTER_DAILY_ITEM_ACTION_CAPS = BUILDER
            .comment("Optional crafter per-item daily caps by crafter level.")
            .comment("Format: item_id|daily_cap0,daily_cap1,daily_cap2,...  (cap <= 0 disables cap)")
            .comment("Example: minecraft:furnace|16,32,64,128")
            .defineListAllowEmpty(
                    "profession.crafterDailyItemActionCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_EVENT_HOOK_RULES = BUILDER
            .comment("Optional event-driven profession action hooks.")
            .comment("Legacy format: hook|profession|actions_per_trigger|optional custom deny message")
            .comment("Extended format: hook|profession|actions_per_trigger|key=value|key=value|...")
            .comment("Hooks: ANIMAL_BREED, ANIMAL_TAME, SHEAR_ENTITY, SHEAR_BLOCK, PLACE_SCAFFOLDING, BONEMEAL_USE")
            .comment("Additional hooks: TOOL_STRIP_LOG, TOOL_TILL_SOIL, TOOL_FLATTEN_PATH, TOOL_DOUSE_CAMPFIRE")
            .comment("Additional hooks: TOOL_SCRAPE_COPPER, TOOL_WAX_OFF, FARMLAND_TRAMPLE, VILLAGER_INTERACT")
            .comment("Additional hooks: VILLAGER_TRADE, ANVIL_USE, ANVIL_RENAME, ANVIL_REPAIR_TOOL, ANVIL_COMBINE_ENCHANT, ITEM_SMELT, ITEM_ENCHANT, POTION_BREW, ITEM_TOSS, STAT_AWARD")
            .comment("Option keys: min_profession_level, min_general_level, min_membership_hours,")
            .comment("window_seconds/window_minutes/window_hours, max_triggers, profession_xp, general_xp, stat_prefix, deny_message")
            .comment("Deny placeholders: %hook%, %profession%, %current%, %limit%, %cost%")
            .comment("Examples: ANIMAL_BREED|FARMER|1, TOOL_TILL_SOIL|FARMER|1, VILLAGER_INTERACT|CRAFTER|1")
            .comment("Example (quota): ANVIL_USE|CRAFTER|0|window_hours=24|max_triggers=1|profession_xp=25")
            .comment("Example (age gate): PLACE_SCAFFOLDING|TERRAFORMER|1|min_membership_hours=48")
            .defineListAllowEmpty(
                    "profession.eventHookRules",
                    List.of(
                            "ITEM_ENCHANT|ENCHANTER|1",
                            "POTION_BREW|BREWER|1",
                            "VILLAGER_TRADE|TRADER|1",
                            "TOOL_STRIP_LOG|LUMBERJACK|1",
                            "SHEAR_ENTITY|SHEPHERD|1",
                            "SHEAR_BLOCK|SHEPHERD|1",
                            "ANIMAL_BREED|BREEDER|1",
                            "ANIMAL_TAME|BREEDER|1",
                            "ANVIL_USE|SMITHY|0|min_profession_level=1",
                            "ANVIL_RENAME|SMITHY|0|min_profession_level=2",
                            "ANVIL_COMBINE_ENCHANT|SMITHY|0|min_profession_level=4",
                            "ANVIL_REPAIR_TOOL|SMITHY|0|min_profession_level=1",
                            "ITEM_SMELT|SMELTER|1"),
                    () -> "",
                    RealCivConfig::isString);

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

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_LEVEL_CAPS = BUILDER
            .comment("Optional max level caps by profession. Format: profession|max_level.")
            .comment("Example: MINER|120")
            .defineListAllowEmpty(
                    "progression.professionLevelCaps",
                    List.of(),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.IntValue MAX_PROFESSION_LEVEL_GAINS_PER_DAY = BUILDER
            .comment("Max profession levels a player can gain per real-world day, per profession. 0 disables this cap.")
            .defineInRange("progression.maxProfessionLevelGainsPerDay", 0, 0, 100_000);

    public static final ModConfigSpec.IntValue MAX_GENERAL_LEVEL_GAINS_PER_DAY = BUILDER
            .comment("Max general levels a player can gain per real-world day. 0 disables this cap.")
            .defineInRange("progression.maxGeneralLevelGainsPerDay", 0, 0, 100_000);

    public static final ModConfigSpec.DoubleValue DEATH_ACTION_REFUND_PERCENT = BUILDER
            .comment("Percent of all profession action counters refunded on death to avoid deadlocks from lost resources.")
            .defineInRange("progression.deathActionRefundPercent", 100.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue STALE_ACTION_RESET_ENABLED = BUILDER
            .comment("When true, profession action counters auto-reset to 0 after prolonged inactivity to prevent deadlocks.")
            .define("progression.staleActionResetEnabled", true);

    public static final ModConfigSpec.IntValue STALE_ACTION_RESET_MINUTES = BUILDER
            .comment("Real-time minutes after last profession action update before that profession counter is auto-reset to 0.")
            .defineInRange("progression.staleActionResetMinutes", 120, 1, 60 * 24 * 30);

    public static final ModConfigSpec.BooleanValue SPECIALIZATION_SINGLE_PROFESSION_LOCK_ENABLED = BUILDER
            .comment("When true, players must choose a profession focus and can only progress that focused profession.")
            .define("specialization.singleProfessionLockEnabled", false);

    public static final ModConfigSpec.BooleanValue SPECIALIZATION_XP_DECAY_ENABLED = BUILDER
            .comment("When true, gaining XP in one profession decays XP in all other professions.")
            .define("specialization.xpDecayEnabled", false);

    public static final ModConfigSpec.DoubleValue SPECIALIZATION_XP_DECAY_RATE = BUILDER
            .comment("XP decay multiplier applied to each non-active profession per profession XP gained.")
            .comment("Example: 1.0 means +10 XP in one profession removes 10 XP from each other profession.")
            .defineInRange("specialization.xpDecayRate", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.IntValue WARRIOR_XP_PER_PLAYER_KILL = BUILDER
            .comment("Warrior profession XP awarded instantly per enemy player kill.")
            .defineInRange("progression.warriorXpPerPlayerKill", 150, 0, 100_000);

    public static final ModConfigSpec.IntValue WARRIOR_GENERAL_XP_PER_PLAYER_KILL = BUILDER
            .comment("General XP awarded instantly per enemy player kill that counts toward warrior progression.")
            .defineInRange("progression.warriorGeneralXpPerPlayerKill", 10, 0, 100_000);

    public static final ModConfigSpec.BooleanValue WARRIOR_REQUIRE_HUB_REGISTRATION = BUILDER
            .comment("When true, warrior kill XP is queued and only awarded when the player returns to the Community Hub.")
            .define("progression.warriorRequireHubRegistration", false);

    public static final ModConfigSpec.BooleanValue WARRIOR_HOME_DEFENSE_NO_ACTION_COST = BUILDER
            .comment("When true, kills against enemy players inside your civilization's land do not consume warrior actions.")
            .comment("This lets defenders repel invaders without being hard-capped by warrior action limits.")
            .define("combat.warriorHomeDefenseNoActionCost", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUB_REWARD_RULES = BUILDER
            .comment("Accepted hub item rewards. Format: item_id|profession|credits|profession_xp|general_xp")
            .defineListAllowEmpty(
                    "hub.rewardRules",
                    List.of(
                            "minecraft:wheat|FARMER|0.10|2|1",
                            "minecraft:wheat_seeds|FARMER|0.10|1|0",
                            "minecraft:carrot|FARMER|0.10|2|1",
                            "minecraft:potato|FARMER|0.10|2|1",
                            "minecraft:beetroot|FARMER|0.10|2|1",
                            "minecraft:beetroot_seeds|FARMER|0.10|1|0",
                            "minecraft:melon_slice|FARMER|0.10|1|1",
                            "minecraft:pumpkin|FARMER|0.10|2|1",
                            "minecraft:nether_wart|FARMER|0.10|2|1",
                            "minecraft:sugar_cane|FARMER|0.10|1|1",
                            "minecraft:sweet_berries|FARMER|0.10|1|1",
                            "minecraft:cocoa_beans|FARMER|0.10|1|1",
                            "minecraft:cobblestone|MINER|0.10|1|1",
                            "minecraft:stone|MINER|0.10|1|1",
                            "minecraft:deepslate|MINER|0.10|1|1",
                            "minecraft:coal|MINER|0.10|3|2",
                            "minecraft:raw_iron|MINER|0.10|5|3",
                            "minecraft:raw_copper|MINER|0.10|3|2",
                            "minecraft:raw_gold|MINER|0.10|6|4",
                            "minecraft:diamond|MINER|0.10|8|4",
                            "minecraft:emerald|MINER|0.10|8|4",
                            "minecraft:redstone|MINER|0.10|2|1",
                            "minecraft:lapis_lazuli|MINER|0.10|3|2",
                            "minecraft:quartz|MINER|0.10|3|2",
                            "minecraft:amethyst_shard|MINER|0.10|3|2",
                            "minecraft:netherite_scrap|MINER|0.10|20|10",
                            "minecraft:iron_ore|MINER|0.10|6|4",
                            "minecraft:deepslate_iron_ore|MINER|0.10|6|4",
                            "minecraft:copper_ore|MINER|0.10|4|2",
                            "minecraft:deepslate_copper_ore|MINER|0.10|4|2",
                            "minecraft:gold_ore|MINER|0.10|7|4",
                            "minecraft:deepslate_gold_ore|MINER|0.10|7|4",
                            "minecraft:coal_ore|MINER|0.10|3|2",
                            "minecraft:deepslate_coal_ore|MINER|0.10|3|2",
                            "minecraft:diamond_ore|MINER|0.10|10|5",
                            "minecraft:deepslate_diamond_ore|MINER|0.10|10|5",
                            "minecraft:emerald_ore|MINER|0.10|10|5",
                            "minecraft:deepslate_emerald_ore|MINER|0.10|10|5",
                            "minecraft:redstone_ore|MINER|0.10|4|2",
                            "minecraft:deepslate_redstone_ore|MINER|0.10|4|2",
                            "minecraft:lapis_ore|MINER|0.10|5|3",
                            "minecraft:deepslate_lapis_ore|MINER|0.10|5|3",
                            "minecraft:nether_quartz_ore|MINER|0.10|4|2",
                            "minecraft:nether_gold_ore|MINER|0.10|4|2",
                            "minecraft:ancient_debris|MINER|0.10|24|12",
                            "minecraft:dirt|TERRAFORMER|0.10|1|1",
                            "minecraft:coarse_dirt|TERRAFORMER|0.10|1|1",
                            "minecraft:rooted_dirt|TERRAFORMER|0.10|2|1",
                            "minecraft:grass_block|TERRAFORMER|0.10|1|1",
                            "minecraft:podzol|TERRAFORMER|0.10|2|1",
                            "minecraft:mycelium|TERRAFORMER|0.10|2|1",
                            "minecraft:sand|TERRAFORMER|0.10|1|1",
                            "minecraft:red_sand|TERRAFORMER|0.10|1|1",
                            "minecraft:gravel|TERRAFORMER|0.10|2|1",
                            "minecraft:clay|TERRAFORMER|0.10|2|1",
                            "minecraft:clay_ball|TERRAFORMER|0.10|2|1",
                            "minecraft:soul_sand|TERRAFORMER|0.10|2|1",
                            "minecraft:soul_soil|TERRAFORMER|0.10|2|1",
                            "minecraft:mud|TERRAFORMER|0.10|2|1",
                            "minecraft:muddy_mangrove_roots|TERRAFORMER|0.10|2|1",
                            "minecraft:snow_block|TERRAFORMER|0.10|1|1",
                            "minecraft:oak_log|LUMBERJACK|0.10|2|1",
                            "minecraft:spruce_log|LUMBERJACK|0.10|2|1",
                            "minecraft:birch_log|LUMBERJACK|0.10|2|1",
                            "minecraft:jungle_log|LUMBERJACK|0.10|2|1",
                            "minecraft:acacia_log|LUMBERJACK|0.10|2|1",
                            "minecraft:dark_oak_log|LUMBERJACK|0.10|2|1",
                            "minecraft:cherry_log|LUMBERJACK|0.10|2|1",
                            "minecraft:mangrove_log|LUMBERJACK|0.10|2|1",
                            "minecraft:crimson_stem|LUMBERJACK|0.10|2|1",
                            "minecraft:warped_stem|LUMBERJACK|0.10|2|1",
                            "minecraft:oak_planks|CRAFTER|0.10|1|1",
                            "minecraft:bamboo_block|LUMBERJACK|0.10|2|1",
                            "minecraft:stripped_bamboo_block|LUMBERJACK|0.10|2|1",
                            "minecraft:cod|FISHER|0.10|2|1",
                            "minecraft:salmon|FISHER|0.10|2|1",
                            "minecraft:tropical_fish|FISHER|0.10|3|2",
                            "minecraft:pufferfish|FISHER|0.10|3|2",
                            "minecraft:ink_sac|FISHER|0.10|2|1",
                            "minecraft:nautilus_shell|FISHER|0.10|6|3",
                            "minecraft:rotten_flesh|HUNTER|0.10|2|1",
                            "minecraft:bone|HUNTER|0.10|2|1",
                            "minecraft:string|HUNTER|0.10|2|1",
                            "minecraft:gunpowder|HUNTER|0.10|3|2",
                            "minecraft:spider_eye|HUNTER|0.10|3|2",
                            "minecraft:ender_pearl|HUNTER|0.10|6|3",
                            "minecraft:blaze_rod|HUNTER|0.10|6|3",
                            "minecraft:magma_cream|HUNTER|0.10|4|2",
                            "minecraft:ghast_tear|HUNTER|0.10|8|4",
                            "minecraft:slime_ball|HUNTER|0.10|3|2",
                            "minecraft:prismarine_shard|HUNTER|0.10|3|2",
                            "minecraft:prismarine_crystals|HUNTER|0.10|4|2",
                            "minecraft:leather|HUNTER|0.10|2|1",
                            "minecraft:feather|HUNTER|0.10|2|1",
                            "minecraft:rabbit_hide|HUNTER|0.10|2|1",
                            "minecraft:rabbit_foot|HUNTER|0.10|4|2",
                            "minecraft:porkchop|HUNTER|0.10|2|1",
                            "minecraft:beef|HUNTER|0.10|2|1",
                            "minecraft:chicken|HUNTER|0.10|2|1",
                            "minecraft:mutton|HUNTER|0.10|2|1",
                            "minecraft:rabbit|HUNTER|0.10|2|1",
                            "minecraft:bread|CRAFTER|0.10|2|1",
                            "minecraft:torch|CRAFTER|0.10|1|1",
                            "minecraft:crafting_table|CRAFTER|0.10|2|1",
                            "minecraft:chest|CRAFTER|0.10|2|1",
                            "minecraft:furnace|CRAFTER|0.10|3|2",
                            "minecraft:stone_pickaxe|CRAFTER|0.10|4|2",
                            "minecraft:iron_pickaxe|CRAFTER|0.10|8|4",
                            "minecraft:white_wool|SHEPHERD|0.10|2|1",
                            "minecraft:mutton|SHEPHERD|0.10|1|1",
                            "minecraft:egg|BREEDER|0.10|1|1",
                            "minecraft:feather|BREEDER|0.10|1|1",
                            "minecraft:leather|BREEDER|0.10|2|1",
                            "minecraft:honeycomb|BREEDER|0.10|2|1",
                            "minecraft:honey_bottle|BREEDER|0.10|2|1",
                            "minecraft:milk_bucket|BREEDER|0.10|3|2",
                            "minecraft:iron_ingot|SMITHY|0.10|3|2",
                            "minecraft:gold_ingot|SMITHY|0.10|4|2",
                            "minecraft:copper_ingot|SMITHY|0.10|2|1",
                            "minecraft:diamond|SMITHY|0.10|8|4",
                            "minecraft:netherite_ingot|SMITHY|0.10|16|8",
                            "minecraft:netherite_scrap|SMITHY|0.10|8|4",
                            "minecraft:iron_ingot|SMELTER|0.10|2|1",
                            "minecraft:gold_ingot|SMELTER|0.10|3|2",
                            "minecraft:copper_ingot|SMELTER|0.10|1|1",
                            "minecraft:netherite_scrap|SMELTER|0.10|8|4",
                            "minecraft:glass|SMELTER|0.10|1|1",
                            "minecraft:brick|SMELTER|0.10|1|1",
                            "minecraft:charcoal|SMELTER|0.10|1|1"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUB_TAG_REWARD_RULES = BUILDER
            .comment("Tag-backed hub reward rules. Format: selector_type|tag_id|profession|credits|profession_xp|general_xp")
            .comment("selector_type: BLOCK_TAG or ITEM_TAG")
            .defineListAllowEmpty(
                    "hub.tagRewardRules",
                    List.of(
                            "ITEM_TAG|realciv:farmer_contributions|FARMER|0.10|2|1",
                            "ITEM_TAG|realciv:miner_contributions|MINER|0.10|3|2",
                            "ITEM_TAG|realciv:terraformer_contributions|TERRAFORMER|0.10|2|1",
                            "ITEM_TAG|realciv:lumberjack_contributions|LUMBERJACK|0.10|2|1",
                            "ITEM_TAG|realciv:fisher_contributions|FISHER|0.10|2|1",
                            "ITEM_TAG|realciv:hunter_contributions|HUNTER|0.10|3|2",
                            "ITEM_TAG|realciv:crafter_contributions|CRAFTER|0.10|2|1",
                            "ITEM_TAG|realciv:enchanter_contributions|ENCHANTER|0.10|2|1",
                            "ITEM_TAG|realciv:brewer_contributions|BREWER|0.10|2|1",
                            "ITEM_TAG|realciv:trader_contributions|TRADER|0.10|2|1",
                            "ITEM_TAG|realciv:shepherd_contributions|SHEPHERD|0.10|2|1",
                            "ITEM_TAG|realciv:breeder_contributions|BREEDER|0.10|2|1",
                            "ITEM_TAG|realciv:smithy_contributions|SMITHY|0.10|3|2",
                            "ITEM_TAG|realciv:smelter_contributions|SMELTER|0.10|2|1",
                            "BLOCK_TAG|minecraft:mineable/pickaxe|MINER|0.10|1|1",
                            "BLOCK_TAG|minecraft:mineable/shovel|TERRAFORMER|0.10|1|1",
                            "BLOCK_TAG|minecraft:logs|LUMBERJACK|0.10|2|1",
                            "BLOCK_TAG|minecraft:bamboo_blocks|LUMBERJACK|0.10|2|1"),
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
                            "ITEM_TAG|realciv:fisher_reset_items|FISHER|1.0",
                            "ITEM_TAG|realciv:hunter_reset_items|HUNTER|1.0",
                            "ITEM_TAG|realciv:crafter_reset_items|CRAFTER|1.0",
                            "ITEM_TAG|realciv:enchanter_reset_items|ENCHANTER|1.0",
                            "ITEM_TAG|realciv:brewer_reset_items|BREWER|1.0",
                            "ITEM_TAG|realciv:trader_reset_items|TRADER|1.0",
                            "ITEM_TAG|realciv:shepherd_reset_items|SHEPHERD|1.0",
                            "ITEM_TAG|realciv:breeder_reset_items|BREEDER|1.0",
                            "ITEM_TAG|realciv:smithy_reset_items|SMITHY|1.0",
                            "ITEM_TAG|realciv:smelter_reset_items|SMELTER|1.0"),
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

    public static final ModConfigSpec.BooleanValue HUB_DEPOSIT_GENERAL_XP_ENABLED = BUILDER
            .comment("When true, Community Hub deposits apply each rule's general_xp value.")
            .comment("This grants both RealCiv general XP and vanilla Minecraft XP from deposits.")
            .define("hub.depositGeneralXpEnabled", false);

    public static final ModConfigSpec.DoubleValue DEFAULT_PERSONAL_WITHDRAW_PERCENT = BUILDER
            .comment("Default percent of a player's own contributed item count they can withdraw from the hub.")
            .defineInRange("hub.defaultPersonalWithdrawalPercent", 10.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue TOOLS_PROFESSION_LEVEL_GATES_ENABLED = BUILDER
            .comment("When true, profession-specific tool tier gates are enforced (pickaxe=miner, axe=lumberjack, shovel=terraformer, hoe=farmer, sword=warrior).")
            .define("tools.professionLevelGatesEnabled", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFESSION_TOOL_TIER_REQUIREMENTS = BUILDER
            .comment("Profession-specific tool tier level requirements.")
            .comment("Format: PROFESSION|WOOD=0|GOLD=2|STONE=2|IRON=8|DIAMOND=25|NETHERITE=40|UNKNOWN=40")
            .comment("Missing tier keys default to 0 (no profession-level gate for that tier).")
            .defineListAllowEmpty(
                    "profession.toolTierRequirements",
                    List.of(
                            "MINER|WOOD=0|GOLD=2|STONE=2|IRON=8|DIAMOND=25|NETHERITE=40|UNKNOWN=40",
                            "LUMBERJACK|WOOD=0|GOLD=2|STONE=2|IRON=8|DIAMOND=25|NETHERITE=40|UNKNOWN=40",
                            "TERRAFORMER|WOOD=0|GOLD=2|STONE=2|IRON=8|DIAMOND=25|NETHERITE=40|UNKNOWN=40",
                            "FARMER|WOOD=0|GOLD=2|STONE=2|IRON=8|DIAMOND=25|NETHERITE=40|UNKNOWN=40",
                            "WARRIOR|WOOD=0|GOLD=2|STONE=2|IRON=8|DIAMOND=25|NETHERITE=40|UNKNOWN=40"),
                    () -> "",
                    RealCivConfig::isString);

    public static final ModConfigSpec.BooleanValue TOOLS_GENERAL_LEVEL_GATES_ENABLED = BUILDER
            .comment("When true, general-level tier gates below are enforced.")
            .define("tools.generalLevelGatesEnabled", false);

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

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SMITHY_REPAIR_TIER_LEVELS = BUILDER
            .comment("Smithy level requirements for repairing each material tier on an anvil.")
            .comment("Format: TIER_NAME=required_level")
            .comment("Tiers: WOOD, STONE, GOLD, IRON, DIAMOND, NETHERITE, LEATHER, CHAINMAIL, TURTLE, DEFAULT")
            .defineListAllowEmpty(
                    "smithy.repairTierLevels",
                    List.of(
                            "WOOD=0",
                            "STONE=1",
                            "GOLD=2",
                            "IRON=2",
                            "DIAMOND=4",
                            "NETHERITE=6",
                            "LEATHER=0",
                            "CHAINMAIL=2",
                            "TURTLE=2",
                            "DEFAULT=1"),
                    () -> "",
                    RealCivConfig::isString);

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
                            "FISHER|1.0",
                            "HUNTER|1.0",
                            "WARRIOR|1.0",
                            "EXPLOSIVES_EXPERT|1.0",
                            "CRAFTER|1.0",
                            "ENCHANTER|1.0",
                            "BREWER|1.0",
                            "TRADER|1.0"),
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
            .comment("Default is 1 action per block broken. Set >1 to make a block cost more actions.")
            .comment("Example: minecraft:coal_ore|2 means breaking one coal ore uses 2 profession actions.")
            .defineListAllowEmpty(
                    "profession.breakActionCostOverrides",
                    List.of(),
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

    public static final ModConfigSpec.ConfigValue<String> LAND_TOWN_CLAIM_SCALING_MODE = BUILDER
            .comment("Town-claim pricing mode. Valid values: linear, exponential.")
            .comment("linear: base + (added_per_owned * owned_chunks)")
            .comment("exponential: base * (growth_factor ^ owned_chunks)")
            .define("land.townClaimScalingMode", "linear");

    public static final ModConfigSpec.DoubleValue LAND_TOWN_CLAIM_GROWTH_FACTOR = BUILDER
            .comment("Growth factor used when land.townClaimScalingMode=exponential.")
            .comment("Example: 2.0 doubles the claim cost for each additional owned CIVIC chunk.")
            .defineInRange("land.townClaimGrowthFactor", 2.0D, 1.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue LAND_TOWN_CLAIM_MAX_COST = BUILDER
            .comment("Optional hard cap for town claim cost.")
            .comment("Set to 0 to disable the cap.")
            .defineInRange("land.townClaimMaxCost", 0.0D, 0.0D, 1_000_000_000.0D);

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

    public static final ModConfigSpec.DoubleValue MAX_CONTRIBUTION_KARMA_GAIN_PER_DAY = BUILDER
            .comment("Max contribution karma a player can gain per real-world day per civilization.")
            .comment("Set to 0 to disable this cap.")
            .defineInRange("economy.maxContributionKarmaGainPerDay", 0.0D, 0.0D, 100_000_000.0D);

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

    public static final ModConfigSpec.BooleanValue LAND_ALLOW_NEUTRAL_CIV_BUILD_BREAK = BUILDER
            .comment("When true, NEUTRAL cross-civilization players can place/break blocks in claimed land.")
            .define("land.allowNeutralCivBuildBreak", false);

    public static final ModConfigSpec.BooleanValue LAND_ALLOW_ALLY_CIV_BUILD_BREAK = BUILDER
            .comment("When true, ALLY cross-civilization players can place/break blocks in claimed land.")
            .define("land.allowAllyCivBuildBreak", true);

    public static final ModConfigSpec.BooleanValue LAND_ALLOW_WAR_CIV_BUILD_BREAK = BUILDER
            .comment("When true, WAR cross-civilization players can place/break blocks in claimed land.")
            .comment("Keep false until war-mode rules (destruction vs PvP) are configured as desired.")
            .define("land.allowWarCivBuildBreak", false);

    public static final ModConfigSpec.ConfigValue<String> LAND_CLAIM_DIMENSION_POLICY = BUILDER
            .comment("Land-claim policy for dimensions.")
            .comment("Valid: allow_all, allowlist, denylist")
            .define("land.claimDimensionPolicy", "denylist");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LAND_CLAIM_DIMENSIONS = BUILDER
            .comment("Dimension ids used by land.claimDimensionPolicy.")
            .comment("With denylist, claims are blocked in listed dimensions.")
            .comment("With allowlist, claims are only allowed in listed dimensions.")
            .defineListAllowEmpty(
                    "land.claimDimensions",
                    List.of("minecraft:the_end"),
                    () -> "",
                    RealCivConfig::isString);

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

    public static final ModConfigSpec.IntValue MAX_REDSTONERS_PER_CIV = BUILDER
            .comment("Maximum number of designated redstoners per civilization. Set to 0 to disable regulated redstone placement.")
            .defineInRange("civ.maxRedstonersPerCivilization", 2, 0, 256);

    public static final ModConfigSpec.BooleanValue REQUIRE_FOUNDER_APPROVAL = BUILDER
            .comment("When true, only approved players (or admins) can found new civilizations.")
            .define("civ.requireFounderApproval", true);

    public static final ModConfigSpec.IntValue WAR_DEFAULT_PVP_KILL_TARGET = BUILDER
            .comment("Default kill target used for PvP war declarations when no explicit target is provided.")
            .defineInRange("civ.war.defaultPvpKillTarget", 10, 1, 100_000);

    public static final ModConfigSpec.BooleanValue GOVERNANCE_APPROVAL_WORKFLOW_ENABLED = BUILDER
            .comment("When true, council/democratic civ policy changes can require in-game proposal votes.")
            .comment("Leadership/admin can still apply urgent changes immediately.")
            .define("civ.governanceApprovalWorkflowEnabled", true);
    public static final ModConfigSpec.IntValue GOVERNANCE_ELECTION_DURATION_MINUTES = BUILDER
            .comment("Minutes an active leadership election remains open before automatic resolution.")
            .defineInRange("civ.governanceElectionDurationMinutes", 15, 1, 24 * 60);
    public static final ModConfigSpec.IntValue GOVERNANCE_COUP_DURATION_MINUTES = BUILDER
            .comment("Minutes an active coup vote remains open before automatic resolution.")
            .defineInRange("civ.governanceCoupDurationMinutes", 15, 1, 24 * 60);
    public static final ModConfigSpec.IntValue GOVERNANCE_COUP_MIN_MEMBERS = BUILDER
            .comment("Minimum civilization member count required to start a coup vote.")
            .defineInRange("civ.governanceCoupMinMembers", 12, 2, 10_000);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REDSTONER_RESTRICTED_BLOCKS = BUILDER
            .comment("Blocks treated as regulated redstone components. Players must be designated as Redstoners to place these blocks.")
            .defineListAllowEmpty(
                    "redstone.restrictedBlocks",
                    List.of(
                            "minecraft:redstone_wire",
                            "minecraft:redstone_torch",
                            "minecraft:repeater",
                            "minecraft:comparator",
                            "minecraft:observer",
                            "minecraft:piston",
                            "minecraft:sticky_piston",
                            "minecraft:dispenser",
                            "minecraft:dropper",
                            "minecraft:hopper"),
                    () -> "",
                    RealCivConfig::isString);

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
        return ProfessionLimitConfig.farmerLimitForLevel(farmerLevel);
    }

    public static int minerLimitForLevel(int minerLevel) {
        return ProfessionLimitConfig.minerLimitForLevel(minerLevel);
    }

    public static int terraformerLimitForLevel(int terraformerLevel) {
        return ProfessionLimitConfig.terraformerLimitForLevel(terraformerLevel);
    }

    public static int lumberjackLimitForLevel(int lumberjackLevel) {
        return ProfessionLimitConfig.lumberjackLimitForLevel(lumberjackLevel);
    }

    public static int fisherLimitForLevel(int fisherLevel) {
        return ProfessionLimitConfig.fisherLimitForLevel(fisherLevel);
    }

    public static int hunterLimitForLevel(int hunterLevel) {
        return ProfessionLimitConfig.hunterLimitForLevel(hunterLevel);
    }

    public static int hunterMobActionCapForLevel(ResourceLocation entityId, int hunterLevel) {
        return ProfessionLimitConfig.hunterMobActionCapForLevel(entityId, hunterLevel);
    }

    public static Map<ResourceLocation, Integer> hunterMobActionCapsForLevel(int hunterLevel) {
        return ProfessionLimitConfig.hunterMobActionCapsForLevel(hunterLevel);
    }

    public static int hunterRequiredLevelForMob(ResourceLocation entityId) {
        return ProfessionLimitConfig.hunterRequiredLevelForMob(entityId);
    }

    public static int dailyActionCapForLevel(Profession profession, int level) {
        return ProfessionLimitConfig.dailyActionCapForLevel(profession, level);
    }

    public static int minerBlockActionCapForLevel(ResourceLocation blockId, int minerLevel) {
        return ProfessionLimitConfig.minerBlockActionCapForLevel(blockId, minerLevel);
    }

    public static int minerDailyBlockActionCapForLevel(ResourceLocation blockId, int minerLevel) {
        return ProfessionLimitConfig.minerDailyBlockActionCapForLevel(blockId, minerLevel);
    }

    public static int lumberjackBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return ProfessionLimitConfig.lumberjackBlockActionCapForLevel(blockId, level);
    }

    public static int lumberjackDailyBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return ProfessionLimitConfig.lumberjackDailyBlockActionCapForLevel(blockId, level);
    }

    public static int terraformerBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return ProfessionLimitConfig.terraformerBlockActionCapForLevel(blockId, level);
    }

    public static int terraformerDailyBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return ProfessionLimitConfig.terraformerDailyBlockActionCapForLevel(blockId, level);
    }

    public static int crafterItemActionCapForLevel(ResourceLocation itemId, int crafterLevel) {
        return ProfessionLimitConfig.crafterItemActionCapForLevel(itemId, crafterLevel);
    }

    public static int crafterDailyItemActionCapForLevel(ResourceLocation itemId, int crafterLevel) {
        return ProfessionLimitConfig.crafterDailyItemActionCapForLevel(itemId, crafterLevel);
    }

    public static int warriorLimitForLevel(int warriorLevel) {
        return ProfessionLimitConfig.warriorLimitForLevel(warriorLevel);
    }

    public static int explosivesExpertLimitForLevel(int explosivesLevel) {
        return ProfessionLimitConfig.explosivesExpertLimitForLevel(explosivesLevel);
    }

    public static int crafterLimitForLevel(int crafterLevel) {
        return ProfessionLimitConfig.crafterLimitForLevel(crafterLevel);
    }

    public static int enchanterLimitForLevel(int enchanterLevel) {
        return ProfessionLimitConfig.enchanterLimitForLevel(enchanterLevel);
    }

    public static int brewerLimitForLevel(int brewerLevel) {
        return ProfessionLimitConfig.brewerLimitForLevel(brewerLevel);
    }

    public static int traderLimitForLevel(int traderLevel) {
        return ProfessionLimitConfig.traderLimitForLevel(traderLevel);
    }

    public static int shepherdLimitForLevel(int level) {
        return ProfessionLimitConfig.shepherdLimitForLevel(level);
    }

    public static int explorerLimitForLevel(int level) {
        return ProfessionLimitConfig.explorerLimitForLevel(level);
    }

    public static int treasureHunterLimitForLevel(int level) {
        return ProfessionLimitConfig.treasureHunterLimitForLevel(level);
    }

    public static int breederLimitForLevel(int level) {
        return ProfessionLimitConfig.breederLimitForLevel(level);
    }

    public static int smithyLimitForLevel(int level) {
        return ProfessionLimitConfig.smithyLimitForLevel(level);
    }

    public static int smithyRepairTierRequirement(String tierKey) {
        return ToolConfig.smithyRepairTierRequirement(tierKey);
    }

    public static int smelterLimitForLevel(int level) {
        return ProfessionLimitConfig.smelterLimitForLevel(level);
    }

    public static int limitForProfession(Profession profession, int level) {
        return ProfessionLimitConfig.limitForProfession(profession, level);
    }

    public static boolean professionToolLevelGatesEnabled() {
        return ToolConfig.professionToolLevelGatesEnabled();
    }

    public static boolean generalToolLevelGatesEnabled() {
        return ToolConfig.generalToolLevelGatesEnabled();
    }

    public static int requiredProfessionLevelForToolTier(Profession profession, RealCivUtil.ToolTier tier) {
        return ToolConfig.requiredProfessionLevelForToolTier(profession, tier);
    }

    public static boolean specializationSingleProfessionLockEnabled() {
        return ProgressionConfig.specializationSingleProfessionLockEnabled();
    }

    public static boolean specializationXpDecayEnabled() {
        return ProgressionConfig.specializationXpDecayEnabled();
    }

    public static double specializationXpDecayRate() {
        return ProgressionConfig.specializationXpDecayRate();
    }

    public static int warriorXpPerPlayerKill() {
        return ProgressionConfig.warriorXpPerPlayerKill();
    }

    public static int warriorGeneralXpPerPlayerKill() {
        return ProgressionConfig.warriorGeneralXpPerPlayerKill();
    }

    public static boolean warriorRequireHubRegistration() {
        return ProgressionConfig.warriorRequireHubRegistration();
    }

    public static boolean warriorHomeDefenseNoActionCost() {
        return ProgressionConfig.warriorHomeDefenseNoActionCost();
    }

    public static boolean governanceApprovalWorkflowEnabled() {
        return CivConfig.governanceApprovalWorkflowEnabled();
    }

    public static long governanceElectionDurationMillis() {
        return CivConfig.governanceElectionDurationMillis();
    }

    public static long governanceCoupDurationMillis() {
        return CivConfig.governanceCoupDurationMillis();
    }

    public static int governanceCoupMinMembers() {
        return CivConfig.governanceCoupMinMembers();
    }

    public static double civTreasuryDepositRatio() {
        return HubConfig.civTreasuryDepositRatio();
    }

    public static long upkeepIntervalTicks() {
        return LandConfig.upkeepIntervalTicks();
    }

    public static long upkeepGraceTicks() {
        return LandConfig.upkeepGraceTicks();
    }

    public static boolean blockUnclaimedBuilding() {
        return LandConfig.blockUnclaimedBuilding();
    }

    public static boolean allowNeutralCivBuildBreak() {
        return LandConfig.allowNeutralCivBuildBreak();
    }

    public static boolean allowAllyCivBuildBreak() {
        return LandConfig.allowAllyCivBuildBreak();
    }

    public static boolean allowWarCivBuildBreak() {
        return LandConfig.allowWarCivBuildBreak();
    }

    public static boolean canClaimDimension(@Nullable String dimensionIdRaw) {
        return LandConfig.canClaimDimension(dimensionIdRaw);
    }

    public static String claimDimensionPolicyLabel() {
        return LandConfig.claimDimensionPolicyLabel();
    }

    public static Set<String> claimDimensionSet() {
        return LandConfig.claimDimensionSet();
    }

    public static int landWandVisualizeRadiusChunks() {
        return LandConfig.landWandVisualizeRadiusChunks();
    }

    public static int landWandMaxSelectionChunks() {
        return LandConfig.landWandMaxSelectionChunks();
    }

    public static String ftbMayorDefaultClaimMode() {
        return LandConfig.ftbMayorDefaultClaimMode();
    }

    public static String defaultCivilizationId() {
        return CivConfig.defaultCivilizationId();
    }

    public static String defaultCivilizationName() {
        return CivConfig.defaultCivilizationName();
    }

    public static boolean adminBypassRestrictions() {
        return CivConfig.adminBypassRestrictions();
    }

    public static boolean requireFounderApproval() {
        return CivConfig.requireFounderApproval();
    }

    public static int defaultWarPvpKillTarget() {
        return CivConfig.defaultWarPvpKillTarget();
    }

    public static int maxExplosivesExpertsPerCivilization() {
        return CivConfig.maxExplosivesExpertsPerCivilization();
    }

    public static int maxRedstonersPerCivilization() {
        return CivConfig.maxRedstonersPerCivilization();
    }

    public static boolean blockNonPlayerExplosionDamageInClaims() {
        return CivConfig.blockNonPlayerExplosionDamageInClaims();
    }

    public static Set<ResourceLocation> regulatedRedstoneBlocks() {
        return CivConfig.regulatedRedstoneBlocks();
    }

    public static Set<ResourceLocation> regulatedExplosiveItems() {
        return CivConfig.regulatedExplosiveItems();
    }

    public static boolean carryCapPickupEnabled() {
        return CivConfig.carryCapPickupEnabled();
    }

    public static boolean carryCapCraftEnabled() {
        return CivConfig.carryCapCraftEnabled();
    }

    public static Map<Profession, Double> carryCapProfessionMultipliers() {
        return CivConfig.carryCapProfessionMultipliers();
    }

    public static Map<ResourceLocation, Integer> carryCapItemMaxOverrides() {
        return CivConfig.carryCapItemMaxOverrides();
    }

    public static Map<ResourceLocation, Integer> breakActionCostOverrides() {
        return CivConfig.breakActionCostOverrides();
    }

    public static Map<Profession, Map<RealCivUtil.ToolTier, Integer>> professionToolTierRequirements() {
        return ToolConfig.professionToolTierRequirements();
    }

    public static List<ProfessionEventHookRule> professionEventHookRules() {
        return CivConfig.professionEventHookRules();
    }

    public static boolean migrateLegacyCommonConfigIfNeeded() {
        return CivConfig.migrateLegacyCommonConfigIfNeeded();
    }

    public static double defaultPersonalWithdrawRatio() {
        return HubConfig.defaultPersonalWithdrawRatio();
    }

    public static boolean hubDepositGeneralXpEnabled() {
        return HubConfig.hubDepositGeneralXpEnabled();
    }

    public static double deathActionRefundRatio() {
        return ProgressionConfig.deathActionRefundRatio();
    }

    public static boolean staleActionResetEnabled() {
        return ProgressionConfig.staleActionResetEnabled();
    }

    public static long staleActionResetMillis() {
        return ProgressionConfig.staleActionResetMillis();
    }

    public static boolean useProfessionRuleFiles() {
        return HubConfig.useProfessionRuleFiles();
    }

    public static String hubProfessionRuleDirectory() {
        return HubConfig.hubProfessionRuleDirectory();
    }

    public static void invalidateExternalRuleFileCache() {
        HubConfig.invalidateExternalRuleFileCache();
    }

    public static double upkeepCostCents() {
        return LandConfig.upkeepCostCents();
    }

    public static int professionLevelFromXp(@Nullable Profession profession, int xp) {
        return ProgressionConfig.professionLevelFromXp(profession, xp);
    }

    public static int professionLevelCap(@Nullable Profession profession) {
        return ProgressionConfig.professionLevelCap(profession);
    }

    public static int generalLevelFromXp(int xp) {
        return ProgressionConfig.generalLevelFromXp(xp);
    }

    public static int maxProfessionLevelGainsPerDay() {
        return ProgressionConfig.maxProfessionLevelGainsPerDay();
    }

    public static int maxGeneralLevelGainsPerDay() {
        return ProgressionConfig.maxGeneralLevelGainsPerDay();
    }

    public static long maxContributionKarmaGainPerDayCents() {
        return HubConfig.maxContributionKarmaGainPerDayCents();
    }

    public static long townClaimCostCents() {
        return LandConfig.townClaimCostCents();
    }

    public static long townClaimCostAddedPerOwnedCents() {
        return LandConfig.townClaimCostAddedPerOwnedCents();
    }

    public static long nextTownClaimCostCents(int civicChunksOwned) {
        return LandConfig.nextTownClaimCostCents(civicChunksOwned);
    }

    public static long rentCostCents() {
        return LandConfig.rentCostCents();
    }

    public static long rentCostAddedPerOwnedPrivateCents() {
        return LandConfig.rentCostAddedPerOwnedPrivateCents();
    }

    public static double hubWithdrawCreditPenaltyRatio() {
        return HubConfig.hubWithdrawCreditPenaltyRatio();
    }

    public static int hubStarterAreaBlocks() {
        return LandConfig.hubStarterAreaBlocks();
    }

    public static Map<ResourceLocation, RewardRule> rewardRules() {
        return HubConfig.rewardRules();
    }

    public static List<TagRewardRule> tagRewardRules() {
        return HubConfig.tagRewardRules();
    }

    public static Map<ResourceLocation, ItemResetRule> itemResetRules() {
        return HubConfig.itemResetRules();
    }

    public static List<TagResetRule> tagResetRules() {
        return HubConfig.tagResetRules();
    }



    @Nullable
    static Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    static Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
