package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum TaxPaymentMode {
    KARMA,
    ITEM;

    @Nullable
    public static TaxPaymentMode fromSerializedName(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "KARMA", "CREDIT", "CREDITS", "COMMUNITY_KARMA" -> KARMA;
            case "ITEM", "ITEMS", "GOODS" -> ITEM;
            default -> null;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
