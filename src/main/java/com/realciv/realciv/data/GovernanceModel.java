package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum GovernanceModel {
    AUTOCRATIC,
    COUNCIL,
    DEMOCRATIC;

    @Nullable
    public static GovernanceModel fromSerializedName(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "AUTOCRATIC", "AUTOCRACY", "AUTO" -> AUTOCRATIC;
            case "COUNCIL", "OLIGARCHY" -> COUNCIL;
            case "DEMOCRATIC", "DEMOCRACY", "DEMO" -> DEMOCRATIC;
            default -> null;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
