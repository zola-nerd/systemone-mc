package com.zolanerd.systemone_mc.client.chat;

import com.zolanerd.systemone_mc.SystemOneMc;
import com.zolanerd.systemone_mc.client.action.ActionExecutor;
import com.zolanerd.systemone_mc.client.goal.GoalManager;
import com.zolanerd.systemone_mc.client.goal.GoalType;
import com.zolanerd.systemone_mc.client.tick.SystemOneMcTick;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Chat lines that start with ".jev " (or the ".s1 " alias) stay on the client.
 * The same keywords the radial menu uses are matched here before any LLM sidecar is involved.
 */
public final class SystemOneMcChat {
	private SystemOneMcChat() {
	}

	public static void register() {
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> !tryConsume(message));
	}

	public static boolean tryConsume(String message) {
		if (message == null) {
			return false;
		}
		String trimmed = message.stripLeading();
		String rest = commandBody(trimmed);
		if (rest == null) {
			return false;
		}
		handle(rest);
		return true;
	}

	/**
	 * @return the text after the prefix, or null when the line is not a client command.
	 * ".jev " is checked first. ".s1 " is the same parser.
	 */
	private static String commandBody(String trimmed) {
		String lower = trimmed.toLowerCase(Locale.ROOT);
		for (String prefix : new String[] {SystemOneMc.CHAT_PREFIX, SystemOneMc.CHAT_PREFIX_ALT}) {
			String bare = prefix.stripTrailing();
			if (lower.equals(bare)) {
				return "";
			}
			if (lower.startsWith(prefix)) {
				return trimmed.substring(prefix.length()).trim();
			}
		}
		return null;
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
				SystemOneMcTick.openSettingsLater();
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
		minecraft.gui.getChat().addMessage(Component.literal("[System One MC] " + text));
	}

	private static String help() {
		return "Commands: protect [name], follow [name], build, idle, survive, stop, ai on, ai off, settings.";
	}
}
