package com.realciv.realciv.ledger;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record ProfessionLedgerSnapshot(
        String civName,
        String playerName,
        int generalLevel,
        int generalXp,
        long karmaCents,
        String topProfessionLine,
        String focusLine,
        int pendingWarriorRegistrations,
        int contributedItemTypes,
        int civMemberCount,
        List<ProfessionRow> professions) {

    public static final int MAX = 96;
    public static final int MAX_ROWS = 16;

    public record ProfessionRow(String name, int level, int xp, String actionLine) {}

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(civName, MAX);
        buffer.writeUtf(playerName, MAX);
        buffer.writeVarInt(generalLevel);
        buffer.writeVarInt(generalXp);
        buffer.writeLong(karmaCents);
        buffer.writeUtf(topProfessionLine, MAX);
        buffer.writeUtf(focusLine, MAX);
        buffer.writeVarInt(pendingWarriorRegistrations);
        buffer.writeVarInt(contributedItemTypes);
        buffer.writeVarInt(civMemberCount);
        buffer.writeVarInt(Math.min(professions.size(), MAX_ROWS));
        for (int i = 0; i < Math.min(professions.size(), MAX_ROWS); i++) {
            ProfessionRow r = professions.get(i);
            buffer.writeUtf(r.name, 48);
            buffer.writeVarInt(r.level);
            buffer.writeVarInt(r.xp);
            buffer.writeUtf(r.actionLine, MAX);
        }
    }

    public static ProfessionLedgerSnapshot read(RegistryFriendlyByteBuf buffer) {
        String civ = buffer.readUtf(MAX);
        String player = buffer.readUtf(MAX);
        int gl = buffer.readVarInt();
        int gx = buffer.readVarInt();
        long karma = buffer.readLong();
        String top = buffer.readUtf(MAX);
        String focus = buffer.readUtf(MAX);
        int pwr = buffer.readVarInt();
        int cit = buffer.readVarInt();
        int mem = buffer.readVarInt();
        int n = Math.min(MAX_ROWS, buffer.readVarInt());
        List<ProfessionRow> rows = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            rows.add(new ProfessionRow(buffer.readUtf(48), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(MAX)));
        }
        return new ProfessionLedgerSnapshot(civ, player, gl, gx, karma, top, focus, pwr, cit, mem, List.copyOf(rows));
    }
}
