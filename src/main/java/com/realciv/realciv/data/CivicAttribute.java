package com.realciv.realciv.data;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum CivicAttribute {
    DIRECT_RULE(AttributeCategory.EXECUTIVE, "direct_rule", "Direct Rule"),
    COUNCIL_VOTE(AttributeCategory.EXECUTIVE, "council_vote", "Council Vote"),
    POPULAR_VOTE(AttributeCategory.EXECUTIVE, "popular_vote", "Popular Vote"),

    APPOINTED(AttributeCategory.SUCCESSION, "appointed", "Appointed"),
    ELECTION(AttributeCategory.SUCCESSION, "election", "Election"),
    COUP(AttributeCategory.SUCCESSION, "coup", "Coup"),

    CONTRIBUTION_SHARE(AttributeCategory.RESOURCE, "contribution_share", "Contribution Share"),
    EQUAL_SHARE(AttributeCategory.RESOURCE, "equal_share", "Equal Share"),
    RATIONED(AttributeCategory.RESOURCE, "rationed", "Rationed"),

    KARMA_TAX(AttributeCategory.TAXATION, "karma_tax", "Karma Tax"),
    GOODS_TAX(AttributeCategory.TAXATION, "goods_tax", "Goods Tax"),
    EXEMPT(AttributeCategory.TAXATION, "exempt", "Exempt"),

    OPEN(AttributeCategory.MEMBERSHIP, "open", "Open"),
    INVITE_ONLY(AttributeCategory.MEMBERSHIP, "invite_only", "Invite Only"),
    APPLICATION(AttributeCategory.MEMBERSHIP, "application", "Application"),

    OPEN_CLAIM(AttributeCategory.LAND, "open_claim", "Open Claim"),
    LEADER_CLAIM(AttributeCategory.LAND, "leader_claim", "Leader Claim"),
    TAXED_CLAIM(AttributeCategory.LAND, "taxed_claim", "Taxed Claim");

    private final AttributeCategory category;
    private final String serializedName;
    private final String displayName;

    CivicAttribute(AttributeCategory category, String serializedName, String displayName) {
        this.category = category;
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    public AttributeCategory category() {
        return category;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    @Nullable
    public static CivicAttribute fromSerializedName(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        for (CivicAttribute attr : values()) {
            if (attr.serializedName.equals(trimmed)) {
                return attr;
            }
        }
        return null;
    }

    public static CivicAttribute defaultFor(AttributeCategory category) {
        return switch (category) {
            case EXECUTIVE -> DIRECT_RULE;
            case SUCCESSION -> APPOINTED;
            case RESOURCE -> CONTRIBUTION_SHARE;
            case TAXATION -> KARMA_TAX;
            case MEMBERSHIP -> INVITE_ONLY;
            case LAND -> OPEN_CLAIM;
        };
    }
}
