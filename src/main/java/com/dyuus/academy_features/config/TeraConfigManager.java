package com.dyuus.academy_features.config;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TeraConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("dyuus-academy-features/dyuus-tera-config.json");

    private static TeraConfig config;

    public static void initialize() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, TeraConfig.class);
                DyuusAcademyFeatures.LOGGER.info("Loaded tera config from file");
            } else {
                config = new TeraConfig();
                save();
                DyuusAcademyFeatures.LOGGER.info("Created default tera config");
            }
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to load tera config", e);
            config = new TeraConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            DyuusAcademyFeatures.LOGGER.error("Failed to save tera config", e);
        }
    }

    public static TeraConfig getConfig() {
        return config;
    }
}
