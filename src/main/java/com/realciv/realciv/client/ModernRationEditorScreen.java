package com.realciv.realciv.client;

import com.realciv.realciv.hub.HubRationSnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import dev.ftb.mods.ftblibrary.ui.IntTextBox;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernRationEditorScreen extends RealCivScreen {
    public static final int ACTION_PREV_PAGE = 1;
    public static final int ACTION_NEXT_PAGE = 2;
    public static final int ACTION_ALLOWANCE_UP = 1000;
    public static final int ACTION_ALLOWANCE_DOWN = 2000;
    public static final int ACTION_ALLOWANCE_CLEAR = 3000;

    private static final int BTN_Y = FOOTER_Y;
    private static final int ROW_BTN_Y_OFFSET = 2;
    private static final int ROW_BTN_H = 14;
    private static final int MAX_ALLOWANCE = 9999;
    private static final int DRAFT_COLS = 9;
    private static final int DRAFT_ROWS = 3;
    private static final int DRAFT_SLOT_COUNT = DRAFT_COLS * DRAFT_ROWS;
    private static final int DRAFT_SLOT_SIZE = 18;
    private static final int DRAFT_SLOT_GAP = 2;

    private HubRationSnapshot snapshot;
    private int heldAllowanceDraft = 16;
    private IntTextBox draftQtyInput;
    private final List<DraftEntry> draftSlots = new ArrayList<>(DRAFT_SLOT_COUNT);
    private boolean draftInitialized;

    public ModernRationEditorScreen(HubRationSnapshot snapshot) {
        super(Component.literal("Ration Editor"), "Daily item allowances for rationed policy", 0xFF26A69A);
        this.snapshot = snapshot;
        for (int i = 0; i < DRAFT_SLOT_COUNT; i++) {
            draftSlots.add(null);
        }
    }

    public void refresh(HubRationSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        this.draftQtyInput = null;
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        SimpleTextButton prevBtn = makeFixedBtn(ACTION_PREV_PAGE, "< Prev", 50);
        prevBtn.setPos(10, BTN_Y);
        add(prevBtn);

        SimpleTextButton nextBtn = makeFixedBtn(ACTION_NEXT_PAGE, "Next >", 50);
        nextBtn.setPos(PANEL_W - 60, BTN_Y);
        add(nextBtn);

        if (snapshot.canManage()) {
            addLocalFixedButton("- Qty", 76, BTN_Y, 40, () -> setHeldAllowanceDraft(getDraftQtyValue() - 1));
            addLocalFixedButton("+ Qty", 120, BTN_Y, 40, () -> setHeldAllowanceDraft(getDraftQtyValue() + 1));
            draftQtyInput = makeDraftQtyInput();
            draftQtyInput.setPosAndSize(164, BTN_Y, 44, BTN_H);
            add(draftQtyInput);
            addLocalFixedButton("Set Held", 212, BTN_Y, 66, this::setAllowanceFromHeldItem);
            addLocalFixedButton("Clear Held", 282, BTN_Y, 72, this::clearAllowanceFromHeldItem);
        }
    }

    @Override
    protected void addScrollContent(Panel panel) {
        ensureDraftInitialized();
        addIdentitySection(snapshot.civDisplayName(), "", snapshot.canManage());
        addSection("Ration Configuration", 0xFF80CBC4);
        addLabelRow("Player", snapshot.playerName());
        addLabelRow("Page", (snapshot.page() + 1) + "/" + Math.max(1, snapshot.totalPages()), 0xFF9DB0C2);
        addLabelRow("Rows", String.valueOf(snapshot.entries().size()), 0xFFB0BEC5);
        if (snapshot.canManage()) {
            addLabelRow("Draft allowance", getDraftQtyValue() + "/day", 0xFFE0F2F1);
            addLabelRow("", "Use chest draft slots to stage many ration entries and apply once.", 0xFF78909C);
        } else {
            addLabelRow("", "View only. Leadership manages ration limits.", 0xFF9E9E9E);
        }

        if (snapshot.canManage()) {
            addSpacer(4);
            addSection("Chest Draft (Ghost Slots)", 0xFF80CBC4);
            addLabelRow("", "Left click with held item = set slot. Empty hand = clear. Mouse wheel/right click adjusts count.", 0xFF78909C);

            int gridX = COL_LABEL;
            int gridY = currentY;
            for (int row = 0; row < DRAFT_ROWS; row++) {
                for (int col = 0; col < DRAFT_COLS; col++) {
                    int slotIndex = row * DRAFT_COLS + col;
                    int x = gridX + col * (DRAFT_SLOT_SIZE + DRAFT_SLOT_GAP);
                    int y = gridY + row * (DRAFT_SLOT_SIZE + DRAFT_SLOT_GAP);
                    panel.add(new DraftSlotWidget(panel, x, y, slotIndex));
                }
            }

            int buttonsX = 196;
            addRowButton(panel, "Apply Draft", buttonsX, gridY, 88, this::applyDraftToServer);
            addRowButton(panel, "Reset Draft", buttonsX + 92, gridY, 88, this::clearDraftLocal);
            addRowButton(panel, "Load Current", buttonsX + 184, gridY, 88, this::reloadDraftFromSnapshot);

            int draftHeight = DRAFT_ROWS * (DRAFT_SLOT_SIZE + DRAFT_SLOT_GAP) - DRAFT_SLOT_GAP;
            currentY += draftHeight + 6;
        }

        addSpacer(4);
        addSection("Allowance Table", 0xFFC6D2DE);
        panel.add(new LabelWidget(panel, "Item", 4, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Stock", 214, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Allow/d", 270, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Actions", 330, currentY, 0xFF78909C));
        currentY += ROW_H;

        for (int i = 0; i < snapshot.entries().size(); i++) {
            HubRationSnapshot.RationRow row = snapshot.entries().get(i);
            panel.add(new RationRowWidget(panel, 4, currentY, row, snapshot.canManage()));
            if (snapshot.canManage()) {
                int rowIndex = i;
                addRowButton(panel, "+", 330, currentY + ROW_BTN_Y_OFFSET, 24, () -> sendAction(ACTION_ALLOWANCE_UP + rowIndex));
                addRowButton(panel, "-", 358, currentY + ROW_BTN_Y_OFFSET, 24, () -> sendAction(ACTION_ALLOWANCE_DOWN + rowIndex));
                addRowButton(panel, "Clear", 386, currentY + ROW_BTN_Y_OFFSET, 58, () -> sendAction(ACTION_ALLOWANCE_CLEAR + rowIndex));
            }
            currentY += ROW_H;
        }

        if (snapshot.entries().isEmpty()) {
            addLabelRow("", "No ration entries configured yet.", 0xFF9E9E9E);
        }
        addSpacer(20);
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_RATION_EDITOR, actionId));
    }

    private void ensureDraftInitialized() {
        if (draftInitialized) {
            return;
        }
        loadDraftFromSnapshot();
        draftInitialized = true;
    }

    private void loadDraftFromSnapshot() {
        for (int i = 0; i < DRAFT_SLOT_COUNT; i++) {
            draftSlots.set(i, null);
        }
        int slot = 0;
        for (HubRationSnapshot.RationRow row : snapshot.entries()) {
            if (row.dailyAllowance() <= 0) {
                continue;
            }
            if (slot >= DRAFT_SLOT_COUNT) {
                break;
            }
            draftSlots.set(slot, new DraftEntry(row.itemId(), clampAllowance(row.dailyAllowance())));
            slot++;
        }
    }

    private void reloadDraftFromSnapshot() {
        loadDraftFromSnapshot();
        refreshWidgets();
    }

    private IntTextBox makeDraftQtyInput() {
        IntTextBox box = new IntTextBox(this) {
            @Override
            public void onEnterPressed() {
                setHeldAllowanceDraft(getIntValue());
            }
        };
        box.setMinMax(1, MAX_ALLOWANCE);
        box.setAmount(clampAllowance(heldAllowanceDraft));
        box.ghostText = "Qty";
        return box;
    }

    private int getDraftQtyValue() {
        if (draftQtyInput != null) {
            heldAllowanceDraft = clampAllowance(draftQtyInput.getIntValue());
        }
        return heldAllowanceDraft;
    }

    private void setHeldAllowanceDraft(int nextValue) {
        heldAllowanceDraft = clampAllowance(nextValue);
        if (draftQtyInput != null) {
            draftQtyInput.setAmount(heldAllowanceDraft);
        }
        refreshWidgets();
    }

    private int clampAllowance(int value) {
        return Math.max(1, Math.min(MAX_ALLOWANCE, value));
    }

    private void setAllowanceFromHeldItem() {
        ItemStack held = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        PacketDistributor.sendToServer(new RealCivPayloads.SetHubAllowancePayload(itemId.toString(), getDraftQtyValue()));
    }

    private void clearAllowanceFromHeldItem() {
        ItemStack held = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        PacketDistributor.sendToServer(new RealCivPayloads.SetHubAllowancePayload(itemId.toString(), 0));
    }

    private void applyDraftToServer() {
        Map<String, Integer> staged = new LinkedHashMap<>();
        for (DraftEntry entry : draftSlots) {
            if (entry == null || entry.dailyAllowance() <= 0) {
                continue;
            }
            staged.put(entry.itemId(), clampAllowance(entry.dailyAllowance()));
        }
        List<String> itemIds = new ArrayList<>(staged.size());
        List<Integer> dailyCounts = new ArrayList<>(staged.size());
        for (Map.Entry<String, Integer> entry : staged.entrySet()) {
            itemIds.add(entry.getKey());
            dailyCounts.add(entry.getValue());
        }
        PacketDistributor.sendToServer(new RealCivPayloads.SetHubAllowanceBatchPayload(itemIds, dailyCounts, true));
    }

    private void clearDraftLocal() {
        for (int i = 0; i < DRAFT_SLOT_COUNT; i++) {
            draftSlots.set(i, null);
        }
        refreshWidgets();
    }

    private void setDraftSlotFromHeld(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= DRAFT_SLOT_COUNT) {
            return;
        }
        ItemStack held = Minecraft.getInstance().player == null ? ItemStack.EMPTY : Minecraft.getInstance().player.getMainHandItem();
        if (held.isEmpty()) {
            draftSlots.set(slotIndex, null);
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        String key = itemId.toString();
        for (int i = 0; i < draftSlots.size(); i++) {
            if (i == slotIndex) {
                continue;
            }
            DraftEntry existing = draftSlots.get(i);
            if (existing != null && existing.itemId().equals(key)) {
                draftSlots.set(i, null);
            }
        }
        draftSlots.set(slotIndex, new DraftEntry(key, getDraftQtyValue()));
    }

    private void adjustDraftSlotAllowance(int slotIndex, int delta) {
        if (slotIndex < 0 || slotIndex >= DRAFT_SLOT_COUNT) {
            return;
        }
        DraftEntry entry = draftSlots.get(slotIndex);
        if (entry == null) {
            return;
        }
        int next = entry.dailyAllowance() + delta;
        if (next <= 0) {
            draftSlots.set(slotIndex, null);
            return;
        }
        draftSlots.set(slotIndex, new DraftEntry(entry.itemId(), clampAllowance(next)));
    }

    private void addLocalFixedButton(String label, int x, int y, int width, Runnable onClick) {
        SimpleTextButton btn = makePanelBtn(this, label, button -> onClick.run());
        btn.setPosAndSize(x, y, width, BTN_H);
        add(btn);
    }

    private void addRowButton(Panel panel, String label, int x, int y, int width, Runnable onClick) {
        SimpleTextButton btn = makePanelBtn(panel, label, button -> onClick.run());
        btn.setPosAndSize(x, y, width, ROW_BTN_H);
        panel.add(btn);
    }

    private record DraftEntry(String itemId, int dailyAllowance) {
    }

    private class DraftSlotWidget extends Widget {
        private final int slotIndex;

        DraftSlotWidget(Panel parent, int x, int y, int slotIndex) {
            super(parent);
            setPosAndSize(x, y, DRAFT_SLOT_SIZE, DRAFT_SLOT_SIZE);
            this.slotIndex = slotIndex;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            graphics.fill(x, y, x + DRAFT_SLOT_SIZE, y + DRAFT_SLOT_SIZE, 0xFF555555);
            graphics.fill(x + 1, y + 1, x + DRAFT_SLOT_SIZE - 1, y + DRAFT_SLOT_SIZE - 1, 0xFF2A2E3A);
            DraftEntry entry = draftSlots.get(slotIndex);
            if (entry != null) {
                try {
                    ResourceLocation itemId = ResourceLocation.parse(entry.itemId());
                    Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
                    if (item != Items.AIR) {
                        ItemStack stack = new ItemStack(item);
                        graphics.renderItem(stack, x + 1, y + 1);
                        if (entry.dailyAllowance() > 1) {
                            graphics.renderItemDecorations(
                                    Minecraft.getInstance().font,
                                    new ItemStack(item, entry.dailyAllowance()),
                                    x + 1,
                                    y + 1);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (isMouseOver()) {
                graphics.fill(x, y, x + DRAFT_SLOT_SIZE, y + DRAFT_SLOT_SIZE, 0x40FFFFFF);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (!isMouseOver()) {
                return false;
            }
            if (button.isRight()) {
                adjustDraftSlotAllowance(slotIndex, -1);
            } else {
                setDraftSlotFromHeld(slotIndex);
            }
            refreshWidgets();
            return true;
        }

        @Override
        public boolean mouseScrolled(double amount) {
            if (!isMouseOver()) {
                return false;
            }
            int delta = amount > 0 ? 1 : -1;
            adjustDraftSlotAllowance(slotIndex, delta);
            refreshWidgets();
            return true;
        }
    }

    private static class RationRowWidget extends Widget {
        private final HubRationSnapshot.RationRow row;
        private final boolean canManage;

        RationRowWidget(Panel parent, int x, int y, HubRationSnapshot.RationRow row, boolean canManage) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.row = row;
            this.canManage = canManage;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver() && canManage) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(row.itemName(), 200)),
                    x + 4, y + 2, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.stockAvailable())),
                    x + 214, y + 2, 0xFFB0BEC5, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.dailyAllowance())),
                    x + 270, y + 2, 0xFFFFD54F, false);
        }
    }
}
