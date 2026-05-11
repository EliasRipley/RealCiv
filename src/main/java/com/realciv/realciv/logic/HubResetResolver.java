package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class HubResetResolver {
    private HubResetResolver() {
    }

    public static int resolveActionsRestored(ItemStack stack, Profession profession) {
        if (stack.isEmpty() || profession == Profession.NONE) {
            return 0;
        }

        int count = Math.max(0, stack.getCount());
        if (count <= 0) {
            return 0;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Map<ResourceLocation, ItemResetRule> itemRules = RealCivConfig.itemResetRules();
        ItemResetRule itemRule = itemRules.get(itemId);
        if (itemRule != null && itemRule.profession() == profession) {
            double actions = Math.max(0.0D, itemRule.actionsPerItem()) * count;
            return Math.max(0, (int) Math.floor(actions));
        }

        List<TagResetRule> rules = RealCivConfig.tagResetRules();
        for (TagResetRule rule : rules) {
            if (rule.profession() != profession) {
                continue;
            }
            if (!HubRewardResolver.matchesSelector(stack, rule.selectorType(), rule.tagId())) {
                continue;
            }
            double actions = Math.max(0.0D, rule.actionsPerItem()) * count;
            return Math.max(0, (int) Math.floor(actions));
        }
        return 0;
    }
}
