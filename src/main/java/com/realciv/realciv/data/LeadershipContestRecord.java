package com.realciv.realciv.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class LeadershipContestRecord {
    private final LeadershipContestType contestType;
    @Nullable
    private final UUID initiatorId;
    @Nullable
    private final UUID coupLeaderId;
    private final long startedAtMillis;
    private final long expiresAtMillis;
    private final Set<UUID> candidates = new HashSet<>();
    private final Map<UUID, UUID> electionVotes = new HashMap<>();
    private final Set<UUID> coupApprovals = new HashSet<>();

    private LeadershipContestRecord(
            LeadershipContestType contestType,
            @Nullable UUID initiatorId,
            @Nullable UUID coupLeaderId,
            long startedAtMillis,
            long expiresAtMillis) {
        this.contestType = contestType == null ? LeadershipContestType.ELECTION : contestType;
        this.initiatorId = initiatorId;
        this.coupLeaderId = coupLeaderId;
        this.startedAtMillis = startedAtMillis;
        this.expiresAtMillis = expiresAtMillis;
    }

    public static LeadershipContestRecord newElection(
            @Nullable UUID initiatorId,
            long startedAtMillis,
            long expiresAtMillis) {
        return new LeadershipContestRecord(
                LeadershipContestType.ELECTION,
                initiatorId,
                null,
                startedAtMillis,
                expiresAtMillis);
    }

    public static LeadershipContestRecord newCoup(
            @Nullable UUID initiatorId,
            @Nullable UUID coupLeaderId,
            long startedAtMillis,
            long expiresAtMillis) {
        return new LeadershipContestRecord(
                LeadershipContestType.COUP,
                initiatorId,
                coupLeaderId,
                startedAtMillis,
                expiresAtMillis);
    }

    public LeadershipContestType contestType() {
        return contestType;
    }

    @Nullable
    public UUID initiatorId() {
        return initiatorId;
    }

    @Nullable
    public UUID coupLeaderId() {
        return coupLeaderId;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    public Set<UUID> candidates() {
        return candidates;
    }

    public Map<UUID, UUID> electionVotes() {
        return electionVotes;
    }

    public Set<UUID> coupApprovals() {
        return coupApprovals;
    }

    public LeadershipContestRecord copy() {
        LeadershipContestRecord copy = new LeadershipContestRecord(
                contestType,
                initiatorId,
                coupLeaderId,
                startedAtMillis,
                expiresAtMillis);
        copy.candidates.addAll(candidates);
        copy.electionVotes.putAll(electionVotes);
        copy.coupApprovals.addAll(coupApprovals);
        return copy;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("contestType", contestType.serializedName());
        if (initiatorId != null) {
            tag.putString("initiatorId", initiatorId.toString());
        }
        if (coupLeaderId != null) {
            tag.putString("coupLeaderId", coupLeaderId.toString());
        }
        tag.putLong("startedAtMillis", startedAtMillis);
        tag.putLong("expiresAtMillis", expiresAtMillis);

        ListTag candidateTags = new ListTag();
        for (UUID candidateId : candidates) {
            candidateTags.add(StringTag.valueOf(candidateId.toString()));
        }
        tag.put("candidates", candidateTags);

        ListTag voteTags = new ListTag();
        for (Map.Entry<UUID, UUID> vote : electionVotes.entrySet()) {
            CompoundTag voteTag = new CompoundTag();
            voteTag.putString("voterId", vote.getKey().toString());
            voteTag.putString("candidateId", vote.getValue().toString());
            voteTags.add(voteTag);
        }
        tag.put("electionVotes", voteTags);

        ListTag approvalTags = new ListTag();
        for (UUID voterId : coupApprovals) {
            approvalTags.add(StringTag.valueOf(voterId.toString()));
        }
        tag.put("coupApprovals", approvalTags);
        return tag;
    }

    @Nullable
    public static LeadershipContestRecord load(CompoundTag tag) {
        if (!tag.contains("contestType", Tag.TAG_STRING)) {
            return null;
        }
        @Nullable LeadershipContestType type = LeadershipContestType.fromSerializedName(tag.getString("contestType"));
        if (type == null) {
            return null;
        }

        @Nullable UUID initiator = null;
        if (tag.contains("initiatorId", Tag.TAG_STRING)) {
            try {
                initiator = UUID.fromString(tag.getString("initiatorId"));
            } catch (Exception ignored) {
            }
        }

        @Nullable UUID coupLeader = null;
        if (tag.contains("coupLeaderId", Tag.TAG_STRING)) {
            try {
                coupLeader = UUID.fromString(tag.getString("coupLeaderId"));
            } catch (Exception ignored) {
            }
        }

        LeadershipContestRecord out = new LeadershipContestRecord(
                type,
                initiator,
                coupLeader,
                tag.getLong("startedAtMillis"),
                tag.getLong("expiresAtMillis"));

        ListTag candidateTags = tag.getList("candidates", Tag.TAG_STRING);
        for (Tag entry : candidateTags) {
            try {
                out.candidates.add(UUID.fromString(entry.getAsString()));
            } catch (Exception ignored) {
            }
        }

        ListTag voteTags = tag.getList("electionVotes", Tag.TAG_COMPOUND);
        for (Tag entry : voteTags) {
            if (!(entry instanceof CompoundTag voteTag)) {
                continue;
            }
            if (!voteTag.contains("voterId", Tag.TAG_STRING) || !voteTag.contains("candidateId", Tag.TAG_STRING)) {
                continue;
            }
            try {
                UUID voterId = UUID.fromString(voteTag.getString("voterId"));
                UUID candidateId = UUID.fromString(voteTag.getString("candidateId"));
                out.electionVotes.put(voterId, candidateId);
            } catch (Exception ignored) {
            }
        }

        ListTag approvalTags = tag.getList("coupApprovals", Tag.TAG_STRING);
        for (Tag entry : approvalTags) {
            try {
                out.coupApprovals.add(UUID.fromString(entry.getAsString()));
            } catch (Exception ignored) {
            }
        }
        return out;
    }
}
