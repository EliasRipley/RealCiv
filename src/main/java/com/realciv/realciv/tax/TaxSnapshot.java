package com.realciv.realciv.tax;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record TaxSnapshot(
        String civilizationId,
        String civDisplayName,
        String playerName,
        String playerRole,
        int ownedPlots,
        int delinquentPlots,
        long nextUpkeepTick,
        long cycleCostCents,
        long cycleItemCost,
        long playerKarmaCents,
        long civTreasuryCents,
        double rateMultiplier,
        String paymentMode,
        String taxItemId,
        int taxItemCount,
        long taxItemPerPlotRate,
        boolean canManage,
        int memberPage,
        int totalMemberPages,
        List<MemberRow> members) {

    private static final int MAX_STR = 128;

    public record MemberRow(String name, int ownedPlots, int delinquentPlots, long karmaCents, boolean isLeader) {
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(civilizationId, MAX_STR);
        buf.writeUtf(civDisplayName, MAX_STR);
        buf.writeUtf(playerName, MAX_STR);
        buf.writeUtf(playerRole, MAX_STR);
        buf.writeInt(ownedPlots);
        buf.writeInt(delinquentPlots);
        buf.writeLong(nextUpkeepTick);
        buf.writeLong(cycleCostCents);
        buf.writeLong(cycleItemCost);
        buf.writeLong(playerKarmaCents);
        buf.writeLong(civTreasuryCents);
        buf.writeDouble(rateMultiplier);
        buf.writeUtf(paymentMode, MAX_STR);
        buf.writeUtf(taxItemId, MAX_STR);
        buf.writeInt(taxItemCount);
        buf.writeLong(taxItemPerPlotRate);
        buf.writeBoolean(canManage);
        buf.writeInt(memberPage);
        buf.writeInt(totalMemberPages);
        buf.writeInt(members.size());
        for (MemberRow m : members) {
            buf.writeUtf(m.name(), MAX_STR);
            buf.writeInt(m.ownedPlots());
            buf.writeInt(m.delinquentPlots());
            buf.writeLong(m.karmaCents());
            buf.writeBoolean(m.isLeader());
        }
    }

    public static TaxSnapshot read(RegistryFriendlyByteBuf buf) {
        String civId = buf.readUtf(MAX_STR);
        String civName = buf.readUtf(MAX_STR);
        String pName = buf.readUtf(MAX_STR);
        String pRole = buf.readUtf(MAX_STR);
        int plots = buf.readInt();
        int del = buf.readInt();
        long tick = buf.readLong();
        long cost = buf.readLong();
        long itemCost = buf.readLong();
        long karma = buf.readLong();
        long treasury = buf.readLong();
        double rate = buf.readDouble();
        String mode = buf.readUtf(MAX_STR);
        String itemId = buf.readUtf(MAX_STR);
        int itemCount = buf.readInt();
        long itemRate = buf.readLong();
        boolean manage = buf.readBoolean();
        int page = buf.readInt();
        int pages = buf.readInt();
        int mCount = buf.readInt();
        List<MemberRow> members = new ArrayList<>(mCount);
        for (int i = 0; i < mCount; i++) {
            members.add(new MemberRow(
                    buf.readUtf(MAX_STR),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readLong(),
                    buf.readBoolean()));
        }
        return new TaxSnapshot(civId, civName, pName, pRole, plots, del, tick, cost, itemCost,
                karma, treasury, rate, mode, itemId, itemCount, itemRate, manage, page, pages, members);
    }
}
