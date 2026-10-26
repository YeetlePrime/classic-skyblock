package dev.yeetle.classicskyblock.player.domain;

import dev.yeetle.classicskyblock.common.valueobjects.SpawnPoint;
import dev.yeetle.classicskyblock.player.persistence.PlayerRepository;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PlayerService {
	private final PlayerRepository playerRepository;

	public PlayerService(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
	}

	public void saveLastWorld(Player player) {
		playerRepository.saveLastWorld(player.getUniqueId(), player.getName(), player.getWorld().getKey());
	}

	/**
	 * Gets the world the player was in the last time.
	 *
	 * @return the key of the world or null if the player has no last world
	 */
	public NamespacedKey getLastWorld(Player player) {
		return playerRepository.findLastWorld(player.getUniqueId());
	}

	public void saveSpawnPoint(Player player, Location location) {
		playerRepository.saveSpawnPoint(
			player.getUniqueId(),
			player.getName(),
			location.getWorld().getKey(),
			new SpawnPoint(location)
		);
	}

	/**
	 * Gets the spawn point of the player in the given world.
	 *
	 * @return the spawn point or null if the player has none in that world
	 */
	public Location getSpawnPoint(Player player, World world) {
		SpawnPoint spawnPoint = playerRepository.findSpawnPoint(player.getUniqueId(), world.getKey());

		return spawnPoint != null ? spawnPoint.toLocation(world) : null;
	}

	public void removeSpawnPoint(Player player, World world) {
		playerRepository.removeSpawnPoint(player.getUniqueId(), world.getKey());
	}
}
