package com.realciv.realciv.mixin;

import com.realciv.realciv.logic.CraftingLimitService;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void realciv$mayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(serverPlayer)) {
            return;
        }

        ItemStack result = ((Slot) (Object) this).getItem();
        if (result.isEmpty()) {
            return;
        }

        if (!CraftingLimitService.canTakeCraftingResult(serverPlayer, result)) {
            CraftingLimitService.notifyCraftDenied(serverPlayer);
            cir.setReturnValue(false);
        }
    }
}
