package com.zolanerd.systemone_mc.client.action;

import com.zolanerd.systemone_mc.SystemOneMc;
import com.zolanerd.systemone_mc.client.goal.GoalPlanner;
import com.zolanerd.systemone_mc.client.world.WorldQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Drives the local player with movement keys, look vectors, and the normal
 * client interaction manager. Does not teleport or spoof position packets.
 */
public final class ActionExecutor {
	private static String lastSay = "";
	private static long lastSayTime = Long.MIN_VALUE;
	private static int placeCooldown;

	private ActionExecutor() {
	}

	public static void tickCooldown() {
		if (placeCooldown > 0) {
			placeCooldown--;
		}
	}

	public static void release(Minecraft minecraft) {
		if (minecraft.options == null) {
			return;
		}
		minecraft.options.keyUp.setDown(false);
		minecraft.options.keyDown.setDown(false);
		minecraft.options.keyLeft.setDown(false);
		minecraft.options.keyRight.setDown(false);
		minecraft.options.keyJump.setDown(false);
		minecraft.options.keyShift.setDown(false);
		minecraft.options.keySprint.setDown(false);
		minecraft.options.keyAttack.setDown(false);
		minecraft.options.keyUse.setDown(false);
	}

	public static void execute(Minecraft minecraft, Action action) {
		LocalPlayer player = minecraft.player;
		if (player == null || action == null) {
			return;
		}
		release(minecraft);
		switch (action.type()) {
			case STOP -> {
			}
			case MOVE_TO -> moveTo(minecraft, player, action);
			case LOOK_AT -> look(player, action.x(), action.y(), action.z());
			case ATTACK -> attack(minecraft, player, action.target());
			case USE_ITEM -> useItem(minecraft, player, action.slot());
			case PLACE_BLOCK -> place(minecraft, player, action.blockPos());
			case BREAK_BLOCK -> breakBlock(minecraft, player, action.blockPos());
			case JUMP -> minecraft.options.keyJump.setDown(true);
			case SNEAK -> minecraft.options.keyShift.setDown(action.enabled());
			case HOTBAR_SELECT -> selectHotbar(player, action.slot());
			case SAY -> say(minecraft, player, action.message());
		}
	}

	private static void moveTo(Minecraft minecraft, LocalPlayer player, Action action) {
		look(player, action.x(), player.getEyeY(), action.z());
		double distance = WorldQuery.horizontalDistance(player, action.x(), action.z());
		if (distance > 1.2) {
			minecraft.options.keyUp.setDown(true);
			minecraft.options.keySprint.setDown(action.sprint());
			if (player.horizontalCollision && player.onGround()) {
				minecraft.options.keyJump.setDown(true);
			}
		}
	}

	private static void look(LocalPlayer player, double x, double y, double z) {
		player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(x, y, z));
	}

	private static void attack(Minecraft minecraft, LocalPlayer player, String key) {
		Entity target = WorldQuery.findEntity(player, key);
		if (target == null || minecraft.gameMode == null) {
			return;
		}
		look(player, target.getX(), target.getEyeY(), target.getZ());
		if (player.distanceTo(target) > 3.4) {
			minecraft.options.keyUp.setDown(true);
			return;
		}
		if (player.getAttackStrengthScale(0.0f) >= 0.9f) {
			minecraft.gameMode.attack(player, target);
			player.swing(InteractionHand.MAIN_HAND);
		}
	}

	private static void useItem(Minecraft minecraft, LocalPlayer player, int slot) {
		selectHotbar(player, slot);
		minecraft.options.keyUse.setDown(true);
	}

	private static void place(Minecraft minecraft, LocalPlayer player, BlockPos pos) {
		if (minecraft.gameMode == null || placeCooldown > 0) {
			return;
		}
		BlockState state = player.level().getBlockState(pos);
		if (state.is(net.minecraft.world.level.block.Blocks.DIRT)) {
			return;
		}
		if (!state.isAir() && !state.canBeReplaced()) {
			return;
		}
		int slot = WorldQuery.findHotbar(player, stack -> stack.is(net.minecraft.world.item.Items.DIRT));
		if (slot < 0) {
			say(minecraft, player, "No dirt in the hotbar.");
			return;
		}
		Direction clicked = GoalPlanner.supportFace(player, pos);
		if (clicked == null) {
			return;
		}
		selectHotbar(player, slot);
		BlockPos neighbor = pos.relative(clicked);
		Direction face = clicked.getOpposite();
		Vec3 hit = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		look(player, hit.x, hit.y, hit.z);
		minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, neighbor, false));
		player.swing(InteractionHand.MAIN_HAND);
		placeCooldown = 4;
	}

	private static void breakBlock(Minecraft minecraft, LocalPlayer player, BlockPos pos) {
		if (minecraft.gameMode == null) {
			return;
		}
		Vec3 center = Vec3.atCenterOf(pos);
		look(player, center.x, center.y, center.z);
		Direction face = Direction.getNearest(
			player.getX() - center.x,
			player.getEyeY() - center.y,
			player.getZ() - center.z
		);
		if (!minecraft.gameMode.isDestroying()) {
			minecraft.gameMode.startDestroyBlock(pos, face);
		} else {
			minecraft.gameMode.continueDestroyBlock(pos, face);
		}
		player.swing(InteractionHand.MAIN_HAND);
	}

	private static void selectHotbar(LocalPlayer player, int slot) {
		if (slot < 0 || slot >= 9) {
			return;
		}
		// The client game mode notices a hotbar change on its own tick and sends
		// the carried-item packet. Setting the slot here is the same path as the number keys.
		Inventory inventory = player.getInventory();
		if (inventory.selected != slot) {
			inventory.selected = slot;
		}
	}

	private static void say(Minecraft minecraft, LocalPlayer player, String message) {
		if (message == null || message.isBlank() || minecraft.gui == null) {
			return;
		}
		long now = player.level().getGameTime();
		if (message.equals(lastSay) && now - lastSayTime < 40) {
			return;
		}
		lastSay = message;
		lastSayTime = now;
		minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("[" + SystemOneMc.DISPLAY_NAME + "] " + message));
	}
}
