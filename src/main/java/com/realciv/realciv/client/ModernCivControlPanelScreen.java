package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.icon.Icon;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.network.RealCivPayloads;
import com.realciv.realciv.panel.CivControlPanelSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModernCivControlPanelScreen extends RealCivScreen {
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
    public static final int ACTION_SET_ATTRIBUTE = 1000;

    private static final int TAB_W = 90;
    private static final int TAB_H = 16;
    private static final int BTN_Y = 270;
    private static final int VOTE_Y = 245;

    private CivControlPanelSnapshot snapshot;
    private String selectedTab = "OVERVIEW";

    public ModernCivControlPanelScreen(CivControlPanelSnapshot snapshot) {
        super(Component.literal("Civic Control Console"), "Governance, roles, and civ management", 0xFF448AFF);
        this.snapshot = snapshot;
    }

    public void refresh(CivControlPanelSnapshot newSnapshot) {
        this.snapshot = newSnapshot;
        refreshWidgets();
    }

    @Override
    protected void addFixedWidgets() {
        addTabButtons();
        addBottomButtons();
        addVoteButtons();
    }

    private void addTabButtons() {
        int tx = 10;
        for (String[] tabDef : TAB_DEFS) {
            String tabId = tabDef[1];
            String tabLabel = tabDef[0];
            Widget btn = createTabButton(tabLabel, tabId, tx);
            add(btn);
            tx += 94;
        }
    }

    private Widget createTabButton(String label, String tabId, int x) {
        return new SimpleTextButton(this, Component.literal(label), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                if (!selectedTab.equals(tabId)) {
                    selectedTab = tabId;
                    refreshWidgets();
                }
            }

            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                boolean active = tabId.equals(selectedTab);
                graphics.fill(x, y, x + w, y + h, active ? 0xFF2A3A5C : 0xFF1A2129);
                graphics.fill(x, y + h - 2, x + w, y + h, active ? 0xFF448AFF : 0xFF333333);
                int textX = x + (w - theme.getStringWidth(getTitle())) / 2;
                int textY = y + (h - theme.getFontHeight()) / 2;
                var font = Minecraft.getInstance().font;
                graphics.drawString(font, getTitle(), textX, textY,
                        active ? 0xFFFFFFFF : 0xFF9BA9B7, true);
            }
        }.setPosAndSize(x, 2, TAB_W, TAB_H);
    }

    private void addBottomButtons() {
        switch (selectedTab) {
            case "GOVERNANCE" -> {
                addBtn(ACTION_PROPOSAL_YES, "Approve", 50, 10, BTN_Y);
                addBtn(ACTION_PROPOSAL_NO, "Reject", 50, 64, BTN_Y);
                boolean hasElection = "Election".equals(snapshot.leadershipContestType());
                boolean hasCoup = "Coup".equals(snapshot.leadershipContestType());

                int bx = 118;
                if (!hasElection && !hasCoup) {
                    addBtn(ACTION_START_ELECTION, "Elect", 50, bx, BTN_Y);
                    addBtn(ACTION_START_COUP, "Coup", 40, bx + 54, BTN_Y);
                }
                if (hasElection) {
                    addBtn(ACTION_JOIN_ELECTION, "Join", 50, bx, BTN_Y);
                    addBtn(ACTION_START_COUP, "Coup", 50, bx + 54, BTN_Y);
                }
                if (hasCoup) {
                    addBtn(ACTION_APPROVE_COUP, "Join", 50, bx, BTN_Y);
                    addBtn(ACTION_START_ELECTION, "Elect", 50, bx + 54, BTN_Y);
                }
            }
            case "ROLES" -> {
                addBtn(ACTION_DISTRIBUTION_TOGGLE, "Policy", 60, 10, BTN_Y);
                addBtn(ACTION_FRIENDLY_FIRE_TOGGLE, "Friendly PvP", 80, 74, BTN_Y);
                addBtn(ACTION_ROLES_CREATE, "Create Role", 80, 158, BTN_Y);
            }
            default -> {
                addBtn(ACTION_DISTRIBUTION_TOGGLE, "Policy", 60, 10, BTN_Y);
                addBtn(ACTION_FRIENDLY_FIRE_TOGGLE, "Friendly PvP", 80, 74, BTN_Y);
                addBtn(ACTION_PROPOSAL_YES, "Approve", 50, 158, BTN_Y);
                addBtn(ACTION_PROPOSAL_NO, "Reject", 50, 212, BTN_Y);
            }
        }
    }

    private void addBtn(int action, String label, int width, int x, int y) {
        SimpleTextButton btn = makeFixedBtn(action, label, width);
        btn.setPos(x, y);
        add(btn);
    }

    private void addVoteButtons() {
        if (!"GOVERNANCE".equals(selectedTab)) return;
        if (snapshot.leadershipCandidateCount() <= 0) return;

        int x = PANEL_W - 170;
        for (int i = 0; i < Math.min(5, snapshot.leadershipCandidateCount()); i++) {
            SimpleTextButton btn = makeFixedBtn(ACTION_VOTE_CANDIDATE_1 + i, "V" + (i + 1), 29);
            btn.setPos(x + i * 31, VOTE_Y);
            add(btn);
        }
    }

    @Override
    protected void addScrollContent(Panel panel) {
        switch (selectedTab) {
            case "OVERVIEW" -> populateOverview();
            case "ECONOMY" -> populateEconomy();
            case "GOVERNANCE" -> populateGovernance();
            case "ROLES" -> populateRoles();
        }
    }

    private void populateOverview() {
        addLabelRow("Leader", snapshot.leaderTitle());
        for (AttributeCategory cat : AttributeCategory.values()) {
            String attrKey = activeAttributeKey(cat);
            CivicAttribute attr = CivicAttribute.fromSerializedName(attrKey);
            String display = attr != null ? attr.displayName() : attrKey;
            addLabelRow(cat.displayName(), display);
        }
        addLabelRow("Members", String.valueOf(snapshot.memberCount()));
        addLabelRow("Managers", String.valueOf(snapshot.managerCount()));
        addLabelRow("Custom roles", String.valueOf(snapshot.customRoleCount()));
        addLabelRow("Join requests", String.valueOf(snapshot.joinRequestCount()));
        addLabelRow("Open invites", String.valueOf(snapshot.inviteCount()));
        addLabelRow("Friendly fire", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled");
        addSpacer(4);
        addSection("Proposal");
        addLabelRow("Summary", snapshot.pendingProposalSummary(), 0xFF9BA9B7);
        String votes = snapshot.pendingProposalRequiredYesVotes() <= 0 ? "-"
                : snapshot.pendingProposalYesVotes() + "/" + snapshot.pendingProposalRequiredYesVotes() + " yes";
        addLabelRow("Votes", votes);
    }

    private void populateEconomy() {
        addLabelRow("Civ treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents()));
        addLabelRow("Your karma", RealCivUtil.formatCredits(snapshot.playerContributionKarmaCents()));
        addLabelRow("Total contributions", String.valueOf(snapshot.playerContributedItemsTotal()));
        addLabelRow("Hub stock types", String.valueOf(snapshot.hubStockEntryCount()));
        addLabelRow("Distribution mode", snapshot.resourceAttribute());
        addLabelRow("Daily allowances", String.valueOf(snapshot.hubDailyAllowanceCount()));
        String cap = snapshot.maxContributionKarmaGainPerDayCents() <= 0
                ? "Unlimited" : RealCivUtil.formatCredits(snapshot.maxContributionKarmaGainPerDayCents());
        addLabelRow("Karma cap/day", cap);
        addSpacer(4);
        addSection("Resource Policy", 0xFFC6D2DE);
        addAttributeSelectorButtons(snapshot.resourceAttribute());
    }

    private void populateGovernance() {
        addLabelRow("Leader", snapshot.leaderTitle());
        addLabelRow("Workflow", snapshot.governanceApprovalWorkflowEnabled() ? "Enabled" : "Disabled");
        addLabelRow("You are", snapshot.playerRole());
        addSpacer(4);

        for (AttributeCategory cat : AttributeCategory.values()) {
            if (cat == AttributeCategory.RESOURCE) continue;
            addSection(cat.displayName(), 0xFFC6D2DE);
            addAttributeSelectorButtons(activeAttributeKey(cat));
        }

        addSpacer(4);
        addSection("Leadership Contest", 0xFF9DB0C2);

        boolean hasContest = !"None".equals(snapshot.leadershipContestType());
        if (hasContest) {
            currentY += ROW_H;
            addLabelRow("Active Contest", snapshot.leadershipContestType(), 0xFFFFD54F);
            addLabelRow("Status", snapshot.leadershipContestSummary());
            if ("Election".equals(snapshot.leadershipContestType())) {
                addLabelRow("Votes cast", String.valueOf(snapshot.leadershipElectionVoteCount()));
                addLabelRow("Candidates", String.valueOf(snapshot.leadershipCandidateCount()));
                for (int i = 0; i < Math.min(5, snapshot.leadershipCandidateEntries().size()); i++) {
                    String entry = snapshot.leadershipCandidateEntries().get(i);
                    int split = entry.indexOf('|');
                    String val = split >= 0 && split + 1 < entry.length() ? entry.substring(split + 1) : entry;
                    addLabelRow("Candidate #" + (i + 1), val);
                }
                if (snapshot.leadershipCandidateCount() > 0) {
                    addLabelRow("", "Use V1-V5 buttons below to vote", 0xFF78909C);
                }
            } else if ("Coup".equals(snapshot.leadershipContestType())) {
                addLabelRow("Coup leader", snapshot.leadershipCoupLeaderName());
                String approvals = snapshot.leadershipCoupRequiredApprovals() <= 0 ? "-"
                        : snapshot.leadershipCoupApprovalCount() + "/" + snapshot.leadershipCoupRequiredApprovals();
                addLabelRow("Approvals", approvals);
            }
        } else {
            addLabelRow("Contest", "None");
            addLabelRow("", "Start an election or coup to change leadership.", 0xFF78909C);
        }
    }

    private void addAttributeSelectorButtons(String active) {
        for (AttributeCategory cat : AttributeCategory.values()) {
            String catActive = activeAttributeKey(cat);
            for (CivicAttribute attr : CivicAttribute.values()) {
                if (attr.category() != cat) continue;
                if (!attr.serializedName().equals(catActive) && attr.category() == AttributeCategory.RESOURCE && cat != AttributeCategory.RESOURCE) continue;
                boolean isActive = attr.serializedName().equals(catActive);
                String label = (isActive ? "[x] " : "[  ] ") + attr.displayName();
                int color = isActive ? 0xFFA5D6A7 : 0xFF9BA9B7;
                int attrOrdinal = attr.ordinal();
                addSelectableRow(label, color, btn -> sendAction(ACTION_SET_ATTRIBUTE + attrOrdinal));
            }
        }
    }

    private String activeAttributeKey(AttributeCategory category) {
        return switch (category) {
            case EXECUTIVE -> snapshot.executiveAttribute();
            case SUCCESSION -> snapshot.successionAttribute();
            case RESOURCE -> snapshot.resourceAttribute();
            case TAXATION -> snapshot.taxationAttribute();
            case MEMBERSHIP -> snapshot.membershipAttribute();
            case LAND -> snapshot.landAttribute();
        };
    }

    private void populateRoles() {
        addLabelRow("Custom roles", String.valueOf(snapshot.customRoleCount()));
        addLabelRow("Managers", String.valueOf(snapshot.managerCount()));
        addLabelRow("Leader title", snapshot.leaderTitle());
        addLabelRow("Resource policy", snapshot.resourceAttribute());
        addLabelRow("Friendly PvP", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled");
        addSpacer(4);

        if (snapshot.canManageGovernance()) {
            addSection("Available permissions for roles", 0xFFC6D2DE);
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
                addLabelRow("  " + p[1], "", 0xFF9BA9B7);
            }
            addSpacer(4);
            addLabelRow("", "Use 'Create Role' button below to add a new role.", 0xFF78909C);
        } else {
            addLabelRow("", "Role management is available to leadership.", 0xFF78909C);
        }
    }

    @Override
    protected void sendAction(int actionId) {
        PacketDistributor.sendToServer(new RealCivPayloads.RealCivActionPayload(
                RealCivPayloads.SCREEN_CONTROL_PANEL, actionId));
    }

    private static final String[][] TAB_DEFS = {
        {"Overview", "OVERVIEW"},
        {"Governance", "GOVERNANCE"},
        {"Economy", "ECONOMY"},
        {"Roles", "ROLES"}
    };
}
