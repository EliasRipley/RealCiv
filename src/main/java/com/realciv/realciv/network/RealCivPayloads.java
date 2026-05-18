package com.realciv.realciv.network;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.census.CensusSnapshot;
import com.realciv.realciv.hub.HubRationSnapshot;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshot;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

// Custom packet payloads for RealCiv client-server communication.
// Each inner record implements CustomPacketPayload with a STREAM_CODEC for NeoForge networking.
public final class RealCivPayloads {
    private RealCivPayloads() {}

    // ---- Screen identifier constants (sent in RealCivActionPayload) ----
    public static final String SCREEN_TAX = "tax";
    public static final String SCREEN_DIPLOMACY = "diplomacy";
    public static final String SCREEN_PROFESSION = "profession";
    public static final String SCREEN_CENSUS = "census";
    public static final String SCREEN_CONTROL_PANEL = "control_panel";
    public static final String SCREEN_HUB_STOCK = "hub_stock";
    public static final String SCREEN_RATION_EDITOR = "ration_editor";
    public static final String SCREEN_ROLE_MANAGER = "role_manager";

    public record RealCivActionPayload(String screenType, int actionId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RealCivActionPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "screen_action"));

        public static final StreamCodec<ByteBuf, RealCivActionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RealCivActionPayload::screenType,
                ByteBufCodecs.VAR_INT, RealCivActionPayload::actionId,
                RealCivActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenProfessionLedgerPayload(ProfessionLedgerSnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenProfessionLedgerPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_profession"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenProfessionLedgerPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenProfessionLedgerPayload(ProfessionLedgerSnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenCensusPayload(CensusSnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenCensusPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_census"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenCensusPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenCensusPayload(CensusSnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenControlPanelPayload(com.realciv.realciv.panel.CivControlPanelSnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenControlPanelPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_control_panel"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenControlPanelPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenControlPanelPayload(com.realciv.realciv.panel.CivControlPanelSnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenTaxPayload(com.realciv.realciv.tax.TaxSnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenTaxPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_tax"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenTaxPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenTaxPayload(com.realciv.realciv.tax.TaxSnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenDiplomacyPayload(com.realciv.realciv.diplomacy.DiplomacySnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenDiplomacyPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_diplomacy"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenDiplomacyPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenDiplomacyPayload(com.realciv.realciv.diplomacy.DiplomacySnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenHubStockPayload(com.realciv.realciv.hub.HubStockSnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenHubStockPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_hub_stock"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenHubStockPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenHubStockPayload(com.realciv.realciv.hub.HubStockSnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenRationEditorPayload(HubRationSnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenRationEditorPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_ration_editor"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenRationEditorPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenRationEditorPayload(HubRationSnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OpenRoleManagerPayload(com.realciv.realciv.panel.CivRoleManagerSnapshot snapshot) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenRoleManagerPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "open_role_manager"));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenRoleManagerPayload> STREAM_CODEC =
                StreamCodec.of(
                        (b, p) -> p.snapshot().write(b),
                        b -> new OpenRoleManagerPayload(com.realciv.realciv.panel.CivRoleManagerSnapshot.read(b)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ForceMapRefreshPayload(String dimension, int chunkX, int chunkZ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ForceMapRefreshPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "force_map_refresh"));

        public static final StreamCodec<ByteBuf, ForceMapRefreshPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ForceMapRefreshPayload::dimension,
                ByteBufCodecs.VAR_INT, ForceMapRefreshPayload::chunkX,
                ByteBufCodecs.VAR_INT, ForceMapRefreshPayload::chunkZ,
                ForceMapRefreshPayload::new);

        public ResourceKey<Level> dimensionKey() {
            return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetTaxItemPayload(String itemId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetTaxItemPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "set_tax_item"));

        public static final StreamCodec<ByteBuf, SetTaxItemPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SetTaxItemPayload::itemId,
                SetTaxItemPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetHubAllowancePayload(String itemId, int dailyCount) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetHubAllowancePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "set_hub_allowance"));

        public static final StreamCodec<ByteBuf, SetHubAllowancePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SetHubAllowancePayload::itemId,
                ByteBufCodecs.VAR_INT, SetHubAllowancePayload::dailyCount,
                SetHubAllowancePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetTaxItemCountPayload(int itemCount) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetTaxItemCountPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "set_tax_item_count"));

        public static final StreamCodec<ByteBuf, SetTaxItemCountPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetTaxItemCountPayload::itemCount,
                SetTaxItemCountPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RenameRolePayload(String roleId, String displayName) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RenameRolePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "rename_role"));

        public static final StreamCodec<ByteBuf, RenameRolePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RenameRolePayload::roleId,
                ByteBufCodecs.STRING_UTF8, RenameRolePayload::displayName,
                RenameRolePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // Batch-set daily allowance limits for multiple hub items at once (from the ration draft UI).
    // Wire format: VAR_INT count, then count pairs of (STRING_UTF8 itemId, VAR_INT dailyCount), then boolean replaceExisting.
    // The server handler caps the decoded count at 256 to prevent OOM from malicious packets.
    public record SetHubAllowanceBatchPayload(
            List<String> itemIds,
            List<Integer> dailyCounts,
            boolean replaceExisting) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetHubAllowanceBatchPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealCivMod.MOD_ID, "set_hub_allowance_batch"));

        public static final StreamCodec<ByteBuf, SetHubAllowanceBatchPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    int count = Math.min(payload.itemIds().size(), payload.dailyCounts().size());
                    ByteBufCodecs.VAR_INT.encode(buf, count);
                    for (int i = 0; i < count; i++) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, payload.itemIds().get(i));
                        ByteBufCodecs.VAR_INT.encode(buf, payload.dailyCounts().get(i));
                    }
                    buf.writeBoolean(payload.replaceExisting());
                },
                buf -> {
                    int rawCount = ByteBufCodecs.VAR_INT.decode(buf);
                    int count = Math.min(rawCount, 256);
                    List<String> itemIds = new ArrayList<>(count);
                    List<Integer> dailyCounts = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        itemIds.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                        dailyCounts.add(ByteBufCodecs.VAR_INT.decode(buf));
                    }
                    boolean replaceExisting = buf.readBoolean();
                    return new SetHubAllowanceBatchPayload(itemIds, dailyCounts, replaceExisting);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
