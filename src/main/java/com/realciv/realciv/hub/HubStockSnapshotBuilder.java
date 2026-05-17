package com.realciv.realciv.hub;

import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivilizationRecord;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.PlayerRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class HubStockSnapshotBuilder {
    public static final int ROWS_PER_PAGE = 20;

    private HubStockSnapshotBuilder() {}

    public static HubStockSnapshot build(ServerPlayer player, CivSavedData data, String civId, boolean canManage, int page) {
        @Nullable CivilizationRecord civ = data.getCivilization(civId);
        String civName = civ == null ? civId : civ.displayName();
        String distMode = data.civicAttribute(civId, AttributeCategory.RESOURCE).displayName();
        PlayerRecord record = data.getOrCreatePlayer(player.getUUID());

        List<Map.Entry<String, Long>> stockEntries = data.getHubStockEntriesSorted(civId);
        int totalPages = Math.max(1, (stockEntries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(stockEntries.size(), start + ROWS_PER_PAGE);
        List<HubStockSnapshot.StockRow> rows = new ArrayList<>(end - start);
        for (int idx = start; idx < end; idx++) {
            Map.Entry<String, Long> entry = stockEntries.get(idx);
            ResourceLocation itemId = ResourceLocation.parse(entry.getKey());
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            String itemName = item == Items.AIR ? entry.getKey() : item.getName(item.getDefaultInstance()).getString();

            long contributed = record.contributions(civId).getOrDefault(entry.getKey(), 0L);
            long allowance = data.hubDailyAllowanceLimit(civId, ResourceLocation.parse(entry.getKey()));

            rows.add(new HubStockSnapshot.StockRow(entry.getKey(), itemName,
                    entry.getValue(), contributed, allowance));
        }

        return new HubStockSnapshot(civId, civName, player.getGameProfile().getName(),
                distMode, canManage, page, totalPages, rows);
    }
}
