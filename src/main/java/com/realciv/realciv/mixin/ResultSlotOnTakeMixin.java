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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotOnTakeMixin {
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void realciv$onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(serverPlayer)) {
            return;
        }

        if (!CraftingLimitService.canTakeCraftingResult(serverPlayer, stack)) {
            CraftingLimitService.notifyCraftDenied(serverPlayer, stack);
            ((Slot) (Object) this).set(stack);
            serverPlayer.containerMenu.setCarried(ItemStack.EMPTY);
            ci.cancel();
        }
    }
}
