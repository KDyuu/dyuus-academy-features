package com.dyuus.academy_features.util;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.dyuus.academy_features.config.TeraConfigManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CobblemonHelper {

    public static void handleTeraChange(ServerPlayerEntity player, int partySlot, String teraTypeName) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon pokemon = party.get(partySlot);

        if (pokemon == null) {
            player.sendMessage(Text.literal("§c✖ Aucun Pokémon dans ce slot!"), false);
            return;
        }

        // ✅ Récupérer le TeraType depuis TeraTypes
        TeraType teraType = TeraTypes.INSTANCE.get(Identifier.of("cobblemon", teraTypeName.toLowerCase()));
        if (teraType == null) {
            player.sendMessage(Text.literal("§c✖ Type Tera invalide: " + teraTypeName), false);
            return;
        }

        // Vérifier le coût en shards
        int cost = TeraConfigManager.getConfig().getCost(teraTypeName.toLowerCase());
        Identifier shardId = Identifier.of("cobblemon", teraTypeName.toLowerCase() + "_tera_shard");

        if (!hasEnoughShards(player, shardId, cost)) {
            player.sendMessage(Text.literal("§c✖ Pas assez de §e" + teraTypeName + " shards§c!\n§7Requis: §f" + cost), false);
            return;
        }

        // Consommer les shards
        consumeShards(player, shardId, cost);

        // ✅ Changer le Tera Type du Pokémon
        pokemon.setTeraType(teraType);

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§a✔ Type Tera de §e" + pokemon.getSpecies().getName() +
                "§a changé en §6" + teraType.getName().toUpperCase() + "§a!"), false);
        player.sendMessage(Text.literal("§7Shards consommés: §f" + cost + "x " + teraTypeName + "_tera_shard"), false);
        player.sendMessage(Text.literal(""), false);
    }

    private static boolean hasEnoughShards(ServerPlayerEntity player, Identifier shardId, int count) {
        Item shardItem = Registries.ITEM.get(shardId);
        if (shardItem == null || shardItem.getDefaultStack().isEmpty()) {
            return false;
        }

        int totalCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == shardItem) {
                totalCount += stack.getCount();
            }
        }
        return totalCount >= count;
    }

    private static void consumeShards(ServerPlayerEntity player, Identifier shardId, int count) {
        Item shardItem = Registries.ITEM.get(shardId);
        int remaining = count;

        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == shardItem) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.decrement(toRemove);
                remaining -= toRemove;
            }
        }
    }
}
