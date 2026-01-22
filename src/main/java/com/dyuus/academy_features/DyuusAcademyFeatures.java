package com.dyuus.academy_features;

import com.dyuus.academy_features.command.PokeDollarsCommand;
import com.dyuus.academy_features.command.ShopCommand;
import com.dyuus.academy_features.config.ShopConfigManager;
import com.dyuus.academy_features.currency.CurrencyManager;
// import com.dyuus.academy_features.integration.CobblemonIntegration;
import com.dyuus.academy_features.network.ShopNetworking;
import com.dyuus.academy_features.screen.ShopScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DyuusAcademyFeatures implements ModInitializer {
	public static final String MOD_ID = "dyuus-shop";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ScreenHandlerType<ShopScreenHandler> SHOP_SCREEN_HANDLER =
			Registry.register(
					Registries.SCREEN_HANDLER,
					Identifier.of(MOD_ID, "shop"),
					new ExtendedScreenHandlerType<>(
							ShopScreenHandler::new,
							ShopScreenHandler.Data.PACKET_CODEC  // Utiliser PACKET_CODEC au lieu de CODEC
					)
			);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Dyuus Shop");

		ShopConfigManager.initialize();
		CurrencyManager.initialize();

		ShopNetworking.registerPackets();
		ShopNetworking.registerServerReceivers();

		ShopCommand.register();
		PokeDollarsCommand.register();
		// CobblemonIntegration.initialize();

		LOGGER.info("Dyuus Shop initialized successfully");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
