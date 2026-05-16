package com.realciv.realciv.panel;

import com.realciv.realciv.data.CivicAttribute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GovernanceMathTest {
    @Test
    void requiredYesVotesRespectsExecutiveAttribute() {
        Assertions.assertEquals(1, GovernanceMath.requiredYesVotes(CivicAttribute.DIRECT_RULE, 10));
        Assertions.assertEquals(1, GovernanceMath.requiredYesVotes(CivicAttribute.COUNCIL_VOTE, 1));
        Assertions.assertEquals(2, GovernanceMath.requiredYesVotes(CivicAttribute.COUNCIL_VOTE, 3));
        Assertions.assertEquals(3, GovernanceMath.requiredYesVotes(CivicAttribute.POPULAR_VOTE, 5));
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
