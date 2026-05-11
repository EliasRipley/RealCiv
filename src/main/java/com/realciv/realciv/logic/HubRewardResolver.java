package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public final class HubRewardResolver {
    private HubRewardResolver() {
    }

    @Nullable
    public static RewardRule resolveEffectiveRewardRule(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Map<ResourceLocation, RewardRule> configuredRules = RealCivConfig.rewardRules();
        RewardRule configured = configuredRules.get(itemId);
        if (configured != null) {
            return configured;
        }

        List<TagRewardRule> tagRules = RealCivConfig.tagRewardRules();
        for (TagRewardRule tagRule : tagRules) {
            if (!matchesTagRule(stack, tagRule)) {
                continue;
            }
            return tagRule.toRewardRule(itemId);
        }

        return null;
    }

    public static boolean matchesTagRule(ItemStack stack, TagRewardRule tagRule) {
        return matchesSelector(stack, tagRule.selectorType(), tagRule.tagId());
    }

    public static boolean matchesSelector(
            ItemStack stack,
            TagRewardRule.SelectorType selectorType,
            ResourceLocation tagId) {
        if (selectorType == TagRewardRule.SelectorType.BLOCK_TAG) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return false;
            }
            TagKey<Block> blockTag = TagKey.create(Registries.BLOCK, tagId);
            return blockItem.getBlock().defaultBlockState().is(blockTag);
        }

        if (selectorType == TagRewardRule.SelectorType.ITEM_TAG) {
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagId);
            return stack.is(itemTag);
        }

        return false;
    }
}
