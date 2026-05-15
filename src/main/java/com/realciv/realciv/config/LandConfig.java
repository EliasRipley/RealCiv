package com.realciv.realciv.config;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class LandConfig {
    private LandConfig() {
    }

    public static long upkeepIntervalTicks() {
        return 24_000L * RealCivConfig.LAND_UPKEEP_INTERVAL_DAYS.get();
    }

    public static long upkeepGraceTicks() {
        return 24_000L * RealCivConfig.LAND_UPKEEP_GRACE_DAYS.get();
    }

    public static boolean blockUnclaimedBuilding() {
        return RealCivConfig.LAND_BLOCK_UNCLAIMED_BUILDING.get();
    }

    public static boolean canClaimDimension(@Nullable String dimensionIdRaw) {
        String dimensionId = normalizeDimensionId(dimensionIdRaw);
        if (dimensionId == null) {
            return false;
        }
        ClaimDimensionPolicy policy = claimDimensionPolicy();
        if (policy == ClaimDimensionPolicy.ALLOW_ALL) {
            return true;
        }
        Set<String> configured = claimDimensionSet();
        if (policy == ClaimDimensionPolicy.ALLOWLIST) {
            return configured.contains(dimensionId);
        }
        return !configured.contains(dimensionId);
    }

    public static String claimDimensionPolicyLabel() {
        return switch (claimDimensionPolicy()) {
            case ALLOW_ALL -> "allow_all";
            case ALLOWLIST -> "allowlist";
            case DENYLIST -> "denylist";
        };
    }

    public static Set<String> claimDimensionSet() {
        Set<String> out = new HashSet<>();
        for (String raw : RealCivConfig.LAND_CLAIM_DIMENSIONS.get()) {
            String normalized = normalizeDimensionId(raw);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return Set.copyOf(out);
    }

    public static int landWandVisualizeRadiusChunks() {
        return Math.max(1, RealCivConfig.LAND_WAND_VISUALIZE_RADIUS_CHUNKS.get());
    }

    public static int landWandMaxSelectionChunks() {
        return Math.max(1, RealCivConfig.LAND_WAND_MAX_SELECTION_CHUNKS.get());
    }

    public static String ftbMayorDefaultClaimMode() {
        String raw = RealCivConfig.LAND_FTB_MAYOR_DEFAULT_CLAIM_MODE.get();
        if (raw == null) {
            return "civic";
        }
        String mode = raw.trim().toLowerCase(Locale.ROOT);
        if ("private".equals(mode)) {
            return "private";
        }
        return "civic";
    }

    public static double upkeepCostCents() {
        return Math.max(0.0D, RealCivConfig.LAND_UPKEEP_COST.get());
    }

    public static long townClaimCostCents() {
        return (long) Math.round(Math.max(0.0D, RealCivConfig.LAND_TOWN_CLAIM_COST.get()));
    }

    public static long townClaimCostAddedPerOwnedCents() {
        return (long) Math.round(Math.max(0.0D, RealCivConfig.LAND_TOWN_CLAIM_COST_ADDED_PER_OWNED.get()));
    }

    public static long rentCostCents() {
        return (long) Math.round(Math.max(0.0D, RealCivConfig.LAND_RENT_COST.get()));
    }

    public static long rentCostAddedPerOwnedPrivateCents() {
        return (long) Math.round(Math.max(0.0D, RealCivConfig.LAND_RENT_COST_ADDED_PER_OWNED_PRIVATE.get()));
    }

    public static int hubStarterAreaBlocks() {
        return Math.max(1, RealCivConfig.LAND_HUB_STARTER_AREA_BLOCKS.get());
    }

    @Nullable
    private static ClaimDimensionPolicy claimDimensionPolicy() {
        String raw = RealCivConfig.LAND_CLAIM_DIMENSION_POLICY.get();
        if (raw == null) {
            return ClaimDimensionPolicy.DENYLIST;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "allow_all", "all", "allowall" -> ClaimDimensionPolicy.ALLOW_ALL;
            case "allowlist", "allow_list", "whitelist" -> ClaimDimensionPolicy.ALLOWLIST;
            case "denylist", "deny_list", "blacklist" -> ClaimDimensionPolicy.DENYLIST;
            default -> ClaimDimensionPolicy.DENYLIST;
        };
    }

    @Nullable
    private static String normalizeDimensionId(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private enum ClaimDimensionPolicy {
        ALLOW_ALL,
        ALLOWLIST,
        DENYLIST
    }
}
