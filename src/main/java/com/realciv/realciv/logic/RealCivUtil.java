package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class RealCivUtil {
    private RealCivUtil() {
    }

    public static long creditsToCents(double credits) {
        return Math.round(credits * 100.0D);
    }

    public static String formatCredits(long cents) {
        return String.format(Locale.ROOT, "%.2f", cents / 100.0D);
    }

    public static String formatPercentFromRatio(double ratio) {
        return String.format(Locale.ROOT, "%.2f%%", Math.max(0.0D, ratio) * 100.0D);
    }

    public static int levelFromThresholds(int xp, List<? extends Integer> thresholds) {
        int level = 0;
        for (int i = 0; i < thresholds.size(); i++) {
            Integer threshold = thresholds.get(i);
            int value = threshold == null ? 0 : Math.max(0, threshold);
            if (xp >= value) {
                level = i;
            } else {
                break;
            }
        }
        return level;
    }

    public static int valueForLevel(int level, List<? extends Integer> values, int fallback) {
        if (values.isEmpty()) {
            return fallback;
        }
        int idx = Math.max(0, Math.min(level, values.size() - 1));
        Integer found = values.get(idx);
        return found == null ? fallback : Math.max(0, found);
    }

    public static boolean isBypass(ServerPlayer player) {
        return RealCivConfig.adminBypassRestrictions() && player.hasPermissions(4);
    }

    public static ToolTier toolTier(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredItem tieredItem)) {
            return ToolTier.NONE;
        }
        Tier tier = tieredItem.getTier();
        if (tier == Tiers.WOOD) {
            return ToolTier.WOOD;
        }
        if (tier == Tiers.GOLD) {
            return ToolTier.GOLD;
        }
        if (tier == Tiers.STONE) {
            return ToolTier.STONE;
        }
        if (tier == Tiers.IRON) {
            return ToolTier.IRON;
        }
        if (tier == Tiers.DIAMOND) {
            return ToolTier.DIAMOND;
        }
        if (tier == Tiers.NETHERITE) {
            return ToolTier.NETHERITE;
        }
        return ToolTier.UNKNOWN;
    }

    @Nullable
    public static Profession professionForTieredTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof PickaxeItem) {
            return Profession.MINER;
        }
        if (stack.getItem() instanceof AxeItem) {
            return Profession.LUMBERJACK;
        }
        if (stack.getItem() instanceof ShovelItem) {
            return Profession.TERRAFORMER;
        }
        if (stack.getItem() instanceof HoeItem) {
            return Profession.FARMER;
        }
        if (stack.getItem() instanceof SwordItem) {
            return Profession.WARRIOR;
        }
        return null;
    }

    public static String toolTierLabel(ItemStack stack) {
        return switch (toolTier(stack)) {
            case WOOD -> "wooden";
            case GOLD -> "golden";
            case STONE -> "stone";
            case IRON -> "iron";
            case DIAMOND -> "diamond";
            case NETHERITE -> "netherite";
            case UNKNOWN -> "advanced";
            case NONE -> "";
        };
    }

    public static int requiredGeneralLevelForTool(ItemStack stack) {
        return switch (toolTier(stack)) {
            case WOOD, GOLD -> RealCivConfig.WOOD_TOOL_LEVEL.get();
            case STONE -> RealCivConfig.STONE_TOOL_LEVEL.get();
            case IRON -> RealCivConfig.IRON_TOOL_LEVEL.get();
            case DIAMOND -> RealCivConfig.DIAMOND_TOOL_LEVEL.get();
            case NETHERITE -> RealCivConfig.NETHERITE_TOOL_LEVEL.get();
            case UNKNOWN -> RealCivConfig.UNKNOWN_TIER_TOOL_LEVEL.get();
            case NONE -> 0;
        };
    }

    public static String requiredToolLevelName(ItemStack stack) {
        return switch (toolTier(stack)) {
            case WOOD, GOLD -> "wooden/golden";
            case STONE -> "stone";
            case IRON -> "iron";
            case DIAMOND -> "diamond";
            case NETHERITE -> "netherite";
            case UNKNOWN -> "advanced";
            case NONE -> "";
        };
    }

    public enum ToolTier {
        NONE,
        WOOD,
        GOLD,
        STONE,
        IRON,
        DIAMOND,
        NETHERITE,
        UNKNOWN
    }
}
