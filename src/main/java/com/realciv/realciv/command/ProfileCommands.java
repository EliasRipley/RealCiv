package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ProfileCommands {
    private ProfileCommands() {
    }

    public static int showProfile(CommandSourceStack source, ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(target.getUUID());

        int farmerLevel = record.levelFor(Profession.FARMER);
        int minerLevel = record.levelFor(Profession.MINER);
        int terraformerLevel = record.levelFor(Profession.TERRAFORMER);
        int lumberjackLevel = record.levelFor(Profession.LUMBERJACK);
        int fisherLevel = record.levelFor(Profession.FISHER);
        int hunterLevel = record.levelFor(Profession.HUNTER);
        int warriorLevel = record.levelFor(Profession.WARRIOR);
        int explosivesLevel = record.levelFor(Profession.EXPLOSIVES_EXPERT);
        int crafterLevel = record.levelFor(Profession.CRAFTER);
        int enchanterLevel = record.levelFor(Profession.ENCHANTER);
        int brewerLevel = record.levelFor(Profession.BREWER);
        int traderLevel = record.levelFor(Profession.TRADER);
        int generalLevel = record.generalLevel();

        source.sendSuccess(() -> Component.literal("RealCiv profile for " + target.getGameProfile().getName()), false);
        if (source.hasPermission(3)) {
            source.sendSuccess(() -> Component.literal("Civilization: " + RealCivCommands.civDisplay(data, civId) + " [" + civId + "]"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Civilization: " + RealCivCommands.civDisplay(data, civId)), false);
        }
        source.sendSuccess(() -> Component.literal(
                "Credits: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                        + " | General Level: " + generalLevel + " (" + record.generalXp() + " XP)"), false);
        @Nullable Profession focus = record.focusedProfession();
        source.sendSuccess(() -> Component.literal(
                "Focus: " + (focus == null ? "none" : focus.name())
                        + " | specialization lock: " + (RealCivConfig.specializationSingleProfessionLockEnabled() ? "ON" : "OFF")
                        + " | XP decay: " + (RealCivConfig.specializationXpDecayEnabled() ? "ON" : "OFF")
                        + " (" + String.format(Locale.ROOT, "%.2f", RealCivConfig.specializationXpDecayRate()) + "x)"),
                false);
        source.sendSuccess(() -> Component.literal(
                "Farmer L" + farmerLevel + " | actions " + record.farmerActions() + "/"
                        + RealCivConfig.farmerLimitForLevel(farmerLevel)
                        + " | XP " + record.farmerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Miner L" + minerLevel + " | actions " + record.minerActions() + "/"
                        + RealCivConfig.minerLimitForLevel(minerLevel)
                        + " | XP " + record.minerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Terraformer L" + terraformerLevel + " | actions " + record.terraformerActions() + "/"
                        + RealCivConfig.terraformerLimitForLevel(terraformerLevel)
                        + " | XP " + record.terraformerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Lumberjack L" + lumberjackLevel + " | actions " + record.lumberjackActions() + "/"
                        + RealCivConfig.lumberjackLimitForLevel(lumberjackLevel)
                        + " | XP " + record.lumberjackXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Fisher L" + fisherLevel + " | catches " + record.fisherActions() + "/"
                        + RealCivConfig.fisherLimitForLevel(fisherLevel)
                        + " | XP " + record.fisherXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Hunter L" + hunterLevel + " | kills " + record.hunterActions() + "/"
                        + RealCivConfig.hunterLimitForLevel(hunterLevel)
                        + " | XP " + record.hunterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Warrior L" + warriorLevel + " | player kills " + record.warriorActions() + "/"
                        + RealCivConfig.warriorLimitForLevel(warriorLevel)
                        + " | XP " + record.warriorXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Explosives Expert L" + explosivesLevel + " | actions " + record.explosivesExpertActions() + "/"
                        + RealCivConfig.explosivesExpertLimitForLevel(explosivesLevel)
                        + " | XP " + record.explosivesExpertXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Crafter L" + crafterLevel + " | crafted " + record.crafterActions() + "/"
                        + RealCivConfig.crafterLimitForLevel(crafterLevel)
                        + " | XP " + record.crafterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Enchanter L" + enchanterLevel + " | actions " + record.enchanterActions() + "/"
                        + RealCivConfig.enchanterLimitForLevel(enchanterLevel)
                        + " | XP " + record.enchanterXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Brewer L" + brewerLevel + " | actions " + record.brewerActions() + "/"
                        + RealCivConfig.brewerLimitForLevel(brewerLevel)
                        + " | XP " + record.brewerXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Trader L" + traderLevel + " | actions " + record.traderActions() + "/"
                        + RealCivConfig.traderLimitForLevel(traderLevel)
                        + " | XP " + record.traderXp()), false);
        source.sendSuccess(() -> Component.literal(
                "Hub personal withdrawal rate: "
                        + RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId))
                        + (record.personalWithdrawRatioOverride(civId) == null ? " (default)" : " (mayor override)")),
                false);
        return 1;
    }
}