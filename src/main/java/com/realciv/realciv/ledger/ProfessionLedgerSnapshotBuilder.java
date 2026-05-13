package com.realciv.realciv.ledger;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ProfessionLedgerSnapshotBuilder {
    private static final Profession[] ORDERED_PROFESSIONS = new Profession[] {
            Profession.FARMER,
            Profession.MINER,
            Profession.TERRAFORMER,
            Profession.LUMBERJACK,
            Profession.FISHER,
            Profession.HUNTER,
            Profession.WARRIOR,
            Profession.EXPLOSIVES_EXPERT,
            Profession.CRAFTER,
            Profession.ENCHANTER,
            Profession.BREWER,
            Profession.TRADER
    };

    private ProfessionLedgerSnapshotBuilder() {
    }

    public static ProfessionLedgerSnapshot build(ServerPlayer viewer, CivSavedData data, String civilizationId) {
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civilizationId);
        String civName = civ == null ? civilizationId : civ.displayName();
        Profession top = RealCivUtil.topProfession(record);
        List<ProfessionLedgerSnapshot.ProfessionRow> rows = new ArrayList<>();
        for (Profession profession : ORDERED_PROFESSIONS) {
            int level = record.levelFor(profession);
            int xp = RealCivUtil.xpForProfession(record, profession);
            int actions = record.actionsForProfession(profession);
            int limit = actionLimitForProfession(record, profession);
            String actionText = limit <= 0 ? "actions " + actions : "actions " + actions + "/" + limit;
            rows.add(new ProfessionLedgerSnapshot.ProfessionRow(
                    RealCivUtil.displayProfession(profession), level, xp, actionText));
        }
        return new ProfessionLedgerSnapshot(
                civName,
                viewer.getGameProfile().getName(),
                record.generalLevel(),
                record.generalXp(),
                record.socialCreditCents(civilizationId),
                "Top: " + RealCivUtil.displayProfession(top) + " (Lv " + record.levelFor(top) + ")",
                "Focus: " + RealCivUtil.displayProfession(record.focusedProfession()),
                record.pendingWarriorHubRegistrations(),
                record.contributions(civilizationId).size(),
                data.civilizationMembersSorted(civilizationId).size(),
                List.copyOf(rows));
    }

    private static int actionLimitForProfession(CivSavedData.PlayerRecord record, Profession profession) {
        int level = record.levelFor(profession);
        return switch (profession) {
            case FARMER -> RealCivConfig.farmerLimitForLevel(level);
            case MINER -> RealCivConfig.minerLimitForLevel(level);
            case TERRAFORMER -> RealCivConfig.terraformerLimitForLevel(level);
            case LUMBERJACK -> RealCivConfig.lumberjackLimitForLevel(level);
            case FISHER -> RealCivConfig.fisherLimitForLevel(level);
            case HUNTER -> RealCivConfig.hunterLimitForLevel(level);
            case WARRIOR -> RealCivConfig.warriorLimitForLevel(level);
            case EXPLOSIVES_EXPERT -> RealCivConfig.explosivesExpertLimitForLevel(level);
            case CRAFTER -> RealCivConfig.crafterLimitForLevel(level);
            case ENCHANTER -> RealCivConfig.enchanterLimitForLevel(level);
            case BREWER -> RealCivConfig.brewerLimitForLevel(level);
            case TRADER -> RealCivConfig.traderLimitForLevel(level);
            case SHEPHERD -> RealCivConfig.shepherdLimitForLevel(level);
            case EXPLORER -> RealCivConfig.explorerLimitForLevel(level);
            case TREASURE_HUNTER -> RealCivConfig.treasureHunterLimitForLevel(level);
            case BREEDER -> RealCivConfig.breederLimitForLevel(level);
            case SMITHY -> RealCivConfig.smithyLimitForLevel(level);
            case SMELTER -> RealCivConfig.smelterLimitForLevel(level);
            case NONE -> 0;
        };
    }
}
