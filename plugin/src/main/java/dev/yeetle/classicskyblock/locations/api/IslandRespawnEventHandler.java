package dev.yeetle.classicskyblock.locations.api;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import dev.yeetle.classicskyblock.locations.domain.TeleportationService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class IslandRespawnEventHandler implements Listener {
	private final TeleportationService teleportationService;

	public IslandRespawnEventHandler(TeleportationService teleportationService) {
		this.teleportationService = teleportationService;
	}

	@EventHandler
	public void onSetSpawn(PlayerSetSpawnEvent event) {
		Location location = event.getLocation();

		if (location == null || location.getWorld() == null || !teleportationService.isIslandWorld(location.getWorld())) {
			return;
		}
		if (event.getCause() != PlayerSetSpawnEvent.Cause.BED && event.getCause() != PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR) {
			return;
		}

		if (teleportationService.saveSpawnPoint(event.getPlayer(), location)) {
			event.getPlayer().sendMessage("Respawn point set");
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Location respawnLocation = teleportationService.findRespawnLocation(event.getPlayer());

		if (respawnLocation != null) {
			event.setRespawnLocation(respawnLocation);
		}
	}
}
