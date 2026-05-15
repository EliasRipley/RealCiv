package com.realciv.realciv.data;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.logic.Profession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class PlayerRecord {

        private static final long DAY_MILLIS = 86_400_000L;
        private long socialCreditCents;
        private int farmerActions;
        private int minerActions;
        private long farmerActionsUpdatedAtMillis;
        private long minerActionsUpdatedAtMillis;
        private int farmerXp;
        private int minerXp;
        private final Map<String, Integer> minerBlockActions = new HashMap<>();
        private int terraformerActions;
        private long terraformerActionsUpdatedAtMillis;
        private int terraformerXp;
        private int lumberjackActions;
        private long lumberjackActionsUpdatedAtMillis;
        private int lumberjackXp;
        private int fisherActions;
        private long fisherActionsUpdatedAtMillis;
        private int fisherXp;
        private int hunterActions;
        private long hunterActionsUpdatedAtMillis;
        private int hunterXp;
        private int warriorActions;
        private long warriorActionsUpdatedAtMillis;
        private int warriorXp;
        private int pendingWarriorHubRegistrations;
        private int explosivesExpertActions;
        private long explosivesExpertActionsUpdatedAtMillis;
        private int explosivesExpertXp;
        private int crafterActions;
        private long crafterActionsUpdatedAtMillis;
        private int crafterXp;
        private final Map<String, Integer> crafterItemActions = new HashMap<>();
        private long crafterDailyItemActionDayIndex = -1L;
        private final Map<String, Integer> crafterDailyItemActions = new HashMap<>();
        private int enchanterActions;
        private long enchanterActionsUpdatedAtMillis;
        private int enchanterXp;
        private int brewerActions;
        private long brewerActionsUpdatedAtMillis;
        private int brewerXp;
        private int traderActions;
        private long traderActionsUpdatedAtMillis;
        private int traderXp;
        private int shepherdActions;
        private long shepherdActionsUpdatedAtMillis;
        private int shepherdXp;
        private int explorerActions;
        private long explorerActionsUpdatedAtMillis;
        private int explorerXp;
        private int treasureHunterActions;
        private long treasureHunterActionsUpdatedAtMillis;
        private int treasureHunterXp;
        private int breederActions;
        private long breederActionsUpdatedAtMillis;
        private int breederXp;
        private int smithyActions;
        private long smithyActionsUpdatedAtMillis;
        private int smithyXp;
        private int smelterActions;
        private long smelterActionsUpdatedAtMillis;
        private int smelterXp;
        private int generalXp;
        private long firstSeenAtMillis;
        private final Map<String, HookWindowUsage> hookWindowUsage = new HashMap<>();
        private final Map<String, Integer> hunterMobActions = new HashMap<>();
        private long professionDailyActionDayIndex = -1L;
        private final Map<String, Integer> professionDailyActions = new HashMap<>();
        private long minerDailyBlockActionDayIndex = -1L;
        private final Map<String, Integer> minerDailyBlockActions = new HashMap<>();
        private final Map<String, Integer> lumberjackBlockActions = new HashMap<>();
        private long lumberjackDailyBlockActionDayIndex = -1L;
        private final Map<String, Integer> lumberjackDailyBlockActions = new HashMap<>();
        private final Map<String, Integer> terraformerBlockActions = new HashMap<>();
        private long terraformerDailyBlockActionDayIndex = -1L;
        private final Map<String, Integer> terraformerDailyBlockActions = new HashMap<>();
        private long professionProgressDayIndex = -1L;
        private int generalLevelsGainedToday;
        private final Map<String, Integer> professionLevelsGainedToday = new HashMap<>();
        private long contributionGainDayIndex = -1L;
        private final Map<String, Long> contributionGainTodayCents = new HashMap<>();
        @Nullable
        private Profession focusedProfession;
        private final Map<String, CivAccount> civAccounts = new HashMap<>();

        // Legacy fields retained for migration.
        private final Map<String, Long> contributions = new HashMap<>();
        private final Map<String, Long> personalWithdrawals = new HashMap<>();
        @Nullable
        private Double personalWithdrawRatioOverride;
        @Nullable
        private String ftbClaimModeOverride;

        private CivAccount account(String civIdRaw) {
            String civId = CivSavedData.normalizeCivId(civIdRaw);
            if (civId == null) {
                civId = RealCivConfig.defaultCivilizationId();
            }
            return civAccounts.computeIfAbsent(civId, ignored -> new CivAccount());
        }

        public boolean migrateCivAccount(String fromCivRaw, String toCivRaw) {
            String from = CivSavedData.normalizeCivId(fromCivRaw);
            String to = CivSavedData.normalizeCivId(toCivRaw);
            if (from == null || to == null || from.equals(to)) {
                return false;
            }
            CivAccount fromAccount = civAccounts.remove(from);
            if (fromAccount == null) {
                return false;
            }

            CivAccount target = account(to);
            target.socialCreditCents = Math.max(0L, target.socialCreditCents + Math.max(0L, fromAccount.socialCreditCents));
            for (Map.Entry<String, Long> entry : fromAccount.contributions.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value > 0L) {
                    target.contributions.merge(entry.getKey(), value, Long::sum);
                }
            }
            for (Map.Entry<String, Long> entry : fromAccount.personalWithdrawals.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value > 0L) {
                    target.personalWithdrawals.merge(entry.getKey(), value, Long::sum);
                }
            }
            if (target.personalWithdrawRatioOverride == null && fromAccount.personalWithdrawRatioOverride != null) {
                target.personalWithdrawRatioOverride = clampRatio(fromAccount.personalWithdrawRatioOverride);
            }
            if (target.dailyAllowanceDayIndex == fromAccount.dailyAllowanceDayIndex) {
                for (Map.Entry<String, Long> entry : fromAccount.dailyAllowanceWithdrawals.entrySet()) {
                    long value = Math.max(0L, entry.getValue());
                    if (value > 0L) {
                        target.dailyAllowanceWithdrawals.merge(entry.getKey(), value, Long::sum);
                    }
                }
            } else if (fromAccount.dailyAllowanceDayIndex > target.dailyAllowanceDayIndex) {
                target.dailyAllowanceDayIndex = fromAccount.dailyAllowanceDayIndex;
                target.dailyAllowanceWithdrawals.clear();
                for (Map.Entry<String, Long> entry : fromAccount.dailyAllowanceWithdrawals.entrySet()) {
                    long value = Math.max(0L, entry.getValue());
                    if (value > 0L) {
                        target.dailyAllowanceWithdrawals.put(entry.getKey(), value);
                    }
                }
            }
            return true;
        }

        public void migrateLegacyAccount(String civId) {
            CivAccount account = account(civId);
            if (account.socialCreditCents <= 0L && socialCreditCents > 0L) {
                account.socialCreditCents = socialCreditCents;
            }
            if (account.contributions.isEmpty() && !contributions.isEmpty()) {
                account.contributions.putAll(contributions);
            }
            if (account.personalWithdrawals.isEmpty() && !personalWithdrawals.isEmpty()) {
                account.personalWithdrawals.putAll(personalWithdrawals);
            }
            if (account.personalWithdrawRatioOverride == null && personalWithdrawRatioOverride != null) {
                account.personalWithdrawRatioOverride = clampRatio(personalWithdrawRatioOverride);
            }
        }

        public long socialCreditCents(String civId) {
            return account(civId).socialCreditCents;
        }

        public void setSocialCreditCents(String civId, long cents) {
            account(civId).socialCreditCents = Math.max(0L, cents);
        }

        public long addSocialCreditCents(String civId, long delta) {
            if (delta == 0L) {
                return 0L;
            }
            if (delta < 0L) {
                long before = socialCreditCents(civId);
                setSocialCreditCents(civId, socialCreditCents(civId) + delta);
                long after = socialCreditCents(civId);
                return after - before;
            }
            long allowed = clampContributionGainForToday(civId, delta);
            if (allowed <= 0L) {
                return 0L;
            }
            long before = socialCreditCents(civId);
            setSocialCreditCents(civId, socialCreditCents(civId) + allowed);
            long after = socialCreditCents(civId);
            return after - before;
        }

        public int farmerActions() {
            return farmerActions;
        }

        public void setFarmerActions(int value) {
            int updated = Math.max(0, value);
            if (this.farmerActions != updated) {
                this.farmerActions = updated;
                this.farmerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public int minerActions() {
            return minerActions;
        }

        public void setMinerActions(int value) {
            int updated = Math.max(0, value);
            if (this.minerActions != updated) {
                if (updated <= 0) {
                    minerBlockActions.clear();
                }
                this.minerActions = updated;
                this.minerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public void clearMinerBlockWindow() {
            minerBlockActions.clear();
        }

        public int minerBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            return Math.max(0, minerBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addMinerBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            String key = blockId.toString();
            int current = Math.max(0, minerBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            minerBlockActions.put(key, (int) next);
        }

        public int minerDailyBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            resetMinerDailyBlockActionWindowIfNeeded();
            return Math.max(0, minerDailyBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addMinerDailyBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            resetMinerDailyBlockActionWindowIfNeeded();
            String key = blockId.toString();
            int current = Math.max(0, minerDailyBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            minerDailyBlockActions.put(key, (int) next);
        }

        public void clearLumberjackBlockWindow() {
            lumberjackBlockActions.clear();
        }

        public int lumberjackBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            return Math.max(0, lumberjackBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addLumberjackBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            String key = blockId.toString();
            int current = Math.max(0, lumberjackBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            lumberjackBlockActions.put(key, (int) next);
        }

        public int lumberjackDailyBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            resetLumberjackDailyBlockActionWindowIfNeeded();
            return Math.max(0, lumberjackDailyBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addLumberjackDailyBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            resetLumberjackDailyBlockActionWindowIfNeeded();
            String key = blockId.toString();
            int current = Math.max(0, lumberjackDailyBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            lumberjackDailyBlockActions.put(key, (int) next);
        }

        public void clearTerraformerBlockWindow() {
            terraformerBlockActions.clear();
        }

        public int terraformerBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            return Math.max(0, terraformerBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addTerraformerBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            String key = blockId.toString();
            int current = Math.max(0, terraformerBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            terraformerBlockActions.put(key, (int) next);
        }

        public int terraformerDailyBlockActions(ResourceLocation blockId) {
            if (blockId == null) {
                return 0;
            }
            resetTerraformerDailyBlockActionWindowIfNeeded();
            return Math.max(0, terraformerDailyBlockActions.getOrDefault(blockId.toString(), 0));
        }

        public void addTerraformerDailyBlockActions(ResourceLocation blockId, int delta) {
            if (blockId == null || delta <= 0) {
                return;
            }
            resetTerraformerDailyBlockActionWindowIfNeeded();
            String key = blockId.toString();
            int current = Math.max(0, terraformerDailyBlockActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            terraformerDailyBlockActions.put(key, (int) next);
        }

        public void clearCrafterItemWindow() {
            crafterItemActions.clear();
        }

        public int crafterItemActions(ResourceLocation itemId) {
            if (itemId == null) {
                return 0;
            }
            return Math.max(0, crafterItemActions.getOrDefault(itemId.toString(), 0));
        }

        public void addCrafterItemActions(ResourceLocation itemId, int delta) {
            if (itemId == null || delta <= 0) {
                return;
            }
            String key = itemId.toString();
            int current = Math.max(0, crafterItemActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            crafterItemActions.put(key, (int) next);
        }

        public int crafterDailyItemActions(ResourceLocation itemId) {
            if (itemId == null) {
                return 0;
            }
            resetCrafterDailyItemActionWindowIfNeeded();
            return Math.max(0, crafterDailyItemActions.getOrDefault(itemId.toString(), 0));
        }

        public void addCrafterDailyItemActions(ResourceLocation itemId, int delta) {
            if (itemId == null || delta <= 0) {
                return;
            }
            resetCrafterDailyItemActionWindowIfNeeded();
            String key = itemId.toString();
            int current = Math.max(0, crafterDailyItemActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            crafterDailyItemActions.put(key, (int) next);
        }

        public int dailyProfessionActionsUsed(Profession profession) {
            if (profession == null || profession == Profession.NONE) {
                return 0;
            }
            resetDailyProfessionActionWindowIfNeeded();
            return Math.max(0, professionDailyActions.getOrDefault(profession.name(), 0));
        }

        public void addDailyProfessionActions(Profession profession, int delta) {
            if (profession == null || profession == Profession.NONE || delta <= 0) {
                return;
            }
            resetDailyProfessionActionWindowIfNeeded();
            String key = profession.name();
            int current = Math.max(0, professionDailyActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            professionDailyActions.put(key, (int) next);
        }

        public long farmerActionsUpdatedAtMillis() {
            return farmerActionsUpdatedAtMillis;
        }

        public long minerActionsUpdatedAtMillis() {
            return minerActionsUpdatedAtMillis;
        }

        public int farmerXp() {
            return farmerXp;
        }

        public int minerXp() {
            return minerXp;
        }

        public int terraformerActions() {
            return terraformerActions;
        }

        public void setTerraformerActions(int value) {
            int updated = Math.max(0, value);
            if (this.terraformerActions != updated) {
                this.terraformerActions = updated;
                this.terraformerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long terraformerActionsUpdatedAtMillis() {
            return terraformerActionsUpdatedAtMillis;
        }

        public int terraformerXp() {
            return terraformerXp;
        }

        public int lumberjackActions() {
            return lumberjackActions;
        }

        public void setLumberjackActions(int value) {
            int updated = Math.max(0, value);
            if (this.lumberjackActions != updated) {
                this.lumberjackActions = updated;
                this.lumberjackActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long lumberjackActionsUpdatedAtMillis() {
            return lumberjackActionsUpdatedAtMillis;
        }

        public int lumberjackXp() {
            return lumberjackXp;
        }

        public int fisherActions() {
            return fisherActions;
        }

        public void setFisherActions(int value) {
            int updated = Math.max(0, value);
            if (this.fisherActions != updated) {
                this.fisherActions = updated;
                this.fisherActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long fisherActionsUpdatedAtMillis() {
            return fisherActionsUpdatedAtMillis;
        }

        public int fisherXp() {
            return fisherXp;
        }

        public int hunterActions() {
            return hunterActions;
        }

        public void setHunterActions(int value) {
            int updated = Math.max(0, value);
            if (this.hunterActions != updated) {
                if (updated < this.hunterActions) {
                    trimHunterMobActions(this.hunterActions - updated);
                }
                this.hunterActions = updated;
                this.hunterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long hunterActionsUpdatedAtMillis() {
            return hunterActionsUpdatedAtMillis;
        }

        public int hunterXp() {
            return hunterXp;
        }

        public int hunterMobActions(ResourceLocation entityId) {
            if (entityId == null) {
                return 0;
            }
            return Math.max(0, hunterMobActions.getOrDefault(entityId.toString(), 0));
        }

        public void addHunterMobActions(ResourceLocation entityId, int delta) {
            if (entityId == null || delta <= 0) {
                return;
            }
            String key = entityId.toString();
            int current = Math.max(0, hunterMobActions.getOrDefault(key, 0));
            long next = (long) current + delta;
            if (next > Integer.MAX_VALUE) {
                next = Integer.MAX_VALUE;
            }
            hunterMobActions.put(key, (int) next);
        }

        public void restoreHunterActions(int restoredActions) {
            int refund = Math.min(Math.max(0, restoredActions), hunterActions);
            if (refund <= 0) {
                return;
            }
            setHunterActions(hunterActions - refund);
        }

        private void trimHunterMobActions(int reduction) {
            if (reduction <= 0 || hunterMobActions.isEmpty()) {
                return;
            }
            // Keep per-mob counters in sync with the generic hunter action pool.
            int remaining = reduction;
            List<String> keys = new ArrayList<>(hunterMobActions.keySet());
            keys.sort(String::compareTo);
            for (String key : keys) {
                if (remaining <= 0) {
                    break;
                }
                int used = Math.max(0, hunterMobActions.getOrDefault(key, 0));
                if (used <= 0) {
                    hunterMobActions.remove(key);
                    continue;
                }
                int reduce = Math.min(used, remaining);
                int next = used - reduce;
                if (next <= 0) {
                    hunterMobActions.remove(key);
                } else {
                    hunterMobActions.put(key, next);
                }
                remaining -= reduce;
            }
        }

        public int warriorActions() {
            return warriorActions;
        }

        public void setWarriorActions(int value) {
            int updated = Math.max(0, value);
            if (this.warriorActions != updated) {
                this.warriorActions = updated;
                this.warriorActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long warriorActionsUpdatedAtMillis() {
            return warriorActionsUpdatedAtMillis;
        }

        public int warriorXp() {
            return warriorXp;
        }

        public int pendingWarriorHubRegistrations() {
            return Math.max(0, pendingWarriorHubRegistrations);
        }

        public void addPendingWarriorHubRegistrations(int delta) {
            if (delta <= 0) {
                return;
            }
            pendingWarriorHubRegistrations = Math.max(0, pendingWarriorHubRegistrations + delta);
        }

        public void clearPendingWarriorHubRegistrations() {
            pendingWarriorHubRegistrations = 0;
        }

        public void addWarriorXp(int delta) {
            addProfessionXp(Profession.WARRIOR, delta);
        }

        public int explosivesExpertActions() {
            return explosivesExpertActions;
        }

        public void setExplosivesExpertActions(int value) {
            int updated = Math.max(0, value);
            if (this.explosivesExpertActions != updated) {
                this.explosivesExpertActions = updated;
                this.explosivesExpertActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long explosivesExpertActionsUpdatedAtMillis() {
            return explosivesExpertActionsUpdatedAtMillis;
        }

        public int explosivesExpertXp() {
            return explosivesExpertXp;
        }

        public void addExplosivesExpertXp(int delta) {
            addProfessionXp(Profession.EXPLOSIVES_EXPERT, delta);
        }

        public int crafterActions() {
            return crafterActions;
        }

        public void setCrafterActions(int value) {
            int updated = Math.max(0, value);
            if (this.crafterActions != updated) {
                if (updated <= 0) {
                    crafterItemActions.clear();
                }
                this.crafterActions = updated;
                this.crafterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long crafterActionsUpdatedAtMillis() {
            return crafterActionsUpdatedAtMillis;
        }

        public int crafterXp() {
            return crafterXp;
        }

        public int enchanterActions() {
            return enchanterActions;
        }

        public void setEnchanterActions(int value) {
            int updated = Math.max(0, value);
            if (this.enchanterActions != updated) {
                this.enchanterActions = updated;
                this.enchanterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long enchanterActionsUpdatedAtMillis() {
            return enchanterActionsUpdatedAtMillis;
        }

        public int enchanterXp() {
            return enchanterXp;
        }

        public int brewerActions() {
            return brewerActions;
        }

        public void setBrewerActions(int value) {
            int updated = Math.max(0, value);
            if (this.brewerActions != updated) {
                this.brewerActions = updated;
                this.brewerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long brewerActionsUpdatedAtMillis() {
            return brewerActionsUpdatedAtMillis;
        }

        public int brewerXp() {
            return brewerXp;
        }

        public int traderActions() {
            return traderActions;
        }

        public void setTraderActions(int value) {
            int updated = Math.max(0, value);
            if (this.traderActions != updated) {
                this.traderActions = updated;
                this.traderActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long traderActionsUpdatedAtMillis() {
            return traderActionsUpdatedAtMillis;
        }

        public int traderXp() {
            return traderXp;
        }

        public int shepherdActions() {
            return shepherdActions;
        }

        public void setShepherdActions(int value) {
            int updated = Math.max(0, value);
            if (this.shepherdActions != updated) {
                this.shepherdActions = updated;
                this.shepherdActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long shepherdActionsUpdatedAtMillis() {
            return shepherdActionsUpdatedAtMillis;
        }

        public int shepherdXp() {
            return shepherdXp;
        }

        public int explorerActions() {
            return explorerActions;
        }

        public void setExplorerActions(int value) {
            int updated = Math.max(0, value);
            if (this.explorerActions != updated) {
                this.explorerActions = updated;
                this.explorerActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long explorerActionsUpdatedAtMillis() {
            return explorerActionsUpdatedAtMillis;
        }

        public int explorerXp() {
            return explorerXp;
        }

        public int treasureHunterActions() {
            return treasureHunterActions;
        }

        public void setTreasureHunterActions(int value) {
            int updated = Math.max(0, value);
            if (this.treasureHunterActions != updated) {
                this.treasureHunterActions = updated;
                this.treasureHunterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long treasureHunterActionsUpdatedAtMillis() {
            return treasureHunterActionsUpdatedAtMillis;
        }

        public int treasureHunterXp() {
            return treasureHunterXp;
        }

        public int breederActions() {
            return breederActions;
        }

        public void setBreederActions(int value) {
            int updated = Math.max(0, value);
            if (this.breederActions != updated) {
                this.breederActions = updated;
                this.breederActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long breederActionsUpdatedAtMillis() {
            return breederActionsUpdatedAtMillis;
        }

        public int breederXp() {
            return breederXp;
        }

        public int smithyActions() {
            return smithyActions;
        }

        public void setSmithyActions(int value) {
            int updated = Math.max(0, value);
            if (this.smithyActions != updated) {
                this.smithyActions = updated;
                this.smithyActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long smithyActionsUpdatedAtMillis() {
            return smithyActionsUpdatedAtMillis;
        }

        public int smithyXp() {
            return smithyXp;
        }

        public int smelterActions() {
            return smelterActions;
        }

        public void setSmelterActions(int value) {
            int updated = Math.max(0, value);
            if (this.smelterActions != updated) {
                this.smelterActions = updated;
                this.smelterActionsUpdatedAtMillis = System.currentTimeMillis();
            }
        }

        public long smelterActionsUpdatedAtMillis() {
            return smelterActionsUpdatedAtMillis;
        }

        public int smelterXp() {
            return smelterXp;
        }

        public int actionsForProfession(Profession profession) {
            if (profession == null) {
                return 0;
            }
            return switch (profession) {
                case FARMER -> farmerActions;
                case MINER -> minerActions;
                case TERRAFORMER -> terraformerActions;
                case LUMBERJACK -> lumberjackActions;
                case FISHER -> fisherActions;
                case HUNTER -> hunterActions;
                case WARRIOR -> warriorActions;
                case EXPLOSIVES_EXPERT -> explosivesExpertActions;
                case CRAFTER -> crafterActions;
                case ENCHANTER -> enchanterActions;
                case BREWER -> brewerActions;
                case TRADER -> traderActions;
                case SHEPHERD -> shepherdActions;
                case EXPLORER -> explorerActions;
                case TREASURE_HUNTER -> treasureHunterActions;
                case BREEDER -> breederActions;
                case SMITHY -> smithyActions;
                case SMELTER -> smelterActions;
                case NONE -> 0;
            };
        }

        public void setActionsForProfession(Profession profession, int value) {
            if (profession == null) {
                return;
            }
            switch (profession) {
                case FARMER -> setFarmerActions(value);
                case MINER -> setMinerActions(value);
                case TERRAFORMER -> setTerraformerActions(value);
                case LUMBERJACK -> setLumberjackActions(value);
                case FISHER -> setFisherActions(value);
                case HUNTER -> setHunterActions(value);
                case WARRIOR -> setWarriorActions(value);
                case EXPLOSIVES_EXPERT -> setExplosivesExpertActions(value);
                case CRAFTER -> setCrafterActions(value);
                case ENCHANTER -> setEnchanterActions(value);
                case BREWER -> setBrewerActions(value);
                case TRADER -> setTraderActions(value);
                case SHEPHERD -> setShepherdActions(value);
                case EXPLORER -> setExplorerActions(value);
                case TREASURE_HUNTER -> setTreasureHunterActions(value);
                case BREEDER -> setBreederActions(value);
                case SMITHY -> setSmithyActions(value);
                case SMELTER -> setSmelterActions(value);
                case NONE -> {
                }
            }
        }

        public long actionsUpdatedAtMillisForProfession(Profession profession) {
            if (profession == null) {
                return 0L;
            }
            return switch (profession) {
                case FARMER -> farmerActionsUpdatedAtMillis;
                case MINER -> minerActionsUpdatedAtMillis;
                case TERRAFORMER -> terraformerActionsUpdatedAtMillis;
                case LUMBERJACK -> lumberjackActionsUpdatedAtMillis;
                case FISHER -> fisherActionsUpdatedAtMillis;
                case HUNTER -> hunterActionsUpdatedAtMillis;
                case WARRIOR -> warriorActionsUpdatedAtMillis;
                case EXPLOSIVES_EXPERT -> explosivesExpertActionsUpdatedAtMillis;
                case CRAFTER -> crafterActionsUpdatedAtMillis;
                case ENCHANTER -> enchanterActionsUpdatedAtMillis;
                case BREWER -> brewerActionsUpdatedAtMillis;
                case TRADER -> traderActionsUpdatedAtMillis;
                case SHEPHERD -> shepherdActionsUpdatedAtMillis;
                case EXPLORER -> explorerActionsUpdatedAtMillis;
                case TREASURE_HUNTER -> treasureHunterActionsUpdatedAtMillis;
                case BREEDER -> breederActionsUpdatedAtMillis;
                case SMITHY -> smithyActionsUpdatedAtMillis;
                case SMELTER -> smelterActionsUpdatedAtMillis;
                case NONE -> 0L;
            };
        }

        public int generalXp() {
            return generalXp;
        }

        public void ensureFirstSeenAtMillis(long nowMillis) {
            if (firstSeenAtMillis > 0L) {
                return;
            }
            if (nowMillis <= 0L) {
                return;
            }
            firstSeenAtMillis = nowMillis;
        }

        public long firstSeenAtMillis() {
            return firstSeenAtMillis;
        }

        public long membershipMillis(long nowMillis) {
            if (nowMillis <= 0L || firstSeenAtMillis <= 0L || nowMillis < firstSeenAtMillis) {
                return 0L;
            }
            return nowMillis - firstSeenAtMillis;
        }

        public int hookWindowUsed(String key, long windowStartMillis) {
            if (key == null || key.isBlank()) {
                return 0;
            }
            HookWindowUsage usage = hookWindowUsage.get(key);
            if (usage == null || usage.windowStartMillis != windowStartMillis) {
                return 0;
            }
            return Math.max(0, usage.usedTriggers);
        }

        public void setHookWindowUsed(String key, long windowStartMillis, int usedTriggers) {
            if (key == null || key.isBlank()) {
                return;
            }
            int safeUsed = Math.max(0, usedTriggers);
            if (safeUsed <= 0) {
                hookWindowUsage.remove(key);
                return;
            }
            HookWindowUsage usage = hookWindowUsage.computeIfAbsent(key, ignored -> new HookWindowUsage());
            usage.windowStartMillis = Math.max(0L, windowStartMillis);
            usage.usedTriggers = safeUsed;
        }

        @Nullable
        public Profession focusedProfession() {
            return focusedProfession;
        }

        public void setFocusedProfession(@Nullable Profession profession) {
            if (profession == Profession.NONE) {
                profession = null;
            }
            this.focusedProfession = profession;
        }

        public boolean canProgressProfession(Profession profession) {
            if (profession == Profession.NONE || !RealCivConfig.specializationSingleProfessionLockEnabled()) {
                return true;
            }
            return focusedProfession != null && focusedProfession == profession;
        }

        public void addProfessionXp(Profession profession, int delta) {
            if (profession == null || profession == Profession.NONE || delta <= 0) {
                return;
            }
            if (RealCivConfig.specializationSingleProfessionLockEnabled()
                    && (focusedProfession == null || focusedProfession != profession)) {
                return;
            }
            int startXp = professionXpValue(profession);
            int startLevel = RealCivConfig.professionLevelFromXp(profession, startXp);
            int maxLevelCap = RealCivConfig.professionLevelCap(profession);
            if (startLevel >= maxLevelCap) {
                return;
            }

            int cappedTargetXp = capProfessionXpByDailyLevelGain(profession, startXp, delta, startLevel, maxLevelCap);
            int appliedDelta = Math.max(0, cappedTargetXp - startXp);
            if (appliedDelta <= 0) {
                return;
            }

            setProfessionXpValue(profession, cappedTargetXp);
            applyProfessionXpDecay(profession, appliedDelta);
            int endLevel = RealCivConfig.professionLevelFromXp(profession, professionXpValue(profession));
            recordProfessionLevelGain(profession, Math.max(0, endLevel - startLevel));
        }

        public void addGeneralXp(int delta) {
            if (delta <= 0) {
                return;
            }
            int startXp = generalXp;
            int startLevel = RealCivConfig.generalLevelFromXp(startXp);
            int targetXp = capGeneralXpByDailyLevelGain(startXp, delta, startLevel);
            if (targetXp <= startXp) {
                return;
            }
            generalXp = Math.max(0, targetXp);
            int endLevel = RealCivConfig.generalLevelFromXp(generalXp);
            recordGeneralLevelGain(Math.max(0, endLevel - startLevel));
        }

        private int capProfessionXpByDailyLevelGain(
                Profession profession,
                int startXp,
                int delta,
                int startLevel,
                int maxLevelCap) {
            int targetXp = safeAddPositive(startXp, delta);
            int absoluteMaxXp = maxXpForProfessionLevel(maxLevelCap);
            if (targetXp > absoluteMaxXp) {
                targetXp = absoluteMaxXp;
            }

            int maxDailyLevels = RealCivConfig.maxProfessionLevelGainsPerDay();
            if (maxDailyLevels <= 0) {
                return targetXp;
            }

            rotateProfessionProgressDay();
            String key = profession.name();
            int used = Math.max(0, professionLevelsGainedToday.getOrDefault(key, 0));
            int remaining = Math.max(0, maxDailyLevels - used);
            if (remaining <= 0) {
                return startXp;
            }

            int allowedLevel = Math.min(maxLevelCap, safeAddPositive(startLevel, remaining));
            int maxByDailyLimit = maxXpForProfessionLevel(allowedLevel);
            return Math.min(targetXp, maxByDailyLimit);
        }

        private int capGeneralXpByDailyLevelGain(int startXp, int delta, int startLevel) {
            int targetXp = safeAddPositive(startXp, delta);
            int maxDailyLevels = RealCivConfig.maxGeneralLevelGainsPerDay();
            if (maxDailyLevels <= 0) {
                return targetXp;
            }
            rotateProfessionProgressDay();
            int used = Math.max(0, generalLevelsGainedToday);
            int remaining = Math.max(0, maxDailyLevels - used);
            if (remaining <= 0) {
                return startXp;
            }
            int allowedLevel = safeAddPositive(startLevel, remaining);
            int maxByDailyLimit = maxXpForGeneralLevel(allowedLevel);
            return Math.min(targetXp, maxByDailyLimit);
        }

        private long clampContributionGainForToday(String civIdRaw, long delta) {
            if (delta <= 0L) {
                return 0L;
            }
            long dailyCap = RealCivConfig.maxContributionKarmaGainPerDayCents();
            if (dailyCap <= 0L) {
                return delta;
            }
            rotateContributionGainDay();
            @Nullable String normalized = CivSavedData.normalizeCivId(civIdRaw);
            String civId = normalized == null ? RealCivConfig.defaultCivilizationId() : normalized;
            long used = Math.max(0L, contributionGainTodayCents.getOrDefault(civId, 0L));
            long remaining = Math.max(0L, dailyCap - used);
            if (remaining <= 0L) {
                return 0L;
            }
            long allowed = Math.min(delta, remaining);
            if (allowed <= 0L) {
                return 0L;
            }
            contributionGainTodayCents.put(civId, used + allowed);
            return allowed;
        }

        private void recordProfessionLevelGain(Profession profession, int gained) {
            if (gained <= 0 || profession == null || profession == Profession.NONE) {
                return;
            }
            rotateProfessionProgressDay();
            String key = profession.name();
            int used = Math.max(0, professionLevelsGainedToday.getOrDefault(key, 0));
            professionLevelsGainedToday.put(key, safeAddPositive(used, gained));
        }

        private void recordGeneralLevelGain(int gained) {
            if (gained <= 0) {
                return;
            }
            rotateProfessionProgressDay();
            generalLevelsGainedToday = safeAddPositive(generalLevelsGainedToday, gained);
        }

        private void rotateProfessionProgressDay() {
            long nowDay = currentDayIndex();
            if (professionProgressDayIndex == nowDay) {
                return;
            }
            professionProgressDayIndex = nowDay;
            generalLevelsGainedToday = 0;
            professionLevelsGainedToday.clear();
        }

        private void rotateContributionGainDay() {
            long nowDay = currentDayIndex();
            if (contributionGainDayIndex == nowDay) {
                return;
            }
            contributionGainDayIndex = nowDay;
            contributionGainTodayCents.clear();
        }

        private void resetDailyProfessionActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (professionDailyActionDayIndex == dayIndex) {
                return;
            }
            professionDailyActionDayIndex = dayIndex;
            professionDailyActions.clear();
        }

        private void resetMinerDailyBlockActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (minerDailyBlockActionDayIndex == dayIndex) {
                return;
            }
            minerDailyBlockActionDayIndex = dayIndex;
            minerDailyBlockActions.clear();
        }

        private void resetLumberjackDailyBlockActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (lumberjackDailyBlockActionDayIndex == dayIndex) {
                return;
            }
            lumberjackDailyBlockActionDayIndex = dayIndex;
            lumberjackDailyBlockActions.clear();
        }

        private void resetTerraformerDailyBlockActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (terraformerDailyBlockActionDayIndex == dayIndex) {
                return;
            }
            terraformerDailyBlockActionDayIndex = dayIndex;
            terraformerDailyBlockActions.clear();
        }

        private void resetCrafterDailyItemActionWindowIfNeeded() {
            long dayIndex = currentUtcDayIndex();
            if (crafterDailyItemActionDayIndex == dayIndex) {
                return;
            }
            crafterDailyItemActionDayIndex = dayIndex;
            crafterDailyItemActions.clear();
        }

        private int maxXpForProfessionLevel(int level) {
            if (level < 0) {
                return 0;
            }
            List<? extends Integer> thresholds = RealCivConfig.PROFESSION_XP_THRESHOLDS.get();
            if (thresholds.isEmpty()) {
                return Integer.MAX_VALUE;
            }
            int nextIndex = safeAddPositive(level, 1);
            if (nextIndex >= thresholds.size()) {
                return Integer.MAX_VALUE;
            }
            Integer next = thresholds.get(nextIndex);
            if (next == null) {
                return Integer.MAX_VALUE;
            }
            int nextThreshold = Math.max(0, next);
            if (nextThreshold <= 0) {
                return 0;
            }
            return nextThreshold - 1;
        }

        private int maxXpForGeneralLevel(int level) {
            if (level < 0) {
                return 0;
            }
            List<? extends Integer> thresholds = RealCivConfig.GENERAL_XP_THRESHOLDS.get();
            if (thresholds.isEmpty()) {
                return Integer.MAX_VALUE;
            }
            int nextIndex = safeAddPositive(level, 1);
            if (nextIndex >= thresholds.size()) {
                return Integer.MAX_VALUE;
            }
            Integer next = thresholds.get(nextIndex);
            if (next == null) {
                return Integer.MAX_VALUE;
            }
            int nextThreshold = Math.max(0, next);
            if (nextThreshold <= 0) {
                return 0;
            }
            return nextThreshold - 1;
        }

        private static long currentDayIndex() {
            return Math.floorDiv(System.currentTimeMillis(), DAY_MILLIS);
        }

        private static int safeAddPositive(int left, int right) {
            long value = (long) left + (long) right;
            if (value <= 0L) {
                return 0;
            }
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) value;
        }

        private int professionXpValue(Profession profession) {
            return switch (profession) {
                case FARMER -> farmerXp;
                case MINER -> minerXp;
                case TERRAFORMER -> terraformerXp;
                case LUMBERJACK -> lumberjackXp;
                case FISHER -> fisherXp;
                case HUNTER -> hunterXp;
                case WARRIOR -> warriorXp;
                case EXPLOSIVES_EXPERT -> explosivesExpertXp;
                case CRAFTER -> crafterXp;
                case ENCHANTER -> enchanterXp;
                case BREWER -> brewerXp;
                case TRADER -> traderXp;
                case SHEPHERD -> shepherdXp;
                case EXPLORER -> explorerXp;
                case TREASURE_HUNTER -> treasureHunterXp;
                case BREEDER -> breederXp;
                case SMITHY -> smithyXp;
                case SMELTER -> smelterXp;
                case NONE -> 0;
            };
        }

        private void setProfessionXpValue(Profession profession, int value) {
            int clamped = Math.max(0, value);
            switch (profession) {
                case FARMER -> farmerXp = clamped;
                case MINER -> minerXp = clamped;
                case TERRAFORMER -> terraformerXp = clamped;
                case LUMBERJACK -> lumberjackXp = clamped;
                case FISHER -> fisherXp = clamped;
                case HUNTER -> hunterXp = clamped;
                case WARRIOR -> warriorXp = clamped;
                case EXPLOSIVES_EXPERT -> explosivesExpertXp = clamped;
                case CRAFTER -> crafterXp = clamped;
                case ENCHANTER -> enchanterXp = clamped;
                case BREWER -> brewerXp = clamped;
                case TRADER -> traderXp = clamped;
                case SHEPHERD -> shepherdXp = clamped;
                case EXPLORER -> explorerXp = clamped;
                case TREASURE_HUNTER -> treasureHunterXp = clamped;
                case BREEDER -> breederXp = clamped;
                case SMITHY -> smithyXp = clamped;
                case SMELTER -> smelterXp = clamped;
                case NONE -> {
                }
            }
        }

        private void applyProfessionXpDecay(Profession gainedProfession, int gainedXp) {
            if (!RealCivConfig.specializationXpDecayEnabled() || gainedXp <= 0) {
                return;
            }
            double rate = RealCivConfig.specializationXpDecayRate();
            if (rate <= 0.0D) {
                return;
            }
            int decayAmount = Math.max(0, (int) Math.round(gainedXp * rate));
            if (decayAmount <= 0) {
                return;
            }
            for (Profession profession : Profession.values()) {
                if (profession == Profession.NONE || profession == gainedProfession) {
                    continue;
                }
                setProfessionXpValue(profession, professionXpValue(profession) - decayAmount);
            }
        }

        public Map<String, Long> contributions(String civId) {
            return account(civId).contributions;
        }

        public long contributedCount(String civId, ResourceLocation itemId) {
            return account(civId).contributions.getOrDefault(itemId.toString(), 0L);
        }

        public long personalWithdrawnCount(String civId, ResourceLocation itemId) {
            return account(civId).personalWithdrawals.getOrDefault(itemId.toString(), 0L);
        }

        public long dailyAllowanceWithdrawnCount(String civId, ResourceLocation itemId) {
            CivAccount account = account(civId);
            resetDailyAllowanceWindowIfNeeded(account);
            return account.dailyAllowanceWithdrawals.getOrDefault(itemId.toString(), 0L);
        }

        public double effectivePersonalWithdrawRatio(String civId) {
            CivAccount account = account(civId);
            if (account.personalWithdrawRatioOverride == null) {
                return RealCivConfig.defaultPersonalWithdrawRatio();
            }
            return clampRatio(account.personalWithdrawRatioOverride);
        }

        @Nullable
        public Double personalWithdrawRatioOverride(String civId) {
            return account(civId).personalWithdrawRatioOverride;
        }

        public void setPersonalWithdrawRatioOverride(String civId, @Nullable Double ratio) {
            CivAccount account = account(civId);
            if (ratio == null) {
                account.personalWithdrawRatioOverride = null;
                return;
            }
            account.personalWithdrawRatioOverride = clampRatio(ratio);
        }

        public long personalWithdrawLimit(String civId, ResourceLocation itemId) {
            long contributed = contributedCount(civId, itemId);
            return (long) Math.floor(contributed * effectivePersonalWithdrawRatio(civId));
        }

        public long remainingPersonalWithdraw(String civId, ResourceLocation itemId) {
            long remaining = personalWithdrawLimit(civId, itemId) - personalWithdrawnCount(civId, itemId);
            return Math.max(0L, remaining);
        }

        public void recordPersonalWithdrawal(String civId, ResourceLocation itemId, long count) {
            if (count <= 0L) {
                return;
            }
            account(civId).personalWithdrawals.merge(itemId.toString(), count, Long::sum);
        }

        public long remainingDailyAllowance(String civId, ResourceLocation itemId, long dailyLimit) {
            long safeLimit = Math.max(0L, dailyLimit);
            if (safeLimit <= 0L) {
                return 0L;
            }
            long used = dailyAllowanceWithdrawnCount(civId, itemId);
            long remaining = safeLimit - used;
            return Math.max(0L, remaining);
        }

        public void recordDailyAllowanceWithdrawal(String civId, ResourceLocation itemId, long count) {
            if (count <= 0L) {
                return;
            }
            CivAccount account = account(civId);
            resetDailyAllowanceWindowIfNeeded(account);
            account.dailyAllowanceWithdrawals.merge(itemId.toString(), count, Long::sum);
        }

        @Nullable
        public String ftbClaimModeOverride() {
            return ftbClaimModeOverride;
        }

        public void setFtbClaimModeOverride(@Nullable String rawMode) {
            ftbClaimModeOverride = normalizeFtbClaimMode(rawMode);
        }

        // Compatibility wrappers (default civ).
        public long socialCreditCents() {
            return socialCreditCents(RealCivConfig.defaultCivilizationId());
        }

        public Map<String, Long> contributions() {
            return contributions(RealCivConfig.defaultCivilizationId());
        }

        public long personalWithdrawnCount(ResourceLocation itemId) {
            return personalWithdrawnCount(RealCivConfig.defaultCivilizationId(), itemId);
        }

        public double effectivePersonalWithdrawRatio() {
            return effectivePersonalWithdrawRatio(RealCivConfig.defaultCivilizationId());
        }

        @Nullable
        public Double personalWithdrawRatioOverride() {
            return personalWithdrawRatioOverride(RealCivConfig.defaultCivilizationId());
        }

        public void setPersonalWithdrawRatioOverride(@Nullable Double ratio) {
            setPersonalWithdrawRatioOverride(RealCivConfig.defaultCivilizationId(), ratio);
        }

        public long personalWithdrawLimit(ResourceLocation itemId) {
            return personalWithdrawLimit(RealCivConfig.defaultCivilizationId(), itemId);
        }

        public long remainingPersonalWithdraw(ResourceLocation itemId) {
            return remainingPersonalWithdraw(RealCivConfig.defaultCivilizationId(), itemId);
        }

        public void recordPersonalWithdrawal(ResourceLocation itemId, long count) {
            recordPersonalWithdrawal(RealCivConfig.defaultCivilizationId(), itemId, count);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("socialCreditCents", socialCreditCents);
            tag.putInt("farmerActions", farmerActions);
            tag.putInt("minerActions", minerActions);
            tag.putLong("farmerActionsUpdatedAtMillis", farmerActionsUpdatedAtMillis);
            tag.putLong("minerActionsUpdatedAtMillis", minerActionsUpdatedAtMillis);
            tag.putInt("farmerXp", farmerXp);
            tag.putInt("minerXp", minerXp);
            CompoundTag minerBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : minerBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    minerBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("minerBlockActions", minerBlockTag);
            CompoundTag lumberjackBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : lumberjackBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    lumberjackBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("lumberjackBlockActions", lumberjackBlockTag);
            CompoundTag terraformerBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : terraformerBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    terraformerBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("terraformerBlockActions", terraformerBlockTag);
            CompoundTag crafterItemTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : crafterItemActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    crafterItemTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("crafterItemActions", crafterItemTag);
            tag.putInt("terraformerActions", terraformerActions);
            tag.putLong("terraformerActionsUpdatedAtMillis", terraformerActionsUpdatedAtMillis);
            tag.putInt("terraformerXp", terraformerXp);
            tag.putInt("lumberjackActions", lumberjackActions);
            tag.putLong("lumberjackActionsUpdatedAtMillis", lumberjackActionsUpdatedAtMillis);
            tag.putInt("lumberjackXp", lumberjackXp);
            tag.putInt("fisherActions", fisherActions);
            tag.putLong("fisherActionsUpdatedAtMillis", fisherActionsUpdatedAtMillis);
            tag.putInt("fisherXp", fisherXp);
            tag.putInt("hunterActions", hunterActions);
            tag.putLong("hunterActionsUpdatedAtMillis", hunterActionsUpdatedAtMillis);
            tag.putInt("hunterXp", hunterXp);
            tag.putInt("warriorActions", warriorActions);
            tag.putLong("warriorActionsUpdatedAtMillis", warriorActionsUpdatedAtMillis);
            tag.putInt("warriorXp", warriorXp);
            tag.putInt("pendingWarriorHubRegistrations", pendingWarriorHubRegistrations);
            tag.putInt("explosivesExpertActions", explosivesExpertActions);
            tag.putLong("explosivesExpertActionsUpdatedAtMillis", explosivesExpertActionsUpdatedAtMillis);
            tag.putInt("explosivesExpertXp", explosivesExpertXp);
            tag.putInt("crafterActions", crafterActions);
            tag.putLong("crafterActionsUpdatedAtMillis", crafterActionsUpdatedAtMillis);
            tag.putInt("crafterXp", crafterXp);
            tag.putInt("enchanterActions", enchanterActions);
            tag.putLong("enchanterActionsUpdatedAtMillis", enchanterActionsUpdatedAtMillis);
            tag.putInt("enchanterXp", enchanterXp);
            tag.putInt("brewerActions", brewerActions);
            tag.putLong("brewerActionsUpdatedAtMillis", brewerActionsUpdatedAtMillis);
            tag.putInt("brewerXp", brewerXp);
            tag.putInt("traderActions", traderActions);
            tag.putLong("traderActionsUpdatedAtMillis", traderActionsUpdatedAtMillis);
            tag.putInt("traderXp", traderXp);
            tag.putInt("shepherdActions", shepherdActions);
            tag.putLong("shepherdActionsUpdatedAtMillis", shepherdActionsUpdatedAtMillis);
            tag.putInt("shepherdXp", shepherdXp);
            tag.putInt("explorerActions", explorerActions);
            tag.putLong("explorerActionsUpdatedAtMillis", explorerActionsUpdatedAtMillis);
            tag.putInt("explorerXp", explorerXp);
            tag.putInt("treasureHunterActions", treasureHunterActions);
            tag.putLong("treasureHunterActionsUpdatedAtMillis", treasureHunterActionsUpdatedAtMillis);
            tag.putInt("treasureHunterXp", treasureHunterXp);
            tag.putInt("breederActions", breederActions);
            tag.putLong("breederActionsUpdatedAtMillis", breederActionsUpdatedAtMillis);
            tag.putInt("breederXp", breederXp);
            tag.putInt("smithyActions", smithyActions);
            tag.putLong("smithyActionsUpdatedAtMillis", smithyActionsUpdatedAtMillis);
            tag.putInt("smithyXp", smithyXp);
            tag.putInt("smelterActions", smelterActions);
            tag.putLong("smelterActionsUpdatedAtMillis", smelterActionsUpdatedAtMillis);
            tag.putInt("smelterXp", smelterXp);
            tag.putInt("generalXp", generalXp);
            if (firstSeenAtMillis > 0L) {
                tag.putLong("firstSeenAtMillis", firstSeenAtMillis);
            }
            if (focusedProfession != null && focusedProfession != Profession.NONE) {
                tag.putString("focusedProfession", focusedProfession.name());
            }

            ListTag hookUsageTag = new ListTag();
            for (Map.Entry<String, HookWindowUsage> entry : hookWindowUsage.entrySet()) {
                String key = entry.getKey();
                HookWindowUsage usage = entry.getValue();
                if (key == null || key.isBlank() || usage == null || usage.usedTriggers <= 0) {
                    continue;
                }
                CompoundTag usageTag = new CompoundTag();
                usageTag.putString("key", key);
                usageTag.putLong("windowStartMillis", Math.max(0L, usage.windowStartMillis));
                usageTag.putInt("used", Math.max(0, usage.usedTriggers));
                hookUsageTag.add(usageTag);
            }
            tag.put("hookWindowUsage", hookUsageTag);

            CompoundTag hunterMobTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : hunterMobActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    hunterMobTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("hunterMobActions", hunterMobTag);

            if (professionDailyActionDayIndex >= 0L) {
                tag.putLong("professionDailyActionDayIndex", professionDailyActionDayIndex);
            }
            CompoundTag professionDailyTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : professionDailyActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    professionDailyTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("professionDailyActions", professionDailyTag);

            if (minerDailyBlockActionDayIndex >= 0L) {
                tag.putLong("minerDailyBlockActionDayIndex", minerDailyBlockActionDayIndex);
            }
            CompoundTag minerDailyBlockTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : minerDailyBlockActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    minerDailyBlockTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("minerDailyBlockActions", minerDailyBlockTag);

            if (crafterDailyItemActionDayIndex >= 0L) {
                tag.putLong("crafterDailyItemActionDayIndex", crafterDailyItemActionDayIndex);
            }
            CompoundTag crafterDailyItemTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : crafterDailyItemActions.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    crafterDailyItemTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("crafterDailyItemActions", crafterDailyItemTag);

            if (professionProgressDayIndex >= 0L) {
                tag.putLong("professionProgressDayIndex", professionProgressDayIndex);
            }
            tag.putInt("generalLevelsGainedToday", Math.max(0, generalLevelsGainedToday));
            CompoundTag professionGainTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : professionLevelsGainedToday.entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (value > 0) {
                    professionGainTag.putInt(entry.getKey(), value);
                }
            }
            tag.put("professionLevelsGainedToday", professionGainTag);

            if (contributionGainDayIndex >= 0L) {
                tag.putLong("contributionGainDayIndex", contributionGainDayIndex);
            }
            CompoundTag contributionGainTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : contributionGainTodayCents.entrySet()) {
                long value = Math.max(0L, entry.getValue());
                if (value > 0L) {
                    contributionGainTag.putLong(entry.getKey(), value);
                }
            }
            tag.put("contributionGainTodayCents", contributionGainTag);

            CompoundTag accountsTag = new CompoundTag();
            for (Map.Entry<String, CivAccount> entry : civAccounts.entrySet()) {
                accountsTag.put(entry.getKey(), entry.getValue().save());
            }
            tag.put("civAccounts", accountsTag);

            CompoundTag contributionTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : contributions.entrySet()) {
                contributionTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
            }
            tag.put("contributions", contributionTag);

            CompoundTag withdrawalTag = new CompoundTag();
            for (Map.Entry<String, Long> entry : personalWithdrawals.entrySet()) {
                withdrawalTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
            }
            tag.put("personalWithdrawals", withdrawalTag);

            if (personalWithdrawRatioOverride != null) {
                tag.putDouble("personalWithdrawRatioOverride", clampRatio(personalWithdrawRatioOverride));
            }
            if (ftbClaimModeOverride != null) {
                tag.putString("ftbClaimModeOverride", ftbClaimModeOverride);
            }
            return tag;
        }

        public static PlayerRecord load(CompoundTag tag) {
            PlayerRecord record = new PlayerRecord();
            record.socialCreditCents = Math.max(0L, tag.getLong("socialCreditCents"));
            record.farmerActions = Math.max(0, tag.getInt("farmerActions"));
            record.minerActions = Math.max(0, tag.getInt("minerActions"));
            record.farmerXp = Math.max(0, tag.getInt("farmerXp"));
            record.minerXp = Math.max(0, tag.getInt("minerXp"));
            CompoundTag minerBlockTag = tag.getCompound("minerBlockActions");
            for (String key : minerBlockTag.getAllKeys()) {
                int value = Math.max(0, minerBlockTag.getInt(key));
                if (value > 0) {
                    record.minerBlockActions.put(key, value);
                }
            }
            CompoundTag lumberjackBlockTag = tag.getCompound("lumberjackBlockActions");
            for (String key : lumberjackBlockTag.getAllKeys()) {
                int value = Math.max(0, lumberjackBlockTag.getInt(key));
                if (value > 0) {
                    record.lumberjackBlockActions.put(key, value);
                }
            }
            CompoundTag terraformerBlockTag = tag.getCompound("terraformerBlockActions");
            for (String key : terraformerBlockTag.getAllKeys()) {
                int value = Math.max(0, terraformerBlockTag.getInt(key));
                if (value > 0) {
                    record.terraformerBlockActions.put(key, value);
                }
            }
            CompoundTag crafterItemTag = tag.getCompound("crafterItemActions");
            for (String key : crafterItemTag.getAllKeys()) {
                int value = Math.max(0, crafterItemTag.getInt(key));
                if (value > 0) {
                    record.crafterItemActions.put(key, value);
                }
            }
            record.terraformerActions = Math.max(0, tag.getInt("terraformerActions"));
            record.terraformerXp = Math.max(0, tag.getInt("terraformerXp"));
            record.lumberjackActions = Math.max(0, tag.getInt("lumberjackActions"));
            record.lumberjackXp = Math.max(0, tag.getInt("lumberjackXp"));
            record.fisherActions = Math.max(0, tag.getInt("fisherActions"));
            record.fisherXp = Math.max(0, tag.getInt("fisherXp"));
            record.hunterActions = Math.max(0, tag.getInt("hunterActions"));
            record.hunterXp = Math.max(0, tag.getInt("hunterXp"));
            record.warriorActions = Math.max(0, tag.getInt("warriorActions"));
            record.warriorXp = Math.max(0, tag.getInt("warriorXp"));
            record.pendingWarriorHubRegistrations = Math.max(0, tag.getInt("pendingWarriorHubRegistrations"));
            record.explosivesExpertActions = Math.max(0, tag.getInt("explosivesExpertActions"));
            record.explosivesExpertXp = Math.max(0, tag.getInt("explosivesExpertXp"));
            record.crafterActions = Math.max(0, tag.getInt("crafterActions"));
            record.crafterXp = Math.max(0, tag.getInt("crafterXp"));
            record.enchanterActions = Math.max(0, tag.getInt("enchanterActions"));
            record.enchanterXp = Math.max(0, tag.getInt("enchanterXp"));
            record.brewerActions = Math.max(0, tag.getInt("brewerActions"));
            record.brewerXp = Math.max(0, tag.getInt("brewerXp"));
            record.traderActions = Math.max(0, tag.getInt("traderActions"));
            record.traderXp = Math.max(0, tag.getInt("traderXp"));
            record.shepherdActions = Math.max(0, tag.getInt("shepherdActions"));
            record.shepherdXp = Math.max(0, tag.getInt("shepherdXp"));
            record.explorerActions = Math.max(0, tag.getInt("explorerActions"));
            record.explorerXp = Math.max(0, tag.getInt("explorerXp"));
            record.treasureHunterActions = Math.max(0, tag.getInt("treasureHunterActions"));
            record.treasureHunterXp = Math.max(0, tag.getInt("treasureHunterXp"));
            record.breederActions = Math.max(0, tag.getInt("breederActions"));
            record.breederXp = Math.max(0, tag.getInt("breederXp"));
            record.smithyActions = Math.max(0, tag.getInt("smithyActions"));
            record.smithyXp = Math.max(0, tag.getInt("smithyXp"));
            record.smelterActions = Math.max(0, tag.getInt("smelterActions"));
            record.smelterXp = Math.max(0, tag.getInt("smelterXp"));
            record.generalXp = Math.max(0, tag.getInt("generalXp"));
            if (tag.contains("focusedProfession")) {
                Profession parsed = Profession.fromConfigName(tag.getString("focusedProfession"));
                if (parsed != null && parsed != Profession.NONE) {
                    record.focusedProfession = parsed;
                }
            }

            long loadedAt = System.currentTimeMillis();
            record.firstSeenAtMillis = tag.contains("firstSeenAtMillis")
                    ? Math.max(0L, tag.getLong("firstSeenAtMillis"))
                    : loadedAt;
            if (record.firstSeenAtMillis <= 0L) {
                record.firstSeenAtMillis = loadedAt;
            }
            record.farmerActionsUpdatedAtMillis = tag.contains("farmerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("farmerActionsUpdatedAtMillis"))
                    : (record.farmerActions > 0 ? loadedAt : 0L);
            record.minerActionsUpdatedAtMillis = tag.contains("minerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("minerActionsUpdatedAtMillis"))
                    : (record.minerActions > 0 ? loadedAt : 0L);
            record.terraformerActionsUpdatedAtMillis = tag.contains("terraformerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("terraformerActionsUpdatedAtMillis"))
                    : (record.terraformerActions > 0 ? loadedAt : 0L);
            record.lumberjackActionsUpdatedAtMillis = tag.contains("lumberjackActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("lumberjackActionsUpdatedAtMillis"))
                    : (record.lumberjackActions > 0 ? loadedAt : 0L);
            record.fisherActionsUpdatedAtMillis = tag.contains("fisherActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("fisherActionsUpdatedAtMillis"))
                    : (record.fisherActions > 0 ? loadedAt : 0L);
            record.hunterActionsUpdatedAtMillis = tag.contains("hunterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("hunterActionsUpdatedAtMillis"))
                    : (record.hunterActions > 0 ? loadedAt : 0L);
            record.warriorActionsUpdatedAtMillis = tag.contains("warriorActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("warriorActionsUpdatedAtMillis"))
                    : (record.warriorActions > 0 ? loadedAt : 0L);
            record.explosivesExpertActionsUpdatedAtMillis = tag.contains("explosivesExpertActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("explosivesExpertActionsUpdatedAtMillis"))
                    : (record.explosivesExpertActions > 0 ? loadedAt : 0L);
            record.crafterActionsUpdatedAtMillis = tag.contains("crafterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("crafterActionsUpdatedAtMillis"))
                    : (record.crafterActions > 0 ? loadedAt : 0L);
            record.enchanterActionsUpdatedAtMillis = tag.contains("enchanterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("enchanterActionsUpdatedAtMillis"))
                    : (record.enchanterActions > 0 ? loadedAt : 0L);
            record.brewerActionsUpdatedAtMillis = tag.contains("brewerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("brewerActionsUpdatedAtMillis"))
                    : (record.brewerActions > 0 ? loadedAt : 0L);
            record.traderActionsUpdatedAtMillis = tag.contains("traderActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("traderActionsUpdatedAtMillis"))
                    : (record.traderActions > 0 ? loadedAt : 0L);
            record.shepherdActionsUpdatedAtMillis = tag.contains("shepherdActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("shepherdActionsUpdatedAtMillis"))
                    : (record.shepherdActions > 0 ? loadedAt : 0L);
            record.explorerActionsUpdatedAtMillis = tag.contains("explorerActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("explorerActionsUpdatedAtMillis"))
                    : (record.explorerActions > 0 ? loadedAt : 0L);
            record.treasureHunterActionsUpdatedAtMillis = tag.contains("treasureHunterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("treasureHunterActionsUpdatedAtMillis"))
                    : (record.treasureHunterActions > 0 ? loadedAt : 0L);
            record.breederActionsUpdatedAtMillis = tag.contains("breederActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("breederActionsUpdatedAtMillis"))
                    : (record.breederActions > 0 ? loadedAt : 0L);
            record.smithyActionsUpdatedAtMillis = tag.contains("smithyActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("smithyActionsUpdatedAtMillis"))
                    : (record.smithyActions > 0 ? loadedAt : 0L);
            record.smelterActionsUpdatedAtMillis = tag.contains("smelterActionsUpdatedAtMillis")
                    ? Math.max(0L, tag.getLong("smelterActionsUpdatedAtMillis"))
                    : (record.smelterActions > 0 ? loadedAt : 0L);

            ListTag hookUsageTag = tag.getList("hookWindowUsage", Tag.TAG_COMPOUND);
            for (Tag entry : hookUsageTag) {
                if (!(entry instanceof CompoundTag usageTag)) {
                    continue;
                }
                String key = usageTag.getString("key");
                if (key == null || key.isBlank()) {
                    continue;
                }
                long windowStartMillis = Math.max(0L, usageTag.getLong("windowStartMillis"));
                int used = Math.max(0, usageTag.getInt("used"));
                if (used <= 0) {
                    continue;
                }
                HookWindowUsage usage = new HookWindowUsage();
                usage.windowStartMillis = windowStartMillis;
                usage.usedTriggers = used;
                record.hookWindowUsage.put(key, usage);
            }

            CompoundTag hunterMobTag = tag.getCompound("hunterMobActions");
            for (String key : hunterMobTag.getAllKeys()) {
                int value = Math.max(0, hunterMobTag.getInt(key));
                if (value > 0) {
                    record.hunterMobActions.put(key, value);
                }
            }

            record.professionDailyActionDayIndex = tag.contains("professionDailyActionDayIndex")
                    ? tag.getLong("professionDailyActionDayIndex")
                    : -1L;
            CompoundTag professionDailyTag = tag.getCompound("professionDailyActions");
            for (String key : professionDailyTag.getAllKeys()) {
                int value = Math.max(0, professionDailyTag.getInt(key));
                if (value > 0) {
                    record.professionDailyActions.put(key, value);
                }
            }

            record.minerDailyBlockActionDayIndex = tag.contains("minerDailyBlockActionDayIndex")
                    ? tag.getLong("minerDailyBlockActionDayIndex")
                    : -1L;
            CompoundTag minerDailyBlockTag = tag.getCompound("minerDailyBlockActions");
            for (String key : minerDailyBlockTag.getAllKeys()) {
                int value = Math.max(0, minerDailyBlockTag.getInt(key));
                if (value > 0) {
                    record.minerDailyBlockActions.put(key, value);
                }
            }

            record.crafterDailyItemActionDayIndex = tag.contains("crafterDailyItemActionDayIndex")
                    ? tag.getLong("crafterDailyItemActionDayIndex")
                    : -1L;
            CompoundTag crafterDailyItemTag = tag.getCompound("crafterDailyItemActions");
            for (String key : crafterDailyItemTag.getAllKeys()) {
                int value = Math.max(0, crafterDailyItemTag.getInt(key));
                if (value > 0) {
                    record.crafterDailyItemActions.put(key, value);
                }
            }

            record.professionProgressDayIndex = tag.contains("professionProgressDayIndex")
                    ? tag.getLong("professionProgressDayIndex")
                    : -1L;
            record.generalLevelsGainedToday = Math.max(0, tag.getInt("generalLevelsGainedToday"));
            CompoundTag professionGainTag = tag.getCompound("professionLevelsGainedToday");
            for (String key : professionGainTag.getAllKeys()) {
                int value = Math.max(0, professionGainTag.getInt(key));
                if (value > 0) {
                    record.professionLevelsGainedToday.put(key, value);
                }
            }

            record.contributionGainDayIndex = tag.contains("contributionGainDayIndex")
                    ? tag.getLong("contributionGainDayIndex")
                    : -1L;
            CompoundTag contributionGainTag = tag.getCompound("contributionGainTodayCents");
            for (String key : contributionGainTag.getAllKeys()) {
                long value = Math.max(0L, contributionGainTag.getLong(key));
                if (value > 0L) {
                    record.contributionGainTodayCents.put(key, value);
                }
            }

            CompoundTag accountsTag = tag.getCompound("civAccounts");
            for (String civId : accountsTag.getAllKeys()) {
                record.civAccounts.put(civId, CivAccount.load(accountsTag.getCompound(civId)));
            }

            CompoundTag contributionTag = tag.getCompound("contributions");
            for (String key : contributionTag.getAllKeys()) {
                record.contributions.put(key, Math.max(0L, contributionTag.getLong(key)));
            }

            CompoundTag withdrawalTag = tag.getCompound("personalWithdrawals");
            for (String key : withdrawalTag.getAllKeys()) {
                record.personalWithdrawals.put(key, Math.max(0L, withdrawalTag.getLong(key)));
            }

            if (tag.contains("personalWithdrawRatioOverride")) {
                record.personalWithdrawRatioOverride = clampRatio(tag.getDouble("personalWithdrawRatioOverride"));
            }
            if (tag.contains("ftbClaimModeOverride")) {
                record.ftbClaimModeOverride = normalizeFtbClaimMode(tag.getString("ftbClaimModeOverride"));
            }
            return record;
        }

        public int levelFor(Profession profession) {
            return switch (profession) {
                case FARMER -> RealCivConfig.professionLevelFromXp(Profession.FARMER, farmerXp());
                case MINER -> RealCivConfig.professionLevelFromXp(Profession.MINER, minerXp());
                case TERRAFORMER -> RealCivConfig.professionLevelFromXp(Profession.TERRAFORMER, terraformerXp());
                case LUMBERJACK -> RealCivConfig.professionLevelFromXp(Profession.LUMBERJACK, lumberjackXp());
                case FISHER -> RealCivConfig.professionLevelFromXp(Profession.FISHER, fisherXp());
                case HUNTER -> RealCivConfig.professionLevelFromXp(Profession.HUNTER, hunterXp());
                case WARRIOR -> RealCivConfig.professionLevelFromXp(Profession.WARRIOR, warriorXp());
                case EXPLOSIVES_EXPERT -> RealCivConfig.professionLevelFromXp(Profession.EXPLOSIVES_EXPERT, explosivesExpertXp());
                case CRAFTER -> RealCivConfig.professionLevelFromXp(Profession.CRAFTER, crafterXp());
                case ENCHANTER -> RealCivConfig.professionLevelFromXp(Profession.ENCHANTER, enchanterXp());
                case BREWER -> RealCivConfig.professionLevelFromXp(Profession.BREWER, brewerXp());
                case TRADER -> RealCivConfig.professionLevelFromXp(Profession.TRADER, traderXp());
                case SHEPHERD -> RealCivConfig.professionLevelFromXp(Profession.SHEPHERD, shepherdXp());
                case EXPLORER -> RealCivConfig.professionLevelFromXp(Profession.EXPLORER, explorerXp());
                case TREASURE_HUNTER -> RealCivConfig.professionLevelFromXp(Profession.TREASURE_HUNTER, treasureHunterXp());
                case BREEDER -> RealCivConfig.professionLevelFromXp(Profession.BREEDER, breederXp());
                case SMITHY -> RealCivConfig.professionLevelFromXp(Profession.SMITHY, smithyXp());
                case SMELTER -> RealCivConfig.professionLevelFromXp(Profession.SMELTER, smelterXp());
                case NONE -> 0;
            };
        }

        public int generalLevel() {
            return RealCivConfig.generalLevelFromXp(generalXp());
        }

        private static double clampRatio(double ratio) {
            return Math.max(0.0D, Math.min(1.0D, ratio));
        }

        private static long currentUtcDayIndex() {
            return System.currentTimeMillis() / DAY_MILLIS;
        }

        private static void resetDailyAllowanceWindowIfNeeded(CivAccount account) {
            long dayIndex = currentUtcDayIndex();
            if (account.dailyAllowanceDayIndex != dayIndex) {
                account.dailyAllowanceDayIndex = dayIndex;
                account.dailyAllowanceWithdrawals.clear();
            }
        }

        @Nullable
        private static String normalizeFtbClaimMode(@Nullable String rawMode) {
            if (rawMode == null) {
                return null;
            }
            String mode = rawMode.trim().toLowerCase(java.util.Locale.ROOT);
            if (mode.isEmpty() || "auto".equals(mode)) {
                return null;
            }
            if ("civic".equals(mode) || "private".equals(mode)) {
                return mode;
            }
            return null;
        }

        private static final class HookWindowUsage {
            private long windowStartMillis;
            private int usedTriggers;
        }
}