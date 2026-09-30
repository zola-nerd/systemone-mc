package com.zolanerd.jev.client;

import com.zolanerd.jev.client.chat.JevChat;
import com.zolanerd.jev.client.config.JevConfig;
import com.zolanerd.jev.client.hud.JevHud;
import com.zolanerd.jev.client.tick.JevTick;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JevClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Jev");

	@Override
	public void onInitializeClient() {
		JevConfig.load();
		JevKeys.register();
		JevChat.register();
		JevHud.register();
		JevTick.register();
		LOGGER.info("Jev mod loaded");
	}
}
