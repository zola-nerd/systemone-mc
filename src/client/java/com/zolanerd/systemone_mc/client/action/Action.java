package com.zolanerd.systemone_mc.client.action;

import net.minecraft.core.BlockPos;

/**
 * One client control step. Positions are world coordinates. Movement actions
 * are applied with look direction and movement keys, never by teleporting.
 */
public final class Action {
	public enum Type {
		STOP,
		MOVE_TO,
		LOOK_AT,
		ATTACK,
		USE_ITEM,
		PLACE_BLOCK,
		BREAK_BLOCK,
		JUMP,
		SNEAK,
		HOTBAR_SELECT,
		SAY
	}

	private final Type type;
	private final double x;
	private final double y;
	private final double z;
	private final String target;
	private final int slot;
	private final String message;
	private final boolean enabled;
	private final boolean sprint;

	private Action(
		Type type,
		double x,
		double y,
		double z,
		String target,
		int slot,
		String message,
		boolean enabled,
		boolean sprint
	) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.z = z;
		this.target = target == null ? "" : target;
		this.slot = slot;
		this.message = message == null ? "" : message;
		this.enabled = enabled;
		this.sprint = sprint;
	}

	public static Action stop() {
		return new Action(Type.STOP, 0, 0, 0, "", -1, "", false, false);
	}

	public static Action moveTo(double x, double y, double z) {
		return moveTo(x, y, z, false);
	}

	public static Action moveTo(double x, double y, double z, boolean sprint) {
		return new Action(Type.MOVE_TO, x, y, z, "", -1, "", false, sprint);
	}

	public static Action lookAt(double x, double y, double z) {
		return new Action(Type.LOOK_AT, x, y, z, "", -1, "", false, false);
	}

	public static Action attack(String target) {
		return new Action(Type.ATTACK, 0, 0, 0, target, -1, "", false, false);
	}

	public static Action useItem(int slot) {
		return new Action(Type.USE_ITEM, 0, 0, 0, "", slot, "", false, false);
	}

	public static Action place(BlockPos pos) {
		return new Action(Type.PLACE_BLOCK, pos.getX(), pos.getY(), pos.getZ(), "", -1, "", false, false);
	}

	public static Action breakBlock(BlockPos pos) {
		return new Action(Type.BREAK_BLOCK, pos.getX(), pos.getY(), pos.getZ(), "", -1, "", false, false);
	}

	public static Action jump() {
		return new Action(Type.JUMP, 0, 0, 0, "", -1, "", false, false);
	}

	public static Action sneak(boolean enabled) {
		return new Action(Type.SNEAK, 0, 0, 0, "", -1, "", enabled, false);
	}

	public static Action hotbar(int slot) {
		return new Action(Type.HOTBAR_SELECT, 0, 0, 0, "", slot, "", false, false);
	}

	public static Action say(String message) {
		return new Action(Type.SAY, 0, 0, 0, "", -1, message, false, false);
	}

	public Type type() {
		return type;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double z() {
		return z;
	}

	public String target() {
		return target;
	}

	public int slot() {
		return slot;
	}

	public String message() {
		return message;
	}

	public boolean enabled() {
		return enabled;
	}

	public boolean sprint() {
		return sprint;
	}

	public BlockPos blockPos() {
		return BlockPos.containing(x, y, z);
	}
}
