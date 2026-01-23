package com.dyuus.academy_features.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent;
import com.cobblemon.mod.common.api.pokedex.FormDexRecord;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.SpeciesDexRecord;
import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.config.PokedexRewardConfig;
import com.dyuus.academy_features.currency.CurrencyManager;
import kotlin.Unit;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Pokédex-related events from Cobblemon and rewards players for:
 *
 * CAPTURE REWARDS:
 * 1. New Species: First time capturing any form of a species
 * 2. New Form: First time capturing a specific form (species already caught)
 * 3. First Shiny: First time capturing a shiny of a species (even if normal was already caught)
 *
 * SCAN REWARDS:
 * 4. Scan New Species: First time scanning a species (adds to Pokédex as ENCOUNTERED)
 * 5. Scan New Form: First time scanning a specific form
 * 6. Scan New Variation: Scanning reveals new info (shiny state, gender, etc.)
 *
 * Shiny bonus multiplier applies to capture rewards 1 and 2 when the Pokémon is shiny.
 */
public class PokedexRewardHandler {

    /**
     * Data class to store the previous state before a Pokédex update.
     */
    private static class PreviousState {
        final PokedexEntryProgress formKnowledge;
        final boolean speciesWasCaught;
        final boolean speciesWasEncountered;
        final boolean hadSeenShiny;

        PreviousState(PokedexEntryProgress formKnowledge, boolean speciesWasCaught, boolean speciesWasEncountered, boolean hadSeenShiny) {
            this.formKnowledge = formKnowledge;
            this.speciesWasCaught = speciesWasCaught;
            this.speciesWasEncountered = speciesWasEncountered;
            this.hadSeenShiny = hadSeenShiny;
        }
    }

    /**
     * Stores the previous state for pending updates.
     * Key format: "playerUUID:speciesId:formName"
     */
    private static final Map<String, PreviousState> pendingUpdates = new ConcurrentHashMap<>();

    /**
     * Initializes the Pokédex reward system by subscribing to Cobblemon events.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        // Subscribe to PRE event to capture the previous state for both captures and scans
        CobblemonEvents.POKEDEX_DATA_CHANGED_PRE.subscribe(Priority.NORMAL, event -> {
            handlePokedexDataChangedPre(event);
            return Unit.INSTANCE;
        });

        // Subscribe to POST event to give rewards for both captures and scans
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.NORMAL, event -> {
            handlePokedexDataChangedPost(event);
            return Unit.INSTANCE;
        });

        DyuusAcademyFeatures.LOGGER.info("Pokedex Reward Handler initialized");
    }

    /**
     * Handles the PRE event to capture the previous state before the Pokédex is updated.
     * This handles BOTH captures (CAUGHT) and scans (ENCOUNTERED).
     *
     * @param event The Pokédex data changed PRE event
     */
    private static void handlePokedexDataChangedPre(PokedexDataChangedEvent.Pre event) {
        FormDexRecord formRecord = event.getRecord();
        SpeciesDexRecord speciesRecord = formRecord.getSpeciesDexRecord();

        // Capture the current state BEFORE the update
        PokedexEntryProgress previousFormKnowledge = formRecord.getKnowledge();
        boolean wasSpeciesAlreadyCaught = speciesRecord.hasAtLeast(PokedexEntryProgress.CAUGHT);
        boolean wasSpeciesAlreadyEncountered = speciesRecord.hasAtLeast(PokedexEntryProgress.ENCOUNTERED);
        boolean hadSeenShiny = formRecord.hasSeenShinyState(true);

        // Create a unique key for this update
        String speciesId = speciesRecord.getId().toString();
        String formName = formRecord.getFormName();
        String key = createKey(event.getPlayerUUID(), speciesId, formName);

        // Store the previous state (now includes encountered status)
        pendingUpdates.put(key, new PreviousState(
                previousFormKnowledge,
                wasSpeciesAlreadyCaught,
                wasSpeciesAlreadyEncountered,
                hadSeenShiny
        ));

        DyuusAcademyFeatures.LOGGER.debug(
                "Tracking Pokédex update - Species: {}, Form: {}, Knowledge: {}, PrevKnowledge: {}, WasCaught: {}, WasEncountered: {}, HadShiny: {}",
                speciesId, formName, event.getKnowledge(), previousFormKnowledge, wasSpeciesAlreadyCaught, wasSpeciesAlreadyEncountered, hadSeenShiny
        );
    }

