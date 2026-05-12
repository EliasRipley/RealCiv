package com.realciv.realciv.panel;

import java.util.ArrayList;
import java.util.List;

public final class PolicyRuleListUtil {
    private PolicyRuleListUtil() {
    }

    public static List<String> addUnique(List<? extends String> current, String entry) {
        List<String> out = new ArrayList<>(current.stream().map(String::valueOf).toList());
        if (!out.contains(entry)) {
            out.add(entry);
        }
        return out;
    }

    public static List<String> removeLast(List<? extends String> current) {
        List<String> out = new ArrayList<>(current.stream().map(String::valueOf).toList());
        if (!out.isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out;
    }
}
