package com.zolanerd.systemone_mc.client.goal;

import com.zolanerd.systemone_mc.client.action.Action;
import com.zolanerd.systemone_mc.client.world.WorldQuery;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Keyword goal planner. Runs on the client before the sidecar is consulted.
 * A non-STOP action from POST /v1/systemone overrides the action returned here.
 */
public final class GoalPlanner {
	private static final double PLACE_REACH = 3.6;
	private static final int DIG_LENGTH = 8;

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
			case FIGHT -> fight(player, goals);
			case DIG -> dig(player, goals);
		};
	}

	private static Action fight(LocalPlayer player, GoalManager goals) {
		Player ally = WorldQuery.findPlayer(player, goals.targetName());
		Monster threat = ally == null ? null : WorldQuery.nearestMonsterNear(player, ally, 8);
		if (threat == null) {
			threat = WorldQuery.nearestMonster(player, 8);
		}
		if (threat != null) {
			if (player.distanceTo(threat) > 3.2) {
				return Action.moveTo(threat.getX(), threat.getY(), threat.getZ());
			}
			return Action.attack(threat.getStringUUID());
		}
		if (ally == null) {
			return goals.notice("fight-missing", "No nearby player to fight with.");
		}
		if (player.distanceTo(ally) > 4.5) {
			return Action.moveTo(ally.getX(), ally.getY(), ally.getZ());
		}
		return Action.lookAt(ally.getX(), ally.getEyeY(), ally.getZ());
	}

	private static Action dig(LocalPlayer player, GoalManager goals) {
		if (!goals.digPlanned()) {
			goals.setDigQueue(tunnelAhead(player));
		}
		while (goals.hasDigWork()) {
			BlockPos pos = goals.currentDigBlock();
			BlockState state = player.level().getBlockState(pos);
			if (state.isAir() || state.liquid() || !state.isSolid()) {
				goals.advanceDig();
				continue;
			}
			int tool = WorldQuery.findHotbar(player, stack ->
				stack.getItem() instanceof PickaxeItem || stack.getItem() instanceof ShovelItem);
			if (tool < 0) {
				Action missing = goals.notice("dig-tool", "No pickaxe or shovel in the hotbar. Dig stopped.");
				goals.setGoal(GoalType.IDLE, "");
				return missing.type() == Action.Type.SAY ? missing : Action.stop();
			}
			if (player.getInventory().selected != tool) {
				return Action.hotbar(tool);
			}
			Vec3 center = Vec3.atCenterOf(pos);
			if (WorldQuery.horizontalDistance(player, center.x, center.z) > PLACE_REACH) {
				return Action.moveTo(center.x, pos.getY(), center.z);
			}
			return Action.breakBlock(pos);
		}
		Action done = goals.notice("dig-done", "Tunnel finished.");
		goals.setGoal(GoalType.IDLE, "");
		return done.type() == Action.Type.SAY ? done : Action.stop();
	}

	/**
	 * 1x2 corridor ahead of the player: foot, then head, for {@link #DIG_LENGTH} steps.
	 * Air, fluids, bedrock, barrier, and command blocks are left out.
	 */
	private static List<BlockPos> tunnelAhead(LocalPlayer player) {
		Direction forward = player.getDirection();
		BlockPos feet = player.blockPosition();
		List<BlockPos> blocks = new ArrayList<>();
		for (int step = 1; step <= DIG_LENGTH; step++) {
			BlockPos foot = feet.relative(forward, step);
			offerDig(player, blocks, foot);
			offerDig(player, blocks, foot.above());
		}
		return blocks;
	}

	private static void offerDig(LocalPlayer player, List<BlockPos> blocks, BlockPos pos) {
		BlockState state = player.level().getBlockState(pos);
		if (state.isAir() || state.liquid()) {
			return;
		}
		if (!state.getFluidState().isEmpty() && !state.isSolid()) {
			return;
		}
		if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)
			|| state.is(Blocks.COMMAND_BLOCK)
			|| state.is(Blocks.REPEATING_COMMAND_BLOCK)
			|| state.is(Blocks.CHAIN_COMMAND_BLOCK)) {
			return;
		}
		blocks.add(pos);
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
