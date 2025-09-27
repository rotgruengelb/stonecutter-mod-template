package com.example.modtemplate.event;

import com.example.modtemplate.ModTemplate;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public class ExampleEventHandler {

	public static void onPlayerHurt(ServerPlayer player) {
		// MinecraftServer.pvp is private... only here to test ATs/AWs
		if (Objects.requireNonNull(player.getServer()).pvp) {
			ModTemplate.LOGGER.info("{} took damage. PVP is allowed.", player.getDisplayName());
		} else {
			ModTemplate.LOGGER.info("{} took damage. PVP is disallowed.", player.getDisplayName());
		}
	}
}
