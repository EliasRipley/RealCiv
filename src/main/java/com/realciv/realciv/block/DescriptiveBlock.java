package com.realciv.realciv.block;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DescriptiveBlock extends Block {
    private final Component tooltip;

    public DescriptiveBlock(BlockBehaviour.Properties properties, String tooltipKey) {
        super(properties);
        this.tooltip = Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(tooltip);
    }
}
