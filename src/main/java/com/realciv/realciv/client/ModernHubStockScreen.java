package com.realciv.realciv.client;

import com.daqem.uilib.api.client.gui.component.scroll.ScrollOrientation;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.io.ButtonComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollBarComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollContentComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollPanelComponent;
import com.realciv.realciv.hub.HubStockSnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernHubStockScreen extends RealCivUIScreen {
    public static final int ACTION_WITHDRAW_1 = 1;
    public static final int ACTION_WITHDRAW_64 = 2;
    public static final int ACTION_PREV_PAGE = 3;
    public static final int ACTION_NEXT_PAGE = 4;

    private static final int SCROLL_X = 14;
    private static final int SCROLL_Y = 52;
    private static final int SCROLL_W = PANEL_WIDTH - 42;
    private static final int SCROLL_H = 162;
    private static final int SCROLL_BAR_X = PANEL_WIDTH - 26;
    private static final int SCROLL_BAR_W = 6;

    // Stock table columns
    private static final int COL_ITEM = 4;
    private static final int COL_STOCK = 110;
    private static final int COL_CONTRIB = 170;
    private static final int COL_ALLOW = 250;
    private static final int COL_DAYS = 310;
    private static final int[] COLS = {COL_ITEM, COL_STOCK, COL_CONTRIB, COL_ALLOW, COL_DAYS};
    private static final String[] HEADERS = {"Item", "Stock", "Yours", "Allow/d", "Days"};

    private HubStockSnapshot snapshot;
    private ScrollContentComponent scrollContent;

    public ModernHubStockScreen(HubStockSnapshot snapshot) {
        super(Component.literal("Community Hub Stock"),
                "Withdrawals, quotas, allowances, and distribution policy", 0xFFFF8A3D);
        this.snapshot = snapshot;
    }

    public void refresh(HubStockSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        if (scrollContent != null) {
            scrollContent.getChildren().clear();
            populateScrollContent();
            scrollContent.setY(0);
        }
    }

    @Override
    protected void addScreenWidgets() {
        addComponent(new ButtonComponent(panelX + 10, panelY + 36, 18, 14,
                Component.literal("<"), (btn, screen, mx, my, button) -> { sendAction(ACTION_PREV_PAGE); return true; }));
        addComponent(new ButtonComponent(panelX + PANEL_WIDTH - 28, panelY + 36, 18, 14,
                Component.literal(">"), (btn, screen, mx, my, button) -> { sendAction(ACTION_NEXT_PAGE); return true; }));

        // Distribution mode header (above scroll area)
        addComponent(new RowLabel(panelX + 14, panelY + 48,
                "Distribution mode: " + snapshot.distributionMode(), 0xFFF4F7FA));

        ScrollContentComponent content = new ScrollContentComponent(0, 0, 2, ScrollOrientation.VERTICAL);
        scrollContent = content;
        populateScrollContent();

        addComponent(new ScrollPanelComponent(null, panelX + SCROLL_X, panelY + SCROLL_Y, SCROLL_W, SCROLL_H,
                ScrollOrientation.VERTICAL, content,
                createScrollBar(SCROLL_BAR_X, 0, SCROLL_BAR_W, SCROLL_H, ScrollOrientation.VERTICAL)));
    }

    private void populateScrollContent() {
        String pageInfo = "Page " + (snapshot.page() + 1) + "/" + snapshot.totalPages();
        scrollContent.addChild(new RowLabel(0, 0, pageInfo, 0xFF9DB0C2));
        scrollContent.addChild(new TableHeaderRow(0, 0, HEADERS, COLS));

        for (int i = 0; i < snapshot.entries().size(); i++) {
            scrollContent.addChild(new StockRowComponent(0, 0, i, snapshot.entries().get(i)));
        }

        if (snapshot.entries().isEmpty()) {
            scrollContent.addChild(new RowLabel(0, 0, "No stock items available.", 0xFF9E9E9E));
        }
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_HUB_STOCK, actionId));
    }

    private class StockRowComponent extends AbstractComponent<StockRowComponent> {
        private final int origIndex;
        private final HubStockSnapshot.StockRow row;

        StockRowComponent(int x, int y, int origIndex, HubStockSnapshot.StockRow row) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.origIndex = origIndex;
            this.row = row;
            setOnClickEvent((comp, screen, mx, my, button) -> {
                if (Screen.hasShiftDown()) {
                    if (snapshot.canManage()) {
                        sendAction(button == 0 ? 3000 + origIndex : 4000 + origIndex);
                    }
                } else {
                    sendAction(button == 0 ? 2000 + origIndex : 1000 + origIndex);
                }
                return true;
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isTotalHovered(mouseX, mouseY);
            if (hovered) {
                graphics.fill(-2, -1, CONTENT_WIDTH + 2, 13, 0x20FFFFFF);
            }

            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(row.itemName(), 100)),
                    COL_ITEM, 0, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.available())),
                    COL_STOCK, 0, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.playerContributed())),
                    COL_CONTRIB, 0, 0xFFF4F7FA, false);

            if (row.dailyAllowance() > 0) {
                graphics.drawString(font, Component.literal(String.valueOf(row.dailyAllowance())),
                        COL_ALLOW, 0, 0xFFFFD54F, false);

                if (row.available() > 0) {
                    long daysSupply = row.available() / row.dailyAllowance();
                    graphics.drawString(font, Component.literal(daysSupply + "d"),
                            COL_DAYS, 0, 0xFF90CAF9, false);
                }
            } else {
                graphics.drawString(font, Component.literal("\u221E"),
                        COL_ALLOW, 0, 0xFF78909C, false);
            }

            if (hovered) {
                String tip = "Left: withdraw 64 | Right: withdraw 1";
                if (snapshot.canManage() && row.dailyAllowance() > 0) {
                    tip += " | Shift: adjust allowance";
                }
                graphics.drawString(font, Component.literal(font.plainSubstrByWidth(tip, 300)),
                        COL_ITEM, -10, 0x60FFFFFF, false);
            }
        }
    }
}
