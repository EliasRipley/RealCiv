package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum LeadershipContestType {
    ELECTION,
    COUP;

    @Nullable
    public static LeadershipContestType fromSerializedName(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ELECTION" -> ELECTION;
            case "COUP" -> COUP;
            default -> null;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
