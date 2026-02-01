package com.dyuus.academy_features.currency;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player currency (PokéDollars) with persistent storage.
 * Data is saved to a JSON file in the world folder and persists across server restarts.
 */
public class CurrencyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE_NAME = "pokedollars.json";
    private static final Type DATA_TYPE = new TypeToken<ConcurrentHashMap<UUID, Integer>>() {}.getType();

    // In-memory cache of player balances
    private static final Map<UUID, Integer> playerBalances = new ConcurrentHashMap<>();

    // Reference to the current server (needed for saving)
    private static MinecraftServer currentServer = null;

    /**
     * Initializes the currency system.
     * Registers event listeners for server lifecycle and player connections.
     */
    public static void initialize() {
        // Load data when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            loadData(server);
            DyuusAcademyFeatures.LOGGER.info("Currency data loaded");
        });

        // Save data when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            saveData(server);
            DyuusAcademyFeatures.LOGGER.info("Currency data saved");
            currentServer = null;
        });

        // Save data periodically when a player disconnects (safety measure)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            saveData(server);
        });

        DyuusAcademyFeatures.LOGGER.info("Currency system initialized");
    }

    /**
     * Gets the path to the data file within the world folder.
     *
     * @param server The Minecraft server instance
     * @return Path to the pokedollars.json file
     */
    private static Path getDataPath(MinecraftServer server) {
        // Save in the world folder under a "data" subdirectory
        Path worldFolder = server.getSavePath(WorldSavePath.ROOT);
        Path dataFolder = worldFolder.resolve("data").resolve(DyuusAcademyFeatures.MOD_ID);

        // Create directories if they don't exist
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to create data directory", e);
        }

        return dataFolder.resolve(DATA_FILE_NAME);
    }

    /**
     * Loads currency data from the JSON file.
     *
     * @param server The Minecraft server instance
     */
    private static void loadData(MinecraftServer server) {
        Path dataPath = getDataPath(server);

        if (Files.exists(dataPath)) {
            try (Reader reader = Files.newBufferedReader(dataPath)) {
                Map<UUID, Integer> loadedData = GSON.fromJson(reader, DATA_TYPE);
                if (loadedData != null) {
                    playerBalances.clear();
                    playerBalances.putAll(loadedData);
                    DyuusAcademyFeatures.LOGGER.info("Loaded {} player balances from {}",
                            playerBalances.size(), dataPath);
                }
            } catch (IOException e) {
                DyuusAcademyFeatures.LOGGER.error("Failed to load currency data from {}", dataPath, e);
            } catch (Exception e) {
                DyuusAcademyFeatures.LOGGER.error("Failed to parse currency data", e);
            }
        } else {
            DyuusAcademyFeatures.LOGGER.info("No existing currency data found, starting fresh");
        }
    }

    /**
     * Saves currency data to the JSON file.
     *
     * @param server The Minecraft server instance
     */
    private static void saveData(MinecraftServer server) {
        if (server == null) {
            DyuusAcademyFeatures.LOGGER.warn("Cannot save currency data: server is null");
            return;
        }

        Path dataPath = getDataPath(server);

        try (Writer writer = Files.newBufferedWriter(dataPath)) {
            GSON.toJson(playerBalances, DATA_TYPE, writer);
            DyuusAcademyFeatures.LOGGER.debug("Saved {} player balances to {}",
                    playerBalances.size(), dataPath);
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to save currency data to {}", dataPath, e);
        }
    }

    /**
     * Forces an immediate save of all currency data.
     * Can be called manually if needed (e.g., after important transactions).
     */
    public static void forceSave() {
        if (currentServer != null) {
            saveData(currentServer);
        }
    }

    /**
     * Gets the balance of a player.
     *
     * @param player The player
     * @return The player's balance (0 if not found)
     */
    public static int getBalance(PlayerEntity player) {
        return playerBalances.getOrDefault(player.getUuid(), 0);
    }

    /**
     * Gets the balance of a player by UUID.
     *
     * @param uuid The player's UUID
     * @return The player's balance (0 if not found)
     */
    public static int getBalance(UUID uuid) {
        return playerBalances.getOrDefault(uuid, 0);
    }

    /**
     * Sets the balance of a player.
     *
     * @param player The player
     * @param amount The new balance (will be clamped to minimum 0)
     */
    public static void setBalance(PlayerEntity player, int amount) {
        playerBalances.put(player.getUuid(), Math.max(0, amount));
    }

    /**
     * Sets the balance of a player by UUID.
     *
     * @param uuid The player's UUID
     * @param amount The new balance (will be clamped to minimum 0)
     */
    public static void setBalance(UUID uuid, int amount) {
        playerBalances.put(uuid, Math.max(0, amount));
    }

    /**
     * Adds to a player's balance.
     *
     * @param player The player
     * @param amount The amount to add
     * @return true (always succeeds)
     */
    public static boolean addBalance(PlayerEntity player, int amount) {
        int current = getBalance(player);
        setBalance(player, current + amount);
        return true;
    }

    /**
     * Adds to a player's balance by UUID.
     *
     * @param uuid The player's UUID
     * @param amount The amount to add
     * @return true (always succeeds)
     */
    public static boolean addBalance(UUID uuid, int amount) {
        int current = getBalance(uuid);
        setBalance(uuid, current + amount);
        return true;
    }

    /**
     * Removes from a player's balance if they have enough.
     *
     * @param player The player
     * @param amount The amount to remove
     * @return true if successful, false if insufficient funds
     */
    public static boolean removeBalance(PlayerEntity player, int amount) {
        int current = getBalance(player);
        if (current >= amount) {
            setBalance(player, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Removes from a player's balance by UUID if they have enough.
     *
     * @param uuid The player's UUID
     * @param amount The amount to remove
     * @return true if successful, false if insufficient funds
     */
    public static boolean removeBalance(UUID uuid, int amount) {
        int current = getBalance(uuid);
        if (current >= amount) {
            setBalance(uuid, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Sends a balance message to the player.
     *
     * @param player The player to send the message to
     */
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

    /**
     * Checks if a player has at least the specified amount.
     *
     * @param player The player
     * @param amount The amount to check
     * @return true if player has enough balance
     */
    public static boolean hasBalance(PlayerEntity player, int amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Checks if a player has at least the specified amount by UUID.
     *
     * @param uuid The player's UUID
     * @param amount The amount to check
     * @return true if player has enough balance
     */
    public static boolean hasBalance(UUID uuid, int amount) {
        return getBalance(uuid) >= amount;
    }
}