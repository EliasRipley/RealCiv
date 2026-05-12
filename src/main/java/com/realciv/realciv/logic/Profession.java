package com.realciv.realciv.logic;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Profession {
    FARMER,
    MINER,
    TERRAFORMER,
    LUMBERJACK,
    FISHER,
    HUNTER,
    WARRIOR,
    EXPLOSIVES_EXPERT,
    CRAFTER,
    NONE;

    @Nullable
    public static Profession fromConfigName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "FARMER" -> FARMER;
            case "MINER" -> MINER;
            case "TERRAFORMER", "TERRAFORMING", "DIGGER" -> TERRAFORMER;
            case "LUMBERJACK", "WOODCUTTER" -> LUMBERJACK;
            case "FISHER", "FISHERMAN", "ANGLER", "FISHING" -> FISHER;
            case "HUNTER" -> HUNTER;
            case "WARRIOR", "SOLDIER", "PVP" -> WARRIOR;
            case "EXPLOSIVES_EXPERT", "EXPLOSIVESEXPERT", "EXPLOSIVES", "DEMOLITIONIST", "BOMBER" -> EXPLOSIVES_EXPERT;
            case "CRAFTER" -> CRAFTER;
            case "NONE", "GENERAL" -> NONE;
            default -> null;
        };
    }
}
