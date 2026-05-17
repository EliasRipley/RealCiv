package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshot;
import com.realciv.realciv.logic.RealCivUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ModernProfessionLedgerScreen extends RealCivScreen {
    private ProfessionLedgerSnapshot snapshot;

    public ModernProfessionLedgerScreen(ProfessionLedgerSnapshot snapshot) {
        super(Component.literal("Profession Ledger"), "Your skills, levels, and progression", 0xFF8BC34A);
        this.snapshot = snapshot;
    }

    public void refresh(ProfessionLedgerSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshWidgets();
    }

    @Override
    protected void addScrollContent(Panel panel) {
        addIdentitySection(snapshot.civName(), "", false);
        addSection("Player Status", 0xFFA5D6A7);
        addLabelRow("Player", snapshot.playerName());
        addLabelRow("Karma", RealCivUtil.formatCredits(snapshot.karmaCents()));
        addLabelRow("Civ members", String.valueOf(snapshot.civMemberCount()));
        addLabelRow("Contributed item types", String.valueOf(snapshot.contributedItemTypes()));
        addLabelRow("Pending warrior registrations", String.valueOf(snapshot.pendingWarriorRegistrations()));

        addSpacer(4);

        addSection("General Progress", 0xFFA5D6A7);
        addLabelRow("Level", String.valueOf(snapshot.generalLevel()), 0xFFFFD54F);
        addLabelRow("Experience", String.valueOf(snapshot.generalXp()));
        panel.add(new XpBarWidget(panel, COL_LABEL, currentY, snapshot.generalXp()));
        currentY += 10;

        String top = snapshot.topProfessionLine();
        if (!top.isEmpty()) {
            addSpacer(2);
            addLabelRow("Top Profession", top, 0xFF8BC34A);
        }
        String focus = snapshot.focusLine();
        if (!focus.isEmpty()) {
            addLabelRow("Focus", focus, 0xFF90CAF9);
        }

        addSpacer(6);

        addSection("Professions (" + snapshot.professions().size() + ")", 0xFFA5D6A7);

        for (ProfessionLedgerSnapshot.ProfessionRow r : snapshot.professions()) {
            panel.add(new ProfessionRowWidget(panel, COL_LABEL, currentY, r));
            currentY += 28;
        }

        addSpacer(8);
        addLabelRow("", "Levels and limits follow server rules.", 0xFF78909C);
    }

    @Override
    protected void sendAction(int actionId) {
    }

    private static class XpBarWidget extends Widget {
        private final int xp;

        XpBarWidget(Panel parent, int x, int y, int xp) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, 8);
            this.xp = xp;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            int barW = CONTENT_W - 12;
            graphics.fill(x, y, x + barW, y + 6, 0xFF2A2E3A);
            int fillW = Math.min(barW, Math.max(2, (xp % 1000) * barW / 1000));
            graphics.fill(x + 1, y + 1, x + 1 + fillW, y + 5, 0xFF66BB6A);
        }
    }

    private static class ProfessionRowWidget extends Widget {
        private final ProfessionLedgerSnapshot.ProfessionRow row;

        ProfessionRowWidget(Panel parent, int x, int y, ProfessionLedgerSnapshot.ProfessionRow row) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, 24);
            this.row = row;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(row.name()),
                    x, y, 0xFFF5F5F5, false);
            graphics.drawString(font, Component.literal("Lv " + row.level()),
                    x + 180, y, 0xFFFFD54F, false);

            String xpStr = "XP: " + row.xp();
            graphics.drawString(font, Component.literal(xpStr),
                    x + 4, y + 12, 0xFF9BA9B7, false);

            String action = row.actionLine();
            if (!action.isEmpty()) {
                int actionX = x + 4 + font.width(xpStr) + 6;
                graphics.drawString(font, Component.literal(font.plainSubstrByWidth(action,
                        w - (actionX - x))), actionX, y + 12, 0xFF78909C, false);
            }

            if (isMouseOver()) {
                graphics.fill(x, y, x + w, y + h, 0x15FFFFFF);
            }
        }
    }
}
