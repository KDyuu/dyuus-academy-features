package com.dyuus.academy_features.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TeraNetworkingClient {

    public static void sendSelectTeraType(int partySlot, String teraType) {
        ClientPlayNetworking.send(new TeraNetworking.SelectTeraTypePayload(partySlot, teraType));
    }
}
