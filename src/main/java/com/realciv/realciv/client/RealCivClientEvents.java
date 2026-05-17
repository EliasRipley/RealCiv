package com.realciv.realciv.client;

import com.realciv.realciv.ModMenus;
import com.realciv.realciv.RealCivMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = RealCivMod.MOD_ID, value = Dist.CLIENT)
public final class RealCivClientEvents {
    private RealCivClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.HUB_DEPOSIT.get(), HubDepositScreen::new);
        event.register(ModMenus.RATION_DRAFT.get(), RationDraftScreen::new);
    }
}
