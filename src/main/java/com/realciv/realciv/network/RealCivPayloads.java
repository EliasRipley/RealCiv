package com.realciv.realciv.network;

import com.realciv.realciv.RealCivMod;
import com.realciv.realciv.census.CensusSnapshot;
import com.realciv.realciv.ledger.ProfessionLedgerSnapshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class RealCivPayloads {
    private RealCivPayloads() {}

    public static final String SCREEN_TAX = "tax";
    public static final String SCREEN_DIPLOMACY = "diplomacy";
    public static final String SCREEN_PROFESSION = "profession";
    public static final String SCREEN_CENSUS = "census";
    public static final String SCREEN_CONTROL_PANEL = "control_panel";
    public static final String SCREEN_HUB_STOCK = "hub_stock";

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
}
