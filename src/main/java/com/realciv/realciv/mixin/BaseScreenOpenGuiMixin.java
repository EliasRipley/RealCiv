package com.realciv.realciv.mixin;

import com.realciv.realciv.client.RealCivScreen;
import com.realciv.realciv.client.ResponsiveRealCivScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ContextMenu;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseScreen.class)
public abstract class BaseScreenOpenGuiMixin {
    @Shadow
    public abstract void openContextMenu(ContextMenu newContextMenu);

    @Inject(method = "openGui", at = @At("HEAD"), cancellable = true)
    private void realciv$openResponsiveWrapper(CallbackInfo ci) {
        if ((Object) this instanceof RealCivScreen realCivScreen) {
            openContextMenu((ContextMenu) null);
            Minecraft.getInstance().setScreen(new ResponsiveRealCivScreenWrapper(realCivScreen));
            ci.cancel();
        }
    }
}
