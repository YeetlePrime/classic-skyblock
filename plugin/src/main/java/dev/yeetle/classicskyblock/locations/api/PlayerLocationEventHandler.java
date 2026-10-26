package dev.yeetle.classicskyblock.locations.api;

import dev.yeetle.classicskyblock.locations.domain.TeleportationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerLocationEventHandler implements Listener {
	private final TeleportationService teleportationService;

	public PlayerLocationEventHandler(TeleportationService teleportationService) {
		this.teleportationService = teleportationService;
	}

	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		teleportationService.rememberLastWorld(event.getPlayer());
	}

	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		teleportationService.teleportToLastWorld(event.getPlayer());
	}
}
