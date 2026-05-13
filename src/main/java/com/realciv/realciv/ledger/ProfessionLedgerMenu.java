package com.realciv.realciv.ledger;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.Comparator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class ProfessionLedgerMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 54;
    private static final Profession[] ORDERED_PROFESSIONS = new Profession[] {
            Profession.FARMER,
            Profession.MINER,
            Profession.TERRAFORMER,
            Profession.LUMBERJACK,
            Profession.FISHER,
            Profession.HUNTER,
            Profession.WARRIOR,
            Profession.EXPLOSIVES_EXPERT,
            Profession.CRAFTER,
            Profession.ENCHANTER,
            Profession.BREWER,
            Profession.TRADER
    };

    private final net.minecraft.world.SimpleContainer display = new net.minecraft.world.SimpleContainer(SLOT_COUNT);
    private final ServerPlayer viewer;
    private final CivSavedData data;
    private final String civilizationId;

    public ProfessionLedgerMenu(
            int containerId,
            Inventory inventory,
            ServerPlayer viewer,
            CivSavedData data,
            String civilizationId) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = viewer;
        this.data = data;
        this.civilizationId = civilizationId;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9;
                this.addSlot(new ReadOnlySlot(display, slot, 8 + col * 18, 18 + row * 18));
            }
        }

        int invStartY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, invStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, invStartY + 58));
        }

        refresh();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= this.slots.size()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (slotId >= SLOT_COUNT) {
            super.clicked(slotId, button, clickType, player);
        }
    }

    private void refresh() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }
        for (int slot = 9; slot <= 17; slot++) {
            display.setItem(slot, named(Items.GRAY_STAINED_GLASS_PANE, " "));
        }
        for (int slot = 45; slot <= 53; slot++) {
            display.setItem(slot, named(Items.GRAY_STAINED_GLASS_PANE, " "));
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civilizationId);
        String civName = civ == null ? civilizationId : civ.displayName();
        Profession topProfession = topProfession(record);

        display.setItem(
                0,
                named(
                        Items.BOOK,
                        "Profession Ledger | " + civName
                                + " | " + viewer.getGameProfile().getName()));
        display.setItem(
                1,
                named(
                        Items.EXPERIENCE_BOTTLE,
                        "General Level " + record.generalLevel()
                                + " | XP " + record.generalXp()));
        display.setItem(
                2,
                named(
                        Items.EMERALD,
                        "Contribution Karma: "
                                + RealCivUtil.formatCredits(record.socialCreditCents(civilizationId))));
        display.setItem(
                3,
                named(
                        Items.NETHER_STAR,
                        "Top Profession: " + displayProfession(topProfession)
                                + " (Lv " + record.levelFor(topProfession) + ")"));
        display.setItem(
                4,
                named(
                        Items.COMPASS,
                        "Focus: " + displayProfession(record.focusedProfession())));
        display.setItem(
                5,
                named(
                        Items.CLOCK,
                        "Pending Warrior Registrations: " + record.pendingWarriorHubRegistrations()));
        display.setItem(
                6,
                named(
                        Items.CHEST,
                        "Contributed Item Types: " + record.contributions(civilizationId).size()));
        display.setItem(
                7,
                named(
                        Items.MAP,
                        "Civ Members: " + data.civilizationMembersSorted(civilizationId).size()));
        display.setItem(
                8,
                named(
                        Items.WRITABLE_BOOK,
                        "Use this ledger to track your leveling bottlenecks."));

        for (int i = 0; i < ORDERED_PROFESSIONS.length; i++) {
            Profession profession = ORDERED_PROFESSIONS[i];
            int slot = 18 + i;
            int level = record.levelFor(profession);
            int xp = xpForProfession(record, profession);
            int actions = record.actionsForProfession(profession);
            int limit = actionLimitForProfession(record, profession);
            String actionText = limit <= 0
                    ? "actions " + actions
                    : "actions " + actions + "/" + limit;
            display.setItem(
                    slot,
                    named(
                            iconForProfession(profession),
                            displayProfession(profession)
                                    + " | Lv " + level
                                    + " | XP " + xp
                                    + " | " + actionText));
        }

        display.setItem(
                49,
                named(
                        Items.PAPER,
                        "Legend: Lv=profession level, XP=profession XP, actions=current usage."));
        this.broadcastChanges();
    }

    private Profession topProfession(CivSavedData.PlayerRecord record) {
        return java.util.Arrays.stream(ORDERED_PROFESSIONS)
                .max(Comparator
                        .comparingInt(record::levelFor)
                        .thenComparingInt(profession -> xpForProfession(record, profession)))
                .orElse(Profession.NONE);
    }

    private int xpForProfession(CivSavedData.PlayerRecord record, Profession profession) {
        return switch (profession) {
            case FARMER -> record.farmerXp();
            case MINER -> record.minerXp();
            case TERRAFORMER -> record.terraformerXp();
            case LUMBERJACK -> record.lumberjackXp();
            case FISHER -> record.fisherXp();
            case HUNTER -> record.hunterXp();
            case WARRIOR -> record.warriorXp();
            case EXPLOSIVES_EXPERT -> record.explosivesExpertXp();
            case CRAFTER -> record.crafterXp();
            case ENCHANTER -> record.enchanterXp();
            case BREWER -> record.brewerXp();
            case TRADER -> record.traderXp();
            case NONE -> 0;
        };
    }

    private int actionLimitForProfession(CivSavedData.PlayerRecord record, Profession profession) {
        int level = record.levelFor(profession);
        return switch (profession) {
            case FARMER -> RealCivConfig.farmerLimitForLevel(level);
            case MINER -> RealCivConfig.minerLimitForLevel(level);
            case TERRAFORMER -> RealCivConfig.terraformerLimitForLevel(level);
            case LUMBERJACK -> RealCivConfig.lumberjackLimitForLevel(level);
            case FISHER -> RealCivConfig.fisherLimitForLevel(level);
            case HUNTER -> RealCivConfig.hunterLimitForLevel(level);
            case WARRIOR -> RealCivConfig.warriorLimitForLevel(level);
            case EXPLOSIVES_EXPERT -> RealCivConfig.explosivesExpertLimitForLevel(level);
            case CRAFTER -> RealCivConfig.crafterLimitForLevel(level);
            case ENCHANTER -> RealCivConfig.enchanterLimitForLevel(level);
            case BREWER -> RealCivConfig.brewerLimitForLevel(level);
            case TRADER -> RealCivConfig.traderLimitForLevel(level);
            case NONE -> 0;
        };
    }

    private String displayProfession(@Nullable Profession profession) {
        if (profession == null || profession == Profession.NONE) {
            return "None";
        }
        String name = profession.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            if (words[i].isEmpty()) {
                continue;
            }
            out.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) {
                out.append(words[i].substring(1));
            }
        }
        return out.toString();
    }

    private net.minecraft.world.level.ItemLike iconForProfession(Profession profession) {
        return switch (profession) {
            case FARMER -> Items.WHEAT;
            case MINER -> Items.IRON_PICKAXE;
            case TERRAFORMER -> Items.IRON_SHOVEL;
            case LUMBERJACK -> Items.IRON_AXE;
            case FISHER -> Items.FISHING_ROD;
            case HUNTER -> Items.BOW;
            case WARRIOR -> Items.IRON_SWORD;
            case EXPLOSIVES_EXPERT -> Items.TNT;
            case CRAFTER -> Items.CRAFTING_TABLE;
            case ENCHANTER -> Items.ENCHANTING_TABLE;
            case BREWER -> Items.BREWING_STAND;
            case TRADER -> Items.EMERALD;
            case NONE -> Items.BARRIER;
        };
    }

    private static ItemStack named(net.minecraft.world.level.ItemLike item, String text) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(text));
        return stack;
    }

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
