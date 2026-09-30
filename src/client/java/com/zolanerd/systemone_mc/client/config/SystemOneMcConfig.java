package com.zolanerd.systemone_mc.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zolanerd.systemone_mc.SystemOneMc;
import com.zolanerd.systemone_mc.client.SystemOneMcClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SystemOneMcConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static String sidecarUrl = SystemOneMc.DEFAULT_SIDECAR_URL;

	private SystemOneMcConfig() {
	}

	public static String sidecarUrl() {
		return sidecarUrl;
	}

	public static void setSidecarUrl(String url) {
		sidecarUrl = url == null ? "" : url.trim();
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("systemone_mc.json");
	}

	public static void load() {
		Path path = path();
		if (!Files.isRegularFile(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			FileModel model = GSON.fromJson(reader, FileModel.class);
			if (model != null && model.sidecarUrl != null) {
				sidecarUrl = model.sidecarUrl.trim();
			}
		} catch (Exception exception) {
			SystemOneMcClient.LOGGER.warn("Could not read {}", path, exception);
		}
	}

	public static void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			FileModel model = new FileModel();
			model.sidecarUrl = sidecarUrl;
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(model, writer);
			}
		} catch (IOException exception) {
			SystemOneMcClient.LOGGER.warn("Could not write {}", path, exception);
		}
	}

	private static final class FileModel {
		String sidecarUrl = SystemOneMc.DEFAULT_SIDECAR_URL;
	}
}
