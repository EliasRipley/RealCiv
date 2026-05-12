package com.realciv.realciv.config;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.logic.ItemResetRule;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.TagResetRule;
import com.realciv.realciv.logic.TagRewardRule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

final class ProfessionRuleFileLoader {
    private static final Object LOCK = new Object();
    @Nullable
    private static LoadedHubRules cachedRules;
    private static final List<Profession> PROFESSIONS = List.of(
            Profession.FARMER,
            Profession.MINER,
            Profession.TERRAFORMER,
            Profession.LUMBERJACK,
            Profession.FISHER,
            Profession.HUNTER,
            Profession.CRAFTER);

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
        bootstrapFilesIfMissing(baseDir, legacyRewardRules, legacyTagRewardRules, legacyTagResetRules);

        Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries = new HashMap<>();
        List<TagRewardRule> tagRewardRules = new ArrayList<>();
        Map<ResourceLocation, ItemResetRule> itemResetRules = new HashMap<>();
        List<TagResetRule> tagResetRules = new ArrayList<>();

        for (Profession profession : PROFESSIONS) {
            parseRewardFile(baseDir, profession, exactRewardEntries, tagRewardRules);
            parseResetFile(baseDir, profession, itemResetRules, tagResetRules);
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

        Map<Profession, List<String>> rewardLinesByProfession = new EnumMap<>(Profession.class);
        Map<Profession, List<String>> resetLinesByProfession = new EnumMap<>(Profession.class);
        for (Profession profession : PROFESSIONS) {
            rewardLinesByProfession.put(profession, new ArrayList<>());
            resetLinesByProfession.put(profession, new ArrayList<>());
        }

        for (String raw : legacyRewardRules) {
            seedRewardFromLegacy(raw, rewardLinesByProfession);
        }
        for (String raw : legacyTagRewardRules) {
            seedTagRewardFromLegacy(raw, rewardLinesByProfession);
        }
        for (String raw : legacyTagResetRules) {
            seedResetFromLegacy(raw, resetLinesByProfession);
        }

        for (Profession profession : PROFESSIONS) {
            Path rewardFile = rewardFile(baseDir, profession);
            if (Files.notExists(rewardFile)) {
                writeLinesSafely(rewardFile, defaultRewardFileHeader(profession, rewardLinesByProfession.get(profession)));
            }

            Path resetFile = resetFile(baseDir, profession);
            if (Files.notExists(resetFile)) {
                writeLinesSafely(resetFile, defaultResetFileHeader(profession, resetLinesByProfession.get(profession)));
            }
        }
    }

