package com.realciv.realciv.hub;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.logic.HubResetResolver;
import com.realciv.realciv.logic.HubRewardResolver;
import com.realciv.realciv.logic.Profession;
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

    public String civilizationId() {
        return civilizationId;
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

        CivSavedData data = CivSavedData.get(serverPlayer.getServer());
        boolean grantDepositGeneralXp = RealCivConfig.hubDepositGeneralXpEnabled();
        int acceptedItemCount = 0;
        int acceptedWithoutRewardCount = 0;
        long earnedCreditsCents = 0L;
        int earnedGeneralXp = 0;
        int restoredActionsTotal = 0;

        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack copy = stack.copy();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(copy.getItem());
            RewardRule rewardRule = HubRewardResolver.resolveEffectiveRewardRule(copy);
            int count = copy.getCount();
            if (rewardRule == null) {
                // Accept unmatched items into hub stock with no profession reward/reset.
                rewardRule = new RewardRule(itemId, Profession.NONE, 0L, 0, 0);
                acceptedWithoutRewardCount += count;
            }
            acceptedItemCount += count;
            earnedCreditsCents += rewardRule.creditsPerItemCents() * count;
            if (grantDepositGeneralXp) {
                earnedGeneralXp += rewardRule.generalXpPerItem() * count;
            }
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

            setItem(i, ItemStack.EMPTY);
        }

        if (acceptedItemCount > 0) {
            if (grantDepositGeneralXp && earnedGeneralXp > 0) {
                serverPlayer.giveExperiencePoints(earnedGeneralXp);
            }
            serverPlayer.sendSystemMessage(Component.literal(
                    "Community Hub accepted " + acceptedItemCount + " items. +"
                            + RealCivUtil.formatCredits(earnedCreditsCents)
                            + " contribution karma. Restored " + restoredActionsTotal + " profession action(s)."));
        }
        if (acceptedWithoutRewardCount > 0) {
            serverPlayer.sendSystemMessage(Component.literal(
                    acceptedWithoutRewardCount
                            + " accepted item(s) had no profession mapping, so they granted no rewards or action reset."));
        }
    }
}
