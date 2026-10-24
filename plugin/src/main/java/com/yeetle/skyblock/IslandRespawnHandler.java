package com.yeetle.skyblock;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class IslandRespawnHandler implements Listener {
	private final SaveDataManager saveDataManager;
	private final IslandService islandService;

	public IslandRespawnHandler(SaveDataManager saveDataManager, IslandService islandService) {
		this.saveDataManager = saveDataManager;
		this.islandService = islandService;
	}

	@EventHandler
	public void onSetSpawn(PlayerSetSpawnEvent event) {
		Location location = event.getLocation();

		if (location == null || location.getWorld() == null || !islandService.isIslandWorld(location.getWorld())) {
			return;
		}
		if (event.getCause() != PlayerSetSpawnEvent.Cause.BED && event.getCause() != PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR) {
			return;
		}

		saveDataManager.saveSpawnPoint(event.getPlayer(), location);
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		World deathWorld = player.getWorld();

		if (!islandService.isIslandWorld(deathWorld)) {
			return;
		}

		World overworld = deathWorld.getEnvironment() == World.Environment.NORMAL
			? deathWorld
			: islandService.loadCorrespondingWorld(deathWorld);

		for (World world : new World[]{deathWorld, overworld}) {
			Location spawn = world != null ? findValidSpawnPoint(player, world) : null;

			if (spawn != null) {
				event.setRespawnLocation(spawn);
				return;
			}
		}

		if (overworld != null) {
			event.setRespawnLocation(overworld.getSpawnLocation());
		}
	}

	/**
	 * Gets the stored spawn point of the player in the world.
	 * A spawn point whose bed or respawn anchor is gone is removed.
	 */
	private Location findValidSpawnPoint(Player player, World world) {
		Location spawn = saveDataManager.getSpawnPoint(player, world);

		if (spawn == null) {
			return null;
		}

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					Block block = spawn.getBlock().getRelative(dx, dy, dz);

					if (Tag.BEDS.isTagged(block.getType()) || block.getType() == Material.RESPAWN_ANCHOR) {
						return spawn;
					}
				}
			}
		}

		saveDataManager.removeSpawnPoint(player, world);

		return null;
	}
}
