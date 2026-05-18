package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import com.realciv.realciv.diplomacy.DiplomacySnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernDiplomacyScreen extends RealCivScreen {
    public static final int ACTION_CYCLE_RELATION = 1;
    public static final int ACTION_PREV_PAGE = 2;
    public static final int ACTION_NEXT_PAGE = 3;
    public static final int ACTION_TOGGLE_WAR_TYPE = 4;
    public static final int ACTION_PVP_TARGET_DOWN = 5;
    public static final int ACTION_PVP_TARGET_UP = 6;
    public static final int ACTION_TOGGLE_WAR_SUBMISSION = 7;
    public static final int ACTION_TOGGLE_WAR_LAND = 8;
    public static final int ACTION_TOGGLE_WAR_GAMBLE = 9;
    public static final int ACTION_GAMBLE_ITEM_CYCLE = 10;
    public static final int ACTION_GAMBLE_AMOUNT_DOWN = 11;
    public static final int ACTION_GAMBLE_AMOUNT_UP = 12;
    public static final int ACTION_ACCEPT_WAR_REQUEST = 20_000;
    public static final int ACTION_REJECT_WAR_REQUEST = 21_000;

    private DiplomacySnapshot snapshot;
    private static final int PAGE_Y = FOOTER_Y;

    public ModernDiplomacyScreen(DiplomacySnapshot snapshot) {
        super(Component.literal("Diplomacy Table"), "Relations, alliances, war, and casualties", 0xFFD64545);
        this.snapshot = snapshot;
    }

    public void refresh(DiplomacySnapshot newSnapshot) {
        this.snapshot = newSnapshot;
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
            addLabelRow("War of Submission", snapshot.draftWarOfSubmission() ? "ON" : "OFF",
                    snapshot.draftWarOfSubmission() ? 0xFFFFD54F : 0xFF90A4AE);
            addLabelRow("War of Land", snapshot.draftWarOfLand() ? "ON" : "OFF",
                    snapshot.draftWarOfLand() ? 0xFFFFD54F : 0xFF90A4AE);
            addLabelRow("Resource Gamble", snapshot.draftWarResourceGamble() ? "ON" : "OFF",
                    snapshot.draftWarResourceGamble() ? 0xFFFFD54F : 0xFF90A4AE);
            if (snapshot.draftWarResourceGamble()) {
                String item = snapshot.draftGambleItemId();
                if (item != null && item.contains(":")) {
                    item = item.substring(item.lastIndexOf(':') + 1);
                }
                addLabelRow("Gamble item", item == null ? "none" : item, 0xFFF4F7FA);
                addLabelRow("Gamble amount", String.valueOf(snapshot.draftGambleAmount()), 0xFFF4F7FA);
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
            addRowWithButtons(
                    "Submission term",
                    "Winner can vassalize loser if checked.",
                    makeInlineBtn(ACTION_TOGGLE_WAR_SUBMISSION, snapshot.draftWarOfSubmission() ? "ON" : "OFF", 42));
            addRowWithButtons(
                    "Land term",
                    "Winner can take all loser land if checked.",
                    makeInlineBtn(ACTION_TOGGLE_WAR_LAND, snapshot.draftWarOfLand() ? "ON" : "OFF", 42));
            addRowWithButtons(
                    "Resource gamble",
                    "Both sides wager resources; winner takes loser's wager.",
                    makeInlineBtn(ACTION_TOGGLE_WAR_GAMBLE, snapshot.draftWarResourceGamble() ? "ON" : "OFF", 42));
            if (snapshot.draftWarResourceGamble()) {
                addRowWithButtons(
                        "Gamble item",
                        "Item to wager from hub stock.",
                        makeInlineBtn(ACTION_GAMBLE_ITEM_CYCLE, "Cycle", 48));
                addRowWithButtons(
                        "Gamble amount",
                        "How many of the item to wager.",
                        makeInlineBtn(ACTION_GAMBLE_AMOUNT_DOWN, "-1", 28),
                        makeInlineBtn(ACTION_GAMBLE_AMOUNT_UP, "+1", 28));
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

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_DIPLOMACY, actionId));
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
