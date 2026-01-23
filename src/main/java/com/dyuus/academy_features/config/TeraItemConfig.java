package com.dyuus.academy_features.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TeraItemConfig {
    @SerializedName("display_name")
    public String displayName;
    @SerializedName("tera_type")
    public String teraType;
    @SerializedName("item_id")
    public String itemId;  // "minecraft:fire_charge" (visuel)

    private static List<TeraItemConfig> items = null;

    public static List<TeraItemConfig> getTeraItems() {
        if (items != null) return items;

        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("dyuus-academy-features");
        Path configPath = configDir.resolve("tera_items.json");

        try {
            // Crée dossier si absent
            Files.createDirectories(configDir);

            if (Files.exists(configPath)) {
                // Lit JSON existant
                Gson gson = new Gson();
                TeraItemConfig[] loaded = gson.fromJson(new FileReader(configPath.toFile()), TeraItemConfig[].class);
                items = List.of(loaded);
            } else {
                // ✅ GÉNÈRE AUTOMATIQUE avec tes 19 types
                items = generateDefaultConfig();
                saveConfig(items, configPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            items = new ArrayList<>();
        }
        return items;
    }

    private static List<TeraItemConfig> generateDefaultConfig() {
        TeraConfig teraConfig = new TeraConfig();  // Réutilise TON config !
        String[] types = {"bug", "dark", "dragon", "electric", "fairy", "fighting",
                "fire", "flying", "ghost", "grass", "ground", "ice",
                "normal", "poison", "psychic", "rock", "steel", "water", "stellar"};

        List<TeraItemConfig> defaults = new ArrayList<>();
        for (String type : types) {
            TeraItemConfig item = new TeraItemConfig();
            item.displayName = type.substring(0, 1).toUpperCase() + type.substring(1);
            item.teraType = type;
            item.itemId = "minecraft:barrier";  // Default (remplace dans JSON)
            defaults.add(item);
        }
        return defaults;
    }

    private static void saveConfig(List<TeraItemConfig> items, Path path) {
        try (FileWriter writer = new FileWriter(path.toFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(items.toArray(new TeraItemConfig[0]), writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
