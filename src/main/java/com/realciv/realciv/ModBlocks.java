package com.realciv.realciv;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

    public static final DeferredBlock<Block> CENSUS_BLOCK = BLOCKS.register(
            "census_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> CENSUS_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("census_block", CENSUS_BLOCK);

    public static final DeferredBlock<Block> TAX_BLOCK = BLOCKS.register(
            "tax_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> TAX_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("tax_block", TAX_BLOCK);

    public static final DeferredItem<Item> LAND_WAND = ITEMS.register(
            "land_wand",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
