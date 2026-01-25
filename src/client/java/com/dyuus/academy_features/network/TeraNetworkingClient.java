package com.dyuus.academy_features.network;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.config.TeraItemConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Handles client-side networking for the Tera system.
 */
public class TeraNetworkingClient {

    /**
     * Registers client-side packet receivers.
     * Call this during CLIENT mod initialization (in DyuusAcademyFeaturesClient).
     */
    public static void registerClientReceivers() {
        // Receive tera items sync from server
        ClientPlayNetworking.registerGlobalReceiver(TeraNetworking.SyncTeraItemsPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                TeraItemConfig.setClientItems(payload.items());
                DyuusAcademyFeatures.LOGGER.info("Received {} tera items from server", payload.items().size());
            });
        });

        // Clear cached items when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TeraItemConfig.clearClientItems();
            DyuusAcademyFeatures.LOGGER.debug("Cleared tera items cache on disconnect");
        });

        DyuusAcademyFeatures.LOGGER.info("Tera client receivers registered");
    }

    /**
     * Sends a Tera type selection to the server.
     *
     * @param partySlot The party slot of the Pokémon
     * @param teraType The selected Tera type
     */
    public static void sendSelectTeraType(int partySlot, String teraType) {
        ClientPlayNetworking.send(new TeraNetworking.SelectTeraTypePayload(partySlot, teraType));
    }
}