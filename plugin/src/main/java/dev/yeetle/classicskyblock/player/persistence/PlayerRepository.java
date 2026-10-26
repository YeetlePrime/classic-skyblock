package dev.yeetle.classicskyblock.player.persistence;

import dev.yeetle.classicskyblock.common.persistence.PlayerData;
import dev.yeetle.classicskyblock.common.persistence.SaveDataFileManager;
import dev.yeetle.classicskyblock.common.valueobjects.SpawnPoint;
import org.bukkit.NamespacedKey;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads and writes the data of players.
 */
public class PlayerRepository {
	private final SaveDataFileManager fileManager;

	public PlayerRepository(SaveDataFileManager fileManager) {
		this.fileManager = fileManager;
	}

	public void saveLastWorld(UUID playerId, String name, NamespacedKey worldKey) {
		PlayerData playerData = getOrCreate(playerId, name);

		playerData.setName(name);
		playerData.setLastWorld(worldKey);

		fileManager.save();
	}

	/**
	 * Finds the world the player was in the last time.
	 *
	 * @return the key of the world or null if the player has no last world
	 */
	public NamespacedKey findLastWorld(UUID playerId) {
		PlayerData playerData = players().get(playerId);

		return playerData != null ? playerData.getLastWorld() : null;
	}

	public void saveSpawnPoint(UUID playerId, String name, NamespacedKey worldKey, SpawnPoint spawnPoint) {
		getOrCreate(playerId, name).getSpawnPoints().put(worldKey, spawnPoint);

		fileManager.save();
	}

	/**
	 * Finds the spawn point of the player in the given world.
	 *
	 * @return the spawn point or null if the player has none in that world
	 */
	public SpawnPoint findSpawnPoint(UUID playerId, NamespacedKey worldKey) {
		PlayerData playerData = players().get(playerId);

		return playerData != null ? playerData.getSpawnPoints().get(worldKey) : null;
	}

	public void removeSpawnPoint(UUID playerId, NamespacedKey worldKey) {
		PlayerData playerData = players().get(playerId);

		if (playerData != null && playerData.getSpawnPoints().remove(worldKey) != null) {
			fileManager.save();
		}
	}

	/**
	 * Removes the spawn points in the given worlds from all players.
	 * Keys that are null are ignored.
	 */
	public void removeSpawnPointsInWorlds(Collection<NamespacedKey> worldKeys) {
		Set<NamespacedKey> keys = worldKeys.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		players().values().forEach(playerData -> playerData.getSpawnPoints().keySet().removeAll(keys));

		fileManager.save();
	}

	public Optional<String> findName(UUID playerId) {
		return Optional.ofNullable(players().get(playerId)).map(PlayerData::getName);
	}

	/**
	 * Finds the ids of all players with the given name, ignoring the case.
	 */
	public Set<UUID> findIdsByName(String name) {
		return players().entrySet().stream()
			.filter(entry -> entry.getValue().getName().equalsIgnoreCase(name))
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
	}

	private PlayerData getOrCreate(UUID playerId, String name) {
		return players().computeIfAbsent(playerId, id -> new PlayerData(name));
	}

	private Map<UUID, PlayerData> players() {
		return fileManager.getData().getPlayers();
	}
}
