package com.zolanerd.jev.client.tick;

import com.zolanerd.jev.client.JevKeys;
import com.zolanerd.jev.client.action.Action;
import com.zolanerd.jev.client.action.ActionExecutor;
import com.zolanerd.jev.client.chat.JevChat;
import com.zolanerd.jev.client.config.JevConfig;
import com.zolanerd.jev.client.goal.GoalManager;
import com.zolanerd.jev.client.goal.GoalPlanner;
import com.zolanerd.jev.client.gui.JevSettingsScreen;
import com.zolanerd.jev.client.gui.RadialMenuScreen;
import com.zolanerd.jev.client.net.SidecarClient;
import com.zolanerd.jev.client.observe.ObservationSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Client tick. Keybinds are handled every tick. While AI is on, every second
 * client tick (about 10 Hz) builds an observation, asks the sidecar, and runs
 * either the sidecar action or the local keyword plan.
 */
public final class JevTick {
	private static int phase;
	private static boolean driving;
	private static CompletableFuture<SidecarClient.Reply> inflight;
	private static boolean openSettings;

	private JevTick() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(JevTick::onEndTick);
	}

	public static void openSettingsLater() {
		openSettings = true;
	}

	private static void onEndTick(Minecraft minecraft) {
		ActionExecutor.tickCooldown();
		handleKeys(minecraft);
		flushScheduledScreen(minecraft);

		GoalManager goals = GoalManager.get();
		LocalPlayer player = minecraft.player;
		if (!goals.aiEnabled() || player == null || minecraft.level == null) {
			if (driving) {
				ActionExecutor.release(minecraft);
				driving = false;
			}
			return;
		}
		if (minecraft.screen != null) {
			ActionExecutor.release(minecraft);
			driving = false;
			return;
		}

		phase++;
		if (phase % 2 != 0) {
			return;
		}
		driving = true;
		collectSidecar();
		String observation = ObservationSerializer.capture(player);
		if (inflight == null) {
			inflight = SidecarClient.postObservation(JevConfig.sidecarUrl(), observation);
		}

		Action local = GoalPlanner.plan(player);
		Action remote = SidecarClient.lastRemote();
		// STOP from the dummy sidecar means "no planner override".
		Action chosen = SidecarClient.online() && remote.type() != Action.Type.STOP ? remote : local;
		ActionExecutor.execute(minecraft, chosen);
	}

	private static void handleKeys(Minecraft minecraft) {
		while (JevKeys.radial.consumeClick()) {
			if (minecraft.player != null && minecraft.screen == null) {
				minecraft.setScreen(new RadialMenuScreen());
			}
		}
		while (JevKeys.toggleAi.consumeClick()) {
			GoalManager goals = GoalManager.get();
			goals.toggleAi();
			if (!goals.aiEnabled()) {
				ActionExecutor.release(minecraft);
				driving = false;
			}
			JevChat.feedback(goals.aiEnabled() ? "AI on." : "AI off.");
		}
		while (JevKeys.settings.consumeClick()) {
			if (minecraft.screen == null) {
				minecraft.setScreen(new JevSettingsScreen(null));
			}
		}
	}

	private static void flushScheduledScreen(Minecraft minecraft) {
		if (!openSettings || minecraft.screen != null || minecraft.player == null) {
			return;
		}
		openSettings = false;
		minecraft.setScreen(new JevSettingsScreen(null));
	}

	private static void collectSidecar() {
		if (inflight == null || !inflight.isDone()) {
			return;
		}
		inflight = null;
	}
}
