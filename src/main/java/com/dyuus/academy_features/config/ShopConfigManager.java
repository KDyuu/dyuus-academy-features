package com.dyuus.academy_features.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dyuus.academy_features.DyuusAcademyFeatures;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ShopConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("dyuus-shop");
    private static final Path SHOP_CONFIG_FILE = CONFIG_DIR.resolve("shop_items.json");

    private static ShopConfig currentConfig;

    public static void initialize() {
        try {
            Files.createDirectories(CONFIG_DIR);

            if (Files.exists(SHOP_CONFIG_FILE)) {
                DyuusAcademyFeatures.LOGGER.info("Fichier de config trouvé, chargement...");
                loadConfig();
            } else {
                DyuusAcademyFeatures.LOGGER.info("Pas de fichier de config, création...");
                createDefaultConfig();
            }

            DyuusAcademyFeatures.LOGGER.info("Shop configuration loaded successfully");
            DyuusAcademyFeatures.LOGGER.info("Items count: {}", currentConfig != null ? currentConfig.items.size() : "NULL CONFIG!");

        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to initialize shop configuration", e);
            currentConfig = new ShopConfig();
            currentConfig.items = new ArrayList<>();
        }
    }

    private static void loadConfig() throws IOException {
        String json = Files.readString(SHOP_CONFIG_FILE);
        currentConfig = GSON.fromJson(json, ShopConfig.class);

        if (currentConfig == null) {
            DyuusAcademyFeatures.LOGGER.warn("Config parsée null, création d'une nouvelle");
            createDefaultConfig();
        } else if (currentConfig.items == null) {
            DyuusAcademyFeatures.LOGGER.warn("Items null, initialisation");
            currentConfig.items = new ArrayList<>();
        }
    }

    private static void createDefaultConfig() throws IOException {
        currentConfig = createExampleConfig();
        saveConfig();
        DyuusAcademyFeatures.LOGGER.info("Config par défaut créée avec {} items", currentConfig.items.size());
    }

    public static void saveConfig() throws IOException {
        String json = GSON.toJson(currentConfig);
        Files.writeString(SHOP_CONFIG_FILE, json);
        DyuusAcademyFeatures.LOGGER.info("Config sauvegardée dans: {}", SHOP_CONFIG_FILE);
    }

    public static ShopConfig getConfig() {
        return currentConfig;
    }

    public static void reloadConfig() {
        try {
            loadConfig();
            DyuusAcademyFeatures.LOGGER.info("Configuration reloaded");
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to reload configuration", e);
        }
    }

    private static ShopConfig createExampleConfig() {
        ShopConfig config = new ShopConfig();
        config.items = new ArrayList<>();

        // Exemples d'items (liste plate)
        addItem(config, "minecraft:dirt", "Terre", 10, 5, true, true, 64);
        addItem(config, "minecraft:stone", "Pierre", 15, 7, true, true, 64);
        addItem(config, "minecraft:cobblestone", "Cobblestone", 12, 6, true, true, 64);
        addItem(config, "minecraft:oak_log", "Bûche de chêne", 20, 10, true, true, 64);
        addItem(config, "minecraft:apple", "Pomme", 25, 12, true, true, 16);
        addItem(config, "minecraft:bread", "Pain", 30, 15, true, true, 16);
        addItem(config, "minecraft:iron_ingot", "Lingot de fer", 50, 25, true, true, 64);
        addItem(config, "minecraft:gold_ingot", "Lingot d'or", 100, 50, true, true, 64);
        addItem(config, "minecraft:diamond", "Diamant", 500, 250, true, true, 16);
        addItem(config, "minecraft:emerald", "Émeraude", 300, 150, true, true, 16);

        return config;
    }

    private static void addItem(ShopConfig config, String itemId, String displayName,
                                int buyPrice, int sellPrice, boolean canBuy,
                                boolean canSell, int maxStackSize) {
        ShopItem item = new ShopItem();
        item.itemId = itemId;
        item.displayName = displayName;
        item.buyPrice = buyPrice;
        item.sellPrice = sellPrice;
        item.canBuy = canBuy;
        item.canSell = canSell;
        item.maxStackSize = maxStackSize;
        config.items.add(item);
    }
}
