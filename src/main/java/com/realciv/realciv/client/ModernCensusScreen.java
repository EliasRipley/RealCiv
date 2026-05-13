package com.realciv.realciv.client;

import com.realciv.realciv.census.CensusSnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ModernCensusScreen extends RealCivPanelScreen {
    public static final int ACTION_MEMBER_KICK = 100;
    public static final int ACTION_MEMBER_MANAGER = 200;
    public static final int ACTION_REQUEST_APPROVE = 300;
    public static final int ACTION_REQUEST_DENY = 400;
    public static final int ACTION_INVITE_REVOKE = 500;
    public static final int ACTION_PREV_PAGE = 1;
    public static final int ACTION_NEXT_PAGE = 2;

    private static final int ROW_WIDTH = PANEL_WIDTH - 32;

    private CensusSnapshot snapshot;

    public ModernCensusScreen(CensusSnapshot snapshot) {
        super(Component.literal("Census Bureau"), "Membership, requests, and invites", 0xFF4CAF50);
        this.snapshot = snapshot;
    }

    public void refresh(CensusSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshContent();
    }

    @Override
    public void addWidgets() {
        setupSearchBox("Search members...");
        setupContentPanel();
        refreshContent();

        add(new NavButton("< Prev", 10, PANEL_HEIGHT - 22, 50, ACTION_PREV_PAGE));
        add(new NavButton("Next >", PANEL_WIDTH - 60, PANEL_HEIGHT - 22, 50, ACTION_NEXT_PAGE));
    }

    @Override
    protected void populateContentPanel(Panel panel) {
        String query = (searchBox != null) ? searchBox.getText().toLowerCase() : "";
        Predicate<String> filter = query.isEmpty()
            ? s -> true
            : s -> s.toLowerCase().contains(query);

        String pageInfo = "Members (" + snapshot.totalMembers() + ") page " + (snapshot.memberPage() + 1) + "/" + snapshot.memberPageCount();
        panel.add(createTextWidget(pageInfo, 0xFFC6D2DE));

        boolean hasResults = false;

        for (int i = 0; i < snapshot.members().size(); i++) {
            CensusSnapshot.MemberRow m = snapshot.members().get(i);
            if (!filter.test(m.name())) continue;
            hasResults = true;
            panel.add(new MemberRowWidget(panel, i, m));
        }

        if (hasResults) {
            panel.add(createTextWidget("", 0)); // spacer
        }

        if (snapshot.totalRequests() > 0) {
            List<Integer> filteredRequests = new ArrayList<>();
            for (int i = 0; i < snapshot.requests().size(); i++) {
                if (filter.test(snapshot.requests().get(i).name())) filteredRequests.add(i);
            }
            if (!filteredRequests.isEmpty()) {
                panel.add(createHeaderWidget("Join requests (" + snapshot.totalRequests() + ")", 0xFFB8E986));
                for (int idx : filteredRequests) {
                    panel.add(new RequestRowWidget(panel, idx, snapshot.requests().get(idx)));
                }
            }
        }

        if (snapshot.totalInvites() > 0) {
            List<Integer> filteredInvites = new ArrayList<>();
            for (int i = 0; i < snapshot.invites().size(); i++) {
                if (filter.test(snapshot.invites().get(i).name())) filteredInvites.add(i);
            }
            if (!filteredInvites.isEmpty()) {
                panel.add(createHeaderWidget("Invites (" + snapshot.totalInvites() + ")", 0xFF90CAF9));
                for (int idx : filteredInvites) {
                    panel.add(new InviteRowWidget(panel, idx, snapshot.invites().get(idx)));
                }
            }
        }

        if (!snapshot.canManage()) {
            panel.add(createTextWidget("View only - leadership manages membership.", 0xFF9E9E9E));
        }

        if (!hasResults && !query.isEmpty()) {
            panel.add(createTextWidget("No matching members found.", 0xFF9E9E9E));
        }
    }

    @Override
    protected void alignContentPanel(Panel panel) {
        for (Widget w : panel.getWidgets()) {
            if (w.width == 0) {
                w.setSize(ROW_WIDTH, ROW_HEIGHT);
            } else if (w.width < 10) {
                w.setSize(ROW_WIDTH, w.height > 0 ? w.height : ROW_HEIGHT);
            }
        }
        panel.align(new WidgetLayout.Vertical(2, 2, 4));
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_CENSUS, actionId));
    }

    private class NavButton extends dev.ftb.mods.ftblibrary.ui.NordButton {
        private final int actionId;

        NavButton(String label, int x, int y, int w, int actionId) {
            super(ModernCensusScreen.this, Component.literal(label), Icon.empty());
            this.actionId = actionId;
            setPosAndSize(x, y, w, 16);
        }

        @Override
        public void onClicked(MouseButton button) {
            sendAction(actionId);
        }
    }

    private class MemberRowWidget extends Widget {
        private final int origIndex;
        private final CensusSnapshot.MemberRow member;

        MemberRowWidget(Panel panel, int origIndex, CensusSnapshot.MemberRow member) {
            super(panel);
            this.origIndex = origIndex;
            this.member = member;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver() && snapshot.canManage()) {
                graphics.fill(x - 2, y - 1, x + w + 2, y + h + 1, 0x30FFFFFF);
            }
            var font = Minecraft.getInstance().font;
            String line = member.name() + "  |  " + member.role();
            String profLine = member.profession() + " Lv " + member.level();
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(line, ROW_WIDTH - 120)), x, y, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(profLine, 100)), x + ROW_WIDTH - 100, y, 0xFF8BC34A, false);
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (!snapshot.canManage()) return false;
            if (button.isLeft()) {
                sendAction(ACTION_MEMBER_KICK + origIndex);
            } else {
                sendAction(ACTION_MEMBER_MANAGER + origIndex);
            }
            return true;
        }
    }

    private class RequestRowWidget extends Widget {
        private final int origIndex;
        private final CensusSnapshot.PendingRow request;

        RequestRowWidget(Panel panel, int origIndex, CensusSnapshot.PendingRow request) {
            super(panel);
            this.origIndex = origIndex;
            this.request = request;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && snapshot.canManage();
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(request.name()), x, y, 0xFFE8F5E9, false);

            if (hovered) {
                graphics.fill(x - 2, y - 1, x + w + 2, y + h + 1, 0x30FFFFFF);
                graphics.drawString(font, Component.literal("Left-click: Approve | Right-click: Deny"),
                        x + ROW_WIDTH - 180, y, 0xFFFFD54F, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (!snapshot.canManage()) return false;
            if (button.isLeft()) {
                sendAction(ACTION_REQUEST_APPROVE + origIndex);
            } else {
                sendAction(ACTION_REQUEST_DENY + origIndex);
            }
            return true;
        }
    }

    private class InviteRowWidget extends Widget {
        private final int origIndex;
        private final CensusSnapshot.PendingRow invite;

        InviteRowWidget(Panel panel, int origIndex, CensusSnapshot.PendingRow invite) {
            super(panel);
            this.origIndex = origIndex;
            this.invite = invite;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && snapshot.canManage();
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(invite.name()), x, y, 0xFFE3F2FD, false);

            if (hovered) {
                graphics.fill(x - 2, y - 1, x + w + 2, y + h + 1, 0x30FFFFFF);
                graphics.drawString(font, Component.literal("Click to revoke invite"),
                        x + ROW_WIDTH - 140, y, 0xFFFFD54F, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (!snapshot.canManage()) return false;
            sendAction(ACTION_INVITE_REVOKE + origIndex);
            return true;
        }
    }
}
