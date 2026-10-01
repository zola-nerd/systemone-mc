package com.zolanerd.systemone_mc.client.gui;

import com.zolanerd.systemone_mc.SystemOneMc;
import com.zolanerd.systemone_mc.client.chat.SystemOneMcChat;
import com.zolanerd.systemone_mc.client.goal.GoalManager;
import com.zolanerd.systemone_mc.client.goal.GoalType;
import com.zolanerd.systemone_mc.client.world.WorldQuery;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Goal wheel. Protect, follow, and fight open a nearby-player list and run on click.
 * Build, idle, survive, and dig run on click with no confirm step.
 */
public final class RadialMenuScreen extends Screen {
	private enum Mode {
		WHEEL,
		PLAYERS
	}

	private Mode mode = Mode.WHEEL;
	private GoalType pending = GoalType.PROTECT;
	private boolean needsRebuild;
	private boolean noPlayers;

	public RadialMenuScreen() {
		super(Component.literal(SystemOneMc.DISPLAY_NAME + " goals"));
	}

	@Override
	protected void init() {
		rebuild();
	}

	@Override
	public void tick() {
		if (needsRebuild) {
			needsRebuild = false;
			rebuild();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x88000000);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 128, 0xFFFFFF);
		if (mode == Mode.PLAYERS) {
			String heading = switch (pending) {
				case FOLLOW -> "Follow a player";
				case FIGHT -> "Fight alongside";
				case PROTECT, BUILD, IDLE, SURVIVE, DIG, NONE -> "Protect a player";
			};
			graphics.drawCenteredString(this.font, heading, this.width / 2, this.height / 2 - 96, 0xFFD27F);
			if (noPlayers) {
				graphics.drawCenteredString(this.font, "No nearby players", this.width / 2, this.height / 2 - 8, 0xFFAAAA);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void rebuild() {
		this.clearWidgets();
		if (mode == Mode.WHEEL) {
			buildWheel();
		} else {
			buildPlayers();
		}
	}

	private void buildWheel() {
		GoalType[] types = {
			GoalType.PROTECT, GoalType.FOLLOW, GoalType.BUILD, GoalType.IDLE,
			GoalType.SURVIVE, GoalType.FIGHT, GoalType.DIG
		};
		String[] labels = {"Protect", "Follow", "Build", "Idle", "Survive", "Fight", "Dig"};
		int radius = 100;
		for (int i = 0; i < types.length; i++) {
			double angle = -Math.PI / 2.0 + i * (Math.PI * 2.0 / types.length);
			int x = this.width / 2 + (int) Math.round(Math.cos(angle) * radius) - 40;
			int y = this.height / 2 + (int) Math.round(Math.sin(angle) * radius) - 10;
			GoalType type = types[i];
			this.addRenderableWidget(Button.builder(Component.literal(labels[i]), button -> choose(type))
				.bounds(x, y, 80, 20)
				.build());
		}
		this.addRenderableWidget(Button.builder(Component.literal("Settings"), button -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new SystemOneMcSettingsScreen(this));
			}
		}).bounds(this.width / 2 - 40, this.height / 2 - 10, 80, 20).build());
	}

	private void buildPlayers() {
		LocalPlayer self = this.minecraft == null ? null : this.minecraft.player;
		List<Player> players = self == null ? List.of() : WorldQuery.nearbyPlayers(self, WorldQuery.NEARBY_RANGE);
		noPlayers = players.isEmpty();
		int shown = Math.min(8, players.size());
		int startY = this.height / 2 - shown * 12 - 10;
		for (int i = 0; i < shown; i++) {
			Player other = players.get(i);
			int distance = self == null ? 0 : (int) other.distanceTo(self);
			String label = other.getScoreboardName() + " (" + distance + "m)";
			this.addRenderableWidget(Button.builder(Component.literal(label), button -> pick(other.getScoreboardName()))
				.bounds(this.width / 2 - 90, startY + i * 24, 180, 20)
				.build());
		}
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
			mode = Mode.WHEEL;
			needsRebuild = true;
		}).bounds(this.width / 2 - 40, this.height / 2 + 92, 80, 20).build());
	}

	private void choose(GoalType type) {
		if (type == GoalType.PROTECT || type == GoalType.FOLLOW || type == GoalType.FIGHT) {
			pending = type;
			mode = Mode.PLAYERS;
			needsRebuild = true;
			return;
		}
		GoalManager.get().setGoal(type, "");
		SystemOneMcChat.feedback(SystemOneMcChat.withAi(switch (type) {
			case BUILD -> "Building a 5x5x3 dirt hut.";
			case IDLE -> "Idling.";
			case SURVIVE -> "Survive mode.";
			case DIG -> "Digging a forward 1x2 tunnel.";
			case PROTECT, FOLLOW, FIGHT, NONE -> "Goal set.";
		}));
		this.onClose();
	}

	private void pick(String name) {
		GoalManager.get().setGoal(pending, name);
		String line = switch (pending) {
			case FOLLOW -> "Following " + name + ".";
			case FIGHT -> "Fighting alongside " + name + ".";
			case PROTECT -> "Protecting " + name + ".";
			case BUILD, IDLE, SURVIVE, DIG, NONE -> "Goal set.";
		};
		SystemOneMcChat.feedback(SystemOneMcChat.withAi(line));
		this.onClose();
	}
}
