package com.realciv.realciv.config;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.logic.ItemResetRule;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.ProfessionEventHookRule;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import com.realciv.realciv.logic.TagResetRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class HubConfig {

    private HubConfig() {
    }

    public static double civTreasuryDepositRatio() {
        return Math.max(0.0D, Math.min(1.0D, RealCivConfig.CIV_TREASURY_DEPOSIT_PERCENT.get() / 100.0D));
    }

    public static double defaultPersonalWithdrawRatio() {
        return Math.max(0.0D, Math.min(1.0D, RealCivConfig.DEFAULT_PERSONAL_WITHDRAW_PERCENT.get() / 100.0D));
    }

    public static boolean useProfessionRuleFiles() {
        return RealCivConfig.HUB_USE_PROFESSION_RULE_FILES.get();
    }

    public static String hubProfessionRuleDirectory() {
        String configured = RealCivConfig.HUB_PROFESSION_RULE_DIRECTORY.get();
        if (configured == null || configured.isBlank()) {
            return "realciv/hub";
        }
        return configured.trim();
    }

    public static void invalidateExternalRuleFileCache() {
        ProfessionRuleFileLoader.invalidateCache();
        RealCivConfig.cachedProfessionEventHookRules = null;
    }

    public static long maxContributionKarmaGainPerDayCents() {
        double cents = RealCivConfig.MAX_CONTRIBUTION_KARMA_GAIN_PER_DAY.get();
        if (cents <= 0.0D) return 0L;
        return (long) Math.round(cents);
    }

    public static double hubWithdrawCreditPenaltyRatio() {
        return Math.max(0.0D, Math.min(10.0D, RealCivConfig.HUB_WITHDRAW_CREDIT_PENALTY_PERCENT.get() / 100.0D));
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
        return parseLegacyRewardRules(RealCivConfig.HUB_REWARD_RULES.get());
    }

    public static List<TagRewardRule> tagRewardRules() {
        @Nullable ProfessionRuleFileLoader.LoadedHubRules external = externalHubRulesOrNull();
        if (external != null) {
            return external.tagRewardRules();
        }
        return parseLegacyTagRewardRules(RealCivConfig.HUB_TAG_REWARD_RULES.get());
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
        return parseLegacyTagResetRules(RealCivConfig.HUB_TAG_RESET_RULES.get());
    }

    @Nullable
    private static ProfessionRuleFileLoader.LoadedHubRules externalHubRulesOrNull() {
        if (!useProfessionRuleFiles()) {
            return null;
        }
        return ProfessionRuleFileLoader.loadFromConfiguredFiles(
                hubProfessionRuleDirectory(),
                RealCivConfig.HUB_REWARD_RULES.get(),
                RealCivConfig.HUB_TAG_REWARD_RULES.get(),
                RealCivConfig.HUB_TAG_RESET_RULES.get());
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

            Double credits = RealCivConfig.tryParseDouble(parts[2].trim());
            Integer professionXp = RealCivConfig.tryParseInt(parts[3].trim());
            Integer generalXp = RealCivConfig.tryParseInt(parts[4].trim());
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

            Double credits = RealCivConfig.tryParseDouble(parts[3].trim());
            Integer professionXp = RealCivConfig.tryParseInt(parts[4].trim());
            Integer generalXp = RealCivConfig.tryParseInt(parts[5].trim());
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

            Double actionsPerItem = RealCivConfig.tryParseDouble(parts[3].trim());
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
}
