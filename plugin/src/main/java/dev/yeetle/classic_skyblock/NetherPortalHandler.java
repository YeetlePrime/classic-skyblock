package dev.yeetle.classic_skyblock;

import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

public class NetherPortalHandler implements Listener {
	private final Logger log;
	private final IslandService islandService;

	public NetherPortalHandler(Logger logger, IslandService islandService) {
		this.log = logger;
		this.islandService = islandService;
	}

	@EventHandler
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
			return;
		}

		World source = event.getFrom().getWorld();

		// Default portal behavior for default worlds
		if (!islandService.isIslandWorld(source)) {
			return;
		}

		World target = islandService.loadCorrespondingWorld(source);

		if (target == null) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("Die zugehörige Welt konnte nicht geladen werden.");
			log.warning("Could not load the corresponding world for %s.".formatted(source.getKey().toString()));
			return;
		}

		Location destination = event.getTo().clone();
		destination.setWorld(target);

		event.setTo(destination);
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		if (!event.getPortalType().equals(PortalType.NETHER) || event.getTo() == null) {
			return;
		}
		
		World source = event.getFrom().getWorld();

		// Default portal behavior for default worlds
		if (!islandService.isIslandWorld(source)) {
			return;
		}

		World target = islandService.loadCorrespondingWorld(source);

		if (target == null) {
			event.setCancelled(true);
			log.warning("Could not load the corresponding world for %s.".formatted(source.getKey().toString()));
			return;
		}

		Location destination = event.getTo().clone();
		destination.setWorld(target);

		event.setTo(destination);
	}
}
