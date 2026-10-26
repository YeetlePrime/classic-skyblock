package dev.yeetle.classicskyblock.islandmanagement.api;

import dev.yeetle.classicskyblock.islandmanagement.domain.IslandService;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

public class IslandCreationEventHandler implements Listener {
	private final Logger log;
	private final IslandService islandService;

	public IslandCreationEventHandler(Logger logger, IslandService islandService) {
		this.log = logger;
		this.islandService = islandService;
	}

	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (islandService.ownsAnIsland(player)) {
			return;
		}

		islandService.createIslandForPlayer(player);
	}

	/**
	 * Creates the nether before the portal listener sends the player there.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPortal(PlayerPortalEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
			return;
		}

		World source = event.getFrom().getWorld();

		if (!islandService.isIslandWorld(source) || source.getEnvironment() != World.Environment.NORMAL) {
			return;
		}

		islandService.createNetherIfMissing(source, event.getTo());
	}

	/**
	 * Creates the nether before the portal listener sends the entity there.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPortal(EntityPortalEvent event) {
		if (!event.getPortalType().equals(PortalType.NETHER) || event.getTo() == null) {
			return;
		}

		World source = event.getFrom().getWorld();

		if (!islandService.isIslandWorld(source) || source.getEnvironment() != World.Environment.NORMAL) {
			return;
		}

		islandService.createNetherIfMissing(source, event.getTo());
	}
}
