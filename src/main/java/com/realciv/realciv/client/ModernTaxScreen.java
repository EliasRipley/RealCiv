package com.realciv.realciv.client;

import com.daqem.uilib.api.client.gui.component.event.OnClickEvent;
import com.daqem.uilib.api.client.gui.component.scroll.ScrollOrientation;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.io.ButtonComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollBarComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollContentComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollPanelComponent;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.network.RealCivPayloads;
import com.realciv.realciv.tax.TaxSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernTaxScreen extends RealCivUIScreen {
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
    public static final int ACTION_SET_TAX_ITEM = 13;

    private static final int SCROLL_X = 14;
    private static final int SCROLL_Y = 48;
    private static final int SCROLL_W = PANEL_WIDTH - 42;
    private static final int SCROLL_H = 150;
    private static final int SCROLL_BAR_X = PANEL_WIDTH - 26;
    private static final int SCROLL_BAR_W = 6;
    private static final int PAY_Y = 206;

    // Member table columns
    private static final int COL_NAME = 4;
    private static final int COL_PLOTS = 130;
    private static final int COL_DEL = 180;
    private static final int COL_KARMA = 230;
    private static final int[] MEMBER_COLS = {COL_NAME, COL_PLOTS, COL_DEL, COL_KARMA};
    private static final String[] MEMBER_HEADERS = {"Name", "Plots", "Del", "Karma"};

    // Manage page column
    private static final int MANAGE_LABEL_X = 4;
    private static final int MANAGE_CTRL_X = 240;

    private TaxSnapshot snapshot;
    private ScrollContentComponent scrollContent;
    private ButtonComponent toggleManageBtn;
    private boolean showManagePage;

    public ModernTaxScreen(TaxSnapshot snapshot) {
        super(Component.literal("Tax Office"), "Upkeep, prepayment, and policy", 0xFFE1B12C);
        this.snapshot = snapshot;
    }

    public void refresh(TaxSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        if (scrollContent != null) {
            scrollContent.getChildren().clear();
            populateScrollContent();
            scrollContent.setY(0);
        }
    }

    @Override
    protected void addScreenWidgets() {
        int lx = panelX + 14;

        addComponent(makeButton(panelX + 10, panelY + 36, 18, 14, "<", ACTION_PREV_PAGE));
        addComponent(makeButton(panelX + PANEL_WIDTH - 28, panelY + 36, 18, 14, ">", ACTION_NEXT_PAGE));

        if (snapshot.canManage()) {
            addToggleButton();
        }

        ScrollContentComponent content = new ScrollContentComponent(0, 0, 2, ScrollOrientation.VERTICAL);
        scrollContent = content;
        populateScrollContent();

        addComponent(new ScrollPanelComponent(null, panelX + SCROLL_X, panelY + SCROLL_Y, SCROLL_W, SCROLL_H,
                ScrollOrientation.VERTICAL, content,
                createScrollBar(SCROLL_BAR_X, 0, SCROLL_BAR_W, SCROLL_H, ScrollOrientation.VERTICAL)));

        // Payment row
        addComponent(makeButton(lx, panelY + PAY_Y, 40, 16, "Pay 1", ACTION_PAY_1));
        addComponent(makeButton(lx + 44, panelY + PAY_Y, 40, 16, "Pay 5", ACTION_PAY_5));
        addComponent(makeButton(lx + 88, panelY + PAY_Y, 46, 16, "Pay 25", ACTION_PAY_25));
    }

    private void addToggleButton() {
        if (toggleManageBtn != null) {
            removeComponent(toggleManageBtn);
        }
        String label = showManagePage ? "View" : "Admin";
        toggleManageBtn = new ButtonComponent(
                panelX + PANEL_WIDTH / 2 - 22, panelY + 36, 44, 14,
                Component.literal(label), (btn, scr, mx, my, button) -> {
            showManagePage = !showManagePage;
            refreshScroll();
            addToggleButton();
            return true;
        });
        addComponent(toggleManageBtn);
    }

    private void refreshScroll() {
        if (scrollContent != null) {
            scrollContent.getChildren().clear();
            scrollContent.setY(0);
            populateScrollContent();
        }
    }

    private void populateScrollContent() {
        if (showManagePage && snapshot.canManage()) {
            populateManagePage();
        } else {
            populateViewPage();
        }
    }

    private void populateViewPage() {
        addInfoRow("Your plots", String.valueOf(snapshot.ownedPlots()), 0xFFF4F7FA);
        addInfoRow("Delinquent", String.valueOf(snapshot.delinquentPlots()), 0xFFF4F7FA);
        addInfoRow("Karma", RealCivUtil.formatCredits(snapshot.playerKarmaCents()), 0xFFF4F7FA);
        addInfoRow("Civ Treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents()), 0xFFF4F7FA);
        addInfoRow("Rate", String.format("%.2fx", snapshot.rateMultiplier()), 0xFFF4F7FA);
        addInfoRow("Mode", snapshot.paymentMode(), 0xFFF4F7FA);

        int itemColor = "karma".equalsIgnoreCase(snapshot.paymentMode()) ? 0xFF78909C : 0xFFF4F7FA;
        addInfoRow("Tax item", snapshot.taxItemId() + " x" + snapshot.taxItemCount(), itemColor);

        long perPlotCost = "karma".equalsIgnoreCase(snapshot.paymentMode())
                ? snapshot.cycleCostCents() : snapshot.taxItemPerPlotRate();
        addInfoRow("Per plot/cycle", String.valueOf(perPlotCost), 0xFFB0BEC5);

        scrollContent.addChild(new SpacerRow(0, 0, 6));

        if (snapshot.canManage()) {
            String pageLabel = "Members (page " + (snapshot.memberPage() + 1) + "/" + snapshot.totalMemberPages() + ")";
            scrollContent.addChild(new SectionLabel(0, 0, pageLabel, 0xFF9DB0C2));
            scrollContent.addChild(new TableHeaderRow(0, 0, MEMBER_HEADERS, MEMBER_COLS));

            for (int i = 0; i < snapshot.members().size(); i++) {
                scrollContent.addChild(new MemberRowComponent(0, 0, i, snapshot.members().get(i)));
            }
        } else {
            scrollContent.addChild(new RowLabel(0, 0,
                    "Leadership sees all tax records and can adjust policy.", 0xFF78909C));
        }
    }

    private void populateManagePage() {
        scrollContent.addChild(new SectionLabel(0, 0, "Tax Rate", 0xFFE1B12C));
        scrollContent.addChild(new LabelValueRow(0, 0, "Current rate", String.format("%.2fx", snapshot.rateMultiplier()), 0xFFF4F7FA));
        scrollContent.addChild(new ManageActionRow(0, 0,
                "Adjust the tax multiplier applied to all plots.",
                makeInlineBtn(0, 0, 46, 14, "-10%", ACTION_RATE_DOWN),
                makeInlineBtn(50, 0, 46, 14, "+10%", ACTION_RATE_UP)));

        scrollContent.addChild(new SpacerRow(0, 0, 6));
        scrollContent.addChild(new SectionLabel(0, 0, "Payment Mode", 0xFFE1B12C));
        scrollContent.addChild(new LabelValueRow(0, 0, "Current mode", snapshot.paymentMode(), 0xFFF4F7FA));
        scrollContent.addChild(new ManageActionRow(0, 0,
                "Switch between karma and item-based tax collection.",
                makeInlineBtn(0, 0, 48, 14, "Toggle", ACTION_MODE_TOGGLE)));

        scrollContent.addChild(new SpacerRow(0, 0, 6));
        scrollContent.addChild(new SectionLabel(0, 0, "Tax Item", 0xFFE1B12C));
        scrollContent.addChild(new LabelValueRow(0, 0, "Current item", snapshot.taxItemId() + " x" + snapshot.taxItemCount(),
                "karma".equalsIgnoreCase(snapshot.paymentMode()) ? 0xFF78909C : 0xFFF4F7FA));

        scrollContent.addChild(new ManageActionRow(0, 0,
                "Click the slot while holding an item to set tax item.",
                new ManageSlotInline(0, 0),
                makeInlineBtn(22, 0, 26, 14, "-C", ACTION_ITEM_COUNT_DOWN),
                makeInlineBtn(52, 0, 26, 14, "+C", ACTION_ITEM_COUNT_UP)));

        scrollContent.addChild(new SpacerRow(0, 0, 10));
        scrollContent.addChild(new RowLabel(0, 0,
                "Use the < > buttons to page through members.", 0xFF78909C));
    }

    private void addInfoRow(String label, String value, int valueColor) {
        scrollContent.addChild(new LabelValueRow(0, 0, label, value, valueColor));
    }

    private ButtonComponent makeButton(int x, int y, int w, int h, String label, int actionId) {
        return new ButtonComponent(x, y, w, h, Component.literal(label),
                (btn, screen, mx, my, button) -> { sendAction(actionId); return true; });
    }

    private ButtonComponent makeInlineBtn(int x, int y, int w, int h, String label, int actionId) {
        return new ButtonComponent(x, y, w, h, Component.literal(label),
                (btn, screen, mx, my, button) -> { sendAction(actionId); return true; });
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_TAX, actionId));
    }

    // -- Manage action row: explanation text + inline buttons --

    private class ManageActionRow extends AbstractComponent<ManageActionRow> {
        private final OnClickEvent<ManageActionRow> dummyEvent = (c, s, mx, my, b) -> false;

        ManageActionRow(int x, int y, String explanation, AbstractComponent<?>... controls) {
            super(null, x, y, CONTENT_WIDTH, 20);
            int btnStartX = MANAGE_CTRL_X;
            addChild(new InlineLabel(0, 0, explanation, 0xFF78909C));
            for (AbstractComponent<?> ctrl : controls) {
                ctrl.setX(ctrl.getX() + btnStartX);
                ctrl.setY(ctrl.getY() + 3);
                addChild(ctrl);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        }
    }

    private class InlineLabel extends AbstractComponent<InlineLabel> {
        private final String text;
        private final int color;

        InlineLabel(int x, int y, String text, int color) {
            super(null, x, y, MANAGE_CTRL_X - 4, 14);
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.drawString(Minecraft.getInstance().font, Component.literal(
                    Minecraft.getInstance().font.plainSubstrByWidth(text, MANAGE_CTRL_X - 8)),
                    0, 2, color, false);
        }
    }

    private class ManageSlotInline extends AbstractComponent<ManageSlotInline> {
        ManageSlotInline(int x, int y) {
            super(null, x, y, 18, 18);
            setOnClickEvent((comp, screen, mx, my, button) -> {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    ItemStack held = player.getMainHandItem();
                    if (!held.isEmpty()) {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
                        PacketDistributor.sendToServer(new RealCivPayloads.SetTaxItemPayload(id.toString()));
                    }
                }
                return true;
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(0, 0, 18, 18, 0xFF555555);
            graphics.fill(1, 1, 17, 17, 0xFF2A2E3A);

            ResourceLocation itemId = ResourceLocation.parse(snapshot.taxItemId());
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item);
                graphics.renderItem(stack, 1, 1);
                if (snapshot.taxItemCount() > 1) {
                    graphics.renderItemDecorations(Minecraft.getInstance().font,
                            new ItemStack(item, snapshot.taxItemCount()), 1, 1);
                }
            }

            if (isTotalHovered(mouseX, mouseY)) {
                graphics.fill(0, 0, 18, 18, 0x40FFFFFF);
            }
        }
    }

    // -- Member row with click-to-cycle --

    private class MemberRowComponent extends AbstractComponent<MemberRowComponent> {
        private final int index;
        private final TaxSnapshot.MemberRow member;

        MemberRowComponent(int x, int y, int index, TaxSnapshot.MemberRow member) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.index = index;
            this.member = member;
            setOnClickEvent((comp, screen, mx, my, button) -> {
                if (snapshot.canManage()) {
                    sendAction(ACTION_MEMBER_CLICK * 100 + index);
                    return true;
                }
                return false;
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isTotalHovered(mouseX, mouseY);
            if (hovered) {
                graphics.fill(-2, -1, CONTENT_WIDTH + 2, 13, 0x20FFFFFF);
            }

            var font = Minecraft.getInstance().font;
            String prefix = member.isLeader() ? "\u2605 " : "  ";
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(prefix + member.name(), 100)),
                    COL_NAME, 0, member.isLeader() ? 0xFFFFD54F : 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(member.ownedPlots())),
                    COL_PLOTS, 0, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(member.delinquentPlots())),
                    COL_DEL, 0, member.delinquentPlots() > 0 ? 0xFFF44336 : 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(RealCivUtil.formatCredits(member.karmaCents())),
                    COL_KARMA, 0, 0xFFF4F7FA, false);
        }
    }
}
