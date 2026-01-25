package com.dyuus.academy_features.network;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.config.TeraItemConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from server to client to sync Tera item configuration.
 * This ensures all clients display the same items as defined on the server.
 */
public record TeraItemSyncPayload(List<TeraItemConfig> items) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(DyuusAcademyFeatures.MOD_ID, "tera_item_sync");
    public static final Id<TeraItemSyncPayload> ID = new Id<>(PACKET_ID);

    public static final PacketCodec<RegistryByteBuf, TeraItemSyncPayload> CODEC = PacketCodec.of(
            TeraItemSyncPayload::write,
            TeraItemSyncPayload::read
    );

    /**
     * Writes the payload to the buffer.
     */
    private void write(RegistryByteBuf buf) {
        // Write number of items
        buf.writeVarInt(items.size());

        // Write each item
        for (TeraItemConfig item : items) {
            buf.writeString(item.displayName != null ? item.displayName : "");
            buf.writeString(item.teraType != null ? item.teraType : "");
            buf.writeString(item.itemId != null ? item.itemId : "minecraft:barrier");
        }
    }

    /**
     * Reads the payload from the buffer.
     */
    private static TeraItemSyncPayload read(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        List<TeraItemConfig> items = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String displayName = buf.readString();
            String teraType = buf.readString();
            String itemId = buf.readString();

            items.add(new TeraItemConfig(displayName, teraType, itemId));
        }

        return new TeraItemSyncPayload(items);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}