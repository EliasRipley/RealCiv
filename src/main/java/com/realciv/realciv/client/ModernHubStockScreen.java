package com.realciv.realciv.client;

import com.realciv.realciv.hub.HubStockSnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ModernHubStockScreen extends RealCivPanelScreen {
    public static final int ACTION_WITHDRAW_1 = 1;
    public static final int ACTION_WITHDRAW_64 = 2;
    public static final int ACTION_PREV_PAGE = 3;
    public static final int ACTION_NEXT_PAGE = 4;

    private static final int ROW_WIDTH = PANEL_WIDTH - 32;

    private HubStockSnapshot snapshot;

    public ModernHubStockScreen(HubStockSnapshot snapshot) {
        super(Component.literal("Community Hub Stock"),
                "Withdrawals, quotas, allowances, and distribution policy", 0xFFFF8A3D);
        this.snapshot = snapshot;
    }

    public void refresh(HubStockSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshContent();
    }

    @Override
    public void addWidgets() {
        setupSearchBox("Search items...");
        setupContentPanel();
        refreshContent();

        add(new NordButton(this, Component.literal("<"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                sendAction(ACTION_PREV_PAGE);
            }
        }.setPosAndSize(10, 40, 18, 16));

        add(new NordButton(this, Component.literal(">"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                sendAction(ACTION_NEXT_PAGE);
            }
        }.setPosAndSize(PANEL_WIDTH - 28, 40, 18, 16));
    }

    @Override
    protected void populateContentPanel(Panel panel) {
        String query = (searchBox != null) ? searchBox.getText().toLowerCase() : "";
        Predicate<String> filter = query.isEmpty()
            ? s -> true
            : s -> s.toLowerCase().contains(query);

        // Distribution mode header (not scrollable, rendered at top)
        // Actually, it's inside the scrollable panel for consistency
        panel.add(createTextWidget("", 0));

        // Page info
        String pageInfo = "Page " + (snapshot.page() + 1) + "/" + snapshot.totalPages();
        panel.add(createTextWidget(pageInfo, 0xFF9DB0C2));

        List<Integer> filtered = new ArrayList<>();
        for (int i = 0; i < snapshot.entries().size(); i++) {
            if (filter.test(snapshot.entries().get(i).itemName())) filtered.add(i);
        }

        for (int idx : filtered) {
            panel.add(new StockRowWidget(panel, idx, snapshot.entries().get(idx)));
        }

        if (filtered.isEmpty() && !query.isEmpty()) {
            panel.add(createTextWidget("No matching stock items found.", 0xFF9E9E9E));
        }
    }

    @Override
    protected void alignContentPanel(Panel panel) {
        for (Widget w : panel.getWidgets()) {
            if (w.width == 0) w.setSize(ROW_WIDTH, ROW_HEIGHT);
            else if (w.width < 10) w.setSize(ROW_WIDTH, w.height > 0 ? w.height : ROW_HEIGHT);
        }
        panel.align(new WidgetLayout.Vertical(2, 2, 4));
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);

        // Distribution mode line (outside scrollable area)
        var mcFont = Minecraft.getInstance().font;
        graphics.drawString(mcFont, Component.literal("Distribution mode"), x + 14, y + CONTENT_TOP + 14, 0xFF9BA9B7, false);
        String distMode = mcFont.plainSubstrByWidth(snapshot.distributionMode(), PANEL_WIDTH - 134);
        graphics.drawString(mcFont, Component.literal(distMode), x + 134, y + CONTENT_TOP + 14, 0xFFF4F7FA, false);
    }

    @Override
    protected void onSearchChanged() {
        refreshContent();
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_HUB_STOCK, actionId));
    }

    private class StockRowWidget extends Widget {
        private final int origIndex;
        private final HubStockSnapshot.StockRow row;

        StockRowWidget(Panel panel, int origIndex, HubStockSnapshot.StockRow row) {
            super(panel);
            this.origIndex = origIndex;
            this.row = row;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver();
            var font = Minecraft.getInstance().font;

            if (hovered) {
                graphics.fill(x - 2, y - 1, x + w + 2, y + h + 1, 0x30FFFFFF);
            }

            String line = row.itemName() + "  stock:" + row.available() + "  yours:" + row.playerContributed();
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(line, ROW_WIDTH)),
                    x, y, 0xFFF4F7FA, false);

            if (row.dailyAllowance() > 0) {
                String allowText = "allow:" + row.dailyAllowance() + "/d";
                graphics.drawString(font, Component.literal(allowText),
                        x + ROW_WIDTH - 80, y, 0xFFFFD54F, false);

                if (row.available() > 0 && row.dailyAllowance() > 0) {
                    long daysSupply = row.available() / row.dailyAllowance();
                    graphics.drawString(font, Component.literal(daysSupply + "d"),
                            x + ROW_WIDTH - 28, y, 0xFF90CAF9, false);
                }
            }

            if (hovered) {
                String tip = "Left-click: withdraw 64 | Right-click: withdraw 1";
                if (snapshot.canManage() && row.dailyAllowance() > 0) {
                    tip += " | Shift+click: adjust allowance";
                }
                graphics.drawString(font, Component.literal(font.plainSubstrByWidth(tip, ROW_WIDTH)),
                        x + 4, y, 0x60FFFFFF, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            boolean shift = Widget.isShiftKeyDown();

            if (shift && snapshot.canManage()) {
                if (button.isLeft()) {
                    sendAction(3000 + origIndex);
                } else {
                    sendAction(4000 + origIndex);
                }
                return true;
            }

            if (button.isLeft()) {
                sendAction(2000 + origIndex);
            } else {
                sendAction(1000 + origIndex);
            }
            return true;
        }
    }
}
