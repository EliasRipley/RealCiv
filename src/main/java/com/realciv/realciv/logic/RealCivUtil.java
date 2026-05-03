package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.server.level.ServerPlayer;

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

    public static int requiredGeneralLevelForTool(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredItem tieredItem)) {
            return 0;
        }

        Tier tier = tieredItem.getTier();
        if (tier == Tiers.WOOD || tier == Tiers.GOLD) {
            return RealCivConfig.WOOD_TOOL_LEVEL.get();
        }
        if (tier == Tiers.STONE) {
            return RealCivConfig.STONE_TOOL_LEVEL.get();
        }
        if (tier == Tiers.IRON) {
            return RealCivConfig.IRON_TOOL_LEVEL.get();
        }
        if (tier == Tiers.DIAMOND) {
            return RealCivConfig.DIAMOND_TOOL_LEVEL.get();
        }
        if (tier == Tiers.NETHERITE) {
            return RealCivConfig.NETHERITE_TOOL_LEVEL.get();
        }

        return RealCivConfig.UNKNOWN_TIER_TOOL_LEVEL.get();
    }

    public static String requiredToolLevelName(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredItem tieredItem)) {
            return "";
        }

        Tier tier = tieredItem.getTier();
        if (tier == Tiers.WOOD || tier == Tiers.GOLD) {
            return "wooden/golden";
        }
        if (tier == Tiers.STONE) {
            return "stone";
        }
        if (tier == Tiers.IRON) {
            return "iron";
        }
        if (tier == Tiers.DIAMOND) {
            return "diamond";
        }
        if (tier == Tiers.NETHERITE) {
            return "netherite";
        }
        return "advanced";
    }
}
