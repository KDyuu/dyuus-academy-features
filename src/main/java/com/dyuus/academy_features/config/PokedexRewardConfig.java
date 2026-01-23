package com.dyuus.academy_features.config;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration class for Pokedex capture rewards.
 * Handles loading and saving reward settings from/to a JSON file.
 */
public class PokedexRewardConfig {

    // Gson instance with pretty printing for readable JSON files
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Config file location in the minecraft config folder
    private static File configFile;

    // Singleton instance of the config
    private static PokedexRewardConfig INSTANCE;

    // ==================== Configuration Fields ====================

    /**
     * Amount of PokéDollars rewarded for capturing a new Pokémon species.
     * This applies only when the Pokémon wasn't previously registered as CAUGHT in the Pokédex.
     */
    public int newPokedexEntryReward = 500;

    /**
     * Amount of PokéDollars rewarded for capturing a new form of an already caught species.
     * For example: catching a regional form for the first time.
     */
    public int newFormReward = 250;

    /**
     * Amount of PokéDollars rewarded for capturing a shiny version of a species for the first time.
     * This applies even if the normal version was already registered in the Pokédex.
     * Set to 0 to disable this reward.
     */
    public int firstShinyReward = 1000;

    // ==================== Scan Rewards ====================

    /**
     * Amount of PokéDollars rewarded for scanning a new Pokémon species with the Pokédex.
     * This applies only when the species wasn't previously registered (ENCOUNTERED or CAUGHT).
     * Set to 0 to disable scan rewards.
     */
    public int scanNewSpeciesReward = 50;

    /**
     * Amount of PokéDollars rewarded for scanning a new form of a species.
     * This applies when the species was already seen but this specific form is new.
     * Set to 0 to disable this reward.
     */
    public int scanNewFormReward = 25;

    /**
     * Amount of PokéDollars rewarded for scanning a new variation (shiny, gender, etc.).
     * This applies when scanning reveals new information about an already known form.
     * Set to 0 to disable this reward.
     */
    public int scanNewVariationReward = 10;

    /**
     * Whether to send a message to the player when they receive a reward.
     */
    public boolean enableRewardMessage = true;

    /**
     * Message sent to the player when they capture a new Pokémon species.
     * Placeholders:
     *   %amount% - The reward amount
     *   %pokemon% - The Pokémon species name
     *   %player% - The player's name
     */
    public String newSpeciesMessage = "§a+%amount% PokéDollars §7for registering §b%pokemon% §7in your Pokédex!";

    /**
     * Message sent to the player when they capture a new form.
     * Placeholders:
     *   %amount% - The reward amount
     *   %pokemon% - The Pokémon species name
     *   %form% - The form name
     *   %player% - The player's name
     */
    public String newFormMessage = "§a+%amount% PokéDollars §7for registering a new form of §b%pokemon%§7!";

    /**
     * Message sent to the player when they capture a shiny for the first time.
     * Placeholders:
     *   %amount% - The reward amount
     *   %pokemon% - The Pokémon species name
     *   %player% - The player's name
     */
    public String firstShinyMessage = "§6✦ §a+%amount% PokéDollars §7for capturing your first shiny §b%pokemon%§7!";

    /**
     * Message sent to the player when they scan a new species.
     * Placeholders:
     *   %amount% - The reward amount
     *   %pokemon% - The Pokémon species name
     *   %player% - The player's name
     */
    public String scanNewSpeciesMessage = "§a+%amount% PokéDollars §7for discovering §b%pokemon%§7!";

    /**
     * Message sent to the player when they scan a new form.
     * Placeholders:
     *   %amount% - The reward amount
     *   %pokemon% - The Pokémon species name
     *   %form% - The form name
     *   %player% - The player's name
     */
    public String scanNewFormMessage = "§a+%amount% PokéDollars §7for discovering a new form of §b%pokemon%§7!";

    /**
     * Message sent to the player when they scan a new variation.
     * Placeholders:
     *   %amount% - The reward amount
     *   %pokemon% - The Pokémon species name
     *   %player% - The player's name
     */
    public String scanNewVariationMessage = "§a+%amount% PokéDollars §7for discovering new info about §b%pokemon%§7!";

    /**
     * Whether to enable bonus multiplier for shiny Pokémon on NEW entries.
     * This multiplier applies to newPokedexEntryReward and newFormReward when the Pokémon is shiny.
     * Note: This is separate from firstShinyReward which is a flat bonus.
     */
    public boolean enableShinyBonus = true;

    /**
     * Multiplier applied to the reward when capturing a shiny Pokémon that is also a new entry.
     * For example: 2.0 means double the normal reward for new entries.
     * This stacks with firstShinyReward if the shiny is also the first of that species.
     */
    public double shinyBonusMultiplier = 2.0;

    /**
     * Message sent when a shiny bonus multiplier is applied to a new entry.
     */
    public String shinyBonusMessage = "§6✦ Shiny Bonus! §7Reward multiplied by §e%multiplier%x§7!";

    // ==================== Static Methods ====================

    /**
     * Initializes the configuration system.
     * Creates the config file if it doesn't exist, or loads existing config.
     */
    public static void initialize() {
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(),
                "dyuus-academy-features/pokedex_rewards.json");

        // Create parent directories if they don't exist
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        // Load existing config or create new one
        if (configFile.exists()) {
            load();
        } else {
            INSTANCE = new PokedexRewardConfig();
            save();
        }

        DyuusAcademyFeatures.LOGGER.info("Pokedex Reward Config initialized - New entry reward: {} PokéDollars",
                INSTANCE.newPokedexEntryReward);
    }

    /**
     * Returns the current configuration instance.
     * @return The config instance, or a new default instance if not initialized.
     */
    public static PokedexRewardConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new PokedexRewardConfig();
        }
        return INSTANCE;
    }

    /**
     * Loads the configuration from the JSON file.
     */
    public static void load() {
        try (FileReader reader = new FileReader(configFile)) {
            INSTANCE = GSON.fromJson(reader, PokedexRewardConfig.class);

            // Validate loaded values
            if (INSTANCE == null) {
                INSTANCE = new PokedexRewardConfig();
            }

            DyuusAcademyFeatures.LOGGER.info("Pokedex Reward Config loaded successfully");
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to load Pokedex Reward Config, using defaults", e);
            INSTANCE = new PokedexRewardConfig();
            save(); // Save defaults
        }
    }

    /**
     * Saves the current configuration to the JSON file.
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(INSTANCE, writer);
            DyuusAcademyFeatures.LOGGER.info("Pokedex Reward Config saved successfully");
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to save Pokedex Reward Config", e);
        }
    }

    /**
     * Reloads the configuration from disk.
     * Useful for applying changes made to the config file without restarting.
     */
    public static void reload() {
        load();
        DyuusAcademyFeatures.LOGGER.info("Pokedex Reward Config reloaded");
    }
}