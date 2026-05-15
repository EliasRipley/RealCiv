package com.realciv.realciv.data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CivRoleView(
        String roleId,
        String displayName,
        Set<String> permissions,
        List<UUID> members) {
}
