package com.realciv.realciv.client;

import com.realciv.realciv.hub.CommunityHubDepositMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HubDepositScreen extends AbstractContainerScreen<CommunityHubDepositMenu> {
    private static final int BG_W = 420;
    private static final int BG_H = 266;

    private static final int SLOTS_LEFT = 8;
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int DEPOSIT_ROWS = 6;
    private static final int INV_Y = 140;
    private static final int HOTBAR_Y = 198;
    private static final int LABEL_X = 190;
    private static final int LABEL_W = BG_W - LABEL_X - 18;

    public HubDepositScreen(CommunityHubDepositMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BG_W;
        imageHeight = BG_H;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(2, (width - BG_W) / 2);
        topPos = Math.max(2, (height - BG_H) / 2);
        addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
                .bounds(leftPos + BG_W - 26, topPos + 8, 18, 14)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xB0111318);
        graphics.fill(leftPos, topPos, leftPos + BG_W, topPos + BG_H, 0xFF0F141C);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + BG_W - 3, topPos + BG_H - 3, 0xFF1E2734);
        graphics.fill(leftPos + 8, topPos + 40, leftPos + BG_W - 8, topPos + BG_H - 8, 0xFF141B25);
        graphics.fill(leftPos + 8, topPos + 40, leftPos + BG_W - 8, topPos + 42, 0xFFFF8A3D);
        graphics.fill(leftPos + 190, topPos + 18, leftPos + BG_W - 14, topPos + BG_H - 14, 0x181B2534);
        graphics.fill(leftPos + 190, topPos + 122, leftPos + BG_W - 14, topPos + 124, 0xFF2B3545);

        drawSlotGrid(graphics, leftPos + SLOTS_LEFT, topPos + 18, COLS, DEPOSIT_ROWS);
        drawSlotGrid(graphics, leftPos + SLOTS_LEFT, topPos + INV_Y, COLS, 3);
        drawSlotGrid(graphics, leftPos + SLOTS_LEFT, topPos + HOTBAR_Y, COLS, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal("Community Hub Deposit"),
                14, 8, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal("Contribute goods to gain karma and restore profession actions"),
                14, 22, 0xFF9DB0C2, false);
        String civLabel = menu.civilizationId().isBlank() ? "-" : menu.civilizationId();
        graphics.drawString(font, Component.literal("Civilization: " + civLabel),
                14, 32, 0xFF90CAF9, false);

        int infoY = 18;
        graphics.drawString(font, Component.literal("Deposit Rules"),
                LABEL_X, infoY, 0xFFF2F6FB, false);
        infoY += 14;
        infoY = drawWrapped(graphics, "Accepted stacks convert into civ contribution karma.", LABEL_X, infoY, LABEL_W, 0xFF9DB0C2);
        infoY = drawWrapped(graphics, "Profession actions are restored based on reward tables.", LABEL_X, infoY + 2, LABEL_W, 0xFF9DB0C2);
        infoY = drawWrapped(graphics, "Unaccepted stacks return to your inventory when this closes.", LABEL_X, infoY + 6, LABEL_W, 0xFF78909C);

        int tipsY = 132;
        graphics.drawString(font, Component.literal("Quick Tips"),
                LABEL_X, tipsY, 0xFF9DB0C2, false);
        tipsY += 14;
        tipsY = drawWrapped(graphics, "- Deposit grid: top-left 6x9 slots.", LABEL_X, tipsY, LABEL_W, 0xFF78909C);
        tipsY = drawWrapped(graphics, "- Shift-click from inventory to auto-move.", LABEL_X, tipsY + 2, LABEL_W, 0xFF78909C);
        drawWrapped(graphics, "- Close the screen to finalize the contribution.", LABEL_X, tipsY + 2, LABEL_W, 0xFF78909C);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawSlotGrid(GuiGraphics graphics, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = x + col * SLOT_SIZE;
                int sy = y + row * SLOT_SIZE;
                graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF555555);
                graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0xFF2A2E3A);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        var lines = Minecraft.getInstance().font.split(Component.literal(text), maxWidth);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(Minecraft.getInstance().font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }
}
