package com.realciv.realciv.logic;

import net.minecraft.resources.ResourceLocation;

public record RewardRule(
        ResourceLocation itemId,
        Profession profession,
        long creditsPerItemCents,
        int professionXpPerItem,
        int generalXpPerItem) {
}
