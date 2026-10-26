package dev.yeetle.classicskyblock.locations.domain;

import dev.yeetle.classicskyblock.islandmanagement.domain.IslandService;
import dev.yeetle.classicskyblock.player.domain.PlayerService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

public class TeleportationService {
	private final Logger log;
	private final PlayerService playerService;
	private final IslandService islandService;

	public TeleportationService(Logger logger, PlayerService playerService, IslandService islandService) {
		this.log = logger;
		this.playerService = playerService;
		this.islandService = islandService;
	}

	public boolean isIslandWorld(World world) {
		return islandService.isIslandWorld(world);
	}

	/**
	 * Sends a joined player to the island world the player was in the last time.
	 * Nothing happens if there is no such world.
	 */
	public void teleportToLastWorld(Player player) {
		NamespacedKey lastWorldKey = playerService.getLastWorld(player);

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

		Location targetLocation = player.getLocation().clone();
		targetLocation.setWorld(world);
		player.teleport(targetLocation);

		log.info("Player %s joined world %s.".formatted(player.getName(), lastWorldKey));
	}

	public void rememberLastWorld(Player player) {
		log.info("Player \"%s\" changed world to %s.".formatted(player.getName(), player.getWorld().getName()));

		playerService.saveLastWorld(player);
	}

	/**
	 * Sends the player to the island of the owner with the given name and tells the player about the result.
	 */
	public void joinIsland(Player player, String ownerName) {
		NamespacedKey overworldKey = islandService.findOverworldKey(ownerName);

		if (overworldKey == null) {
			player.sendMessage("There is no island owned by %s.".formatted(ownerName));
			return;
		}

		World world = islandService.loadWorld(overworldKey);

		if (world == null) {
			player.sendMessage("The island of %s could not be loaded.".formatted(ownerName));
			return;
		}

		player.teleport(world.getSpawnLocation());
		player.sendMessage("You joined the island of %s.".formatted(ownerName));
	}

	public List<String> getJoinableIslandOwners() {
		return islandService.getIslandOwnerNames();
	}

	/**
	 * Finds the destination in the world that corresponds to the source of a portal.
	 *
	 * @param source  the island world the portal is in
	 * @param arrival the location where vanilla would send the traveller
	 * @return the destination or null if the corresponding world could not be loaded
	 */
	public Location findPortalDestination(World source, Location arrival) {
		World target = islandService.loadCorrespondingWorld(source);

		if (target == null) {
			log.warning("Could not load the corresponding world for %s.".formatted(source.getKey().toString()));
			return null;
		}

		Location destination = arrival.clone();
		destination.setWorld(target);

		return destination;
	}

	public void saveSpawnPoint(Player player, Location location) {
		playerService.saveSpawnPoint(player, location);
	}

	/**
	 * Finds the location where the player respawns after dying in an island world.
	 * The spawn point of the death world is preferred over the one of the overworld, the spawn of the overworld is the last resort.
	 *
	 * @return the location or null if the player did not die in an island world or no world could be loaded
	 */
	public Location findRespawnLocation(Player player) {
		World deathWorld = player.getWorld();

		if (!islandService.isIslandWorld(deathWorld)) {
			return null;
		}

		World overworld = deathWorld.getEnvironment() == World.Environment.NORMAL
			? deathWorld
			: islandService.loadCorrespondingWorld(deathWorld);

		for (World world : new World[]{deathWorld, overworld}) {
			Location spawn = world != null ? findValidSpawnPoint(player, world) : null;

			if (spawn != null) {
				return spawn;
			}
		}

		return overworld != null ? overworld.getSpawnLocation() : null;
	}

	/**
	 * Gets the stored spawn point of the player in the world.
	 * A spawn point whose bed or respawn anchor is gone is removed.
	 */
	private Location findValidSpawnPoint(Player player, World world) {
		Location spawn = playerService.getSpawnPoint(player, world);

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

		playerService.removeSpawnPoint(player, world);

		return null;
	}
}
