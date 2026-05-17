package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import com.realciv.realciv.census.CensusSnapshot;
import com.realciv.realciv.network.RealCivPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernCensusScreen extends RealCivScreen {
    public static final int ACTION_MEMBER_KICK = 100;
    public static final int ACTION_MEMBER_MANAGER = 200;
    public static final int ACTION_REQUEST_APPROVE = 300;
    public static final int ACTION_REQUEST_DENY = 400;
    public static final int ACTION_INVITE_REVOKE = 500;
    public static final int ACTION_PREV_PAGE = 1;
    public static final int ACTION_NEXT_PAGE = 2;

    private static final int BTN_Y = FOOTER_Y;
    private static final int MEMBER_ACTION_KICK_X = 350;
    private static final int MEMBER_ACTION_MANAGER_X = 404;
    private static final int MEMBER_ACTION_Y_OFFSET = 2;
    private static final int MEMBER_ACTION_H = 14;

    private CensusSnapshot snapshot;

    public ModernCensusScreen(CensusSnapshot snapshot) {
        super(Component.literal("Census Bureau"), "Membership, requests, and invites", 0xFF4CAF50);
        this.snapshot = snapshot;
    }

    public void refresh(CensusSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        {
            SimpleTextButton btn = makeFixedBtn(ACTION_PREV_PAGE, "< Prev", 50);
            btn.setPos(10, BTN_Y);
            add(btn);
        }
        {
            SimpleTextButton btn = makeFixedBtn(ACTION_NEXT_PAGE, "Next >", 50);
            btn.setPos(PANEL_W - 60, BTN_Y);
            add(btn);
        }
    }

    @Override
    protected void addScrollContent(Panel panel) {
        addIdentitySection(snapshot.civDisplayName(), "", snapshot.canManage());
        addSection("Membership Overview", 0xFFA5D6A7);
        addLabelRow("Members", String.valueOf(snapshot.totalMembers()));
        addLabelRow("Join requests", String.valueOf(snapshot.totalRequests()));
        addLabelRow("Open invites", String.valueOf(snapshot.totalInvites()));
        addLabelRow("Page", (snapshot.memberPage() + 1) + "/" + Math.max(1, snapshot.memberPageCount()), 0xFF9DB0C2);

        addSpacer(4);
        addSection("Member Directory", 0xFFC6D2DE);

        panel.add(new LabelWidget(panel, "Name", 4, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Role", 130, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Profession", 230, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Lv", 320, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Actions", 350, currentY, 0xFF78909C));
        currentY += ROW_H;

        boolean hasMembers = false;
        for (int i = 0; i < snapshot.members().size(); i++) {
            hasMembers = true;
            CensusSnapshot.MemberRow member = snapshot.members().get(i);
            panel.add(new MemberRowWidget(panel, 4, currentY, member, snapshot.canManage()));
            if (snapshot.canManage()) {
                int rowIndex = i;
                addRowButton(panel, "Kick", MEMBER_ACTION_KICK_X, currentY + MEMBER_ACTION_Y_OFFSET, 50,
                        () -> confirmKick(member, rowIndex));
                String managerLabel = isManagerRole(member.role()) ? "Demote" : "Promote";
                addRowButton(panel, managerLabel, MEMBER_ACTION_MANAGER_X, currentY + MEMBER_ACTION_Y_OFFSET, 74,
                        () -> sendAction(ACTION_MEMBER_MANAGER + rowIndex));
            }
            currentY += ROW_H;
        }
        if (hasMembers) addSpacer(4);

        if (snapshot.totalRequests() > 0) {
            addSection("Join Requests (" + snapshot.totalRequests() + ")", 0xFFB8E986);
            for (int i = 0; i < snapshot.requests().size(); i++) {
                panel.add(new RequestRowWidget(panel, 4, currentY, snapshot.requests().get(i), snapshot.canManage()));
                if (snapshot.canManage()) {
                    int rowIndex = i;
                    addRowButton(panel, "Approve", MEMBER_ACTION_KICK_X, currentY + MEMBER_ACTION_Y_OFFSET, 50,
                            () -> sendAction(ACTION_REQUEST_APPROVE + rowIndex));
                    addRowButton(panel, "Deny", MEMBER_ACTION_MANAGER_X, currentY + MEMBER_ACTION_Y_OFFSET, 74,
                            () -> sendAction(ACTION_REQUEST_DENY + rowIndex));
                }
                currentY += ROW_H;
            }
            addSpacer(4);
        }

        if (snapshot.totalInvites() > 0) {
            addSection("Outstanding Invites (" + snapshot.totalInvites() + ")", 0xFF90CAF9);
            for (int i = 0; i < snapshot.invites().size(); i++) {
                panel.add(new InviteRowWidget(panel, 4, currentY, snapshot.invites().get(i), snapshot.canManage()));
                if (snapshot.canManage()) {
                    int rowIndex = i;
                    addRowButton(panel, "Revoke", MEMBER_ACTION_KICK_X, currentY + MEMBER_ACTION_Y_OFFSET, 128,
                            () -> sendAction(ACTION_INVITE_REVOKE + rowIndex));
                }
                currentY += ROW_H;
            }
            addSpacer(4);
        }

        if (!snapshot.canManage()) {
            addLabelRow("", "View only. Leadership manages approvals and removals.", 0xFF9E9E9E);
        }
        if (!hasMembers) {
            addLabelRow("", "No members found.", 0xFF9E9E9E);
        }
        addSpacer(20);
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_CENSUS, actionId));
    }

    private void confirmKick(CensusSnapshot.MemberRow member, int rowIndex) {
        openYesNo(
                Component.literal("Kick " + member.name() + "?"),
                Component.literal("This will remove the player from your civilization."),
                () -> sendAction(ACTION_MEMBER_KICK + rowIndex));
    }

    private void addRowButton(Panel panel, String label, int x, int y, int width, Runnable action) {
        SimpleTextButton btn = makePanelBtn(panel, label, button -> action.run());
        btn.setPosAndSize(x, y, width, MEMBER_ACTION_H);
        panel.add(btn);
    }

    private boolean isManagerRole(String roleName) {
        String normalized = roleName == null ? "" : roleName.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("manager");
    }

    private static class MemberRowWidget extends Widget {
        private final CensusSnapshot.MemberRow member;
        private final boolean canManage;

        MemberRowWidget(Panel parent, int x, int y, CensusSnapshot.MemberRow member,
                        boolean canManage) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.member = member;
            this.canManage = canManage;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && canManage;
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(member.name(), 96)),
                    x + 4, y + 2, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(member.role(), 72)),
                    x + 130, y + 2, 0xFFB0BEC5, false);
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(member.profession(), 76)),
                    x + 230, y + 2, 0xFF8BC34A, false);
            graphics.drawString(font, Component.literal(String.valueOf(member.level())),
                    x + 320, y + 2, 0xFF8BC34A, false);
        }
    }

    private static class RequestRowWidget extends Widget {
        private final CensusSnapshot.PendingRow request;
        private final boolean canManage;

        RequestRowWidget(Panel parent, int x, int y, CensusSnapshot.PendingRow request,
                         boolean canManage) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.request = request;
            this.canManage = canManage;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && canManage;
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(request.name()), x + 4, y + 2, 0xFFE8F5E9, false);
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }
        }
    }

    private static class InviteRowWidget extends Widget {
        private final CensusSnapshot.PendingRow invite;
        private final boolean canManage;

        InviteRowWidget(Panel parent, int x, int y, CensusSnapshot.PendingRow invite,
                        boolean canManage) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.invite = invite;
            this.canManage = canManage;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && canManage;
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(invite.name()), x + 4, y + 2, 0xFFE3F2FD, false);
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }
        }
    }
}
