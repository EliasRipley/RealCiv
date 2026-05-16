package com.realciv.realciv.command;

import com.realciv.realciv.config.RealCivConfig;
import com.realciv.realciv.data.AttributeCategory;
import com.realciv.realciv.data.CivSavedData;
import com.realciv.realciv.data.CivicAttribute;
import com.realciv.realciv.data.PlayerRecord;
import com.realciv.realciv.hub.HubStockSnapshot;
import com.realciv.realciv.hub.HubStockSnapshotBuilder;
import com.realciv.realciv.logic.HubRewardResolver;
import com.realciv.realciv.logic.Profession;
import com.realciv.realciv.logic.RealCivUtil;
import com.realciv.realciv.logic.RewardRule;
import com.realciv.realciv.logic.TagRewardRule;
import com.realciv.realciv.network.RealCivPayloads;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class HubCommands {
    private static final TagKey<Block> PICKAXE_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/pickaxe"));
    private static final TagKey<Block> SHOVEL_MINEABLE_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:mineable/shovel"));
    private static final TagKey<Block> BAMBOO_BLOCKS_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.parse("minecraft:bamboo_blocks"));

    private HubCommands() {
    }

    public static int openHubStockMenu(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(player.getUUID());
        boolean canManage = RealCivCommands.hasCivPermission(
                source,
                data,
                civId,
                CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION);
        HubStockSnapshot snap = HubStockSnapshotBuilder.build(player, data, civId, canManage, 0);
        PacketDistributor.sendToPlayer(player, new RealCivPayloads.OpenHubStockPayload(snap));
        player.sendSystemMessage(Component.literal("Opened hub stock screen for " + RealCivCommands.civDisplay(data, civId) + "."));
        if (canManage) {
            player.sendSystemMessage(Component.literal(
                    "Leadership controls enabled: policy mode, shared ratio, and daily allowances."));
        }
        return 1;
    }

    public static int withdrawFromHub(
            CommandSourceStack source,
            ResourceLocation itemId,
            int count,
            ServerPlayer target) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = source.getEntity() instanceof ServerPlayer player
                ? data.getOrAssignCivilization(player.getUUID())
                : RealCivConfig.defaultCivilizationId();
        ServerPlayer requester = source.getEntity() instanceof ServerPlayer player ? player : null;
        boolean canTargetOthers = RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_WITHDRAWALS);
        boolean canBypassQuota = source.hasPermission(3) || (requester != null && RealCivUtil.isBypass(requester));

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            source.sendFailure(Component.literal("Unknown item: " + itemId));
            return 0;
        }

        if (!source.hasPermission(3)) {
            String targetCiv = data.getOrAssignCivilization(target.getUUID());
            if (!targetCiv.equals(civId)) {
                source.sendFailure(Component.literal(
                        "Target must belong to your civilization unless you are admin."));
                return 0;
            }
        }

        PlayerRecord quotaRecord = data.getOrCreatePlayer(target.getUUID());
        CivicAttribute distributionMode = data.civicAttribute(civId, AttributeCategory.RESOURCE);
        long activeModeLimit = 0L;
        if (!canBypassQuota) {
            if (requester == null) {
                source.sendFailure(Component.literal("Only players can use personal hub withdrawals."));
                return 0;
            }
            if (!canTargetOthers && !requester.getUUID().equals(target.getUUID())) {
                source.sendFailure(Component.literal("You can only withdraw to yourself unless you are leadership/admin."));
                return 0;
            }

            long remainingAllowance;
            if (distributionMode == CivicAttribute.RATIONED) {
                activeModeLimit = data.hubDailyAllowanceLimit(civId, itemId);
                if (activeModeLimit <= 0L) {
                    source.sendFailure(Component.literal(
                            "No daily allowance is configured for " + itemId + " in "
                                    + RealCivCommands.civDisplay(data, civId) + "."));
                    return 0;
                }
                remainingAllowance = quotaRecord.remainingDailyAllowance(civId, itemId, activeModeLimit);
            } else if (distributionMode == CivicAttribute.EQUAL_SHARE) {
                activeModeLimit = data.hubSharedStockDailyLimit(civId, itemId);
                remainingAllowance = quotaRecord.remainingDailyAllowance(civId, itemId, activeModeLimit);
            } else {
                remainingAllowance = quotaRecord.remainingPersonalWithdraw(civId, itemId);
            }
            if (remainingAllowance <= 0L) {
                if (distributionMode == CivicAttribute.RATIONED) {
                    source.sendFailure(Component.literal(
                            "No daily allowance remaining for " + itemId + " for "
                                    + target.getGameProfile().getName() + "."));
                } else if (distributionMode == CivicAttribute.EQUAL_SHARE) {
                    source.sendFailure(Component.literal(
                            "No shared-stock allowance remaining for " + itemId + " for "
                                    + target.getGameProfile().getName()
                                    + " today (ratio "
                                    + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + ")."));
                } else {
                    source.sendFailure(Component.literal(
                            "No personal withdrawal allowance left for " + itemId + " for "
                                    + target.getGameProfile().getName()
                                    + ". Contribute more to increase quota."));
                }
                return 0;
            }
            if (count > remainingAllowance) {
                if (distributionMode == CivicAttribute.RATIONED) {
                    source.sendFailure(Component.literal(
                            "You can withdraw at most " + remainingAllowance + "x " + itemId + " for "
                                    + target.getGameProfile().getName() + " right now (daily allowance)."));
                } else if (distributionMode == CivicAttribute.EQUAL_SHARE) {
                    source.sendFailure(Component.literal(
                            "You can withdraw at most " + remainingAllowance + "x " + itemId + " for "
                                    + target.getGameProfile().getName() + " right now (shared-stock allowance)."));
                } else {
                    source.sendFailure(Component.literal(
                            "You can withdraw at most " + remainingAllowance + "x " + itemId + " for "
                                    + target.getGameProfile().getName() + " right now (personal quota)."));
                }
                return 0;
            }
        }

        long penaltyCents = 0L;
        double penaltyRatio = RealCivConfig.hubWithdrawCreditPenaltyRatio();
        if (penaltyRatio > 0.0D) {
            RewardRule rule = HubRewardResolver.resolveEffectiveRewardRule(new ItemStack(item, 1));
            if (rule != null) {
                penaltyCents = Math.round(rule.creditsPerItemCents() * count * penaltyRatio);
            }
        }
        if (penaltyCents > 0L && quotaRecord.socialCreditCents(civId) < penaltyCents) {
            source.sendFailure(Component.literal(
                    target.getGameProfile().getName() + " needs "
                            + RealCivUtil.formatCredits(penaltyCents)
                            + " contribution karma for this withdrawal's credit penalty, but has "
                            + RealCivUtil.formatCredits(quotaRecord.socialCreditCents(civId)) + "."));
            return 0;
        }

        if (!data.tryWithdrawFromHub(civId, itemId, count)) {
            source.sendFailure(Component.literal(
                    "Hub stock in " + RealCivCommands.civDisplay(data, civId) + " does not contain enough of " + itemId + "."));
            return 0;
        }

        int remaining = count;
        while (remaining > 0) {
            int stackSize = Math.min(item.getDefaultMaxStackSize(), remaining);
            ItemStack stack = new ItemStack(item, stackSize);
            if (!target.getInventory().add(stack)) {
                target.drop(stack, false);
            }
            remaining -= stackSize;
        }

        long newRemaining = 0L;
        if (!canBypassQuota) {
            if (distributionMode == CivicAttribute.RATIONED
                    || distributionMode == CivicAttribute.EQUAL_SHARE) {
                quotaRecord.recordDailyAllowanceWithdrawal(civId, itemId, count);
                newRemaining = quotaRecord.remainingDailyAllowance(civId, itemId, activeModeLimit);
            } else {
                quotaRecord.recordPersonalWithdrawal(civId, itemId, count);
                newRemaining = quotaRecord.remainingPersonalWithdraw(civId, itemId);
            }
        }
        if (penaltyCents > 0L) {
            quotaRecord.addSocialCreditCents(civId, -penaltyCents);
        }
        data.setDirty();

        String actor = RealCivCommands.actorName(source);
        if (canBypassQuota) {
            data.addAuditLog(
                    civId,
                    actor + " withdrew " + count + "x " + itemId + " for " + target.getGameProfile().getName(),
                    RealCivConfig.MAX_AUDIT_LOGS.get());
        } else {
            if (distributionMode == CivicAttribute.RATIONED) {
                long finalNewRemaining = newRemaining;
                long finalConfiguredDailyLimit = activeModeLimit;
                data.addAuditLog(
                        civId,
                        actor + " withdrew " + count + "x " + itemId
                                + " from " + target.getGameProfile().getName()
                                + " daily allowance (remaining today: " + finalNewRemaining
                                + "/" + finalConfiguredDailyLimit + ")",
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                source.sendSuccess(() -> Component.literal(
                        "Daily allowance remaining for " + target.getGameProfile().getName()
                                + " on " + itemId + ": " + finalNewRemaining + "/" + finalConfiguredDailyLimit),
                        false);
            } else if (distributionMode == CivicAttribute.EQUAL_SHARE) {
                long finalNewRemaining = newRemaining;
                long finalSharedLimit = activeModeLimit;
                data.addAuditLog(
                        civId,
                        actor + " withdrew " + count + "x " + itemId
                                + " from " + target.getGameProfile().getName()
                                + " shared-stock allowance (remaining today: " + finalNewRemaining
                                + "/" + finalSharedLimit + ", ratio "
                                + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + ")",
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                source.sendSuccess(() -> Component.literal(
                        "Shared-stock allowance remaining for " + target.getGameProfile().getName()
                                + " on " + itemId + ": " + finalNewRemaining + "/" + finalSharedLimit),
                        false);
            } else {
                long finalNewRemaining = newRemaining;
                data.addAuditLog(
                        civId,
                        actor + " withdrew " + count + "x " + itemId
                                + " from " + target.getGameProfile().getName()
                                + " personal quota (remaining allowance: " + finalNewRemaining + ")",
                        RealCivConfig.MAX_AUDIT_LOGS.get());
                source.sendSuccess(() -> Component.literal(
                        "Personal quota remaining for " + target.getGameProfile().getName()
                                + " on " + itemId + ": " + finalNewRemaining), false);
            }
        }
        if (penaltyCents > 0L) {
            long appliedPenalty = penaltyCents;
            source.sendSuccess(() -> Component.literal(
                    "Withdrawal credit penalty applied to " + target.getGameProfile().getName()
                            + ": -" + RealCivUtil.formatCredits(appliedPenalty)
                            + " contribution karma."), false);
        }

        source.sendSuccess(() -> Component.literal(
                "Withdrew " + count + "x " + itemId + " to " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    public static int showHubStock(CommandSourceStack source, int page) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        List<Map.Entry<String, Long>> entries = data.getHubStockEntriesSorted(civId);
        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "Community Hub stock for " + RealCivCommands.civDisplay(data, civId) + " is empty."), false);
            return 1;
        }

        int totalPages = (entries.size() + pageSize - 1) / pageSize;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(entries.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Community Hub stock for " + RealCivCommands.civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);
        for (int i = start; i < end; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            source.sendSuccess(() -> Component.literal("- " + entry.getKey() + ": " + entry.getValue()), false);
        }
        return 1;
    }

    public static int showHubQuotaSelf(CommandSourceStack source, int page)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return showHubQuota(source, player, page);
    }

    public static int showHubQuotaFor(CommandSourceStack source, ServerPlayer target, int page) {
        if (!(source.getEntity() instanceof ServerPlayer requester) || !requester.getUUID().equals(target.getUUID())) {
            CivSavedData data = CivSavedData.get(source.getServer());
            String targetCiv = data.getOrAssignCivilization(target.getUUID());
            if (!RealCivCommands.hasCivPermission(source, data, targetCiv, CivSavedData.ROLE_PERMISSION_VIEW_HUB_QUOTAS)
                    && !source.hasPermission(3)) {
                source.sendFailure(Component.literal("You can only inspect your own quota unless you are leadership/admin."));
                return 0;
            }
        }
        return showHubQuota(source, target, page);
    }

    private static int showHubQuota(CommandSourceStack source, ServerPlayer target, int page) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(target.getUUID());
        PlayerRecord record = data.getOrCreatePlayer(target.getUUID());
        CivicAttribute mode = data.civicAttribute(civId, AttributeCategory.RESOURCE);

        if (mode == CivicAttribute.RATIONED) {
            List<Map.Entry<String, Integer>> entries = data.hubDailyAllowanceEntriesSorted(civId);
            if (entries.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                        "Hub quota for " + RealCivCommands.civDisplay(data, civId)
                                + " is in daily_allowance mode, but no item limits are configured yet."),
                        false);
                return 1;
            }

            int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
            int totalPages = (entries.size() + pageSize - 1) / pageSize;
            int safePage = Math.max(1, Math.min(page, totalPages));
            int start = (safePage - 1) * pageSize;
            int end = Math.min(entries.size(), start + pageSize);

            source.sendSuccess(() -> Component.literal(
                    "Hub quota for " + target.getGameProfile().getName()
                            + " in " + RealCivCommands.civDisplay(data, civId)
                            + " (mode daily_allowance, page " + safePage + "/" + totalPages + "):"),
                    false);

            for (int i = start; i < end; i++) {
                Map.Entry<String, Integer> entry = entries.get(i);
                String itemKey = entry.getKey();
                int limit = Math.max(0, entry.getValue());
                long withdrawn = 0L;
                long remaining = 0L;
                try {
                    net.minecraft.resources.ResourceLocation parsed = net.minecraft.resources.ResourceLocation.parse(itemKey);
                    withdrawn = record.dailyAllowanceWithdrawnCount(civId, parsed);
                    remaining = record.remainingDailyAllowance(civId, parsed, limit);
                } catch (Exception ignored) {
                }
                long safeWithdrawn = Math.max(0L, withdrawn);
                long safeRemaining = Math.max(0L, remaining);
                source.sendSuccess(() -> Component.literal(
                        "- " + itemKey
                                + " | withdrawn today " + safeWithdrawn
                                + " | remaining today " + safeRemaining + "/" + limit),
                        false);
            }
            return 1;
        }

        if (mode == CivicAttribute.EQUAL_SHARE) {
            List<Map.Entry<String, Long>> stockEntries = data.getHubStockEntriesSorted(civId);
            if (stockEntries.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                        "Hub quota for " + RealCivCommands.civDisplay(data, civId)
                                + " is in shared_stock_ratio mode, but stock is empty."),
                        false);
                return 1;
            }

            int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
            int totalPages = (stockEntries.size() + pageSize - 1) / pageSize;
            int safePage = Math.max(1, Math.min(page, totalPages));
            int start = (safePage - 1) * pageSize;
            int end = Math.min(stockEntries.size(), start + pageSize);

            source.sendSuccess(() -> Component.literal(
                    "Hub quota for " + target.getGameProfile().getName()
                            + " in " + RealCivCommands.civDisplay(data, civId)
                            + " (mode shared_stock_ratio @ "
                            + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId))
                            + ", page " + safePage + "/" + totalPages + "):"),
                    false);

            for (int i = start; i < end; i++) {
                Map.Entry<String, Long> entry = stockEntries.get(i);
                String itemKey = entry.getKey();
                long stock = Math.max(0L, entry.getValue());
                long limit = 0L;
                long withdrawn = 0L;
                long remaining = 0L;
                try {
                    net.minecraft.resources.ResourceLocation parsed = net.minecraft.resources.ResourceLocation.parse(itemKey);
                    limit = data.hubSharedStockDailyLimit(civId, parsed);
                    withdrawn = record.dailyAllowanceWithdrawnCount(civId, parsed);
                    remaining = record.remainingDailyAllowance(civId, parsed, limit);
                } catch (Exception ignored) {
                }
                long safeLimit = Math.max(0L, limit);
                long safeWithdrawn = Math.max(0L, withdrawn);
                long safeRemaining = Math.max(0L, remaining);
                source.sendSuccess(() -> Component.literal(
                        "- " + itemKey
                                + " | stock " + stock
                                + " | shared allowance " + safeRemaining + "/" + safeLimit
                                + " | withdrawn today " + safeWithdrawn),
                        false);
            }
            return 1;
        }

        List<Map.Entry<String, Long>> entries = record.contributions(civId).entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0L)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .toList();

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " has not contributed tracked items in "
                            + RealCivCommands.civDisplay(data, civId) + " yet."),
                    false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = (entries.size() + pageSize - 1) / pageSize;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(entries.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Hub quota for " + target.getGameProfile().getName()
                        + " in " + RealCivCommands.civDisplay(data, civId)
                        + " (rate " + RealCivUtil.formatPercentFromRatio(record.effectivePersonalWithdrawRatio(civId))
                        + ", page " + safePage + "/" + totalPages + "):"),
                false);

        for (int i = start; i < end; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            String itemKey = entry.getKey();
            long contributed = Math.max(0L, entry.getValue());
            long withdrawn = 0L;
            long limit = 0L;
            long remaining = 0L;

            try {
                net.minecraft.resources.ResourceLocation parsed = net.minecraft.resources.ResourceLocation.parse(itemKey);
                withdrawn = record.personalWithdrawnCount(civId, parsed);
                limit = record.personalWithdrawLimit(civId, parsed);
                remaining = record.remainingPersonalWithdraw(civId, parsed);
            } catch (Exception ignored) {
            }

            long safeWithdrawn = Math.max(0L, withdrawn);
            long safeLimit = Math.max(0L, limit);
            long safeRemaining = Math.max(0L, remaining);
            source.sendSuccess(() -> Component.literal(
                    "- " + itemKey
                            + " | contributed " + contributed
                            + " | withdrawn " + safeWithdrawn
                            + " | limit " + safeLimit
                            + " | remaining " + safeRemaining),
                    false);
        }
        return 1;
    }

    public static int hubDistributionShow(CommandSourceStack source) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        CivicAttribute mode = data.civicAttribute(civId, AttributeCategory.RESOURCE);
        source.sendSuccess(() -> Component.literal(
                "Hub distribution mode for " + RealCivCommands.civDisplay(data, civId) + ": " + mode.displayName()),
                false);

        if (mode == CivicAttribute.CONTRIBUTION_SHARE) {
            source.sendSuccess(() -> Component.literal(
                    "Contribution-ratio mode uses each player's contribution quota and personal withdraw rate."),
                    false);
            return 1;
        }
        if (mode == CivicAttribute.EQUAL_SHARE) {
            source.sendSuccess(() -> Component.literal(
                    "Shared-stock ratio mode gives each member a daily allowance per item based on current stock and shared ratio."),
                    false);
            source.sendSuccess(() -> Component.literal(
                    "Current shared ratio: " + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + "."),
                    false);
            return 1;
        }

        List<Map.Entry<String, Integer>> allowances = data.hubDailyAllowanceEntriesSorted(civId);
        if (allowances.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No daily item allowances configured yet. Use /realciv hub distribution allowance set <item> <count>."),
                    false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(
                "Configured daily allowance entries: " + allowances.size() + " item(s)."), false);
        int preview = Math.min(10, allowances.size());
        for (int i = 0; i < preview; i++) {
            Map.Entry<String, Integer> entry = allowances.get(i);
            source.sendSuccess(() -> Component.literal(
                    "- " + entry.getKey() + ": " + entry.getValue() + "/day"),
                    false);
        }
        if (allowances.size() > preview) {
            source.sendSuccess(() -> Component.literal(
                    "Use /realciv hub distribution allowance list for the full list."),
                    false);
        }
        return 1;
    }

    public static int hubDistributionSetMode(CommandSourceStack source, String modeRaw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage hub distribution mode."));
            return 0;
        }
        @Nullable CivicAttribute mode = CivicAttribute.fromSerializedName(modeRaw);
        if (mode == null || mode.category() != AttributeCategory.RESOURCE) {
            source.sendFailure(Component.literal(
                    "Unknown resource policy. Use contribution_share, equal_share, or rationed."));
            return 0;
        }
        if (!data.setCivicAttribute(civId, AttributeCategory.RESOURCE, mode, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Hub distribution mode already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Resource policy for " + RealCivCommands.civDisplay(data, civId)
                        + " set to " + mode.displayName() + "."), true);
        return 1;
    }

    public static int hubDistributionSetSharedRatio(CommandSourceStack source, double percent)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage hub shared-stock ratio."));
            return 0;
        }
        double ratio = Math.max(0.0D, Math.min(1.0D, percent / 100.0D));
        if (!data.setHubSharedWithdrawRatio(civId, ratio, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Shared-stock ratio already matches."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Hub shared-stock ratio for " + RealCivCommands.civDisplay(data, civId)
                        + " set to " + RealCivUtil.formatPercentFromRatio(data.hubSharedWithdrawRatio(civId)) + "."),
                true);
        return 1;
    }

    public static int hubDistributionAllowanceSet(CommandSourceStack source, ResourceLocation itemId, int count)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage daily hub allowances."));
            return 0;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            source.sendFailure(Component.literal("Unknown item: " + itemId));
            return 0;
        }

        if (!data.setHubDailyAllowanceLimit(civId, itemId, count, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. Daily allowance may already match."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Set daily allowance for " + itemId + " in " + RealCivCommands.civDisplay(data, civId)
                        + " to " + count + "/day."), true);
        return 1;
    }

    public static int hubDistributionAllowanceClear(CommandSourceStack source, ResourceLocation itemId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage daily hub allowances."));
            return 0;
        }

        if (!data.setHubDailyAllowanceLimit(civId, itemId, 0, RealCivCommands.actorName(source))) {
            source.sendFailure(Component.literal("No change made. No daily allowance existed for " + itemId + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Cleared daily allowance for " + itemId + " in " + RealCivCommands.civDisplay(data, civId) + "."), true);
        return 1;
    }

    public static int hubDistributionAllowanceClearAll(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = data.getOrAssignCivilization(actor.getUUID());
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_MANAGE_HUB_DISTRIBUTION)) {
            source.sendFailure(Component.literal("Only leadership/admin can manage daily hub allowances."));
            return 0;
        }
        int cleared = data.clearAllHubDailyAllowanceLimits(civId, RealCivCommands.actorName(source));
        if (cleared <= 0) {
            source.sendFailure(Component.literal("No daily allowance entries were configured."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Cleared " + cleared + " daily allowance entr" + (cleared == 1 ? "y" : "ies")
                        + " for " + RealCivCommands.civDisplay(data, civId) + "."), true);
        return 1;
    }

    public static int hubDistributionAllowanceList(CommandSourceStack source, int page) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        List<Map.Entry<String, Integer>> allowances = data.hubDailyAllowanceEntriesSorted(civId);
        if (allowances.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No daily allowance entries configured for " + RealCivCommands.civDisplay(data, civId) + "."), false);
            return 1;
        }

        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = (allowances.size() + pageSize - 1) / pageSize;
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(allowances.size(), start + pageSize);

        source.sendSuccess(() -> Component.literal(
                "Hub daily allowance entries for " + RealCivCommands.civDisplay(data, civId)
                        + " (page " + safePage + "/" + totalPages + "):"), false);
        for (int i = start; i < end; i++) {
            Map.Entry<String, Integer> entry = allowances.get(i);
            source.sendSuccess(() -> Component.literal(
                    "- " + entry.getKey() + ": " + entry.getValue() + "/day"), false);
        }
        return 1;
    }

    public static int showHubCoverage(CommandSourceStack source, int page) {
        Map<ResourceLocation, RewardRule> exactRules = RealCivConfig.rewardRules();
        List<TagRewardRule> tagRules = RealCivConfig.tagRewardRules();

        Map<Profession, Integer> coveredByProfession = new HashMap<>();
        List<String> unmatched = new java.util.ArrayList<>();
        int totalItems = 0;
        int coveredItems = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            totalItems++;
            ItemStack stack = new ItemStack(item);
            RewardRule rule = HubRewardResolver.resolveEffectiveRewardRule(stack);
            if (rule == null) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                unmatched.add(id.toString());
                continue;
            }

            coveredItems++;
            coveredByProfession.merge(rule.profession(), 1, Integer::sum);
        }

        int totalBlocks = 0;
        int minerBlocks = 0;
        int terraformerBlocks = 0;
        int lumberBlocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            totalBlocks++;
            if (block.defaultBlockState().is(PICKAXE_MINEABLE_TAG)) {
                minerBlocks++;
            }
            if (block.defaultBlockState().is(SHOVEL_MINEABLE_TAG)) {
                terraformerBlocks++;
            }
            if (block.defaultBlockState().is(BlockTags.LOGS) || block.defaultBlockState().is(BAMBOO_BLOCKS_TAG)) {
                lumberBlocks++;
            }
        }

        unmatched.sort(String::compareTo);
        int pageSize = Math.max(1, RealCivConfig.HUB_STOCK_LIST_LIMIT.get());
        int totalPages = Math.max(1, (unmatched.size() + pageSize - 1) / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int start = (safePage - 1) * pageSize;
        int end = Math.min(unmatched.size(), start + pageSize);
        final int summaryCoveredItems = coveredItems;
        final int summaryTotalItems = totalItems;
        final int summaryMinerBlocks = minerBlocks;
        final int summaryTerraformerBlocks = terraformerBlocks;
        final int summaryLumberBlocks = lumberBlocks;
        final int summaryTotalBlocks = totalBlocks;

        source.sendSuccess(() -> Component.literal(
                "Hub coverage audit:"
                        + " exact rules=" + exactRules.size()
                        + ", tag rules=" + tagRules.size()
                        + ", items covered=" + summaryCoveredItems + "/" + summaryTotalItems
                        + ", blocks(miner)=" + summaryMinerBlocks + "/" + summaryTotalBlocks
                        + ", blocks(terraformer)=" + summaryTerraformerBlocks + "/" + summaryTotalBlocks
                        + ", blocks(lumberjack)=" + summaryLumberBlocks + "/" + summaryTotalBlocks),
                false);

        for (Profession profession : Profession.values()) {
            int count = coveredByProfession.getOrDefault(profession, 0);
            source.sendSuccess(() -> Component.literal("- " + profession.name() + ": " + count + " item(s)"), false);
        }

        if (unmatched.isEmpty()) {
            source.sendSuccess(() -> Component.literal("All non-air items are covered by hub reward rules."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(
                "Unmatched items (page " + safePage + "/" + totalPages + ", total " + unmatched.size() + "):"),
                false);
        for (int i = start; i < end; i++) {
            String entry = unmatched.get(i);
            source.sendSuccess(() -> Component.literal("- " + entry), false);
        }
        return 1;
    }

    public static int exportHubItemIds(CommandSourceStack source, String namespaceRaw) {
        @Nullable String namespace = sanitizeNamespace(namespaceRaw);
        if (namespace == null) {
            source.sendFailure(Component.literal(
                    "Invalid namespace. Use lowercase namespace characters only: a-z, 0-9, _, -, ."));
            return 0;
        }

        List<String> itemIds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (namespace.equals(id.getNamespace())) {
                itemIds.add(id.toString());
            }
        }
        itemIds.sort(String::compareTo);

        if (itemIds.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No registered non-air items found for namespace '" + namespace + "'."));
            return 0;
        }

        Path exportDir = FMLPaths.CONFIGDIR.get().resolve("realciv").resolve("exports");
        Path exportFile = exportDir.resolve(namespace + "_items.txt");

        List<String> lines = new ArrayList<>();
        lines.add("# RealCiv Item Export");
        lines.add("# namespace: " + namespace);
        lines.add("# generated_utc: " + Instant.now());
        lines.add("# total_items: " + itemIds.size());
        lines.add("# format: one item id per line");
        lines.add("");
        lines.addAll(itemIds);

        try {
            Files.createDirectories(exportDir);
            Files.write(
                    exportFile,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException ex) {
            source.sendFailure(Component.literal(
                    "Failed to write export file '" + exportFile + "': " + ex.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Exported " + itemIds.size() + " item id(s) for namespace '" + namespace + "' to "
                        + exportFile.toAbsolutePath()), true);
        return 1;
    }

    public static int showHubLogs(CommandSourceStack source, int count) {
        CivSavedData data = CivSavedData.get(source.getServer());
        String civId = RealCivCommands.civOfSource(source, data);
        if (!RealCivCommands.hasCivPermission(source, data, civId, CivSavedData.ROLE_PERMISSION_VIEW_HUB_LOGS)) {
            source.sendFailure(Component.literal("Only leadership/admin can inspect hub logs."));
            return 0;
        }

        List<String> logs = data.getRecentAuditLogs(civId, count);
        if (logs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No civic logs available."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(
                "Recent civic logs for " + RealCivCommands.civDisplay(data, civId) + ":"), false);
        for (String entry : logs) {
            source.sendSuccess(() -> Component.literal(entry), false);
        }
        return 1;
    }

    @Nullable
    private static String sanitizeNamespace(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean allowed = (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_'
                    || ch == '-'
                    || ch == '.';
            if (!allowed) {
                return null;
            }
        }
        return value;
    }
}
