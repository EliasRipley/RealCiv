package com.realciv.realciv.hub;

import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.CivPermissionService;
import com.realciv.realciv.logic.RealCivMessages;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RationDraftContainer extends SimpleContainer {
    public static final int DRAFT_SLOT_COUNT = 54;

    private final String civilizationId;
    private boolean applyRequested;
    private boolean processed;

    public RationDraftContainer(String civilizationId) {
        super(DRAFT_SLOT_COUNT);
        this.civilizationId = civilizationId;
    }

    public String civilizationId() {
        return civilizationId;
    }

    public void markApplyRequested() {
        applyRequested = true;
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }
        if (processed) {
            return;
        }
        processed = true;

        if (applyRequested) {
            applyDraft(serverPlayer);
        }
        returnDraftItems(serverPlayer);
    }

    private void applyDraft(ServerPlayer player) {
        CivSavedData data = CivSavedData.get(player.getServer());
        if (!CivPermissionService.hasCivPermission(
                player,
                data,
                civilizationId,
                CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            RealCivMessages.deny(player, "You do not have permission to manage ration allowances.");
            return;
        }

        Map<ResourceLocation, Integer> drafted = new LinkedHashMap<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                continue;
            }
            int amount = Math.max(1, stack.getCount());
            drafted.merge(itemId, amount, Integer::sum);
        }

        String actor = player.getGameProfile().getName();
        int changed = data.clearAllHubDailyAllowanceLimits(civilizationId, actor);
        for (Map.Entry<ResourceLocation, Integer> entry : drafted.entrySet()) {
            if (data.setHubDailyAllowanceLimit(
                    civilizationId,
                    entry.getKey(),
                    entry.getValue(),
                    actor)) {
                changed++;
            }
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Ration draft applied for " + civilizationId + ": "
                        + drafted.size() + " item(s), " + changed + " change(s)."));
    }

    private void returnDraftItems(ServerPlayer player) {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
            setItem(i, ItemStack.EMPTY);
        }
        player.inventoryMenu.broadcastFullState();
    }
}
