package com.realciv.realciv.panel;

import com.realciv.realciv.data.CivicAttribute;

public final class GovernanceMath {
    private GovernanceMath() {
    }

    public static int requiredYesVotes(CivicAttribute executiveAttribute, int eligibleVoters) {
        int eligible = Math.max(1, eligibleVoters);
        if (executiveAttribute == CivicAttribute.DIRECT_RULE) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(eligible / 2.0D));
    }

    public static boolean quorumReached(int yesVotes, int requiredYesVotes) {
        return yesVotes >= Math.max(1, requiredYesVotes);
    }
}
