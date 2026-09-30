package com.zolanerd.jev.client.goal;

import com.zolanerd.jev.client.action.Action;
import com.zolanerd.jev.client.world.WorldQuery;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Keyword goal planner. Runs on the client before the sidecar is consulted.
 * A non-STOP action from POST /v1/systemone overrides the action returned here.
 */
public final class GoalPlanner {
	private static final double PLACE_REACH = 3.6;

	private GoalPlanner() {
	}

	public static Action plan(LocalPlayer player) {
		if (player.isSpectator()) {
			return Action.stop();
		}
		GoalManager goals = GoalManager.get();
		return switch (goals.goal()) {
			case NONE, IDLE -> Action.stop();
			case PROTECT -> protect(player, goals);
			case FOLLOW -> follow(player, goals);
			case BUILD -> build(player, goals);
			case SURVIVE -> survive(player, goals);
		};
	}

	private static Action protect(LocalPlayer player, GoalManager goals) {
		Player target = WorldQuery.findPlayer(player, goals.targetName());
		if (target == null) {
			return goals.notice("protect-missing", "No nearby player to protect.");
		}
		Monster threat = WorldQuery.nearestMonsterNear(player, target, 8);
		if (threat == null) {
			threat = WorldQuery.nearestMonster(player, 6);
		}
		if (threat != null) {
			if (player.distanceTo(threat) > 3.2) {
				return Action.moveTo(threat.getX(), threat.getY(), threat.getZ());
			}
			return Action.attack(threat.getStringUUID());
		}
		if (player.distanceTo(target) > 4.5) {
			return Action.moveTo(target.getX(), target.getY(), target.getZ());
		}
		return Action.lookAt(target.getX(), target.getEyeY(), target.getZ());
	}

	private static Action follow(LocalPlayer player, GoalManager goals) {
		Player target = WorldQuery.findPlayer(player, goals.targetName());
		if (target == null) {
			return goals.notice("follow-missing", "No nearby player to follow.");
		}
		if (player.distanceTo(target) > 3.0) {
			return Action.moveTo(target.getX(), target.getY(), target.getZ());
		}
		return Action.lookAt(target.getX(), target.getEyeY(), target.getZ());
	}

	private static Action build(LocalPlayer player, GoalManager goals) {
		if (!goals.buildPlanned()) {
			goals.setBuildQueue(BuildPlan.hut(player));
		}
		int dirt = WorldQuery.findHotbar(player, stack -> stack.is(Items.DIRT));
		if (dirt < 0) {
			return goals.notice("build-dirt", "No dirt in the hotbar. The hut waits until you have some.");
		}
		while (goals.hasBuildWork()) {
			BlockPos pos = goals.currentBuildBlock();
			BlockState state = player.level().getBlockState(pos);
			if (state.is(Blocks.DIRT) || (!state.isAir() && !state.canBeReplaced())) {
				goals.advanceBuild();
				continue;
			}
			if (supportFace(player, pos) == null) {
				goals.bumpPlaceMiss();
				if (goals.placeMisses() > 8) {
					goals.advanceBuild();
				}
				return Action.stop();
			}
			goals.resetPlaceMiss();
			Vec3 center = Vec3.atCenterOf(pos);
			if (WorldQuery.horizontalDistance(player, center.x, center.z) > PLACE_REACH) {
				return Action.moveTo(center.x, pos.getY(), center.z);
			}
			return Action.place(pos);
		}
		Action done = goals.notice("build-done", "Dirt hut outline finished.");
		if (done.type() == Action.Type.SAY) {
			return done;
		}
		goals.setGoal(GoalType.IDLE, "");
		return Action.stop();
	}

	private static Action survive(LocalPlayer player, GoalManager goals) {
		int gapple = WorldQuery.findHotbar(player, stack ->
			stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE));
		if (player.getHealth() <= 10.0f && gapple >= 0) {
			return Action.useItem(gapple);
		}
		Monster close = WorldQuery.nearestMonster(player, 4);
		int food = WorldQuery.findHotbar(player, stack -> stack.has(DataComponents.FOOD));
		if (player.getFoodData().getFoodLevel() <= 14 && food >= 0 && close == null) {
			return Action.useItem(food);
		}
		Monster threat = WorldQuery.nearestMonster(player, 12);
		if (threat != null && player.distanceTo(threat) < 10) {
			Vec3 away = player.position().subtract(threat.position());
			double horizontal = Math.sqrt(away.x * away.x + away.z * away.z);
			double stepX = horizontal < 0.05 ? 1 : away.x / horizontal;
			double stepZ = horizontal < 0.05 ? 0 : away.z / horizontal;
			return Action.moveTo(player.getX() + stepX * 8, player.getY(), player.getZ() + stepZ * 8, true);
		}
		if (player.getHealth() < player.getMaxHealth() * 0.5f && gapple >= 0) {
			return Action.useItem(gapple);
		}
		return Action.stop();
	}

	public static Direction supportFace(LocalPlayer player, BlockPos target) {
		for (Direction direction : Direction.values()) {
			BlockPos neighbor = target.relative(direction);
			Direction face = direction.getOpposite();
			BlockState state = player.level().getBlockState(neighbor);
			if (state.isFaceSturdy(player.level(), neighbor, face)) {
				return direction;
			}
		}
		return null;
	}
}
