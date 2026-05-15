package com.realciv.realciv.config;

import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class ProfessionLimitConfig {

    private ProfessionLimitConfig() {
    }

    public static int farmerLimitForLevel(int farmerLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(farmerLevel, RealCivConfig.FARMER_LIMIT_BASE.get(), RealCivConfig.FARMER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(farmerLevel, RealCivConfig.FARMER_LIMITS.get(), 8);
    }

    public static int minerLimitForLevel(int minerLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(minerLevel, RealCivConfig.MINER_LIMIT_BASE.get(), RealCivConfig.MINER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(minerLevel, RealCivConfig.MINER_LIMITS.get(), 40);
    }

    public static int terraformerLimitForLevel(int terraformerLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(terraformerLevel, RealCivConfig.TERRAFORMER_LIMIT_BASE.get(), RealCivConfig.TERRAFORMER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(terraformerLevel, RealCivConfig.TERRAFORMER_LIMITS.get(), 40);
    }

    public static int lumberjackLimitForLevel(int lumberjackLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(lumberjackLevel, RealCivConfig.LUMBERJACK_LIMIT_BASE.get(), RealCivConfig.LUMBERJACK_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(lumberjackLevel, RealCivConfig.LUMBERJACK_LIMITS.get(), 8);
    }

    public static int fisherLimitForLevel(int fisherLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(fisherLevel, RealCivConfig.FISHER_LIMIT_BASE.get(), RealCivConfig.FISHER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(fisherLevel, RealCivConfig.FISHER_LIMITS.get(), 8);
    }

    public static int hunterLimitForLevel(int hunterLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(hunterLevel, RealCivConfig.HUNTER_LIMIT_BASE.get(), RealCivConfig.HUNTER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(hunterLevel, RealCivConfig.HUNTER_LIMITS.get(), 1);
    }

    public static int hunterMobActionCapForLevel(ResourceLocation entityId, int hunterLevel) {
        if (entityId == null) {
            return 0;
        }
        Integer direct = hunterMobActionCapsForLevel(hunterLevel).get(entityId);
        if (direct == null) {
            return 0;
        }
        return Math.max(0, direct);
    }

    public static Map<ResourceLocation, Integer> hunterMobActionCapsForLevel(int hunterLevel) {
        Map<ResourceLocation, Integer> caps = new HashMap<>();
        for (String raw : RealCivConfig.HUNTER_MOB_ACTION_CAPS.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }
            ResourceLocation entityId;
            try {
                entityId = ResourceLocation.parse(parts[0].trim());
            } catch (Exception ex) {
                continue;
            }
            int cap = parseLevelIndexedValue(parts[1], hunterLevel);
            caps.put(entityId, Math.max(0, cap));
        }
        return caps;
    }

    public static int hunterRequiredLevelForMob(ResourceLocation entityId) {
        if (entityId == null) {
            return 0;
        }
        for (String raw : RealCivConfig.HUNTER_MOB_MIN_LEVELS.get()) {
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
            ResourceLocation parsedId;
            try {
                parsedId = ResourceLocation.parse(parts[0].trim());
            } catch (Exception ex) {
                continue;
            }
            if (!parsedId.equals(entityId)) {
                continue;
            }
            Integer required = tryParseInt(parts[1].trim());
            return required == null ? 0 : Math.max(0, required);
        }
        return 0;
    }

    public static int dailyActionCapForLevel(Profession profession, int level) {
        if (profession == null || profession == Profession.NONE) {
            return 0;
        }
        for (String raw : RealCivConfig.PROFESSION_DAILY_ACTION_CAPS.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }
            Profession parsedProfession = Profession.fromConfigName(parts[0].trim());
            if (parsedProfession == null || parsedProfession != profession) {
                continue;
            }
            return Math.max(0, parseLevelIndexedValue(parts[1], level));
        }
        return 0;
    }

    public static int minerBlockActionCapForLevel(ResourceLocation blockId, int minerLevel) {
        return parseBlockLevelCap(RealCivConfig.MINER_BLOCK_ACTION_CAPS.get(), blockId, minerLevel);
    }

    public static int minerDailyBlockActionCapForLevel(ResourceLocation blockId, int minerLevel) {
        return parseBlockLevelCap(RealCivConfig.MINER_DAILY_BLOCK_ACTION_CAPS.get(), blockId, minerLevel);
    }

    public static int lumberjackBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return parseBlockLevelCap(RealCivConfig.LUMBERJACK_BLOCK_ACTION_CAPS.get(), blockId, level);
    }

    public static int lumberjackDailyBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return parseBlockLevelCap(RealCivConfig.LUMBERJACK_DAILY_BLOCK_ACTION_CAPS.get(), blockId, level);
    }

    public static int terraformerBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return parseBlockLevelCap(RealCivConfig.TERRAFORMER_BLOCK_ACTION_CAPS.get(), blockId, level);
    }

    public static int terraformerDailyBlockActionCapForLevel(ResourceLocation blockId, int level) {
        return parseBlockLevelCap(RealCivConfig.TERRAFORMER_DAILY_BLOCK_ACTION_CAPS.get(), blockId, level);
    }

    public static int crafterItemActionCapForLevel(ResourceLocation itemId, int crafterLevel) {
        return parseBlockLevelCap(RealCivConfig.CRAFTER_ITEM_ACTION_CAPS.get(), itemId, crafterLevel);
    }

    public static int crafterDailyItemActionCapForLevel(ResourceLocation itemId, int crafterLevel) {
        return parseBlockLevelCap(RealCivConfig.CRAFTER_DAILY_ITEM_ACTION_CAPS.get(), itemId, crafterLevel);
    }

    public static int warriorLimitForLevel(int warriorLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(warriorLevel, RealCivConfig.WARRIOR_LIMIT_BASE.get(), RealCivConfig.WARRIOR_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(warriorLevel, RealCivConfig.WARRIOR_LIMITS.get(), 1);
    }

    public static int explosivesExpertLimitForLevel(int explosivesLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(explosivesLevel, RealCivConfig.EXPLOSIVES_EXPERT_LIMIT_BASE.get(), RealCivConfig.EXPLOSIVES_EXPERT_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(explosivesLevel, RealCivConfig.EXPLOSIVES_EXPERT_LIMITS.get(), 1);
    }

    public static int crafterLimitForLevel(int crafterLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(crafterLevel, RealCivConfig.CRAFTER_LIMIT_BASE.get(), RealCivConfig.CRAFTER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(crafterLevel, RealCivConfig.CRAFTER_LIMITS.get(), 64);
    }

    public static int enchanterLimitForLevel(int enchanterLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(enchanterLevel, RealCivConfig.ENCHANTER_LIMIT_BASE.get(), RealCivConfig.ENCHANTER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(enchanterLevel, RealCivConfig.ENCHANTER_LIMITS.get(), 1);
    }

    public static int brewerLimitForLevel(int brewerLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(brewerLevel, RealCivConfig.BREWER_LIMIT_BASE.get(), RealCivConfig.BREWER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(brewerLevel, RealCivConfig.BREWER_LIMITS.get(), 1);
    }

    public static int traderLimitForLevel(int traderLevel) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(traderLevel, RealCivConfig.TRADER_LIMIT_BASE.get(), RealCivConfig.TRADER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(traderLevel, RealCivConfig.TRADER_LIMITS.get(), 1);
    }

    public static int shepherdLimitForLevel(int level) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(level, RealCivConfig.SHEPHERD_LIMIT_BASE.get(), RealCivConfig.SHEPHERD_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(level, RealCivConfig.SHEPHERD_LIMITS.get(), 2);
    }

    public static int explorerLimitForLevel(int level) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(level, RealCivConfig.EXPLORER_LIMIT_BASE.get(), RealCivConfig.EXPLORER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(level, RealCivConfig.EXPLORER_LIMITS.get(), 1);
    }

    public static int treasureHunterLimitForLevel(int level) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(level, RealCivConfig.TREASURE_HUNTER_LIMIT_BASE.get(), RealCivConfig.TREASURE_HUNTER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(level, RealCivConfig.TREASURE_HUNTER_LIMITS.get(), 1);
    }

    public static int breederLimitForLevel(int level) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(level, RealCivConfig.BREEDER_LIMIT_BASE.get(), RealCivConfig.BREEDER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(level, RealCivConfig.BREEDER_LIMITS.get(), 2);
    }

    public static int smithyLimitForLevel(int level) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(level, RealCivConfig.SMITHY_LIMIT_BASE.get(), RealCivConfig.SMITHY_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(level, RealCivConfig.SMITHY_LIMITS.get(), 1);
    }

    public static int smelterLimitForLevel(int level) {
        if (RealCivConfig.PROFESSION_USE_LINEAR_LIMIT_FORMULAS.get()) {
            return linearLimitForLevel(level, RealCivConfig.SMELTER_LIMIT_BASE.get(), RealCivConfig.SMELTER_LIMIT_PER_LEVEL.get());
        }
        return RealCivUtil.valueForLevel(level, RealCivConfig.SMELTER_LIMITS.get(), 1);
    }

    public static int limitForProfession(Profession profession, int level) {
        if (profession == null) {
            return 0;
        }
        return switch (profession) {
            case FARMER -> farmerLimitForLevel(level);
            case MINER -> minerLimitForLevel(level);
            case TERRAFORMER -> terraformerLimitForLevel(level);
            case LUMBERJACK -> lumberjackLimitForLevel(level);
            case FISHER -> fisherLimitForLevel(level);
            case HUNTER -> hunterLimitForLevel(level);
            case WARRIOR -> warriorLimitForLevel(level);
            case EXPLOSIVES_EXPERT -> explosivesExpertLimitForLevel(level);
            case CRAFTER -> crafterLimitForLevel(level);
            case ENCHANTER -> enchanterLimitForLevel(level);
            case BREWER -> brewerLimitForLevel(level);
            case TRADER -> traderLimitForLevel(level);
            case SHEPHERD -> shepherdLimitForLevel(level);
            case EXPLORER -> explorerLimitForLevel(level);
            case TREASURE_HUNTER -> treasureHunterLimitForLevel(level);
            case BREEDER -> breederLimitForLevel(level);
            case SMITHY -> smithyLimitForLevel(level);
            case SMELTER -> smelterLimitForLevel(level);
            case NONE -> 0;
        };
    }

    private static int parseLevelIndexedValue(String rawLevels, int level) {
        if (rawLevels == null || rawLevels.isBlank()) {
            return 0;
        }
        String[] tokens = rawLevels.split(",");
        int fallback = 0;
        for (int index = 0; index < tokens.length; index++) {
            Integer parsed = tryParseInt(tokens[index].trim());
            if (parsed == null) {
                continue;
            }
            fallback = Math.max(0, parsed);
            if (index >= Math.max(0, level)) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int linearLimitForLevel(int level, int base, int perLevel) {
        long safeLevel = Math.max(0L, level);
        long safeBase = Math.max(0L, base);
        long safePerLevel = Math.max(0L, perLevel);
        long value = safeBase + (safePerLevel * safeLevel);
        if (value <= 0L) {
            return 0;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    private static int parseBlockLevelCap(List<? extends String> lines, @Nullable ResourceLocation blockId, int level) {
        if (blockId == null) {
            return 0;
        }
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }
            ResourceLocation parsedBlockId;
            try {
                parsedBlockId = ResourceLocation.parse(parts[0].trim());
            } catch (Exception ex) {
                continue;
            }
            if (!parsedBlockId.equals(blockId)) {
                continue;
            }
            return Math.max(0, parseLevelIndexedValue(parts[1], level));
        }
        return 0;
    }

    @Nullable
    private static Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
