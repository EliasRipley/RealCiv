package com.realciv.realciv.config;

import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class ToolConfig {
    private ToolConfig() {}

    public static boolean professionToolLevelGatesEnabled() {
        return RealCivConfig.TOOLS_PROFESSION_LEVEL_GATES_ENABLED.get();
    }

    public static boolean generalToolLevelGatesEnabled() {
        return RealCivConfig.TOOLS_GENERAL_LEVEL_GATES_ENABLED.get();
    }

    public static int requiredProfessionLevelForToolTier(Profession profession, RealCivUtil.ToolTier tier) {
        if (profession == null || profession == Profession.NONE || tier == null || tier == RealCivUtil.ToolTier.NONE) {
            return 0;
        }
        Map<RealCivUtil.ToolTier, Integer> configured = professionToolTierRequirements().get(profession);
        if (configured == null) {
            return 0;
        }
        return Math.max(0, configured.getOrDefault(tier, 0));
    }

    public static int smithyRepairTierRequirement(String tierKey) {
        String key = tierKey.trim().toUpperCase(Locale.ROOT);
        for (String entry : RealCivConfig.SMITHY_REPAIR_TIER_LEVELS.get()) {
            String trimmed = entry.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                String entryKey = trimmed.substring(0, eq).trim().toUpperCase(Locale.ROOT);
                if (entryKey.equals(key)) {
                    try {
                        return Integer.parseInt(trimmed.substring(eq + 1).trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 1;
    }

    public static Set<ResourceLocation> regulatedRedstoneBlocks() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String raw : RealCivConfig.REDSTONER_RESTRICTED_BLOCKS.get()) {
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

    public static Set<ResourceLocation> regulatedExplosiveItems() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String raw : RealCivConfig.EXPLOSIVES_RESTRICTED_ITEMS.get()) {
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

    public static Map<Profession, Map<RealCivUtil.ToolTier, Integer>> professionToolTierRequirements() {
        Map<Profession, Map<RealCivUtil.ToolTier, Integer>> out = new HashMap<>();
        for (String raw : RealCivConfig.PROFESSION_TOOL_TIER_REQUIREMENTS.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length < 2) {
                continue;
            }
            Profession profession = Profession.fromConfigName(parts[0].trim());
            if (profession == null || profession == Profession.NONE) {
                continue;
            }
            Map<RealCivUtil.ToolTier, Integer> tierMap = out.computeIfAbsent(profession, ignored -> new HashMap<>());
            for (int i = 1; i < parts.length; i++) {
                String token = parts[i].trim();
                if (token.isEmpty()) {
                    continue;
                }
                int split = token.indexOf('=');
                if (split <= 0 || split >= token.length() - 1) {
                    continue;
                }
                @Nullable RealCivUtil.ToolTier tier = parseToolTierKey(token.substring(0, split));
                if (tier == null || tier == RealCivUtil.ToolTier.NONE) {
                    continue;
                }
                Integer requiredLevel = tryParseInt(token.substring(split + 1).trim());
                if (requiredLevel == null || requiredLevel < 0) {
                    continue;
                }
                tierMap.put(tier, requiredLevel);
            }
        }
        return out;
    }

    @Nullable
    private static Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static RealCivUtil.ToolTier parseToolTierKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "WOOD", "WOODEN" -> RealCivUtil.ToolTier.WOOD;
            case "GOLD", "GOLDEN" -> RealCivUtil.ToolTier.GOLD;
            case "STONE" -> RealCivUtil.ToolTier.STONE;
            case "IRON" -> RealCivUtil.ToolTier.IRON;
            case "DIAMOND" -> RealCivUtil.ToolTier.DIAMOND;
            case "NETHERITE" -> RealCivUtil.ToolTier.NETHERITE;
            case "UNKNOWN", "ADVANCED" -> RealCivUtil.ToolTier.UNKNOWN;
            default -> null;
        };
    }
}
