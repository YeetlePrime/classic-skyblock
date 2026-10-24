package dev.yeetle.classic_skyblock;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

public class PlayerLocationHandler implements Listener {
	private final Logger log;
	private final SaveDataManager saveDataManager;
	private final IslandService islandService;

	public PlayerLocationHandler(Logger logger, SaveDataManager saveDataManager, IslandService islandService) {
		this.log = logger;
		this.saveDataManager = saveDataManager;
		this.islandService = islandService;
	}

	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		World world = player.getWorld();

		log.info("Player \"%s\" changed world to %s.".formatted(player.getName(), world.getName()));

		saveDataManager.saveLastWorld(player);
	}

	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		NamespacedKey lastWorldKey = saveDataManager.getLastWorld(player);
		
		if (lastWorldKey == null) {
			return;
		}
		if (!islandService.isIslandWorld(lastWorldKey)) {
			return;
		}
		
		World world = islandService.loadWorld(lastWorldKey);

		if (world == null) {
			log.warning("Player %s wants to join world %s, but this would could not be loaded.".formatted(player.getName(), lastWorldKey));
			return;
		} 

		Location targetLocation = event.getPlayer().getLocation().clone();
		targetLocation.setWorld(world);
		event.getPlayer().teleport(targetLocation);
		
		log.info("Player %s joined world %s.".formatted(player.getName(), lastWorldKey));
	}

	@EventHandler
	public void onPortal(PlayerPortalEvent event) {
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
			return;
		}

		Location destination = event.getTo().clone();
		destination.setWorld(target);

		event.setTo(destination);
	}
}
