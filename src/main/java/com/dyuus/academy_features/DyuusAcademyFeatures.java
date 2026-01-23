package com.dyuus.academy_features;

import com.dyuus.academy_features.cobblemon.PokedexRewardHandler;
import com.dyuus.academy_features.command.PokeDollarsCommand;
import com.dyuus.academy_features.command.ShopCommand;
import com.dyuus.academy_features.command.TeraCommand;
import com.dyuus.academy_features.config.PokedexRewardConfig;
import com.dyuus.academy_features.config.ShopConfigManager;
import com.dyuus.academy_features.config.TeraConfigManager;
import com.dyuus.academy_features.currency.CurrencyManager;
import com.dyuus.academy_features.network.ShopNetworking;
import com.dyuus.academy_features.network.TeraNetworking;
import com.dyuus.academy_features.screen.ShopScreenHandler;
import com.dyuus.academy_features.screen.TeraScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DyuusAcademyFeatures implements ModInitializer {
	public static final String MOD_ID = "academy_features";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ScreenHandlerType<ShopScreenHandler> SHOP_SCREEN_HANDLER =
			Registry.register(
					Registries.SCREEN_HANDLER,
					id("shop"),
					new ExtendedScreenHandlerType<>(
							ShopScreenHandler::new,
							ShopScreenHandler.Data.PACKET_CODEC
					)
			);

	public static final ScreenHandlerType<TeraScreenHandler> TERA_SCREEN_HANDLER =
			Registry.register(
					Registries.SCREEN_HANDLER,
					id("tera"),
					new ExtendedScreenHandlerType<>(
							TeraScreenHandler::new,
							TeraScreenHandler.Data.PACKET_CODEC
					)
			);


	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Dyuu's Academy Features");

		// ==================== Configuration ====================
		ShopConfigManager.initialize();
		CurrencyManager.initialize();
		TeraConfigManager.initialize();

		// Initialize Pokedex reward configuration
		PokedexRewardConfig.initialize();

		// ==================== Networking ====================
		ShopNetworking.registerPackets();
		ShopNetworking.registerServerReceivers();
		TeraNetworking.registerPackets();
		TeraNetworking.registerServerReceivers();

		// ==================== Commands ====================
		ShopCommand.register();
		PokeDollarsCommand.register();
		TeraCommand.register();

		// ==================== Cobblemon Integration ====================
		// Initialize the Pokedex reward handler to listen for new catches
		PokedexRewardHandler.initialize();

		LOGGER.info("Dyuu's Academy Features initialized successfully");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}