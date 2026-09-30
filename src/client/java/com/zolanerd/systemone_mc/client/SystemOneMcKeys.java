package com.zolanerd.systemone_mc.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class SystemOneMcKeys {
	public static final String CATEGORY = "category.systemone_mc";

	public static KeyMapping radial;
	public static KeyMapping toggleAi;
	public static KeyMapping settings;

	private SystemOneMcKeys() {
	}

	public static void register() {
		radial = register("key.systemone_mc.radial", GLFW.GLFW_KEY_G);
		toggleAi = register("key.systemone_mc.toggle_ai", GLFW.GLFW_KEY_K);
		settings = register("key.systemone_mc.settings", GLFW.GLFW_KEY_J);
	}

	private static KeyMapping register(String translationKey, int glfwKey) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(
			translationKey,
			InputConstants.Type.KEYSYM,
			glfwKey,
			CATEGORY
		));
	}
}
