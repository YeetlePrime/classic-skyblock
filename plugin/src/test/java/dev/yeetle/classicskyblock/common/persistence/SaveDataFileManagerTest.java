package dev.yeetle.classicskyblock.common.persistence;

import dev.yeetle.classicskyblock.common.valueobjects.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SaveDataFileManagerTest {
	@TempDir
	Path folder;

	@Test
	void namespacedKeysAreSavedAsText() throws IOException {
		SaveDataFileManager manager = new SaveDataFileManager(folder);
		manager.load();

		UUID playerId = UUID.randomUUID();
		PlayerData player = new PlayerData("Steve");
		player.setLastWorld(new NamespacedKey("skyblock", "island_overworld_1"));
		manager.getData().getPlayers().put(playerId, player);
		manager.save();

		String json = Files.readString(folder.resolve("savedata.json"));

		assertTrue(json.contains("\"skyblock:island_overworld_1\""), json);
	}

	@Test
	void allSavedDataIsRestored() {
		UUID playerId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID islandId = UUID.randomUUID();
		NamespacedKey overworld = new NamespacedKey("skyblock", "island_overworld_1");
		NamespacedKey nether = new NamespacedKey("skyblock", "island_nether_1");

		SaveDataFileManager manager = new SaveDataFileManager(folder);
		manager.load();

		PlayerData player = new PlayerData("Steve");
		player.setLastWorld(nether);
		player.getSpawnPoints().put(overworld, new SpawnPoint(1.5, 64, -2.5, 90f, 10f));
		manager.getData().getPlayers().put(playerId, player);

		IslandData island = new IslandData(ownerId);
		island.setOverworldKey(overworld);
		island.setNetherKey(nether);
		manager.getData().getIslands().put(islandId, island);
		manager.save();

		SaveDataFileManager other = new SaveDataFileManager(folder);
		other.load();

		PlayerData loadedPlayer = other.getData().getPlayers().get(playerId);
		Location spawn = loadedPlayer.getSpawnPoints().get(overworld).toLocation(null);
		IslandData loadedIsland = other.getData().getIslands().get(islandId);

		assertEquals("Steve", loadedPlayer.getName());
		assertEquals(nether, loadedPlayer.getLastWorld());
		assertEquals(1.5, spawn.getX());
		assertEquals(64, spawn.getY());
		assertEquals(-2.5, spawn.getZ());
		assertEquals(90f, spawn.getYaw());
		assertEquals(10f, spawn.getPitch());
		assertEquals(ownerId, loadedIsland.getOwner());
		assertEquals(overworld, loadedIsland.getOverworldKey());
		assertEquals(nether, loadedIsland.getNetherKey());
	}

	@Test
	void playersWithoutSpawnPointsInTheFileGetAnEmptyMap() throws IOException {
		UUID playerId = UUID.randomUUID();
		Files.writeString(folder.resolve("savedata.json"), """
			{"players": {"%s": {"name": "Alex"}}, "islands": {}}
			""".formatted(playerId));

		SaveDataFileManager manager = new SaveDataFileManager(folder);
		manager.load();

		assertTrue(manager.getData().getPlayers().get(playerId).getSpawnPoints().isEmpty());
	}

	@Test
	void anInvalidNamespacedKeyIsACorruptFile() throws IOException {
		Files.writeString(folder.resolve("savedata.json"), """
			{"players": {"%s": {"name": "Alex", "lastWorld": "Not A Key"}}}
			""".formatted(UUID.randomUUID()));

		assertThrows(IllegalStateException.class, () -> new SaveDataFileManager(folder).load());
	}
}
