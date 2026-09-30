package com.zolanerd.jev.client.goal;

import com.zolanerd.jev.client.action.Action;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Current keyword goal. Touched only on the client thread.
 */
public final class GoalManager {
	private static final GoalManager INSTANCE = new GoalManager();

	private GoalType goal = GoalType.NONE;
	private String targetName = "";
	private boolean aiEnabled;
	private final List<BlockPos> buildQueue = new ArrayList<>();
	private int buildIndex;
	private boolean buildPlanned;
	private int placeMisses;
	private String noticeKey = "";

	private GoalManager() {
	}

	public static GoalManager get() {
		return INSTANCE;
	}

	public GoalType goal() {
		return goal;
	}

	public String targetName() {
		return targetName;
	}

	public boolean aiEnabled() {
		return aiEnabled;
	}

	public void setAiEnabled(boolean aiEnabled) {
		this.aiEnabled = aiEnabled;
	}

	public void toggleAi() {
		aiEnabled = !aiEnabled;
	}

	public void setGoal(GoalType next, String target) {
		goal = next == null ? GoalType.NONE : next;
		targetName = target == null ? "" : target.trim();
		noticeKey = "";
		placeMisses = 0;
		if (goal != GoalType.BUILD) {
			buildQueue.clear();
			buildIndex = 0;
			buildPlanned = false;
		} else {
			buildQueue.clear();
			buildIndex = 0;
			buildPlanned = false;
		}
	}

	public String label() {
		return switch (goal) {
			case NONE -> "none";
			case PROTECT -> targetName.isBlank() ? "protect" : "protect " + targetName;
			case FOLLOW -> targetName.isBlank() ? "follow" : "follow " + targetName;
			case BUILD -> "build";
			case IDLE -> "idle";
			case SURVIVE -> "survive";
		};
	}

	public void setBuildQueue(List<BlockPos> blocks) {
		buildQueue.clear();
		buildQueue.addAll(blocks);
		buildIndex = 0;
		buildPlanned = true;
		placeMisses = 0;
	}

	public boolean buildPlanned() {
		return buildPlanned;
	}

	public boolean hasBuildWork() {
		return buildIndex < buildQueue.size();
	}

	public BlockPos currentBuildBlock() {
		if (!hasBuildWork()) {
			return null;
		}
		return buildQueue.get(buildIndex);
	}

	public void advanceBuild() {
		if (buildIndex < buildQueue.size()) {
			buildIndex++;
		}
		placeMisses = 0;
	}

	public int buildRemaining() {
		return Math.max(0, buildQueue.size() - buildIndex);
	}

	public int buildTotal() {
		return buildQueue.size();
	}

	public void bumpPlaceMiss() {
		placeMisses++;
	}

	public int placeMisses() {
		return placeMisses;
	}

	public void resetPlaceMiss() {
		placeMisses = 0;
	}

	public List<BlockPos> buildQueueView() {
		return Collections.unmodifiableList(buildQueue);
	}

	/**
	 * Say a status once per key until the goal changes.
	 */
	public Action notice(String key, String message) {
		if (key.equals(noticeKey)) {
			return Action.stop();
		}
		noticeKey = key;
		return Action.say(message);
	}
}
