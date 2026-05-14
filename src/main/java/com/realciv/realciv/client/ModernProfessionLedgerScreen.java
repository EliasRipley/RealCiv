package com.realciv.realciv.client;

import com.daqem.uilib.api.client.gui.component.scroll.ScrollOrientation;
import com.daqem.uilib.client.gui.component.scroll.ScrollPanelComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollContentComponent;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshot;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ModernProfessionLedgerScreen extends RealCivUIScreen {
    private static final int SCROLL_X = 14;
    private static final int SCROLL_Y = 48;
    private static final int SCROLL_W = PANEL_WIDTH - 42;
    private static final int SCROLL_H = 168;
    private static final int SCROLL_BAR_X = PANEL_WIDTH - 26;
    private static final int SCROLL_BAR_W = 6;

    private static final int COL_LABEL = 0;
    private static final int COL_VALUE = 180;
    private static final int COL_XP_BAR = 4;

    private final ProfessionLedgerSnapshot snapshot;

    public ModernProfessionLedgerScreen(ProfessionLedgerSnapshot snapshot) {
        super(Component.literal("Profession Ledger"), "Your skills, levels, and progression", 0xFF8BC34A);
        this.snapshot = snapshot;
    }

    @Override
    protected void addScreenWidgets() {
        ScrollContentComponent content = new ScrollContentComponent(0, 0, 2, ScrollOrientation.VERTICAL);
        populateScrollContent(content);

        addComponent(new ScrollPanelComponent(null, panelX + SCROLL_X, panelY + SCROLL_Y, SCROLL_W, SCROLL_H,
                ScrollOrientation.VERTICAL, content,
                createScrollBar(SCROLL_BAR_X, 0, SCROLL_BAR_W, SCROLL_H, ScrollOrientation.VERTICAL)));
    }

    private void populateScrollContent(ScrollContentComponent content) {
        // Player header
        content.addChild(new SectionLabel(0, 0, "Player Info", 0xFFA5D6A7));
        content.addChild(new LabelValueRow(0, 0, "Name", snapshot.playerName(), 0xFFF4F7FA));
        content.addChild(new LabelValueRow(0, 0, "Civilization", snapshot.civName(), 0xFFF4F7FA));
        content.addChild(new LabelValueRow(0, 0, "Karma", RealCivUtil.formatCredits(snapshot.karmaCents()), 0xFFF4F7FA));

        content.addChild(new SpacerRow(0, 0, 4));

        // General level
        content.addChild(new SectionLabel(0, 0, "General Progress", 0xFFA5D6A7));
        content.addChild(new LabelValueRow(0, 0, "Level", String.valueOf(snapshot.generalLevel()), 0xFFFFD54F));
        content.addChild(new LabelValueRow(0, 0, "Experience", String.valueOf(snapshot.generalXp()), 0xFFF4F7FA));
        content.addChild(new XpBarRow(0, 0, snapshot.generalXp()));

        String top = snapshot.topProfessionLine();
        if (!top.isEmpty()) {
            content.addChild(new SpacerRow(0, 0, 2));
            content.addChild(new LabelValueRow(0, 0, "Top Profession", top, 0xFF8BC34A));
        }

        String focus = snapshot.focusLine();
        if (!focus.isEmpty()) {
            content.addChild(new LabelValueRow(0, 0, "Focus", focus, 0xFF90CAF9));
        }

        content.addChild(new SpacerRow(0, 0, 6));

        // Professions list
        content.addChild(new SectionLabel(0, 0, "Professions (" + snapshot.professions().size() + ")", 0xFFA5D6A7));

        for (ProfessionLedgerSnapshot.ProfessionRow r : snapshot.professions()) {
            content.addChild(new ProfessionRowComponent(0, 0, r));
        }

        content.addChild(new SpacerRow(0, 0, 8));
        content.addChild(new LabelValueRow(0, 0, "", "Levels and limits follow server rules.", 0xFF78909C));
    }

    private static class XpBarRow extends com.daqem.uilib.client.gui.component.AbstractComponent<XpBarRow> {
        private final int xp;

        XpBarRow(int x, int y, int xp) {
            super(null, x, y, CONTENT_WIDTH, 8);
            this.xp = xp;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(COL_XP_BAR, 0, COL_XP_BAR + CONTENT_WIDTH - 8, 6, 0xFF2A2E3A);
            int fillW = Math.min(CONTENT_WIDTH - 8, Math.max(2, (xp % 1000) * (CONTENT_WIDTH - 12) / 1000));
            graphics.fill(COL_XP_BAR + 1, 1, COL_XP_BAR + 1 + fillW, 5, 0xFF66BB6A);
        }
    }

    private static class ProfessionRowComponent extends com.daqem.uilib.client.gui.component.AbstractComponent<ProfessionRowComponent> {
        private final ProfessionLedgerSnapshot.ProfessionRow row;

        ProfessionRowComponent(int x, int y, ProfessionLedgerSnapshot.ProfessionRow row) {
            super(null, x, y, CONTENT_WIDTH, 24);
            this.row = row;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(row.name()),
                    COL_LABEL, 0, 0xFFF5F5F5, false);

            String levelStr = "Lv " + row.level();
            graphics.drawString(font, Component.literal(levelStr),
                    COL_VALUE, 0, 0xFFFFD54F, false);

            // XP line
            String xpStr = "XP: " + row.xp();
            graphics.drawString(font, Component.literal(xpStr),
                    COL_XP_BAR, 12, 0xFF9BA9B7, false);

            String action = row.actionLine();
            if (!action.isEmpty()) {
                int actionX = COL_XP_BAR + font.width(xpStr) + 6;
                graphics.drawString(font, Component.literal(font.plainSubstrByWidth(action,
                        CONTENT_WIDTH - actionX)), actionX, 12, 0xFF78909C, false);
            }

            if (isTotalHovered(mouseX, mouseY)) {
                graphics.fill(-2, -1, CONTENT_WIDTH + 2, getHeight(), 0x15FFFFFF);
            }
        }
    }
}
