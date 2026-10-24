package dev.yeetle.classic_skyblock;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

public class IslandCreationHandler implements Listener {
	private final Logger log;
	private final SaveDataManager saveDataManager;
	private final IslandService islandService;

	public IslandCreationHandler(Logger logger, SaveDataManager saveDataManager, IslandService islandService) {
		this.log = logger;
		this.saveDataManager = saveDataManager;
		this.islandService = islandService;
	}

	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (saveDataManager.ownsAnIsland(player)) {
			return;
		}

		islandService.createIslandForPlayer(player);
	}

	/**
	 * Creates the nether before the portal listener sends the player there.
	 * The destination is not changed here, that is up to the portal listener.
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
}
