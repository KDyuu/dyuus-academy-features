package com.dyuus.academy_features;

import com.dyuus.academy_features.network.TeraNetworkingClient;
import com.dyuus.academy_features.screen.ShopScreen;
import com.dyuus.academy_features.screen.TeraTypeSelectionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class DyuusAcademyFeaturesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register screens
		HandledScreens.register(DyuusAcademyFeatures.SHOP_SCREEN_HANDLER, ShopScreen::new);
		HandledScreens.register(DyuusAcademyFeatures.TERA_SCREEN_HANDLER, TeraTypeSelectionScreen::new);

		// Register client-side networking receivers
		TeraNetworkingClient.registerClientReceivers();

		DyuusAcademyFeatures.LOGGER.info("Dyuu's Academy Features Client initialized");
	}
}