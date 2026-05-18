package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.icon.Icon;
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

public class ModernTaxScreen extends RealCivScreen {
    public static final int ACTION_PAY_1 = 1;
    public static final int ACTION_PAY_5 = 2;
    public static final int ACTION_PAY_25 = 3;
    public static final int ACTION_RATE_DOWN = 4;
    public static final int ACTION_RATE_UP = 5;
    public static final int ACTION_MODE_TOGGLE = 6;
    public static final int ACTION_ITEM_CYCLE = 7;
    public static final int ACTION_ITEM_COUNT_DOWN = 8;
    public static final int ACTION_ITEM_COUNT_UP = 9;
    public static final int ACTION_PREV_PAGE = 11;
    public static final int ACTION_NEXT_PAGE = 12;
    public static final int ACTION_SET_TAX_ITEM = 13;

    private static final int PAY_Y = FOOTER_Y;
    private static final int PAGE_Y = FOOTER_Y + 18;
    private static final int TAX_CTRL_X = 322;

    private TaxSnapshot snapshot;
    private boolean showManagePage;
    private IntTextBox itemCountInput;

    public ModernTaxScreen(TaxSnapshot snapshot) {
        super(Component.literal("Tax Office"), "Upkeep, prepayment, and policy", 0xFFE1B12C);
        this.snapshot = snapshot;
    }

    public void refresh(TaxSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        this.itemCountInput = null;
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        addFixedBtn(ACTION_PREV_PAGE, "< Prev", 50, 10, PAGE_Y);
        addFixedBtn(ACTION_NEXT_PAGE, "Next >", 50, PANEL_W - 60, PAGE_Y);

        if (snapshot.canManage()) {
            String label = showManagePage ? "Overview" : "Leader";
            add(makeManageToggleBtn(label));
        }

        addFixedBtn(ACTION_PAY_1, "Pay 1", 40, 10, PAY_Y);
        addFixedBtn(ACTION_PAY_5, "Pay 5", 40, 54, PAY_Y);
        addFixedBtn(ACTION_PAY_25, "Pay 25", 46, 98, PAY_Y);
    }

    private void addFixedBtn(int action, String label, int width, int x, int y) {
        SimpleTextButton btn = makeFixedBtn(action, label, width);
        btn.setPos(x, y);
        add(btn);
    }

