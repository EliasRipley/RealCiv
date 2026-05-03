package com.realciv.realciv;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealCivMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealCivMod.MOD_ID);

    public static final DeferredBlock<Block> COMMUNITY_HUB = BLOCKS.register(
            "community_hub",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> COMMUNITY_HUB_ITEM =
            ITEMS.registerSimpleBlockItem("community_hub", COMMUNITY_HUB);

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
