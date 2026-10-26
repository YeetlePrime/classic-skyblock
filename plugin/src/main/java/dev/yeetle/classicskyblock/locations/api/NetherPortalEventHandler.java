package dev.yeetle.classicskyblock.locations.api;

import dev.yeetle.classicskyblock.locations.domain.TeleportationService;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NetherPortalEventHandler implements Listener {
	private final TeleportationService teleportationService;

	public NetherPortalEventHandler(TeleportationService teleportationService) {
		this.teleportationService = teleportationService;
	}

	@EventHandler
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
			return;
		}

		World source = event.getFrom().getWorld();

		// Default portal behavior for default worlds
		if (!teleportationService.isIslandWorld(source)) {
			return;
		}

		Location destination = teleportationService.findPortalDestination(source, event.getTo());

		if (destination == null) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("Die zugehörige Welt konnte nicht geladen werden.");
			return;
		}

		event.setTo(destination);
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		if (!event.getPortalType().equals(PortalType.NETHER) || event.getTo() == null) {
			return;
		}

		World source = event.getFrom().getWorld();

		// Default portal behavior for default worlds
		if (!teleportationService.isIslandWorld(source)) {
			return;
		}

		Location destination = teleportationService.findPortalDestination(source, event.getTo());

		if (destination == null) {
			event.setCancelled(true);
			return;
		}

		event.setTo(destination);
	}
}
