package com.realciv.realciv.client;

import com.daqem.uilib.api.client.gui.component.scroll.ScrollOrientation;
import com.daqem.uilib.client.gui.AbstractScreen;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.SolidColorComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollBarComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollWheelComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class RealCivUIScreen extends AbstractScreen {
    protected static final int PANEL_WIDTH = 400;
    protected static final int PANEL_HEIGHT = 240;

    protected static final int CONTENT_X = 4;
    protected static final int CONTENT_WIDTH = PANEL_WIDTH - 44;
    protected static final int COL_VALUE = 200;
    protected static final int ROW_HEIGHT = 12;

    private final String subtitleText;
    private final int accentColor;
    protected int panelX;
    protected int panelY;

    public RealCivUIScreen(Component title, String subtitle, int accentColor) {
        super(title);
        this.subtitleText = subtitle;
        this.accentColor = accentColor;
    }

    @Override
    public void startScreen() {
        setBackground(null);
        panelX = Math.max(2, (width - PANEL_WIDTH) / 2);
        panelY = Math.max(2, (height - PANEL_HEIGHT) / 2);

        addComponent(new SolidColorComponent(0, 0, width, height, 0xC0000000));
        addComponent(new SolidColorComponent(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF14171B));
        addComponent(new SolidColorComponent(panelX + 2, panelY + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, 0xFF252D36));
        addComponent(new SolidColorComponent(panelX + 6, panelY + 34, PANEL_WIDTH - 12, PANEL_HEIGHT - 40, 0xFF1A2129));
        addComponent(new SolidColorComponent(panelX + 6, panelY + 34, PANEL_WIDTH - 12, 2, accentColor));

        var font = Minecraft.getInstance().font;
        addComponent(new TextWidget(panelX + 14, panelY + 8, getTitle().getString(), 0xFFF2F6FB));
        addComponent(new TextWidget(panelX + 14, panelY + 20, subtitleText, 0xFF9DB0C2));

        addScreenWidgets();
    }

    protected abstract void addScreenWidgets();

    protected ScrollBarComponent createScrollBar(int x, int y, int width, int height, ScrollOrientation orientation) {
        int thickness = orientation.isHorizontal() ? height : width;
        ScrollBarComponent bar = new ScrollBarComponent(x, y, width, height, orientation,
                new ScrollWheelComponent(null, 0, 0, thickness) {
                    @Override
                    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                        graphics.fill(0, 0, getWidth(), getHeight(), 0xFF555555);
                        graphics.fill(1, 1, getWidth() - 1, getHeight() - 1, 0xFF2A2E3A);
                    }
                });
        bar.removeBackground();
        return bar;
    }

    public void openGui() {
        Minecraft.getInstance().setScreen(this);
    }

    @Override
    public void onTickScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Shared layout components

    protected static class LabelValueRow extends AbstractComponent<LabelValueRow> {
        private final String label;
        private final String value;
        private final int valueColor;

        public LabelValueRow(int x, int y, String label, String value, int valueColor) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.label = label;
            this.value = value;
            this.valueColor = valueColor;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(label), CONTENT_X, 0, 0xFF9BA9B7, false);
            String trimmed = font.plainSubstrByWidth(value, 160);
            graphics.drawString(font, Component.literal(trimmed), COL_VALUE, 0, valueColor, false);
        }
    }

    protected static class RowLabel extends AbstractComponent<RowLabel> {
        private final String text;
        private final int color;

        public RowLabel(int x, int y, String text, int color) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.drawString(Minecraft.getInstance().font, Component.literal(text), CONTENT_X, 0, color, false);
        }
    }

    protected static class SectionLabel extends AbstractComponent<SectionLabel> {
        private final String text;
        private final int color;

        public SectionLabel(int x, int y, String text, int color) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(text).withStyle(net.minecraft.ChatFormatting.UNDERLINE),
                    CONTENT_X, 0, color, false);
        }
    }

    protected static class SpacerRow extends AbstractComponent<SpacerRow> {
        public SpacerRow(int x, int y, int height) {
            super(null, x, y, CONTENT_WIDTH, height);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        }
    }

    protected static class TableHeaderRow extends AbstractComponent<TableHeaderRow> {
        private final String[] columns;
        private final int[] colX;

        public TableHeaderRow(int x, int y, String[] columns, int[] colX) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.columns = columns;
            this.colX = colX;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var font = Minecraft.getInstance().font;
            for (int i = 0; i < columns.length && i < colX.length; i++) {
                graphics.drawString(font, Component.literal(columns[i]), colX[i], 0, 0xFF78909C, false);
            }
        }
    }

    protected static class TextWidget extends AbstractComponent<TextWidget> {
        private final String text;
        private final int color;

        public TextWidget(int x, int y, String text, int color) {
            super(null, x, y, Minecraft.getInstance().font.width(text), Minecraft.getInstance().font.lineHeight);
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.drawString(Minecraft.getInstance().font, Component.literal(text), 0, 0, color, false);
        }
    }
}
