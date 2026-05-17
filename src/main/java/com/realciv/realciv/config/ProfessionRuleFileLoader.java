package com.realciv.realciv.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.logic.ItemResetRule;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.TagResetRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

final class ProfessionRuleFileLoader {
    private static final Object LOCK = new Object();
    @Nullable
    private static LoadedHubRules cachedRules;
    private static final TomlParser TOML_PARSER = new TomlParser();
    private static final TomlWriter TOML_WRITER = new TomlWriter();
    private static final List<Profession> PROFESSIONS = List.of(
            Profession.FARMER,
            Profession.MINER,
            Profession.TERRAFORMER,
            Profession.LUMBERJACK,
            Profession.FISHER,
            Profession.HUNTER,
            Profession.CRAFTER,
            Profession.ENCHANTER,
            Profession.BREWER,
            Profession.TRADER);

    private ProfessionRuleFileLoader() {
    }

    static void invalidateCache() {
        synchronized (LOCK) {
            cachedRules = null;
        }
    }

    static LoadedHubRules loadFromConfiguredFiles(
            String configuredDir,
            List<? extends String> legacyRewardRules,
            List<? extends String> legacyTagRewardRules,
            List<? extends String> legacyTagResetRules) {
        synchronized (LOCK) {
            if (cachedRules != null) {
                return cachedRules;
            }
            cachedRules = loadInternal(configuredDir, legacyRewardRules, legacyTagRewardRules, legacyTagResetRules);
            return cachedRules;
        }
    }

    private static LoadedHubRules loadInternal(
            String configuredDir,
            List<? extends String> legacyRewardRules,
            List<? extends String> legacyTagRewardRules,
            List<? extends String> legacyTagResetRules) {
        Path baseDir = resolveDirectory(configuredDir);
        migrateOldTxtFiles(baseDir);
        bootstrapFilesIfMissing(baseDir, legacyRewardRules, legacyTagRewardRules, legacyTagResetRules);

        Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries = new HashMap<>();
        List<TagRewardRule> tagRewardRules = new ArrayList<>();
        Map<ResourceLocation, ItemResetRule> itemResetRules = new HashMap<>();
        List<TagResetRule> tagResetRules = new ArrayList<>();

        for (Profession profession : PROFESSIONS) {
            parseProfessionFile(baseDir, profession, exactRewardEntries, tagRewardRules, itemResetRules, tagResetRules);
        }

        return new LoadedHubRules(
                Map.copyOf(exactRewardEntries),
                List.copyOf(tagRewardRules),
                Map.copyOf(itemResetRules),
                List.copyOf(tagResetRules));
    }

    private static Path resolveDirectory(String configuredDir) {
        String raw = configuredDir == null ? "" : configuredDir.trim();
        if (raw.isEmpty()) {
            raw = "realciv/hub";
        }
        Path configuredPath = Path.of(raw);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return FMLPaths.CONFIGDIR.get().resolve(configuredPath).normalize();
    }

    private static void migrateOldTxtFiles(Path baseDir) {
        for (Profession profession : PROFESSIONS) {
            Path oldReward = baseDir.resolve(fileStem(profession) + "_rewards.txt");
            Path oldReset = baseDir.resolve(fileStem(profession) + "_resets.txt");
            Path newFile = professionFile(baseDir, profession);
            if (Files.exists(newFile)) continue;
            boolean hasReward = Files.exists(oldReward);
            boolean hasReset = Files.exists(oldReset);
            if (!hasReward && !hasReset) continue;

            try {
                List<Config> rewards = new ArrayList<>();
                List<Config> resets = new ArrayList<>();
                if (hasReward) {
                    for (String line : Files.readAllLines(oldReward)) {
                        String entry = normalizeOldLine(line);
                        if (entry != null) rewards.add(parseOldRewardLine(profession, entry));
                    }
                }
                if (hasReset) {
                    for (String line : Files.readAllLines(oldReset)) {
                        String entry = normalizeOldLine(line);
                        if (entry != null) resets.add(parseOldResetLine(profession, entry));
                    }
                }
                Config config = Config.inMemory();
                if (!rewards.isEmpty()) config.add("reward", rewards);
                if (!resets.isEmpty()) config.add("reset", resets);
                try (var out = Files.newOutputStream(newFile)) {
                    TOML_WRITER.write(config, out);
                }
                if (hasReward) Files.delete(oldReward);
                if (hasReset) Files.delete(oldReset);
                RealCivMod.LOGGER.info("Migrated {} hub rules from .txt to .toml", profession.name());
            } catch (IOException ex) {
                RealCivMod.LOGGER.warn("Failed migrating old hub rule files for {}: {}", profession.name(), ex.toString());
            }
        }
    }

