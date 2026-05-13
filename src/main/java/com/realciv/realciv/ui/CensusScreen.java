package com.realciv.realciv.ui;

import com.realciv.realciv.census.CensusMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CensusScreen extends AbstractContainerScreen<CensusMenu> {
    private static final int BG_WIDTH = 352;
    private static final int BG_HEIGHT = 236;
    private static final int CONTENT_X = 14;
    private static final int CONTENT_Y = 46;
    private static final int ROW_STEP = 13;
    private static final int LABEL_COLOR = 0xFFCBD3D9;
    private static final int VALUE_COLOR = 0xFFF4F7FA;
    private static final int HEADER_COLOR = 0xFF4CAF50;
    private static final int SUBTLE_COLOR = 0xFF9BA9B7;

    public CensusScreen(CensusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        int x1 = leftPos + imageWidth;
        int y1 = topPos + imageHeight;

        guiGraphics.fill(x0, y0, x1, y1, 0xE01D2228);
        guiGraphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 0xF02D3640);
        guiGraphics.fill(x0 + 8, y0 + 40, x1 - 8, y1 - 60, 0xF0222A33);
        guiGraphics.fill(x0 + 8, y0 + 40, x1 - 8, y0 + 41, 0xFF3D4C5A);
        guiGraphics.fill(x0 + 8, y1 - 56, x1 - 8, y1 - 8, 0xF01B2128);
        guiGraphics.fill(x0 + 8, y1 - 56, x1 - 8, y1 - 55, 0xFF3D4C5A);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(
                font,
                Component.literal("Census Bureau").withStyle(s -> s.withColor(HEADER_COLOR).withBold(true)),
                CONTENT_X,
                8,
                VALUE_COLOR,
                false);
        
        guiGraphics.drawString(
                font,
                Component.literal("Manage civilization membership, requests, and invitations"),
                CONTENT_X,
                20,
                SUBTLE_COLOR,
                false);

        guiGraphics.drawString(
                font,
                Component.literal("Use the inventory slots to interact with members and requests"),
                CONTENT_X,
                168,
                SUBTLE_COLOR,
                false);
    }
}
