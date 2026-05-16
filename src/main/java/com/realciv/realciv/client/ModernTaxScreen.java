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
    public static final int ACTION_MEMBER_CLICK = 10;
    public static final int ACTION_PREV_PAGE = 11;
    public static final int ACTION_NEXT_PAGE = 12;
    public static final int ACTION_SET_TAX_ITEM = 13;

    private static final int PAY_Y = 250;
    private static final int PAGE_Y = 2;

    private TaxSnapshot snapshot;
    private boolean showManagePage;

    public ModernTaxScreen(TaxSnapshot snapshot) {
        super(Component.literal("Tax Office"), "Upkeep, prepayment, and policy", 0xFFE1B12C);
        this.snapshot = snapshot;
    }

    public void refresh(TaxSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        addFixedBtn(ACTION_PREV_PAGE, "<", 18, 2, PAGE_Y);
        addFixedBtn(ACTION_NEXT_PAGE, ">", 18, PANEL_W - 20, PAGE_Y);

        if (snapshot.canManage()) {
            String label = showManagePage ? "View" : "Admin";
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
        btn.setPosAndSize(PANEL_W / 2 - 22, PAGE_Y, 44, 14);
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
        addLabelRow("Your plots", String.valueOf(snapshot.ownedPlots()));
        addLabelRow("Delinquent", String.valueOf(snapshot.delinquentPlots()));
        addLabelRow("Karma", RealCivUtil.formatCredits(snapshot.playerKarmaCents()));
        addLabelRow("Civ Treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents()));
        addLabelRow("Rate", String.format("%.2fx", snapshot.rateMultiplier()));
        addLabelRow("Mode", snapshot.paymentMode());

        int itemColor = "karma".equalsIgnoreCase(snapshot.paymentMode()) ? 0xFF78909C : 0xFFF4F7FA;
        addLabelRow("Tax item", snapshot.taxItemId() + " x" + snapshot.taxItemCount(), itemColor);

        long perPlotCost = "karma".equalsIgnoreCase(snapshot.paymentMode())
                ? snapshot.cycleCostCents() : snapshot.taxItemPerPlotRate();
        addLabelRow("Per plot/cycle", String.valueOf(perPlotCost), 0xFFB0BEC5);

        addSpacer(6);

        if (snapshot.canManage()) {
            String pageLabel = "Members (page " + (snapshot.memberPage() + 1) + "/" + snapshot.totalMemberPages() + ")";
            addSection(pageLabel, 0xFF9DB0C2);

            panel.add(new LabelWidget(panel, "Name", 4, currentY, 0xFF78909C));
            panel.add(new LabelWidget(panel, "Plots", 130, currentY, 0xFF78909C));
            panel.add(new LabelWidget(panel, "Del", 180, currentY, 0xFF78909C));
            panel.add(new LabelWidget(panel, "Karma", 230, currentY, 0xFF78909C));
            currentY += ROW_H;

            for (int i = 0; i < snapshot.members().size(); i++) {
                int idx = i;
                TaxSnapshot.MemberRow m = snapshot.members().get(i);
                panel.add(new MemberRowWidget(panel, 4, currentY, m, idx, snapshot.canManage(),
                        () -> sendAction(ACTION_MEMBER_CLICK * 100 + idx)));
                currentY += ROW_H;
            }
        } else {
            addLabelRow("", "Leadership sees all tax records and can adjust policy.", 0xFF78909C);
        }
    }

    private void populateManagePage(Panel panel) {
        addSection("Tax Rate", 0xFFE1B12C);
        addLabelRow("Current rate", String.format("%.2fx", snapshot.rateMultiplier()));
        addRowWithButtons("Adjust rate", "Adjust the tax multiplier applied to all plots.",
                makeInlineBtn(ACTION_RATE_DOWN, "-10%", 46),
                makeInlineBtn(ACTION_RATE_UP, "+10%", 46));

        addSpacer(4);
        addSection("Payment Mode", 0xFFE1B12C);
        addLabelRow("Current mode", snapshot.paymentMode());
        addRowWithButtons("Toggle mode", "Switch between karma and item-based tax collection.",
                makeInlineBtn(ACTION_MODE_TOGGLE, "Toggle", 48));

        addSpacer(4);
        addSection("Tax Item", 0xFFE1B12C);
        addLabelRow("Current item", snapshot.taxItemId() + " x" + snapshot.taxItemCount(),
                "karma".equalsIgnoreCase(snapshot.paymentMode()) ? 0xFF78909C : 0xFFF4F7FA);

        panel.add(new LabelWidget(panel, "Click slot while holding item to set.", COL_LABEL, currentY, 0xFF78909C));
        TaxItemSlot slot = new TaxItemSlot(panel, COL_BTNS, currentY, snapshot);
        panel.add(slot);
        addInlineBtnToPanel(panel, ACTION_ITEM_COUNT_DOWN, "-C", 26, COL_BTNS + 22, currentY);
        addInlineBtnToPanel(panel, ACTION_ITEM_COUNT_UP, "+C", 26, COL_BTNS + 52, currentY);
        currentY += ROW_H + 4;

        addSpacer(8);
        addLabelRow("", "Use the < > buttons to page through members.", 0xFF78909C);
    }

    private void addInlineBtnToPanel(Panel panel, int action, String label, int width, int x, int y) {
        SimpleTextButton btn = makeInlineBtn(action, label, width);
        btn.setPos(x, y);
        panel.add(btn);
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_TAX, actionId));
    }

    private static class MemberRowWidget extends Widget {
        private final TaxSnapshot.MemberRow member;
        private final boolean canManage;
        private final Runnable onClick;

        MemberRowWidget(Panel parent, int x, int y, TaxSnapshot.MemberRow member, int idx, boolean canManage, Runnable onClick) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.member = member;
            this.canManage = canManage;
            this.onClick = onClick;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && canManage;
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
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

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver() && canManage) {
                onClick.run();
                return true;
            }
            return false;
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
