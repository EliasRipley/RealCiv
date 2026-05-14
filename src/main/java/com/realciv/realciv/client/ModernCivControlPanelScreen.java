package com.realciv.realciv.client;

import com.daqem.uilib.api.client.gui.component.scroll.ScrollOrientation;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import com.daqem.uilib.client.gui.component.io.ButtonComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollBarComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollContentComponent;
import com.daqem.uilib.client.gui.component.scroll.ScrollPanelComponent;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.network.RealCivPayloads;
import com.realciv.realciv.panel.CivControlPanelSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

public class ModernCivControlPanelScreen extends RealCivUIScreen {
    public static final int ACTION_GOVERNANCE_CYCLE = 1;
    public static final int ACTION_DISTRIBUTION_TOGGLE = 2;
    public static final int ACTION_FRIENDLY_FIRE_TOGGLE = 3;
    public static final int ACTION_PROPOSAL_YES = 4;
    public static final int ACTION_PROPOSAL_NO = 5;
    public static final int ACTION_START_ELECTION = 6;
    public static final int ACTION_JOIN_ELECTION = 7;
    public static final int ACTION_START_COUP = 8;
    public static final int ACTION_APPROVE_COUP = 9;
    public static final int ACTION_VOTE_CANDIDATE_1 = 10;
    public static final int ACTION_VOTE_CANDIDATE_2 = 11;
    public static final int ACTION_VOTE_CANDIDATE_3 = 12;
    public static final int ACTION_VOTE_CANDIDATE_4 = 13;
    public static final int ACTION_VOTE_CANDIDATE_5 = 14;
    public static final int ACTION_ROLES_CREATE = 15;

    private static final int SCROLL_X = 14;
    private static final int SCROLL_Y = 56;
    private static final int SCROLL_W = PANEL_WIDTH - 42;
    private static final int SCROLL_H = 128;
    private static final int SCROLL_BAR_X = PANEL_WIDTH - 26;
    private static final int SCROLL_BAR_W = 6;
    private static final int TAB_START_X = 10;
    private static final int TAB_Y = 36;
    private static final int TAB_W = 90;
    private static final int TAB_GAP = 95;
    private static final int TAB_H = 16;
    private static final int BTN_Y = PANEL_HEIGHT - 22;
    private static final int VOTE_Y = BTN_Y - 18;

    private static final String[][] TAB_DEFS = {
        {"Overview", "OVERVIEW"},
        {"Governance", "GOVERNANCE"},
        {"Economy", "ECONOMY"},
        {"Roles", "ROLES"}
    };

    private CivControlPanelSnapshot snapshot;
    private String selectedTab = "OVERVIEW";
    private ScrollContentComponent scrollContent;
    private final List<AbstractComponent<?>> dynamicBtns = new ArrayList<>();

    public ModernCivControlPanelScreen(CivControlPanelSnapshot snapshot) {
        super(Component.literal("Civic Control Console"), "Governance, roles, and civ management", 0xFF448AFF);
        this.snapshot = snapshot;
    }

    public void refresh(CivControlPanelSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        if (scrollContent != null) {
            scrollContent.getChildren().clear();
            scrollContent.setY(0);
            populateScrollContent();
        }
        rebuildDynamicButtons();
    }

    @Override
    protected void addScreenWidgets() {
        int tx = panelX + TAB_START_X;
        for (String[] tabDef : TAB_DEFS) {
            String tabId = tabDef[1];
            String tabLabel = tabDef[0];
            boolean active = tabId.equals(selectedTab);
            ButtonComponent btn = new ButtonComponent(tx, panelY + TAB_Y, TAB_W, TAB_H,
                    Component.literal(tabLabel), (b, screen, mx, my, button) -> {
                if (!selectedTab.equals(tabId)) {
                    selectedTab = tabId;
                    refreshTabContent();
                }
                return true;
            });
            addComponent(btn);
            tx += TAB_GAP;
        }

        rebuildDynamicButtons();

        ScrollContentComponent content = new ScrollContentComponent(0, 0, 2, ScrollOrientation.VERTICAL);
        scrollContent = content;
        populateScrollContent();

        addComponent(new ScrollPanelComponent(null, panelX + SCROLL_X, panelY + SCROLL_Y, SCROLL_W, SCROLL_H,
                ScrollOrientation.VERTICAL, content,
                createScrollBar(SCROLL_BAR_X, 0, SCROLL_BAR_W, SCROLL_H, ScrollOrientation.VERTICAL)));
    }

    private void refreshTabContent() {
        if (scrollContent != null) {
            scrollContent.getChildren().clear();
            scrollContent.setY(0);
            populateScrollContent();
        }
        rebuildDynamicButtons();
    }

    private void rebuildDynamicButtons() {
        for (AbstractComponent<?> btn : dynamicBtns) {
            removeComponent(btn);
        }
        dynamicBtns.clear();

        addBottomButtons();
        addVoteButtons();
    }

    private void addBottomButtons() {
        switch (selectedTab) {
            case "GOVERNANCE" -> addGovernanceButtons();
            case "ROLES" -> addRolesButtons();
            default -> addDefaultButtons();
        }
    }

