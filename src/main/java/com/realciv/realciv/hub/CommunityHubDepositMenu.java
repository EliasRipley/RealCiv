package com.realciv.realciv.hub;

import com.realciv.realciv.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CommunityHubDepositMenu extends AbstractContainerMenu {
    private static final int DEPOSIT_SLOTS = 54;
    private final CommunityHubDepositContainer deposit;

    public CommunityHubDepositMenu(int containerId, Inventory inventory, CommunityHubDepositContainer deposit) {
        super(ModMenus.HUB_DEPOSIT.get(), containerId);
        this.deposit = deposit;

        int slotIndex = 0;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(deposit, slotIndex++, 8 + col * 18, 18 + row * 18));
            }
        }

        int invStartY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, invStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, invStartY + 58));
        }
    }

    public static CommunityHubDepositMenu fromBuffer(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new CommunityHubDepositMenu(containerId, inventory, new CommunityHubDepositContainer(""));
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot clicked = this.slots.get(index);
        if (!clicked.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = clicked.getItem();
        original = moved.copy();
        if (index < DEPOSIT_SLOTS) {
            if (!this.moveItemStackTo(moved, DEPOSIT_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(moved, 0, DEPOSIT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (moved.isEmpty()) {
            clicked.setByPlayer(ItemStack.EMPTY);
        } else {
            clicked.setChanged();
        }
        if (moved.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        clicked.onTake(player, moved);
        return original;
    }

    @Override
    public void removed(@NotNull Player player) {
        if (!player.level().isClientSide()) {
            deposit.stopOpen(player);
        }
        super.removed(player);
    }
}
