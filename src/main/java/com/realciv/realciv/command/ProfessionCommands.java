package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.logic.Profession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ProfessionCommands {
    private ProfessionCommands() {
    }

    public static int professionFocusShow(CommandSourceStack source, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CivSavedData data = CivSavedData.get(source.getServer());
        ServerPlayer actor = source.getPlayer();
        if (actor != null
                && !actor.getUUID().equals(target.getUUID())
                && !canManageProfessionFocus(source, data, target)) {
            source.sendFailure(Component.literal("Only leadership/admin can view another player's profession focus."));
            return 0;
        }
        PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        @Nullable Profession focus = record.focusedProfession();
        source.sendSuccess(() -> Component.literal(
                "Profession focus for " + target.getGameProfile().getName() + ": "
                        + (focus == null ? "none" : focus.name())),
                false);
        source.sendSuccess(() -> Component.literal(
                "specialization.singleProfessionLockEnabled="
                        + RealCivConfig.specializationSingleProfessionLockEnabled()
                        + " | specialization.xpDecayEnabled=" + RealCivConfig.specializationXpDecayEnabled()
                        + " | specialization.xpDecayRate="
                        + String.format(Locale.ROOT, "%.2f", RealCivConfig.specializationXpDecayRate())),
                false);
        return 1;
    }

    public static int professionFocusSetSelf(CommandSourceStack source, String professionRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.setPlayerFocusProfession(player.getUUID(), profession, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Focus is already " + profession.name() + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Set profession focus to " + profession.name() + "."), true);
        return 1;
    }

    public static int professionFocusClearSelf(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!data.setPlayerFocusProfession(player.getUUID(), null, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Focus is already cleared."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Cleared profession focus."), true);
        return 1;
    }

    public static int professionFocusAssign(CommandSourceStack source, ServerPlayer target, String professionRaw) {
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!canManageProfessionFocus(source, data, target)) {
            source.sendFailure(Component.literal("Only leadership/admin can assign another player's profession focus."));
            return 0;
        }
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        if (!data.setPlayerFocusProfession(target.getUUID(), profession, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal(
                    "No change made. Focus is already " + profession.name() + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Set profession focus for " + target.getGameProfile().getName() + " to " + profession.name() + "."),
                true);
        return 1;
    }

    public static int professionFocusRemove(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        if (!canManageProfessionFocus(source, data, target)) {
            source.sendFailure(Component.literal("Only leadership/admin can clear another player's profession focus."));
            return 0;
        }
        if (!data.setPlayerFocusProfession(target.getUUID(), null, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Focus is already cleared."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Cleared profession focus for " + target.getGameProfile().getName() + "."),
                true);
        return 1;
    }

    public static int professionXpAdd(CommandSourceStack source, ServerPlayer target, String professionRaw, int amount) {
        return professionXpAdjust(source, target, professionRaw, Math.max(0, amount));
    }

    public static int professionXpReduce(CommandSourceStack source, ServerPlayer target, String professionRaw, int amount) {
        return professionXpAdjust(source, target, professionRaw, -Math.max(0, amount));
    }

    public static int professionXpSet(CommandSourceStack source, ServerPlayer target, String professionRaw, int targetXp) {
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        CivSavedData data = CivSavedData.get(source.getServer());
        PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        int beforeXp = record.professionXp(profession);
        int beforeLevel = record.levelFor(profession);
        record.setProfessionXp(profession, Math.max(0, targetXp));
        int afterXp = record.professionXp(profession);
        int afterLevel = record.levelFor(profession);

        String civId = data.getOrAssignCivilization(target.getUUID());
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " set " + profession.name()
                        + " XP for " + target.getGameProfile().getName()
                        + " from " + beforeXp + " to " + afterXp,
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        source.sendSuccess(() -> Component.literal(
                "Set " + profession.name() + " XP for " + target.getGameProfile().getName()
                        + ": " + beforeXp + " -> " + afterXp
                        + " | level " + beforeLevel + " -> " + afterLevel + "."),
                true);
        return 1;
    }

    public static int professionLevelAdd(CommandSourceStack source, ServerPlayer target, String professionRaw, int deltaLevels) {
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        int currentLevel = CivSavedData.get(source.getServer()).getOrCreatePlayer(target.getUUID()).levelFor(profession);
        int requested = safeAdd(currentLevel, Math.max(0, deltaLevels));
        return professionLevelSet(source, target, professionRaw, requested);
    }

    public static int professionLevelReduce(CommandSourceStack source, ServerPlayer target, String professionRaw, int deltaLevels) {
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        int currentLevel = CivSavedData.get(source.getServer()).getOrCreatePlayer(target.getUUID()).levelFor(profession);
        int requested = Math.max(0, currentLevel - Math.max(0, deltaLevels));
        return professionLevelSet(source, target, professionRaw, requested);
    }

    public static int professionLevelSet(CommandSourceStack source, ServerPlayer target, String professionRaw, int requestedLevel) {
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }

        CivSavedData data = CivSavedData.get(source.getServer());
        PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        int beforeXp = record.professionXp(profession);
        int beforeLevel = record.levelFor(profession);

        LevelTarget targetLevel = resolveLevelTarget(profession, requestedLevel);
        record.setProfessionXp(profession, targetLevel.targetXp());
        int afterXp = record.professionXp(profession);
        int afterLevel = record.levelFor(profession);

        String civId = data.getOrAssignCivilization(target.getUUID());
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " set " + profession.name()
                        + " level for " + target.getGameProfile().getName()
                        + " to " + afterLevel + " (XP " + afterXp + ")",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String clampSuffix = "";
        if (targetLevel.clampedLevel() != requestedLevel) {
            clampSuffix = " (requested " + requestedLevel + ", clamped to " + targetLevel.clampedLevel() + ")";
        }
        final String finalClampSuffix = clampSuffix;
        source.sendSuccess(() -> Component.literal(
                "Set " + profession.name() + " for " + target.getGameProfile().getName()
                        + ": level " + beforeLevel + " -> " + afterLevel
                        + " | XP " + beforeXp + " -> " + afterXp + finalClampSuffix + "."),
                true);
        return 1;
    }

    public static boolean canManageProfessionFocus(CommandSourceStack source, CivSavedData data, ServerPlayer target) {
        if (source.hasPermission(3)) {
            return true;
        }
        ServerPlayer actor = source.getPlayer();
        if (actor == null) {
            return false;
        }
        String actorCiv = data.getOrAssignCivilization(actor.getUUID());
        String targetCiv = data.getOrAssignCivilization(target.getUUID());
        return actorCiv.equals(targetCiv)
                && RealCivCommands.hasCivPermission(source, data, actorCiv, CivSavedData.ROLE_PERMISSION_MANAGE_PROFESSION_FOCUS);
    }

    @Nullable
    public static Profession parseFocusableProfession(@Nullable String raw) {
        Profession parsed = Profession.fromConfigName(raw);
        if (parsed == null || parsed == Profession.NONE) {
            return null;
        }
        return parsed;
    }

    public static List<String> focusableProfessionNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Profession profession : Profession.values()) {
            if (profession != Profession.NONE) {
                names.add(profession.name().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static int professionXpAdjust(CommandSourceStack source, ServerPlayer target, String professionRaw, int deltaXp) {
        Profession profession = parseFocusableProfession(professionRaw);
        if (profession == null) {
            source.sendFailure(Component.literal("Unknown profession. Use one of: " + String.join(", ", focusableProfessionNames()) + "."));
            return 0;
        }
        CivSavedData data = CivSavedData.get(source.getServer());
        PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        int beforeXp = record.professionXp(profession);
        int beforeLevel = record.levelFor(profession);
        int applied = record.addProfessionXpRaw(profession, deltaXp);
        int afterXp = record.professionXp(profession);
        int afterLevel = record.levelFor(profession);

        String civId = data.getOrAssignCivilization(target.getUUID());
        String action = deltaXp >= 0 ? "added" : "reduced";
        data.addAuditLog(
                civId,
                RealCivCommands.actorName(source) + " " + action + " " + profession.name()
                        + " XP for " + target.getGameProfile().getName() + " by " + Math.abs(applied),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        String clampSuffix = "";
        if (applied != deltaXp) {
            clampSuffix = " (clamped by min/max XP bounds)";
        }
        final String finalClampSuffix = clampSuffix;
        source.sendSuccess(() -> Component.literal(
                (deltaXp >= 0 ? "Added " : "Reduced ")
                        + Math.abs(applied) + " " + profession.name() + " XP for " + target.getGameProfile().getName()
                        + ": " + beforeXp + " -> " + afterXp
                        + " | level " + beforeLevel + " -> " + afterLevel
                        + finalClampSuffix + "."),
                true);
        return 1;
    }

    private static LevelTarget resolveLevelTarget(Profession profession, int requestedLevel) {
        int safeRequested = Math.max(0, requestedLevel);
        List<? extends Integer> thresholds = RealCivConfig.PROFESSION_XP_THRESHOLDS.get();
        if (thresholds.isEmpty()) {
            return new LevelTarget(0, 0);
        }

        int maxFromThresholds = Math.max(0, thresholds.size() - 1);
        int configuredCap = RealCivConfig.professionLevelCap(profession);
        int maxLevel = maxFromThresholds;
        if (configuredCap != Integer.MAX_VALUE) {
            maxLevel = Math.max(0, Math.min(maxLevel, configuredCap));
        }

        int clampedLevel = Math.max(0, Math.min(safeRequested, maxLevel));
        Integer threshold = thresholds.get(clampedLevel);
        int targetXp = threshold == null ? 0 : Math.max(0, threshold);
        return new LevelTarget(clampedLevel, targetXp);
    }

    private static int safeAdd(int left, int right) {
        long sum = (long) left + (long) right;
        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (sum < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) sum;
    }

    private record LevelTarget(int clampedLevel, int targetXp) {
    }
}
