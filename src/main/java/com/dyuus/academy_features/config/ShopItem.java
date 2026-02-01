package com.dyuus.academy_features.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ShopItem {
    public String itemId;
    public String displayName;
    public int buyPrice;
    public int sellPrice;
    public boolean canBuy;
    public boolean canSell;
    public int maxStackSize;

    // NOUVEAU : Map optionnelle pour les composants personnalisés
    public Map<String, String> components = new HashMap<>();

    public static final Codec<ShopItem> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("itemId").forGetter(i -> i.itemId),
                    Codec.STRING.fieldOf("displayName").forGetter(i -> i.displayName),
                    Codec.INT.fieldOf("buyPrice").forGetter(i -> i.buyPrice),
                    Codec.INT.fieldOf("sellPrice").forGetter(i -> i.sellPrice),
                    Codec.BOOL.fieldOf("canBuy").forGetter(i -> i.canBuy),
                    Codec.BOOL.fieldOf("canSell").forGetter(i -> i.canSell),
                    Codec.INT.fieldOf("maxStackSize").forGetter(i -> i.maxStackSize)
            ).apply(instance, (itemId, displayName, buyPrice, sellPrice, canBuy, canSell, maxStackSize) -> {
                ShopItem item = new ShopItem();
                item.itemId = itemId;
                item.displayName = displayName;
                item.buyPrice = buyPrice;
                item.sellPrice = sellPrice;
                item.canBuy = canBuy;
                item.canSell = canSell;
                item.maxStackSize = maxStackSize;
                return item;
            })
    );

    public static void encode(RegistryByteBuf buf, ShopItem item) {  // ← CHANGER ICI
        writeString(buf, item.itemId);
        writeString(buf, item.displayName);
        buf.writeInt(item.buyPrice);
        buf.writeInt(item.sellPrice);
        buf.writeBoolean(item.canBuy);
        buf.writeBoolean(item.canSell);
        buf.writeInt(item.maxStackSize);

        // NOUVEAU : Encoder les composants
        buf.writeInt(item.components.size());
        for (Map.Entry<String, String> entry : item.components.entrySet()) {
            writeString(buf, entry.getKey());
            writeString(buf, entry.getValue());
        }
    }

    public static ShopItem decode(RegistryByteBuf buf) {  // ← CHANGER ICI
        ShopItem item = new ShopItem();
        item.itemId = readString(buf);
        item.displayName = readString(buf);
        item.buyPrice = buf.readInt();
        item.sellPrice = buf.readInt();
        item.canBuy = buf.readBoolean();
        item.canSell = buf.readBoolean();
        item.maxStackSize = buf.readInt();

        // NOUVEAU : Décoder les composants
        int componentsSize = buf.readInt();
        item.components = new HashMap<>();
        for (int i = 0; i < componentsSize; i++) {
            String key = readString(buf);
            String value = readString(buf);
            item.components.put(key, value);
        }

        return item;
    }

    private static void writeString(RegistryByteBuf buf, String str) {  // ← CHANGER ICI
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readString(RegistryByteBuf buf) {  // ← CHANGER ICI
        int length = buf.readInt();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
