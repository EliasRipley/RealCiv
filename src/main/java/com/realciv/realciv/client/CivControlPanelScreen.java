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
        int y = topPos + 170;
        addActionButton(x, y, 64, "Gov Model", CivControlPanelActionIds.GOVERNANCE_CYCLE);
        addActionButton(x + 68, y, 64, "Hub Mode", CivControlPanelActionIds.DISTRIBUTION_TOGGLE);
        addActionButton(x + 136, y, 64, "Friendly PvP", CivControlPanelActionIds.FRIENDLY_FIRE_TOGGLE);
        addActionButton(x + 204, y, 56, "Vote Yes", CivControlPanelActionIds.PROPOSAL_VOTE_YES);
        addActionButton(x + 264, y, 56, "Vote No", CivControlPanelActionIds.PROPOSAL_VOTE_NO);

        int leadershipY = y + 20;
        addActionButton(x, leadershipY, 80, "Start Election", CivControlPanelActionIds.LEADERSHIP_START_ELECTION);
        addActionButton(x + 83, leadershipY, 80, "Join/Mem", CivControlPanelActionIds.LEADERSHIP_JOIN_ELECTION);
        addActionButton(x + 166, leadershipY, 80, "Start Coup", CivControlPanelActionIds.LEADERSHIP_START_COUP_SELF);
        addActionButton(x + 249, leadershipY, 80, "Approve/Perm", CivControlPanelActionIds.LEADERSHIP_APPROVE_COUP);

        int voteY = leadershipY + 20;
        addActionButton(x, voteY, 64, "Role+/V1", CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_1);
        addActionButton(x + 66, voteY, 64, "Role>/V2", CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_2);
        addActionButton(x + 132, voteY, 64, "Mem>/V3", CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_3);
        addActionButton(x + 198, voteY, 64, "Perm>/V4", CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_4);
        addActionButton(x + 264, voteY, 64, "MemT/V5", CivControlPanelActionIds.LEADERSHIP_VOTE_CANDIDATE_5);
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
        row = drawRow(guiGraphics, row, "Leadership contest", snapshot.leadershipContestType());
        row = drawRow(guiGraphics, row, "Contest status", snapshot.leadershipContestSummary());
        row = drawRow(guiGraphics, row, "Role control hint", "No election: vote row manages roles.");
        row = drawRow(guiGraphics, row, "Coup leader", snapshot.leadershipCoupLeaderName());
        row = drawRow(
                guiGraphics,
                row,
                "Coup approvals",
                snapshot.leadershipCoupRequiredApprovals() <= 0
                        ? "-"
                        : (snapshot.leadershipCoupApprovalCount() + "/" + snapshot.leadershipCoupRequiredApprovals()));
        row = drawRow(
                guiGraphics,
                row,
                "Election votes",
                snapshot.leadershipElectionVoteCount() + " | candidates " + snapshot.leadershipCandidateCount());
        row = drawRow(guiGraphics, row, "Candidate #1", candidateLabel(snapshot, 0));
        row = drawRow(guiGraphics, row, "Candidate #2", candidateLabel(snapshot, 1));
        row = drawRow(guiGraphics, row, "Candidate #3", candidateLabel(snapshot, 2));
        row = drawRow(guiGraphics, row, "Candidate #4", candidateLabel(snapshot, 3));
        drawRow(guiGraphics, row, "Candidate #5", candidateLabel(snapshot, 4));
    }

    private String candidateLabel(CivControlPanelMenu.Snapshot snapshot, int index) {
        if (index < 0 || index >= snapshot.leadershipCandidateEntries().size()) {
            return "-";
        }
        String entry = snapshot.leadershipCandidateEntries().get(index);
        int split = entry.indexOf('|');
        if (split < 0 || split + 1 >= entry.length()) {
            return entry;
        }
        return entry.substring(split + 1);
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
