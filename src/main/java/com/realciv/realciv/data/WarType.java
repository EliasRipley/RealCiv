package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum WarType {
    DESTRUCTION,
    PVP;

    @Nullable
    public static WarType fromSerializedName(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "DESTRUCTION", "DESTROY", "TOTAL_WAR" -> DESTRUCTION;
            case "PVP", "KILL_COUNT", "DUEL_WAR" -> PVP;
            default -> null;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return switch (this) {
            case DESTRUCTION -> "DESTRUCTION";
            case PVP -> "PVP";
        };
    }
}
