package com.zolanerd.jev.client.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class WorldQuery {
	public static final double NEARBY_RANGE = 48.0;
	public static final double ACT_RANGE = 96.0;

	private WorldQuery() {
	}

	public static ClientLevel world(LocalPlayer player) {
		return (ClientLevel) player.level();
	}

	public static List<Player> nearbyPlayers(LocalPlayer self, double range) {
		List<Player> found = new ArrayList<>();
		for (Object entry : world(self).players()) {
			if (!(entry instanceof Player other) || other == self || !other.isAlive()) {
				continue;
			}
			if (other.distanceTo(self) <= range) {
				found.add(other);
			}
		}
		found.sort(Comparator.comparingDouble(self::distanceTo));
		return found;
	}

	public static Player findPlayer(LocalPlayer self, String name) {
		if (name == null || name.isBlank()) {
			List<Player> near = nearbyPlayers(self, ACT_RANGE);
			return near.isEmpty() ? null : near.get(0);
		}
		String want = name.trim();
		Player best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Object entry : world(self).players()) {
			if (!(entry instanceof Player other) || other == self || !other.isAlive()) {
				continue;
			}
			if (!other.getScoreboardName().equalsIgnoreCase(want)
				&& !other.getName().getString().equalsIgnoreCase(want)) {
				continue;
			}
			double distance = other.distanceTo(self);
			if (distance < bestDistance) {
				best = other;
				bestDistance = distance;
			}
		}
		return best;
	}

	public static Monster nearestMonster(LocalPlayer self, double range) {
		return nearestMonster(self, self.getBoundingBox().inflate(range), self);
	}

	public static Monster nearestMonsterNear(LocalPlayer self, Entity focus, double range) {
		return nearestMonster(self, focus.getBoundingBox().inflate(range), focus);
	}

	private static Monster nearestMonster(LocalPlayer self, AABB box, Entity focus) {
		Predicate<Entity> filter = entity -> entity instanceof Monster && entity.isAlive();
		List<?> found = self.level().getEntities(self, box, filter);
		Monster best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Object entry : found) {
			if (!(entry instanceof Monster monster)) {
				continue;
			}
			double distance = monster.distanceTo(focus);
			if (distance < bestDistance) {
				best = monster;
				bestDistance = distance;
			}
		}
		return best;
	}

	public static Entity findEntity(LocalPlayer self, String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		AABB box = self.getBoundingBox().inflate(48);
		List<?> found = self.level().getEntities(self, box, entity -> true);
		for (Object entry : found) {
			if (!(entry instanceof Entity entity) || !entity.isAlive()) {
				continue;
			}
			if (entity.getStringUUID().equalsIgnoreCase(key)
				|| entity.getName().getString().equalsIgnoreCase(key)
				|| (entity instanceof Player player && player.getScoreboardName().equalsIgnoreCase(key))) {
				return entity;
			}
		}
		return null;
	}

	public static int findHotbar(Player player, Predicate<ItemStack> test) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && test.test(stack)) {
				return slot;
			}
		}
		return -1;
	}

	public static double horizontalDistance(Entity entity, double x, double z) {
		double dx = entity.getX() - x;
		double dz = entity.getZ() - z;
		return Math.sqrt(dx * dx + dz * dz);
	}
}
