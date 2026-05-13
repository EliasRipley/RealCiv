package com.realciv.realciv.client;

import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.network.RealCivPayloads;
import com.realciv.realciv.tax.TaxSnapshot;
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

public class ModernTaxScreen extends RealCivPanelScreen {
    public static final int ACTION_PAY_1 = 1;
    public static final int ACTION_PAY_5 = 2;
    public static final int ACTION_PAY_25 = 3;
    public static final int ACTION_RATE_DOWN = 4;
    public static final int ACTION_RATE_UP = 5;
    public static final int ACTION_MODE_TOGGLE = 6;
    public static final int ACTION_ITEM_CYCLE = 7;
    public static final int ACTION_ITEM_COUNT_DOWN = 8;
    public static final int ACTION_ITEM_COUNT_UP = 9;
    public static final int ACTION_MEMBER_CLICK = 10;
    public static final int ACTION_PREV_PAGE = 11;
    public static final int ACTION_NEXT_PAGE = 12;

    private static final int ROW_WIDTH = PANEL_WIDTH - 32;
    private static final int BOTTOM_BTN_Y = PANEL_HEIGHT - 22;
    private static final int TOOLTIP_Y = PANEL_HEIGHT - 38;

    private TaxSnapshot snapshot;

    public ModernTaxScreen(TaxSnapshot snapshot) {
        super(Component.literal("Tax Office"), "Upkeep, prepayment, and policy", 0xFFE1B12C);
        this.snapshot = snapshot;
    }

    public void refresh(TaxSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshContent();
    }

    @Override
    public void addWidgets() {
        setupContentPanel();
        refreshContent();

        add(new NordButton(this, Component.literal("<"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PREV_PAGE); }
        }.setPosAndSize(10, 40, 18, 16));

        add(new NordButton(this, Component.literal(">"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_NEXT_PAGE); }
        }.setPosAndSize(PANEL_WIDTH - 28, 40, 18, 16));

        add(new NordButton(this, Component.literal("Pay 1"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PAY_1); }
        }.setPosAndSize(10, BOTTOM_BTN_Y, 40, 16));

        add(new NordButton(this, Component.literal("Pay 5"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PAY_5); }
        }.setPosAndSize(54, BOTTOM_BTN_Y, 40, 16));

        add(new NordButton(this, Component.literal("Pay 25"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PAY_25); }
        }.setPosAndSize(98, BOTTOM_BTN_Y, 46, 16));

        if (snapshot.canManage()) {
            int rx = PANEL_WIDTH - 14;

            add(new NordButton(this, Component.literal("-10%"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_RATE_DOWN); }
            }.setPosAndSize(rx - 248, BOTTOM_BTN_Y, 46, 16));

            add(new NordButton(this, Component.literal("+10%"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_RATE_UP); }
            }.setPosAndSize(rx - 198, BOTTOM_BTN_Y, 46, 16));

            add(new NordButton(this, Component.literal("Mode"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_MODE_TOGGLE); }
            }.setPosAndSize(rx - 148, BOTTOM_BTN_Y, 42, 16));

            add(new NordButton(this, Component.literal("Item"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_ITEM_CYCLE); }
            }.setPosAndSize(rx - 102, BOTTOM_BTN_Y, 42, 16));

            add(new NordButton(this, Component.literal("-C"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_ITEM_COUNT_DOWN); }
            }.setPosAndSize(rx - 56, BOTTOM_BTN_Y, 24, 16));

            add(new NordButton(this, Component.literal("+C"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_ITEM_COUNT_UP); }
            }.setPosAndSize(rx - 28, BOTTOM_BTN_Y, 24, 16));
        }
    }

    @Override
    protected void populateContentPanel(Panel panel) {
        panel.add(new InfoRowWidget(panel, "Your plots", String.valueOf(snapshot.ownedPlots()), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "Delinquent", String.valueOf(snapshot.delinquentPlots()), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "Karma", RealCivUtil.formatCredits(snapshot.playerKarmaCents()), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "Civ Treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents()), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "Rate", String.format("%.2fx", snapshot.rateMultiplier()), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "Mode", snapshot.paymentMode(), 0xFFF4F7FA));

        String itemDisplay = snapshot.taxItemId() + " x" + snapshot.taxItemCount();
        int itemColor = "karma".equalsIgnoreCase(snapshot.paymentMode()) ? 0xFF78909C : 0xFFF4F7FA;
        panel.add(new InfoRowWidget(panel, "Tax item", itemDisplay, itemColor));

        long perPlotCost = "karma".equalsIgnoreCase(snapshot.paymentMode())
                ? snapshot.cycleCostCents() : snapshot.taxItemPerPlotRate();
        panel.add(new InfoRowWidget(panel, "Per plot/cycle", String.valueOf(perPlotCost), 0xFFB0BEC5));

        panel.add(createTextWidget("", 0));

        if (snapshot.canManage()) {
            String membersHeader = "All members (page " + (snapshot.memberPage() + 1) + "/" + snapshot.totalMemberPages() + ")";
            panel.add(createTextWidget(membersHeader, 0xFF9DB0C2));

            for (int i = 0; i < snapshot.members().size(); i++) {
                panel.add(new MemberRowWidget(panel, i, snapshot.members().get(i)));
            }
        } else {
            panel.add(createTextWidget("Member view - leadership sees all tax records and can adjust policy.", 0xFF78909C));
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

        var font = Minecraft.getInstance().font;
        if (snapshot.canManage()) {
            graphics.drawString(font, Component.literal("Rate: adjust -10%/+10% | Mode: Karma/Item | Item: cycle | +/-C: adjust count"),
                    x + 14, y + TOOLTIP_Y, 0xFF78909C, false);
        } else {
            graphics.drawString(font, Component.literal("Pay for 1, 5, or 25 upkeep cycles to keep plots protected"),
                    x + 14, y + TOOLTIP_Y, 0xFF78909C, false);
        }
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_TAX, actionId));
    }

    private static class InfoRowWidget extends Widget {
        private final String label;
        private final String value;
        private final int valueColor;

        InfoRowWidget(Panel panel, String label, String value, int valueColor) {
            super(panel);
            this.label = label;
            this.value = value;
            this.valueColor = valueColor;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(label), x, y, 0xFF9BA9B7, false);
            String trimmed = font.plainSubstrByWidth(value, ROW_WIDTH - 120);
            graphics.drawString(font, Component.literal(trimmed), x + 120, y, valueColor, false);
        }

        @Override
        public boolean mousePressed(MouseButton button) { return false; }
    }

    private class MemberRowWidget extends Widget {
        private final int index;
        private final TaxSnapshot.MemberRow member;

        MemberRowWidget(Panel panel, int index, TaxSnapshot.MemberRow member) {
            super(panel);
            this.index = index;
            this.member = member;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver();
            if (hovered) {
                graphics.fill(x - 2, y - 1, x + w + 2, y + h + 1, 0x30FFFFFF);
            }

            var font = Minecraft.getInstance().font;
            String prefix = member.isLeader() ? "\u2605 " : "  ";
            String line = prefix + member.name() + "  plots:" + member.ownedPlots() + "  del:" + member.delinquentPlots()
                    + "  karma:" + RealCivUtil.formatCredits(member.karmaCents());
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(line, ROW_WIDTH)),
                    x, y, member.isLeader() ? 0xFFFFD54F : 0xFFF4F7FA, false);
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (button.isLeft() && snapshot.canManage()) {
                sendAction(ACTION_MEMBER_CLICK * 100 + index);
                return true;
            }
            return false;
        }
    }
}
