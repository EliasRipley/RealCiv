package com.realciv.realciv.client;

import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.icon.Icon;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.network.RealCivPayloads;
import com.realciv.realciv.panel.CivControlPanelSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
    public static final int ACTION_OPEN_RATION_EDITOR = 16;
    public static final int ACTION_ROLES_MANAGE = 17;
    public static final int ACTION_SET_ATTRIBUTE = 1000;

    private static final int TAB_W = 90;
    private static final int TAB_H = 14;
    private static final int TAB_Y = 30;
    private static final int BTN_Y = FOOTER_Y;
    private static final int VOTE_Y = FOOTER_Y + 16;

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
        }.setPosAndSize(x, TAB_Y, TAB_W, TAB_H);
    }

    private void addBottomButtons() {
        if ("OVERVIEW".equals(selectedTab)) {
            addBtn(ACTION_FRIENDLY_FIRE_TOGGLE, "Friendly PvP", 90, PANEL_W - 106, BTN_Y);
        } else if ("ECONOMY".equals(selectedTab)) {
            if (snapshot.canManageHubDistribution() && isRationedPolicy()) {
                addBtn(ACTION_OPEN_RATION_EDITOR, "Rations", 74, PANEL_W - 112, BTN_Y);
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
        addIdentitySection(snapshot.civilizationName(), snapshot.playerRole(), hasAnyManagementAccess());
        addSection("Governance Snapshot", 0xFFAED3FF);
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
        addSection("Quick Actions", 0xFFC6D2DE);
        addRowWithButtons(
                "Friendly PvP",
                "Toggle member PvP.",
                makeActionButton("Toggle", ACTION_FRIENDLY_FIRE_TOGGLE, 62));
        addSpacer(4);
        addSection("Proposal");
        addLabelRow("Summary", snapshot.pendingProposalSummary(), 0xFF9BA9B7);
        String votes = snapshot.pendingProposalRequiredYesVotes() <= 0 ? "-"
                : snapshot.pendingProposalYesVotes() + "/" + snapshot.pendingProposalRequiredYesVotes() + " yes";
        addLabelRow("Votes", votes);
    }

    private void populateEconomy() {
        addIdentitySection(snapshot.civilizationName(), snapshot.playerRole(), snapshot.canManageHubDistribution());
        addSection("Economy Snapshot", 0xFFFFC58A);
        addLabelRow("Civ treasury", RealCivUtil.formatCredits(snapshot.civTreasuryCents()));
        addLabelRow("Your karma", RealCivUtil.formatCredits(snapshot.playerContributionKarmaCents()));
        addLabelRow("Total contributions", String.valueOf(snapshot.playerContributedItemsTotal()));
        addLabelRow("Hub stock types", String.valueOf(snapshot.hubStockEntryCount()));
        addLabelRow("Distribution mode", displayAttribute(snapshot.resourceAttribute()));
        addLabelRow("Daily allowances", String.valueOf(snapshot.hubDailyAllowanceCount()));
        if (!snapshot.hubDailyAllowanceEntries().isEmpty()) {
            addSpacer(2);
            addSection("Ration Allowance Preview", 0xFF80CBC4);
            for (String allowanceEntry : snapshot.hubDailyAllowanceEntries()) {
                int split = allowanceEntry.indexOf('|');
                if (split <= 0 || split + 1 >= allowanceEntry.length()) {
                    continue;
                }
                String itemId = allowanceEntry.substring(0, split);
                int dailyAllowance = parseNonNegativeInt(allowanceEntry.substring(split + 1));
                addLabelRow(resolveItemDisplayName(itemId), dailyAllowance + "/day", 0xFF9DB0C2);
            }
            int hidden = Math.max(0, snapshot.hubDailyAllowanceCount() - snapshot.hubDailyAllowanceEntries().size());
            if (hidden > 0) {
                addLabelRow("", "+" + hidden + " more item allowance entr" + (hidden == 1 ? "y" : "ies"), 0xFF78909C);
            }
        }
        String cap = snapshot.maxContributionKarmaGainPerDayCents() <= 0
                ? "Unlimited" : RealCivUtil.formatCredits(snapshot.maxContributionKarmaGainPerDayCents());
        addLabelRow("Karma cap/day", cap);
        addSpacer(4);
        addSection("Resource Policy", 0xFFC6D2DE);
        if (snapshot.canManageHubDistribution() && isRationedPolicy()) {
            addRowWithButtons(
                    "Rations",
                    "Open ration limits editor.",
                    makeActionButton("Open", ACTION_OPEN_RATION_EDITOR, 62));
        }
        addAttributeSelectorButtons(AttributeCategory.RESOURCE, snapshot.resourceAttribute());
    }

    private void populateGovernance() {
        addIdentitySection(snapshot.civilizationName(), snapshot.playerRole(), snapshot.canManageGovernance());
        addSection("Governance Controls", 0xFFAED3FF);
        addLabelRow("Leader", snapshot.leaderTitle());
        addLabelRow("Workflow", snapshot.governanceApprovalWorkflowEnabled() ? "Enabled" : "Disabled");
        addLabelRow("Friendly fire", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled");
        addSpacer(4);
        addSection("Proposal Decision", 0xFFC6D2DE);
        addLabelRow("Proposal", snapshot.pendingProposalSummary(), 0xFF9BA9B7);
        addRowWithButtons(
                "Vote",
                "Approve/reject proposal.",
                makeActionButton("Approve", ACTION_PROPOSAL_YES, 68),
                makeActionButton("Reject", ACTION_PROPOSAL_NO, 62));
        addSpacer(4);

        for (AttributeCategory cat : AttributeCategory.values()) {
            if (cat == AttributeCategory.RESOURCE) continue;
            addSection(cat.displayName(), 0xFFC6D2DE);
            addAttributeSelectorButtons(cat, activeAttributeKey(cat));
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
                addRowWithButtons(
                        "Election actions",
                        "Join ballot or start coup.",
                        makeActionButton("Join Election", ACTION_JOIN_ELECTION, 92),
                        makeActionButton("Start Coup", ACTION_START_COUP, 78));
            } else if ("Coup".equals(snapshot.leadershipContestType())) {
                addLabelRow("Coup leader", snapshot.leadershipCoupLeaderName());
                String approvals = snapshot.leadershipCoupRequiredApprovals() <= 0 ? "-"
                        : snapshot.leadershipCoupApprovalCount() + "/" + snapshot.leadershipCoupRequiredApprovals();
                addLabelRow("Approvals", approvals);
                addRowWithButtons(
                        "Coup actions",
                        "Approve coup or elect.",
                        makeActionButton("Approve Coup", ACTION_APPROVE_COUP, 88),
                        makeActionButton("Start Election", ACTION_START_ELECTION, 90));
            }
        } else {
            addLabelRow("Contest", "None");
            addLabelRow("", "Start an election or coup to change leadership.", 0xFF78909C);
            addRowWithButtons(
                    "Contest actions",
                    "Start leadership process.",
                    makeActionButton("Start Election", ACTION_START_ELECTION, 90),
                    makeActionButton("Start Coup", ACTION_START_COUP, 78));
        }
    }

    private void addAttributeSelectorButtons(AttributeCategory category, String active) {
        for (CivicAttribute attr : CivicAttribute.values()) {
            if (attr.category() != category) {
                continue;
            }
            boolean isActive = attr.serializedName().equals(active);
            String label = (isActive ? "[x] " : "[  ] ") + attr.displayName();
            int color = isActive ? 0xFFA5D6A7 : 0xFF9BA9B7;
            int attrOrdinal = attr.ordinal();
            addSelectableRow(label, color, btn -> sendAction(ACTION_SET_ATTRIBUTE + attrOrdinal));
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
        addIdentitySection(snapshot.civilizationName(), snapshot.playerRole(), snapshot.canManageGovernance());
        addSection("Role Administration", 0xFFB0BEC5);
        addLabelRow("Custom roles", String.valueOf(snapshot.customRoleCount()));
        addLabelRow("Managers", String.valueOf(snapshot.managerCount()));
        addLabelRow("Leader title", snapshot.leaderTitle());
        addLabelRow("Resource policy", displayAttribute(snapshot.resourceAttribute()));
        addLabelRow("Friendly PvP", snapshot.allowIntraCivPvp() ? "Enabled" : "Disabled");
        addSpacer(4);

        if (snapshot.canManageGovernance()) {
            addSection("Role Actions", 0xFFC6D2DE);
            addRowWithButtons(
                    "Role tools",
                    "Open role manager.",
                    makeActionButton("Manage Roles", ACTION_ROLES_MANAGE, 102));
            addLabelRow("", "Advanced path: /realciv civ role ...", 0xFF78909C);
            addSpacer(4);
            addSection("Available permissions for roles", 0xFFC6D2DE);
            for (String permissionKey : CivSavedData.knownRolePermissions()) {
                addLabelRow("  " + permissionKey, permissionSummaryLabel(permissionKey), 0xFF9BA9B7);
            }
            addSpacer(4);
            addLabelRow("", "Use /realciv civ role commands to rename, grant permissions, and assign members.", 0xFF78909C);
        } else {
            addLabelRow("", "Role management is available to leadership.", 0xFF78909C);
        }
    }

    private String permissionSummaryLabel(String permissionKey) {
        return switch (permissionKey) {
            case CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE -> "Governance and role configuration";
            case CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY -> "Diplomacy and war actions";
            case CivSavedData.ROLE_PERMISSION_MANAGE_FRIENDLY_FIRE -> "Intra-civ PvP toggle";
            case CivSavedData.ROLE_PERMISSION_MANAGE_PROFESSION_FOCUS -> "Member profession focus assignment";
            case CivSavedData.ROLE_PERMISSION_MANAGE_EXPLOSIVES -> "Explosives specialist controls";
            case CivSavedData.ROLE_PERMISSION_MANAGE_REDSTONERS -> "Redstoner assignment controls";
            case CivSavedData.ROLE_PERMISSION_MANAGE_TOWN_CLAIMS -> "CIVIC claim and unclaim";
            case CivSavedData.ROLE_PERMISSION_MANAGE_LAND_ZONING -> "Land zoning and private unclaim policy";
            case CivSavedData.ROLE_PERMISSION_MANAGE_LAND_MANAGERS -> "Civic manager assignment";
            case CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE -> "FTB map claim-mode controls";
            case CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS -> "Membership/invite/request management";
            case CivSavedData.ROLE_PERMISSION_POLICE_MEMBERS -> "Member discipline actions";
            case CivSavedData.ROLE_PERMISSION_MANAGE_CENSUS_ROLES -> "Census role policy controls";
            case CivSavedData.ROLE_PERMISSION_MANAGE_LEADERSHIP -> "Leadership assignment actions";
            case CivSavedData.ROLE_PERMISSION_MANAGE_WITHDRAW_RATES -> "Hub withdrawal-rate overrides";
            case CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION -> "Hub allowance distribution policy";
            case CivSavedData.ROLE_PERMISSION_MANAGE_HUB_WITHDRAWALS -> "Hub withdrawal execution controls";
            case CivSavedData.ROLE_PERMISSION_VIEW_HUB_LOGS -> "View hub audit logs";
            case CivSavedData.ROLE_PERMISSION_VIEW_HUB_QUOTAS -> "View hub quota details";
            case CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP -> "Tax and upkeep management";
            default -> permissionKey.replace('_', ' ');
        };
    }

    private String displayAttribute(String serializedName) {
        CivicAttribute attr = CivicAttribute.fromSerializedName(serializedName);
        return attr == null ? fallbackText(serializedName, "-") : attr.displayName();
    }

    private int parseNonNegativeInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String resolveItemDisplayName(String itemIdRaw) {
        try {
            ResourceLocation itemId = ResourceLocation.parse(itemIdRaw);
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) {
                return itemIdRaw;
            }
            return item.getDescription().getString();
        } catch (Exception ignored) {
            return itemIdRaw;
        }
    }

    private boolean hasAnyManagementAccess() {
        return snapshot.canManageGovernance()
                || snapshot.canManageCensus()
                || snapshot.canManageHubDistribution();
    }

    private boolean isRationedPolicy() {
        CivicAttribute attr = CivicAttribute.fromSerializedName(snapshot.resourceAttribute());
        return attr == CivicAttribute.RATIONED;
    }

    private Widget makeActionButton(String label, int actionId, int width) {
        Widget btn = makeInlineBtn(actionId, label, width);
        btn.setPos(0, 0);
        return btn;
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
