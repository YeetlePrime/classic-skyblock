package dev.yeetle.classicskyblock.islandmanagement.persistence;

import dev.yeetle.classicskyblock.common.persistence.IslandData;
import dev.yeetle.classicskyblock.common.persistence.SaveDataFileManager;
import dev.yeetle.classicskyblock.common.valueobjects.SpawnPoint;
import dev.yeetle.classicskyblock.player.persistence.PlayerRepository;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IslandRepositoryTest {
	private static final NamespacedKey OVERWORLD = new NamespacedKey("skyblock", "island_overworld_1");
	private static final NamespacedKey NETHER = new NamespacedKey("skyblock", "island_nether_1");

	@TempDir
	Path folder;

	private PlayerRepository playerRepository;
	private IslandRepository repository;
	private final UUID ownerId = UUID.randomUUID();
	private final UUID islandId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		SaveDataFileManager fileManager = new SaveDataFileManager(folder);
		fileManager.load();
		playerRepository = new PlayerRepository(fileManager);
		repository = new IslandRepository(fileManager, playerRepository);
	}

	private IslandRepository reload() {
		SaveDataFileManager fileManager = new SaveDataFileManager(folder);
		fileManager.load();

		return new IslandRepository(fileManager, new PlayerRepository(fileManager));
	}

	@Test
	void anIslandIsCreatedWithItsOverworld() {
		repository.saveOverworld(islandId, ownerId, OVERWORLD);

		IslandData island = reload().findIsland(islandId);

		assertEquals(ownerId, island.getOwner());
		assertEquals(OVERWORLD, island.getOverworldKey());
		assertNull(island.getNetherKey());
	}

	@Test
	void theNetherIsAddedToAnExistingIsland() {
		repository.saveOverworld(islandId, ownerId, OVERWORLD);
		repository.saveNether(islandId, NETHER);

		assertEquals(NETHER, reload().findIsland(islandId).getNetherKey());
	}

	@Test
	void theNetherOfAnUnknownIslandCannotBeSaved() {
		assertThrows(IllegalArgumentException.class, () -> repository.saveNether(islandId, NETHER));
	}

	@Test
	void anUnknownIslandIsNotFound() {
		assertNull(repository.findIsland(islandId));
		assertNull(repository.findIsland(null));
	}

	@Test
	void ownersAndTheirIslandsAreFound() {
		repository.saveOverworld(islandId, ownerId, OVERWORLD);

		assertTrue(repository.ownsAnIsland(ownerId));
		assertFalse(repository.ownsAnIsland(UUID.randomUUID()));
		assertEquals(islandId, repository.findIslandIdByOwner(ownerId));
		assertNull(repository.findIslandIdByOwner(UUID.randomUUID()));
	}

	@Test
	void islandsAreFoundByBothOfTheirWorlds() {
		repository.saveOverworld(islandId, ownerId, OVERWORLD);
		repository.saveNether(islandId, NETHER);

		assertEquals(islandId, repository.findIslandIdByWorld(OVERWORLD));
		assertEquals(islandId, repository.findIslandIdByWorld(NETHER));
		assertNull(repository.findIslandIdByWorld(new NamespacedKey("skyblock", "island_overworld_2")));
	}

	@Test
	void islandsAreFoundByTheNameOfTheirOwner() {
		playerRepository.saveLastWorld(ownerId, "Steve", OVERWORLD);
		repository.saveOverworld(islandId, ownerId, OVERWORLD);

		assertEquals(OVERWORLD, repository.findIslandByOwnerName("steve").getOverworldKey());
		assertNull(repository.findIslandByOwnerName("Alex"));
	}

	@Test
	void ownerNamesOnlyContainOwnersThatAreKnownAsPlayers() {
		UUID unknownOwnerId = UUID.randomUUID();
		playerRepository.saveLastWorld(ownerId, "Steve", OVERWORLD);
		repository.saveOverworld(islandId, ownerId, OVERWORLD);
		repository.saveOverworld(UUID.randomUUID(), unknownOwnerId, new NamespacedKey("skyblock", "island_overworld_2"));

		assertEquals(List.of("Steve"), repository.findIslandOwnerNames());
	}

	@Test
	void removingAnIslandRemovesItAndTheSpawnPointsInItsWorlds() {
		playerRepository.saveSpawnPoint(ownerId, "Steve", OVERWORLD, new SpawnPoint(1, 2, 3, 0f, 0f));
		playerRepository.saveSpawnPoint(ownerId, "Steve", NETHER, new SpawnPoint(1, 2, 3, 0f, 0f));
		repository.saveOverworld(islandId, ownerId, OVERWORLD);
		repository.saveNether(islandId, NETHER);

		repository.removeIsland(islandId);

		assertNull(reload().findIsland(islandId));
		assertNull(playerRepository.findSpawnPoint(ownerId, OVERWORLD));
		assertNull(playerRepository.findSpawnPoint(ownerId, NETHER));
	}

	@Test
	void removingAnUnknownIslandChangesNothing() {
		repository.saveOverworld(islandId, ownerId, OVERWORLD);

		repository.removeIsland(UUID.randomUUID());

		assertNotNull(repository.findIsland(islandId));
	}
}
