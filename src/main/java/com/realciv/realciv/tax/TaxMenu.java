package com.realciv.realciv.tax;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class TaxMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 54;
    private static final int MEMBERS_START = 18;
    private static final int MEMBERS_COUNT = 27;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int PAY_ONE_SLOT = 2;
    private static final int PAY_FIVE_SLOT = 3;
    private static final int PAY_TWENTY_FIVE_SLOT = 4;
    private static final int RATE_DOWN_SLOT = 7;
    private static final int RATE_UP_SLOT = 8;
    private static final int MODE_TOGGLE_SLOT = 9;
    private static final int ITEM_CYCLE_SLOT = 10;
    private static final int ITEM_COUNT_DOWN_SLOT = 11;
    private static final int ITEM_COUNT_UP_SLOT = 12;

    private final net.minecraft.world.SimpleContainer display = new net.minecraft.world.SimpleContainer(SLOT_COUNT);
    private final ServerPlayer viewer;
    private final CivSavedData data;
    private final String civilizationId;
    private final boolean canManage;
    private final Map<Integer, UUID> memberSlots = new HashMap<>();
    private int memberPage;
    private int totalMemberPages = 1;

    public TaxMenu(
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
                CivSavedData.ROLE_PERMISSION_MANAGE_UPKEEP);

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

        switch (slotId) {
            case PAY_ONE_SLOT -> payCycles(1);
            case PAY_FIVE_SLOT -> payCycles(5);
            case PAY_TWENTY_FIVE_SLOT -> payCycles(25);
            case RATE_DOWN_SLOT -> adjustTaxMultiplier(-0.10D);
            case RATE_UP_SLOT -> adjustTaxMultiplier(0.10D);
            case MODE_TOGGLE_SLOT -> toggleTaxPaymentMode();
            case ITEM_CYCLE_SLOT -> cycleTaxItemType();
            case ITEM_COUNT_DOWN_SLOT -> adjustTaxItemCount(-1);
            case ITEM_COUNT_UP_SLOT -> adjustTaxItemCount(1);
            case INFO_SLOT -> {
            }
            default -> {
                @Nullable UUID memberId = memberSlots.get(slotId);
                if (memberId != null && canManage) {
                    sendMemberTaxStatus(memberId);
                }
            }
        }
        refresh();
    }

    private void payCycles(int cycles) {
        TaxStats stats = taxStats(viewer.getUUID());
        if (stats.ownedPlots() <= 0) {
            RealCivMessages.deny(viewer, "You do not own private plots in this civilization.");
            return;
        }

        int safeCycles = Math.max(1, cycles);
        CivSavedData.TaxPaymentMode paymentMode = data.taxPaymentMode(civilizationId);
        long karmaCost = stats.cycleCostCents() * safeCycles;
        ResourceLocation taxItemId = data.taxItemId(civilizationId);
        long itemCost = stats.cycleItemCost() * safeCycles;

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        if (paymentMode == CivSavedData.TaxPaymentMode.KARMA) {
            long balance = record.socialCreditCents(civilizationId);
            if (balance < karmaCost) {
                RealCivMessages.deny(
                        viewer,
                        "Insufficient contribution karma. Need "
                                + RealCivUtil.formatCredits(karmaCost)
                                + ", you have " + RealCivUtil.formatCredits(balance) + ".");
                return;
            }
        } else {
            Item taxItem = BuiltInRegistries.ITEM.getOptional(taxItemId).orElse(Items.AIR);
            if (taxItem == Items.AIR) {
                RealCivMessages.deny(viewer, "Tax item is invalid for this civilization. Ask leadership to reconfigure it.");
                return;
            }
            long available = countInventoryItem(taxItem);
            if (available < itemCost) {
                RealCivMessages.deny(
                        viewer,
                        "Insufficient tax items. Need " + itemCost + "x " + taxItemId + ", you have " + available + ".");
                return;
            }
            removeInventoryItem(taxItem, itemCost);
        }

        long now = viewer.getServer() == null || viewer.getServer().overworld() == null
                ? 0L
                : viewer.getServer().overworld().getGameTime();
        int affectedPlots = data.prepayPrivatePlotUpkeep(
                civilizationId,
                viewer.getUUID(),
                safeCycles,
                now,
                viewer.getGameProfile().getName());
        if (affectedPlots <= 0) {
            RealCivMessages.deny(viewer, "No private plots were eligible for upkeep prepayment.");
            return;
        }

        if (paymentMode == CivSavedData.TaxPaymentMode.KARMA) {
            record.addSocialCreditCents(civilizationId, -karmaCost);
            data.addCivTreasuryCents(civilizationId, karmaCost);
        } else {
            data.addToHubStock(civilizationId, taxItemId, itemCost, viewer.getGameProfile().getName());
        }
        data.addAuditLog(
                civilizationId,
                viewer.getGameProfile().getName()
                        + " paid upkeep tax ("
                        + (paymentMode == CivSavedData.TaxPaymentMode.KARMA
                        ? RealCivUtil.formatCredits(karmaCost) + " karma"
                        : itemCost + "x " + taxItemId)
                        + ")"
                        + " via Tax Block menu for "
                        + affectedPlots
                        + " private plot(s) across "
                        + safeCycles
                        + " cycle(s).",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        if (paymentMode == CivSavedData.TaxPaymentMode.KARMA) {
            viewer.sendSystemMessage(Component.literal(
                    "Paid " + RealCivUtil.formatCredits(karmaCost)
                            + " upkeep tax for "
                            + affectedPlots
                            + " plot(s). New balance: "
                            + RealCivUtil.formatCredits(record.socialCreditCents(civilizationId))
                            + "."));
        } else {
            viewer.sendSystemMessage(Component.literal(
                    "Paid " + itemCost + "x " + taxItemId
                            + " upkeep tax for "
                            + affectedPlots
                            + " plot(s)."));
        }
    }

    private void adjustTaxMultiplier(double delta) {
        if (!canManage) {
            RealCivMessages.deny(viewer, "Only civilization leadership can adjust the tax rate.");
            return;
        }
        double current = data.upkeepRateMultiplier(civilizationId);
        double next = current + delta;
        if (!data.setUpkeepRateMultiplier(civilizationId, next, viewer.getGameProfile().getName())) {
            RealCivMessages.deny(viewer, "Tax rate is already at this value.");
            return;
        }
        double applied = data.upkeepRateMultiplier(civilizationId);
        viewer.sendSystemMessage(Component.literal(
                "Tax rate multiplier updated to "
                        + String.format(java.util.Locale.ROOT, "%.2f", applied)
                        + "x (per-plot upkeep: "
                        + RealCivUtil.formatCredits(data.upkeepCostPerPlotCents(civilizationId))
                        + ")."));
    }

    private void toggleTaxPaymentMode() {
        if (!canManage) {
            RealCivMessages.deny(viewer, "Only civilization leadership can change tax payment mode.");
            return;
        }
        CivSavedData.TaxPaymentMode current = data.taxPaymentMode(civilizationId);
        CivSavedData.TaxPaymentMode next = current == CivSavedData.TaxPaymentMode.KARMA
                ? CivSavedData.TaxPaymentMode.ITEM
                : CivSavedData.TaxPaymentMode.KARMA;
        if (!data.setTaxPaymentMode(civilizationId, next, viewer.getGameProfile().getName())) {
            RealCivMessages.deny(viewer, "Tax payment mode is already set.");
            return;
        }
        viewer.sendSystemMessage(Component.literal("Tax payment mode set to " + next.serializedName() + "."));
    }

    private void cycleTaxItemType() {
        if (!canManage) {
            RealCivMessages.deny(viewer, "Only civilization leadership can change tax item type.");
            return;
        }
        ResourceLocation current = data.taxItemId(civilizationId);
        List<ResourceLocation> options = availableTaxItems();
        if (options.isEmpty()) {
            options = List.of(
                    ResourceLocation.parse("minecraft:gold_nugget"),
                    ResourceLocation.parse("minecraft:iron_ingot"),
                    ResourceLocation.parse("minecraft:emerald"));
        }
        int currentIndex = options.indexOf(current);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % options.size();
        ResourceLocation next = options.get(nextIndex);
        if (!data.setTaxItemRule(
                civilizationId,
                next,
                data.taxItemCountPerPlot(civilizationId),
                viewer.getGameProfile().getName())) {
            return;
        }
        viewer.sendSystemMessage(Component.literal("Tax item changed to " + next + "."));
    }

    private void adjustTaxItemCount(int delta) {
        if (!canManage) {
            RealCivMessages.deny(viewer, "Only civilization leadership can change tax item count.");
            return;
        }
        int current = data.taxItemCountPerPlot(civilizationId);
        int next = Math.max(1, current + delta);
        ResourceLocation itemId = data.taxItemId(civilizationId);
        if (!data.setTaxItemRule(civilizationId, itemId, next, viewer.getGameProfile().getName())) {
            RealCivMessages.deny(viewer, "Tax item count is already set.");
            return;
        }
        viewer.sendSystemMessage(Component.literal("Tax item count set to " + next + " per plot per cycle."));
    }

    private List<ResourceLocation> availableTaxItems() {
        List<ResourceLocation> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : data.getHubStockEntriesSorted(civilizationId)) {
            try {
                ResourceLocation id = ResourceLocation.parse(entry.getKey());
                if (!items.contains(id)) {
                    items.add(id);
                }
            } catch (Exception ignored) {
            }
        }
        if (!items.contains(ResourceLocation.parse("minecraft:gold_nugget"))) {
            items.add(ResourceLocation.parse("minecraft:gold_nugget"));
        }
        if (!items.contains(ResourceLocation.parse("minecraft:iron_ingot"))) {
            items.add(ResourceLocation.parse("minecraft:iron_ingot"));
        }
        if (!items.contains(ResourceLocation.parse("minecraft:emerald"))) {
            items.add(ResourceLocation.parse("minecraft:emerald"));
        }
        return items;
    }

    private void sendMemberTaxStatus(UUID memberId) {
        TaxStats stats = taxStats(memberId);
        String name = displayName(memberId);
        long balance = data.getOrCreatePlayer(memberId).socialCreditCents(civilizationId);
        CivSavedData.TaxPaymentMode paymentMode = data.taxPaymentMode(civilizationId);
        String modeText = paymentMode == CivSavedData.TaxPaymentMode.KARMA
                ? "cycle cost=" + RealCivUtil.formatCredits(stats.cycleCostCents())
                : "cycle cost=" + stats.cycleItemCost() + "x " + data.taxItemId(civilizationId);
        viewer.sendSystemMessage(Component.literal(
                "Tax status for " + name + ": plots=" + stats.ownedPlots()
                        + " | delinquent=" + stats.delinquentPlots()
                        + " | next tick=" + stats.nextUpkeepTick()
                        + " | " + modeText
                        + " | balance=" + RealCivUtil.formatCredits(balance)));
    }

    private long countInventoryItem(Item item) {
        long total = 0L;
        for (ItemStack stack : viewer.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : viewer.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return Math.max(0L, total);
    }

    private void removeInventoryItem(Item item, long count) {
        long remaining = Math.max(0L, count);
        if (remaining <= 0L) {
            return;
        }
        for (ItemStack stack : viewer.getInventory().items) {
            if (remaining <= 0L) {
                break;
            }
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int remove = (int) Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }
        for (ItemStack stack : viewer.getInventory().offhand) {
            if (remaining <= 0L) {
                break;
            }
            if (stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            int remove = (int) Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }
    }

    private void refresh() {
        memberSlots.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }

        TaxStats ownStats = taxStats(viewer.getUUID());
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        @Nullable CivSavedData.CivilizationRecord civ = data.getCivilization(civilizationId);
        String civName = civ == null ? civilizationId : civ.displayName();
        CivSavedData.TaxPaymentMode paymentMode = data.taxPaymentMode(civilizationId);
        ResourceLocation taxItemId = data.taxItemId(civilizationId);
        int taxItemCount = data.taxItemCountPerPlot(civilizationId);
        long taxItemPerPlotCurrentRate = data.taxItemCostPerPlotCurrentRate(civilizationId);

        display.setItem(
                0,
                named(
                        Items.BOOK,
                        "Tax Office | " + civName
                                + " | plots " + ownStats.ownedPlots()
                                + " | delinquent " + ownStats.delinquentPlots()));
        display.setItem(
                1,
                named(
                        Items.EMERALD,
                        "Your Karma: " + RealCivUtil.formatCredits(record.socialCreditCents(civilizationId))));
        display.setItem(
                PAY_ONE_SLOT,
                named(
                        Items.GOLD_NUGGET,
                        paymentMode == CivSavedData.TaxPaymentMode.KARMA
                                ? "Pay 1 Cycle | Cost " + RealCivUtil.formatCredits(ownStats.cycleCostCents())
                                : "Pay 1 Cycle | Cost " + ownStats.cycleItemCost() + "x " + taxItemId));
        display.setItem(
                PAY_FIVE_SLOT,
                named(
                        Items.GOLD_INGOT,
                        paymentMode == CivSavedData.TaxPaymentMode.KARMA
                                ? "Pay 5 Cycles | Cost " + RealCivUtil.formatCredits(ownStats.cycleCostCents() * 5L)
                                : "Pay 5 Cycles | Cost " + (ownStats.cycleItemCost() * 5L) + "x " + taxItemId));
        display.setItem(
                PAY_TWENTY_FIVE_SLOT,
                named(
                        Items.GOLD_BLOCK,
                        paymentMode == CivSavedData.TaxPaymentMode.KARMA
                                ? "Pay 25 Cycles | Cost " + RealCivUtil.formatCredits(ownStats.cycleCostCents() * 25L)
                                : "Pay 25 Cycles | Cost " + (ownStats.cycleItemCost() * 25L) + "x " + taxItemId));
        display.setItem(
                5,
                named(
                        Items.CHEST,
                        "Civ Treasury: " + RealCivUtil.formatCredits(data.civTreasuryCents(civilizationId))));
        display.setItem(
                6,
                named(
                        Items.CLOCK,
                        "Per-Plot Tax: "
                                + (paymentMode == CivSavedData.TaxPaymentMode.KARMA
                                ? RealCivUtil.formatCredits(data.upkeepCostPerPlotCents(civilizationId))
                                : taxItemPerPlotCurrentRate + "x " + taxItemId)
                                + " | Next tick: " + ownStats.nextUpkeepTick()));

        if (canManage) {
            double multiplier = data.upkeepRateMultiplier(civilizationId);
            display.setItem(
                    RATE_DOWN_SLOT,
                    named(
                            Items.REDSTONE,
                            "Tax Rate -10% | Now " + String.format(java.util.Locale.ROOT, "%.2f", multiplier) + "x"));
            display.setItem(
                    RATE_UP_SLOT,
                    named(
                            Items.REDSTONE_TORCH,
                            "Tax Rate +10% | Now " + String.format(java.util.Locale.ROOT, "%.2f", multiplier) + "x"));
            display.setItem(
                    MODE_TOGGLE_SLOT,
                    named(
                            Items.COMPARATOR,
                            "Tax Mode: " + paymentMode.serializedName() + " | Click to toggle"));
            display.setItem(
                    ITEM_CYCLE_SLOT,
                    named(
                            Items.HOPPER,
                            "Tax Item: " + taxItemId + " | Click to cycle"));
            display.setItem(
                    ITEM_COUNT_DOWN_SLOT,
                    named(
                            Items.STONE_BUTTON,
                            "Tax Item Count -1 | Now " + taxItemCount + "/plot"));
            display.setItem(
                    ITEM_COUNT_UP_SLOT,
                    named(
                            Items.OAK_BUTTON,
                            "Tax Item Count +1 | Now " + taxItemCount + "/plot"));
        } else {
            display.setItem(RATE_DOWN_SLOT, named(Items.BARRIER, "Only leadership can adjust tax rate."));
            display.setItem(RATE_UP_SLOT, named(Items.BARRIER, "Only leadership can adjust tax rate."));
            display.setItem(MODE_TOGGLE_SLOT, named(Items.BARRIER, "Only leadership can change tax mode."));
            display.setItem(ITEM_CYCLE_SLOT, named(Items.BARRIER, "Only leadership can change tax item."));
            display.setItem(ITEM_COUNT_DOWN_SLOT, named(Items.BARRIER, "Only leadership can change item count."));
            display.setItem(ITEM_COUNT_UP_SLOT, named(Items.BARRIER, "Only leadership can change item count."));
        }

        List<UUID> members = data.civilizationMembersSorted(civilizationId);
        totalMemberPages = Math.max(1, (members.size() + MEMBERS_COUNT - 1) / MEMBERS_COUNT);
        memberPage = Math.max(0, Math.min(memberPage, totalMemberPages - 1));

        int start = memberPage * MEMBERS_COUNT;
        int end = Math.min(members.size(), start + MEMBERS_COUNT);
        int totalDelinquentPlots = 0;
        long totalEstimatedOwedKarma = 0L;
        long totalEstimatedOwedItems = 0L;
        for (int idx = start; idx < end; idx++) {
            UUID memberId = members.get(idx);
            TaxStats memberStats = taxStats(memberId);
            CivSavedData.PlayerRecord memberRecord = data.getOrCreatePlayer(memberId);
            totalDelinquentPlots += Math.max(0, memberStats.delinquentPlots());
            totalEstimatedOwedKarma += Math.max(0L, memberStats.delinquentPlots() * data.upkeepCostPerPlotCents(civilizationId));
            totalEstimatedOwedItems += Math.max(0L, memberStats.delinquentPlots() * taxItemPerPlotCurrentRate);
            String owedEstimate = paymentMode == CivSavedData.TaxPaymentMode.KARMA
                    ? RealCivUtil.formatCredits(memberStats.delinquentPlots() * data.upkeepCostPerPlotCents(civilizationId))
                    : (memberStats.delinquentPlots() * taxItemPerPlotCurrentRate) + "x " + taxItemId;
            int slot = MEMBERS_START + (idx - start);

            ItemStack icon;
            if (data.isMayor(civilizationId, memberId)) {
                icon = new ItemStack(Items.NETHER_STAR);
            } else if (memberStats.delinquentPlots() > 0) {
                icon = new ItemStack(Items.RED_DYE);
            } else if (memberStats.ownedPlots() > 0) {
                icon = new ItemStack(Items.LIME_DYE);
            } else {
                icon = new ItemStack(Items.GRAY_DYE);
            }
            icon.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(
                            displayName(memberId)
                                    + " | plots " + memberStats.ownedPlots()
                                    + " | delinquent " + memberStats.delinquentPlots()
                                    + " | est owed " + owedEstimate
                                    + " | balance " + RealCivUtil.formatCredits(memberRecord.socialCreditCents(civilizationId))
                                    + (canManage ? " | click for details" : "")));
            display.setItem(slot, icon);
            memberSlots.put(slot, memberId);
        }

        for (int i = 45; i < SLOT_COUNT; i++) {
            if (display.getItem(i).isEmpty()) {
                display.setItem(i, named(Items.GRAY_STAINED_GLASS_PANE, " "));
            }
        }
        display.setItem(PREV_SLOT, named(Items.ARROW, "Previous Members Page"));
        display.setItem(NEXT_SLOT, named(Items.ARROW, "Next Members Page"));
        display.setItem(
                INFO_SLOT,
                named(
                        Items.PAPER,
                        "Tax Members page " + (memberPage + 1) + "/" + totalMemberPages
                                + " | mode " + paymentMode.serializedName()
                                + " | delinquent plots " + totalDelinquentPlots
                                + " | est owed "
                                + (paymentMode == CivSavedData.TaxPaymentMode.KARMA
                                ? RealCivUtil.formatCredits(totalEstimatedOwedKarma)
                                : totalEstimatedOwedItems + "x " + taxItemId)
                                + " | " + (canManage ? "leadership controls enabled" : "member view")));

        this.broadcastChanges();
    }

    private TaxStats taxStats(UUID playerId) {
        int ownedPlots = data.privatePlotCountForOwner(civilizationId, playerId);
        int delinquentPlots = data.delinquentPrivatePlotCountForOwner(civilizationId, playerId);
        long nextTick = data.earliestPrivatePlotUpkeepTick(civilizationId, playerId);
        long cycleCost = data.upkeepCostPerPlotCents(civilizationId) * ownedPlots;
        long cycleItemCost = data.taxItemCostPerPlotCurrentRate(civilizationId) * ownedPlots;
        return new TaxStats(ownedPlots, delinquentPlots, nextTick, cycleCost, cycleItemCost);
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

    private static ItemStack named(net.minecraft.world.level.ItemLike item, String text) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(text));
        return stack;
    }

    private record TaxStats(
            int ownedPlots,
            int delinquentPlots,
            long nextUpkeepTick,
            long cycleCostCents,
            long cycleItemCost) {
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
