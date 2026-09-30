package com.zolanerd.jev.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zolanerd.jev.Jev;
import com.zolanerd.jev.client.JevClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JevConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static String sidecarUrl = Jev.DEFAULT_SIDECAR_URL;

	private JevConfig() {
	}

	public static String sidecarUrl() {
		return sidecarUrl;
	}

	public static void setSidecarUrl(String url) {
		sidecarUrl = url == null ? "" : url.trim();
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("jev.json");
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
			JevClient.LOGGER.warn("Could not read {}", path, exception);
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
			JevClient.LOGGER.warn("Could not write {}", path, exception);
		}
	}

	private static final class FileModel {
		String sidecarUrl = Jev.DEFAULT_SIDECAR_URL;
	}
}
