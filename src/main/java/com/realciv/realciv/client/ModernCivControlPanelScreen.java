package com.realciv.realciv.client;

import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.network.RealCivPayloads;
import com.realciv.realciv.panel.CivControlPanelSnapshot;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernCivControlPanelScreen extends RealCivPanelScreen {
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

    private static final int ROW_WIDTH = PANEL_WIDTH - 32;
    private static final int TAB_START_X = 10;
    private static final int TAB_Y = 38;
    private static final int TAB_WIDTH = 74;
    private static final int TAB_SPACING = 78;
    private static final int BOTTOM_BTN_Y = PANEL_HEIGHT - 22;
    private static final int VOTE_BTN_Y = BOTTOM_BTN_Y - 18;

    private CivControlPanelSnapshot snapshot;
    private String selectedTab = "OVERVIEW";

    public ModernCivControlPanelScreen(CivControlPanelSnapshot snapshot) {
        super(Component.literal("Civic Control Console"), "Governance, roles, and civ management", 0xFF448AFF);
        this.snapshot = snapshot;
    }

    public void refresh(CivControlPanelSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshContent();
    }

    @Override
    public void addWidgets() {
        String[][] tabsDef = {{"Overview", "OVERVIEW"}, {"Governance", "GOVERNANCE"}, {"Economy", "ECONOMY"}, {"Roles", "ROLES"}};
        int tx = TAB_START_X;
        for (String[] tabDef : tabsDef) {
            String tabId = tabDef[1];
            String tabLabel = tabDef[0];
            boolean active = tabId.equals(selectedTab);
            int tc = active ? 0xFFFFFFFF : 0xFF78909C;
            add(new NordButton(this, Component.literal(tabLabel), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) {
                    if (!selectedTab.equals(tabId)) {
                        selectedTab = tabId;
                        refreshWidgets();
                    }
                }
            }.setPosAndSize(tx, TAB_Y, TAB_WIDTH, 18));
            tx += TAB_SPACING;
        }

        setupContentPanel();
        refreshContent();

        switch (selectedTab) {
            case "GOVERNANCE" -> addGovernanceButtons();
            case "ROLES" -> addRolesButtons();
            default -> addDefaultButtons();
        }

        int voteBtnX = PANEL_WIDTH - 160;
        if (snapshot.leadershipCandidateCount() > 0 && "GOVERNANCE".equals(selectedTab)) {
            for (int i = 0; i < Math.min(5, snapshot.leadershipCandidateCount()); i++) {
                int ci = i;
                add(new NordButton(this, Component.literal("V" + (i + 1)), Icon.empty()) {
                    @Override
                    public void onClicked(MouseButton button) {
                        sendAction(ACTION_VOTE_CANDIDATE_1 + ci);
                    }
                }.setPosAndSize(voteBtnX + i * 31, VOTE_BTN_Y, 29, 16));
            }
        }
    }

    private void addDefaultButtons() {
        add(new NordButton(this, Component.literal("Policy"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_DISTRIBUTION_TOGGLE); }
        }.setPosAndSize(10, BOTTOM_BTN_Y, 60, 16));

        add(new NordButton(this, Component.literal("Friendly PvP"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_FRIENDLY_FIRE_TOGGLE); }
        }.setPosAndSize(74, BOTTOM_BTN_Y, 60, 16));

        add(new NordButton(this, Component.literal("Approve"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PROPOSAL_YES); }
        }.setPosAndSize(138, BOTTOM_BTN_Y, 50, 16));

        add(new NordButton(this, Component.literal("Reject"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PROPOSAL_NO); }
        }.setPosAndSize(192, BOTTOM_BTN_Y, 50, 16));
    }

    private void addGovernanceButtons() {
        add(new NordButton(this, Component.literal("Model"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_GOVERNANCE_CYCLE); }
        }.setPosAndSize(10, BOTTOM_BTN_Y, 60, 16));

        add(new NordButton(this, Component.literal("Approve"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PROPOSAL_YES); }
        }.setPosAndSize(74, BOTTOM_BTN_Y, 50, 16));

        add(new NordButton(this, Component.literal("Reject"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_PROPOSAL_NO); }
        }.setPosAndSize(128, BOTTOM_BTN_Y, 50, 16));

        boolean hasElection = "Election".equals(snapshot.leadershipContestType());
        boolean hasCoup = "Coup".equals(snapshot.leadershipContestType());

        if (!hasElection && !hasCoup) {
            add(new NordButton(this, Component.literal("Elect"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_START_ELECTION); }
            }.setPosAndSize(182, BOTTOM_BTN_Y, 50, 16));
            add(new NordButton(this, Component.literal("Coup"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_START_COUP); }
            }.setPosAndSize(236, BOTTOM_BTN_Y, 40, 16));
        }
        if (hasElection) {
            add(new NordButton(this, Component.literal("Join"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_JOIN_ELECTION); }
            }.setPosAndSize(182, BOTTOM_BTN_Y, 50, 16));
            add(new NordButton(this, Component.literal("Coup"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_START_COUP); }
            }.setPosAndSize(236, BOTTOM_BTN_Y, 50, 16));
        }
        if (hasCoup) {
            add(new NordButton(this, Component.literal("Join"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_APPROVE_COUP); }
            }.setPosAndSize(182, BOTTOM_BTN_Y, 50, 16));
            add(new NordButton(this, Component.literal("Elect"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton button) { sendAction(ACTION_START_ELECTION); }
            }.setPosAndSize(236, BOTTOM_BTN_Y, 50, 16));
        }
    }

    private void addRolesButtons() {
        add(new NordButton(this, Component.literal("Policy"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_DISTRIBUTION_TOGGLE); }
        }.setPosAndSize(10, BOTTOM_BTN_Y, 60, 16));

        add(new NordButton(this, Component.literal("Friendly PvP"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_FRIENDLY_FIRE_TOGGLE); }
        }.setPosAndSize(74, BOTTOM_BTN_Y, 60, 16));

        add(new NordButton(this, Component.literal("Create Role"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) { sendAction(ACTION_ROLES_CREATE); }
        }.setPosAndSize(138, BOTTOM_BTN_Y, 60, 16));
    }

    @Override
    protected void populateContentPanel(Panel panel) {
        switch (selectedTab) {
            case "OVERVIEW" -> populateOverview(panel);
            case "ECONOMY" -> populateEconomy(panel);
            case "GOVERNANCE" -> populateGovernance(panel);
            case "ROLES" -> populateRoles(panel);
        }
    }

    private void populateOverview(Panel panel) {
        panel.add(new InfoRowWidget(panel, "Leader", snapshot.leaderTitle()));
        panel.add(new InfoRowWidget(panel, "Model", snapshot.governanceModel()));
        panel.add(new InfoRowWidget(panel, "Members", String.valueOf(snapshot.memberCount())));
        panel.add(new InfoRowWidget(panel, "Managers", String.valueOf(snapshot.managerCount())));
        panel.add(new InfoRowWidget(panel, "Custom roles", String.valueOf(snapshot.customRoleCount())));
        panel.add(new InfoRowWidget(panel, "Join requests", String.valueOf(snapshot.joinRequestCount())));
        panel.add(new InfoRowWidget(panel, "Open invites", String.valueOf(snapshot.inviteCount())));
        panel.add(new InfoRowWidget(panel, "Friendly fire", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled"));
        panel.add(new InfoRowWidget(panel, "Proposal", snapshot.pendingProposalSummary()));
        panel.add(new InfoRowWidget(panel, "Votes", snapshot.pendingProposalRequiredYesVotes() <= 0 ? "-"
                : snapshot.pendingProposalYesVotes() + "/" + snapshot.pendingProposalRequiredYesVotes() + " yes"));
    }

    private void populateEconomy(Panel panel) {
        panel.add(new InfoRowWidget(panel, "Civ treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents())));
        panel.add(new InfoRowWidget(panel, "Your karma", RealCivUtil.formatCredits(snapshot.playerContributionKarmaCents())));
        panel.add(new InfoRowWidget(panel, "Total contributions", String.valueOf(snapshot.playerContributedItemsTotal())));
        panel.add(new InfoRowWidget(panel, "Hub stock types", String.valueOf(snapshot.hubStockEntryCount())));
        panel.add(new InfoRowWidget(panel, "Distribution", snapshot.hubDistributionMode()));
        panel.add(new InfoRowWidget(panel, "Daily allowances", String.valueOf(snapshot.hubDailyAllowanceCount())));
        panel.add(new InfoRowWidget(panel, "Karma cap/day", snapshot.maxContributionKarmaGainPerDayCents() <= 0
                ? "Unlimited" : RealCivUtil.formatCredits(snapshot.maxContributionKarmaGainPerDayCents())));
    }

    private void populateGovernance(Panel panel) {
        panel.add(new InfoRowWidget(panel, "Leader", snapshot.leaderTitle()));
        panel.add(new InfoRowWidget(panel, "Model", snapshot.governanceModel()));
        panel.add(new InfoRowWidget(panel, "Workflow", snapshot.governanceApprovalWorkflowEnabled() ? "Enabled" : "Disabled"));
        panel.add(new InfoRowWidget(panel, "You are", snapshot.playerRole()));

        panel.add(createTextWidget("", 0));

        boolean hasContest = !"None".equals(snapshot.leadershipContestType());
        if (hasContest) {
            panel.add(createHeaderWidget("Active Contest: " + snapshot.leadershipContestType(), 0xFFFFD54F));
            panel.add(new InfoRowWidget(panel, "Status", snapshot.leadershipContestSummary()));

            if ("Election".equals(snapshot.leadershipContestType())) {
                panel.add(new InfoRowWidget(panel, "Votes cast", String.valueOf(snapshot.leadershipElectionVoteCount())));
                panel.add(new InfoRowWidget(panel, "Candidates", String.valueOf(snapshot.leadershipCandidateCount())));

                for (int i = 0; i < Math.min(5, snapshot.leadershipCandidateEntries().size()); i++) {
                    String entry = snapshot.leadershipCandidateEntries().get(i);
                    int split = entry.indexOf('|');
                    String val = split >= 0 && split + 1 < entry.length() ? entry.substring(split + 1) : entry;
                    panel.add(new InfoRowWidget(panel, "Candidate #" + (i + 1), val));
                }

                if (snapshot.leadershipCandidateCount() > 0) {
                    panel.add(createTextWidget("Use V1-V5 buttons below to vote", 0xFF78909C));
                }
            } else if ("Coup".equals(snapshot.leadershipContestType())) {
                panel.add(new InfoRowWidget(panel, "Coup leader", snapshot.leadershipCoupLeaderName()));
                panel.add(new InfoRowWidget(panel, "Approvals",
                        snapshot.leadershipCoupRequiredApprovals() <= 0 ? "-"
                                : snapshot.leadershipCoupApprovalCount() + "/" + snapshot.leadershipCoupRequiredApprovals()));
            }
        } else {
            panel.add(new InfoRowWidget(panel, "Contest", "None"));
            panel.add(createTextWidget("Start an election or coup to change leadership.", 0xFF78909C));
        }
    }

    private void populateRoles(Panel panel) {
        panel.add(new InfoRowWidget(panel, "Custom roles", String.valueOf(snapshot.customRoleCount())));
        panel.add(new InfoRowWidget(panel, "Managers", String.valueOf(snapshot.managerCount())));
        panel.add(new InfoRowWidget(panel, "Leader title", snapshot.leaderTitle()));
        panel.add(new InfoRowWidget(panel, "Distribution", snapshot.hubDistributionMode()));
        panel.add(new InfoRowWidget(panel, "Friendly PvP", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled"));

        panel.add(createTextWidget("", 0));

        if (snapshot.canManageGovernance()) {
            panel.add(createTextWidget("Available permissions for roles:", 0xFFC6D2DE));

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
                panel.add(createTextWidget("  " + p[1], 0xFF9BA9B7));
            }

            panel.add(createTextWidget("", 0));
            panel.add(createTextWidget("Use 'Create Role' button below to add a new role.", 0xFF78909C));
        } else {
            panel.add(createTextWidget("Role management is available to leadership.", 0xFF78909C));
        }
    }

    @Override
    protected void alignContentPanel(Panel panel) {
        for (Widget w : panel.getWidgets()) {
            if (w.width == 0) w.setSize(ROW_WIDTH, ROW_HEIGHT);
            else if (w.width < 10) w.setSize(ROW_WIDTH, w.height > 0 ? w.height : ROW_HEIGHT);
        }
        panel.align(new WidgetLayout.Vertical(2, 2, 4));
    }

    private void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_CONTROL_PANEL, actionId));
    }

    private static class InfoRowWidget extends Widget {
        private final String label;
        private final String value;

        InfoRowWidget(Panel panel, String label, String value) {
            super(panel);
            this.label = label;
            this.value = value;
            setSize(ROW_WIDTH, ROW_HEIGHT);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.literal(label), x, y, 0xFF9BA9B7, false);
            String trimmed = font.plainSubstrByWidth(value, ROW_WIDTH - 120);
            graphics.drawString(font, Component.literal(trimmed), x + 120, y, 0xFFF4F7FA, false);
        }

        @Override
        public boolean mousePressed(MouseButton button) { return false; }
    }
}
