package dev.yeetle.classic_skyblock;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NetherPortalHandler implements Listener {
	private final IslandService islandService;

	public NetherPortalHandler(IslandService islandService) {
		this.islandService = islandService;
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