    private void addVoteButtons() {
        if (!"GOVERNANCE".equals(selectedTab)) return;
        if (snapshot.leadershipCandidateCount() <= 0) return;

        int x = panelX + PANEL_WIDTH - 160;
        for (int i = 0; i < Math.min(5, snapshot.leadershipCandidateCount()); i++) {
            int ci = i;
            ButtonComponent btn = new ButtonComponent(x + i * 31, panelY + VOTE_Y, 29, 16,
                    Component.literal("V" + (i + 1)), (b, screen, mx, my, button) -> {
                sendAction(ACTION_VOTE_CANDIDATE_1 + ci);
                return true;
            });
            addComponent(btn);
            dynamicBtns.add(btn);
        }
    }

    private void addDefaultButtons() {
        dynamicBtns.add(addBtn(panelX + 10, panelY + BTN_Y, 60, 16, "Policy", ACTION_DISTRIBUTION_TOGGLE));
        dynamicBtns.add(addBtn(panelX + 74, panelY + BTN_Y, 60, 16, "Friendly PvP", ACTION_FRIENDLY_FIRE_TOGGLE));
        dynamicBtns.add(addBtn(panelX + 138, panelY + BTN_Y, 50, 16, "Approve", ACTION_PROPOSAL_YES));
        dynamicBtns.add(addBtn(panelX + 192, panelY + BTN_Y, 50, 16, "Reject", ACTION_PROPOSAL_NO));
    }

    private void addGovernanceButtons() {
        dynamicBtns.add(addBtn(panelX + 10, panelY + BTN_Y, 60, 16, "Model", ACTION_GOVERNANCE_CYCLE));
        dynamicBtns.add(addBtn(panelX + 74, panelY + BTN_Y, 50, 16, "Approve", ACTION_PROPOSAL_YES));
        dynamicBtns.add(addBtn(panelX + 128, panelY + BTN_Y, 50, 16, "Reject", ACTION_PROPOSAL_NO));

        boolean hasElection = "Election".equals(snapshot.leadershipContestType());
        boolean hasCoup = "Coup".equals(snapshot.leadershipContestType());

        if (!hasElection && !hasCoup) {
            dynamicBtns.add(addBtn(panelX + 182, panelY + BTN_Y, 50, 16, "Elect", ACTION_START_ELECTION));
            dynamicBtns.add(addBtn(panelX + 236, panelY + BTN_Y, 40, 16, "Coup", ACTION_START_COUP));
        }
        if (hasElection) {
            dynamicBtns.add(addBtn(panelX + 182, panelY + BTN_Y, 50, 16, "Join", ACTION_JOIN_ELECTION));
            dynamicBtns.add(addBtn(panelX + 236, panelY + BTN_Y, 50, 16, "Coup", ACTION_START_COUP));
        }
        if (hasCoup) {
            dynamicBtns.add(addBtn(panelX + 182, panelY + BTN_Y, 50, 16, "Join", ACTION_APPROVE_COUP));
            dynamicBtns.add(addBtn(panelX + 236, panelY + BTN_Y, 50, 16, "Elect", ACTION_START_ELECTION));
        }
    }

    private void addRolesButtons() {
        dynamicBtns.add(addBtn(panelX + 10, panelY + BTN_Y, 60, 16, "Policy", ACTION_DISTRIBUTION_TOGGLE));
        dynamicBtns.add(addBtn(panelX + 74, panelY + BTN_Y, 60, 16, "Friendly PvP", ACTION_FRIENDLY_FIRE_TOGGLE));
        dynamicBtns.add(addBtn(panelX + 138, panelY + BTN_Y, 60, 16, "Create Role", ACTION_ROLES_CREATE));
    }

    private ButtonComponent addBtn(int x, int y, int w, int h, String label, int actionId) {
        ButtonComponent btn = new ButtonComponent(x, y, w, h, Component.literal(label),
                (b, screen, mx, my, button) -> { sendAction(actionId); return true; });
        addComponent(btn);
        return btn;
    }

    // -- Scroll content population --

    private void populateScrollContent() {
        switch (selectedTab) {
            case "OVERVIEW" -> populateOverview();
            case "ECONOMY" -> populateEconomy();
            case "GOVERNANCE" -> populateGovernance();
            case "ROLES" -> populateRoles();
        }
    }

