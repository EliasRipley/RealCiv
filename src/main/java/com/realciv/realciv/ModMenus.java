package com.realciv.realciv;

import com.realciv.realciv.hub.CommunityHubDepositMenu;
import com.realciv.realciv.hub.RationDraftMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RealCivMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CommunityHubDepositMenu>> HUB_DEPOSIT = MENUS.register(
            "hub_deposit",
            () -> IMenuTypeExtension.create(CommunityHubDepositMenu::fromBuffer));

    public static final DeferredHolder<MenuType<?>, MenuType<RationDraftMenu>> RATION_DRAFT = MENUS.register(
            "ration_draft",
            () -> IMenuTypeExtension.create(RationDraftMenu::fromBuffer));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
