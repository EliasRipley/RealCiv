package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum LandClass {
    PUBLIC,
    CIVIC,
    PRIVATE;

    @Nullable
    public static LandClass fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "PUBLIC" -> PUBLIC;
            case "CIVIC" -> CIVIC;
            case "PRIVATE" -> PRIVATE;
            default -> null;
        };
    }
}
