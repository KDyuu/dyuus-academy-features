package com.dyuus.academy_features;

import com.dyuus.academy_features.screen.ShopScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class DyuusAcademyFeaturesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Enregistrer l'écran GUI côté client
		HandledScreens.register(DyuusAcademyFeatures.SHOP_SCREEN_HANDLER, ShopScreen::new);

		DyuusAcademyFeatures.LOGGER.info("Dyuus Shop client initialized");
	}
}
