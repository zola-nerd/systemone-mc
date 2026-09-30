package com.zolanerd.jev.client.chat;

import com.zolanerd.jev.client.action.ActionExecutor;
import com.zolanerd.jev.client.goal.GoalManager;
import com.zolanerd.jev.client.goal.GoalType;
import com.zolanerd.jev.client.tick.JevTick;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Chat lines that start with ".jev " stay on the client. The same keywords
 * the radial menu uses are matched here before any LLM sidecar is involved.
 */
public final class JevChat {
	private JevChat() {
	}

	public static void register() {
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> !tryConsume(message));
	}

	public static boolean tryConsume(String message) {
		if (message == null) {
			return false;
		}
		String trimmed = message.stripLeading();
		String lower = trimmed.toLowerCase(Locale.ROOT);
		String rest;
		if (lower.equals(".jev")) {
			rest = "";
		} else if (lower.startsWith(".jev ")) {
			rest = trimmed.substring(5).trim();
		} else {
			return false;
		}
		handle(rest);
		return true;
	}

	private static void handle(String rest) {
		if (rest.isEmpty()) {
			feedback(help());
			return;
		}
		String[] parts = rest.split("\\s+", 2);
		String command = parts[0].toLowerCase(Locale.ROOT);
		String args = parts.length > 1 ? parts[1].trim() : "";
		GoalManager goals = GoalManager.get();
		switch (command) {
			case "protect", "follow", "build", "idle", "survive", "stop" -> {
				GoalType type = GoalType.fromCommand(command);
				goals.setGoal(type, args);
				feedback(withAi(switch (type) {
					case PROTECT -> args.isEmpty() ? "Protecting the nearest player." : "Protecting " + args + ".";
					case FOLLOW -> args.isEmpty() ? "Following the nearest player." : "Following " + args + ".";
					case BUILD -> "Building a 5x5x3 dirt hut.";
					case IDLE -> "Idling.";
					case SURVIVE -> "Survive mode.";
					case NONE -> "Stopped.";
				}));
			}
			case "ai" -> {
				boolean enabled = switch (args.toLowerCase(Locale.ROOT)) {
					case "on", "true", "1" -> true;
					case "off", "false", "0" -> false;
					default -> !goals.aiEnabled();
				};
				goals.setAiEnabled(enabled);
				if (!enabled) {
					ActionExecutor.release(Minecraft.getInstance());
				}
				feedback(enabled ? "AI on." : "AI off.");
			}
			case "settings" -> {
				JevTick.openSettingsLater();
				feedback("Opening settings.");
			}
			case "help" -> feedback(help());
			default -> feedback("Unknown command. " + help());
		}
	}

	public static String withAi(String message) {
		if (!GoalManager.get().aiEnabled()) {
			return message + " AI is off (press K).";
		}
		return message;
	}

	public static void feedback(String text) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui == null) {
			return;
		}
		minecraft.gui.getChat().addMessage(Component.literal("[Jev] " + text));
	}

	private static String help() {
		return "Commands: protect [name], follow [name], build, idle, survive, stop, ai on, ai off, settings.";
	}
}
