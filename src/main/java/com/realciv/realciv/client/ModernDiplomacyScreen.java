package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import com.realciv.realciv.diplomacy.DiplomacySnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernDiplomacyScreen extends RealCivScreen {
    public static final int ACTION_CYCLE_RELATION = 1;
    public static final int ACTION_PREV_PAGE = 2;
    public static final int ACTION_NEXT_PAGE = 3;
    public static final int ACTION_TOGGLE_WAR_TYPE = 4;
    public static final int ACTION_PVP_TARGET_DOWN = 5;
    public static final int ACTION_PVP_TARGET_UP = 6;
    public static final int ACTION_SET_WAR_TERM = 7;
    public static final int ACTION_GAMBLE_AMOUNT_DOWN = 8;
    public static final int ACTION_GAMBLE_AMOUNT_UP = 9;
    public static final int ACTION_ACCEPT_WAR_REQUEST = 20_000;
    public static final int ACTION_REJECT_WAR_REQUEST = 21_000;

    private DiplomacySnapshot snapshot;
    private IntTextBox gambleAmountInput;
    private static final int PAGE_Y = FOOTER_Y;
    private static final int GAMBLE_CTRL_X = 310;

    public ModernDiplomacyScreen(DiplomacySnapshot snapshot) {
        super(Component.literal("Diplomacy Table"), "Relations, alliances, war, and casualties", 0xFFD64545);
        this.snapshot = snapshot;
    }

    public void refresh(DiplomacySnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        this.gambleAmountInput = null;
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        SimpleTextButton prev = makeFixedBtn(ACTION_PREV_PAGE, "< Prev", 50);
        prev.setPos(10, PAGE_Y);
        add(prev);

        SimpleTextButton next = makeFixedBtn(ACTION_NEXT_PAGE, "Next >", 50);
        next.setPos(PANEL_W - 60, PAGE_Y);
        add(next);
    }

    @Override
    protected void addScrollContent(Panel panel) {
        addIdentitySection(snapshot.civDisplayName(), "", snapshot.canManage());
        addSection("Diplomacy Overview", 0xFFE57373);
        addLabelRow("Relations listed", String.valueOf(snapshot.relations().size()));
        addLabelRow("Page", (snapshot.page() + 1) + "/" + Math.max(1, snapshot.totalPages()), 0xFF9DB0C2);

        if (snapshot.canManage()) {
            addSpacer(4);
            addSection("War Declaration Draft", 0xFFE57373);
            addLabelRow("Type", snapshot.draftWarType(), 0xFFF4F7FA);
            if ("PVP".equalsIgnoreCase(snapshot.draftWarType())) {
                addLabelRow("PvP kill target", String.valueOf(snapshot.draftPvpKillTarget()), 0xFFF4F7FA);
            }

            addRowWithButtons(
                    "War type",
                    "Used when relation request becomes WAR.",
                    makeInlineBtn(ACTION_TOGGLE_WAR_TYPE, "Toggle", 58));
            if ("PVP".equalsIgnoreCase(snapshot.draftWarType())) {
                addRowWithButtons(
                        "PvP target",
                        "First side to this kill count wins.",
                        makeInlineBtn(ACTION_PVP_TARGET_DOWN, "-1", 28),
                        makeInlineBtn(ACTION_PVP_TARGET_UP, "+1", 28));
            }

            addSpacer(2);
            addSection("War Terms (select one)", 0xFFE57373);

            String currentTerm = snapshot.draftWarTerm();
            String[] termValues = {"none", "submission", "land", "gamble"};
            String[] termLabels = {"None", "Vassalage", "Land Claim", "Resource Gamble"};
            for (int i = 0; i < termValues.length; i++) {
                boolean isActive = termValues[i].equals(currentTerm);
                String display = (isActive ? "[x] " : "[  ] ") + termLabels[i];
                int color = isActive ? 0xFFA5D6A7 : 0xFF9BA9B7;
                int idx = i;
                addSelectableRow(display, color, btn -> sendAction(ACTION_SET_WAR_TERM + idx));
            }

            if ("gamble".equals(currentTerm)) {
                addSpacer(2);
                addLabelRow("Gamble item", snapshot.draftGambleItemId() == null ? "none" : snapshot.draftGambleItemId(), 0xFFF4F7FA);
                addLabelRow("Gamble amount", String.valueOf(snapshot.draftGambleAmount()), 0xFFF4F7FA);

                panel.add(new LabelWidget(panel, "Set item from held", COL_LABEL, currentY, 0xFF78909C));
                GambleItemSlot slot = new GambleItemSlot(panel, GAMBLE_CTRL_X, currentY, snapshot);
                panel.add(slot);
                addInlineBtnToPanel(panel, ACTION_GAMBLE_AMOUNT_DOWN, "-1", 26, GAMBLE_CTRL_X + 22, currentY);
                addInlineBtnToPanel(panel, ACTION_GAMBLE_AMOUNT_UP, "+1", 26, GAMBLE_CTRL_X + 52, currentY);
                gambleAmountInput = makeGambleAmountInput(panel);
                gambleAmountInput.setPosAndSize(GAMBLE_CTRL_X + 82, currentY, 40, BTN_H);
                panel.add(gambleAmountInput);
                SimpleTextButton setAmountBtn = makePanelBtn(panel, "Set", button -> submitGambleAmountFromInput());
                setAmountBtn.setPosAndSize(GAMBLE_CTRL_X + 126, currentY, 34, BTN_H);
                panel.add(setAmountBtn);
                currentY += ROW_H + 4;

                addLabelRow("", "Hold item in hand, click slot to set.", 0xFF78909C);
            }

            addSpacer(4);
            addSection("Incoming War Requests", 0xFFE57373);
            if (snapshot.incomingWarRequests().isEmpty()) {
                addLabelRow("", "No pending WAR declarations.", 0xFF78909C);
            } else {
                for (int i = 0; i < snapshot.incomingWarRequests().size(); i++) {
                    DiplomacySnapshot.IncomingWarRequest request = snapshot.incomingWarRequests().get(i);
                    String mode = "PVP".equalsIgnoreCase(request.warType())
                            ? "PVP target " + request.pvpKillTarget()
                            : "DESTRUCTION";
                    String terms = "submission:" + (request.warOfSubmission() ? "on" : "off")
                            + ", land:" + (request.warOfLand() ? "on" : "off")
                            + (request.warResourceGamble() ? ", gamble:" + request.warGambleAmount() + "x " + request.warGambleItemId() : "");
                    addRowWithButtons(
                            "From " + request.requesterCivName(),
                            mode + " | " + terms,
                            makeInlineBtn(ACTION_ACCEPT_WAR_REQUEST + i, "Accept", 52),
                            makeInlineBtn(ACTION_REJECT_WAR_REQUEST + i, "Reject", 52));
                }
            }
        }

        addSpacer(4);
        addSection("Relation Matrix", 0xFFC6D2DE);

        panel.add(new LabelWidget(panel, "Civilization", 4, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Status", 160, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Casualties", 260, currentY, 0xFF78909C));
        currentY += ROW_H;

        for (int i = 0; i < snapshot.relations().size(); i++) {
            panel.add(new RelationRowWidget(panel, 4, currentY, i, snapshot.relations().get(i), snapshot.canManage(), this::sendAction));
            currentY += ROW_H;
        }

        if (snapshot.relations().isEmpty()) {
            addLabelRow("", "No diplomatic relations.", 0xFF78909C);
        }
    }

    private void addInlineBtnToPanel(Panel panel, int action, String label, int width, int x, int y) {
        SimpleTextButton btn = makeInlineBtn(action, label, width);
        btn.setPos(x, y);
        panel.add(btn);
    }

    private IntTextBox makeGambleAmountInput(Panel panel) {
        IntTextBox box = new IntTextBox(panel) {
            @Override
            public void onEnterPressed() {
                submitGambleAmountFromInput();
            }
        };
        box.setMinMax(1, 999999);
        box.setAmount(Math.max(1, (int) Math.min(Integer.MAX_VALUE, snapshot.draftGambleAmount())));
        box.ghostText = "Amt";
        return box;
    }

    private void submitGambleAmountFromInput() {
        if (gambleAmountInput == null) return;
        int next = Math.max(1, Math.min(999999, gambleAmountInput.getIntValue()));
        PacketDistributor.sendToServer(new RealCivPayloads.SetGambleAmountPayload(next));
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_DIPLOMACY, actionId));
    }

    private static class GambleItemSlot extends Widget {
        private final DiplomacySnapshot snapshot;

        GambleItemSlot(Panel parent, int x, int y, DiplomacySnapshot snapshot) {
            super(parent);
            setPosAndSize(x, y, 18, 18);
            this.snapshot = snapshot;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            graphics.fill(x, y, x + 18, y + 18, 0xFF555555);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF2A2E3A);

            String itemId = snapshot.draftGambleItemId();
            if (itemId != null && !itemId.isEmpty()) {
                ResourceLocation id = ResourceLocation.parse(itemId);
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
                if (item != Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    graphics.renderItem(stack, x + 1, y + 1);
                    long count = snapshot.draftGambleAmount();
                    if (count > 1) {
                        graphics.renderItemDecorations(Minecraft.getInstance().font,
                                new ItemStack(item, (int) Math.min(count, 99)), x + 1, y + 1);
                    }
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
                        PacketDistributor.sendToServer(new RealCivPayloads.SetGambleItemPayload(id.toString()));
                    }
                }
                return true;
            }
            return false;
        }
    }

    private static class RelationRowWidget extends Widget {
        private final int index;
        private final DiplomacySnapshot.RelationRow relation;
        private final boolean canManage;
        private final java.util.function.IntConsumer actionSender;

        RelationRowWidget(Panel parent, int x, int y, int index, DiplomacySnapshot.RelationRow relation,
                          boolean canManage, java.util.function.IntConsumer actionSender) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.index = index;
            this.relation = relation;
            this.canManage = canManage;
            this.actionSender = actionSender;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && canManage;
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }

            int statusColor = switch (relation.state()) {
                case "ALLY" -> 0xFF4CAF50;
                case "WAR" -> 0xFFF44336;
                default -> 0xFFB0BEC5;
            };

            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(relation.otherCivName(), 120)),
                    x + 4, y + 2, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(relation.state()),
                    x + 160, y + 2, statusColor, false);

            String casualties = "";
            if ("WAR".equals(relation.state())) {
                casualties = "us:" + relation.ourCasualties() + " them:" + relation.theirCasualties();
            } else if ("ALLY".equals(relation.state())) {
                casualties = "allied";
            } else {
                casualties = "-";
            }
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(casualties, 100)),
                    x + 260, y + 2, 0xFF78909C, false);

            if (hovered) {
                graphics.drawString(font, Component.literal("Click to request next relation state (WAR uses draft terms)"),
                        x + 260, y + h, 0x60FFFFFF, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver() && canManage) {
                actionSender.accept(ACTION_CYCLE_RELATION * 100 + index);
                return true;
            }
            return false;
        }
    }
}