    private void populateOverview() {
        addInfo("Leader", snapshot.leaderTitle());
        addInfo("Model", snapshot.governanceModel());
        addInfo("Members", String.valueOf(snapshot.memberCount()));
        addInfo("Managers", String.valueOf(snapshot.managerCount()));
        addInfo("Custom roles", String.valueOf(snapshot.customRoleCount()));
        addInfo("Join requests", String.valueOf(snapshot.joinRequestCount()));
        addInfo("Open invites", String.valueOf(snapshot.inviteCount()));
        addInfo("Friendly fire", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled");

        scrollContent.addChild(new SpacerRow(0, 0, 4));
        scrollContent.addChild(new SectionLabel(0, 0, "Proposal", 0xFF9DB0C2));
        addInfo("Summary", snapshot.pendingProposalSummary());
        String votes = snapshot.pendingProposalRequiredYesVotes() <= 0 ? "-"
                : snapshot.pendingProposalYesVotes() + "/" + snapshot.pendingProposalRequiredYesVotes() + " yes";
        addInfo("Votes", votes);
    }

    private void populateEconomy() {
        addInfo("Civ treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents()));
        addInfo("Your karma", RealCivUtil.formatCredits(snapshot.playerContributionKarmaCents()));
        addInfo("Total contributions", String.valueOf(snapshot.playerContributedItemsTotal()));
        addInfo("Hub stock types", String.valueOf(snapshot.hubStockEntryCount()));
        addInfo("Distribution mode", snapshot.hubDistributionMode());
        addInfo("Daily allowances", String.valueOf(snapshot.hubDailyAllowanceCount()));
        String cap = snapshot.maxContributionKarmaGainPerDayCents() <= 0
                ? "Unlimited" : RealCivUtil.formatCredits(snapshot.maxContributionKarmaGainPerDayCents());
        addInfo("Karma cap/day", cap);
    }

    private void populateGovernance() {
        addInfo("Leader", snapshot.leaderTitle());
        addInfo("Model", snapshot.governanceModel());
        addInfo("Workflow", snapshot.governanceApprovalWorkflowEnabled() ? "Enabled" : "Disabled");
        addInfo("You are", snapshot.playerRole());

        scrollContent.addChild(new SpacerRow(0, 0, 4));

        boolean hasContest = !"None".equals(snapshot.leadershipContestType());
        if (hasContest) {
            scrollContent.addChild(new SectionLabel(0, 0,
                    "Active Contest: " + snapshot.leadershipContestType(), 0xFFFFD54F));
            addInfo("Status", snapshot.leadershipContestSummary());

            if ("Election".equals(snapshot.leadershipContestType())) {
                addInfo("Votes cast", String.valueOf(snapshot.leadershipElectionVoteCount()));
                addInfo("Candidates", String.valueOf(snapshot.leadershipCandidateCount()));

                for (int i = 0; i < Math.min(5, snapshot.leadershipCandidateEntries().size()); i++) {
                    String entry = snapshot.leadershipCandidateEntries().get(i);
                    int split = entry.indexOf('|');
                    String val = split >= 0 && split + 1 < entry.length() ? entry.substring(split + 1) : entry;
                    addInfo("Candidate #" + (i + 1), val);
                }

                if (snapshot.leadershipCandidateCount() > 0) {
                    scrollContent.addChild(new RowLabel(0, 0,
                            "Use V1-V5 buttons below to vote", 0xFF78909C));
                }
            } else if ("Coup".equals(snapshot.leadershipContestType())) {
                addInfo("Coup leader", snapshot.leadershipCoupLeaderName());
                String approvals = snapshot.leadershipCoupRequiredApprovals() <= 0 ? "-"
                        : snapshot.leadershipCoupApprovalCount() + "/" + snapshot.leadershipCoupRequiredApprovals();
                addInfo("Approvals", approvals);
            }
        } else {
            addInfo("Contest", "None");
            scrollContent.addChild(new RowLabel(0, 0,
                    "Start an election or coup to change leadership.", 0xFF78909C));
        }
    }

    private void populateRoles() {
        addInfo("Custom roles", String.valueOf(snapshot.customRoleCount()));
        addInfo("Managers", String.valueOf(snapshot.managerCount()));
        addInfo("Leader title", snapshot.leaderTitle());
        addInfo("Distribution", snapshot.hubDistributionMode());
        addInfo("Friendly PvP", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled");

        scrollContent.addChild(new SpacerRow(0, 0, 4));

        if (snapshot.canManageGovernance()) {
            scrollContent.addChild(new SectionLabel(0, 0, "Available permissions for roles", 0xFFC6D2DE));

            String[][] perms = {
                {"manage_diplomacy", "Manage diplomacy"},
                {"manage_census", "Manage membership"},
                {"manage_hub_distribution", "Manage hub policy"},
                {"manage_upkeep", "Manage tax/upkeep"},
                {"manage_friendly_fire", "Toggle PvP"},
                {"manage_leadership", "Manage leadership"},
                {"manage_land_zoning", "Manage land zones"},
                {"police_members", "Police members"}
            };
            for (String[] p : perms) {
                scrollContent.addChild(new RowLabel(0, 0, "  \u2022 " + p[1], 0xFF9BA9B7));
            }

            scrollContent.addChild(new SpacerRow(0, 0, 4));
            scrollContent.addChild(new RowLabel(0, 0,
                    "Use 'Create Role' button below to add a new role.", 0xFF78909C));
        } else {
            scrollContent.addChild(new RowLabel(0, 0,
                    "Role management is available to leadership.", 0xFF78909C));
        }
    }

    private void addInfo(String label, String value) {
        scrollContent.addChild(new LabelValueRow(0, 0, label, value, 0xFFF4F7FA));
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_CONTROL_PANEL, actionId));
    }
}
