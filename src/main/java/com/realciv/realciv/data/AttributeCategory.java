package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum AttributeCategory {
    EXECUTIVE("executive", "Executive Power", "How policy decisions are made"),
    SUCCESSION("succession", "Succession", "How leadership changes"),
    RESOURCE("resource", "Resource Policy", "How community resources are shared"),
    TAXATION("taxation", "Taxation", "How upkeep is funded"),
    MEMBERSHIP("membership", "Membership", "Who can join"),
    LAND("land", "Land Policy", "How territory is claimed");

    private final String serializedName;
    private final String displayName;
    private final String description;

    AttributeCategory(String serializedName, String displayName, String description) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.description = description;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    @Nullable
    public static AttributeCategory fromSerializedName(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (AttributeCategory cat : values()) {
            if (cat.serializedName.equals(raw.trim().toLowerCase(Locale.ROOT))) {
                return cat;
            }
        }
        return null;
    }
}
