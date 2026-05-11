package com.realciv.realciv.hub;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.HubResetResolver;
import com.realciv.realciv.logic.HubRewardResolver;
import com.realciv.realciv.logic.RealCivMessages;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CommunityHubDepositContainer extends SimpleContainer {
    private final String civilizationId;
    private boolean processed;

    public CommunityHubDepositContainer(String civilizationId) {
        super(54);
        this.civilizationId = civilizationId;
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);

        if (processed) {
            return;
        }
        processed = true;

        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }

        CivSavedData data = CivSavedData.get(serverPlayer.getServer());
        int acceptedItemCount = 0;
        long earnedCreditsCents = 0L;
        int earnedGeneralXp = 0;
        int restoredActionsTotal = 0;
        int returnedStackCount = 0;

        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack copy = stack.copy();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(copy.getItem());
            RewardRule rewardRule = HubRewardResolver.resolveEffectiveRewardRule(copy);

            if (rewardRule != null) {
                int count = copy.getCount();
                acceptedItemCount += count;
                earnedCreditsCents += rewardRule.creditsPerItemCents() * count;
                earnedGeneralXp += rewardRule.generalXpPerItem() * count;
                int restoredActions = HubResetResolver.resolveActionsRestored(copy, rewardRule.profession());
                restoredActionsTotal += Math.max(0, restoredActions);
                data.applyDeposit(
                        civilizationId,
                        serverPlayer.getUUID(),
                        itemId,
                        count,
                        rewardRule,
                        restoredActions,
                        serverPlayer.getGameProfile().getName());
            } else {
                if (!serverPlayer.getInventory().add(copy)) {
                    serverPlayer.drop(copy, false);
                }
                returnedStackCount++;
            }

            setItem(i, ItemStack.EMPTY);
        }

        if (acceptedItemCount > 0) {
            serverPlayer.giveExperiencePoints(earnedGeneralXp);
            serverPlayer.sendSystemMessage(Component.literal(
                    "Community Hub accepted " + acceptedItemCount + " items. +"
                            + RealCivUtil.formatCredits(earnedCreditsCents)
                            + " contribution karma. Restored " + restoredActionsTotal + " profession action(s)."));
        }

        if (returnedStackCount > 0) {
            RealCivMessages.deny(
                    serverPlayer,
                    "Some items are not accepted by Community Hub rules. "
                            + returnedStackCount + " stack(s) were returned.");
        }
    }
}
