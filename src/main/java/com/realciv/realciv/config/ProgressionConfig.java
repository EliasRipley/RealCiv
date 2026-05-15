package com.realciv.realciv.config;

import com.realciv.realciv.logic.Profession;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class ProgressionConfig {
    private ProgressionConfig() {}

    public static boolean specializationSingleProfessionLockEnabled() {
        return RealCivConfig.SPECIALIZATION_SINGLE_PROFESSION_LOCK_ENABLED.get();
    }

    public static boolean specializationXpDecayEnabled() {
        return RealCivConfig.SPECIALIZATION_XP_DECAY_ENABLED.get();
    }

    public static double specializationXpDecayRate() {
        return Math.max(0.0D, RealCivConfig.SPECIALIZATION_XP_DECAY_RATE.get());
    }

    public static int warriorXpPerPlayerKill() {
        return Math.max(0, RealCivConfig.WARRIOR_XP_PER_PLAYER_KILL.get());
    }

    public static int warriorGeneralXpPerPlayerKill() {
        return Math.max(0, RealCivConfig.WARRIOR_GENERAL_XP_PER_PLAYER_KILL.get());
    }

    public static boolean warriorRequireHubRegistration() {
        return RealCivConfig.WARRIOR_REQUIRE_HUB_REGISTRATION.get();
    }

    public static boolean warriorHomeDefenseNoActionCost() {
        return RealCivConfig.WARRIOR_HOME_DEFENSE_NO_ACTION_COST.get();
    }

    public static double deathActionRefundRatio() {
        return Math.max(0.0D, Math.min(1.0D, RealCivConfig.DEATH_ACTION_REFUND_PERCENT.get() / 100.0D));
    }

    public static boolean staleActionResetEnabled() {
        return RealCivConfig.STALE_ACTION_RESET_ENABLED.get();
    }

    public static long staleActionResetMillis() {
        long minutes = Math.max(1L, RealCivConfig.STALE_ACTION_RESET_MINUTES.get());
        return minutes * 60_000L;
    }

    public static int professionLevelFromXp(@Nullable Profession profession, int xp) {
        if (profession == null || profession == Profession.NONE || xp < 0) {
            return 0;
        }
        List<? extends Integer> thresholds = RealCivConfig.PROFESSION_XP_THRESHOLDS.get();
        int level = 0;
        for (int i = 0; i < thresholds.size(); i++) {
            Integer threshold = thresholds.get(i);
            if (threshold == null || xp < threshold) {
                break;
            }
            level = i;
        }
        return level;
    }

    public static int professionLevelCap(@Nullable Profession profession) {
        if (profession == null || profession == Profession.NONE) {
            return 0;
        }
        for (String raw : RealCivConfig.PROFESSION_LEVEL_CAPS.get()) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) continue;
            Profession parsed = Profession.fromConfigName(parts[0].trim());
            if (parsed == profession) {
                try {
                    return Math.max(0, Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException ignored) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    public static int generalLevelFromXp(int xp) {
        if (xp < 0) return 0;
        List<? extends Integer> thresholds = RealCivConfig.GENERAL_XP_THRESHOLDS.get();
        int level = 0;
        for (int i = 0; i < thresholds.size(); i++) {
            Integer threshold = thresholds.get(i);
            if (threshold == null || xp < threshold) {
                break;
            }
            level = i;
        }
        return level;
    }

    public static int maxProfessionLevelGainsPerDay() {
        return Math.max(0, RealCivConfig.MAX_PROFESSION_LEVEL_GAINS_PER_DAY.get());
    }

    public static int maxGeneralLevelGainsPerDay() {
        return Math.max(0, RealCivConfig.MAX_GENERAL_LEVEL_GAINS_PER_DAY.get());
    }
}
