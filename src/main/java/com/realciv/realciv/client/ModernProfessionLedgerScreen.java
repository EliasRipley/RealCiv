package com.realciv.realciv.client;

import com.realciv.realciv.ledger.ProfessionLedgerSnapshot;
import com.realciv.realciv.logic.RealCivUtil;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ModernProfessionLedgerScreen extends RealCivPanelScreen {
    private static final int ROW_WIDTH = PANEL_WIDTH - 32;

    private final ProfessionLedgerSnapshot snapshot;

    public ModernProfessionLedgerScreen(ProfessionLedgerSnapshot snapshot) {
        super(Component.literal("Profession Ledger"), "Your skills, levels, and progression", 0xFF8BC34A);
        this.snapshot = snapshot;
    }

    @Override
    public void addWidgets() {
        setupContentPanel();
        refreshContent();
    }

    @Override
    protected void populateContentPanel(Panel panel) {
        var font = Minecraft.getInstance().font;

        panel.add(new InfoRowWidget(panel, "Civilization", snapshot.civName(), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "Player", snapshot.playerName(), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "General Level", String.valueOf(snapshot.generalLevel()), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "General XP", String.valueOf(snapshot.generalXp()), 0xFFF4F7FA));
        panel.add(new InfoRowWidget(panel, "Karma", RealCivUtil.formatCredits(snapshot.karmaCents()), 0xFFF4F7FA));

        panel.add(createTextWidget("", 0));

        if (!snapshot.topProfessionLine().isEmpty()) {
            panel.add(createTextWidget(snapshot.topProfessionLine(), 0xFFE0E0E0));
        }
        if (!snapshot.focusLine().isEmpty()) {
            panel.add(createTextWidget(snapshot.focusLine(), 0xFFE0E0E0));
        }

        panel.add(createTextWidget("", 0));

        panel.add(createHeaderWidget("Professions", 0xFFFFFFFF));

        for (ProfessionLedgerSnapshot.ProfessionRow r : snapshot.professions()) {
            String line = r.name() + "  Lv " + r.level() + "  XP " + r.xp() + "  " + r.actionLine();
            panel.add(createTextWidget(font.plainSubstrByWidth(line, ROW_WIDTH), 0xFFF5F5F5));
        }

        panel.add(createTextWidget("", 0));
        panel.add(createTextWidget("Levels and limits follow server rules.", 0xFF78909C));
    }

    @Override
    protected void alignContentPanel(Panel panel) {
        for (Widget w : panel.getWidgets()) {
            if (w.width == 0) w.setSize(ROW_WIDTH, ROW_HEIGHT);
            else if (w.width < 10) w.setSize(ROW_WIDTH, w.height > 0 ? w.height : ROW_HEIGHT);
        }
        panel.align(new WidgetLayout.Vertical(2, 2, 4));
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
            setSize(ModernProfessionLedgerScreen.ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(label), x, y, 0xFF9BA9B7, false);
            String trimmed = font.plainSubstrByWidth(value, ROW_WIDTH - 120);
            graphics.drawString(font, Component.literal(trimmed), x + 120, y, valueColor, false);
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            return false;
        }
    }
}