    /**
     * Handles the POST event to give rewards after the Pokédex is updated.
     * Handles both CAUGHT (capture) and ENCOUNTERED (scan) events.
     *
     * @param event The Pokédex data changed POST event
     */
    private static void handlePokedexDataChangedPost(PokedexDataChangedEvent.Post event) {
        PokedexEntryProgress eventKnowledge = event.getKnowledge();

        // Only process CAUGHT or ENCOUNTERED entries
        if (eventKnowledge != PokedexEntryProgress.CAUGHT && eventKnowledge != PokedexEntryProgress.ENCOUNTERED) {
            return;
        }

        // Create the key to look up the previous state
        String speciesId = event.getRecord().getSpeciesDexRecord().getId().toString();
        String formName = event.getRecord().getFormName();
        String key = createKey(event.getPlayerUUID(), speciesId, formName);

        // Get and remove the previous state
        PreviousState previousState = pendingUpdates.remove(key);

        if (previousState == null) {
            DyuusAcademyFeatures.LOGGER.debug("No previous state found for key: {}", key);
            return;
        }

        // Get the player
        ServerPlayerEntity player = getPlayerFromUUID(event.getPlayerUUID());
        if (player == null) {
            DyuusAcademyFeatures.LOGGER.debug("Could not find player for UUID: {}", event.getPlayerUUID());
            return;
        }

        // Get Pokémon information
        String speciesName = event.getDataSource().getPokemon().getSpecies().getName();
        String pokemonFormName = event.getDataSource().getPokemon().getForm().getName();
        boolean isShiny = event.getDataSource().getPokemon().getShiny();

        PokedexRewardConfig config = PokedexRewardConfig.get();
        int totalReward = 0;

        // ==================== CAUGHT: Capture Rewards ====================
        if (eventKnowledge == PokedexEntryProgress.CAUGHT) {
            // Check if this is a NEW form/species registration (wasn't CAUGHT before)
            if (previousState.formKnowledge != PokedexEntryProgress.CAUGHT) {
                int baseReward;
                String messageTemplate;

                if (!previousState.speciesWasCaught) {
                    // NEW SPECIES: First time catching this species at all
                    baseReward = config.newPokedexEntryReward;
                    messageTemplate = config.newSpeciesMessage;
                    DyuusAcademyFeatures.LOGGER.info("Player {} registered NEW species: {} (Form: {})",
                            player.getName().getString(), speciesName, pokemonFormName);
                } else {
                    // NEW FORM: Species was caught before, but this is a new form
                    baseReward = config.newFormReward;
                    messageTemplate = config.newFormMessage;
                    DyuusAcademyFeatures.LOGGER.info("Player {} registered NEW form: {} - {}",
                            player.getName().getString(), speciesName, pokemonFormName);
                }

                // Apply shiny bonus multiplier if applicable
                double multiplier = 1.0;
                if (isShiny && config.enableShinyBonus) {
                    multiplier = config.shinyBonusMultiplier;
                }

                int entryReward = (int) Math.round(baseReward * multiplier);
                totalReward += entryReward;

                // Send entry reward message
                if (config.enableRewardMessage) {
                    String message = formatMessage(messageTemplate, entryReward, speciesName, pokemonFormName, player.getName().getString());
                    player.sendMessage(Text.literal(message), false);

                    // Send shiny bonus multiplier message
                    if (isShiny && config.enableShinyBonus && multiplier > 1.0) {
                        String shinyMessage = config.shinyBonusMessage
                                .replace("%multiplier%", String.format("%.1f", multiplier));
                        player.sendMessage(Text.literal(shinyMessage), false);
                    }
                }
            }

            // Check for first shiny reward (only for CAUGHT)
            if (isShiny && !previousState.hadSeenShiny && config.firstShinyReward > 0) {
                totalReward += config.firstShinyReward;

                DyuusAcademyFeatures.LOGGER.info("Player {} captured their FIRST SHINY: {} (Form: {})",
                        player.getName().getString(), speciesName, pokemonFormName);

                if (config.enableRewardMessage) {
                    String message = formatMessage(config.firstShinyMessage, config.firstShinyReward, speciesName, pokemonFormName, player.getName().getString());
                    player.sendMessage(Text.literal(message), false);
                }
            }
        }

        // ==================== ENCOUNTERED: Scan Rewards ====================
        else if (eventKnowledge == PokedexEntryProgress.ENCOUNTERED) {
            int scanReward = 0;
            String messageTemplate = null;

            // Check what new information was learned from the scan
            if (previousState.formKnowledge == PokedexEntryProgress.NONE) {
                // This form was never seen before
                if (!previousState.speciesWasEncountered) {
                    // NEW SPECIES: First time seeing this species at all
                    scanReward = config.scanNewSpeciesReward;
                    messageTemplate = config.scanNewSpeciesMessage;
                    DyuusAcademyFeatures.LOGGER.info("Player {} scanned NEW species: {}",
                            player.getName().getString(), speciesName);
                } else {
                    // NEW FORM: Species was seen before, but this is a new form
                    scanReward = config.scanNewFormReward;
                    messageTemplate = config.scanNewFormMessage;
                    DyuusAcademyFeatures.LOGGER.info("Player {} scanned NEW form: {} - {}",
                            player.getName().getString(), speciesName, pokemonFormName);
                }
            } else if (previousState.formKnowledge == PokedexEntryProgress.ENCOUNTERED) {
                // Form was already seen, but might have new variation info (gender, shiny state)
                // Check if we learned something new (shiny state we hadn't seen)
                if (isShiny && !previousState.hadSeenShiny) {
                    scanReward = config.scanNewVariationReward;
                    messageTemplate = config.scanNewVariationMessage;
                    DyuusAcademyFeatures.LOGGER.info("Player {} scanned NEW variation (shiny): {} - {}",
                            player.getName().getString(), speciesName, pokemonFormName);
                }
                // Note: Could also check for new gender, but that's handled internally by Cobblemon
            }

            if (scanReward > 0) {
                totalReward += scanReward;

                if (config.enableRewardMessage && messageTemplate != null) {
                    String message = formatMessage(messageTemplate, scanReward, speciesName, pokemonFormName, player.getName().getString());
                    player.sendMessage(Text.literal(message), false);
                }
            }
        }

        // ==================== Give total reward ====================

        if (totalReward > 0) {
            CurrencyManager.addBalance(player, totalReward);
            DyuusAcademyFeatures.LOGGER.info("Rewarded {} with {} PokéDollars total for Pokédex update",
                    player.getName().getString(), totalReward);
        }
    }

