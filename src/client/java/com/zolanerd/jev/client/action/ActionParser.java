package com.zolanerd.jev.client.action;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the flat action object returned by the sidecar.
 */
public final class ActionParser {
	private ActionParser() {
	}

	public static Action parse(String json) {
		if (json == null || json.isBlank()) {
			return Action.stop();
		}
		String name = stringField(json, "action");
		if (name == null || name.isBlank()) {
			return Action.stop();
		}
		Action.Type type;
		try {
			type = Action.Type.valueOf(name.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return Action.stop();
		}
		double x = numberField(json, "x", 0);
		double y = numberField(json, "y", 0);
		double z = numberField(json, "z", 0);
		String target = stringField(json, "target");
		int slot = (int) numberField(json, "slot", -1);
		String message = stringField(json, "message");
		boolean enabled = boolField(json, "enabled", true);
		boolean sprint = boolField(json, "sprint", false);
		return switch (type) {
			case STOP -> Action.stop();
			case MOVE_TO -> Action.moveTo(x, y, z, sprint);
			case LOOK_AT -> Action.lookAt(x, y, z);
			case ATTACK -> Action.attack(target == null ? "" : target);
			case USE_ITEM -> Action.useItem(slot);
			case PLACE_BLOCK -> newParsed(Action.Type.PLACE_BLOCK, x, y, z);
			case BREAK_BLOCK -> newParsed(Action.Type.BREAK_BLOCK, x, y, z);
			case JUMP -> Action.jump();
			case SNEAK -> Action.sneak(enabled);
			case HOTBAR_SELECT -> Action.hotbar(slot);
			case SAY -> Action.say(message == null ? "" : message);
		};
	}

	private static Action newParsed(Action.Type type, double x, double y, double z) {
		if (type == Action.Type.PLACE_BLOCK) {
			return Action.place(net.minecraft.core.BlockPos.containing(x, y, z));
		}
		return Action.breakBlock(net.minecraft.core.BlockPos.containing(x, y, z));
	}

	static String stringField(String json, String key) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
			.matcher(json);
		if (!matcher.find()) {
			return null;
		}
		return unescape(matcher.group(1));
	}

	static double numberField(String json, String key, double fallback) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)")
			.matcher(json);
		if (!matcher.find()) {
			return fallback;
		}
		try {
			return Double.parseDouble(matcher.group(1));
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	static boolean boolField(String json, String key, boolean fallback) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)")
			.matcher(json);
		if (!matcher.find()) {
			return fallback;
		}
		return "true".equals(matcher.group(1));
	}

	private static String unescape(String raw) {
		StringBuilder out = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c != '\\' || i + 1 >= raw.length()) {
				out.append(c);
				continue;
			}
			char next = raw.charAt(++i);
			switch (next) {
				case '"' -> out.append('"');
				case '\\' -> out.append('\\');
				case 'n' -> out.append('\n');
				case 'r' -> out.append('\r');
				case 't' -> out.append('\t');
				default -> out.append(next);
			}
		}
		return out.toString();
	}
}
