package com.realciv.realciv.client;

import com.realciv.realciv.diplomacy.DiplomacySnapshot;
import com.realciv.realciv.network.RealCivPayloads;
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

public class ModernDiplomacyScreen extends RealCivPanelScreen {
    public static final int ACTION_CYCLE_RELATION = 1;
    public static final int ACTION_PREV_PAGE = 2;
    public static final int ACTION_NEXT_PAGE = 3;

    private static final int ROW_WIDTH = PANEL_WIDTH - 32;

    private DiplomacySnapshot snapshot;

    public ModernDiplomacyScreen(DiplomacySnapshot snapshot) {
        super(Component.literal("Diplomacy Table"), "Relations, alliances, war, and casualties", 0xFFD64545);
        this.snapshot = snapshot;
    }

    public void refresh(DiplomacySnapshot newSnapshot) {
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
        }.setPosAndSize(10, 40, 20, 16));

        add(new NordButton(this, Component.literal(">"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_NEXT_PAGE); }
        }.setPosAndSize(PANEL_WIDTH - 30, 40, 20, 16));
    }

    @Override
    protected void populateContentPanel(Panel panel) {
        String pageInfo = "Page " + (snapshot.page() + 1) + "/" + snapshot.totalPages();
        panel.add(createTextWidget(pageInfo, 0xFF9DB0C2));

        for (int i = 0; i < snapshot.relations().size(); i++) {
            panel.add(new RelationRowWidget(panel, i, snapshot.relations().get(i)));
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

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_DIPLOMACY, actionId));
    }

    private class RelationRowWidget extends Widget {
        private final int index;
        private final DiplomacySnapshot.RelationRow relation;

        RelationRowWidget(Panel panel, int index, DiplomacySnapshot.RelationRow relation) {
            super(panel);
            this.index = index;
            this.relation = relation;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            int color = switch (relation.state()) {
                case "ALLY" -> 0xFF4CAF50;
                case "WAR" -> 0xFFF44336;
                default -> 0xFFB0BEC5;
            };

            String line = relation.otherCivName() + "  [" + relation.state() + "]";
            if ("WAR".equals(relation.state())) {
                line += "  K: us=" + relation.ourCasualties() + " them=" + relation.theirCasualties();
            }

            boolean hovered = isMouseOver() && snapshot.canManage();
            if (hovered) {
                graphics.fill(x - 2, y - 1, x + w + 2, y + h + 1, 0x30FFFFFF);
            }

            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(line, ROW_WIDTH)), x, y, color, false);
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (button.isLeft() && snapshot.canManage()) {
                sendAction(ACTION_CYCLE_RELATION * 100 + index);
                return true;
            }
            return false;
        }
    }
}