    @Nullable
    private static String normalizeOldLine(@Nullable String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null;
        int commentIndex = trimmed.indexOf('#');
        if (commentIndex >= 0) trimmed = trimmed.substring(0, commentIndex).trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Config parseOldRewardLine(Profession profession, String line) {
        String[] parts = line.split("\\|");
        Config entry = Config.inMemory();
        if (parts.length == 4) {
            entry.add("item", parts[0].trim());
            entry.add("credits", tryParseDouble(parts[1].trim()));
            entry.add("profession_xp", tryParseInt(parts[2].trim()));
            entry.add("general_xp", tryParseInt(parts[3].trim()));
        } else if (parts.length == 5) {
            String kind = parts[0].trim().toUpperCase(Locale.ROOT);
            if (kind.equals("ITEM")) {
                entry.add("item", parts[1].trim());
            } else {
                entry.add("selector", kind);
                entry.add("tag", parts[1].trim());
            }
            entry.add("credits", tryParseDouble(parts[2].trim()));
            entry.add("profession_xp", tryParseInt(parts[3].trim()));
            entry.add("general_xp", tryParseInt(parts[4].trim()));
        }
        return entry;
    }

    private static Config parseOldResetLine(Profession profession, String line) {
        String[] parts = line.split("\\|");
        Config entry = Config.inMemory();
        if (parts.length == 2) {
            entry.add("item", parts[0].trim());
            entry.add("actions", tryParseDouble(parts[1].trim()));
        } else if (parts.length == 3) {
            String kind = parts[0].trim().toUpperCase(Locale.ROOT);
            if (kind.equals("ITEM")) {
                entry.add("item", parts[1].trim());
            } else {
                entry.add("selector", kind);
                entry.add("tag", parts[1].trim());
            }
            entry.add("actions", tryParseDouble(parts[2].trim()));
        }
        return entry;
    }

    private static void bootstrapFilesIfMissing(
            Path baseDir,
            List<? extends String> legacyRewardRules,
            List<? extends String> legacyTagRewardRules,
            List<? extends String> legacyTagResetRules) {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ex) {
            RealCivMod.LOGGER.warn("Failed to create RealCiv hub rule directory '{}': {}", baseDir, ex.toString());
            return;
        }

        for (Profession profession : PROFESSIONS) {
            Path file = professionFile(baseDir, profession);
            if (Files.exists(file)) continue;

            List<Config> rewards = new ArrayList<>();
            List<Config> resets = new ArrayList<>();
            seedFromLegacyRules(profession, rewards, resets, legacyRewardRules, legacyTagRewardRules, legacyTagResetRules);

            Config config = Config.inMemory();
            if (!rewards.isEmpty()) config.add("reward", rewards);
            if (!resets.isEmpty()) config.add("reset", resets);
                try (var out = Files.newOutputStream(file)) {
                    TOML_WRITER.write(config, out);
                    RealCivMod.LOGGER.info("Created default hub rule file: {}", file);
                } catch (IOException ex) {
                    RealCivMod.LOGGER.warn("Failed writing default hub rule file '{}': {}", file, ex.toString());
                }
        }
    }

