package com.realciv.realciv;

import com.realciv.realciv.panel.CivControlPanelMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RealCivMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CivControlPanelMenu>> CIV_CONTROL_PANEL = MENUS.register(
            "civ_control_panel",
            () -> IMenuTypeExtension.create(CivControlPanelMenu::fromBuffer));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
