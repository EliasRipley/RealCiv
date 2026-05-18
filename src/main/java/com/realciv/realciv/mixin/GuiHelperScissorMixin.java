package com.realciv.realciv.mixin;

import com.realciv.realciv.client.RealCivScreenScaleContext;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiHelper.class)
public class GuiHelperScissorMixin {
    @ModifyVariable(method = "pushScissor", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static int realciv$mapScissorX(int x) {
        return RealCivScreenScaleContext.mapScissorX(x);
    }

    @ModifyVariable(method = "pushScissor", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static int realciv$mapScissorY(int y) {
        return RealCivScreenScaleContext.mapScissorY(y);
    }

    @ModifyVariable(method = "pushScissor", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private static int realciv$mapScissorW(int w) {
        return RealCivScreenScaleContext.mapScissorW(w);
    }

    @ModifyVariable(method = "pushScissor", at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private static int realciv$mapScissorH(int h) {
        return RealCivScreenScaleContext.mapScissorH(h);
    }
}
