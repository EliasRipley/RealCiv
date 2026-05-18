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
                graphics.drawString(font, Component.literal("Click to cycle relation state"),
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
