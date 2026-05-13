package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.NordTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class RealCivPanelScreen extends BaseScreen {
    protected static final int PANEL_WIDTH = 340;
    protected static final int PANEL_HEIGHT = 220;
    protected static final int CONTENT_TOP = 44;
    protected static final int CONTENT_BOTTOM = PANEL_HEIGHT - 36;
    protected static final int CONTENT_HEIGHT = CONTENT_BOTTOM - CONTENT_TOP;
    protected static final int ROW_HEIGHT = 12;
    protected static final int SEARCH_BAR_HEIGHT = 12;

    private final Component screenTitle;
    private final String subtitleText;
    private final int accentColor;

    protected Panel contentPanel;
    protected PanelScrollBar scrollBar;
    protected TextBox searchBox;

    public RealCivPanelScreen(Component title, String subtitle, int accentColor) {
        this.screenTitle = title;
        this.subtitleText = subtitle;
        this.accentColor = accentColor;
    }

    @Override
    public boolean onInit() {
        setSize(PANEL_WIDTH, PANEL_HEIGHT);
        return true;
    }

    @Override
    public void alignWidgets() {
        int mx = (getScreen().getGuiScaledWidth() - PANEL_WIDTH) / 2;
        int my = (getScreen().getGuiScaledHeight() - PANEL_HEIGHT) / 2;
        setPos(mx, my);

        if (contentPanel != null) {
            int searchH = (searchBox != null) ? SEARCH_BAR_HEIGHT + 2 : 0;
            contentPanel.setPosAndSize(8, CONTENT_TOP + searchH, PANEL_WIDTH - 24, CONTENT_HEIGHT - searchH);
        }
        if (scrollBar != null) {
            int searchH = (searchBox != null) ? SEARCH_BAR_HEIGHT + 2 : 0;
            scrollBar.setPosAndSize(PANEL_WIDTH - 14, CONTENT_TOP + searchH, 6, CONTENT_HEIGHT - searchH);
        }
        if (searchBox != null) {
            searchBox.setPosAndSize(10, CONTENT_TOP + 1, PANEL_WIDTH - 22, SEARCH_BAR_HEIGHT);
        }
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        graphics.fill(0, 0, getScreen().getGuiScaledWidth(), getScreen().getGuiScaledHeight(), 0xC0000000);

        graphics.fill(x, y, x + w, y + h, 0xFF14171B);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF252D36);
        graphics.fill(x + 6, y + 34, x + w - 6, y + h - 6, 0xFF1A2129);
        graphics.fill(x + 6, y + 34, x + w - 6, y + 36, accentColor);

        var mcFont = Minecraft.getInstance().font;
        graphics.drawString(mcFont, screenTitle, x + 14, y + 8, 0xFFF2F6FB, false);
        graphics.drawString(mcFont, Component.literal(subtitleText), x + 14, y + 20, 0xFF9DB0C2, false);
    }

    @Override
    public Theme getTheme() {
        return NordTheme.THEME;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected void setupContentPanel() {
        contentPanel = new Panel(this) {
            @Override
            public void addWidgets() {
                RealCivPanelScreen.this.populateContentPanel(this);
            }
            @Override
            public void alignWidgets() {
                RealCivPanelScreen.this.alignContentPanel(this);
            }
        };
        contentPanel.setOnlyRenderWidgetsInside(true);
        add(contentPanel);

        scrollBar = new PanelScrollBar(this, contentPanel);
        add(scrollBar);
    }

    protected void setupSearchBox(String ghostText) {
        searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                super.onTextChanged();
                onSearchChanged();
            }
        };
        searchBox.setMaxLength(32);
        searchBox.ghostText = ghostText;
        add(searchBox);
    }

    protected void refreshContent() {
        if (contentPanel != null) {
            contentPanel.setScrollY(0);
            contentPanel.refreshWidgets();
        }
    }

    protected void onSearchChanged() {
        refreshContent();
    }

    protected boolean matchesSearch(Widget widget) {
        if (searchBox == null) return true;
        String query = searchBox.getText().toLowerCase();
        if (query.isEmpty()) return true;
        String text = getFilterText(widget);
        return text != null && text.toLowerCase().contains(query);
    }

    protected String getFilterText(Widget widget) {
        return widget.getTitle().getString();
    }

    protected Widget createTextWidget(String text, int color) {
        return new Widget(contentPanel) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                var mcFont = Minecraft.getInstance().font;
                graphics.drawString(mcFont, Component.literal(text), x, y, color, false);
            }
        };
    }

    protected Widget createHeaderWidget(String text, int color) {
        return new Widget(contentPanel) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                var mcFont = Minecraft.getInstance().font;
                graphics.drawString(mcFont, Component.literal(text).withStyle(net.minecraft.ChatFormatting.UNDERLINE), x, y, color, false);
            }
        };
    }

    protected abstract void populateContentPanel(Panel panel);
    protected abstract void alignContentPanel(Panel panel);
}
