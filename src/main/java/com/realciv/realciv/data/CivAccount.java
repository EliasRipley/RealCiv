package com.realciv.realciv.data;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public final class CivAccount {
    long socialCreditCents;
    final Map<String, Long> contributions = new HashMap<>();
    final Map<String, Long> personalWithdrawals = new HashMap<>();
    long dailyAllowanceDayIndex = -1L;
    final Map<String, Long> dailyAllowanceWithdrawals = new HashMap<>();
    @Nullable
    Double personalWithdrawRatioOverride;

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("socialCreditCents", socialCreditCents);

        CompoundTag contributionTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : contributions.entrySet()) {
            contributionTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
        }
        tag.put("contributions", contributionTag);

        CompoundTag withdrawalTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : personalWithdrawals.entrySet()) {
            withdrawalTag.putLong(entry.getKey(), Math.max(0L, entry.getValue()));
        }
        tag.put("personalWithdrawals", withdrawalTag);

        if (dailyAllowanceDayIndex >= 0L) {
            tag.putLong("dailyAllowanceDayIndex", dailyAllowanceDayIndex);
        }
        CompoundTag dailyAllowanceTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : dailyAllowanceWithdrawals.entrySet()) {
            long value = Math.max(0L, entry.getValue());
            if (value > 0L) {
                dailyAllowanceTag.putLong(entry.getKey(), value);
            }
        }
        tag.put("dailyAllowanceWithdrawals", dailyAllowanceTag);

        if (personalWithdrawRatioOverride != null) {
            tag.putDouble("personalWithdrawRatioOverride", Math.max(0.0D, Math.min(1.0D, personalWithdrawRatioOverride)));
        }
        return tag;
    }

    public static CivAccount load(CompoundTag tag) {
        CivAccount account = new CivAccount();
        account.socialCreditCents = Math.max(0L, tag.getLong("socialCreditCents"));

        CompoundTag contributionTag = tag.getCompound("contributions");
        for (String key : contributionTag.getAllKeys()) {
            account.contributions.put(key, Math.max(0L, contributionTag.getLong(key)));
        }

        CompoundTag withdrawalTag = tag.getCompound("personalWithdrawals");
        for (String key : withdrawalTag.getAllKeys()) {
            account.personalWithdrawals.put(key, Math.max(0L, withdrawalTag.getLong(key)));
        }

        account.dailyAllowanceDayIndex = tag.contains("dailyAllowanceDayIndex")
                ? tag.getLong("dailyAllowanceDayIndex")
                : -1L;
        CompoundTag dailyAllowanceTag = tag.getCompound("dailyAllowanceWithdrawals");
        for (String key : dailyAllowanceTag.getAllKeys()) {
            long value = Math.max(0L, dailyAllowanceTag.getLong(key));
            if (value > 0L) {
                account.dailyAllowanceWithdrawals.put(key, value);
            }
        }

        if (tag.contains("personalWithdrawRatioOverride")) {
            account.personalWithdrawRatioOverride = Math.max(0.0D, Math.min(1.0D, tag.getDouble("personalWithdrawRatioOverride")));
        }
        return account;
    }
}
