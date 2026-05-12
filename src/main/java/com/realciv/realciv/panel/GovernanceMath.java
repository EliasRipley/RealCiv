package com.realciv.realciv.panel;

import java.util.Locale;

public final class GovernanceMath {
    private GovernanceMath() {
    }

    public static int requiredYesVotes(String governanceModel, int eligibleVoters) {
        int eligible = Math.max(1, eligibleVoters);
        String normalized = governanceModel == null ? "" : governanceModel.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AUTOCRATIC", "AUTOCRACY", "AUTO" -> 1;
            case "COUNCIL", "OLIGARCHY", "DEMOCRATIC", "DEMOCRACY", "DEMO" ->
                    Math.max(1, (int) Math.ceil(eligible / 2.0D));
            default -> Math.max(1, (int) Math.ceil(eligible / 2.0D));
        };
    }

    public static boolean quorumReached(int yesVotes, int requiredYesVotes) {
        return yesVotes >= Math.max(1, requiredYesVotes);
    }
}
