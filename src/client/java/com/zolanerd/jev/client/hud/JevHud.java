package com.zolanerd.jev.client.hud;

import com.zolanerd.jev.client.goal.GoalManager;
import com.zolanerd.jev.client.net.SidecarClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class JevHud {
	private JevHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(JevHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.options.hideGui) {
			return;
		}
		Font font = minecraft.font;
		GoalManager goals = GoalManager.get();
		boolean online = SidecarClient.online();
		String ai = "AI: " + (goals.aiEnabled() ? "ON" : "OFF");
		String goal = "Goal: " + goals.label();
		String sidecar = online ? "Jev: sidecar online" : "Jev: sidecar offline (dummy)";
		int width = Math.max(font.width(ai), Math.max(font.width(goal), font.width(sidecar)));
		graphics.fill(2, 2, 10 + width, 38, 0x80000000);
		graphics.drawString(font, ai, 6, 6, goals.aiEnabled() ? 0x55FF55 : 0xFF6666, false);
		graphics.drawString(font, goal, 6, 16, 0xFFFFFF, false);
		graphics.drawString(font, sidecar, 6, 26, online ? 0xA8E6A8 : 0xFFCC66, false);
	}
}
