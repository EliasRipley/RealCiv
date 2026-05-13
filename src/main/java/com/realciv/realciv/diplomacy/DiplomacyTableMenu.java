package com.realciv.realciv.diplomacy;

import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class DiplomacyTableMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 54;
    private static final int RELATIONS_START = 9;
    private static final int RELATIONS_COUNT = 36;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final net.minecraft.world.SimpleContainer display = new net.minecraft.world.SimpleContainer(SLOT_COUNT);
    private final ServerPlayer viewer;
    private final CivSavedData data;
    private final String civilizationId;
    private final boolean canManage;
    private final Map<Integer, String> relationSlots = new HashMap<>();
    private int relationPage;
    private int totalRelationPages = 1;

    public DiplomacyTableMenu(
            int containerId,
            Inventory inventory,
            ServerPlayer viewer,
            CivSavedData data,
            String civilizationId) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = viewer;
        this.data = data;
        this.civilizationId = civilizationId;
        this.canManage = CivPermissionService.hasCivPermission(
                viewer,
                data,
                civilizationId,
                CivSavedData.ROLE_PERMISSION_MANAGE_DIPLOMACY);

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
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.getUUID().equals(viewer.getUUID())) {
            return;
        }

        if (slotId == PREV_SLOT) {
            if (relationPage > 0) {
                relationPage--;
                refresh();
            }
            return;
        }
        if (slotId == NEXT_SLOT) {
            if (relationPage + 1 < totalRelationPages) {
                relationPage++;
                refresh();
            }
            return;
        }

        @Nullable String otherCivId = relationSlots.get(slotId);
        if (otherCivId != null && canManage) {
            CivSavedData.DiplomacyState current = data.diplomacyState(civilizationId, otherCivId);
            CivSavedData.DiplomacyState next = switch (current) {
                case NEUTRAL -> CivSavedData.DiplomacyState.ALLY;
                case ALLY -> CivSavedData.DiplomacyState.WAR;
                case WAR -> CivSavedData.DiplomacyState.NEUTRAL;
            };
            if (data.setDiplomacyState(civilizationId, otherCivId, next, viewer.getGameProfile().getName())) {
                viewer.sendSystemMessage(Component.literal(
                        "Diplomacy with " + civDisplay(otherCivId) + " is now " + next.displayName() + "."));
            } else {
                RealCivMessages.deny(viewer, "No diplomacy change was applied.");
            }
            refresh();
        } else if (otherCivId != null) {
            RealCivMessages.deny(viewer, "Only civilization leadership can change diplomacy.");
        }
    }

    private void refresh() {
        relationSlots.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }

        List<String> otherCivilizations = new ArrayList<>(data.civilizationIdsSorted());
        otherCivilizations.removeIf(id -> id.equals(civilizationId));

        int allyCount = 0;
        int neutralCount = 0;
        int warCount = 0;
        for (String other : otherCivilizations) {
            switch (data.diplomacyState(civilizationId, other)) {
                case ALLY -> allyCount++;
                case WAR -> warCount++;
                case NEUTRAL -> neutralCount++;
            }
        }

        display.setItem(
                0,
                named(
                        Items.MAP,
                        "Diplomacy Table | " + civDisplay(civilizationId)
                                + " | allies " + allyCount
                                + " | neutral " + neutralCount
                                + " | war " + warCount));
        display.setItem(
                1,
                named(
                        Items.SHIELD,
                        "Intra-civ PvP: " + (data.allowIntraCivPvp(civilizationId) ? "enabled" : "disabled")));
        display.setItem(
                2,
                named(
                        Items.PAPER,
                        canManage
                                ? "Click relation entries to cycle Neutral -> Ally -> War."
                                : "Read-only mode. Leadership permissions required to edit diplomacy."));
        display.setItem(
                3,
                named(
                        Items.BOOK,
                        "War casualties are tracked per civilization pair."));

        totalRelationPages = Math.max(1, (otherCivilizations.size() + RELATIONS_COUNT - 1) / RELATIONS_COUNT);
        relationPage = Math.max(0, Math.min(relationPage, totalRelationPages - 1));

        int start = relationPage * RELATIONS_COUNT;
        int end = Math.min(otherCivilizations.size(), start + RELATIONS_COUNT);
        for (int idx = start; idx < end; idx++) {
            String otherCivId = otherCivilizations.get(idx);
            CivSavedData.DiplomacyState state = data.diplomacyState(civilizationId, otherCivId);
            CivSavedData.WarCasualtyView casualtyView = data.warCasualtiesBetween(civilizationId, otherCivId);
            int slot = RELATIONS_START + (idx - start);

            ItemStack icon = new ItemStack(iconForState(state));
            String casualtyLine = state == CivSavedData.DiplomacyState.WAR
                    ? " | casualties us "
                    + casualtyView.yourCasualties()
                    + " / them "
                    + casualtyView.otherCasualties()
                    : "";
            icon.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(
                            civDisplay(otherCivId)
                                    + " [" + otherCivId + "]"
                                    + " | " + state.displayName()
                                    + casualtyLine
                                    + (canManage ? " | click to cycle" : "")));
            display.setItem(slot, icon);
            relationSlots.put(slot, otherCivId);
        }

        for (int i = 45; i < SLOT_COUNT; i++) {
            if (display.getItem(i).isEmpty()) {
                display.setItem(i, named(Items.GRAY_STAINED_GLASS_PANE, " "));
            }
        }
        display.setItem(PREV_SLOT, named(Items.ARROW, "Previous Diplomacy Page"));
        display.setItem(NEXT_SLOT, named(Items.ARROW, "Next Diplomacy Page"));
        display.setItem(
                INFO_SLOT,
                named(
                        Items.PAPER,
                        "Relations page " + (relationPage + 1) + "/" + totalRelationPages
                                + " | " + otherCivilizations.size() + " civilization(s) listed"));
        this.broadcastChanges();
    }

    private String civDisplay(String civId) {
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civId);
        return civ == null ? civId : civ.displayName();
    }

    private net.minecraft.world.level.ItemLike iconForState(CivSavedData.DiplomacyState state) {
        return switch (state) {
            case ALLY -> Items.LIME_BANNER;
            case NEUTRAL -> Items.LIGHT_GRAY_BANNER;
            case WAR -> Items.RED_BANNER;
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
