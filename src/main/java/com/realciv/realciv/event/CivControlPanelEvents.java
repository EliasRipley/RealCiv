package com.realciv.realciv.event;

import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.panel.CivControlPanelSnapshotBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CivControlPanelEvents {
    private CivControlPanelEvents() {
    }

    public static void openControlPanel(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, CivSavedData data) {
        String civId = data.getOrAssignCivilization(player.getUUID());
        var snapshot = CivControlPanelSnapshotBuilder.build(player, data, civId);
        PacketDistributor.sendToPlayer(player, new com.realciv.realciv.network.RealCivPayloads.OpenControlPanelPayload(snapshot));
        player.sendSystemMessage(Component.literal(
                "Control Panel opened for " + RealCivUtil.civilizationDisplayName(data, civId) + "."));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
