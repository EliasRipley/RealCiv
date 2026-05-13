package com.realciv.realciv.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class RealCivBaseScreen extends Screen {
    protected static final int PANEL_WIDTH = 340;
    protected static final int PANEL_HEIGHT = 220;
    protected static final int CONTENT_LEFT = 14;
    protected static final int CONTENT_RIGHT = PANEL_WIDTH - 14;
    protected static final int CONTENT_TOP = 44;
    protected static final int CONTENT_BOTTOM = PANEL_HEIGHT - 36;
    protected static final int CONTENT_WIDTH = PANEL_WIDTH - 28;
    protected static final int ROW_HEIGHT = 12;

    protected int leftPos;
    protected int topPos;
    protected int scrollOffset;
    protected int maxScroll;

    private final String subtitleText;
    private final int accentColor;

    protected RealCivBaseScreen(Component title, String subtitleText, int accentColor) {
        super(title);
        this.subtitleText = subtitleText;
        this.accentColor = accentColor;
    }

    @Override
    protected void init() {
        leftPos = (width - PANEL_WIDTH) / 2;
        topPos = (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int w = width;
        int h = height;
        graphics.fill(0, 0, w, h, 0xC0000000);
        drawPanel(graphics);
        drawTitle(graphics);
        drawScrollbar(graphics);
        renderContent(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltips(graphics, mouseX, mouseY);
    }

    private void drawPanel(GuiGraphics graphics) {
        int x0 = leftPos;
        int y0 = topPos;
        int x1 = leftPos + PANEL_WIDTH;
        int y1 = topPos + PANEL_HEIGHT;

        graphics.fill(x0, y0, x1, y1, 0xFF14171B);
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, 0xFF252D36);
        graphics.fill(x0 + 6, y0 + 34, x1 - 6, y1 - 6, 0xFF1A2129);
        graphics.fill(x0 + 6, y0 + 34, x1 - 6, y0 + 36, accentColor);
    }

    private void drawTitle(GuiGraphics graphics) {
        graphics.drawString(font, title, leftPos + CONTENT_LEFT, topPos + 8, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal(subtitleText), leftPos + CONTENT_LEFT, topPos + 20, 0xFF9DB0C2, false);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (maxScroll <= 0) return;
        int sbRight = leftPos + PANEL_WIDTH - 10;
        int sbTop = topPos + 40;
        int sbHeight = CONTENT_BOTTOM - 40;
        float ratio = (float) sbHeight / (sbHeight + maxScroll);
        int handleHeight = Math.max(8, (int) (sbHeight * ratio));
        int handlePos = sbTop + (int) ((float) scrollOffset / maxScroll * (sbHeight - handleHeight));

        graphics.fill(sbRight - 1, sbTop, sbRight + 1, sbTop + sbHeight, 0x401A2129);
        graphics.fill(sbRight - 1, handlePos, sbRight + 1, handlePos + handleHeight, 0xB0FFFFFF);
    }

    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInContentArea(mouseX, mouseY)) {
            int prev = scrollOffset;
            scrollOffset = (int) Math.clamp(scrollOffset - scrollY * ROW_HEIGHT * 3, 0, maxScroll);
            return scrollOffset != prev;
        }
        return false;
    }

    protected boolean isInContentArea(double mouseX, double mouseY) {
        return mouseX >= leftPos + 8 && mouseX < leftPos + PANEL_WIDTH - 8
            && mouseY >= topPos + 38 && mouseY < topPos + CONTENT_BOTTOM;
    }

    protected void drawRow(GuiGraphics graphics, int index, String label, String value, int color) {
        int y = CONTENT_TOP + index * ROW_HEIGHT - scrollOffset;
        if (y < 36 || y > CONTENT_BOTTOM - ROW_HEIGHT) return;
        graphics.drawString(font, Component.literal(label), leftPos + CONTENT_LEFT, y, 0xFF9BA9B7, false);
        String trimmed = font.plainSubstrByWidth(value, CONTENT_WIDTH - 120);
        graphics.drawString(font, Component.literal(trimmed), leftPos + CONTENT_LEFT + 120, y, color, false);
    }

    protected void drawRow(GuiGraphics graphics, int index, String label, String value) {
        drawRow(graphics, index, label, value, 0xFFF4F7FA);
    }

    protected Button makeButton(int x, int y, int w, String label, Runnable onClick) {
        return Button.builder(Component.literal(label), b -> onClick.run())
                .bounds(x, y, w, 16)
                .build();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
