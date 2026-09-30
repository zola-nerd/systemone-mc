package com.zolanerd.jev.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class JevKeys {
	public static final String CATEGORY = "category.jev";

	public static KeyMapping radial;
	public static KeyMapping toggleAi;
	public static KeyMapping settings;

	private JevKeys() {
	}

	public static void register() {
		radial = register("key.jev.radial", GLFW.GLFW_KEY_G);
		toggleAi = register("key.jev.toggle_ai", GLFW.GLFW_KEY_K);
		settings = register("key.jev.settings", GLFW.GLFW_KEY_J);
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
