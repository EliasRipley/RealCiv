package com.realciv.realciv.client;

import com.realciv.realciv.network.RealCivPayloads;
import com.realciv.realciv.panel.CivRoleManagerSnapshot;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernRoleManagerScreen extends RealCivScreen {
    public static final int ACTION_PREV_PAGE = 1;
    public static final int ACTION_NEXT_PAGE = 2;
    public static final int ACTION_CREATE_ROLE = 3;
    public static final int ACTION_DELETE_SELECTED = 4;
    public static final int ACTION_BACK_TO_CONTROL_PANEL = 5;
    public static final int ACTION_SELECT_ROLE = 1000;
    public static final int ACTION_TOGGLE_PERMISSION = 2000;
    public static final int ACTION_DELETE_ROLE = 3000;

    private static final int BTN_Y = FOOTER_Y;
    private static final int ROLE_ROW_Y_OFFSET = 2;
    private static final int ROLE_ROW_BTN_H = 14;
    private static final int ROLE_COL_MEMBERS_X = 232;
    private static final int ROLE_COL_PERMS_X = 290;
    private static final int ROLE_COL_ACTION_X = 342;

    private CivRoleManagerSnapshot snapshot;
    private TextBox renameInput;
    private String renameDraft;
    private String renameDraftRoleId;

    public ModernRoleManagerScreen(CivRoleManagerSnapshot snapshot) {
        super(Component.literal("Role Manager"), "Configure custom roles and delegation", 0xFF26A69A);
        this.snapshot = snapshot;
        this.renameInput = null;
        this.renameDraft = "";
        this.renameDraftRoleId = "";
        syncRenameDraftWithSelection();
    }

    public void refresh(CivRoleManagerSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        this.renameInput = null;
        syncRenameDraftWithSelection();
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        SimpleTextButton prevBtn = makeFixedBtn(ACTION_PREV_PAGE, "< Prev", 50);
        prevBtn.setPos(10, BTN_Y);
        add(prevBtn);

        SimpleTextButton nextBtn = makeFixedBtn(ACTION_NEXT_PAGE, "Next >", 50);
        nextBtn.setPos(PANEL_W - 60, BTN_Y);
        add(nextBtn);

        SimpleTextButton backBtn = makeFixedBtn(ACTION_BACK_TO_CONTROL_PANEL, "Back", 52);
        backBtn.setPos(PANEL_W - 120, BTN_Y);
        add(backBtn);
    }

    @Override
    protected void addScrollContent(Panel panel) {
        addIdentitySection(snapshot.civDisplayName(), snapshot.playerRole(), snapshot.canManageGovernance());
        addSection("Role Overview", 0xFF80CBC4);
        addLabelRow("Roles", String.valueOf(snapshot.totalRoles()));
        addLabelRow("Page", (snapshot.rolePage() + 1) + "/" + Math.max(1, snapshot.rolePageCount()), 0xFF9DB0C2);

        addSpacer(4);
        if (snapshot.canManageGovernance()) {
            addSection("Role Actions", 0xFFC6D2DE);
            addRowWithButtons(
                    "Templates",
                    "Create a role, then click its name below to configure it.",
                    makeActionButton("Create Role", ACTION_CREATE_ROLE, 90));
            addLabelRow("", "Use census/member tools to move players between roles.", 0xFF78909C);
            addSpacer(4);
        } else {
            addLabelRow("", "View only. Leadership manages role configuration.", 0xFF9E9E9E);
            addSpacer(4);
        }

        addSection("Roles", 0xFFC6D2DE);
        panel.add(new LabelWidget(panel, "Role", 4, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Members", ROLE_COL_MEMBERS_X, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Perms", ROLE_COL_PERMS_X, currentY, 0xFF78909C));
        panel.add(new LabelWidget(panel, "Actions", ROLE_COL_ACTION_X, currentY, 0xFF78909C));
        currentY += ROW_H;

        for (int i = 0; i < snapshot.roles().size(); i++) {
            CivRoleManagerSnapshot.RoleRow row = snapshot.roles().get(i);
            int rowIndex = i;
            panel.add(new RoleRowWidget(
                    panel,
                    4,
                    currentY,
                    row,
                    snapshot.canManageGovernance(),
                    () -> sendAction(ACTION_SELECT_ROLE + rowIndex)));
            if (snapshot.canManageGovernance()) {
                if (!row.selected()) {
                    addRowButton(panel, "Select", ROLE_COL_ACTION_X, currentY + ROLE_ROW_Y_OFFSET, 56,
                            () -> sendAction(ACTION_SELECT_ROLE + rowIndex));
                } else {
                    addRowButton(panel, "Active", ROLE_COL_ACTION_X, currentY + ROLE_ROW_Y_OFFSET, 56, () -> {
                    });
                }
                addRowButton(panel, "Delete", ROLE_COL_ACTION_X + 60, currentY + ROLE_ROW_Y_OFFSET, 56,
                        () -> sendAction(ACTION_DELETE_ROLE + rowIndex));
            }
            currentY += ROW_H;
        }
        if (snapshot.roles().isEmpty()) {
            addLabelRow("", "No roles on this page.", 0xFF9E9E9E);
        }

        addSpacer(4);
        addSection("Selected Role", 0xFFC6D2DE);
        addLabelRow("Role", fallbackText(snapshot.selectedRoleDisplayName(), "-"));
        addLabelRow("Role ID", fallbackText(snapshot.selectedRoleId(), "-"), 0xFFB0BEC5);
        addLabelRow("Members", String.valueOf(snapshot.selectedRoleMemberCount()), 0xFFB0BEC5);
        addLabelRow("Permissions", String.valueOf(snapshot.selectedRolePermissionCount()), 0xFFB0BEC5);
        if (snapshot.canManageGovernance() && !snapshot.selectedRoleId().isBlank()) {
            panel.add(new LabelWidget(panel, "Rename", COL_LABEL, currentY, 0xFF9BA9B7));
            renameInput = makeRenameInput(panel);
            renameInput.setPosAndSize(COL_VALUE, currentY + 1, 148, BTN_H);
            panel.add(renameInput);
            addRowButton(panel, "Rename", 328, currentY + ROLE_ROW_Y_OFFSET, 62, this::submitRoleRename);
            currentY += ROW_H;
            addLabelRow("", "Tip: click a role name above, then rename it here.", 0xFF78909C);
        }

        addSpacer(4);
        addSection("Permission Matrix", 0xFFC6D2DE);
        for (CivRoleManagerSnapshot.PermissionRow row : snapshot.permissions()) {
            String status = row.granted() ? "Granted" : "Not granted";
            panel.add(new LabelWidget(panel, row.permissionLabel(), 4, currentY, 0xFFF4F7FA));
            panel.add(new LabelWidget(panel, row.permissionKey(), 230, currentY, 0xFF78909C));
            panel.add(new LabelWidget(panel, status, 360, currentY, row.granted() ? 0xFFA5D6A7 : 0xFFB0BEC5));
            if (snapshot.canManageGovernance() && !snapshot.selectedRoleId().isBlank()) {
                int actionId = ACTION_TOGGLE_PERMISSION + row.permissionIndex();
                addRowButton(panel, row.granted() ? "Revoke" : "Grant", 430, currentY + ROLE_ROW_Y_OFFSET, 54,
                        () -> sendAction(actionId));
            }
            currentY += ROW_H;
        }

        addSpacer(4);
        addSection("Role Members", 0xFFC6D2DE);
        if (snapshot.selectedMembers().isEmpty()) {
            addLabelRow("", "No members assigned.", 0xFF9E9E9E);
        } else {
            for (String memberName : snapshot.selectedMembers()) {
                addLabelRow("Member", memberName, 0xFFE0F2F1);
            }
        }
        addLabelRow("", "Command path for membership edits: /realciv civ role member ...", 0xFF78909C);
        addSpacer(20);
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_ROLE_MANAGER, actionId));
    }

    private SimpleTextButton makeActionButton(String label, int actionId, int width) {
        return makeInlineBtn(actionId, label, width);
    }

    private TextBox makeRenameInput(Panel panel) {
        TextBox box = new TextBox(panel) {
            @Override
            public void onTextChanged() {
                renameDraft = getText();
            }

            @Override
            public void onEnterPressed() {
                submitRoleRename();
            }
        };
        box.setMaxLength(64);
        box.ghostText = "Role name";
        box.setText(renameDraft);
        return box;
    }

    private void submitRoleRename() {
        if (!snapshot.canManageGovernance()) {
            return;
        }
        String selectedRoleId = snapshot.selectedRoleId();
        if (selectedRoleId == null || selectedRoleId.isBlank()) {
            return;
        }
        String nextName = renameInput == null ? renameDraft : renameInput.getText();
        if (nextName == null) {
            return;
        }
        nextName = nextName.trim();
        if (nextName.isEmpty() || nextName.equals(snapshot.selectedRoleDisplayName())) {
            return;
        }
        renameDraft = nextName;
        PacketDistributor.sendToServer(new RealCivPayloads.RenameRolePayload(selectedRoleId, nextName));
    }

    private void syncRenameDraftWithSelection() {
        String selectedRoleId = snapshot.selectedRoleId() == null ? "" : snapshot.selectedRoleId();
        if (!selectedRoleId.equals(renameDraftRoleId)) {
            renameDraftRoleId = selectedRoleId;
            String selectedName = snapshot.selectedRoleDisplayName();
            if (selectedName == null || selectedName.isBlank() || "-".equals(selectedName)) {
                renameDraft = "";
            } else {
                renameDraft = selectedName;
            }
        }
    }

    private void addRowButton(Panel panel, String label, int x, int y, int width, Runnable action) {
        SimpleTextButton btn = makePanelBtn(panel, label, button -> action.run());
        btn.setPosAndSize(x, y, width, ROLE_ROW_BTN_H);
        panel.add(btn);
    }

    private static class RoleRowWidget extends Widget {
        private final CivRoleManagerSnapshot.RoleRow row;
        private final boolean selectable;
        private final Runnable onSelect;

        private RoleRowWidget(
                Panel parent,
                int x,
                int y,
                CivRoleManagerSnapshot.RoleRow row,
                boolean selectable,
                Runnable onSelect) {
            super(parent);
            setPosAndSize(x, y, CONTENT_W, ROW_H);
            this.row = row;
            this.selectable = selectable;
            this.onSelect = onSelect;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (row.selected()) {
                graphics.fill(x, y, x + w, y + h, 0x304CAF50);
            } else if (isMouseOver()) {
                graphics.fill(x, y, x + w, y + h, 0x20FFFFFF);
            }
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(row.displayName(), 200)),
                    x + 4, y + 2, 0xFFF4F7FA, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.memberCount())),
                    x + ROLE_COL_MEMBERS_X, y + 2, 0xFFB0BEC5, false);
            graphics.drawString(font, Component.literal(String.valueOf(row.permissionCount())),
                    x + ROLE_COL_PERMS_X, y + 2, 0xFFB0BEC5, false);
        }

        @Override
        public boolean mousePressed(dev.ftb.mods.ftblibrary.ui.input.MouseButton button) {
            if (selectable && isMouseOver()) {
                onSelect.run();
                return true;
            }
            return false;
        }
    }
}
