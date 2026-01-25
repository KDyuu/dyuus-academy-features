package com.dyuus.academy_features.config;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for Tera Type items displayed in the Tera selection GUI.
 *
 * SERVER-SIDE: Loads from JSON file and sends to clients
 * CLIENT-SIDE: Receives from server (does NOT read local file)
 */
public class TeraItemConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("dyuus-academy-features");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("tera_items.json");

    // ==================== Instance Fields ====================

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("tera_type")
    public String teraType;

    @SerializedName("item_id")
    public String itemId;

    // ==================== Constructors ====================

    /** Default constructor for GSON */
    public TeraItemConfig() {}

    /** Constructor for creating instances programmatically */
    public TeraItemConfig(String displayName, String teraType, String itemId) {
        this.displayName = displayName;
        this.teraType = teraType;
        this.itemId = itemId;
    }

    // ==================== Static Fields ====================

    /** Server-side config loaded from file */
    private static List<TeraItemConfig> serverItems = null;

    /** Client-side config received from server */
    private static List<TeraItemConfig> clientItems = null;

    private static boolean serverInitialized = false;

    // ==================== Server-Side API ====================

    /**
     * Initializes the server-side Tera item configuration.
     * Should be called during mod initialization on SERVER only.
     */
    public static void initializeServer() {
        if (serverInitialized) {
            return;
        }

        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
                DyuusAcademyFeatures.LOGGER.info("Created config directory: {}", CONFIG_DIR);
            }

            if (Files.exists(CONFIG_FILE)) {
                loadServerConfig();
                DyuusAcademyFeatures.LOGGER.info("Loaded tera_items.json with {} items", serverItems.size());
            } else {
                serverItems = generateDefaultConfig();
                saveServerConfig();
                DyuusAcademyFeatures.LOGGER.info("Created default tera_items.json with {} items", serverItems.size());
            }

            serverInitialized = true;

        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to initialize tera items config", e);
            serverItems = generateDefaultConfig();
            serverInitialized = true;
        }
    }

    /**
     * Gets the server-side list of Tera items (for sending to clients).
     *
     * @return List of TeraItemConfig entries
     */
    public static List<TeraItemConfig> getServerItems() {
        if (!serverInitialized) {
            initializeServer();
        }
        return serverItems != null ? serverItems : new ArrayList<>();
    }

    /**
     * Reloads the server configuration from disk.
     */
    public static void reloadServer() {
        serverInitialized = false;
        serverItems = null;
        initializeServer();
    }

    // ==================== Client-Side API ====================

    /**
     * Sets the client-side items received from the server.
     * Called when the client receives the sync packet.
     *
     * @param items List of TeraItemConfig from server
     */
    public static void setClientItems(List<TeraItemConfig> items) {
        clientItems = new ArrayList<>(items);
    }

    /**
     * Gets the list of Tera items for the client GUI.
     * Returns server-synced items, or empty list if not yet received.
     *
     * @return List of TeraItemConfig entries
     */
    public static List<TeraItemConfig> getTeraItems() {
        // On client: use synced items from server
        if (clientItems != null && !clientItems.isEmpty()) {
            return clientItems;
        }

        // Fallback: if we're on integrated server (singleplayer), use server items
        if (serverItems != null && !serverItems.isEmpty()) {
            return serverItems;
        }

        // Not yet synced - return empty (GUI will show nothing until sync)
        DyuusAcademyFeatures.LOGGER.debug("Tera items not yet synced from server");
        return new ArrayList<>();
    }

    /**
     * Clears client-side cached items (called on disconnect).
     */
    public static void clearClientItems() {
        clientItems = null;
    }

    // ==================== Private Methods ====================

    private static void loadServerConfig() throws IOException {
        String json = Files.readString(CONFIG_FILE);
        TeraItemConfig[] loaded = GSON.fromJson(json, TeraItemConfig[].class);

        if (loaded != null) {
            serverItems = new ArrayList<>(Arrays.asList(loaded));
        } else {
            DyuusAcademyFeatures.LOGGER.warn("tera_items.json parsed as null, creating default");
            serverItems = generateDefaultConfig();
            saveServerConfig();
        }
    }

    private static void saveServerConfig() throws IOException {
        if (!Files.exists(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }

        String json = GSON.toJson(serverItems.toArray(new TeraItemConfig[0]));
        Files.writeString(CONFIG_FILE, json);
    }

    private static List<TeraItemConfig> generateDefaultConfig() {
        String[] types = {
                "bug", "dark", "dragon", "electric", "fairy", "fighting",
                "fire", "flying", "ghost", "grass", "ground", "ice",
                "normal", "poison", "psychic", "rock", "steel", "water", "stellar"
        };

        List<TeraItemConfig> defaults = new ArrayList<>();

        for (String type : types) {
            TeraItemConfig item = new TeraItemConfig();
            item.displayName = capitalizeFirst(type);
            item.teraType = type;
            item.itemId = "minecraft:barrier";
            defaults.add(item);
        }

        return defaults;
    }

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}