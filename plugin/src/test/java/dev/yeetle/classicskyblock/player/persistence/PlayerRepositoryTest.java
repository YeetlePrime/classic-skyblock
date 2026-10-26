package dev.yeetle.classicskyblock.player.persistence;

import dev.yeetle.classicskyblock.common.persistence.SaveDataFileManager;
import dev.yeetle.classicskyblock.common.valueobjects.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRepositoryTest {
	private static final NamespacedKey OVERWORLD = new NamespacedKey("skyblock", "island_overworld_1");
	private static final NamespacedKey NETHER = new NamespacedKey("skyblock", "island_nether_1");

	@TempDir
	Path folder;

	private SaveDataFileManager fileManager;
	private PlayerRepository repository;
	private final UUID playerId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		fileManager = new SaveDataFileManager(folder);
		fileManager.load();
		repository = new PlayerRepository(fileManager);
	}

	private PlayerRepository reload() {
		SaveDataFileManager other = new SaveDataFileManager(folder);
		other.load();

		return new PlayerRepository(other);
	}

	@Test
	void anUnknownPlayerHasNoLastWorld() {
		assertNull(repository.findLastWorld(playerId));
	}

	@Test
	void theLastWorldIsSavedPersistently() {
		repository.saveLastWorld(playerId, "Steve", OVERWORLD);

		assertEquals(OVERWORLD, reload().findLastWorld(playerId));
	}

	@Test
	void savingTheLastWorldUpdatesTheName() {
		repository.saveLastWorld(playerId, "Steve", OVERWORLD);
		repository.saveLastWorld(playerId, "Alex", NETHER);

		assertEquals("Alex", repository.findName(playerId).orElseThrow());
		assertEquals(NETHER, repository.findLastWorld(playerId));
	}

	@Test
	void spawnPointsAreSavedPerWorld() {
		repository.saveSpawnPoint(playerId, "Steve", OVERWORLD, new SpawnPoint(1, 2, 3, 4f, 5f));

		Location spawn = reload().findSpawnPoint(playerId, OVERWORLD).toLocation(null);

		assertEquals(1, spawn.getX());
		assertEquals(2, spawn.getY());
		assertEquals(3, spawn.getZ());
		assertNull(repository.findSpawnPoint(playerId, NETHER));
	}

	@Test
	void aRemovedSpawnPointIsGone() {
		repository.saveSpawnPoint(playerId, "Steve", OVERWORLD, new SpawnPoint(1, 2, 3, 4f, 5f));
		repository.removeSpawnPoint(playerId, OVERWORLD);

		assertNull(reload().findSpawnPoint(playerId, OVERWORLD));
	}

	@Test
	void removingASpawnPointOfAnUnknownPlayerDoesNothing() {
		assertDoesNotThrow(() -> repository.removeSpawnPoint(playerId, OVERWORLD));
	}

	@Test
	void spawnPointsInWorldsAreRemovedFromAllPlayersAndNullKeysAreIgnored() {
		UUID otherId = UUID.randomUUID();
		NamespacedKey kept = new NamespacedKey("skyblock", "island_overworld_2");
		repository.saveSpawnPoint(playerId, "Steve", OVERWORLD, new SpawnPoint(1, 2, 3, 0f, 0f));
		repository.saveSpawnPoint(otherId, "Alex", NETHER, new SpawnPoint(1, 2, 3, 0f, 0f));
		repository.saveSpawnPoint(otherId, "Alex", kept, new SpawnPoint(1, 2, 3, 0f, 0f));

		repository.removeSpawnPointsInWorlds(Arrays.asList(OVERWORLD, NETHER, null));

		assertNull(repository.findSpawnPoint(playerId, OVERWORLD));
		assertNull(repository.findSpawnPoint(otherId, NETHER));
		assertNotNull(repository.findSpawnPoint(otherId, kept));
	}

	@Test
	void playersAreFoundByNameIgnoringTheCase() {
		repository.saveLastWorld(playerId, "Steve", OVERWORLD);

		assertEquals(Set.of(playerId), repository.findIdsByName("sTEVE"));
		assertTrue(repository.findIdsByName("Alex").isEmpty());
	}

	@Test
	void anUnknownPlayerHasNoName() {
		assertTrue(repository.findName(playerId).isEmpty());
	}
}
