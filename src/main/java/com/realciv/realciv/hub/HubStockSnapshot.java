package com.realciv.realciv.hub;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record HubStockSnapshot(
        String civilizationId,
        String civDisplayName,
        String playerName,
        String distributionMode,
        boolean canManage,
        int page,
        int totalPages,
        List<StockRow> entries) {

    private static final int MAX_STR = 128;

    public record StockRow(String itemId, String itemName, long available, long playerContributed, long dailyAllowance) {
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(civilizationId, MAX_STR);
        buf.writeUtf(civDisplayName, MAX_STR);
        buf.writeUtf(playerName, MAX_STR);
        buf.writeUtf(distributionMode, MAX_STR);
        buf.writeBoolean(canManage);
        buf.writeInt(page);
        buf.writeInt(totalPages);
        buf.writeInt(entries.size());
        for (StockRow r : entries) {
            buf.writeUtf(r.itemId(), MAX_STR);
            buf.writeUtf(r.itemName(), MAX_STR);
            buf.writeLong(r.available());
            buf.writeLong(r.playerContributed());
            buf.writeLong(r.dailyAllowance());
        }
    }

    public static HubStockSnapshot read(RegistryFriendlyByteBuf buf) {
        String civId = buf.readUtf(MAX_STR);
        String civName = buf.readUtf(MAX_STR);
        String pName = buf.readUtf(MAX_STR);
        String distMode = buf.readUtf(MAX_STR);
        boolean manage = buf.readBoolean();
        int page = buf.readInt();
        int pages = buf.readInt();
        int count = buf.readInt();
        List<StockRow> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new StockRow(
                    buf.readUtf(MAX_STR),
                    buf.readUtf(MAX_STR),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong()));
        }
        return new HubStockSnapshot(civId, civName, pName, distMode, manage, page, pages, entries);
    }
}
