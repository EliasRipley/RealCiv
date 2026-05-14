package com.realciv.realciv.client;

import com.daqem.uilib.api.client.gui.component.scroll.ScrollOrientation;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.io.ButtonComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollBarComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollContentComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollPanelComponent;
import com.realciv.realciv.census.CensusSnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernCensusScreen extends RealCivUIScreen {
    public static final int ACTION_MEMBER_KICK = 100;
    public static final int ACTION_MEMBER_MANAGER = 200;
    public static final int ACTION_REQUEST_APPROVE = 300;
    public static final int ACTION_REQUEST_DENY = 400;
    public static final int ACTION_INVITE_REVOKE = 500;
    public static final int ACTION_PREV_PAGE = 1;
    public static final int ACTION_NEXT_PAGE = 2;

    private static final int SCROLL_X = 14;
    private static final int SCROLL_Y = 48;
    private static final int SCROLL_W = PANEL_WIDTH - 42;
    private static final int SCROLL_H = 154;
    private static final int SCROLL_BAR_X = PANEL_WIDTH - 26;
    private static final int SCROLL_BAR_W = 6;
    private static final int BTN_Y = PANEL_HEIGHT - 22;

    // Member table columns
    private static final int COL_NAME = 4;
    private static final int COL_ROLE = 130;
    private static final int COL_PROF = 230;
    private static final int COL_LVL = 320;
    private static final int[] MEMBER_COLS = {COL_NAME, COL_ROLE, COL_PROF, COL_LVL};
    private static final String[] MEMBER_HEADERS = {"Name", "Role", "Profession", "Lv"};

    private CensusSnapshot snapshot;
    private ScrollContentComponent scrollContent;

    public ModernCensusScreen(CensusSnapshot snapshot) {
        super(Component.literal("Census Bureau"), "Membership, requests, and invites", 0xFF4CAF50);
        this.snapshot = snapshot;
    }

    public void refresh(CensusSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        if (scrollContent != null) {
            scrollContent.getChildren().clear();
            populateScrollContent();
            scrollContent.setY(0);
        }
    }

    @Override
    protected void addScreenWidgets() {
        addComponent(new ButtonComponent(panelX + 10, panelY + BTN_Y, 50, 16,
                Component.literal("< Prev"), (btn, screen, mx, my, button) -> { sendAction(ACTION_PREV_PAGE); return true; }));
        addComponent(new ButtonComponent(panelX + PANEL_WIDTH - 60, panelY + BTN_Y, 50, 16,
                Component.literal("Next >"), (btn, screen, mx, my, button) -> { sendAction(ACTION_NEXT_PAGE); return true; }));

        ScrollContentComponent content = new ScrollContentComponent(0, 0, 2, ScrollOrientation.VERTICAL);
        scrollContent = content;
        populateScrollContent();

        addComponent(new ScrollPanelComponent(null, panelX + SCROLL_X, panelY + SCROLL_Y, SCROLL_W, SCROLL_H,
                ScrollOrientation.VERTICAL, content,
                createScrollBar(SCROLL_BAR_X, 0, SCROLL_BAR_W, SCROLL_H, ScrollOrientation.VERTICAL)));
    }

    private void populateScrollContent() {
        // -- Members section --
        String memberLabel = "Members (" + snapshot.totalMembers() + ") page "
                + (snapshot.memberPage() + 1) + "/" + snapshot.memberPageCount();
        scrollContent.addChild(new SectionLabel(0, 0, memberLabel, 0xFFC6D2DE));
        scrollContent.addChild(new TableHeaderRow(0, 0, MEMBER_HEADERS, MEMBER_COLS));

        boolean hasMembers = false;
        for (int i = 0; i < snapshot.members().size(); i++) {
            hasMembers = true;
            scrollContent.addChild(new MemberRowComponent(0, 0, i, snapshot.members().get(i)));
        }

        if (hasMembers) {
            scrollContent.addChild(new SpacerRow(0, 0, 4));
        }

        // -- Requests section --
        if (snapshot.totalRequests() > 0) {
            scrollContent.addChild(new SectionLabel(0, 0,
                    "Join requests (" + snapshot.totalRequests() + ")", 0xFFB8E986));
            for (int i = 0; i < snapshot.requests().size(); i++) {
                scrollContent.addChild(new RequestRowComponent(0, 0, i, snapshot.requests().get(i)));
            }
            scrollContent.addChild(new SpacerRow(0, 0, 4));
        }

        // -- Invites section --
        if (snapshot.totalInvites() > 0) {
            scrollContent.addChild(new SectionLabel(0, 0,
                    "Invites (" + snapshot.totalInvites() + ")", 0xFF90CAF9));
            for (int i = 0; i < snapshot.invites().size(); i++) {
                scrollContent.addChild(new InviteRowComponent(0, 0, i, snapshot.invites().get(i)));
            }
            scrollContent.addChild(new SpacerRow(0, 0, 4));
        }

        if (!snapshot.canManage()) {
            scrollContent.addChild(new RowLabel(0, 0,
                    "View only \u2014 leadership manages membership.", 0xFF9E9E9E));
        }

        if (!hasMembers) {
            scrollContent.addChild(new RowLabel(0, 0, "No members found.", 0xFF9E9E9E));
        }

        // Spacer to fill remaining scroll area
        scrollContent.addChild(new SpacerRow(0, 0, 20));
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_CENSUS, actionId));
    }

    // -- Member row --

    private class MemberRowComponent extends AbstractComponent<MemberRowComponent> {
        private final int origIndex;
        private final CensusSnapshot.MemberRow member;

        MemberRowComponent(int x, int y, int origIndex, CensusSnapshot.MemberRow member) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.origIndex = origIndex;
            this.member = member;
            setOnClickEvent((comp, screen, mx, my, button) -> {
                if (!snapshot.canManage()) return false;
                sendAction(button == 0 ? ACTION_MEMBER_KICK + origIndex : ACTION_MEMBER_MANAGER + origIndex);
                return true;
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isTotalHovered(mouseX, mouseY) && snapshot.canManage();
            if (hovered) {
                graphics.fill(-2, -1, CONTENT_WIDTH + 2, 13, 0x20FFFFFF);
            }

            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(member.name(), 96)),
                    COL_NAME, 0, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(member.role(), 72)),
                    COL_ROLE, 0, 0xFFB0BEC5, false);
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(member.profession(), 76)),
                    COL_PROF, 0, 0xFF8BC34A, false);
            graphics.drawString(font, Component.literal(String.valueOf(member.level())),
                    COL_LVL, 0, 0xFF8BC34A, false);

            if (hovered) {
                graphics.drawString(font, Component.literal("Left: kick  |  Right: toggle manager"),
                        COL_NAME, -10, 0x60FFFFFF, false);
            }
        }
    }

    // -- Request row --

    private class RequestRowComponent extends AbstractComponent<RequestRowComponent> {
        private final int origIndex;
        private final CensusSnapshot.PendingRow request;

        RequestRowComponent(int x, int y, int origIndex, CensusSnapshot.PendingRow request) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.origIndex = origIndex;
            this.request = request;
            setOnClickEvent((comp, screen, mx, my, button) -> {
                if (!snapshot.canManage()) return false;
                sendAction(button == 0 ? ACTION_REQUEST_APPROVE + origIndex : ACTION_REQUEST_DENY + origIndex);
                return true;
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isTotalHovered(mouseX, mouseY) && snapshot.canManage();
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(request.name()), COL_NAME, 0, 0xFFE8F5E9, false);

            if (hovered) {
                graphics.fill(-2, -1, CONTENT_WIDTH + 2, 13, 0x20FFFFFF);
                graphics.drawString(font, Component.literal("Left: approve  |  Right: deny"),
                        COL_NAME, -10, 0xFFFFD54F, false);
            }
        }
    }

    // -- Invite row --

    private class InviteRowComponent extends AbstractComponent<InviteRowComponent> {
        private final int origIndex;
        private final CensusSnapshot.PendingRow invite;

        InviteRowComponent(int x, int y, int origIndex, CensusSnapshot.PendingRow invite) {
            super(null, x, y, CONTENT_WIDTH, ROW_HEIGHT);
            this.origIndex = origIndex;
            this.invite = invite;
            setOnClickEvent((comp, screen, mx, my, button) -> {
                if (!snapshot.canManage()) return false;
                sendAction(ACTION_INVITE_REVOKE + origIndex);
                return true;
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isTotalHovered(mouseX, mouseY) && snapshot.canManage();
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(invite.name()), COL_NAME, 0, 0xFFE3F2FD, false);

            if (hovered) {
                graphics.fill(-2, -1, CONTENT_WIDTH + 2, 13, 0x20FFFFFF);
                graphics.drawString(font, Component.literal("Click to revoke invite"),
                        COL_NAME, -10, 0xFFFFD54F, false);
            }
        }
    }
}
