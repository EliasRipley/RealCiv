package com.realciv.realciv.logic;

import net.minecraft.resources.ResourceLocation;

public record TagRewardRule(
        SelectorType selectorType,
        ResourceLocation tagId,
        Profession profession,
        long creditsPerItemCents,
        int professionXpPerItem,
        int generalXpPerItem) {

    public RewardRule toRewardRule(ResourceLocation itemId) {
        return new RewardRule(
                itemId,
                profession,
                creditsPerItemCents,
                professionXpPerItem,
                generalXpPerItem);
    }

    public enum SelectorType {
        BLOCK_TAG,
        ITEM_TAG;

        public static SelectorType fromConfig(String raw) {
            if (raw == null) {
                return null;
            }
            return switch (raw.trim().toUpperCase(java.util.Locale.ROOT)) {
                case "BLOCK_TAG", "BLOCK" -> BLOCK_TAG;
                case "ITEM_TAG", "ITEM" -> ITEM_TAG;
                default -> null;
            };
        }
    }
}
