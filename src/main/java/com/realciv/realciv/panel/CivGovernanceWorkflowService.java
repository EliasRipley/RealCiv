package com.realciv.realciv.panel;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.*;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class CivGovernanceWorkflowService {
    private static final long PROPOSAL_TTL_MILLIS = 30L * 60L * 1000L;

    private CivGovernanceWorkflowService() {
    }

    public static synchronized Decision requestAction(ServerPlayer actor, CivSavedData data, String civId, PanelAction action) {
        if (!RealCivConfig.governanceApprovalWorkflowEnabled()) {
            return Decision.applyNow("Approval workflow disabled; change applied immediately.", action);
        }
        if (actor.hasPermissions(3) || data.isMayor(civId, actor.getUUID())) {
            return Decision.applyNow("Leader/admin override; change applied immediately.", action);
        }

        GovernanceModel model = data.governanceModel(civId);
        if (model == GovernanceModel.AUTOCRATIC) {
            return Decision.applyNow("Autocratic governance keeps direct leadership execution.", action);
        }

        if (!isEligibleVoter(model, data, civId, actor.getUUID())) {
            return Decision.denied("You are not eligible to propose policy changes under current governance rules.");
        }

        @Nullable GovernanceProposalRecord existing = clearExpiredAndGet(data, civId);
        if (existing != null) {
            if (!existing.matchesAction(action.type(), action.payload(), action.permissionKey())) {
                return Decision.denied("Another policy proposal is already pending. Resolve it first.");
            }
            existing.voteYes(actor.getUUID());
            return evaluateAndPersist(data, civId, existing);
        }

        int requiredYes = requiredYesVotes(model, data, civId);
        GovernanceProposalRecord proposal = new GovernanceProposalRecord(
                action.type(),
                action.payload(),
                action.summary(),
                action.permissionKey(),
                model,
                actor.getUUID(),
                requiredYes,
                System.currentTimeMillis() + PROPOSAL_TTL_MILLIS);
        proposal.voteYes(actor.getUUID());
        return evaluateAndPersist(data, civId, proposal);
    }

    public static synchronized Decision vote(ServerPlayer actor, CivSavedData data, String civId, boolean yes) {
        @Nullable GovernanceProposalRecord proposal = clearExpiredAndGet(data, civId);
        if (proposal == null) {
            return Decision.denied("No pending policy proposal for your civilization.");
        }
        if (!isEligibleVoter(proposal.governanceModel(), data, civId, actor.getUUID())) {
            return Decision.denied("You are not eligible to vote on this proposal.");
        }
        if (yes) {
            proposal.voteYes(actor.getUUID());
        } else {
            proposal.voteNo(actor.getUUID());
        }
        return evaluateAndPersist(data, civId, proposal);
    }

    public static synchronized @Nullable ProposalView proposalView(CivSavedData data, String civId) {
        @Nullable GovernanceProposalRecord proposal = clearExpiredAndGet(data, civId);
        if (proposal == null) {
            return null;
        }
        int eligibleVoters = eligibleVoterCount(proposal.governanceModel(), data, civId);
        return new ProposalView(
                proposal.summary(),
                proposal.governanceModel().serializedName(),
                proposal.requiredYesVotes(),
                proposal.yesVotes().size(),
                proposal.noVotes().size(),
                Math.max(0, eligibleVoters),
                proposal.expiresAtMillis());
    }

    private static @Nullable GovernanceProposalRecord clearExpiredAndGet(CivSavedData data, String civId) {
        @Nullable GovernanceProposalRecord proposal = data.governanceProposal(civId);
        if (proposal == null) {
            return null;
        }
        if (System.currentTimeMillis() > proposal.expiresAtMillis()) {
            data.clearGovernanceProposal(civId);
            return null;
        }
        return proposal;
    }

    private static Decision evaluateAndPersist(
            CivSavedData data,
            String civId,
            GovernanceProposalRecord proposal) {
        if (GovernanceMath.quorumReached(proposal.yesVotes().size(), proposal.requiredYesVotes())) {
            data.clearGovernanceProposal(civId);
            return Decision.approved(
                    "Proposal reached quorum and is now applied.",
                    panelActionFromProposal(proposal));
        }
        data.setGovernanceProposal(civId, proposal);
        return Decision.proposed(
                "Proposal pending: " + proposal.yesVotes().size() + "/" + proposal.requiredYesVotes() + " yes vote(s).");
    }

    private static PanelAction panelActionFromProposal(GovernanceProposalRecord proposal) {
        return new PanelAction(
                proposal.actionType(),
                proposal.payload(),
                proposal.summary(),
                proposal.permissionKey());
    }

    private static boolean isEligibleVoter(
            GovernanceModel model,
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

    private static int requiredYesVotes(GovernanceModel model, CivSavedData data, String civId) {
        int eligible = eligibleVoterCount(model, data, civId);
        return GovernanceMath.requiredYesVotes(model.serializedName(), eligible);
    }

    private static int eligibleVoterCount(GovernanceModel model, CivSavedData data, String civId) {
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

    public record PanelAction(
            String type,
            String payload,
            String summary,
            String permissionKey) {
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
}
