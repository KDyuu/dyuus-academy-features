package com.dyuus.academy_features.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ShopNetworkingClient {

    public static void sendBuyRequest(int itemIndex, int quantity) {
        ClientPlayNetworking.send(new ShopNetworking.BuyPayload(itemIndex, quantity));
    }

    public static void sendSellRequest(int itemIndex, int quantity) {
        ClientPlayNetworking.send(new ShopNetworking.SellPayload(itemIndex, quantity));
    }
}
