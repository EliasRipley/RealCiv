package com.realciv.realciv.census;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record CensusSnapshot(
        String civDisplayName,
        boolean canManage,
        int memberPage,
        int memberPageCount,
        int totalMembers,
        int totalRequests,
        int totalInvites,
        List<MemberRow> members,
        List<PendingRow> requests,
        List<PendingRow> invites) {

    public static final int MAX_CIV_NAME = 128;
    public static final int MAX_NAME = 64;
    public static final int MAX_ROLE = 32;
    public static final int MAX_PROF = 48;
    public static final int MAX_ROWS = 12;

    public record MemberRow(String name, String role, String profession, int level, UUID id) {}

    public record PendingRow(String name, UUID id) {}

    public static CensusSnapshot empty() {
        return new CensusSnapshot("", false, 0, 1, 0, 0, 0, List.of(), List.of(), List.of());
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(civDisplayName, MAX_CIV_NAME);
        buffer.writeBoolean(canManage);
        buffer.writeVarInt(memberPage);
        buffer.writeVarInt(memberPageCount);
        buffer.writeVarInt(totalMembers);
        buffer.writeVarInt(totalRequests);
        buffer.writeVarInt(totalInvites);
        buffer.writeVarInt(Math.min(members.size(), MAX_ROWS));
        for (int i = 0; i < Math.min(members.size(), MAX_ROWS); i++) {
            MemberRow m = members.get(i);
            buffer.writeUtf(m.name, MAX_NAME);
            buffer.writeUtf(m.role, MAX_ROLE);
            buffer.writeUtf(m.profession, MAX_PROF);
            buffer.writeVarInt(m.level);
            buffer.writeUUID(m.id);
        }
        buffer.writeVarInt(Math.min(requests.size(), MAX_ROWS));
        for (int i = 0; i < Math.min(requests.size(), MAX_ROWS); i++) {
            PendingRow r = requests.get(i);
            buffer.writeUtf(r.name, MAX_NAME);
            buffer.writeUUID(r.id);
        }
        buffer.writeVarInt(Math.min(invites.size(), MAX_ROWS));
        for (int i = 0; i < Math.min(invites.size(), MAX_ROWS); i++) {
            PendingRow r = invites.get(i);
            buffer.writeUtf(r.name, MAX_NAME);
            buffer.writeUUID(r.id);
        }
    }

    public static CensusSnapshot read(RegistryFriendlyByteBuf buffer) {
        String civ = buffer.readUtf(MAX_CIV_NAME);
        boolean manage = buffer.readBoolean();
        int mPage = buffer.readVarInt();
        int mPages = buffer.readVarInt();
        int tMem = buffer.readVarInt();
        int tReq = buffer.readVarInt();
        int tInv = buffer.readVarInt();
        int mc = Math.min(MAX_ROWS, buffer.readVarInt());
        List<MemberRow> mems = new ArrayList<>(mc);
        for (int i = 0; i < mc; i++) {
            mems.add(new MemberRow(
                    buffer.readUtf(MAX_NAME),
                    buffer.readUtf(MAX_ROLE),
                    buffer.readUtf(MAX_PROF),
                    buffer.readVarInt(),
                    buffer.readUUID()));
        }
        int rc = Math.min(MAX_ROWS, buffer.readVarInt());
        List<PendingRow> reqs = new ArrayList<>(rc);
        for (int i = 0; i < rc; i++) {
            reqs.add(new PendingRow(buffer.readUtf(MAX_NAME), buffer.readUUID()));
        }
        int ic = Math.min(MAX_ROWS, buffer.readVarInt());
        List<PendingRow> invs = new ArrayList<>(ic);
        for (int i = 0; i < ic; i++) {
            invs.add(new PendingRow(buffer.readUtf(MAX_NAME), buffer.readUUID()));
        }
        return new CensusSnapshot(civ, manage, mPage, mPages, tMem, tReq, tInv, List.copyOf(mems), List.copyOf(reqs), List.copyOf(invs));
    }
}
