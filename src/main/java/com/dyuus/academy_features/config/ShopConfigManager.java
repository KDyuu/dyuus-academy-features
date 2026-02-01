package com.dyuus.academy_features.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dyuus.academy_features.DyuusAcademyFeatures;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ShopConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("dyuus-shop");
    private static final Path SHOPS_DIR = CONFIG_DIR.resolve("shops");

    // Map of shopId -> ShopConfig
    private static final Map<String, ShopConfig> shops = new HashMap<>();

    // Default shop ID (used when no shop is specified)
    private static final String DEFAULT_SHOP_ID = "general";

    public static void initialize() {
        try {
            Files.createDirectories(SHOPS_DIR);

            // Load all shop configs from the shops directory
            loadAllShops();

            // Create default shop if no shops exist
            if (shops.isEmpty()) {
                DyuusAcademyFeatures.LOGGER.info("No shops found, creating default shop...");
                createDefaultShops();
            }

            DyuusAcademyFeatures.LOGGER.info("Shop configuration loaded successfully - {} shops available", shops.size());

        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to initialize shop configuration", e);
        }
    }

    /**
     * Loads all shop JSON files from the shops directory.
     */
    private static void loadAllShops() {
        shops.clear();

        try (Stream<Path> paths = Files.list(SHOPS_DIR)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                    .forEach(ShopConfigManager::loadShopFromFile);
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to list shop files", e);
        }
    }

    /**
     * Loads a single shop from a JSON file.
     *
     * @param filePath Path to the shop JSON file
     */
    private static void loadShopFromFile(Path filePath) {
        try {
            String json = Files.readString(filePath);
            ShopConfig config = GSON.fromJson(json, ShopConfig.class);

            if (config != null) {
                // Extract shop ID from filename (without .json extension)
                String fileName = filePath.getFileName().toString();
                String shopId = fileName.substring(0, fileName.length() - 5);

                // Ensure shopId is set correctly
                config.shopId = shopId;

                shops.put(shopId, config);
                DyuusAcademyFeatures.LOGGER.info("Loaded shop '{}' with {} items", shopId, config.items.size());
            }
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to load shop from {}", filePath, e);
        }
    }

    /**
     * Saves a shop config to its JSON file.
     *
     * @param config The shop configuration to save
     */
    public static void saveShop(ShopConfig config) {
        Path filePath = SHOPS_DIR.resolve(config.shopId + ".json");

        try {
            String json = GSON.toJson(config);
            Files.writeString(filePath, json);
            DyuusAcademyFeatures.LOGGER.info("Saved shop '{}' to {}", config.shopId, filePath);
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to save shop '{}'", config.shopId, e);
        }
    }

    /**
     * Creates default example shops.
     */
    private static void createDefaultShops() {
        // General shop
        ShopConfig generalShop = createGeneralShop();
        shops.put(generalShop.shopId, generalShop);
        saveShop(generalShop);

        // Pokemon shop (example)
        ShopConfig pokemonShop = createPokemonShop();
        shops.put(pokemonShop.shopId, pokemonShop);
        saveShop(pokemonShop);

        DyuusAcademyFeatures.LOGGER.info("Created {} default shops", shops.size());
    }

    /**
     * Creates the default general shop.
     */
    private static ShopConfig createGeneralShop() {
        ShopConfig config = new ShopConfig("general", "§6Magasin Général");

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

    /**
     * Creates an example Pokemon-themed shop.
     */
    private static ShopConfig createPokemonShop() {
        ShopConfig config = new ShopConfig("pokemon", "§bCentre Pokémon");

        // Example items - adjust to your Cobblemon items
        addItem(config, "cobblemon:poke_ball", "Poké Ball", 200, 100, true, true, 64);
        addItem(config, "cobblemon:great_ball", "Super Ball", 600, 300, true, true, 64);
        addItem(config, "cobblemon:ultra_ball", "Hyper Ball", 1200, 600, true, true, 64);
        addItem(config, "cobblemon:potion", "Potion", 300, 150, true, true, 64);
        addItem(config, "cobblemon:super_potion", "Super Potion", 700, 350, true, true, 64);
        addItem(config, "cobblemon:revive", "Rappel", 1500, 750, true, true, 64);

        return config;
    }

    /**
     * Helper method to add an item to a shop config.
     */
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

    /**
     * Gets a shop by its ID.
     *
     * @param shopId The shop identifier
     * @return The ShopConfig, or null if not found
     */
    public static ShopConfig getShop(String shopId) {
        return shops.get(shopId);
    }

    /**
     * Gets the default shop.
     *
     * @return The default ShopConfig
     */
    public static ShopConfig getDefaultShop() {
        return shops.get(DEFAULT_SHOP_ID);
    }

    /**
     * Gets all available shop IDs.
     *
     * @return Set of shop identifiers
     */
    public static Set<String> getShopIds() {
        return shops.keySet();
    }

    /**
     * Checks if a shop exists.
     *
     * @param shopId The shop identifier
     * @return true if the shop exists
     */
    public static boolean shopExists(String shopId) {
        return shops.containsKey(shopId);
    }

    /**
     * Reloads all shop configurations from disk.
     */
    public static void reloadConfig() {
        loadAllShops();
        DyuusAcademyFeatures.LOGGER.info("Reloaded {} shops", shops.size());
    }

    // Legacy method for backwards compatibility
    @Deprecated
    public static ShopConfig getConfig() {
        return getDefaultShop();
    }
}