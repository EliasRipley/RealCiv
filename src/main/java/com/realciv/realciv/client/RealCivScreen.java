package com.realciv.realciv.client;

import com.realciv.realciv.RealCivMod;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public abstract class RealCivScreen extends BaseScreen {
    protected static final int PANEL_W = 520;
    protected static final int PANEL_H = 380;

    protected static final int CONTENT_X = 10;
    protected static final int CONTENT_Y = 44;
    protected static final int CONTENT_W = PANEL_W - 32;
    protected static final int CONTENT_H = PANEL_H - CONTENT_Y - 64;
    protected static final int FOOTER_Y = 326;

    protected static final int COL_LABEL = 4;
    protected static final int COL_VALUE = 175;
    protected static final int COL_BTNS = 310;
    protected static final int ROW_H = 18;
    protected static final int BTN_H = 14;

    private final Component screenTitle;
    private final String subtitleText;
    private final int accentColor;
    protected int currentY = 2;
    private Panel scrollPanel;

    public RealCivScreen(Component title, String subtitle, int accentColor) {
        super();
        setSize(PANEL_W, PANEL_H);
        this.screenTitle = title;
        this.subtitleText = subtitle;
        this.accentColor = accentColor;
    }

    @Override
    public Component getTitle() {
        return screenTitle;
    }

    @Override
    public boolean onInit() {
        return true;
    }

    @Override
    public final void addWidgets() {
        scrollPanel = new ScrollablePanel(this);
        scrollPanel.setPosAndSize(CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_H);
        add(scrollPanel);

        PanelScrollBar scrollBar = new PanelScrollBar(this, ScrollBar.Plane.VERTICAL, scrollPanel);
        scrollBar.setPosAndSize(CONTENT_X + CONTENT_W + 2, CONTENT_Y, 8, CONTENT_H);
        add(scrollBar);

        addFixedWidgets();
        addGlobalWidgets();
    }

    protected abstract void addScrollContent(Panel panel);

    protected void addFixedWidgets() {
    }

    protected void addGlobalWidgets() {
        addCloseButton();
    }

    private void addCloseButton() {
        SimpleTextButton closeBtn = new SimpleTextButton(this, Component.literal("X"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                closeGui(true);
            }

            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                drawBackground(graphics, theme, x, y, w, h);
                int textX = x + (w - theme.getStringWidth(getTitle())) / 2;
                int textY = y + (h - theme.getFontHeight() + 1) / 2;
                theme.drawString(graphics, getTitle(), textX, textY,
                        theme.getContentColor(getWidgetType()), Theme.SHADOW);
            }
        };
        closeBtn.setPosAndSize(PANEL_W - 56, 8, 18, 14);
        add(closeBtn);
    }

    protected final void addSection(String title, int color) {
        scrollPanel.add(new SectionLabel(scrollPanel, title, COL_LABEL, currentY, color));
        currentY += ROW_H + 1;
    }

    protected final void addSection(String title) {
        addSection(title, 0xFFC6D2DE);
    }

    protected final void addLabelRow(String label, String value, int valueColor) {
        scrollPanel.add(new LabelWidget(scrollPanel, label, COL_LABEL, currentY, 0xFF9BA9B7));
        scrollPanel.add(new LabelWidget(scrollPanel, value, COL_VALUE, currentY, valueColor));
        currentY += ROW_H;
    }

    protected final void addLabelRow(String label, String value) {
        addLabelRow(label, value, 0xFFF4F7FA);
    }

    protected final void addSelectableRow(String text, int color, Consumer<MouseButton> onClick) {
        scrollPanel.add(new SelectableRowLabel(scrollPanel, COL_LABEL, currentY, text, color, onClick));
        currentY += ROW_H;
    }

    protected final void addRowWithButtons(String label, String desc, Widget... buttons) {
        scrollPanel.add(new LabelWidget(scrollPanel, label, COL_LABEL, currentY, 0xFF9BA9B7));
        scrollPanel.add(new LabelWidget(scrollPanel, desc, COL_VALUE, currentY, 0xFF78909C));
        int nextButtonX = COL_BTNS;
        for (Widget btn : buttons) {
            int localX = btn.posX > 0 ? (btn.posX + COL_BTNS) : nextButtonX;
            btn.setPos(localX, btn.posY + currentY);
            scrollPanel.add(btn);
            nextButtonX = localX + btn.width + 4;
        }
        currentY += ROW_H + 2;
    }

    protected final void addSpacer(int height) {
        currentY += height;
    }

    protected final void addIdentitySection(String civName, String role, boolean canManage) {
        addSection("Civilization", 0xFFE8EEF5);
        addLabelRow("Civ", fallbackText(civName, "Unknown"), 0xFFF4F7FA);
        String roleText = fallbackText(role, "");
        if (!roleText.isBlank()) {
            addLabelRow("Role", roleText, 0xFF90CAF9);
        }
        addLabelRow("Access", canManage ? "Leadership controls enabled" : "Citizen controls only", 0xFFB0BEC5);
        addSpacer(4);
    }

    protected final String fallbackText(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text;
    }

    protected final SimpleTextButton makeBtn(String label, int actionId) {
        return new SimpleTextButton(scrollPanel, Component.literal(label), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                sendAction(actionId);
            }

            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                drawBackground(graphics, theme, x, y, w, h);
                int textX = x + (w - theme.getStringWidth(getTitle())) / 2;
                int textY = y + (h - theme.getFontHeight() + 1) / 2;
                theme.drawString(graphics, getTitle(), textX, textY,
                        theme.getContentColor(getWidgetType()), Theme.SHADOW);
            }
        };
    }

    protected final SimpleTextButton makeInlineBtn(int actionId, String label, int width) {
        SimpleTextButton btn = makeBtn(label, actionId);
        btn.setSize(width, BTN_H);
        return btn;
    }

    protected final SimpleTextButton makePanelBtn(Panel parent, String label, java.util.function.Consumer<MouseButton> onClick) {
        return new SimpleTextButton(parent, Component.literal(label), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                onClick.accept(button);
            }

            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                drawBackground(graphics, theme, x, y, w, h);
                int textX = x + (w - theme.getStringWidth(getTitle())) / 2;
                int textY = y + (h - theme.getFontHeight() + 1) / 2;
                theme.drawString(graphics, getTitle(), textX, textY,
                        theme.getContentColor(getWidgetType()), Theme.SHADOW);
            }
        };
    }

    protected final SimpleTextButton makeFixedBtn(int actionId, String label, int width) {
        SimpleTextButton btn = new SimpleTextButton(this, Component.literal(label), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                sendAction(actionId);
            }

            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                drawBackground(graphics, theme, x, y, w, h);
                int textX = x + (w - theme.getStringWidth(getTitle())) / 2;
                int textY = y + (h - theme.getFontHeight() + 1) / 2;
                theme.drawString(graphics, getTitle(), textX, textY,
                        theme.getContentColor(getWidgetType()), Theme.SHADOW);
            }
        };
        btn.setSize(width, BTN_H);
        return btn;
    }

    protected abstract void sendAction(int actionId);

    private void populateScrollableContent(Panel panel) {
        currentY = 2;
        try {
            addScrollContent(panel);
        } catch (Exception ex) {
            RealCivMod.LOGGER.error("Failed to build screen '{}': {}", getTitle().getString(), ex.toString(), ex);
            panel.add(new SectionLabel(panel, "Screen Error", COL_LABEL, currentY, 0xFFF44336));
            currentY += ROW_H + 1;
            panel.add(new LabelWidget(panel, "RealCiv could not render this panel.", COL_LABEL, currentY, 0xFFFFCDD2));
            panel.add(new LabelWidget(panel, "Check latest.log for details.", COL_VALUE, currentY, 0xFFFFCDD2));
            currentY += ROW_H;
            panel.add(new LabelWidget(panel, ex.getClass().getSimpleName(), COL_LABEL, currentY, 0xFFFFA726));
            String message = ex.getMessage() == null ? "(no message)" : ex.getMessage();
            panel.add(new LabelWidget(panel, message, COL_VALUE, currentY, 0xFFFFA726));
            currentY += ROW_H;
        }
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        var mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, sw, sh, 0xB0111318);
        graphics.fill(x, y, x + w, y + h, 0xFF0F141C);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF1D2632);
        graphics.fill(x + 6, y + 6, x + w - 6, y + 34, 0xFF16212E);
        graphics.fill(x + 6, y + 34, x + w - 6, y + h - 6, 0xFF141B25);
        graphics.fill(x + 6, y + 33, x + w - 6, y + 35, accentColor);
        graphics.fill(x + 6, y + h - 7, x + w - 6, y + h - 6, 0xFF2A3442);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, getTitle(), x + 14, y + 8, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal(subtitleText), x + 14, y + 20, 0xFF9DB0C2, false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void addMouseOverText(TooltipList list) {
    }

    private class ScrollablePanel extends Panel {
        ScrollablePanel(Panel parent) {
            super(parent);
            setOnlyRenderWidgetsInside(true);
        }

        @Override
        public void addWidgets() {
            RealCivScreen.this.populateScrollableContent(this);
        }

        @Override
        public void alignWidgets() {
        }
    }

    protected static class LabelWidget extends Widget {
        private final String text;
        private final int color;

        LabelWidget(Panel parent, String text, int x, int y, int color) {
            super(parent);
            setPosAndSize(x, y, 160, ROW_H);
            this.text = text;
            this.color = color;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(text, w - 2)),
                    x, y + 2, color, false);
        }
    }

    protected static class SectionLabel extends Widget {
        private final String text;
        private final int color;

        SectionLabel(Panel parent, String text, int x, int y, int color) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.text = text;
            this.color = color;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            graphics.drawString(Minecraft.getInstance().font,
                    Component.literal(text).withStyle(ChatFormatting.UNDERLINE),
                    x, y + 2, color, false);
        }
    }

    protected static class SelectableRowLabel extends Widget {
        private final String text;
        private final int normalColor;
        private final Consumer<MouseButton> onClick;

        SelectableRowLabel(Panel parent, int x, int y, String text, int color, Consumer<MouseButton> onClick) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.text = text;
            this.normalColor = color;
            this.onClick = onClick;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            int displayColor = isMouseOver() ? 0xFFFFFFFF : normalColor;
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(text, w - 4)),
                    x + 4, y + 2, displayColor, false);
            if (isMouseOver()) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver()) {
                onClick.accept(button);
                return true;
            }
            return false;
        }
    }
}
