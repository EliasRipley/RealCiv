package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.LandClass;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.HashMap;
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
import org.jetbrains.annotations.Nullable;

public class LandClaimMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 54;
    private static final int MAP_SLOTS = 49;
    private static final int GRID_SIDE = 7;
    private static final int WEST_SLOT = 45;
    private static final int NORTH_SLOT = 46;
    private static final int CENTER_SLOT = 47;
    private static final int MODE_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int HELP_SLOT = 50;
    private static final int EAST_SLOT = 51;
    private static final int SOUTH_SLOT = 52;
    private static final int REFRESH_SLOT = 53;

    private final net.minecraft.world.SimpleContainer display = new net.minecraft.world.SimpleContainer(SLOT_COUNT);
    private final ServerPlayer viewer;
    private final CivSavedData data;
    private final String civId;
    private final boolean admin;
    private final boolean mayor;
    private final Map<Integer, ChunkRef> mapSlots = new HashMap<>();
    private long centerChunkX;
    private long centerChunkZ;
    private ViewMode mode = ViewMode.PRIVATE;

    public LandClaimMenu(int containerId, Inventory inventory, ServerPlayer viewer, CivSavedData data, String civId) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = viewer;
        this.data = data;
        this.civId = civId;
        this.admin = viewer.hasPermissions(3);
        this.mayor = data.isMayor(civId, viewer.getUUID());
        this.centerChunkX = viewer.chunkPosition().x;
        this.centerChunkZ = viewer.chunkPosition().z;
        if (admin || mayor) {
            this.mode = ViewMode.TOWN;
        }

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

        switch (slotId) {
            case WEST_SLOT -> centerChunkX -= GRID_SIDE;
            case EAST_SLOT -> centerChunkX += GRID_SIDE;
            case NORTH_SLOT -> centerChunkZ -= GRID_SIDE;
            case SOUTH_SLOT -> centerChunkZ += GRID_SIDE;
            case CENTER_SLOT -> {
                centerChunkX = viewer.chunkPosition().x;
                centerChunkZ = viewer.chunkPosition().z;
            }
            case MODE_SLOT -> {
                if (mode == ViewMode.TOWN && !(admin || mayor)) {
                    mode = ViewMode.PRIVATE;
                } else {
                    mode = mode == ViewMode.TOWN ? ViewMode.PRIVATE : ViewMode.TOWN;
                }
            }
            case INFO_SLOT, HELP_SLOT, REFRESH_SLOT -> {
            }
            default -> {
                @Nullable ChunkRef chunk = mapSlots.get(slotId);
                if (chunk != null) {
                    handleChunkAction(chunk.chunkX(), chunk.chunkZ(), button, clickType);
                }
            }
        }
        refresh();
    }

    private void handleChunkAction(long chunkX, long chunkZ, int button, ClickType clickType) {
        boolean rightClick = clickType == ClickType.PICKUP && button == 1;
        if (mode == ViewMode.TOWN) {
            if (!(admin || mayor)) {
                RealCivMessages.deny(viewer, "Only the mayor/admin can manage town (CIVIC) claims.");
                return;
            }
            if (rightClick) {
                unclaimTownChunk(chunkX, chunkZ);
            } else {
                claimTownChunk(chunkX, chunkZ);
            }
            return;
        }

        if (rightClick) {
            unclaimPrivateChunk(chunkX, chunkZ);
        } else {
            claimOrRenewPrivateChunk(chunkX, chunkZ);
        }
    }

    private void claimTownChunk(long chunkX, long chunkZ) {
        String dimension = viewer.serverLevel().dimension().location().toString();
        if (!isClaimDimensionAllowed(dimension)) {
            return;
        }
        long now = viewer.serverLevel().getServer().overworld().getGameTime();

        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing != null && !existing.civilizationId().equals(civId) && !admin) {
            RealCivMessages.deny(viewer, "This chunk belongs to another civilization.");
            return;
        }
        if (existing != null && existing.civilizationId().equals(civId) && existing.plot().landClass() == LandClass.CIVIC) {
            RealCivMessages.deny(viewer, "That chunk is already a CIVIC town claim.");
            return;
        }
        int civicChunks = data.countPlotsByClass(civId, LandClass.CIVIC);
        if (civicChunks > 0 && !isWithinOrAdjacentToTown(dimension, chunkX, chunkZ)) {
            RealCivMessages.deny(viewer, "Town claims must be within or adjacent to existing town land.");
            return;
        }

        long claimCost = nextTownClaimCostCents(civicChunks);
        long treasury = data.civTreasuryCents(civId);
        if (treasury < claimCost) {
            RealCivMessages.deny(
                    viewer,
                    "Not enough collective contribution karma. Need " + RealCivUtil.formatCredits(claimCost)
                            + ", civ has " + RealCivUtil.formatCredits(treasury) + ".");
            return;
        }

        if (existing != null && !existing.civilizationId().equals(civId) && admin) {
            data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        }

        data.addCivTreasuryCents(civId, -claimCost);
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.CIVIC, null, now, 0L);
        data.addAuditLog(
                civId,
                viewer.getGameProfile().getName() + " claimed town chunk " + dimension + "[" + chunkX + "," + chunkZ + "] via land GUI"
                        + " for " + RealCivUtil.formatCredits(claimCost),
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        viewer.sendSystemMessage(Component.literal(
                "Town chunk claimed [" + chunkX + ", " + chunkZ + "]. Collective contribution karma now: "
                        + RealCivUtil.formatCredits(data.civTreasuryCents(civId))));
    }

    private void unclaimTownChunk(long chunkX, long chunkZ) {
        String dimension = viewer.serverLevel().dimension().location().toString();
        @Nullable CivSavedData.PlotLookup existing = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (existing == null) {
            RealCivMessages.deny(viewer, "That chunk is not claimed.");
            return;
        }
        if (!existing.civilizationId().equals(civId) && !admin) {
            RealCivMessages.deny(viewer, "You cannot unclaim another civilization's chunk.");
            return;
        }
        if (existing.plot().landClass() == LandClass.PRIVATE && !admin) {
            RealCivMessages.deny(viewer, "Use private-mode right click (or plot unclaim) for PRIVATE plots.");
            return;
        }

        data.clearPlot(existing.civilizationId(), dimension, chunkX, chunkZ);
        data.addAuditLog(
                existing.civilizationId(),
                viewer.getGameProfile().getName() + " unclaimed chunk " + dimension + "[" + chunkX + "," + chunkZ + "] via land GUI",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        viewer.sendSystemMessage(Component.literal("Unclaimed chunk [" + chunkX + ", " + chunkZ + "]."));
    }

    private void claimOrRenewPrivateChunk(long chunkX, long chunkZ) {
        String dimension = viewer.serverLevel().dimension().location().toString();
        if (!isClaimDimensionAllowed(dimension)) {
            return;
        }
        long now = viewer.serverLevel().getServer().overworld().getGameTime();
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        int days = Math.max(1, RealCivConfig.LAND_RENT_DAYS.get());
        long paidTicks = days * 24_000L;

        if (!isWithinOrAdjacentToTown(dimension, chunkX, chunkZ)) {
            RealCivMessages.deny(viewer, "Private plots must be within or adjacent to your town's CIVIC claims.");
            return;
        }

        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup != null && !lookup.civilizationId().equals(civId) && !admin) {
            RealCivMessages.deny(viewer, "That chunk belongs to another civilization.");
            return;
        }
        if (lookup != null
                && lookup.civilizationId().equals(civId)
                && lookup.plot().landClass() == LandClass.PRIVATE
                && lookup.plot().ownerId() != null
                && !lookup.plot().ownerId().equals(viewer.getUUID())
                && !admin) {
            RealCivMessages.deny(viewer, "That private plot is owned by another player.");
            return;
        }
        if (lookup != null
                && lookup.civilizationId().equals(civId)
                && lookup.plot().landClass() == LandClass.CIVIC
                && !admin) {
            RealCivMessages.deny(viewer, "This chunk is CIVIC land. Ask your mayor to allot it.");
            return;
        }

        int ownedPrivate = data.privatePlotCountForOwner(civId, viewer.getUUID());
        long cost = nextPrivateClaimCostCents(ownedPrivate);
        if (record.socialCreditCents(civId) < cost) {
            RealCivMessages.deny(
                    viewer,
                    "Not enough contribution karma. Need " + RealCivUtil.formatCredits(cost)
                            + ", you have " + RealCivUtil.formatCredits(record.socialCreditCents(civId)) + ".");
            return;
        }

        if (lookup != null && !lookup.civilizationId().equals(civId) && admin) {
            data.clearPlot(lookup.civilizationId(), dimension, chunkX, chunkZ);
        }

        if (lookup != null
                && lookup.civilizationId().equals(civId)
                && lookup.plot().landClass() == LandClass.PRIVATE
                && viewer.getUUID().equals(lookup.plot().ownerId())) {
            long next = Math.max(now, lookup.plot().nextUpkeepTick()) + paidTicks;
            lookup.plot().setOwnerId(viewer.getUUID());
            lookup.plot().setDelinquentSinceTick(-1L);
            lookup.plot().setNextUpkeepTick(next);
            record.addSocialCreditCents(civId, -cost);
            data.addAuditLog(
                    civId,
                    viewer.getGameProfile().getName() + " renewed private plot " + dimension + "[" + chunkX + "," + chunkZ + "] via land GUI"
                            + " until upkeep tick " + next,
                    RealCivConfig.MAX_AUDIT_LOGS.get());
            data.setDirty();
            viewer.sendSystemMessage(Component.literal(
                    "Private plot renewed [" + chunkX + ", " + chunkZ + "]. Cost: " + RealCivUtil.formatCredits(cost)
                            + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))));
            return;
        }

        record.addSocialCreditCents(civId, -cost);
        data.setPlot(civId, dimension, chunkX, chunkZ, LandClass.PRIVATE, viewer.getUUID(), now, paidTicks);
        data.addAuditLog(
                civId,
                viewer.getGameProfile().getName() + " claimed private plot " + dimension + "[" + chunkX + "," + chunkZ + "] via land GUI",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        viewer.sendSystemMessage(Component.literal(
                "Private chunk claimed [" + chunkX + ", " + chunkZ + "]. Cost: " + RealCivUtil.formatCredits(cost)
                        + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civId))));
    }

    private void unclaimPrivateChunk(long chunkX, long chunkZ) {
        String dimension = viewer.serverLevel().dimension().location().toString();
        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        if (lookup == null) {
            RealCivMessages.deny(viewer, "That chunk is not claimed.");
            return;
        }
        if (!lookup.civilizationId().equals(civId) && !admin) {
            RealCivMessages.deny(viewer, "You cannot unclaim another civilization's chunk.");
            return;
        }
        if (lookup.plot().landClass() != LandClass.PRIVATE) {
            RealCivMessages.deny(viewer, "That chunk is not PRIVATE.");
            return;
        }
        if (lookup.plot().ownerId() != null
                && !lookup.plot().ownerId().equals(viewer.getUUID())
                && !admin
                && !mayor) {
            RealCivMessages.deny(viewer, "Only owner/mayor/admin can unclaim this private plot.");
            return;
        }

        data.clearPlot(lookup.civilizationId(), dimension, chunkX, chunkZ);
        data.addAuditLog(
                lookup.civilizationId(),
                viewer.getGameProfile().getName() + " unclaimed private plot " + dimension + "[" + chunkX + "," + chunkZ + "] via land GUI",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();
        viewer.sendSystemMessage(Component.literal("Private plot unclaimed [" + chunkX + ", " + chunkZ + "]."));
    }

    private boolean isClaimDimensionAllowed(String dimension) {
        if (admin) {
            return true;
        }
        if (RealCivConfig.canClaimDimension(dimension)) {
            return true;
        }
        RealCivMessages.deny(
                viewer,
                "Land claiming is disabled in dimension '" + dimension + "' by server policy ("
                        + RealCivConfig.claimDimensionPolicyLabel() + ").");
        return false;
    }

    private void refresh() {
        mapSlots.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }

        String dimension = viewer.serverLevel().dimension().location().toString();
        int index = 0;
        for (int row = 0; row < GRID_SIDE; row++) {
            for (int col = 0; col < GRID_SIDE; col++) {
                long chunkX = centerChunkX + (col - 3);
                long chunkZ = centerChunkZ + (row - 3);
                ItemStack cell = mapItemForChunk(dimension, chunkX, chunkZ);
                display.setItem(index, cell);
                mapSlots.put(index, new ChunkRef(chunkX, chunkZ));
                index++;
            }
        }

        display.setItem(WEST_SLOT, named(Items.ARROW, "Pan West"));
        display.setItem(NORTH_SLOT, named(Items.ARROW, "Pan North"));
        display.setItem(CENTER_SLOT, named(Items.COMPASS, "Center On You"));
        display.setItem(EAST_SLOT, named(Items.ARROW, "Pan East"));
        display.setItem(SOUTH_SLOT, named(Items.ARROW, "Pan South"));
        display.setItem(REFRESH_SLOT, named(Items.SPYGLASS, "Refresh"));

        String modeText = mode == ViewMode.TOWN ? "Mode: TOWN (CIVIC)" : "Mode: PRIVATE";
        display.setItem(MODE_SLOT, named(mode == ViewMode.TOWN ? Items.BLUE_BANNER : Items.YELLOW_BANNER, modeText));

        String civName = civDisplay(civId);
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        ItemStack info = new ItemStack(Items.BOOK);
        info.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(
                        "Land GUI | " + civName
                                + " | center [" + centerChunkX + "," + centerChunkZ + "]"
                                + " | credits " + RealCivUtil.formatCredits(record.socialCreditCents(civId))
                                + " | collective contribution karma " + RealCivUtil.formatCredits(data.civTreasuryCents(civId))));
        display.setItem(INFO_SLOT, info);

        ItemStack help = new ItemStack(Items.PAPER);
        String clickHelp = mode == ViewMode.TOWN
                ? "TOWN mode: L-claim CIVIC, R-unclaim (mayor/admin)"
                : "PRIVATE mode: L-claim/renew self, R-unclaim";
        help.set(DataComponents.CUSTOM_NAME, Component.literal(clickHelp + " | Click mode button to switch."));
        display.setItem(HELP_SLOT, help);

        this.broadcastChanges();
    }

    private ItemStack mapItemForChunk(String dimension, long chunkX, long chunkZ) {
        @Nullable CivSavedData.PlotLookup lookup = data.getPlotAnyCivilization(dimension, chunkX, chunkZ);
        boolean atPlayer = chunkX == viewer.chunkPosition().x && chunkZ == viewer.chunkPosition().z;

        ItemStack stack;
        String status;
        if (lookup == null) {
            stack = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
            status = "Wilderness (unclaimed)";
        } else if (!lookup.civilizationId().equals(civId)) {
            stack = new ItemStack(Items.RED_STAINED_GLASS_PANE);
            status = "Other civ: " + civDisplay(lookup.civilizationId()) + " | " + lookup.plot().landClass().name();
        } else if (lookup.plot().landClass() == LandClass.CIVIC) {
            stack = new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS_PANE);
            status = "Your civ CIVIC";
        } else if (lookup.plot().landClass() == LandClass.COMMUNITY) {
            stack = new ItemStack(Items.WHITE_STAINED_GLASS_PANE);
            status = "Your civ COMMUNITY";
        } else if (lookup.plot().ownerId() != null && lookup.plot().ownerId().equals(viewer.getUUID())) {
            stack = new ItemStack(Items.YELLOW_STAINED_GLASS_PANE);
            status = "Your PRIVATE plot";
        } else {
            stack = new ItemStack(Items.ORANGE_STAINED_GLASS_PANE);
            status = "Other player's PRIVATE plot";
        }

        String marker = atPlayer ? " [YOU]" : "";
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(
                        "Chunk [" + chunkX + "," + chunkZ + "]" + marker
                                + " | " + status));
        return stack;
    }

    private boolean isWithinOrAdjacentToTown(String dimension, long chunkX, long chunkZ) {
        return isTownChunk(dimension, chunkX, chunkZ)
                || isTownChunk(dimension, chunkX + 1, chunkZ)
                || isTownChunk(dimension, chunkX - 1, chunkZ)
                || isTownChunk(dimension, chunkX, chunkZ + 1)
                || isTownChunk(dimension, chunkX, chunkZ - 1);
    }

    private boolean isTownChunk(String dimension, long chunkX, long chunkZ) {
        @Nullable CivSavedData.PlotRecord plot = data.getPlot(civId, dimension, chunkX, chunkZ);
        return plot != null && plot.landClass() == LandClass.CIVIC;
    }

    private long nextTownClaimCostCents(int civicChunksOwned) {
        long base = RealCivConfig.townClaimCostCents();
        long extra = RealCivConfig.townClaimCostAddedPerOwnedCents() * Math.max(0, civicChunksOwned);
        return Math.max(0L, base + extra);
    }

    private long nextPrivateClaimCostCents(int privateOwnedByPlayer) {
        long base = RealCivConfig.rentCostCents();
        long extra = RealCivConfig.rentCostAddedPerOwnedPrivateCents() * Math.max(0, privateOwnedByPlayer);
        return Math.max(0L, base + extra);
    }

    private String civDisplay(String id) {
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(id);
        return civ == null ? id : civ.displayName();
    }

    private static ItemStack named(net.minecraft.world.level.ItemLike item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private enum ViewMode {
        TOWN,
        PRIVATE
    }

    private record ChunkRef(long chunkX, long chunkZ) {
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
