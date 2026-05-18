package com.realciv.realciv;

import com.mojang.logging.LogUtils;
import com.realciv.realciv.command.RealCivCommands;
import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.event.RealCivEvents;
import com.realciv.realciv.integration.RealCivFTBChunksBridge;
import com.realciv.realciv.network.RealCivNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(RealCivMod.MOD_ID)
public class RealCivMod {
    public static final String MOD_ID = "realciv";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealCivMod(IEventBus modEventBus, ModContainer modContainer) {
        // Check FTB Chunks version before any integration code runs.
        checkFTBChunksVersion();

        // Register blocks, menus, and network payloads on the mod event bus.
        ModBlocks.register(modEventBus);
        ModMenus.register(modEventBus);
        modEventBus.addListener(RealCivNetwork::registerPayloads);
        modEventBus.addListener(this::addCreativeTabContents);
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);

        modContainer.registerConfig(ModConfig.Type.COMMON, RealCivConfig.SPEC);

        // Register FTB Chunks integration (claim interceptors).
        RealCivFTBChunksBridge.register();

        // Register all game event handlers on the NeoForge event bus.
        NeoForge.EVENT_BUS.addListener(RealCivCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onAnimalBreed);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onAnimalTame);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemFished);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBlockToolModification);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onFarmlandTrample);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onUseItemOnBlock);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onBonemealUse);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onVillagerTrade);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onAnvilRepair);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemSmelted);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemEnchanted);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onPotionBrewed);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemToss);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onStatAward);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onExplosionStart);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onItemPickupPre);
        NeoForge.EVENT_BUS.addListener(RealCivEvents::onServerTick);
    }

    // Verifies the installed FTB Chunks version is on the expected 1.21.1 line (2101.1.x)
    // and warns if it predates the minimum recommended version (2101.1.14).
    private static void checkFTBChunksVersion() {
        ModList.get().getModContainerById("ftbchunks").ifPresentOrElse(container -> {
            String version = container.getModInfo().getVersion().toString();
            if (version.startsWith("2101.1.")) {
                int patch = 0;
                try {
                    patch = Integer.parseInt(version.substring("2101.1.".length()));
                } catch (NumberFormatException ignored) {}
                if (patch < 14) {
                    LOGGER.warn("FTB Chunks version {} is older than recommended 2101.1.14. Upgrade recommended for compatibility.", version);
                }
            } else {
                LOGGER.warn("FTB Chunks version {} may be incompatible. Expected 2101.1.x (1.21.1 line).", version);
            }
        }, () -> LOGGER.error("FTB Chunks mod (ftbchunks) not found! RealCiv requires FTB Chunks to be installed."));
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == RealCivConfig.SPEC) {
            RealCivConfig.migrateLegacyCommonConfigIfNeeded();
            RealCivConfig.invalidateExternalRuleFileCache();
        }
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == RealCivConfig.SPEC) {
            RealCivConfig.migrateLegacyCommonConfigIfNeeded();
            RealCivConfig.invalidateExternalRuleFileCache();
        }
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.COMMUNITY_HUB_ITEM);
            event.accept(ModBlocks.CENSUS_BLOCK_ITEM);
            event.accept(ModBlocks.TAX_BLOCK_ITEM);
            event.accept(ModBlocks.CIVIC_CONTROL_CONSOLE_ITEM);
            event.accept(ModBlocks.PROFESSION_LEDGER_ITEM);
            event.accept(ModBlocks.WAR_TABLE_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModBlocks.LAND_WAND);
        }
    }
}
