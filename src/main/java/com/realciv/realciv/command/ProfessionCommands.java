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
}
