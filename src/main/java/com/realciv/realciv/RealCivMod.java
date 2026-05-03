package com.realciv.realciv;

import com.mojang.logging.LogUtils;
import com.realciv.realciv.command.RealCivCommands;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.event.RealCivEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(RealCivMod.MOD_ID)
public class RealCivMod {
    public static final String MOD_ID = "realciv";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealCivMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabContents);
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);

        modContainer.registerConfig(ModConfig.Type.COMMON, RealCivConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(RealCivCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemPickupPre);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onServerTick);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == RealCivConfig.SPEC) {
            RealCivConfig.migrateLegacyCommonConfigIfNeeded();
        }
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == RealCivConfig.SPEC) {
            RealCivConfig.migrateLegacyCommonConfigIfNeeded();
        }
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.COMMUNITY_HUB_ITEM);
            event.accept(ModBlocks.CENSUS_BLOCK_ITEM);
            event.accept(ModBlocks.TAX_BLOCK_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModBlocks.LAND_WAND);
        }
    }
}
