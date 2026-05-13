package com.realciv.realciv.client;

import com.realciv.realciv.hub.CommunityHubDepositMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HubDepositScreen extends AbstractContainerScreen<CommunityHubDepositMenu> {
    private static final int BG_WIDTH = 352;
    private static final int BG_HEIGHT = 236;

    public HubDepositScreen(CommunityHubDepositMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.inventoryLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int w = width;
        int h = height;
        graphics.fill(0, 0, w, h, 0xC0000000);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        int x1 = leftPos + BG_WIDTH;
        int y1 = topPos + BG_HEIGHT;

        graphics.fill(x0, y0, x1, y1, 0xFF14171B);
        graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 0xFF252D36);
        graphics.fill(x0 + 8, y0 + 40, x1 - 8, y1 - 60, 0xFF1A2129);
        graphics.fill(x0 + 8, y0 + 40, x1 - 8, y0 + 42, 0xFFFF8A3D);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Community Hub Deposit"), 14, 8, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal("Place items to contribute. Close to finalize."), 14, 22, 0xFF9DB0C2, false);
    }
}
