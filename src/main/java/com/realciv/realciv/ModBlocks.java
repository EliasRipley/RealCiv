package com.realciv.realciv;

import com.realciv.realciv.block.DescriptiveBlock;
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
            () -> new DescriptiveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops(),
                    "tooltip.realciv.community_hub"));

    public static final DeferredItem<BlockItem> COMMUNITY_HUB_ITEM =
            ITEMS.registerSimpleBlockItem("community_hub", COMMUNITY_HUB);

    public static final DeferredBlock<Block> CENSUS_BLOCK = BLOCKS.register(
            "census_block",
            () -> new DescriptiveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops(),
                    "tooltip.realciv.census_block"));

    public static final DeferredItem<BlockItem> CENSUS_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("census_block", CENSUS_BLOCK);

    public static final DeferredBlock<Block> TAX_BLOCK = BLOCKS.register(
            "tax_block",
            () -> new DescriptiveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops(),
                    "tooltip.realciv.tax_block"));

    public static final DeferredItem<BlockItem> TAX_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("tax_block", TAX_BLOCK);

    public static final DeferredBlock<Block> CIVIC_CONTROL_CONSOLE = BLOCKS.register(
            "civic_control_console",
            () -> new DescriptiveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops(),
                    "tooltip.realciv.civic_control_console"));

    public static final DeferredItem<BlockItem> CIVIC_CONTROL_CONSOLE_ITEM =
            ITEMS.registerSimpleBlockItem("civic_control_console", CIVIC_CONTROL_CONSOLE);

    public static final DeferredBlock<Block> PROFESSION_LEDGER = BLOCKS.register(
            "profession_ledger",
            () -> new DescriptiveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F, 4.0F)
                    .requiresCorrectToolForDrops(),
                    "tooltip.realciv.profession_ledger"));

    public static final DeferredItem<BlockItem> PROFESSION_LEDGER_ITEM =
            ITEMS.registerSimpleBlockItem("profession_ledger", PROFESSION_LEDGER);

    public static final DeferredBlock<Block> WAR_TABLE = BLOCKS.register(
            "war_table",
            () -> new DescriptiveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops(),
                    "tooltip.realciv.war_table"));

    public static final DeferredItem<BlockItem> WAR_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem("war_table", WAR_TABLE);

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
