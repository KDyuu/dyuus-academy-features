package com.dyuus.academy_features.network;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.util.CobblemonHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class TeraNetworking {
    public static final Identifier SELECT_TYPE_PACKET_ID = DyuusAcademyFeatures.id("select_tera_type");

    public static void registerPackets() {
        PayloadTypeRegistry.playC2S().register(SelectTeraTypePayload.ID, SelectTeraTypePayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SelectTeraTypePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                CobblemonHelper.handleTeraChange(player, payload.partySlot(), payload.teraType());
            });
        });
    }

    public record SelectTeraTypePayload(int partySlot, String teraType) implements CustomPayload {
        public static final Id<SelectTeraTypePayload> ID = new Id<>(SELECT_TYPE_PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, SelectTeraTypePayload> CODEC = new PacketCodec<>() {
            @Override
            public SelectTeraTypePayload decode(RegistryByteBuf buf) {
                return new SelectTeraTypePayload(buf.readInt(), buf.readString());
            }

            @Override
            public void encode(RegistryByteBuf buf, SelectTeraTypePayload value) {
                buf.writeInt(value.partySlot);
                buf.writeString(value.teraType);
            }
        };

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
