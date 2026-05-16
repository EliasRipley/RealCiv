package com.realciv.realciv.client;

import com.realciv.realciv.hub.CommunityHubDepositMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HubDepositScreen extends AbstractContainerScreen<CommunityHubDepositMenu> {
    private static final int BG_W = 420;
    private static final int BG_H = 256;

    private static final int SLOTS_LEFT = 8;
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int DEPOSIT_ROWS = 6;
    private static final int INV_Y = 140;
    private static final int HOTBAR_Y = 198;
    private static final int LABEL_X = 190;

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
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xC0000000);
        graphics.fill(leftPos, topPos, leftPos + BG_W, topPos + BG_H, 0xFF14171B);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + BG_W - 3, topPos + BG_H - 3, 0xFF252D36);
        graphics.fill(leftPos + 8, topPos + 40, leftPos + BG_W - 8, topPos + BG_H - 8, 0xFF1A2129);
        graphics.fill(leftPos + 8, topPos + 40, leftPos + BG_W - 8, topPos + 42, 0xFFFF8A3D);

        drawSlotGrid(graphics, leftPos + SLOTS_LEFT, topPos + 18, COLS, DEPOSIT_ROWS);
        drawSlotGrid(graphics, leftPos + SLOTS_LEFT, topPos + INV_Y, COLS, 3);
        drawSlotGrid(graphics, leftPos + SLOTS_LEFT, topPos + HOTBAR_Y, COLS, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal("Community Hub Deposit"),
                leftPos + 14 - leftPos, topPos + 8 - topPos, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal("Items are returned on close if not accepted"),
                leftPos + 14 - leftPos, topPos + 22 - topPos, 0xFF9DB0C2, false);

        // Right-side labels
        graphics.drawString(font, Component.literal("Deposit Area"),
                LABEL_X, 18, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal("Place items here to"),
                LABEL_X, 32, 0xFF9DB0C2, false);
        graphics.drawString(font, Component.literal("contribute to the"),
                LABEL_X, 44, 0xFF9DB0C2, false);
        graphics.drawString(font, Component.literal("community hub."),
                LABEL_X, 56, 0xFF9DB0C2, false);
        graphics.drawString(font, Component.literal("Close the screen to"),
                LABEL_X, 72, 0xFF78909C, false);
        graphics.drawString(font, Component.literal("finalize deposit."),
                LABEL_X, 84, 0xFF78909C, false);

        graphics.drawString(font, Component.literal("Your Inventory"),
                LABEL_X, INV_Y - 6, 0xFF9DB0C2, false);

        graphics.drawString(font, Component.literal("Drag items from your"),
                LABEL_X, HOTBAR_Y + 4, 0xFF78909C, false);
        graphics.drawString(font, Component.literal("inventory into the"),
                LABEL_X, HOTBAR_Y + 16, 0xFF78909C, false);
        graphics.drawString(font, Component.literal("deposit area above."),
                LABEL_X, HOTBAR_Y + 28, 0xFF78909C, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBg(graphics, delta, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
        renderLabels(graphics, mouseX, mouseY);
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
}
