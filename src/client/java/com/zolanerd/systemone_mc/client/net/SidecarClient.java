package com.zolanerd.systemone_mc.client.net;

import com.zolanerd.systemone_mc.client.action.Action;
import com.zolanerd.systemone_mc.client.action.ActionParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP bridge to the Python sidecar.
 * <p>
 * Keyword goals are planned in Java first. This endpoint is the later hook for
 * Laya, jevos, or another LLM planner. The bundled sidecar always returns STOP,
 * and an unreachable sidecar is treated as that same dummy STOP.
 */
public final class SidecarClient {
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofMillis(400))
		.build();

	private static volatile boolean online;
	private static volatile String statusDetail = "not checked";
	private static volatile Action lastRemote = Action.stop();

	private SidecarClient() {
	}

	public static boolean online() {
		return online;
	}

	public static String statusDetail() {
		return statusDetail;
	}

	public static Action lastRemote() {
		return lastRemote == null ? Action.stop() : lastRemote;
	}

	public static void markOffline(String detail) {
		lastRemote = Action.stop();
		statusDetail = detail == null ? "offline" : detail;
		online = false;
	}

	public static CompletableFuture<Reply> postObservation(String baseUrl, String json) {
		String base = normalize(baseUrl);
		if (base.isEmpty()) {
			markOffline("empty address");
			return CompletableFuture.completedFuture(Reply.fail("empty address"));
		}
		HttpRequest request;
		try {
			request = HttpRequest.newBuilder(URI.create(base + "/v1/systemone"))
				.timeout(Duration.ofMillis(450))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();
		} catch (Exception exception) {
			markOffline(exception.getMessage());
			return CompletableFuture.completedFuture(Reply.fail(exception.getMessage()));
		}
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.handle((response, error) -> {
				if (error != null || response == null) {
					String message = error == null ? "no response" : rootMessage(error);
					markOffline(message);
					return Reply.fail(message);
				}
				if (response.statusCode() / 100 != 2) {
					String message = "HTTP " + response.statusCode();
					markOffline(message);
					return Reply.fail(message);
				}
				lastRemote = ActionParser.parse(response.body());
				statusDetail = "online";
				online = true;
				return Reply.ok(response.body());
			});
	}

	public static CompletableFuture<TestResult> test(String baseUrl) {
		String base = normalize(baseUrl);
		if (base.isEmpty()) {
			return CompletableFuture.completedFuture(new TestResult(false, "Empty address. Using the built-in dummy."));
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/health"))
					.timeout(Duration.ofMillis(1500))
					.GET()
					.build();
				HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() / 100 == 2) {
					online = true;
					statusDetail = "online";
					return new TestResult(true, "Connected (" + response.statusCode() + ").");
				}
				markOffline("HTTP " + response.statusCode());
				return new TestResult(false, "Sidecar returned HTTP " + response.statusCode() + ". Using the dummy.");
			} catch (Exception exception) {
				String message = rootMessage(exception);
				markOffline(message);
				return new TestResult(false, "Failed: " + message + ". Using the dummy.");
			}
		});
	}

	public static String normalize(String url) {
		if (url == null) {
			return "";
		}
		String trimmed = url.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		String suffix = "/v1/systemone";
		if (trimmed.endsWith(suffix)) {
			trimmed = trimmed.substring(0, trimmed.length() - suffix.length());
		}
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private static String rootMessage(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		String message = current.getMessage();
		if (message == null || message.isBlank()) {
			return current.getClass().getSimpleName();
		}
		return current.getClass().getSimpleName() + ": " + message;
	}

	public record Reply(boolean ok, String body, String error) {
		static Reply ok(String body) {
			return new Reply(true, body, "");
		}

		static Reply fail(String error) {
			return new Reply(false, "", error == null ? "failed" : error);
		}
	}

	public record TestResult(boolean ok, String message) {
	}
}