    /**
     * Creates a unique key for tracking pending updates.
     *
     * @param playerUUID The player's UUID
     * @param speciesId The species identifier
     * @param formName The form name
     * @return A unique key string
     */
    private static String createKey(UUID playerUUID, String speciesId, String formName) {
        return playerUUID.toString() + ":" + speciesId + ":" + formName.toLowerCase();
    }

    /**
     * Formats a message template with the provided values.
     *
     * @param template The message template with placeholders
     * @param amount The reward amount
     * @param pokemonName The Pokémon species name
     * @param formName The form name
     * @param playerName The player's name
     * @return The formatted message
     */
    private static String formatMessage(String template, int amount, String pokemonName, String formName, String playerName) {
        return template
                .replace("%amount%", String.valueOf(amount))
                .replace("%pokemon%", pokemonName)
                .replace("%form%", formName)
                .replace("%player%", playerName);
    }

    /**
     * Gets a ServerPlayerEntity from a UUID.
     * Uses Cobblemon's extension function which handles the server lookup internally.
     *
     * @param uuid The player's UUID
     * @return The ServerPlayerEntity, or null if not found/online
     */
    @SuppressWarnings("unchecked")
    private static ServerPlayerEntity getPlayerFromUUID(UUID uuid) {
        try {
            return (ServerPlayerEntity) com.cobblemon.mod.common.util.PlayerExtensionsKt.getPlayer(uuid);
        } catch (Exception e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to get player from UUID: {}", uuid, e);
            return null;
        }
    }
}