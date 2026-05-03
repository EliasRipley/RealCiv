package com.realciv.realciv.mixin;

import com.realciv.realciv.logic.CraftingLimitService;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void realciv$quickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != 0 || !(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }
        if (RealCivUtil.isBypass(serverPlayer)) {
            return;
        }

        ItemStack result = ((AbstractContainerMenu) (Object) this).slots.getFirst().getItem();
        if (result.isEmpty()) {
            return;
        }

        if (!CraftingLimitService.canTakeCraftingResult(serverPlayer, result)) {
            CraftingLimitService.notifyCraftDenied(serverPlayer);
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
