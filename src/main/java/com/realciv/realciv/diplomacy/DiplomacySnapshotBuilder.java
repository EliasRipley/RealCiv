package com.realciv.realciv.diplomacy;

import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CivPermissionService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class DiplomacySnapshotBuilder {
    private static final int ROWS_PER_PAGE = 27;

    private DiplomacySnapshotBuilder() {}

    public static DiplomacySnapshot build(ServerPlayer player, CivSavedData data, String civId, int page) {
        boolean canManage = CivPermissionService.hasCivPermission(
                player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY);
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
        String civName = civ == null ? civId : civ.displayName();

        List<String> others = new ArrayList<>(data.civilizationIdsSorted());
        others.removeIf(id -> id.equals(civId));

        int totalPages = Math.max(1, (others.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(others.size(), start + ROWS_PER_PAGE);
        List<DiplomacySnapshot.RelationRow> rows = new ArrayList<>(end - start);
        for (int idx = start; idx < end; idx++) {
            String otherId = others.get(idx);
            String state = data.diplomacyState(civId, otherId).displayName();
            CivSavedData.WarCasualtyView cv = data.warCasualtiesBetween(civId, otherId);
            @Nullable CivSavedData.CivilizationRecord otherCiv = data.getCivilization(otherId);
            String otherName = otherCiv == null ? otherId : otherCiv.displayName();
            rows.add(new DiplomacySnapshot.RelationRow(otherId, otherName, state,
                    (int) cv.yourCasualties(), (int) cv.otherCasualties()));
        }

        return new DiplomacySnapshot(civId, civName, canManage, page, totalPages, rows);
    }
}
