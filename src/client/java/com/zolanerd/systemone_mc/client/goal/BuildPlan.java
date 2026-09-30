package com.zolanerd.systemone_mc.client.goal;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * 5x5x3 dirt hut in front of the player: full floor, one-high walls with a door,
 * and a roof outline on the top layer.
 */
public final class BuildPlan {
	private BuildPlan() {
	}

	public static List<BlockPos> hut(LocalPlayer player) {
		Direction forward = player.getDirection();
		Direction right = forward.getClockWise();
		BlockPos door = player.blockPosition().relative(forward, 3);
		BlockPos corner = door.relative(right, -2);
		List<BlockPos> blocks = new ArrayList<>();

		for (int localZ = 0; localZ < 5; localZ++) {
			for (int localX = 0; localX < 5; localX++) {
				blocks.add(at(corner, right, forward, localX, localZ, 0));
			}
		}
		for (int localZ = 0; localZ < 5; localZ++) {
			for (int localX = 0; localX < 5; localX++) {
				if (!isEdge(localX, localZ) || isDoor(localX, localZ)) {
					continue;
				}
				blocks.add(at(corner, right, forward, localX, localZ, 1));
			}
		}
		for (int localZ = 0; localZ < 5; localZ++) {
			for (int localX = 0; localX < 5; localX++) {
				if (!isEdge(localX, localZ)) {
					continue;
				}
				blocks.add(at(corner, right, forward, localX, localZ, 2));
			}
		}
		return blocks;
	}

	private static boolean isEdge(int localX, int localZ) {
		return localX == 0 || localX == 4 || localZ == 0 || localZ == 4;
	}

	private static boolean isDoor(int localX, int localZ) {
		return localX == 2 && localZ == 0;
	}

	private static BlockPos at(BlockPos corner, Direction right, Direction forward, int localX, int localZ, int up) {
		return corner.relative(right, localX).relative(forward, localZ).offset(0, up, 0);
	}
}
