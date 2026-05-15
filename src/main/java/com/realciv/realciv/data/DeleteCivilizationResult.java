package com.realciv.realciv.data;

public record DeleteCivilizationResult(
        String deletedId,
        String deletedDisplayName,
        int reassignedMembers,
        int migratedAccounts,
        int transferredStockEntries,
        long transferredStockItems,
        int removedPlots) {
}
