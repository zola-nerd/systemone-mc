package com.zolanerd.systemone_mc.client;

import com.zolanerd.systemone_mc.client.chat.SystemOneMcChat;
import com.zolanerd.systemone_mc.client.config.SystemOneMcConfig;
import com.zolanerd.systemone_mc.client.hud.SystemOneMcHud;
import com.zolanerd.systemone_mc.client.tick.SystemOneMcTick;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SystemOneMcClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("System One MC");

	@Override
	public void onInitializeClient() {
		SystemOneMcConfig.load();
		SystemOneMcKeys.register();
		SystemOneMcChat.register();
		SystemOneMcHud.register();
		SystemOneMcTick.register();
		LOGGER.info("System One MC mod loaded");
	}
}
