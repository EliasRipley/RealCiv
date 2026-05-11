package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum LandClass {
    COMMUNITY,
    CIVIC,
    PRIVATE;

    @Nullable
    public static LandClass fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "COMMUNITY", "PUBLIC" -> COMMUNITY;
            case "CIVIC" -> CIVIC;
            case "PRIVATE" -> PRIVATE;
            default -> null;
        };
    }
}
