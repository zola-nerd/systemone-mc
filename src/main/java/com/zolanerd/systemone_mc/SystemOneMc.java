package com.zolanerd.systemone_mc;

/**
 * Shared mod constants. Gameplay code lives in the client source set.
 */
public final class SystemOneMc {
	public static final String MOD_ID = "systemone_mc";
	public static final String DISPLAY_NAME = "System One MC";
	/** Primary chat prefix. Kept so existing ".jev protect Steve" lines still work. */
	public static final String CHAT_PREFIX = ".jev ";
	/** Short alias. Same command parser as {@link #CHAT_PREFIX}. */
	public static final String CHAT_PREFIX_ALT = ".s1 ";
	public static final String DEFAULT_SIDECAR_URL = "http://127.0.0.1:8765";

	private SystemOneMc() {
	}
}
