package com.realciv.realciv.hub;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record HubRationSnapshot(
        String civilizationId,
        String civDisplayName,
        String playerName,
        boolean canManage,
        int page,
        int totalPages,
        List<RationRow> entries) {

    private static final int MAX_STR = 128;

    public record RationRow(String itemId, String itemName, long stockAvailable, int dailyAllowance) {
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(civilizationId, MAX_STR);
        buf.writeUtf(civDisplayName, MAX_STR);
        buf.writeUtf(playerName, MAX_STR);
        buf.writeBoolean(canManage);
        buf.writeInt(page);
        buf.writeInt(totalPages);
        buf.writeInt(entries.size());
        for (RationRow row : entries) {
            buf.writeUtf(row.itemId(), MAX_STR);
            buf.writeUtf(row.itemName(), MAX_STR);
            buf.writeLong(row.stockAvailable());
            buf.writeInt(row.dailyAllowance());
        }
    }

    public static HubRationSnapshot read(RegistryFriendlyByteBuf buf) {
        String civId = buf.readUtf(MAX_STR);
        String civName = buf.readUtf(MAX_STR);
        String player = buf.readUtf(MAX_STR);
        boolean manage = buf.readBoolean();
        int page = buf.readInt();
        int totalPages = buf.readInt();
        int count = buf.readInt();
        List<RationRow> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(new RationRow(
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readLong(),
                    buf.readInt()));
        }
        return new HubRationSnapshot(civId, civName, player, manage, page, totalPages, rows);
    }
}