    private static void seedFromLegacyRules(
            Profession profession,
            List<Config> rewards,
            List<Config> resets,
            List<? extends String> legacyRewardRules,
            List<? extends String> legacyTagRewardRules,
            List<? extends String> legacyTagResetRules) {
        for (String raw : legacyRewardRules) {
            seedReward(profession, raw, rewards);
        }
        for (String raw : legacyTagRewardRules) {
            seedTagReward(profession, raw, rewards);
        }
        for (String raw : legacyTagResetRules) {
            seedReset(profession, raw, resets);
        }
    }

    private static void seedReward(Profession profession, String raw, List<Config> out) {
        String[] parts = splitOldLine(raw);
        if (parts == null || parts.length != 5) return;
        Profession p = Profession.fromConfigName(parts[1].trim());
        if (p != profession) return;
        Config entry = Config.inMemory();
        entry.add("item", parts[0].trim());
        entry.add("credits", tryParseDouble(parts[2].trim()));
        entry.add("profession_xp", tryParseInt(parts[3].trim()));
        entry.add("general_xp", tryParseInt(parts[4].trim()));
        out.add(entry);
    }

    private static void seedTagReward(Profession profession, String raw, List<Config> out) {
        String[] parts = splitOldLine(raw);
        if (parts == null || parts.length != 6) return;
        Profession p = Profession.fromConfigName(parts[2].trim());
        if (p != profession) return;
        Config entry = Config.inMemory();
        entry.add("selector", parts[0].trim());
        entry.add("tag", parts[1].trim());
        entry.add("credits", tryParseDouble(parts[3].trim()));
        entry.add("profession_xp", tryParseInt(parts[4].trim()));
        entry.add("general_xp", tryParseInt(parts[5].trim()));
        out.add(entry);
    }

    private static void seedReset(Profession profession, String raw, List<Config> out) {
        String[] parts = splitOldLine(raw);
        if (parts == null || parts.length != 4) return;
        Profession p = Profession.fromConfigName(parts[2].trim());
        if (p != profession) return;
        Config entry = Config.inMemory();
        if ("ITEM".equalsIgnoreCase(parts[0].trim())) {
            entry.add("item", parts[1].trim());
        } else {
            entry.add("selector", parts[0].trim());
            entry.add("tag", parts[1].trim());
        }
        entry.add("actions", tryParseDouble(parts[3].trim()));
        out.add(entry);
    }

    @Nullable
    private static String[] splitOldLine(@Nullable String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null;
        return trimmed.split("\\|");
    }

    private static void parseProfessionFile(
            Path baseDir,
            Profession profession,
            Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries,
            List<TagRewardRule> tagRewardRules,
            Map<ResourceLocation, ItemResetRule> itemResetRules,
            List<TagResetRule> tagResetRules) {
        Path file = professionFile(baseDir, profession);
        if (Files.notExists(file)) return;

        Config config;
        try (var in = Files.newInputStream(file)) {
            config = TOML_PARSER.parse(in);
        } catch (Exception ex) {
            RealCivMod.LOGGER.warn("Failed parsing hub rule file '{}': {}", file, ex.toString());
            return;
        }

        List<Object> rawRewards = config.get("reward");
        if (rawRewards != null) {
            for (Object raw : rawRewards) {
                if (raw instanceof Config entry) {
                    parseRewardEntry(profession, file, entry, exactRewardEntries, tagRewardRules);
                }
            }
        }

        List<Object> rawResets = config.get("reset");
        if (rawResets != null) {
            for (Object raw : rawResets) {
                if (raw instanceof Config entry) {
                    parseResetEntry(profession, file, entry, itemResetRules, tagResetRules);
                }
            }
        }
    }

