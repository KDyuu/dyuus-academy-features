package com.dyuus.academy_features.currency;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CurrencyManager {
    // Utilise l'API d'attachement de Fabric pour stocker les données de monnaie
    public static final AttachmentType<Integer> PLAYER_CURRENCY = AttachmentRegistry.<Integer>builder()
            .persistent(com.mojang.serialization.Codec.INT)
            .initializer(() -> 0)
            .buildAndRegister(DyuusAcademyFeatures.id("currency"));

    public static void initialize() {
        DyuusAcademyFeatures.LOGGER.info("Currency system initialized");
    }

    public static int getBalance(PlayerEntity player) {
        return player.getAttachedOrCreate(PLAYER_CURRENCY);
    }

    public static void setBalance(PlayerEntity player, int amount) {
        player.setAttached(PLAYER_CURRENCY, Math.max(0, amount));
    }

    public static boolean addBalance(PlayerEntity player, int amount) {
        int current = getBalance(player);
        setBalance(player, current + amount);
        return true;
    }

    public static boolean removeBalance(PlayerEntity player, int amount) {
        int current = getBalance(player);
        if (current >= amount) {
            setBalance(player, current - amount);
            return true;
        }
        return false;
    }

    public static void sendBalanceMessage(ServerPlayerEntity player) {
        int balance = getBalance(player);
        player.sendMessage(
                Text.literal("Solde: ")
                        .formatted(Formatting.GOLD)
                        .append(Text.literal(String.valueOf(balance))
                                .formatted(Formatting.YELLOW))
                        .append(Text.literal(" PokéDollars")
                                .formatted(Formatting.GOLD)),
                false
        );
    }
}
