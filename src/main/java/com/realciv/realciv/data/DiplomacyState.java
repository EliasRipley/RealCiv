package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum DiplomacyState {
    ALLY,
    NEUTRAL,
    WAR;

    @Nullable
    public static DiplomacyState fromSerializedName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ALLY", "ALLIES" -> ALLY;
            case "NEUTRAL", "NONE" -> NEUTRAL;
            case "WAR", "HOSTILE", "HOSTILES" -> WAR;
            default -> null;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return switch (this) {
            case ALLY -> "ALLY";
            case NEUTRAL -> "NEUTRAL";
            case WAR -> "WAR";
        };
    }
}