    private static void parseRewardEntry(
            Profession profession,
            Path file,
            Config entry,
            Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries,
            List<TagRewardRule> tagRewardRules) {
        String item = entry.get("item");
        if (item != null) {
            ResourceLocation itemId = ResourceLocation.tryParse(item.trim());
            if (itemId == null) {
                RealCivMod.LOGGER.warn("Invalid item id in '{}': {}", file, item);
                return;
            }
            double credits = toDouble(entry.get("credits"));
            int profXp = toInt(entry.get("profession_xp"));
            int genXp = toInt(entry.get("general_xp"));
            exactRewardEntries.put(itemId, new ParsedRewardEntry(
                    profession,
                    Math.max(0.0D, credits),
                    Math.max(0, profXp),
                    Math.max(0, genXp)));
            return;
        }

        String tag = entry.get("tag");
        if (tag != null) {
            String selector = entry.get("selector");
            TagRewardRule.SelectorType selectorType = selector == null
                    ? TagRewardRule.SelectorType.ITEM_TAG
                    : TagRewardRule.SelectorType.fromConfig(selector);
            if (selectorType == null) {
                RealCivMod.LOGGER.warn("Invalid selector in '{}': {}", file, selector);
                return;
            }
            ResourceLocation tagId = ResourceLocation.tryParse(tag.trim());
            if (tagId == null) {
                RealCivMod.LOGGER.warn("Invalid tag id in '{}': {}", file, tag);
                return;
            }
            double credits = toDouble(entry.get("credits"));
            int profXp = toInt(entry.get("profession_xp"));
            int genXp = toInt(entry.get("general_xp"));
            tagRewardRules.add(new TagRewardRule(
                    selectorType,
                    tagId,
                    profession,
                    com.realciv.realciv.logic.RealCivUtil.creditsToCents(Math.max(0.0D, credits)),
                    Math.max(0, profXp),
                    Math.max(0, genXp)));
            return;
        }

        RealCivMod.LOGGER.warn("Reward entry missing both 'item' and 'tag' in '{}'", file);
    }

    private static void parseResetEntry(
            Profession profession,
            Path file,
            Config entry,
            Map<ResourceLocation, ItemResetRule> itemResetRules,
            List<TagResetRule> tagResetRules) {
        String item = entry.get("item");
        if (item != null) {
            ResourceLocation itemId = ResourceLocation.tryParse(item.trim());
            if (itemId == null) {
                RealCivMod.LOGGER.warn("Invalid item id in reset entry in '{}': {}", file, item);
                return;
            }
            double actions = toDouble(entry.get("actions"));
            itemResetRules.put(itemId, new ItemResetRule(itemId, profession, Math.max(0.0D, actions)));
            return;
        }

        String tag = entry.get("tag");
        if (tag != null) {
            String selector = entry.get("selector");
            TagRewardRule.SelectorType selectorType = selector == null
                    ? TagRewardRule.SelectorType.ITEM_TAG
                    : TagRewardRule.SelectorType.fromConfig(selector);
            if (selectorType == null) {
                RealCivMod.LOGGER.warn("Invalid selector in reset entry in '{}': {}", file, selector);
                return;
            }
            ResourceLocation tagId = ResourceLocation.tryParse(tag.trim());
            if (tagId == null) {
                RealCivMod.LOGGER.warn("Invalid tag id in reset entry in '{}': {}", file, tag);
                return;
            }
            double actions = toDouble(entry.get("actions"));
            tagResetRules.add(new TagResetRule(selectorType, tagId, profession, Math.max(0.0D, actions)));
            return;
        }

        RealCivMod.LOGGER.warn("Reset entry missing both 'item' and 'tag' in '{}'", file);
    }

    private static double toDouble(@Nullable Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return 0.0D;
    }

    private static int toInt(@Nullable Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    private static double tryParseDouble(String value) {
        try { return Double.parseDouble(value.trim()); } catch (NumberFormatException e) { return 0.0D; }
    }

    private static int tryParseInt(String value) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static Path professionFile(Path baseDir, Profession profession) {
        return baseDir.resolve(fileStem(profession) + ".toml");
    }

    private static String fileStem(Profession profession) {
        return profession.name().toLowerCase(Locale.ROOT);
    }

    record ParsedRewardEntry(
            Profession profession,
            double creditsPerItem,
            int professionXpPerItem,
            int generalXpPerItem) {
    }

    record LoadedHubRules(
            Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries,
            List<TagRewardRule> tagRewardRules,
            Map<ResourceLocation, ItemResetRule> itemResetRules,
            List<TagResetRule> tagResetRules) {
    }
}
