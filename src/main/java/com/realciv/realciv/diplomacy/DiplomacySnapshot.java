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
        List<RelationRow> relations) {

    private static final int MAX_STR = 128;

    public record RelationRow(String otherCivId, String otherCivName, String state, int ourCasualties, int theirCasualties) {
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(civilizationId, MAX_STR);
        buf.writeUtf(civDisplayName, MAX_STR);
        buf.writeBoolean(canManage);
        buf.writeInt(page);
        buf.writeInt(totalPages);
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
        return new DiplomacySnapshot(civId, civName, manage, page, pages, relations);
    }
}
