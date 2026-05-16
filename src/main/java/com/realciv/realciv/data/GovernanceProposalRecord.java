package com.realciv.realciv.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class GovernanceProposalRecord {
    private final String actionType;
    private final String payload;
    private final String summary;
    private final String permissionKey;
    private final CivicAttribute executiveAttribute;
    @Nullable
    private final UUID proposerId;
    private final int requiredYesVotes;
    private final long expiresAtMillis;
    private final Set<UUID> yesVotes = new HashSet<>();
    private final Set<UUID> noVotes = new HashSet<>();

    public GovernanceProposalRecord(
            String actionType,
            String payload,
            String summary,
            String permissionKey,
            CivicAttribute executiveAttribute,
            @Nullable UUID proposerId,
            int requiredYesVotes,
            long expiresAtMillis) {
        this.actionType = actionType;
        this.payload = payload;
        this.summary = summary;
        this.permissionKey = permissionKey;
        this.executiveAttribute = executiveAttribute != null && executiveAttribute.category() == AttributeCategory.EXECUTIVE
                ? executiveAttribute
                : CivicAttribute.DIRECT_RULE;
        this.proposerId = proposerId;
        this.requiredYesVotes = Math.max(1, requiredYesVotes);
        this.expiresAtMillis = expiresAtMillis;
    }

    public String actionType() {
        return actionType;
    }

    public String payload() {
        return payload;
    }

    public String summary() {
        return summary;
    }

    public String permissionKey() {
        return permissionKey;
    }

    public CivicAttribute executiveAttribute() {
        return executiveAttribute;
    }

    @Nullable
    public UUID proposerId() {
        return proposerId;
    }

    public int requiredYesVotes() {
        return requiredYesVotes;
    }

    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    public Set<UUID> yesVotes() {
        return Collections.unmodifiableSet(yesVotes);
    }

    public Set<UUID> noVotes() {
        return Collections.unmodifiableSet(noVotes);
    }

    public void voteYes(UUID playerId) {
        noVotes.remove(playerId);
        yesVotes.add(playerId);
    }

    public void voteNo(UUID playerId) {
        yesVotes.remove(playerId);
        noVotes.add(playerId);
    }

    public boolean matchesAction(String type, String actionPayload, String permission) {
        return actionType.equals(type)
                && payload.equals(actionPayload)
                && permissionKey.equals(permission);
    }

    public GovernanceProposalRecord copy() {
        GovernanceProposalRecord copy = new GovernanceProposalRecord(
                actionType,
                payload,
                summary,
                permissionKey,
                executiveAttribute,
                proposerId,
                requiredYesVotes,
                expiresAtMillis);
        copy.yesVotes.addAll(yesVotes);
        copy.noVotes.addAll(noVotes);
        return copy;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("actionType", actionType);
        tag.putString("payload", payload);
        tag.putString("summary", summary);
        tag.putString("permissionKey", permissionKey);
        tag.putString("executiveAttribute", executiveAttribute.serializedName());
        if (proposerId != null) {
            tag.putString("proposerId", proposerId.toString());
        }
        tag.putInt("requiredYesVotes", Math.max(1, requiredYesVotes));
        tag.putLong("expiresAtMillis", expiresAtMillis);
        ListTag yesTags = new ListTag();
        for (UUID id : yesVotes) {
            yesTags.add(StringTag.valueOf(id.toString()));
        }
        tag.put("yesVotes", yesTags);
        ListTag noTags = new ListTag();
        for (UUID id : noVotes) {
            noTags.add(StringTag.valueOf(id.toString()));
        }
        tag.put("noVotes", noTags);
        return tag;
    }

    @Nullable
    public static GovernanceProposalRecord load(CompoundTag tag) {
        if (!tag.contains("actionType", Tag.TAG_STRING)
                || !tag.contains("payload", Tag.TAG_STRING)
                || !tag.contains("permissionKey", Tag.TAG_STRING)) {
            return null;
        }
        @Nullable CivicAttribute execAttr = null;
        // Try new key first, then fall back to old governanceModel key
        if (tag.contains("executiveAttribute", Tag.TAG_STRING)) {
            execAttr = CivicAttribute.fromSerializedName(tag.getString("executiveAttribute"));
        } else if (tag.contains("governanceModel", Tag.TAG_STRING)) {
            String raw = tag.getString("governanceModel").trim().toUpperCase(Locale.ROOT);
            execAttr = switch (raw) {
                case "AUTOCRATIC", "AUTOCRACY", "AUTO" -> CivicAttribute.DIRECT_RULE;
                case "COUNCIL", "OLIGARCHY" -> CivicAttribute.COUNCIL_VOTE;
                case "DEMOCRATIC", "DEMOCRACY", "DEMO" -> CivicAttribute.POPULAR_VOTE;
                default -> null;
            };
        }
        if (execAttr == null) {
            return null;
        }
        String summary = tag.contains("summary", Tag.TAG_STRING)
                ? tag.getString("summary")
                : tag.getString("actionType");
        @Nullable UUID proposerId = null;
        if (tag.contains("proposerId", Tag.TAG_STRING)) {
            try {
                proposerId = UUID.fromString(tag.getString("proposerId"));
            } catch (Exception ignored) {
            }
        }
        GovernanceProposalRecord out = new GovernanceProposalRecord(
                tag.getString("actionType"),
                tag.getString("payload"),
                summary,
                tag.getString("permissionKey"),
                execAttr,
                proposerId,
                Math.max(1, tag.getInt("requiredYesVotes")),
                tag.getLong("expiresAtMillis"));
        ListTag yesTags = tag.getList("yesVotes", Tag.TAG_STRING);
        for (Tag entry : yesTags) {
            try {
                out.yesVotes.add(UUID.fromString(entry.getAsString()));
            } catch (Exception ignored) {
            }
        }
        ListTag noTags = tag.getList("noVotes", Tag.TAG_STRING);
        for (Tag entry : noTags) {
            try {
                out.noVotes.add(UUID.fromString(entry.getAsString()));
            } catch (Exception ignored) {
            }
        }
        return out;
    }
}
