package com.realciv.realciv.census;

import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class CensusSnapshotBuilder {
    public static final int MEMBERS_PER_PAGE = 10;

    private CensusSnapshotBuilder() {
    }

    public static CensusSnapshot build(
            ServerPlayer viewer,
            CivSavedData data,
            String civilizationId,
            boolean canManage,
            int memberPage) {
        List<UUID> members = data.civilizationMembersSorted(civilizationId);
        List<UUID> requests = data.joinRequestsSorted(civilizationId);
        List<UUID> invites = data.invitedPlayersSorted(civilizationId);

        int totalMemberPages = Math.max(1, (members.size() + MEMBERS_PER_PAGE - 1) / MEMBERS_PER_PAGE);
        int page = Math.max(0, Math.min(memberPage, totalMemberPages - 1));

        int start = page * MEMBERS_PER_PAGE;
        int end = Math.min(members.size(), start + MEMBERS_PER_PAGE);
        List<CensusSnapshot.MemberRow> rows = new ArrayList<>();
        for (int i = start; i < end; i++) {
            UUID id = members.get(i);
            CivSavedData.PlayerRecord pr = data.getOrCreatePlayer(id);
            var top = RealCivUtil.topProfession(pr);
            rows.add(new CensusSnapshot.MemberRow(
                    RealCivUtil.playerDisplayName(viewer, id),
                    roleLabel(data, civilizationId, id),
                    RealCivUtil.displayProfession(top),
                    pr.levelFor(top),
                    id));
        }

        List<CensusSnapshot.PendingRow> reqRows = new ArrayList<>();
        for (int i = 0; i < Math.min(CensusSnapshot.MAX_ROWS, requests.size()); i++) {
            UUID id = requests.get(i);
            reqRows.add(new CensusSnapshot.PendingRow(RealCivUtil.playerDisplayName(viewer, id), id));
        }
        List<CensusSnapshot.PendingRow> invRows = new ArrayList<>();
        for (int i = 0; i < Math.min(CensusSnapshot.MAX_ROWS, invites.size()); i++) {
            UUID id = invites.get(i);
            invRows.add(new CensusSnapshot.PendingRow(RealCivUtil.playerDisplayName(viewer, id), id));
        }

        return new CensusSnapshot(
                RealCivUtil.civilizationDisplayName(data, civilizationId),
                canManage,
                page,
                totalMemberPages,
                members.size(),
                requests.size(),
                invites.size(),
                List.copyOf(rows),
                List.copyOf(reqRows),
                List.copyOf(invRows));
    }

    private static String roleLabel(CivSavedData data, String civilizationId, UUID memberId) {
        if (data.isMayor(civilizationId, memberId)) {
            return "Mayor";
        }
        if (data.isCivicManager(civilizationId, memberId)) {
            return "Manager";
        }
        return "Citizen";
    }
}
