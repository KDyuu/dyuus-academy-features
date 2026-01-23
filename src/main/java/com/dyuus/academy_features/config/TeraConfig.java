package com.dyuus.academy_features.config;

import java.util.HashMap;
import java.util.Map;

public class TeraConfig {
    public Map<String, Integer> shardCosts = new HashMap<>();

    public TeraConfig() {
        // Initialisation avec les 18 types Pokémon + le type stellaire
        String[] types = {"bug", "dark", "dragon", "electric", "fairy", "fighting",
                "fire", "flying", "ghost", "grass", "ground", "ice",
                "normal", "poison", "psychic", "rock", "steel", "water", "stellar"};

        for (String type : types) {
            shardCosts.put(type, 50); // Coût par défaut : 50 shards
        }
    }

    public int getCost(String type) {
        return shardCosts.getOrDefault(type.toLowerCase(), 50);
    }
}
