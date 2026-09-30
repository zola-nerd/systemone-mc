package com.zolanerd.jev.client.goal;

public enum GoalType {
	NONE,
	PROTECT,
	FOLLOW,
	BUILD,
	IDLE,
	SURVIVE;

	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	public static GoalType fromCommand(String token) {
		if (token == null) {
			return null;
		}
		return switch (token.toLowerCase(java.util.Locale.ROOT)) {
			case "protect" -> PROTECT;
			case "follow" -> FOLLOW;
			case "build" -> BUILD;
			case "idle" -> IDLE;
			case "survive" -> SURVIVE;
			case "stop", "none" -> NONE;
			default -> null;
		};
	}
}
