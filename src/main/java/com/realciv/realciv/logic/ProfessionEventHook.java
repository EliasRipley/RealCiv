package com.realciv.realciv.logic;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Config-facing identifiers for optional event-driven profession action limits.
 */
public enum ProfessionEventHook {
    ANIMAL_BREED,
    ANIMAL_TAME,
    SHEAR_ENTITY,
    SHEAR_BLOCK,
    PLACE_SCAFFOLDING,
    BONEMEAL_USE,
    TOOL_STRIP_LOG,
    TOOL_TILL_SOIL,
    TOOL_FLATTEN_PATH,
    TOOL_DOUSE_CAMPFIRE,
    TOOL_SCRAPE_COPPER,
    TOOL_WAX_OFF,
    FARMLAND_TRAMPLE,
    VILLAGER_INTERACT,
    VILLAGER_TRADE,
    ANVIL_USE,
    ANVIL_REPAIR, // legacy alias, prefer ANVIL_REPAIR_TOOL / ANVIL_RENAME / ANVIL_COMBINE_ENCHANT
    ANVIL_RENAME,
    ANVIL_REPAIR_TOOL,
    ANVIL_COMBINE_ENCHANT,
    ITEM_SMELT,
    ITEM_ENCHANT,
    POTION_BREW,
    ITEM_TOSS,
    STAT_AWARD;

    @Nullable
    public static ProfessionEventHook fromConfigName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ANIMAL_BREED", "BREED", "BREED_ANIMAL", "ANIMAL_BREEDING" -> ANIMAL_BREED;
            case "ANIMAL_TAME", "TAME", "TAME_ANIMAL" -> ANIMAL_TAME;
            case "SHEAR_ENTITY", "ENTITY_SHEAR", "SHEAR_MOB", "SHEAR_ANIMAL" -> SHEAR_ENTITY;
            case "SHEAR_BLOCK", "BLOCK_SHEAR", "SHEAR_PLANT" -> SHEAR_BLOCK;
            case "PLACE_SCAFFOLDING", "SCAFFOLDING_PLACE", "SCAFFOLD_PLACE" -> PLACE_SCAFFOLDING;
            case "BONEMEAL_USE", "BONEMEAL", "USE_BONEMEAL" -> BONEMEAL_USE;
            case "TOOL_STRIP_LOG", "STRIP_LOG", "AXE_STRIP" -> TOOL_STRIP_LOG;
            case "TOOL_TILL_SOIL", "TILL_SOIL", "HOE_TILL" -> TOOL_TILL_SOIL;
            case "TOOL_FLATTEN_PATH", "FLATTEN_PATH", "PATH_FLATTEN", "SHOVEL_FLATTEN" -> TOOL_FLATTEN_PATH;
            case "TOOL_DOUSE_CAMPFIRE", "DOUSE_CAMPFIRE", "SHOVEL_DOUSE" -> TOOL_DOUSE_CAMPFIRE;
            case "TOOL_SCRAPE_COPPER", "SCRAPE_COPPER", "AXE_SCRAPE" -> TOOL_SCRAPE_COPPER;
            case "TOOL_WAX_OFF", "WAX_OFF", "AXE_WAX_OFF" -> TOOL_WAX_OFF;
            case "FARMLAND_TRAMPLE", "TRAMPLE_FARMLAND", "TRAMPLE" -> FARMLAND_TRAMPLE;
            case "VILLAGER_INTERACT", "INTERACT_VILLAGER", "OPEN_VILLAGER_TRADE" -> VILLAGER_INTERACT;
            case "VILLAGER_TRADE", "TRADE_VILLAGER", "COMPLETE_VILLAGER_TRADE" -> VILLAGER_TRADE;
            case "ANVIL_USE", "USE_ANVIL", "ANVIL_INTERACT" -> ANVIL_USE;
            case "ANVIL_REPAIR", "ANVIL_OUTPUT", "ANVIL_TAKE_RESULT" -> ANVIL_REPAIR;
            case "ANVIL_RENAME", "RENAME_ANVIL", "ANVIL_NAME" -> ANVIL_RENAME;
            case "ANVIL_REPAIR_TOOL", "REPAIR_TOOL", "ANVIL_REPAIR_ITEM", "REPAIR_ITEM" -> ANVIL_REPAIR_TOOL;
            case "ANVIL_COMBINE_ENCHANT", "COMBINE_ENCHANT", "ANVIL_ENCHANT", "ENCHANT_COMBINE" -> ANVIL_COMBINE_ENCHANT;
            case "ITEM_SMELT", "SMELT_ITEM", "ITEM_SMELTED" -> ITEM_SMELT;
            case "ITEM_ENCHANT", "ENCHANT_ITEM", "ITEM_ENCHANTED" -> ITEM_ENCHANT;
            case "POTION_BREW", "BREW_POTION", "POTION_BREWED" -> POTION_BREW;
            case "ITEM_TOSS", "TOSS_ITEM", "DROP_ITEM" -> ITEM_TOSS;
            case "STAT_AWARD", "AWARD_STAT", "STAT_PROGRESS" -> STAT_AWARD;
            default -> null;
        };
    }
}
