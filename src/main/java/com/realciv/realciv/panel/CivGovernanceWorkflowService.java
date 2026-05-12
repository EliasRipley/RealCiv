package com.realciv.realciv.panel;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class CivGovernanceWorkflowService {
    private static final long PROPOSAL_TTL_MILLIS = 30L * 60L * 1000L;
    private static final Map<String, PendingProposal> PENDING = new HashMap<>();

    private CivGovernanceWorkflowService() {
    }

    public static synchronized Decision requestAction(ServerPlayer actor, CivSavedData data, String civId, PanelAction action) {
        if (!RealCivConfig.governanceApprovalWorkflowEnabled()) {
            return Decision.applyNow("Approval workflow disabled; change applied immediately.", action);
        }
        if (actor.hasPermissions(3) || data.isMayor(civId, actor.getUUID())) {
            return Decision.applyNow("Leader/admin override; change applied immediately.", action);
        }

        CivSavedData.GovernanceModel model = data.governanceModel(civId);
        if (model == CivSavedData.GovernanceModel.AUTOCRATIC) {
            return Decision.applyNow("Autocratic governance keeps direct leadership execution.", action);
        }

        if (!isEligibleVoter(model, data, civId, actor.getUUID())) {
            return Decision.denied("You are not eligible to propose policy changes under current governance rules.");
        }

        clearExpired(civId);
        PendingProposal existing = PENDING.get(civId);
        if (existing != null) {
            if (!existing.action.matches(action)) {
                return Decision.denied("Another policy proposal is already pending. Resolve it first.");
            }
            existing.voteYes(actor.getUUID());
            return evaluate(existing);
        }

        int requiredYes = requiredYesVotes(model, data, civId);
        PendingProposal proposal = new PendingProposal(
                civId,
                action,
                actor.getUUID(),
                model,
                requiredYes,
                System.currentTimeMillis() + PROPOSAL_TTL_MILLIS);
        proposal.voteYes(actor.getUUID());
        PENDING.put(civId, proposal);
        return evaluate(proposal);
    }

    public static synchronized Decision vote(ServerPlayer actor, CivSavedData data, String civId, boolean yes) {
        clearExpired(civId);
        PendingProposal proposal = PENDING.get(civId);
        if (proposal == null) {
            return Decision.denied("No pending policy proposal for your civilization.");
        }
        if (!isEligibleVoter(proposal.model, data, civId, actor.getUUID())) {
            return Decision.denied("You are not eligible to vote on this proposal.");
        }
        if (yes) {
            proposal.voteYes(actor.getUUID());
        } else {
            proposal.voteNo(actor.getUUID());
        }
        return evaluate(proposal);
    }

    public static synchronized @Nullable ProposalView proposalView(CivSavedData data, String civId) {
        clearExpired(civId);
        PendingProposal proposal = PENDING.get(civId);
        if (proposal == null) {
            return null;
        }
        int eligibleVoters = eligibleVoterCount(proposal.model, data, civId);
        return new ProposalView(
                proposal.action.summary,
                proposal.model.serializedName(),
                proposal.requiredYes,
                proposal.yesVotes.size(),
                proposal.noVotes.size(),
                Math.max(0, eligibleVoters),
                proposal.expiresAtMillis);
    }

    private static Decision evaluate(PendingProposal proposal) {
        if (GovernanceMath.quorumReached(proposal.yesVotes.size(), proposal.requiredYes)) {
            PENDING.remove(proposal.civId);
            return Decision.approved("Proposal reached quorum and is now applied.", proposal.action);
        }
        return Decision.proposed(
                "Proposal pending: " + proposal.yesVotes.size() + "/" + proposal.requiredYes + " yes vote(s).");
    }

    private static boolean isEligibleVoter(
            CivSavedData.GovernanceModel model,
            CivSavedData data,
            String civId,
            UUID playerId) {
        return switch (model) {
            case AUTOCRATIC -> data.isMayor(civId, playerId);
            case COUNCIL -> data.isMayor(civId, playerId)
                    || data.isCivicManager(civId, playerId)
                    || data.hasCustomRolePermission(civId, playerId, CivSavedData.ROLE_PERMISSION_MANAGE_GOVERNANCE);
            case DEMOCRATIC -> data.isCivilizationMember(civId, playerId);
        };
    }

    private static int requiredYesVotes(CivSavedData.GovernanceModel model, CivSavedData data, String civId) {
        int eligible = eligibleVoterCount(model, data, civId);
        return GovernanceMath.requiredYesVotes(model.serializedName(), eligible);
    }

    private static int eligibleVoterCount(CivSavedData.GovernanceModel model, CivSavedData data, String civId) {
        List<UUID> members = data.civilizationMembersSorted(civId);
        if (members.isEmpty()) {
            return 0;
        }
        int eligible = 0;
        for (UUID memberId : members) {
            if (isEligibleVoter(model, data, civId, memberId)) {
                eligible++;
            }
        }
        return eligible;
    }

    private static void clearExpired(String civId) {
        PendingProposal proposal = PENDING.get(civId);
        if (proposal == null) {
            return;
        }
        if (System.currentTimeMillis() > proposal.expiresAtMillis) {
            PENDING.remove(civId);
        }
    }

    public record PanelAction(
            String type,
            String payload,
            String summary,
            String permissionKey) {
        private boolean matches(PanelAction other) {
            return type.equals(other.type) && payload.equals(other.payload) && permissionKey.equals(other.permissionKey);
        }
    }

    public record Decision(
            Result result,
            String message,
            @Nullable PanelAction actionToApply) {
        public static Decision applyNow(String message, PanelAction action) {
            return new Decision(Result.APPLY_NOW, message, action);
        }

        public static Decision approved(String message, PanelAction action) {
            return new Decision(Result.APPROVED_APPLY, message, action);
        }

        public static Decision proposed(String message) {
            return new Decision(Result.PROPOSED_PENDING, message, null);
        }

        public static Decision denied(String message) {
            return new Decision(Result.DENIED, message, null);
        }
    }

    public enum Result {
        APPLY_NOW,
        APPROVED_APPLY,
        PROPOSED_PENDING,
        DENIED
    }

    public record ProposalView(
            String summary,
            String governanceModel,
            int requiredYesVotes,
            int yesVotes,
            int noVotes,
            int eligibleVoters,
            long expiresAtMillis) {
    }

    private static final class PendingProposal {
        private final String civId;
        private final PanelAction action;
        private final UUID proposerId;
        private final CivSavedData.GovernanceModel model;
        private final int requiredYes;
        private final long expiresAtMillis;
        private final Set<UUID> yesVotes = new HashSet<>();
        private final Set<UUID> noVotes = new HashSet<>();

        private PendingProposal(
                String civId,
                PanelAction action,
                UUID proposerId,
                CivSavedData.GovernanceModel model,
                int requiredYes,
                long expiresAtMillis) {
            this.civId = civId;
            this.action = action;
            this.proposerId = proposerId;
            this.model = model;
            this.requiredYes = requiredYes;
            this.expiresAtMillis = expiresAtMillis;
        }

        private void voteYes(UUID voter) {
            noVotes.remove(voter);
            yesVotes.add(voter);
        }

        private void voteNo(UUID voter) {
            yesVotes.remove(voter);
            noVotes.add(voter);
        }
    }
}
