package com.realciv.realciv.hub;

import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.CivilizationRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class HubRationSnapshotBuilder {
    public static final int ROWS_PER_PAGE = 20;

    private HubRationSnapshotBuilder() {
    }

    public static HubRationSnapshot build(ServerPlayer player, CivSavedData data, String civId, boolean canManage, int page) {
        @Nullable CivilizationRecord civ = data.getCivilization(civId);
        String civName = civ == null ? civId : civ.displayName();

        List<Map.Entry<String, Integer>> allowanceEntries = data.hubDailyAllowanceEntriesSorted(civId);
        Map<String, Integer> allowanceByItem = new HashMap<>();
        for (Map.Entry<String, Integer> entry : allowanceEntries) {
            allowanceByItem.put(entry.getKey(), Math.max(0, entry.getValue()));
        }

        Map<String, Long> stockByItem = new HashMap<>();
        for (Map.Entry<String, Long> stockEntry : data.getHubStockEntriesSorted(civId)) {
            stockByItem.put(stockEntry.getKey(), Math.max(0L, stockEntry.getValue()));
            allowanceByItem.putIfAbsent(stockEntry.getKey(), 0);
        }

        List<Map.Entry<String, Integer>> merged = new ArrayList<>(allowanceByItem.entrySet());
        merged.sort(Map.Entry.comparingByKey());

        int totalPages = Math.max(1, (merged.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int start = safePage * ROWS_PER_PAGE;
        int end = Math.min(merged.size(), start + ROWS_PER_PAGE);

        List<HubRationSnapshot.RationRow> rows = new ArrayList<>(Math.max(0, end - start));
        for (int index = start; index < end; index++) {
            Map.Entry<String, Integer> entry = merged.get(index);
            String itemKey = entry.getKey();
            ResourceLocation itemId = ResourceLocation.parse(itemKey);
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            String itemName = item == Items.AIR ? itemKey : item.getName(item.getDefaultInstance()).getString();
            long stock = stockByItem.getOrDefault(itemKey, 0L);
            int allowance = Math.max(0, entry.getValue());
            rows.add(new HubRationSnapshot.RationRow(itemKey, itemName, stock, allowance));
        }

        return new HubRationSnapshot(
                civId,
                civName,
                player.getGameProfile().getName(),
                canManage,
                safePage,
                totalPages,
                rows);
    }
}

