package com.realciv.realciv.tax;

import com.realciv.realciv.data.CivilizationRecord;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class TaxSnapshotBuilder {
    private TaxSnapshotBuilder() {}

    public static TaxSnapshot build(ServerPlayer player, CivSavedData data, String civId) {
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());
        @Nullable CivilizationRecord civ = data.getCivilization(civId);
        String civName = civ == null ? civId : civ.displayName();
        boolean canManage = CivPermissionService.hasCivPermission(
                player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP);

        int ownedPlots = data.privatePlotCountForOwner(civId, player.getUUID());
        int delinquentPlots = data.delinquentPrivatePlotCountForOwner(civId, player.getUUID());
        long nextTick = data.earliestPrivatePlotUpkeepTick(civId, player.getUUID());
        long cycleCost = data.upkeepCostPerPlotCents(civId) * ownedPlots;
        long cycleItemCost = data.taxItemCostPerPlotCurrentRate(civId) * ownedPlots;
        long karma = record.socialCreditCents(civId);
        long treasury = data.civTreasuryCents(civId);
        double rate = data.upkeepRateMultiplier(civId);
        String mode = data.taxPaymentMode(civId).serializedName();
        String itemId = data.taxItemId(civId).toString();
        int itemCount = data.taxItemCountPerPlot(civId);
        long itemRate = data.taxItemCostPerPlotCurrentRate(civId);

        String role = data.isMayor(civId, player.getUUID()) ? data.leaderTitle(civId)
                : data.isCivicManager(civId, player.getUUID()) ? "Civic Manager" : "Citizen";

        List<UUID> members = data.civilizationMembersSorted(civId);
        int totalPages = Math.max(1, (members.size() + 26) / 27);
        int page = 0;

        int start = page * 27;
        int end = Math.min(members.size(), start + 27);
        List<TaxSnapshot.MemberRow> rows = new ArrayList<>(end - start);
        for (int idx = start; idx < end; idx++) {
            UUID mid = members.get(idx);
            int mp = data.privatePlotCountForOwner(civId, mid);
            int md = data.delinquentPrivatePlotCountForOwner(civId, mid);
            long mk = data.getOrCreatePlayer(mid).socialCreditCents(civId);
            boolean leader = data.isMayor(civId, mid);
            rows.add(new TaxSnapshot.MemberRow(RealCivUtil.playerDisplayName(player, mid), mp, md, mk, leader));
        }

        return new TaxSnapshot(civId, civName, player.getGameProfile().getName(), role,
                ownedPlots, delinquentPlots, nextTick, cycleCost, cycleItemCost,
                karma, treasury, rate, mode, itemId, itemCount, itemRate,
                canManage, page, totalPages, rows);
    }
}
