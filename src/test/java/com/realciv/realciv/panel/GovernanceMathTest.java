package com.realciv.realciv.panel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GovernanceMathTest {
    @Test
    void requiredYesVotesRespectsGovernanceModel() {
        Assertions.assertEquals(1, GovernanceMath.requiredYesVotes("AUTOCRATIC", 10));
        Assertions.assertEquals(1, GovernanceMath.requiredYesVotes("COUNCIL", 1));
        Assertions.assertEquals(2, GovernanceMath.requiredYesVotes("COUNCIL", 3));
        Assertions.assertEquals(3, GovernanceMath.requiredYesVotes("DEMOCRATIC", 5));
    }

    @Test
    void quorumReachedRequiresAtLeastConfiguredThreshold() {
        Assertions.assertFalse(GovernanceMath.quorumReached(0, 1));
        Assertions.assertTrue(GovernanceMath.quorumReached(1, 1));
        Assertions.assertFalse(GovernanceMath.quorumReached(2, 3));
        Assertions.assertTrue(GovernanceMath.quorumReached(3, 3));
        Assertions.assertTrue(GovernanceMath.quorumReached(4, 3));
    }
}
