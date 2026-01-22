package com.dyuus.academy_features.network;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.screen.ShopScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ShopNetworking {
    public static final Identifier BUY_PACKET_ID = DyuusAcademyFeatures.id("buy_item");
    public static final Identifier SELL_PACKET_ID = DyuusAcademyFeatures.id("sell_item");

    public static void registerPackets() {
        PayloadTypeRegistry.playC2S().register(BuyPayload.ID, BuyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SellPayload.ID, SellPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(BuyPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (player.currentScreenHandler instanceof ShopScreenHandler shopHandler) {
                    shopHandler.buyItem(player, payload.itemIndex(), payload.quantity());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SellPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (player.currentScreenHandler instanceof ShopScreenHandler shopHandler) {
                    shopHandler.sellItem(player, payload.itemIndex(), payload.quantity());
                }
            });
        });
    }

    public record BuyPayload(int itemIndex, int quantity) implements CustomPayload {
        public static final Id<BuyPayload> ID = new Id<>(BUY_PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, BuyPayload> CODEC = new PacketCodec<>() {
            @Override
            public BuyPayload decode(RegistryByteBuf buf) {
                return new BuyPayload(buf.readInt(), buf.readInt());
            }

            @Override
            public void encode(RegistryByteBuf buf, BuyPayload value) {
                buf.writeInt(value.itemIndex);
                buf.writeInt(value.quantity);
            }
        };

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SellPayload(int itemIndex, int quantity) implements CustomPayload {
        public static final Id<SellPayload> ID = new Id<>(SELL_PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, SellPayload> CODEC = new PacketCodec<>() {
            @Override
            public SellPayload decode(RegistryByteBuf buf) {
                return new SellPayload(buf.readInt(), buf.readInt());
            }

            @Override
            public void encode(RegistryByteBuf buf, SellPayload value) {
                buf.writeInt(value.itemIndex);
                buf.writeInt(value.quantity);
            }
        };

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
