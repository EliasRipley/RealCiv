package com.realciv.realciv.config;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.ProfessionEventHook;
import com.realciv.realciv.logic.ProfessionEventHookRule;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class CivConfig {
    private CivConfig() {}

    public static boolean governanceApprovalWorkflowEnabled() {
        return RealCivConfig.GOVERNANCE_APPROVAL_WORKFLOW_ENABLED.get();
    }

    public static long governanceElectionDurationMillis() {
        return 60_000L * Math.max(1, RealCivConfig.GOVERNANCE_ELECTION_DURATION_MINUTES.get());
    }

    public static long governanceCoupDurationMillis() {
        return 60_000L * Math.max(1, RealCivConfig.GOVERNANCE_COUP_DURATION_MINUTES.get());
    }

    public static int governanceCoupMinMembers() {
        return Math.max(2, RealCivConfig.GOVERNANCE_COUP_MIN_MEMBERS.get());
    }

    public static String defaultCivilizationId() {
        String value = RealCivConfig.DEFAULT_CIVILIZATION_ID.get();
        if (value == null || value.isBlank()) {
            return "unaligned";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static String defaultCivilizationName() {
        String value = RealCivConfig.DEFAULT_CIVILIZATION_NAME.get();
        if (value == null || value.isBlank()) {
            return "Unaligned";
        }
        return value.trim();
    }

    public static boolean adminBypassRestrictions() {
        return RealCivConfig.ADMIN_BYPASS_RESTRICTIONS.get();
    }

    public static boolean requireFounderApproval() {
        return RealCivConfig.REQUIRE_FOUNDER_APPROVAL.get();
    }

    public static int maxExplosivesExpertsPerCivilization() {
        return Math.max(0, RealCivConfig.MAX_EXPLOSIVES_EXPERTS_PER_CIV.get());
    }

    public static int maxRedstonersPerCivilization() {
        return Math.max(0, RealCivConfig.MAX_REDSTONERS_PER_CIV.get());
    }

    public static boolean blockNonPlayerExplosionDamageInClaims() {
        return RealCivConfig.EXPLOSIVES_BLOCK_NON_PLAYER_DAMAGE_IN_CLAIMS.get();
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

    public static boolean carryCapPickupEnabled() {
        return RealCivConfig.CARRY_CAP_PICKUP_ENABLED.get();
    }

    public static boolean carryCapCraftEnabled() {
        return RealCivConfig.CARRY_CAP_CRAFT_ENABLED.get();
    }

    public static Map<Profession, Double> carryCapProfessionMultipliers() {
        Map<Profession, Double> multipliers = new HashMap<>();
        multipliers.put(Profession.FARMER, 1.0D);
        multipliers.put(Profession.MINER, 1.0D);
        multipliers.put(Profession.TERRAFORMER, 1.0D);
        multipliers.put(Profession.LUMBERJACK, 1.0D);
        multipliers.put(Profession.FISHER, 1.0D);
        multipliers.put(Profession.HUNTER, 1.0D);
        multipliers.put(Profession.WARRIOR, 1.0D);
        multipliers.put(Profession.EXPLOSIVES_EXPERT, 1.0D);
        multipliers.put(Profession.CRAFTER, 1.0D);
        multipliers.put(Profession.ENCHANTER, 1.0D);
        multipliers.put(Profession.BREWER, 1.0D);
        multipliers.put(Profession.TRADER, 1.0D);

        for (String raw : RealCivConfig.CARRY_CAP_PROFESSION_MULTIPLIERS.get()) {
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

            Double parsed = RealCivConfig.tryParseDouble(parts[1].trim());
            if (parsed == null) {
                continue;
            }
            multipliers.put(profession, Math.max(0.0D, parsed));
        }

        return multipliers;
    }

    public static Map<ResourceLocation, Integer> carryCapItemMaxOverrides() {
        Map<ResourceLocation, Integer> overrides = new HashMap<>();
        for (String raw : RealCivConfig.CARRY_CAP_ITEM_MAX_OVERRIDES.get()) {
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

            Integer maxCount = RealCivConfig.tryParseInt(parts[1].trim());
            if (maxCount == null) {
                continue;
            }
            overrides.put(itemId, Math.max(0, maxCount));
        }
        return overrides;
    }

    public static Map<ResourceLocation, Integer> breakActionCostOverrides() {
        Map<ResourceLocation, Integer> overrides = new HashMap<>();
        for (String raw : RealCivConfig.BREAK_ACTION_COST_OVERRIDES.get()) {
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

            Integer cost = RealCivConfig.tryParseInt(parts[1].trim());
            if (cost == null) {
                continue;
            }
            overrides.put(blockId, Math.max(1, cost));
        }
        return overrides;
    }

    public static List<ProfessionEventHookRule> professionEventHookRules() {
        @Nullable List<ProfessionEventHookRule> cached = RealCivConfig.cachedProfessionEventHookRules;
        if (cached != null) {
            return cached;
        }

        ArrayList<ProfessionEventHookRule> rules = new ArrayList<>();
        for (String raw : RealCivConfig.PROFESSION_EVENT_HOOK_RULES.get()) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length < 3) {
                RealCivMod.LOGGER.warn(
                        "Skipping profession.eventHookRules entry with invalid field count (expected at least 3): {}",
                        line);
                continue;
            }

            ProfessionEventHook hook = ProfessionEventHook.fromConfigName(parts[0].trim());
            if (hook == null) {
                RealCivMod.LOGGER.warn(
                        "Skipping profession.eventHookRules entry with invalid hook '{}': {}",
                        parts[0],
                        line);
                continue;
            }

            Profession profession = Profession.fromConfigName(parts[1].trim());
            if (profession == null || profession == Profession.NONE) {
                RealCivMod.LOGGER.warn(
                        "Skipping profession.eventHookRules entry with invalid profession '{}': {}",
                        parts[1],
                        line);
                continue;
            }

            Integer actionCost = RealCivConfig.tryParseInt(parts[2].trim());
            if (actionCost == null || actionCost < 0) {
                RealCivMod.LOGGER.warn(
                        "Skipping profession.eventHookRules entry with invalid actions_per_trigger '{}': {}",
                        parts[2],
                        line);
                continue;
            }

            int minProfessionLevel = 0;
            int minGeneralLevel = 0;
            long minMembershipMillis = 0L;
            int windowSeconds = 0;
            int maxTriggersPerWindow = 0;
            int professionXpPerTrigger = 0;
            int generalXpPerTrigger = 0;
            @Nullable String statPrefix = null;
            @Nullable String denyMessage = null;

            for (int index = 3; index < parts.length; index++) {
                String token = parts[index].trim();
                if (token.isEmpty()) {
                    continue;
                }
                int equalsIndex = token.indexOf('=');
                if (equalsIndex <= 0) {
                    if (denyMessage == null) {
                        denyMessage = token;
                    } else {
                        denyMessage = denyMessage + "|" + token;
                    }
                    continue;
                }

                String key = token.substring(0, equalsIndex).trim().toLowerCase(Locale.ROOT);
                String value = token.substring(equalsIndex + 1).trim();
                if (value.isEmpty()) {
                    RealCivMod.LOGGER.warn(
                            "Ignoring empty option value '{}' in profession.eventHookRules entry: {}",
                            key,
                            line);
                    continue;
                }

                if ("deny".equals(key) || "deny_message".equals(key)) {
                    denyMessage = value;
                    continue;
                }
                if ("stat_prefix".equals(key)) {
                    statPrefix = value;
                    continue;
                }

                if ("min_profession_level".equals(key)
                        || "min_level".equals(key)
                        || "required_profession_level".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    minProfessionLevel = parsed;
                    continue;
                }
                if ("min_general_level".equals(key) || "required_general_level".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    minGeneralLevel = parsed;
                    continue;
                }
                if ("min_membership_hours".equals(key) || "required_membership_hours".equals(key)) {
                    Double parsed = RealCivConfig.tryParseDouble(value);
                    if (parsed == null || parsed < 0.0D) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    minMembershipMillis = Math.max(0L, (long) Math.round(parsed * 3_600_000.0D));
                    continue;
                }
                if ("window_seconds".equals(key) || "window_s".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    windowSeconds = parsed;
                    continue;
                }
                if ("window_minutes".equals(key) || "window_m".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    windowSeconds = parsed > (Integer.MAX_VALUE / 60)
                            ? Integer.MAX_VALUE
                            : Math.max(0, parsed * 60);
                    continue;
                }
                if ("window_hours".equals(key) || "window_h".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    windowSeconds = parsed > (Integer.MAX_VALUE / 3_600)
                            ? Integer.MAX_VALUE
                            : Math.max(0, parsed * 3_600);
                    continue;
                }
                if ("max_triggers".equals(key)
                        || "window_max_triggers".equals(key)
                        || "window_budget".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    maxTriggersPerWindow = parsed;
                    continue;
                }
                if ("profession_xp".equals(key) || "profession_xp_per_trigger".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    professionXpPerTrigger = parsed;
                    continue;
                }
                if ("general_xp".equals(key) || "general_xp_per_trigger".equals(key)) {
                    Integer parsed = RealCivConfig.tryParseInt(value);
                    if (parsed == null || parsed < 0) {
                        RealCivMod.LOGGER.warn(
                                "Ignoring invalid {}='{}' in profession.eventHookRules entry: {}",
                                key,
                                value,
                                line);
                        continue;
                    }
                    generalXpPerTrigger = parsed;
                    continue;
                }

                RealCivMod.LOGGER.warn(
                        "Ignoring unknown profession.eventHookRules option '{}' in entry: {}",
                        key,
                        line);
            }

            rules.add(new ProfessionEventHookRule(
                    hook,
                    profession,
                    actionCost,
                    minProfessionLevel,
                    minGeneralLevel,
                    minMembershipMillis,
                    windowSeconds,
                    maxTriggersPerWindow,
                    professionXpPerTrigger,
                    generalXpPerTrigger,
                    statPrefix,
                    denyMessage));
        }
        List<ProfessionEventHookRule> parsed = List.copyOf(rules);
        RealCivConfig.cachedProfessionEventHookRules = parsed;
        return parsed;
    }

    public static boolean migrateLegacyCommonConfigIfNeeded() {
        boolean changed = false;
        List<Integer> current = sanitizeIntegerList(RealCivConfig.LUMBERJACK_LIMITS.get());
        if (current.equals(RealCivConfig.LEGACY_LUMBERJACK_LIMITS)) {
            RealCivConfig.LUMBERJACK_LIMITS.set(RealCivConfig.CURRENT_LUMBERJACK_LIMITS);
            RealCivMod.LOGGER.info(
                    "Migrated legacy lumberjack limits {} -> {} for RealCiv common config.",
                    RealCivConfig.LEGACY_LUMBERJACK_LIMITS,
                    RealCivConfig.CURRENT_LUMBERJACK_LIMITS);
            changed = true;
        }

        String configuredCivId = RealCivConfig.DEFAULT_CIVILIZATION_ID.get();
        if (configuredCivId != null && configuredCivId.equalsIgnoreCase(RealCivConfig.LEGACY_DEFAULT_CIV_ID)) {
            RealCivConfig.DEFAULT_CIVILIZATION_ID.set("unaligned");
            RealCivMod.LOGGER.info("Migrated legacy default civilization id '{}' -> 'unaligned'.", configuredCivId);
            changed = true;
        }

        String configuredCivName = RealCivConfig.DEFAULT_CIVILIZATION_NAME.get();
        if (configuredCivName != null && configuredCivName.trim().equals(RealCivConfig.LEGACY_DEFAULT_CIV_NAME)) {
            RealCivConfig.DEFAULT_CIVILIZATION_NAME.set("Unaligned");
            RealCivMod.LOGGER.info("Migrated legacy default civilization name '{}' -> 'Unaligned'.", configuredCivName);
            changed = true;
        }

        if (RealCivConfig.LAND_BLOCK_UNCLAIMED_BUILDING.get() && changed) {
            RealCivConfig.LAND_BLOCK_UNCLAIMED_BUILDING.set(false);
            RealCivMod.LOGGER.info("Migrated legacy land.blockUnclaimedBuilding true -> false.");
        }

        if (changed) {
            RealCivConfig.SPEC.save();
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
}
