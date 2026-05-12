package com.realciv.realciv.panel;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PolicyRuleListUtilTest {
    @Test
    void addUniqueAppendsOnlyWhenMissing() {
        List<String> base = List.of("rule_a", "rule_b");
        List<String> withNew = PolicyRuleListUtil.addUnique(base, "rule_c");
        Assertions.assertEquals(List.of("rule_a", "rule_b", "rule_c"), withNew);

        List<String> withDuplicate = PolicyRuleListUtil.addUnique(base, "rule_b");
        Assertions.assertEquals(List.of("rule_a", "rule_b"), withDuplicate);
    }

    @Test
    void removeLastSafelyHandlesEmptyAndNonEmptyLists() {
        Assertions.assertEquals(List.of(), PolicyRuleListUtil.removeLast(List.of()));
        Assertions.assertEquals(List.of("rule_a"), PolicyRuleListUtil.removeLast(List.of("rule_a", "rule_b")));
    }
}
