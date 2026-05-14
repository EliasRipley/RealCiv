package com.realciv.realciv.client;

import com.daqem.uilib.api.client.gui.component.scroll.ScrollOrientation;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.io.ButtonComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollBarComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollContentComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollPanelComponent;
import com.realciv.realciv.diplomacy.DiplomacySnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernDiplomacyScreen extends RealCivUIScreen {
    public static final int ACTION_CYCLE_RELATION = 1;
    public static final int ACTION_PREV_PAGE = 2;
    public static final int ACTION_NEXT_PAGE = 3;

    private static final int SCROLL_X = 14;
    private static final int SCROLL_Y = 52;
    private static final int SCROLL_W = PANEL_WIDTH - 42;
    private static final int SCROLL_H = 162;
    private static final int SCROLL_BAR_X = PANEL_WIDTH - 26;
    private static final int SCROLL_BAR_W = 6;

    // Table columns
    private static final int COL_NAME = 4;
    private static final int COL_STATUS = 160;
    private static final int COL_CAS = 260;
    private static final int[] COLS = {COL_NAME, COL_STATUS, COL_CAS};
    private static final String[] HEADERS = {"Civilization", "Status", "Casualties"};

    private DiplomacySnapshot snapshot;
    private ScrollContentComponent scrollContent;

    public ModernDiplomacyScreen(DiplomacySnapshot snapshot) {
        super(Component.literal("Diplomacy Table"), "Relations, alliances, war, and casualties", 0xFFD64545);
        this.snapshot = snapshot;
    }

    public void refresh(DiplomacySnapshot newSnapshot) {
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

        for (int i = 0; i < snapshot.relations().size(); i++) {
            scrollContent.addChild(new RelationRowComponent(0, 0, i, snapshot.relations().get(i)));
        }

        if (snapshot.relations().isEmpty()) {
            scrollContent.addChild(new RowLabel(0, 0, "No diplomatic relations.", 0xFF78909C));
        }
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_DIPLOMACY, actionId));
    }

    private class RelationRowComponent extends AbstractComponent<RelationRowComponent> {
        private final int index;
        private final DiplomacySnapshot.RelationRow relation;

        RelationRowComponent(int x, int y, int index, DiplomacySnapshot.RelationRow relation) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.index = index;
            this.relation = relation;
            setOnClickEvent((comp, screen, mx, my, button) -> {
                if (snapshot.canManage()) {
                    sendAction(ACTION_CYCLE_RELATION * 100 + index);
                    return true;
                }
                return false;
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isTotalHovered(mouseX, mouseY) && snapshot.canManage();
            if (hovered) {
                graphics.fill(-2, -1, CONTENT_WIDTH + 2, 13, 0x20FFFFFF);
            }

            int statusColor = switch (relation.state()) {
                case "ALLY" -> 0xFF4CAF50;
                case "WAR" -> 0xFFF44336;
                default -> 0xFFB0BEC5;
            };

            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(relation.otherCivName(), 120)),
                    COL_NAME, 0, 0xFFF4F7FA, false);

            graphics.drawString(font, Component.literal(relation.state()),
                    COL_STATUS, 0, statusColor, false);

            String casualties = "";
            if ("WAR".equals(relation.state())) {
                casualties = "us:" + relation.ourCasualties() + " them:" + relation.theirCasualties();
            } else if ("ALLY".equals(relation.state())) {
                casualties = "\u2713 allied";
            }
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(casualties, 100)),
                    COL_CAS, 0, 0xFF78909C, false);

            if (hovered) {
                graphics.drawString(font, Component.literal("Click to cycle relation state"),
                        COL_CAS, -10, 0x60FFFFFF, false);
            }
        }
    }
}
