package com.realciv.realciv.logic;

import com.realciv.realciv.config.RealCivConfig;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RealCivMessages {
    private static final Map<UUID, DenyMessageState> LAST_DENY_MESSAGE = new ConcurrentHashMap<>();

    private RealCivMessages() {
    }

    public static void deny(ServerPlayer player, String message) {
        long now = player.serverLevel().getGameTime();
        DenyMessageState state = LAST_DENY_MESSAGE.get(player.getUUID());
        int cooldown = Math.max(1, RealCivConfig.DENY_MESSAGE_COOLDOWN_TICKS.get());
        boolean repeated = state != null && state.message().equals(message) && now - state.tick() < cooldown;

        Component text = Component.literal("[RealCiv] " + message);
        // Always send an overlay message so denials remain visible during repeated attempts.
        player.sendSystemMessage(text, true);

        if (repeated) {
            return;
        }

        LAST_DENY_MESSAGE.put(player.getUUID(), new DenyMessageState(message, now));
        player.sendSystemMessage(text);
    }

    private record DenyMessageState(String message, long tick) {
    }
}
