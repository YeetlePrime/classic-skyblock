package dev.yeetle.classicskyblock.islandmanagement.persistence;

import dev.yeetle.classicskyblock.common.persistence.IslandData;
import dev.yeetle.classicskyblock.common.persistence.SaveDataFileManager;
import dev.yeetle.classicskyblock.player.persistence.PlayerRepository;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class IslandRepository {
	private final SaveDataFileManager fileManager;
	private final PlayerRepository playerRepository;

	public IslandRepository(SaveDataFileManager fileManager, PlayerRepository playerRepository) {
		this.fileManager = fileManager;
		this.playerRepository = playerRepository;
	}

	/**
	 * Saves the overworld of an island.
	 */
	public void saveOverworld(UUID islandId, UUID ownerId, NamespacedKey worldKey) {
		islands().computeIfAbsent(islandId, id -> new IslandData(ownerId)).setOverworldKey(worldKey);

		fileManager.save();
	}

	/**
	 * Saves the nether of an island that already exists.
	 */
	public void saveNether(UUID islandId, NamespacedKey worldKey) {
		IslandData islandData = islands().get(islandId);

		if (islandData == null) {
			throw new IllegalArgumentException("Es gibt keine Insel mit der Id " + islandId);
		}

		islandData.setNetherKey(worldKey);

		fileManager.save();
	}

	/**
	 * Finds the island with the given id.
	 *
	 * @return the island or null if there is no island with that id
	 */
	public IslandData findIsland(UUID islandId) {
		return islands().get(islandId);
	}

	public boolean ownsAnIsland(UUID ownerId) {
		return islands().values().stream()
			.anyMatch(island -> island.getOwner().equals(ownerId));
	}

	/**
	 * Finds the id of the island owned by the player.
	 *
	 * @return the island id or null if the player owns no island
	 */
	public UUID findIslandIdByOwner(UUID ownerId) {
		return islands().entrySet().stream()
			.filter(entry -> entry.getValue().getOwner().equals(ownerId))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Finds the id of the island with the given world.
	 *
	 * @return the island id or null if there is no island with that world
	 */
	public UUID findIslandIdByWorld(NamespacedKey worldKey) {
		return islands().entrySet().stream()
			.filter(entry -> worldKey.equals(entry.getValue().getOverworldKey()) || worldKey.equals(entry.getValue().getNetherKey()))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Finds the island owned by the player with the given name.
	 *
	 * @return the island or null if no player of that name owns an island
	 */
	public IslandData findIslandByOwnerName(String ownerName) {
		Set<UUID> ownerIds = playerRepository.findIdsByName(ownerName);

		return islands().values().stream()
			.filter(island -> ownerIds.contains(island.getOwner()))
			.findFirst()
			.orElse(null);
	}

	public List<String> findIslandOwnerNames() {
		return islands().values().stream()
			.map(island -> playerRepository.findName(island.getOwner()))
			.flatMap(Optional::stream)
			.toList();
	}

	/**
	 * Removes the island and the spawn points of all players in its worlds.
	 */
	public void removeIsland(UUID islandId) {
		IslandData island = islands().remove(islandId);

		if (island != null) {
			playerRepository.removeSpawnPointsInWorlds(Arrays.asList(island.getOverworldKey(), island.getNetherKey()));
		}

		fileManager.save();
	}

	private Map<UUID, IslandData> islands() {
		return fileManager.getData().getIslands();
	}
}
