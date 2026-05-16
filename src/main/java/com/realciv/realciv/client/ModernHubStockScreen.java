package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import com.realciv.realciv.hub.HubStockSnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernHubStockScreen extends RealCivScreen {
    public static final int ACTION_WITHDRAW_1 = 1;
    public static final int ACTION_WITHDRAW_64 = 2;
    public static final int ACTION_PREV_PAGE = 3;
    public static final int ACTION_NEXT_PAGE = 4;

    private HubStockSnapshot snapshot;

    public ModernHubStockScreen(HubStockSnapshot snapshot) {
        super(Component.literal("Community Hub Stock"), "Withdrawals, quotas, allowances, and distribution policy", 0xFFFF8A3D);
        this.snapshot = snapshot;
    }

    public void refresh(HubStockSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        {
            SimpleTextButton btn = makeFixedBtn(ACTION_PREV_PAGE, "<", 18);
            btn.setPos(2, 2);
            add(btn);
        }
        {
            SimpleTextButton btn = makeFixedBtn(ACTION_NEXT_PAGE, ">", 18);
            btn.setPos(PANEL_W - 20, 2);
            add(btn);
        }
    }

    @Override
    protected void addScrollContent(Panel panel) {
        addLabelRow("Distribution mode: " + snapshot.distributionMode(), "", 0xFFF4F7FA);

        String pageInfo = "Page " + (snapshot.page() + 1) + "/" + snapshot.totalPages();
        addLabelRow("", pageInfo, 0xFF9DB0C2);

        panel.add(new LabelWidget(panel, "Item", 4, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Stock", 110, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Yours", 170, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Allow/d", 250, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Days", 310, currentY, 0xFF78909C));
        currentY += ROW_H;

        for (int i = 0; i < snapshot.entries().size(); i++) {
            panel.add(new StockRowWidget(panel, 4, currentY, i, snapshot.entries().get(i), snapshot.canManage(), this::sendAction));
            currentY += ROW_H;
        }

        if (snapshot.entries().isEmpty()) {
            addLabelRow("", "No stock items available.", 0xFF9E9E9E);
        }
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_HUB_STOCK, actionId));
    }

    private static class StockRowWidget extends Widget {
        private final int origIndex;
        private final HubStockSnapshot.StockRow row;
        private final boolean canManage;
        private final java.util.function.IntConsumer actionSender;

        StockRowWidget(Panel parent, int x, int y, int origIndex, HubStockSnapshot.StockRow row,
                       boolean canManage, java.util.function.IntConsumer actionSender) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.origIndex = origIndex;
            this.row = row;
            this.canManage = canManage;
            this.actionSender = actionSender;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver();
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(row.itemName(), 100)),
                    x + 4, y + 2, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.available())),
                    x + 110, y + 2, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.playerContributed())),
                    x + 170, y + 2, 0xFFF4F7FA, false);

            if (row.dailyAllowance() > 0) {
                graphics.drawString(font, Component.literal(String.valueOf(row.dailyAllowance())),
                        x + 250, y + 2, 0xFFFFD54F, false);
                if (row.available() > 0) {
                    long daysSupply = row.available() / row.dailyAllowance();
                    graphics.drawString(font, Component.literal(daysSupply + "d"),
                            x + 310, y + 2, 0xFF90CAF9, false);
                }
            } else {
                graphics.drawString(font, Component.literal("oo"),
                        x + 250, y + 2, 0xFF78909C, false);
            }

            if (hovered) {
                String tip = "Left: withdraw 64 | Right: withdraw 1";
                if (canManage && row.dailyAllowance() > 0) {
                    tip += " | Shift: adjust allowance";
                }
                graphics.drawString(font, Component.literal(font.plainSubstrByWidth(tip, 300)),
                        x + 4, y + h, 0x60FFFFFF, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver()) {
                if (Screen.hasShiftDown()) {
                    if (canManage) {
                        actionSender.accept(button == MouseButton.LEFT ? 3000 + origIndex : 4000 + origIndex);
                    }
                } else {
                    actionSender.accept(button == MouseButton.LEFT ? 2000 + origIndex : 1000 + origIndex);
                }
                return true;
            }
            return false;
        }
    }
}
