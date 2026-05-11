package com.realciv.realciv.logic;

import net.minecraft.resources.ResourceLocation;

public record TagResetRule(
        TagRewardRule.SelectorType selectorType,
        ResourceLocation tagId,
        Profession profession,
        double actionsPerItem) {
}

