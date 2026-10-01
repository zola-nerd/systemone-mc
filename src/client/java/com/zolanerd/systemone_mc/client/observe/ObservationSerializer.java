package com.zolanerd.systemone_mc.client.observe;

import com.zolanerd.systemone_mc.client.goal.GoalManager;
import com.zolanerd.systemone_mc.client.util.JsonText;
import com.zolanerd.systemone_mc.client.world.WorldQuery;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Compact JSON snapshot for the sidecar. Keyword goals are chosen on the Java
 * side; this payload is what a later LLM planner would read.
 */
public final class ObservationSerializer {
	private ObservationSerializer() {
	}

	public static String capture(LocalPlayer player) {
		GoalManager goals = GoalManager.get();
		String dimension = "unknown";
		ResourceLocation key = player.level().dimension().location();
		if (key != null) {
			dimension = key.toString();
		}

		StringBuilder json = new StringBuilder(512);
		json.append('{');
		json.append("\"player\":{");
		num(json, "x", player.getX(), true);
		num(json, "y", player.getY(), false);
		num(json, "z", player.getZ(), false);
		num(json, "yaw", player.getYRot(), false);
		num(json, "pitch", player.getXRot(), false);
		num(json, "health", player.getHealth(), false);
		num(json, "food", player.getFoodData().getFoodLevel(), false);
		json.append(",\"onGround\":").append(player.onGround());
		json.append("},\"goal\":{");
		str(json, "type", goals.goal().id(), true);
		str(json, "target", goals.targetName(), false);
		json.append("},\"ai\":").append(goals.aiEnabled());
		json.append(",\"dimension\":\"").append(JsonText.escape(dimension)).append('"');
		json.append(",\"time\":").append(player.level().getGameTime());
		json.append(",\"players\":[");
		List<Player> players = WorldQuery.nearbyPlayers(player, WorldQuery.ACT_RANGE);
		int playerCount = Math.min(8, players.size());
		for (int i = 0; i < playerCount; i++) {
			Player other = players.get(i);
			if (i > 0) {
				json.append(',');
			}
			json.append('{');
			str(json, "name", other.getScoreboardName(), true);
			num(json, "dist", other.distanceTo(player), false);
			num(json, "x", other.getX(), false);
			num(json, "y", other.getY(), false);
			num(json, "z", other.getZ(), false);
			json.append('}');
		}
		json.append("],\"entities\":[");
		appendEntities(json, player);
		json.append("],\"inventory\":{");
		json.append("\"selected\":").append(player.getInventory().selected);
		json.append(",\"hotbar\":[");
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < 9; slot++) {
			if (slot > 0) {
				json.append(',');
			}
			ItemStack stack = inventory.getItem(slot);
			String label = stack.isEmpty() ? "empty" : stack.getHoverName().getString() + " x" + stack.getCount();
			json.append('"').append(JsonText.escape(label)).append('"');
		}
		json.append("]}}");
		return json.toString();
	}

	private static void appendEntities(StringBuilder json, LocalPlayer player) {
		AABB box = player.getBoundingBox().inflate(24);
		List<Entity> entities = new ArrayList<>();
		for (Object entry : player.level().getEntities(player, box, entity -> entity.isAlive() && !(entity instanceof Player))) {
			if (entry instanceof Entity entity) {
				entities.add(entity);
			}
		}
		entities.sort(Comparator.comparingDouble(player::distanceTo));
		int count = Math.min(8, entities.size());
		for (int i = 0; i < count; i++) {
			Entity entity = entities.get(i);
			if (i > 0) {
				json.append(',');
			}
			ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			String type = id == null ? "entity" : id.getPath();
			json.append('{');
			str(json, "type", type, true);
			str(json, "id", entity.getStringUUID(), false);
			num(json, "dist", entity.distanceTo(player), false);
			num(json, "x", entity.getX(), false);
			num(json, "y", entity.getY(), false);
			num(json, "z", entity.getZ(), false);
			json.append('}');
		}
	}

	private static void str(StringBuilder json, String key, String value, boolean first) {
		if (!first) {
			json.append(',');
		}
		json.append('"').append(key).append("\":\"").append(JsonText.escape(value)).append('"');
	}

	private static void num(StringBuilder json, String key, double value, boolean first) {
		if (!first) {
			json.append(',');
		}
		json.append('"').append(key).append("\":");
		json.append(String.format(Locale.ROOT, "%.2f", value));
	}
}
