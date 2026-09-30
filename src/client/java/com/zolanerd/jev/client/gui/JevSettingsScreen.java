package com.zolanerd.jev.client.gui;

import com.zolanerd.jev.Jev;
import com.zolanerd.jev.client.config.JevConfig;
import com.zolanerd.jev.client.net.SidecarClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class JevSettingsScreen extends Screen {
	private final Screen parent;
	private EditBox address;
	private Button testButton;
	private String status = "Sidecar address. Empty falls back to the built-in dummy.";

	public JevSettingsScreen(Screen parent) {
		super(Component.literal("Jev settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		int y = this.height / 2 - 24;
		this.address = new EditBox(this.font, center - 150, y, 300, 20, Component.literal("Sidecar address"));
		this.address.setMaxLength(256);
		this.address.setValue(JevConfig.sidecarUrl());
		this.address.setHint(Component.literal(Jev.DEFAULT_SIDECAR_URL));
		this.addRenderableWidget(this.address);

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
			.bounds(center - 150, y + 28, 96, 20)
			.build());
		this.testButton = Button.builder(Component.literal("Test connection"), button -> test())
			.bounds(center - 48, y + 28, 120, 20)
			.build();
		this.addRenderableWidget(this.testButton);
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> close())
			.bounds(center + 78, y + 28, 72, 20)
			.build());
		this.setInitialFocus(this.address);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 52, 0xFFFFFF);
		graphics.drawCenteredString(this.font, this.status, this.width / 2, this.height / 2 + 36, 0xFFD27F);
	}

	private void save() {
		JevConfig.setSidecarUrl(this.address.getValue());
		JevConfig.save();
		if (JevConfig.sidecarUrl().isBlank()) {
			SidecarClient.markOffline("empty address");
			this.status = "Saved an empty address. Using the built-in dummy.";
		} else {
			this.status = "Saved " + JevConfig.sidecarUrl();
		}
	}

	private void test() {
		String url = this.address.getValue();
		JevConfig.setSidecarUrl(url);
		JevConfig.save();
		this.testButton.active = false;
		this.status = "Testing...";
		SidecarClient.test(url).whenComplete((result, error) -> {
			if (this.minecraft == null) {
				return;
			}
			this.minecraft.execute(() -> {
				this.testButton.active = true;
				if (error != null) {
					this.status = "Failed: " + error.getClass().getSimpleName();
					return;
				}
				this.status = result.message();
			});
		});
	}

	private void close() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}
}
