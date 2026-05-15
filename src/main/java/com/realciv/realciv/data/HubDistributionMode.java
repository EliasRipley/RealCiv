package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum HubDistributionMode {
    CONTRIBUTION_RATIO,
    SHARED_STOCK_RATIO,
    DAILY_ALLOWANCE;

    @Nullable
    public static HubDistributionMode fromSerializedName(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "CONTRIBUTION_RATIO", "RATIO", "CONTRIBUTION", "DEFAULT" -> CONTRIBUTION_RATIO;
            case "SHARED_STOCK_RATIO", "SHARED", "GLOBAL", "ALL_GOODS", "STOCK_RATIO" -> SHARED_STOCK_RATIO;
            case "DAILY_ALLOWANCE", "ALLOWANCE", "DAILY" -> DAILY_ALLOWANCE;
            default -> null;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
