package com.realciv.realciv.hub;

import com.realciv.realciv.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RationDraftMenu extends AbstractContainerMenu {
    public static final int ACTION_APPLY_AND_CLOSE = 0;

    private static final int DRAFT_SLOTS = RationDraftContainer.DRAFT_SLOT_COUNT;
    private static final int MAX_ITEM_ID_LEN = 128;
    private final RationDraftContainer draft;
    private final String civilizationId;
    private final List<AllowancePreviewEntry> allowancePreview;

    public RationDraftMenu(int containerId, Inventory inventory, RationDraftContainer draft) {
        this(containerId, inventory, draft, List.of());
    }

    public RationDraftMenu(
            int containerId,
            Inventory inventory,
            RationDraftContainer draft,
            List<AllowancePreviewEntry> allowancePreview) {
        super(ModMenus.RATION_DRAFT.get(), containerId);
        this.draft = draft;
        this.civilizationId = draft.civilizationId();
        this.allowancePreview = allowancePreview == null ? List.of() : List.copyOf(allowancePreview);

        int draftStartY = 54;
        int slotIndex = 0;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(draft, slotIndex++, 8 + col * 18, draftStartY + row * 18));
            }
        }

        int invStartY = 168;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, invStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, invStartY + 58));
        }
    }

    public static RationDraftMenu fromBuffer(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        String civId = buffer.readableBytes() > 0 ? buffer.readUtf(128) : "";
        List<AllowancePreviewEntry> entries = List.of();
        if (buffer.readableBytes() > 0) {
            int declaredSize = Math.max(0, Math.min(64, buffer.readVarInt()));
            List<AllowancePreviewEntry> loaded = new ArrayList<>(declaredSize);
            for (int i = 0; i < declaredSize; i++) {
                if (buffer.readableBytes() <= 0) {
                    break;
                }
                String itemId = buffer.readUtf(MAX_ITEM_ID_LEN);
                int allowance = buffer.readVarInt();
                loaded.add(new AllowancePreviewEntry(itemId, Math.max(0, allowance)));
            }
            entries = List.copyOf(loaded);
        }
        return new RationDraftMenu(containerId, inventory, new RationDraftContainer(civId), entries);
    }

    public String civilizationId() {
        return civilizationId;
    }

    public List<AllowancePreviewEntry> allowancePreview() {
        return allowancePreview;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == ACTION_APPLY_AND_CLOSE) {
            draft.markApplyRequested();
            player.closeContainer();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot clicked = this.slots.get(index);
        if (!clicked.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = clicked.getItem();
        ItemStack original = moved.copy();
        if (index < DRAFT_SLOTS) {
            if (!this.moveItemStackTo(moved, DRAFT_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(moved, 0, DRAFT_SLOTS, false)) {
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
            draft.stopOpen(player);
        }
        super.removed(player);
    }

    public record AllowancePreviewEntry(String itemId, int dailyAllowance) {
    }
}