    private static void writeLinesSafely(Path target, List<String> lines) {
        try {
            Files.write(target, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            RealCivMod.LOGGER.warn("Failed writing RealCiv rule file '{}': {}", target, ex.toString());
        }
    }

    private static List<String> defaultRewardFileHeader(Profession profession, List<String> defaults) {
        List<String> out = new ArrayList<>();
        out.add("# RealCiv " + profession.name() + " reward rules");
        out.add("# One rule per line. Profession is implied by this file.");
        out.add("#");
        out.add("# Item reward (shorthand):");
        out.add("#   minecraft:wheat|1.0|2|1");
        out.add("# Item reward (explicit):");
        out.add("#   ITEM|minecraft:wheat|1.0|2|1");
        out.add("# Tag reward (item tag):");
        out.add("#   ITEM_TAG|realciv:farmer_contributions|1.0|2|1");
        out.add("# Tag reward (block tag):");
        out.add("#   BLOCK_TAG|minecraft:logs|1.0|2|1");
        out.add("#");
        if (defaults != null && !defaults.isEmpty()) {
            out.addAll(defaults);
        } else {
            out.add("# No defaults generated for this profession.");
        }
        return out;
    }

    private static List<String> defaultResetFileHeader(Profession profession, List<String> defaults) {
        List<String> out = new ArrayList<>();
        out.add("# RealCiv " + profession.name() + " reset rules");
        out.add("# One rule per line. Profession is implied by this file.");
        out.add("#");
        out.add("# Exact item reset (shorthand):");
        out.add("#   minecraft:wheat|1.0");
        out.add("# Exact item reset (explicit):");
        out.add("#   ITEM|minecraft:wheat|1.0");
        out.add("# Tag reset (item tag):");
        out.add("#   ITEM_TAG|realciv:farmer_reset_items|1.0");
        out.add("# Tag reset (block tag):");
        out.add("#   BLOCK_TAG|minecraft:logs|1.0");
        out.add("#");
        if (defaults != null && !defaults.isEmpty()) {
            out.addAll(defaults);
        } else {
            out.add("# No defaults generated for this profession.");
        }
        return out;
    }

    private static void seedRewardFromLegacy(String raw, Map<Profession, List<String>> out) {
        String line = normalizeDataLine(raw);
        if (line == null) {
            return;
        }
        String[] parts = line.split("\\|");
        if (parts.length != 5) {
            return;
        }
        Profession profession = Profession.fromConfigName(parts[1].trim());
        if (profession == null || profession == Profession.NONE || !out.containsKey(profession)) {
            return;
        }
        out.get(profession).add("ITEM|" + parts[0].trim() + "|" + parts[2].trim() + "|" + parts[3].trim() + "|" + parts[4].trim());
    }

    private static void seedTagRewardFromLegacy(String raw, Map<Profession, List<String>> out) {
        String line = normalizeDataLine(raw);
        if (line == null) {
            return;
        }
        String[] parts = line.split("\\|");
        if (parts.length != 6) {
            return;
        }
        Profession profession = Profession.fromConfigName(parts[2].trim());
        if (profession == null || profession == Profession.NONE || !out.containsKey(profession)) {
            return;
        }
        out.get(profession).add(parts[0].trim() + "|" + parts[1].trim() + "|" + parts[3].trim() + "|" + parts[4].trim() + "|" + parts[5].trim());
    }

    private static void seedResetFromLegacy(String raw, Map<Profession, List<String>> out) {
        String line = normalizeDataLine(raw);
        if (line == null) {
            return;
        }
        String[] parts = line.split("\\|");
        if (parts.length != 4) {
            return;
        }
        Profession profession = Profession.fromConfigName(parts[2].trim());
        if (profession == null || profession == Profession.NONE || !out.containsKey(profession)) {
            return;
        }
        out.get(profession).add(parts[0].trim() + "|" + parts[1].trim() + "|" + parts[3].trim());
    }

    private static void parseRewardFile(
            Path baseDir,
            Profession profession,
            Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries,
            List<TagRewardRule> tagRewardRules) {
        Path file = rewardFile(baseDir, profession);
        if (Files.notExists(file)) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            RealCivMod.LOGGER.warn("Failed reading reward file '{}': {}", file, ex.toString());
            return;
        }

        for (String raw : lines) {
            String line = normalizeDataLine(raw);
            if (line == null) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length == 4) {
                parseRewardItemLine(profession, file, line, parts, exactRewardEntries);
                continue;
            }
            if (parts.length == 5) {
                parseTypedRewardLine(profession, file, line, parts, exactRewardEntries, tagRewardRules);
                continue;
            }

            RealCivMod.LOGGER.warn(
                    "Skipping malformed reward line in '{}': '{}' (expected 4 or 5 fields)",
                    file,
                    line);
        }
    }

    private static void parseRewardItemLine(
            Profession profession,
            Path file,
            String line,
            String[] parts,
            Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries) {
        ResourceLocation itemId = parseResourceLocation(parts[0], file, line, "item id");
        if (itemId == null) {
            return;
        }
        Double credits = tryParseDouble(parts[1].trim());
        Integer professionXp = tryParseInt(parts[2].trim());
        Integer generalXp = tryParseInt(parts[3].trim());
        if (credits == null || professionXp == null || generalXp == null) {
            RealCivMod.LOGGER.warn("Skipping reward line with invalid numeric values in '{}': '{}'", file, line);
            return;
        }
        exactRewardEntries.put(itemId, new ParsedRewardEntry(
                profession,
                Math.max(0.0D, credits),
                Math.max(0, professionXp),
                Math.max(0, generalXp)));
    }

    private static void parseTypedRewardLine(
            Profession profession,
            Path file,
            String line,
            String[] parts,
            Map<ResourceLocation, ParsedRewardEntry> exactRewardEntries,
            List<TagRewardRule> tagRewardRules) {
        String kind = parts[0].trim().toUpperCase(java.util.Locale.ROOT);
        if (kind.equals("ITEM")) {
            parseRewardItemLine(profession, file, line, new String[]{parts[1], parts[2], parts[3], parts[4]}, exactRewardEntries);
            return;
        }

        TagRewardRule.SelectorType selectorType = TagRewardRule.SelectorType.fromConfig(kind);
        if (selectorType == null) {
            RealCivMod.LOGGER.warn("Skipping reward line with invalid selector in '{}': '{}'", file, line);
            return;
        }

        ResourceLocation tagId = parseResourceLocation(parts[1], file, line, "tag id");
        if (tagId == null) {
            return;
        }
        Double credits = tryParseDouble(parts[2].trim());
        Integer professionXp = tryParseInt(parts[3].trim());
        Integer generalXp = tryParseInt(parts[4].trim());
        if (credits == null || professionXp == null || generalXp == null) {
            RealCivMod.LOGGER.warn("Skipping reward line with invalid numeric values in '{}': '{}'", file, line);
            return;
        }

        tagRewardRules.add(new TagRewardRule(
                selectorType,
                tagId,
                profession,
                com.realciv.realciv.logic.RealCivUtil.creditsToCents(Math.max(0.0D, credits)),
                Math.max(0, professionXp),
                Math.max(0, generalXp)));
    }

    private static void parseResetFile(
            Path baseDir,
            Profession profession,
            Map<ResourceLocation, ItemResetRule> itemResetRules,
            List<TagResetRule> tagResetRules) {
        Path file = resetFile(baseDir, profession);
        if (Files.notExists(file)) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            RealCivMod.LOGGER.warn("Failed reading reset file '{}': {}", file, ex.toString());
            return;
        }

        for (String raw : lines) {
            String line = normalizeDataLine(raw);
            if (line == null) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length == 2) {
                parseResetItemLine(profession, file, line, parts, itemResetRules);
                continue;
            }
            if (parts.length == 3) {
                parseTypedResetLine(profession, file, line, parts, itemResetRules, tagResetRules);
                continue;
            }

            RealCivMod.LOGGER.warn(
                    "Skipping malformed reset line in '{}': '{}' (expected 2 or 3 fields)",
                    file,
                    line);
        }
    }

    private static void parseResetItemLine(
            Profession profession,
            Path file,
            String line,
            String[] parts,
            Map<ResourceLocation, ItemResetRule> itemResetRules) {
        ResourceLocation itemId = parseResourceLocation(parts[0], file, line, "item id");
        if (itemId == null) {
            return;
        }
        Double actionsPerItem = tryParseDouble(parts[1].trim());
        if (actionsPerItem == null) {
            RealCivMod.LOGGER.warn("Skipping reset line with invalid action value in '{}': '{}'", file, line);
            return;
        }
        itemResetRules.put(itemId, new ItemResetRule(itemId, profession, Math.max(0.0D, actionsPerItem)));
    }

    private static void parseTypedResetLine(
            Profession profession,
            Path file,
            String line,
            String[] parts,
            Map<ResourceLocation, ItemResetRule> itemResetRules,
            List<TagResetRule> tagResetRules) {
        String kind = parts[0].trim().toUpperCase(java.util.Locale.ROOT);
        if (kind.equals("ITEM")) {
            parseResetItemLine(profession, file, line, new String[]{parts[1], parts[2]}, itemResetRules);
            return;
        }

        TagRewardRule.SelectorType selectorType = TagRewardRule.SelectorType.fromConfig(kind);
        if (selectorType == null) {
            RealCivMod.LOGGER.warn("Skipping reset line with invalid selector in '{}': '{}'", file, line);
            return;
        }

        ResourceLocation tagId = parseResourceLocation(parts[1], file, line, "tag id");
        if (tagId == null) {
            return;
        }
        Double actionsPerItem = tryParseDouble(parts[2].trim());
        if (actionsPerItem == null) {
            RealCivMod.LOGGER.warn("Skipping reset line with invalid action value in '{}': '{}'", file, line);
            return;
        }

        tagResetRules.add(new TagResetRule(
                selectorType,
                tagId,
                profession,
                Math.max(0.0D, actionsPerItem)));
    }

    @Nullable
    private static String normalizeDataLine(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        int commentIndex = trimmed.indexOf('#');
        if (commentIndex >= 0) {
            trimmed = trimmed.substring(0, commentIndex).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(String value, Path file, String line, String label) {
        try {
            return ResourceLocation.parse(value.trim());
        } catch (Exception ex) {
            RealCivMod.LOGGER.warn(
                    "Skipping line with invalid {} in '{}': '{}'",
                    label,
                    file,
                    line);
            return null;
        }
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

    private static Path rewardFile(Path baseDir, Profession profession) {
        return baseDir.resolve(fileStem(profession) + "_rewards.txt");
    }

    private static Path resetFile(Path baseDir, Profession profession) {
        return baseDir.resolve(fileStem(profession) + "_resets.txt");
    }

    private static String fileStem(Profession profession) {
        return profession.name().toLowerCase(java.util.Locale.ROOT);
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
