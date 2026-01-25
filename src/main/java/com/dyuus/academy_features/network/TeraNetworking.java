package com.dyuus.academy_features.network;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.config.TeraItemConfig;
import com.dyuus.academy_features.util.CobblemonHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TeraNetworking {

    // ==================== Packet IDs ====================

    public static final Identifier SELECT_TYPE_PACKET_ID = DyuusAcademyFeatures.id("select_tera_type");
    public static final Identifier SYNC_ITEMS_PACKET_ID = DyuusAcademyFeatures.id("sync_tera_items");

    // ==================== Registration ====================

    public static void registerPackets() {
        // Client -> Server: Tera type selection
        PayloadTypeRegistry.playC2S().register(SelectTeraTypePayload.ID, SelectTeraTypePayload.CODEC);

        // Server -> Client: Tera items sync
        PayloadTypeRegistry.playS2C().register(SyncTeraItemsPayload.ID, SyncTeraItemsPayload.CODEC);

        DyuusAcademyFeatures.LOGGER.info("Tera networking packets registered");
    }

    public static void registerServerReceivers() {
        // Handle Tera type selection from client
        ServerPlayNetworking.registerGlobalReceiver(SelectTeraTypePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                CobblemonHelper.handleTeraChange(player, payload.partySlot(), payload.teraType());
            });
        });

        // Send tera items config when player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            syncTeraItemsToPlayer(handler.getPlayer());
        });

        DyuusAcademyFeatures.LOGGER.info("Tera server receivers registered");
    }

    // ==================== Sync Methods ====================

    /**
     * Sends the Tera item configuration to a specific player.
     */
    public static void syncTeraItemsToPlayer(ServerPlayerEntity player) {
        List<TeraItemConfig> items = TeraItemConfig.getServerItems();
        SyncTeraItemsPayload payload = new SyncTeraItemsPayload(items);
        ServerPlayNetworking.send(player, payload);
        DyuusAcademyFeatures.LOGGER.debug("Synced {} tera items to player {}",
                items.size(), player.getName().getString());
    }

    // ==================== Payload: Select Tera Type (Client -> Server) ====================

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

    // ==================== Payload: Sync Tera Items (Server -> Client) ====================

    public record SyncTeraItemsPayload(List<TeraItemConfig> items) implements CustomPayload {
        public static final Id<SyncTeraItemsPayload> ID = new Id<>(SYNC_ITEMS_PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, SyncTeraItemsPayload> CODEC = new PacketCodec<>() {
            @Override
            public SyncTeraItemsPayload decode(RegistryByteBuf buf) {
                int count = buf.readVarInt();
                List<TeraItemConfig> items = new ArrayList<>(count);

                for (int i = 0; i < count; i++) {
                    String displayName = buf.readString();
                    String teraType = buf.readString();
                    String itemId = buf.readString();
                    items.add(new TeraItemConfig(displayName, teraType, itemId));
                }

                return new SyncTeraItemsPayload(items);
            }

            @Override
            public void encode(RegistryByteBuf buf, SyncTeraItemsPayload value) {
                buf.writeVarInt(value.items.size());

                for (TeraItemConfig item : value.items) {
                    buf.writeString(item.displayName != null ? item.displayName : "");
                    buf.writeString(item.teraType != null ? item.teraType : "");
                    buf.writeString(item.itemId != null ? item.itemId : "minecraft:barrier");
                }
            }
        };

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}