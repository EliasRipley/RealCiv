package com.realciv.realciv.logic;

import net.minecraft.resources.ResourceLocation;

public record ItemResetRule(
        ResourceLocation itemId,
        Profession profession,
        double actionsPerItem) {
}

