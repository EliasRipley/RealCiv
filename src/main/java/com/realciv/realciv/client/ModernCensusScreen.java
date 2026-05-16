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

    private static final int BTN_Y = 270;

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
        String memberLabel = "Members (" + snapshot.totalMembers() + ") page "
                + (snapshot.memberPage() + 1) + "/" + snapshot.memberPageCount();
        addSection(memberLabel, 0xFFC6D2DE);

        panel.add(new LabelWidget(panel, "Name", 4, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Role", 130, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Profession", 230, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Lv", 320, currentY, 0xFF78909C));
        currentY += ROW_H;

        boolean hasMembers = false;
        for (int i = 0; i < snapshot.members().size(); i++) {
            hasMembers = true;
            panel.add(new MemberRowWidget(panel, 4, currentY, i, snapshot.members().get(i), snapshot.canManage(), this::sendAction));
            currentY += ROW_H;
        }
        if (hasMembers) addSpacer(4);

        if (snapshot.totalRequests() > 0) {
            addSection("Join requests (" + snapshot.totalRequests() + ")", 0xFFB8E986);
            for (int i = 0; i < snapshot.requests().size(); i++) {
                panel.add(new RequestRowWidget(panel, 4, currentY, i, snapshot.requests().get(i), snapshot.canManage(), this::sendAction));
                currentY += ROW_H;
            }
            addSpacer(4);
        }

        if (snapshot.totalInvites() > 0) {
            addSection("Invites (" + snapshot.totalInvites() + ")", 0xFF90CAF9);
            for (int i = 0; i < snapshot.invites().size(); i++) {
                panel.add(new InviteRowWidget(panel, 4, currentY, i, snapshot.invites().get(i), snapshot.canManage(), this::sendAction));
                currentY += ROW_H;
            }
            addSpacer(4);
        }

        if (!snapshot.canManage()) {
            addLabelRow("", "View only -- leadership manages membership.", 0xFF9E9E9E);
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

    private static class MemberRowWidget extends Widget {
        private final int origIndex;
        private final CensusSnapshot.MemberRow member;
        private final boolean canManage;
        private final java.util.function.IntConsumer actionSender;

        MemberRowWidget(Panel parent, int x, int y, int origIndex, CensusSnapshot.MemberRow member,
                        boolean canManage, java.util.function.IntConsumer actionSender) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.origIndex = origIndex;
            this.member = member;
            this.canManage = canManage;
            this.actionSender = actionSender;
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
            if (hovered) {
                graphics.drawString(font, Component.literal("Left: kick  |  Right: toggle manager"),
                        x + 4, y + h, 0x60FFFFFF, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver() && canManage) {
                actionSender.accept(button == MouseButton.LEFT ? ACTION_MEMBER_KICK + origIndex : ACTION_MEMBER_MANAGER + origIndex);
                return true;
            }
            return false;
        }
    }

    private static class RequestRowWidget extends Widget {
        private final int origIndex;
        private final CensusSnapshot.PendingRow request;
        private final boolean canManage;
        private final java.util.function.IntConsumer actionSender;

        RequestRowWidget(Panel parent, int x, int y, int origIndex, CensusSnapshot.PendingRow request,
                         boolean canManage, java.util.function.IntConsumer actionSender) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.origIndex = origIndex;
            this.request = request;
            this.canManage = canManage;
            this.actionSender = actionSender;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && canManage;
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(request.name()), x + 4, y + 2, 0xFFE8F5E9, false);
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
                graphics.drawString(font, Component.literal("Left: approve  |  Right: deny"),
                        x + 4, y + h, 0xFFFFD54F, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver() && canManage) {
                actionSender.accept(button == MouseButton.LEFT ? ACTION_REQUEST_APPROVE + origIndex : ACTION_REQUEST_DENY + origIndex);
                return true;
            }
            return false;
        }
    }

    private static class InviteRowWidget extends Widget {
        private final int origIndex;
        private final CensusSnapshot.PendingRow invite;
        private final boolean canManage;
        private final java.util.function.IntConsumer actionSender;

        InviteRowWidget(Panel parent, int x, int y, int origIndex, CensusSnapshot.PendingRow invite,
                        boolean canManage, java.util.function.IntConsumer actionSender) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.origIndex = origIndex;
            this.invite = invite;
            this.canManage = canManage;
            this.actionSender = actionSender;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            boolean hovered = isMouseOver() && canManage;
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(invite.name()), x + 4, y + 2, 0xFFE3F2FD, false);
            if (hovered) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
                graphics.drawString(font, Component.literal("Click to revoke invite"),
                        x + 4, y + h, 0xFFFFD54F, false);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver() && canManage) {
                actionSender.accept(ACTION_INVITE_REVOKE + origIndex);
                return true;
            }
            return false;
        }
    }
}