    private SimpleTextButton makeManageToggleBtn(String label) {
        SimpleTextButton btn = new SimpleTextButton(this, Component.literal(label), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                showManagePage = !showManagePage;
                refreshWidgets();
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
        btn.setPosAndSize(PANEL_W / 2 - 34, PAGE_Y, 68, 14);
        return btn;
    }

    @Override
    protected void addScrollContent(Panel panel) {
        if (showManagePage && snapshot.canManage()) {
            populateManagePage(panel);
        } else {
            populateViewPage(panel);
        }
    }

    private void populateViewPage(Panel panel) {
        addIdentitySection(snapshot.civDisplayName(), snapshot.playerRole(), snapshot.canManage());
        addSection("Your Upkeep", 0xFFE1B12C);
        addLabelRow("Owned plots", String.valueOf(snapshot.ownedPlots()));
        addLabelRow("Delinquent plots", String.valueOf(snapshot.delinquentPlots()),
                snapshot.delinquentPlots() > 0 ? 0xFFF44336 : 0xFFF4F7FA);
        addLabelRow("Your karma", RealCivUtil.formatCredits(snapshot.playerKarmaCents()));
        addLabelRow("Next upkeep tick", String.valueOf(snapshot.nextUpkeepTick()), 0xFF9DB0C2);

        addSpacer(4);
        addSection("Civilization Tax Policy", 0xFFE1B12C);
        addLabelRow("Civ treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents()));
        addLabelRow("Rate", String.format("%.2fx", snapshot.rateMultiplier()));
        addLabelRow("Mode", snapshot.paymentMode());

        int itemColor = isKarmaMode() ? 0xFF78909C : 0xFFF4F7FA;
        addLabelRow("Tax item", snapshot.taxItemId() + " x" + snapshot.taxItemCount(), itemColor);

        long perPlotCost = isKarmaMode()
                ? (snapshot.ownedPlots() > 0 ? snapshot.cycleCostCents() / snapshot.ownedPlots() : 0L)
                : snapshot.taxItemPerPlotRate();
        addLabelRow("Per plot/cycle", String.valueOf(perPlotCost), 0xFFB0BEC5);
        addLabelRow("Cycle cost (you)", String.valueOf(isKarmaMode()
                ? snapshot.cycleCostCents() : snapshot.cycleItemCost()), 0xFFB0BEC5);

        addSpacer(6);

        if (snapshot.canManage()) {
            String pageLabel = "Member Upkeep (page " + (snapshot.memberPage() + 1) + "/" + snapshot.totalMemberPages() + ")";
            addSection(pageLabel, 0xFF9DB0C2);

            panel.add(new LabelWidget(panel, "Name", 4, currentY, 0xFF78909C));
            panel.add(new LabelWidget(panel, "Plots", 130, currentY, 0xFF78909C));
            panel.add(new LabelWidget(panel, "Del", 180, currentY, 0xFF78909C));
            panel.add(new LabelWidget(panel, "Karma", 230, currentY, 0xFF78909C));
            currentY += ROW_H;

            for (int i = 0; i < snapshot.members().size(); i++) {
                TaxSnapshot.MemberRow m = snapshot.members().get(i);
                panel.add(new MemberRowWidget(panel, 4, currentY, m));
                currentY += ROW_H;
            }
        } else {
            addLabelRow("", "Leadership sees member tax records and can adjust policy.", 0xFF78909C);
        }
    }

    private void populateManagePage(Panel panel) {
        addIdentitySection(snapshot.civDisplayName(), snapshot.playerRole(), true);
        addSection("Leader Controls", 0xFFE1B12C);
        addLabelRow("Member page", (snapshot.memberPage() + 1) + "/" + Math.max(1, snapshot.totalMemberPages()), 0xFF9DB0C2);
        addSpacer(2);

        addSection("Tax Rate", 0xFFE1B12C);
        addLabelRow("Current rate", String.format("%.2fx", snapshot.rateMultiplier()));
        addRowWithButtons("Adjust rate", "Adjust plot tax multiplier.",
                makeInlineBtn(ACTION_RATE_DOWN, "-10%", 46),
                makeInlineBtn(ACTION_RATE_UP, "+10%", 46));

        addSpacer(4);
        addSection("Payment Mode", 0xFFE1B12C);
        addLabelRow("Current mode", snapshot.paymentMode());
        addRowWithButtons("Toggle mode", "Switch karma/item mode.",
                makeInlineBtn(ACTION_MODE_TOGGLE, "Toggle", 48));

        addSpacer(4);
        addSection("Tax Item", 0xFFE1B12C);
        addLabelRow("Current item", snapshot.taxItemId() + " x" + snapshot.taxItemCount(),
                isKarmaMode() ? 0xFF78909C : 0xFFF4F7FA);

        panel.add(new LabelWidget(panel, "Set from held item", COL_LABEL, currentY, 0xFF78909C));
        TaxItemSlot slot = new TaxItemSlot(panel, TAX_CTRL_X, currentY, snapshot);
        panel.add(slot);
        addInlineBtnToPanel(panel, ACTION_ITEM_COUNT_DOWN, "-C", 26, TAX_CTRL_X + 22, currentY);
        addInlineBtnToPanel(panel, ACTION_ITEM_COUNT_UP, "+C", 26, TAX_CTRL_X + 52, currentY);
        itemCountInput = makeItemCountInput(panel);
        itemCountInput.setPosAndSize(TAX_CTRL_X + 82, currentY, 40, BTN_H);
        panel.add(itemCountInput);
        SimpleTextButton setCountBtn = makePanelBtn(panel, "Set", button -> submitItemCountFromInput());
        setCountBtn.setPosAndSize(TAX_CTRL_X + 126, currentY, 34, BTN_H);
        panel.add(setCountBtn);
        currentY += ROW_H + 4;

        addLabelRow("", "Hold item, click slot to set.", 0xFF78909C);
        addLabelRow("", "Use Qty + Enter/Set to apply.", 0xFF78909C);
        addLabelRow("", "Use Prev/Next below for members.", 0xFF78909C);
    }

    private void addInlineBtnToPanel(Panel panel, int action, String label, int width, int x, int y) {
        SimpleTextButton btn = makeInlineBtn(action, label, width);
        btn.setPos(x, y);
        panel.add(btn);
    }

    private IntTextBox makeItemCountInput(Panel panel) {
        IntTextBox box = new IntTextBox(panel) {
            @Override
            public void onEnterPressed() {
                submitItemCountFromInput();
            }
        };
        box.setMinMax(1, 9999);
        box.setAmount(Math.max(1, snapshot.taxItemCount()));
        box.ghostText = "Qty";
        return box;
    }

    private void submitItemCountFromInput() {
        if (itemCountInput == null) {
            return;
        }
        int next = Math.max(1, Math.min(9999, itemCountInput.getIntValue()));
        PacketDistributor.sendToServer(new RealCivPayloads.SetTaxItemCountPayload(next));
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_TAX, actionId));
    }

    private boolean isKarmaMode() {
        return snapshot.paymentMode().toLowerCase(java.util.Locale.ROOT).contains("karma");
    }

    private static class MemberRowWidget extends Widget {
        private final TaxSnapshot.MemberRow member;

        MemberRowWidget(Panel parent, int x, int y, TaxSnapshot.MemberRow member) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.member = member;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver();
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x18FFFFFF);
            }
            var font = Minecraft.getInstance().font;
            String prefix = member.isLeader() ? "* " : "  ";
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(prefix + member.name(), 100)),
                    x + 4, y + 2, member.isLeader() ? 0xFFFFD54F : 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(member.ownedPlots())),
                    x + 130, y + 2, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(member.delinquentPlots())),
                    x + 180, y + 2, member.delinquentPlots() > 0 ? 0xFFF44336 : 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(RealCivUtil.formatCredits(member.karmaCents())),
                    x + 230, y + 2, 0xFFF4F7FA, false);
        }
    }

    private static class TaxItemSlot extends Widget {
        private final TaxSnapshot snapshot;

        TaxItemSlot(Panel parent, int x, int y, TaxSnapshot snapshot) {
            super(parent);
            setPosAndSize(x, y, 18, 18);
            this.snapshot = snapshot;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            graphics.fill(x, y, x + 18, y + 18, 0xFF555555);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF2A2E3A);

            ResourceLocation itemId = ResourceLocation.parse(snapshot.taxItemId());
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item);
                graphics.renderItem(stack, x + 1, y + 1);
                if (snapshot.taxItemCount() > 1) {
                    graphics.renderItemDecorations(Minecraft.getInstance().font,
                            new ItemStack(item, snapshot.taxItemCount()), x + 1, y + 1);
                }
            }
            if (isMouseOver()) {
                graphics.fill(x, y, x + 18, y + 18, 0x40FFFFFF);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver()) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    ItemStack held = player.getMainHandItem();
                    if (!held.isEmpty()) {
                        ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
                        PacketDistributor.sendToServer(new RealCivPayloads.SetTaxItemPayload(id.toString()));
                    }
                }
                return true;
            }
            return false;
        }
    }
}
