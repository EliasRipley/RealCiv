package com.realciv.realciv.diplomacy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record DiplomacySnapshot(
        String civilizationId,
        String civDisplayName,
        boolean canManage,
        int page,
        int totalPages,
        String draftWarType,
        int draftPvpKillTarget,
        boolean draftWarOfSubmission,
        boolean draftWarOfLand,
        List<IncomingWarRequest> incomingWarRequests,
        List<RelationRow> relations) {

    private static final int MAX_STR = 128;

    public record RelationRow(String otherCivId, String otherCivName, String state, int ourCasualties, int theirCasualties) {
    }

    public record IncomingWarRequest(
            String requesterCivId,
            String requesterCivName,
            String warType,
            int pvpKillTarget,
            boolean warOfSubmission,
            boolean warOfLand) {
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(civilizationId, MAX_STR);
        buf.writeUtf(civDisplayName, MAX_STR);
        buf.writeBoolean(canManage);
        buf.writeInt(page);
        buf.writeInt(totalPages);
        buf.writeUtf(draftWarType, MAX_STR);
        buf.writeInt(draftPvpKillTarget);
        buf.writeBoolean(draftWarOfSubmission);
        buf.writeBoolean(draftWarOfLand);
        buf.writeInt(incomingWarRequests.size());
        for (IncomingWarRequest request : incomingWarRequests) {
            buf.writeUtf(request.requesterCivId(), MAX_STR);
            buf.writeUtf(request.requesterCivName(), MAX_STR);
            buf.writeUtf(request.warType(), MAX_STR);
            buf.writeInt(request.pvpKillTarget());
            buf.writeBoolean(request.warOfSubmission());
            buf.writeBoolean(request.warOfLand());
        }
        buf.writeInt(relations.size());
        for (RelationRow r : relations) {
            buf.writeUtf(r.otherCivId(), MAX_STR);
            buf.writeUtf(r.otherCivName(), MAX_STR);
            buf.writeUtf(r.state(), MAX_STR);
            buf.writeInt(r.ourCasualties());
            buf.writeInt(r.theirCasualties());
        }
    }

    public static DiplomacySnapshot read(RegistryFriendlyByteBuf buf) {
        String civId = buf.readUtf(MAX_STR);
        String civName = buf.readUtf(MAX_STR);
        boolean manage = buf.readBoolean();
        int page = buf.readInt();
        int pages = buf.readInt();
        String draftWarType = buf.readUtf(MAX_STR);
        int draftPvpKillTarget = buf.readInt();
        boolean draftWarOfSubmission = buf.readBoolean();
        boolean draftWarOfLand = buf.readBoolean();
        int incomingCount = buf.readInt();
        List<IncomingWarRequest> incomingWarRequests = new ArrayList<>(incomingCount);
        for (int i = 0; i < incomingCount; i++) {
            incomingWarRequests.add(new IncomingWarRequest(
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean()));
        }
        int count = buf.readInt();
        List<RelationRow> relations = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            relations.add(new RelationRow(
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt()));
        }
        return new DiplomacySnapshot(
                civId,
                civName,
                manage,
                page,
                pages,
                draftWarType,
                draftPvpKillTarget,
                draftWarOfSubmission,
                draftWarOfLand,
                incomingWarRequests,
                relations);
    }
}
