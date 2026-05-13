package com.realciv.realciv.hub;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.HubRewardResolver;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class CommunityHubStockMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 54;
    private static final int STOCK_START_SLOT = 9;
    private static final int STOCK_PAGE_SLOTS = 36;

    private static final int MODE_TOGGLE_SLOT = 0;
    private static final int SHARED_RATIO_DOWN_SLOT = 1;
    private static final int SHARED_RATIO_UP_SLOT = 2;
    private static final int DAILY_ITEM_CYCLE_SLOT = 3;
    private static final int DAILY_COUNT_DOWN_SLOT = 4;
    private static final int DAILY_COUNT_UP_SLOT = 5;
    private static final int POLICY_INFO_SLOT = 6;
    private static final int DAYS_SUPPLY_SLOT = 7;
    private static final int REFRESH_SLOT = 8;

    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final net.minecraft.world.SimpleContainer display = new net.minecraft.world.SimpleContainer(SLOT_COUNT);
    private final ServerPlayer viewer;
    private final CivSavedData data;
    private final String civilizationId;
    private final boolean privileged;
    private final boolean canManagePolicy;
    private final Map<Integer, ResourceLocation> slotItems = new HashMap<>();
    private int pageIndex;
    private int totalPages = 1;
    private ResourceLocation selectedDailyItem = ResourceLocation.parse("minecraft:bread");

    public CommunityHubStockMenu(
            int containerId,
            Inventory inventory,
            ServerPlayer viewer,
            CivSavedData data,
            String civilizationId,
            boolean privileged,
            boolean canManagePolicy) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = viewer;
        this.data = data;
        this.civilizationId = civilizationId;
        this.privileged = privileged;
        this.canManagePolicy = canManagePolicy;

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
            if (pageIndex > 0) {
                pageIndex--;
                refresh();
            }
            return;
        }
        if (slotId == NEXT_SLOT) {
            if (pageIndex + 1 < totalPages) {
                pageIndex++;
                refresh();
            }
            return;
        }
        if (slotId == INFO_SLOT || slotId == REFRESH_SLOT) {
            refresh();
            return;
        }

        if (slotId < STOCK_START_SLOT) {
            handlePolicyControl(slotId, button, clickType);
            refresh();
            return;
        }

        ResourceLocation itemId = slotItems.get(slotId);
        if (itemId == null) {
            return;
        }

        withdrawFromSlot(itemId, button, clickType);
    }

    private void handlePolicyControl(int slotId, int button, ClickType clickType) {
        if (!canManagePolicy) {
            RealCivMessages.deny(viewer, "Only civilization leadership can manage withdrawal policy.");
            return;
        }

        switch (slotId) {
            case MODE_TOGGLE_SLOT -> toggleDistributionMode();
            case SHARED_RATIO_DOWN_SLOT -> adjustSharedRatio(-0.05D);
            case SHARED_RATIO_UP_SLOT -> adjustSharedRatio(0.05D);
            case DAILY_ITEM_CYCLE_SLOT -> cycleDailyAllowanceItem();
            case DAILY_COUNT_DOWN_SLOT -> adjustDailyAllowanceCount(-adjustStep(button, clickType));
            case DAILY_COUNT_UP_SLOT -> adjustDailyAllowanceCount(adjustStep(button, clickType));
            default -> {
            }
        }
    }

    private void toggleDistributionMode() {
        CivSavedData.HubDistributionMode current = data.hubDistributionMode(civilizationId);
        CivSavedData.HubDistributionMode next = switch (current) {
            case CONTRIBUTION_RATIO -> CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO;
            case SHARED_STOCK_RATIO -> CivSavedData.HubDistributionMode.DAILY_ALLOWANCE;
            case DAILY_ALLOWANCE -> CivSavedData.HubDistributionMode.CONTRIBUTION_RATIO;
        };
        if (!data.setHubDistributionMode(civilizationId, next, viewer.getGameProfile().getName())) {
            return;
        }
        viewer.sendSystemMessage(Component.literal("Hub withdrawal policy set to " + next.serializedName() + "."));
    }

    private void adjustSharedRatio(double delta) {
        double current = data.hubSharedWithdrawRatio(civilizationId);
        double next = Math.max(0.0D, Math.min(1.0D, current + delta));
        if (!data.setHubSharedWithdrawRatio(civilizationId, next, viewer.getGameProfile().getName())) {
            return;
        }
        viewer.sendSystemMessage(Component.literal(
                "Shared-stock ratio set to " + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civilizationId)) + "."));
    }

    private void cycleDailyAllowanceItem() {
        List<ResourceLocation> options = availablePolicyItems();
        if (options.isEmpty()) {
            selectedDailyItem = ResourceLocation.parse("minecraft:bread");
            return;
        }
        int idx = options.indexOf(selectedDailyItem);
        int nextIdx = idx < 0 ? 0 : (idx + 1) % options.size();
        selectedDailyItem = options.get(nextIdx);
        viewer.sendSystemMessage(Component.literal("Selected allowance item: " + selectedDailyItem + "."));
    }

    private void adjustDailyAllowanceCount(int delta) {
        if (delta == 0) {
            return;
        }
        int current = data.hubDailyAllowanceLimit(civilizationId, selectedDailyItem);
        int next = Math.max(0, current + delta);
        if (!data.setHubDailyAllowanceLimit(civilizationId, selectedDailyItem, next, viewer.getGameProfile().getName())) {
            return;
        }
        if (next <= 0) {
            viewer.sendSystemMessage(Component.literal("Cleared daily allowance for " + selectedDailyItem + "."));
        } else {
            viewer.sendSystemMessage(Component.literal(
                    "Daily allowance for " + selectedDailyItem + " set to " + next + "/day."));
        }
    }

    private int adjustStep(int button, ClickType clickType) {
        if (clickType == ClickType.QUICK_MOVE) {
            return 64;
        }
        if (clickType == ClickType.PICKUP && button == 1) {
            return 8;
        }
        return 1;
    }

    private void withdrawFromSlot(ResourceLocation itemId, int button, ClickType clickType) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            refresh();
            return;
        }

        long stock = data.getHubStock(civilizationId, itemId);
        if (stock <= 0L) {
            refresh();
            return;
        }

        long requested = requestSize(item, button, clickType);
        if (requested <= 0L) {
            return;
        }

        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());
        CivSavedData.HubDistributionMode distributionMode = data.hubDistributionMode(civilizationId);
        long activeModeLimit = 0L;
        if (!privileged) {
            long allowance;
            if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
                activeModeLimit = data.hubDailyAllowanceLimit(civilizationId, itemId);
                if (activeModeLimit <= 0L) {
                    RealCivMessages.deny(
                            viewer,
                            "No daily allowance is configured for " + itemId + " in this civilization.");
                    refresh();
                    return;
                }
                allowance = record.remainingDailyAllowance(civilizationId, itemId, activeModeLimit);
            } else if (distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                activeModeLimit = data.hubSharedStockDailyLimit(civilizationId, itemId);
                allowance = record.remainingDailyAllowance(civilizationId, itemId, activeModeLimit);
            } else {
                allowance = record.remainingPersonalWithdraw(civilizationId, itemId);
            }
            if (allowance <= 0L) {
                if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
                    RealCivMessages.deny(
                            viewer,
                            "No daily allowance remaining for " + itemId + ".");
                } else if (distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                    RealCivMessages.deny(
                            viewer,
                            "No shared-stock allowance remaining for " + itemId
                                    + " today. Current ratio: "
                                    + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civilizationId)) + ".");
                } else {
                    RealCivMessages.deny(
                            viewer,
                            "No personal withdrawal allowance left for " + itemId
                                    + ". Contribute more to increase quota.");
                }
                refresh();
                return;
            }
            requested = Math.min(requested, allowance);
        }

        requested = Math.min(requested, stock);
        if (requested <= 0L) {
            refresh();
            return;
        }

        long penaltyCents = 0L;
        double penaltyRatio = RealCivConfig.hubWithdrawCreditPenaltyRatio();
        if (penaltyRatio > 0.0D) {
            RewardRule rule = HubRewardResolver.resolveEffectiveRewardRule(new ItemStack(item, 1));
            if (rule != null) {
                penaltyCents = Math.round(rule.creditsPerItemCents() * requested * penaltyRatio);
            }
        }

        if (penaltyCents > 0L && record.socialCreditCents(civilizationId) < penaltyCents) {
            RealCivMessages.deny(
                    viewer,
                    "Not enough contribution karma for this withdrawal. Need "
                            + RealCivUtil.formatCredits(penaltyCents) + " for withdrawal penalty.");
            refresh();
            return;
        }

        if (!data.tryWithdrawFromHub(civilizationId, itemId, requested)) {
            RealCivMessages.deny(viewer, "Hub stock changed before withdrawal could complete. Try again.");
            refresh();
            return;
        }

        long remainingToGive = requested;
        while (remainingToGive > 0L) {
            int stackSize = (int) Math.min(item.getDefaultMaxStackSize(), remainingToGive);
            ItemStack stack = new ItemStack(item, stackSize);
            if (!viewer.getInventory().add(stack)) {
                viewer.drop(stack, false);
            }
            remainingToGive -= stackSize;
        }

        long remainingPersonal = 0L;
        if (!privileged) {
            if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE
                    || distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
                record.recordDailyAllowanceWithdrawal(civilizationId, itemId, requested);
                remainingPersonal = record.remainingDailyAllowance(civilizationId, itemId, activeModeLimit);
            } else {
                record.recordPersonalWithdrawal(civilizationId, itemId, requested);
                remainingPersonal = record.remainingPersonalWithdraw(civilizationId, itemId);
            }
        }
        if (penaltyCents > 0L) {
            record.addSocialCreditCents(civilizationId, -penaltyCents);
        }

        data.addAuditLog(
                civilizationId,
                viewer.getGameProfile().getName() + " withdrew " + requested + "x " + itemId + " via hub menu",
                RealCivConfig.MAX_AUDIT_LOGS.get());
        data.setDirty();

        if (privileged) {
            viewer.sendSystemMessage(Component.literal(
                    "Withdrew " + requested + "x " + itemId
                            + (penaltyCents > 0L
                            ? " | Credit penalty: -" + RealCivUtil.formatCredits(penaltyCents)
                            : "")));
        } else if (distributionMode == CivSavedData.HubDistributionMode.DAILY_ALLOWANCE) {
            long finalModeLimit = activeModeLimit;
            viewer.sendSystemMessage(Component.literal(
                    "Withdrew " + requested + "x " + itemId
                            + " | Remaining daily allowance: " + remainingPersonal + "/" + finalModeLimit
                            + (penaltyCents > 0L
                            ? " | Credit penalty: -" + RealCivUtil.formatCredits(penaltyCents)
                            : "")));
        } else if (distributionMode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
            long finalModeLimit = activeModeLimit;
            viewer.sendSystemMessage(Component.literal(
                    "Withdrew " + requested + "x " + itemId
                            + " | Remaining shared allowance today: " + remainingPersonal + "/" + finalModeLimit
                            + (penaltyCents > 0L
                            ? " | Credit penalty: -" + RealCivUtil.formatCredits(penaltyCents)
                            : "")));
        } else {
            viewer.sendSystemMessage(Component.literal(
                    "Withdrew " + requested + "x " + itemId
                            + " | Remaining personal quota: " + remainingPersonal
                            + (penaltyCents > 0L
                            ? " | Credit penalty: -" + RealCivUtil.formatCredits(penaltyCents)
                            : "")));
        }
        refresh();
    }

    private long requestSize(Item item, int button, ClickType clickType) {
        int maxStack = Math.max(1, item.getDefaultMaxStackSize());
        if (clickType == ClickType.QUICK_MOVE) {
            return (long) maxStack * 4L;
        }
        if (clickType == ClickType.PICKUP && button == 1) {
            return 1L;
        }
        return maxStack;
    }

    private void refresh() {
        slotItems.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }

        ensureSelectedDailyItemValid();

        CivSavedData.HubDistributionMode distributionMode = data.hubDistributionMode(civilizationId);
        double sharedRatio = data.hubSharedWithdrawRatio(civilizationId);
        CivSavedData.PlayerRecord record = data.getOrCreatePlayer(viewer.getUUID());

        if (canManagePolicy) {
            display.setItem(
                    MODE_TOGGLE_SLOT,
                    named(Items.COMPARATOR, "Policy: " + distributionMode.serializedName() + " | click to cycle"));
            display.setItem(
                    SHARED_RATIO_DOWN_SLOT,
                    named(Items.REDSTONE, "Shared ratio -5% | " + RealCivUtil.formatPercentFromRatio(sharedRatio)));
            display.setItem(
                    SHARED_RATIO_UP_SLOT,
                    named(Items.REDSTONE_TORCH, "Shared ratio +5% | " + RealCivUtil.formatPercentFromRatio(sharedRatio)));
            display.setItem(
                    DAILY_ITEM_CYCLE_SLOT,
                    named(Items.HOPPER, "Allowance item: " + selectedDailyItem + " | click to cycle"));
            int selectedDailyCount = data.hubDailyAllowanceLimit(civilizationId, selectedDailyItem);
            display.setItem(
                    DAILY_COUNT_DOWN_SLOT,
                    named(Items.STONE_BUTTON, "Allowance count -" + " | now " + selectedDailyCount + "/day"));
            display.setItem(
                    DAILY_COUNT_UP_SLOT,
                    named(Items.OAK_BUTTON, "Allowance count +" + " | now " + selectedDailyCount + "/day"));
        } else {
            display.setItem(MODE_TOGGLE_SLOT, named(Items.BARRIER, "Only leadership can change withdrawal policy."));
            display.setItem(SHARED_RATIO_DOWN_SLOT, named(Items.BARRIER, "Only leadership can change shared ratio."));
            display.setItem(SHARED_RATIO_UP_SLOT, named(Items.BARRIER, "Only leadership can change shared ratio."));
            display.setItem(DAILY_ITEM_CYCLE_SLOT, named(Items.BARRIER, "Only leadership can set daily allowances."));
            display.setItem(DAILY_COUNT_DOWN_SLOT, named(Items.BARRIER, "Only leadership can set daily allowances."));
            display.setItem(DAILY_COUNT_UP_SLOT, named(Items.BARRIER, "Only leadership can set daily allowances."));
        }

        display.setItem(POLICY_INFO_SLOT, named(Items.BOOK, policySummary(distributionMode)));
        display.setItem(DAYS_SUPPLY_SLOT, named(Items.CLOCK, daysSupplySummary(distributionMode)));
        display.setItem(REFRESH_SLOT, named(Items.PAPER, "Refresh"));

        List<Map.Entry<String, Long>> stockEntries = data.getHubStockEntriesSorted(civilizationId);
        int pageSize = STOCK_PAGE_SLOTS;
        totalPages = Math.max(1, (stockEntries.size() + pageSize - 1) / pageSize);
        pageIndex = Math.max(0, Math.min(pageIndex, totalPages - 1));

        int start = pageIndex * pageSize;
        int end = Math.min(stockEntries.size(), start + pageSize);

        int slot = STOCK_START_SLOT;
        for (int i = start; i < end; i++) {
            Map.Entry<String, Long> entry = stockEntries.get(i);
            ResourceLocation itemId;
            try {
                itemId = ResourceLocation.parse(entry.getKey());
            } catch (Exception ignored) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) {
                continue;
            }

            long stock = Math.max(0L, entry.getValue());
            ItemStack stack = new ItemStack(item, (int) Math.min(Math.max(1L, stock), item.getDefaultMaxStackSize()));

            if (privileged) {
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                        itemId + " | stock " + stock
                                + " | Left click: stack, Right click: 1, Shift click: x4 stacks"));
            } else {
                switch (distributionMode) {
                    case DAILY_ALLOWANCE -> {
                        long limit = data.hubDailyAllowanceLimit(civilizationId, itemId);
                        long withdrawn = record.dailyAllowanceWithdrawnCount(civilizationId, itemId);
                        long remaining = record.remainingDailyAllowance(civilizationId, itemId, limit);
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                                itemId + " | stock " + stock
                                        + " | mode daily_allowance"
                                        + " | withdrawn today " + withdrawn
                                        + " | remaining today " + remaining + "/" + limit));
                    }
                    case SHARED_STOCK_RATIO -> {
                        long limit = data.hubSharedStockDailyLimit(civilizationId, itemId);
                        long withdrawn = record.dailyAllowanceWithdrawnCount(civilizationId, itemId);
                        long remaining = record.remainingDailyAllowance(civilizationId, itemId, limit);
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                                itemId + " | stock " + stock
                                        + " | mode shared_stock_ratio"
                                        + " | shared ratio " + RealCivUtil.formatPercentFromRatio(sharedRatio)
                                        + " | withdrawn today " + withdrawn
                                        + " | remaining today " + remaining + "/" + limit));
                    }
                    case CONTRIBUTION_RATIO -> {
                        long contributed = record.contributedCount(civilizationId, itemId);
                        long limit = record.personalWithdrawLimit(civilizationId, itemId);
                        long withdrawn = record.personalWithdrawnCount(civilizationId, itemId);
                        long remaining = record.remainingPersonalWithdraw(civilizationId, itemId);
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                                itemId + " | stock " + stock
                                        + " | contributed " + contributed
                                        + " | withdrawn " + withdrawn
                                        + " | remaining " + remaining + "/" + limit));
                    }
                }
            }

            display.setItem(slot, stack);
            slotItems.put(slot, itemId);
            slot++;
        }

        for (int i = PREV_SLOT; i < SLOT_COUNT; i++) {
            if (display.getItem(i).isEmpty()) {
                ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                display.setItem(i, pane);
            }
        }

        display.setItem(PREV_SLOT, named(Items.ARROW, "Previous Page"));
        display.setItem(NEXT_SLOT, named(Items.ARROW, "Next Page"));
        display.setItem(
                INFO_SLOT,
                named(
                        Items.BOOK,
                        "Civilization: " + civilizationId
                                + " | page " + (pageIndex + 1) + "/" + totalPages
                                + " | mode " + distributionMode.serializedName()
                                + " | Balance: " + RealCivUtil.formatCredits(record.socialCreditCents(civilizationId))));

        this.broadcastChanges();
    }

    private void ensureSelectedDailyItemValid() {
        List<ResourceLocation> options = availablePolicyItems();
        if (options.isEmpty()) {
            selectedDailyItem = ResourceLocation.parse("minecraft:bread");
            return;
        }
        if (!options.contains(selectedDailyItem)) {
            selectedDailyItem = options.get(0);
        }
    }

    private List<ResourceLocation> availablePolicyItems() {
        Set<ResourceLocation> out = new HashSet<>();
        for (Map.Entry<String, Long> entry : data.getHubStockEntriesSorted(civilizationId)) {
            try {
                out.add(ResourceLocation.parse(entry.getKey()));
            } catch (Exception ignored) {
            }
        }
        for (Map.Entry<String, Integer> entry : data.hubDailyAllowanceEntriesSorted(civilizationId)) {
            try {
                out.add(ResourceLocation.parse(entry.getKey()));
            } catch (Exception ignored) {
            }
        }
        out.add(ResourceLocation.parse("minecraft:bread"));
        out.add(ResourceLocation.parse("minecraft:wheat"));
        out.add(ResourceLocation.parse("minecraft:iron_ingot"));
        out.add(ResourceLocation.parse("minecraft:gold_nugget"));
        List<ResourceLocation> list = new ArrayList<>(out);
        list.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        return list;
    }

    private String policySummary(CivSavedData.HubDistributionMode mode) {
        return switch (mode) {
            case CONTRIBUTION_RATIO -> "Policy: personal contribution ratio quotas";
            case SHARED_STOCK_RATIO -> "Policy: shared-stock ratio (" + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civilizationId)) + ")";
            case DAILY_ALLOWANCE -> "Policy: daily item allowances";
        };
    }

    private String daysSupplySummary(CivSavedData.HubDistributionMode mode) {
        if (mode == CivSavedData.HubDistributionMode.CONTRIBUTION_RATIO) {
            return "Days supplies left: n/a in contribution-ratio mode";
        }
        if (mode == CivSavedData.HubDistributionMode.SHARED_STOCK_RATIO) {
            long stock = data.getHubStock(civilizationId, selectedDailyItem);
            long daily = data.hubSharedStockDailyLimit(civilizationId, selectedDailyItem);
            if (stock <= 0L || daily <= 0L) {
                return "Days supplies left (" + selectedDailyItem + "): 0.0";
            }
            double days = (double) stock / (double) daily;
            return String.format(
                    Locale.ROOT,
                    "Days supplies left (%s): %.1f",
                    selectedDailyItem,
                    days);
        }

        List<Map.Entry<String, Integer>> allowances = data.hubDailyAllowanceEntriesSorted(civilizationId);
        if (allowances.isEmpty()) {
            return "Days supplies left: n/a (no daily allowances configured)";
        }
        double minDays = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Integer> entry : allowances) {
            int limit = Math.max(0, entry.getValue());
            if (limit <= 0) {
                continue;
            }
            long stock = 0L;
            try {
                stock = Math.max(0L, data.getHubStock(civilizationId, ResourceLocation.parse(entry.getKey())));
            } catch (Exception ignored) {
            }
            double days = (double) stock / (double) limit;
            if (days < minDays) {
                minDays = days;
            }
        }
        if (!Double.isFinite(minDays)) {
            return "Days supplies left: n/a";
        }
        return String.format(Locale.ROOT, "Days supplies left: %.1f (min across configured allowances)", minDays);
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
