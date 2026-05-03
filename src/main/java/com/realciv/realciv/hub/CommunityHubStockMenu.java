package com.realciv.realciv.hub;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CommunityHubStockMenu extends AbstractContainerMenu {
    private static final int HUB_SLOT_COUNT = 54;
    private static final int PAGE_BODY_SLOTS = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final net.minecraft.world.SimpleContainer display = new net.minecraft.world.SimpleContainer(HUB_SLOT_COUNT);
    private final ServerPlayer viewer;
    private final CivSavedData data;
    private final String civilizationId;
    private final boolean privileged;
    private final Map<Integer, ResourceLocation> slotItems = new HashMap<>();
    private int pageIndex;
    private int totalPages = 1;

    public CommunityHubStockMenu(
            int containerId,
            Inventory inventory,
            ServerPlayer viewer,
            CivSavedData data,
            String civilizationId,
            boolean privileged) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = viewer;
        this.data = data;
        this.civilizationId = civilizationId;
        this.privileged = privileged;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9;
                this.addSlot(new ReadOnlySlot(display, slot, 8 + col * 18, 18 + row * 18));
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

        refresh();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= this.slots.size()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (slotId >= HUB_SLOT_COUNT) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.getUUID().equals(viewer.getUUID())) {
            return;
        }

        if (slotId == PREV_SLOT) {
            if (pageIndex > 0) {
                pageIndex--;
                refresh();
            }
            return;
        }
        if (slotId == NEXT_SLOT) {
            if (pageIndex + 1 < totalPages) {
                pageIndex++;
                refresh();
            }
            return;
        }
        if (slotId == INFO_SLOT) {
            refresh();
            return;
        }

        ResourceLocation itemId = slotItems.get(slotId);
        if (itemId == null) {
            return;
        }

        withdrawFromSlot(itemId, button, clickType);
    }

    private void withdrawFromSlot(ResourceLocation itemId, int button, ClickType clickType) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            refresh();
            return;
        }

        long stock = data.getHubStock(civilizationId, itemId);
        if (stock <= 0L) {
            refresh();
            return;
        }

        long requested = requestSize(item, button, clickType);
        if (requested <= 0L) {
            return;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        if (!privileged) {
            long allowance = record.remainingPersonalWithdraw(civilizationId, itemId);
            if (allowance <= 0L) {
                RealCivMessages.deny(
                        viewer,
                        "No personal withdrawal allowance left for " + itemId
                                + ". Contribute more to increase quota.");
                refresh();
                return;
            }
            requested = Math.min(requested, allowance);
        }

        requested = Math.min(requested, stock);
        if (requested <= 0L) {
            refresh();
            return;
        }

        if (!data.tryWithdrawFromHub(civilizationId, itemId, requested)) {
            RealCivMessages.deny(viewer, "Hub stock changed before withdrawal could complete. Try again.");
            refresh();
            return;
        }

        long remainingToGive = requested;
        while (remainingToGive > 0L) {
            int stackSize = (int) Math.min(item.getDefaultMaxStackSize(), remainingToGive);
            ItemStack stack = new ItemStack(item, stackSize);
            if (!viewer.getInventory().add(stack)) {
                viewer.drop(stack, false);
            }
            remainingToGive -= stackSize;
        }

        if (!privileged) {
            record.recordPersonalWithdrawal(civilizationId, itemId, requested);
        }

        data.addAuditLog(
                civilizationId,
                viewer.getGameProfile().getName() + " withdrew " + requested + "x " + itemId + " via hub menu",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        long remainingPersonal = record.remainingPersonalWithdraw(civilizationId, itemId);
        viewer.sendSystemMessage(Component.literal(
                "Withdrew " + requested + "x " + itemId
                        + " | Remaining personal quota: " + remainingPersonal));
        refresh();
    }

    private long requestSize(Item item, int button, ClickType clickType) {
        int maxStack = Math.max(1, item.getDefaultMaxStackSize());
        if (clickType == ClickType.QUICK_MOVE) {
            return (long) maxStack * 4L;
        }
        if (clickType == ClickType.PICKUP && button == 1) {
            return 1L;
        }
        return maxStack;
    }

    private void refresh() {
        slotItems.clear();
        for (int i = 0; i < HUB_SLOT_COUNT; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }

        List<Map.Entry<String, Long>> stockEntries = data.getHubStockEntriesSorted(civilizationId);
        int pageSize = PAGE_BODY_SLOTS;
        totalPages = Math.max(1, (stockEntries.size() + pageSize - 1) / pageSize);
        pageIndex = Math.max(0, Math.min(pageIndex, totalPages - 1));

        int start = pageIndex * pageSize;
        int end = Math.min(stockEntries.size(), start + pageSize);
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());

        int slot = 0;
        for (int i = start; i < end; i++) {
            Map.Entry<String, Long> entry = stockEntries.get(i);
            ResourceLocation itemId;
            try {
                itemId = ResourceLocation.parse(entry.getKey());
            } catch (Exception ignored) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) {
                continue;
            }

            long stock = Math.max(0L, entry.getValue());
            ItemStack stack = new ItemStack(item, (int) Math.min(Math.max(1L, stock), item.getDefaultMaxStackSize()));

            if (privileged) {
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                        itemId + " | stock " + stock
                                + " | Left click: stack, Right click: 1, Shift click: x4 stacks"));
            } else {
                long contributed = record.contributedCount(civilizationId, itemId);
                long limit = record.personalWithdrawLimit(civilizationId, itemId);
                long withdrawn = record.personalWithdrawnCount(civilizationId, itemId);
                long remaining = record.remainingPersonalWithdraw(civilizationId, itemId);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                        itemId + " | stock " + stock
                                + " | contributed " + contributed
                                + " | withdrawn " + withdrawn
                                + " | remaining " + remaining + "/" + limit));
            }

            display.setItem(slot, stack);
            slotItems.put(slot, itemId);
            slot++;
        }

        for (int i = 45; i < HUB_SLOT_COUNT; i++) {
            if (display.getItem(i).isEmpty()) {
                ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                display.setItem(i, pane);
            }
        }

        ItemStack previous = new ItemStack(Items.ARROW);
        previous.set(DataComponents.CUSTOM_NAME, Component.literal("Previous Page"));
        display.setItem(PREV_SLOT, previous);

        ItemStack next = new ItemStack(Items.ARROW);
        next.set(DataComponents.CUSTOM_NAME, Component.literal("Next Page"));
        display.setItem(NEXT_SLOT, next);

        ItemStack info = new ItemStack(Items.BOOK);
        info.set(DataComponents.CUSTOM_NAME, Component.literal(
                "Civilization: " + civilizationId
                        + " | page " + (pageIndex + 1) + "/" + totalPages
                        + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civilizationId))));
        display.setItem(INFO_SLOT, info);

        this.broadcastChanges();
    }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
