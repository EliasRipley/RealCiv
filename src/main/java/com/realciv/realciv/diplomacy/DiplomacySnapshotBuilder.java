package com.realciv.realciv.diplomacy;

import com.realciv.realciv.data.*;
import com.realciv.realciv.logic.CivPermissionService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class DiplomacySnapshotBuilder {
    private static final int ROWS_PER_PAGE = 27;

    private DiplomacySnapshotBuilder() {}

    public static DiplomacySnapshot build(ServerPlayer player, CivSavedData data, String civId, int page) {
        return build(
                player,
                data,
                civId,
                page,
                com.realciv.realciv.data.WarType.DESTRUCTION.displayName(),
                com.realciv.realciv.config.RealCivConfig.defaultWarPvpKillTarget(),
                false,
                false);
    }

    public static DiplomacySnapshot build(
            ServerPlayer player,
            CivSavedData data,
            String civId,
            int page,
            String draftWarType,
            int draftPvpKillTarget,
            boolean draftWarOfSubmission,
            boolean draftWarOfLand) {
        boolean canManage = CivPermissionService.hasCivPermission(
                player, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY);
        @Nullable CivilizationRecord civ = data.getCivilization(civId);
        String civName = civ == null ? civId : civ.displayName();

        List<String> others = new ArrayList<>(data.civilizationIdsSorted());
        others.removeIf(id -> id.equals(civId));
        others.removeIf(id -> id.equals(com.realciv.realciv.config.RealCivConfig.defaultCivilizationId()));

        int totalPages = Math.max(1, (others.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * ROWS_PER_PAGE;
        int end = Math.min(others.size(), start + ROWS_PER_PAGE);
        List<DiplomacySnapshot.RelationRow> rows = new ArrayList<>(end - start);
        for (int idx = start; idx < end; idx++) {
            String otherId = others.get(idx);
            String state = data.diplomacyState(civId, otherId).displayName();
            WarCasualtyView cv = data.warCasualtiesBetween(civId, otherId);
            @Nullable CivilizationRecord otherCiv = data.getCivilization(otherId);
            String otherName = otherCiv == null ? otherId : otherCiv.displayName();
            rows.add(new DiplomacySnapshot.RelationRow(otherId, otherName, state,
                    (int) cv.yourCasualties(), (int) cv.otherCasualties()));
        }

        List<DiplomacySnapshot.IncomingWarRequest> incomingWarRequests = new ArrayList<>();
        for (CivSavedData.DiplomacyRequestView request : data.incomingDiplomacyRequestsFor(civId)) {
            if (request.requestedState() != DiplomacyState.WAR) {
                continue;
            }
            String requesterId = request.requesterCivilizationId();
            @Nullable CivilizationRecord requester = data.getCivilization(requesterId);
            String requesterName = requester == null ? requesterId : requester.displayName();
            String warType = request.warType() == null ? "DESTRUCTION" : request.warType().displayName();
            incomingWarRequests.add(new DiplomacySnapshot.IncomingWarRequest(
                    requesterId,
                    requesterName,
                    warType,
                    Math.max(1, request.pvpKillTarget()),
                    request.warOfSubmission(),
                    request.warOfLand()));
        }

        return new DiplomacySnapshot(
                civId,
                civName,
                canManage,
                page,
                totalPages,
                draftWarType,
                Math.max(1, draftPvpKillTarget),
                draftWarOfSubmission,
                draftWarOfLand,
                incomingWarRequests,
                rows);
    }
}
