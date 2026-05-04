package com.realciv.realciv.census;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.RealCivMessages;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

public class CensusMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 54;
    private static final int MEMBERS_START = 0;
    private static final int MEMBERS_COUNT = 27;
    private static final int REQUESTS_START = 27;
    private static final int REQUESTS_COUNT = 9;
    private static final int INVITES_START = 36;
    private static final int INVITES_COUNT = 9;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final net.minecraft.world.SimpleContainer display = new net.minecraft.world.SimpleContainer(SLOT_COUNT);
    private final ServerPlayer viewer;
    private final CivSavedData data;
    private final String civilizationId;
    private final boolean canManage;
    private final Map<Integer, SlotAction> slotActions = new HashMap<>();
    private int memberPage;
    private int totalMemberPages = 1;

    public CensusMenu(
            int containerId,
            Inventory inventory,
            ServerPlayer viewer,
            CivSavedData data,
            String civilizationId,
            boolean canManage) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = viewer;
        this.data = data;
        this.civilizationId = civilizationId;
        this.canManage = canManage;

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
            if (memberPage > 0) {
                memberPage--;
                refresh();
            }
            return;
        }
        if (slotId == NEXT_SLOT) {
            if (memberPage + 1 < totalMemberPages) {
                memberPage++;
                refresh();
            }
            return;
        }
        if (slotId == INFO_SLOT) {
            refresh();
            return;
        }

        SlotAction action = slotActions.get(slotId);
        if (action == null || !canManage) {
            return;
        }

        switch (action.kind()) {
            case MEMBER -> handleMemberAction(action.playerId(), button, clickType);
            case REQUEST -> handleRequestAction(action.playerId(), button, clickType);
            case INVITE -> handleInviteAction(action.playerId(), button, clickType);
        }
        refresh();
    }

    private void handleMemberAction(UUID memberId, int button, ClickType clickType) {
        if (memberId.equals(viewer.getUUID()) && !viewer.hasPermissions(3)) {
            RealCivMessages.deny(viewer, "Use /realciv civ leave to leave your own civilization.");
            return;
        }
        boolean rightClick = clickType == ClickType.PICKUP && button == 1;
        if (rightClick) {
            boolean currentlyManager = data.isCivicManager(civilizationId, memberId);
            data.setCivicManager(civilizationId, memberId, !currentlyManager, viewer.getGameProfile().getName());
            viewer.sendSystemMessage(Component.literal(
                    (currentlyManager ? "Removed manager role from " : "Assigned manager role to ")
                            + displayName(memberId) + "."));
            return;
        }

        if (data.removeMemberToDefault(civilizationId, memberId, viewer.getGameProfile().getName())) {
            viewer.sendSystemMessage(Component.literal("Removed " + displayName(memberId) + " from the civilization."));
            notifyOnline(memberId, "You were removed from civilization " + displayCivName() + ".");
        } else {
            RealCivMessages.deny(viewer, "Could not remove that member.");
        }
    }

    private void handleRequestAction(UUID playerId, int button, ClickType clickType) {
        boolean rightClick = clickType == ClickType.PICKUP && button == 1;
        if (rightClick) {
            boolean removedRequest = data.removeJoinRequest(civilizationId, playerId, viewer.getGameProfile().getName());
            boolean removedInvite = data.removeInvite(civilizationId, playerId, viewer.getGameProfile().getName());
            if (removedRequest || removedInvite) {
                viewer.sendSystemMessage(Component.literal("Denied/cleared join state for " + displayName(playerId) + "."));
                notifyOnline(playerId, "Your join request/invite for " + displayCivName() + " was denied or revoked.");
            } else {
                RealCivMessages.deny(viewer, "No pending request for that player.");
            }
            return;
        }

        if (data.setPlayerCivilization(playerId, civilizationId, viewer.getGameProfile().getName())) {
            viewer.sendSystemMessage(Component.literal("Approved " + displayName(playerId) + " into the civilization."));
            notifyOnline(playerId, "Your membership in " + displayCivName() + " was approved.");
        } else {
            RealCivMessages.deny(viewer, "Could not approve that request.");
        }
    }

    private void handleInviteAction(UUID playerId, int button, ClickType clickType) {
        if (data.removeInvite(civilizationId, playerId, viewer.getGameProfile().getName())) {
            viewer.sendSystemMessage(Component.literal("Revoked invite for " + displayName(playerId) + "."));
            notifyOnline(playerId, "Your invite to join " + displayCivName() + " was revoked.");
        } else {
            RealCivMessages.deny(viewer, "No invite found for that player.");
        }
    }

    private void refresh() {
        slotActions.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }

        List<UUID> members = data.civilizationMembersSorted(civilizationId);
        List<UUID> requests = data.joinRequestsSorted(civilizationId);
        List<UUID> invites = data.invitedPlayersSorted(civilizationId);

        totalMemberPages = Math.max(1, (members.size() + MEMBERS_COUNT - 1) / MEMBERS_COUNT);
        memberPage = Math.max(0, Math.min(memberPage, totalMemberPages - 1));

        int memberStart = memberPage * MEMBERS_COUNT;
        int memberEnd = Math.min(members.size(), memberStart + MEMBERS_COUNT);
        int uiSlot = MEMBERS_START;
        for (int i = memberStart; i < memberEnd; i++) {
            UUID memberId = members.get(i);
            ItemStack stack = memberIcon(memberId);
            String role = data.isMayor(civilizationId, memberId)
                    ? "MAYOR"
                    : (data.isCivicManager(civilizationId, memberId) ? "MANAGER" : "CITIZEN");
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                    "[Member] " + displayName(memberId) + " | " + role
                            + (canManage ? " | LClick remove, RClick manager" : "")));
            display.setItem(uiSlot, stack);
            slotActions.put(uiSlot, new SlotAction(ActionKind.MEMBER, memberId));
            uiSlot++;
        }

        for (int i = 0; i < Math.min(REQUESTS_COUNT, requests.size()); i++) {
            UUID id = requests.get(i);
            int slot = REQUESTS_START + i;
            ItemStack stack = new ItemStack(Items.LIME_DYE);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                    "[Request] " + displayName(id)
                            + (canManage ? " | LClick approve, RClick deny" : "")));
            display.setItem(slot, stack);
            slotActions.put(slot, new SlotAction(ActionKind.REQUEST, id));
        }

        for (int i = 0; i < Math.min(INVITES_COUNT, invites.size()); i++) {
            UUID id = invites.get(i);
            int slot = INVITES_START + i;
            ItemStack stack = new ItemStack(Items.PAPER);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                    "[Invite] " + displayName(id)
                            + (canManage ? " | LClick revoke" : "")));
            display.setItem(slot, stack);
            slotActions.put(slot, new SlotAction(ActionKind.INVITE, id));
        }

        for (int i = 45; i < SLOT_COUNT; i++) {
            if (display.getItem(i).isEmpty()) {
                ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                display.setItem(i, pane);
            }
        }

        ItemStack prev = new ItemStack(Items.ARROW);
        prev.set(DataComponents.CUSTOM_NAME, Component.literal("Previous Members Page"));
        display.setItem(PREV_SLOT, prev);

        ItemStack next = new ItemStack(Items.ARROW);
        next.set(DataComponents.CUSTOM_NAME, Component.literal("Next Members Page"));
        display.setItem(NEXT_SLOT, next);

        ItemStack info = new ItemStack(Items.BOOK);
        info.set(DataComponents.CUSTOM_NAME, Component.literal(
                "Census " + displayCivName()
                        + " | members " + members.size()
                        + " | requests " + requests.size()
                        + " | invites " + invites.size()
                        + " | page " + (memberPage + 1) + "/" + totalMemberPages));
        display.setItem(INFO_SLOT, info);

        ItemStack requestHelp = new ItemStack(Items.LIME_DYE);
        requestHelp.set(DataComponents.CUSTOM_NAME, Component.literal("Requests row: approve/deny join requests"));
        display.setItem(47, requestHelp);
        ItemStack inviteHelp = new ItemStack(Items.PAPER);
        inviteHelp.set(DataComponents.CUSTOM_NAME, Component.literal("Invite via /realciv census invite <player>"));
        display.setItem(51, inviteHelp);

        this.broadcastChanges();
    }

    private String displayName(UUID playerId) {
        if (viewer.getServer() != null) {
            ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(playerId);
            if (online != null) {
                return online.getGameProfile().getName();
            }
        }
        String raw = playerId.toString();
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }

    private ItemStack memberIcon(UUID playerId) {
        if (data.isMayor(civilizationId, playerId)) {
            return new ItemStack(Items.NETHER_STAR);
        }
        if (data.isCivicManager(civilizationId, playerId)) {
            return new ItemStack(Items.GOLDEN_HELMET);
        }
        return new ItemStack(Items.PLAYER_HEAD);
    }

    private String displayCivName() {
        @org.jetbrains.annotations.Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civilizationId);
        return civ == null ? civilizationId : civ.displayName();
    }

    private void notifyOnline(UUID playerId, String message) {
        if (viewer.getServer() == null) {
            return;
        }
        ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(playerId);
        if (online != null) {
            online.sendSystemMessage(Component.literal(message));
        }
    }

    private enum ActionKind {
        MEMBER,
        REQUEST,
        INVITE
    }

    private record SlotAction(ActionKind kind, UUID playerId) {
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
