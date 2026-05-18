package com.realciv.realciv.mixin;

import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.integration.RealCivFTBChunksMirror;
import com.realciv.realciv.logic.CivPermissionService;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.AbstractTeam;
import dev.ftb.mods.ftbteams.net.UpdatePropertiesRequestMessage;
import dev.ftb.mods.ftbteams.net.UpdatePropertiesResponseMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UpdatePropertiesRequestMessage.class)
public abstract class UpdatePropertiesRequestMessageMixin {
    private static final String SETTINGS_LOCKED_TRANSLATION_KEY = "realciv.ftbteams.settings_locked";

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void realciv$enforceRealCivSettingsLock(
            UpdatePropertiesRequestMessage message,
            NetworkManager.PacketContext context,
            CallbackInfo ci) {
        ci.cancel();

        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player) || player.server == null) {
                return;
            }

            if (FTBTeamsAPI.api() == null || !FTBTeamsAPI.api().isManagerLoaded()) {
                return;
            }

            FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
                if (!(team instanceof AbstractTeam abstractTeam)) {
                    return;
                }

                CivSavedData data = CivSavedData.get(player.server);
                @Nullable String civId = RealCivFTBChunksMirror.findCivilizationIdForTeam(data, team);
                boolean isRealCivCivTeam = civId != null;
                boolean canManageCivFtbSettings = isRealCivCivTeam
                        && CivPermissionService.hasCivPermission(
                                player,
                                data,
                                civId,
                                CivSavedData.ROLE_PERMISSION_MANAGE_FTB_MODE);

                if (isRealCivCivTeam && !canManageCivFtbSettings) {
                    player.displayClientMessage(Component.translatable(SETTINGS_LOCKED_TRANSLATION_KEY), false);
                    return;
                }

                if (abstractTeam.isOfficerOrBetter(player.getUUID()) || canManageCivFtbSettings) {
                    abstractTeam.updatePropertiesFrom(message.properties());
                    NetworkManager.sendToPlayers(
                            player.server.getPlayerList().getPlayers(),
                            new UpdatePropertiesResponseMessage(team.getId(), abstractTeam.getProperties()));
                }
            });
        });
    }
}
