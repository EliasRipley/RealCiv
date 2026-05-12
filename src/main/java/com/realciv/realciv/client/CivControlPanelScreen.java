package com.realciv.realciv.client;

import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.panel.CivControlPanelActionIds;
import com.realciv.realciv.panel.CivControlPanelMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CivControlPanelScreen extends AbstractContainerScreen<CivControlPanelMenu> {
    private static final int BG_WIDTH = 352;
    private static final int BG_HEIGHT = 236;
    private static final int TAB_ROW_Y = 22;
    private static final int CONTENT_X = 14;
    private static final int CONTENT_Y = 46;
    private static final int ROW_STEP = 13;
    private static final int LABEL_COLOR = 0xFFCBD3D9;
    private static final int VALUE_COLOR = 0xFFF4F7FA;
    private static final int SUBTLE_COLOR = 0xFF9BA9B7;

    private PanelTab selectedTab = PanelTab.OVERVIEW;

    public CivControlPanelScreen(CivControlPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int tabX = leftPos + 12;
        int tabY = topPos + TAB_ROW_Y;
        for (PanelTab tab : PanelTab.values()) {
            int currentX = tabX;
            this.addRenderableWidget(Button.builder(Component.literal(tab.label), ignored -> selectedTab = tab)
                    .bounds(currentX, tabY, 78, 18)
                    .build());
            tabX += 82;
        }

        int x = leftPos + 12;
        int y = topPos + 182;
        addActionButton(x, y, 78, "Gov Cycle", CivControlPanelActionIds.GOVERNANCE_CYCLE);
        addActionButton(x + 82, y, 90, "Dist Toggle", CivControlPanelActionIds.DISTRIBUTION_TOGGLE);
        addActionButton(x + 176, y, 84, "Friendly PvP", CivControlPanelActionIds.FRIENDLY_FIRE_TOGGLE);
        addActionButton(x + 264, y, 36, "Yes", CivControlPanelActionIds.PROPOSAL_VOTE_YES);
        addActionButton(x + 304, y, 36, "No", CivControlPanelActionIds.PROPOSAL_VOTE_NO);

        y += 20;
        addActionButton(x, y, 54, "Bread +", CivControlPanelActionIds.ALLOWANCE_BREAD_PLUS);
        addActionButton(x + 58, y, 54, "Bread -", CivControlPanelActionIds.ALLOWANCE_BREAD_MINUS);
        addActionButton(x + 116, y, 54, "Gold +", CivControlPanelActionIds.ALLOWANCE_GOLD_PLUS);
        addActionButton(x + 174, y, 54, "Gold -", CivControlPanelActionIds.ALLOWANCE_GOLD_MINUS);
        addActionButton(x + 232, y, 108, "Clear Allow", CivControlPanelActionIds.ALLOWANCE_CLEAR_ALL);

        y += 20;
        addActionButton(x, y, 74, "Hook Breed", CivControlPanelActionIds.HOOK_ADD_ANIMAL_BREED);
        addActionButton(x + 78, y, 74, "Hook Trade", CivControlPanelActionIds.HOOK_ADD_VILLAGER_TRADE);
        addActionButton(x + 156, y, 74, "Hook Shear", CivControlPanelActionIds.HOOK_ADD_SHEAR_ENTITY);
        addActionButton(x + 234, y, 106, "Hook Remove", CivControlPanelActionIds.HOOK_REMOVE_LAST);

        y += 20;
        addActionButton(x, y, 94, "Cap Zombie", CivControlPanelActionIds.HUNTER_CAP_ADD_ZOMBIE);
        addActionButton(x + 98, y, 108, "Cap Dragon", CivControlPanelActionIds.HUNTER_CAP_ADD_ENDER_DRAGON);
        addActionButton(x + 210, y, 130, "Cap Remove", CivControlPanelActionIds.HUNTER_CAP_REMOVE_LAST);
    }

    private void addActionButton(int x, int y, int width, String label, int actionId) {
        this.addRenderableWidget(Button.builder(Component.literal(label), ignored -> sendPanelAction(actionId))
                .bounds(x, y, width, 18)
                .build());
    }

    private void sendPanelAction(int actionId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, actionId);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        int x1 = leftPos + imageWidth;
        int y1 = topPos + imageHeight;

        guiGraphics.fill(x0, y0, x1, y1, 0xE01D2228);
        guiGraphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 0xF02D3640);
        guiGraphics.fill(x0 + 8, y0 + 40, x1 - 8, y1 - 60, 0xF0222A33);
        guiGraphics.fill(x0 + 8, y0 + 40, x1 - 8, y0 + 41, 0xFF3D4C5A);
        guiGraphics.fill(x0 + 8, y1 - 56, x1 - 8, y1 - 8, 0xF01B2128);
        guiGraphics.fill(x0 + 8, y1 - 56, x1 - 8, y1 - 55, 0xFF3D4C5A);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        CivControlPanelMenu.Snapshot snapshot = menu.snapshot();
        guiGraphics.drawString(
                font,
                Component.literal(snapshot.civilizationName() + " Control Panel").withStyle(ChatFormatting.BOLD),
                CONTENT_X,
                8,
                VALUE_COLOR,
                false);
        guiGraphics.drawString(
                font,
                Component.literal("Role: " + snapshot.playerRole() + " | Civ ID: " + snapshot.civilizationId()),
                CONTENT_X,
                20,
                SUBTLE_COLOR,
                false);

        int row = CONTENT_Y;
        row = drawActiveTabIndicator(guiGraphics, row);
        row += 2;

        switch (selectedTab) {
            case OVERVIEW -> renderOverview(guiGraphics, snapshot, row);
            case ECONOMY -> renderEconomy(guiGraphics, snapshot, row);
            case LAND -> renderLand(guiGraphics, snapshot, row);
            case GOVERNANCE -> renderGovernance(guiGraphics, snapshot, row);
        }

        guiGraphics.drawString(
                font,
                Component.literal("Action Toolbar: buttons issue server-side policy actions with permissions."),
                CONTENT_X,
                168,
                SUBTLE_COLOR,
                false);
    }

    private int drawActiveTabIndicator(GuiGraphics guiGraphics, int row) {
        guiGraphics.drawString(
                font,
                Component.literal("Viewing: " + selectedTab.label).withStyle(ChatFormatting.GRAY),
                CONTENT_X,
                row,
                SUBTLE_COLOR,
                false);
        return row + ROW_STEP;
    }

    private void renderOverview(GuiGraphics guiGraphics, CivControlPanelMenu.Snapshot snapshot, int row) {
        row = drawRow(guiGraphics, row, "Members", Integer.toString(snapshot.memberCount()));
        row = drawRow(guiGraphics, row, "Managers", Integer.toString(snapshot.managerCount()));
        row = drawRow(guiGraphics, row, "Custom roles", Integer.toString(snapshot.customRoleCount()));
        row = drawRow(guiGraphics, row, "Join requests", Integer.toString(snapshot.joinRequestCount()));
        row = drawRow(guiGraphics, row, "Open invites", Integer.toString(snapshot.inviteCount()));
        row = drawRow(guiGraphics, row, "Friendly fire", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled");
        row = drawRow(
                guiGraphics,
                row,
                "Pending proposal",
                snapshot.pendingProposalSummary());
        drawRow(
                guiGraphics,
                row,
                "Proposal votes",
                snapshot.pendingProposalRequiredYesVotes() <= 0
                        ? "-"
                        : (snapshot.pendingProposalYesVotes() + "/" + snapshot.pendingProposalRequiredYesVotes() + " yes"));
    }

    private void renderEconomy(GuiGraphics guiGraphics, CivControlPanelMenu.Snapshot snapshot, int row) {
        row = drawRow(
                guiGraphics,
                row,
                "Civ treasury",
                RealCivUtil.formatCredits(snapshot.civTreasuryCents()));
        row = drawRow(
                guiGraphics,
                row,
                "Your contribution karma",
                RealCivUtil.formatCredits(snapshot.playerContributionKarmaCents()));
        row = drawRow(
                guiGraphics,
                row,
                "Your total item contributions",
                Long.toString(snapshot.playerContributedItemsTotal()));
        row = drawRow(guiGraphics, row, "Hub stock item types", Integer.toString(snapshot.hubStockEntryCount()));
        row = drawRow(guiGraphics, row, "Hub distribution mode", snapshot.hubDistributionMode());
        row = drawRow(guiGraphics, row, "Daily allowance entries", Integer.toString(snapshot.hubDailyAllowanceCount()));
        drawRow(
                guiGraphics,
                row,
                "Max contribution karma/day",
                snapshot.maxContributionKarmaGainPerDayCents() <= 0L
                        ? "Unlimited"
                        : RealCivUtil.formatCredits(snapshot.maxContributionKarmaGainPerDayCents()));
    }

    private void renderLand(GuiGraphics guiGraphics, CivControlPanelMenu.Snapshot snapshot, int row) {
        row = drawRow(guiGraphics, row, "Dimension claim policy", snapshot.claimDimensionPolicy());
        row = drawRow(guiGraphics, row, "Total plots", Integer.toString(snapshot.plotTotalCount()));
        row = drawRow(guiGraphics, row, "CIVIC plots", Integer.toString(snapshot.plotCivicCount()));
        row = drawRow(guiGraphics, row, "PRIVATE plots", Integer.toString(snapshot.plotPrivateCount()));
        drawRow(guiGraphics, row, "COMMUNITY plots", Integer.toString(snapshot.plotCommunityCount()));
    }

    private void renderGovernance(GuiGraphics guiGraphics, CivControlPanelMenu.Snapshot snapshot, int row) {
        row = drawRow(guiGraphics, row, "Leader title", snapshot.leaderTitle());
        row = drawRow(guiGraphics, row, "Governance model", snapshot.governanceModel());
        row = drawRow(guiGraphics, row, "Workflow mode", snapshot.governanceApprovalWorkflowEnabled() ? "Enabled" : "Disabled");
        row = drawRow(guiGraphics, row, "May edit governance", snapshot.canManageGovernance() ? "Yes" : "No");
        row = drawRow(guiGraphics, row, "May manage census", snapshot.canManageCensus() ? "Yes" : "No");
        row = drawRow(guiGraphics, row, "May manage hub", snapshot.canManageHubDistribution() ? "Yes" : "No");
        row = drawRow(guiGraphics, row, "Hook rules", Integer.toString(snapshot.professionHookRuleCount()));
        drawRow(guiGraphics, row, "Hunter selector rules", Integer.toString(snapshot.hunterMobActionCapRuleCount()));
    }

    private int drawRow(GuiGraphics guiGraphics, int rowY, String label, String value) {
        guiGraphics.drawString(font, Component.literal(label + ":"), CONTENT_X, rowY, LABEL_COLOR, false);
        String trimmed = font.plainSubstrByWidth(value, 220);
        guiGraphics.drawString(font, Component.literal(trimmed), CONTENT_X + 116, rowY, VALUE_COLOR, false);
        return rowY + ROW_STEP;
    }

    private enum PanelTab {
        OVERVIEW("Overview"),
        ECONOMY("Economy"),
        LAND("Land"),
        GOVERNANCE("Governance");

        private final String label;

        PanelTab(String label) {
            this.label = label;
        }
    }
}
