package com.realciv.realciv.logic;

import org.jetbrains.annotations.Nullable;

/**
 * One config-defined mapping from a NeoForge gameplay hook to a profession action counter.
 */
public record ProfessionEventHookRule(
        ProfessionEventHook hook,
        Profession profession,
        int actionCost,
        int minProfessionLevel,
        int minGeneralLevel,
        long minMembershipMillis,
        int windowSeconds,
        int maxTriggersPerWindow,
        int professionXpPerTrigger,
        int generalXpPerTrigger,
        @Nullable String statPrefixFilter,
        @Nullable String denyMessageOverride) {

    public ProfessionEventHookRule {
        actionCost = Math.max(0, actionCost);
        minProfessionLevel = Math.max(0, minProfessionLevel);
        minGeneralLevel = Math.max(0, minGeneralLevel);
        minMembershipMillis = Math.max(0L, minMembershipMillis);
        windowSeconds = Math.max(0, windowSeconds);
        maxTriggersPerWindow = Math.max(0, maxTriggersPerWindow);
        if (windowSeconds <= 0 || maxTriggersPerWindow <= 0) {
            windowSeconds = 0;
            maxTriggersPerWindow = 0;
        }
        professionXpPerTrigger = Math.max(0, professionXpPerTrigger);
        generalXpPerTrigger = Math.max(0, generalXpPerTrigger);
        if (statPrefixFilter != null && statPrefixFilter.isBlank()) {
            statPrefixFilter = null;
        }
        if (denyMessageOverride != null && denyMessageOverride.isBlank()) {
            denyMessageOverride = null;
        }
    }

    public boolean hasWindowQuota() {
        return windowSeconds > 0 && maxTriggersPerWindow > 0;
    }

    public String windowCounterKey() {
        String stat = statPrefixFilter == null ? "" : statPrefixFilter;
        return hook.name()
                + "|"
                + profession.name()
                + "|"
                + actionCost
                + "|"
                + minProfessionLevel
                + "|"
                + minGeneralLevel
                + "|"
                + minMembershipMillis
                + "|"
                + windowSeconds
                + "|"
                + maxTriggersPerWindow
                + "|"
                + professionXpPerTrigger
                + "|"
                + generalXpPerTrigger
                + "|"
                + stat;
    }
}
