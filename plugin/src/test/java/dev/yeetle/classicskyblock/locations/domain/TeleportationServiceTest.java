package dev.yeetle.classicskyblock.locations.domain;

import dev.yeetle.classicskyblock.common.persistence.SaveDataFileManager;
import dev.yeetle.classicskyblock.islandmanagement.domain.IslandService;
import dev.yeetle.classicskyblock.islandmanagement.persistence.IslandRepository;
import dev.yeetle.classicskyblock.player.domain.PlayerService;
import dev.yeetle.classicskyblock.player.persistence.PlayerRepository;
import org.bukkit.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.block.BlockMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.Coordinate;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TeleportationServiceTest {
	private static final NamespacedKey ISLAND_KEY = new NamespacedKey("skyblock", "island_overworld_1");
	private static final NamespacedKey OTHER_KEY = new NamespacedKey("minecraft", "overworld");

	@TempDir
	Path folder;

	private ServerMock server;
	private EmptyWorld world;
	private PlayerMock player;
	private PlayerService playerService;
	private TeleportationService service;

	@BeforeEach
	void setUp() {
		server = MockBukkit.mock();

		world = new EmptyWorld(ISLAND_KEY);
		server.addWorld(world);
		world.setSpawnLocation(0, 10, 0);

		SaveDataFileManager fileManager = new SaveDataFileManager(folder);
		fileManager.load();
		PlayerRepository playerRepository = new PlayerRepository(fileManager);
		IslandRepository islandRepository = new IslandRepository(fileManager, playerRepository);
		Logger logger = Logger.getLogger("test");
		playerService = new PlayerService(playerRepository);
		service = new TeleportationService(logger, playerService, new IslandService(logger, islandRepository));

		player = server.addPlayer();
		player.teleport(new Location(world, 0, 10, 0));
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	private void place(int x, int y, int z, Material material) {
		world.getBlockAt(x, y, z).setType(material);
	}

	/**
	 * Places a block with a collision at the position, which is a safe floor if the blocks above are empty.
	 */
	private void floor(int x, int y, int z) {
		place(x, y, z, Material.STONE);
	}

	private Location standingOn(int x, int y, int z) {
		return new Location(world, x + 0.5, y + 1, z + 0.5);
	}

	private void assertSamePosition(Location expected, Location actual) {
		assertNotNull(actual);
		assertEquals(expected.getWorld(), actual.getWorld());
		assertEquals(expected.getX(), actual.getX(), 1e-9);
		assertEquals(expected.getY(), actual.getY(), 1e-9);
		assertEquals(expected.getZ(), actual.getZ(), 1e-9);
	}

	@Test
	void nothingHappensOutsideOfIslandWorlds() {
		EmptyWorld otherWorld = new EmptyWorld(OTHER_KEY);
		server.addWorld(otherWorld);
		player.teleport(new Location(otherWorld, 0, 10, 0));

		assertNull(service.findRespawnLocation(player));
	}

	@Test
	void theWorldSpawnIsUsedIfThereIsNothingToStandOn() {
		assertSamePosition(world.getSpawnLocation(), service.findRespawnLocation(player));
	}

	@Test
	void theClosestSafeBlockIsUsed() {
		floor(5, 9, 0);
		floor(3, 9, 0);
		floor(-7, 9, 4);

		assertSamePosition(standingOn(3, 9, 0), service.findRespawnLocation(player));
	}

	@Test
	void theVerticalDistanceIsPartOfTheDistance() {
		// The column of the first block is closer, but the second block is closer in total.
		floor(1, 0, 0);
		floor(8, 9, 0);

		assertSamePosition(standingOn(8, 9, 0), service.findRespawnLocation(player));
	}

	@Test
	void torchesAboveTheBlockDoNotPreventTheSpawn() {
		floor(2, 9, 0);
		place(2, 10, 0, Material.TORCH);

		assertSamePosition(standingOn(2, 9, 0), service.findRespawnLocation(player));
	}

	@Test
	void aBottomSlabIsAValidFloorAndThePlayerStandsOnTopOfIt() {
		place(2, 9, 0, Material.STONE_SLAB);

		assertSamePosition(new Location(world, 2.5, 9.5, 0.5), service.findRespawnLocation(player));
	}

	@Test
	void twoFreeBlocksAreRequiredAboveTheFloor() {
		floor(2, 9, 0);
		// A fence is no floor itself, so only the head of the player is affected.
		place(2, 11, 0, Material.OAK_FENCE);

		assertSamePosition(world.getSpawnLocation(), service.findRespawnLocation(player));
	}

	@Test
	void aFloorThatIsHigherThanABlockIsNotUsed() {
		place(2, 9, 0, Material.OAK_FENCE);

		assertSamePosition(world.getSpawnLocation(), service.findRespawnLocation(player));
	}

	@Test
	void aSafeBlockIsPreferredOverACloserHazardousBlock() {
		place(1, 9, 0, Material.MAGMA_BLOCK);
		place(3, 9, 0, Material.CACTUS);
		floor(8, 9, 0);

		assertSamePosition(standingOn(8, 9, 0), service.findRespawnLocation(player));
	}

	@Test
	void aBlockWithAHazardInTheBodyIsPreferredOverNothing() {
		floor(2, 9, 0);
		place(2, 10, 0, Material.FIRE);

		assertSamePosition(standingOn(2, 9, 0), service.findRespawnLocation(player));
	}

	@Test
	void theClosestHazardousBlockIsUsedIfThereIsNoSafeBlock() {
		place(6, 9, 0, Material.MAGMA_BLOCK);
		place(2, 9, 0, Material.CACTUS);
		floor(-4, 9, 0);
		place(-4, 10, 0, Material.LAVA);

		assertSamePosition(standingOn(2, 9, 0), service.findRespawnLocation(player));
	}

	@Test
	void everyKindOfHazardMakesALocationUnsafe() {
		List<Material> hazardsInBody = List.of(
			Material.LAVA,
			Material.WATER,
			Material.FIRE,
			Material.SOUL_FIRE,
			Material.COBWEB,
			Material.SWEET_BERRY_BUSH,
			Material.WITHER_ROSE,
			Material.POWDER_SNOW
		);
		floor(9, 9, 0);

		for (Material hazard : hazardsInBody) {
			place(1, 9, 0, Material.STONE);
			place(1, 10, 0, hazard);

			assertSamePosition(standingOn(9, 9, 0), service.findRespawnLocation(player), "Hazard " + hazard + " was accepted in the body");

			place(1, 10, 0, Material.AIR);
		}
		for (Material hazard : List.of(Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE)) {
			place(1, 9, 0, hazard);

			assertSamePosition(standingOn(9, 9, 0), service.findRespawnLocation(player), "Hazard " + hazard + " was accepted as floor");
		}
	}

	private void assertSamePosition(Location expected, Location actual, String message) {
		assertNotNull(actual, message);
		assertEquals(expected.getX(), actual.getX(), 1e-9, message);
		assertEquals(expected.getY(), actual.getY(), 1e-9, message);
		assertEquals(expected.getZ(), actual.getZ(), 1e-9, message);
	}

	@Test
	void blocksUpToTheRespawnRadiusAreFound() {
		world.setGameRule(GameRules.RESPAWN_RADIUS, 4);
		floor(4, 9, 4);

		assertSamePosition(standingOn(4, 9, 4), service.findRespawnLocation(player));
	}

	@Test
	void blocksBeyondTheRespawnRadiusAreIgnored() {
		world.setGameRule(GameRules.RESPAWN_RADIUS, 4);
		floor(5, 9, 0);
		floor(0, 9, -5);

		assertSamePosition(world.getSpawnLocation(), service.findRespawnLocation(player));
	}

	@Test
	void aRespawnRadiusOfZeroOnlyChecksTheColumnOfTheSpawn() {
		world.setGameRule(GameRules.RESPAWN_RADIUS, 0);
		floor(1, 9, 0);
		floor(0, 4, 0);

		assertSamePosition(standingOn(0, 4, 0), service.findRespawnLocation(player));
	}

	@Test
	void theWholeHeightOfTheWorldIsSearched() {
		world.setGameRule(GameRules.RESPAWN_RADIUS, 0);
		int highestFloor = world.getMaxHeight() - 3;

		floor(0, world.getMinHeight(), 0);
		assertSamePosition(standingOn(0, world.getMinHeight(), 0), service.findRespawnLocation(player));

		place(0, world.getMinHeight(), 0, Material.AIR);
		floor(0, highestFloor, 0);
		assertSamePosition(standingOn(0, highestFloor, 0), service.findRespawnLocation(player));

		// A block directly below the build limit has no room for the head.
		place(0, highestFloor, 0, Material.AIR);
		floor(0, highestFloor + 1, 0);
		assertSamePosition(world.getSpawnLocation(), service.findRespawnLocation(player));
	}

	@Test
	void chunksThatAreNotGeneratedAreSkipped() {
		world.markNotGenerated(0, 0);
		floor(3, 9, 0);
		floor(-6, 9, 0);

		assertSamePosition(standingOn(-6, 9, 0), service.findRespawnLocation(player));
	}

	@Test
	void aStoredBedHasPriorityOverTheSearch() {
		floor(2, 9, 0);
		place(-5, 10, 5, Material.RED_BED);
		Location bedSpawn = new Location(world, -5.5, 10, 5.5);
		service.saveSpawnPoint(player, bedSpawn);

		assertSamePosition(bedSpawn, service.findRespawnLocation(player));
	}

	@Test
	void aStoredSpawnPointWithoutBedIsRemovedAndTheSearchIsUsed() {
		floor(2, 9, 0);
		service.saveSpawnPoint(player, new Location(world, -5.5, 10, 5.5));

		assertSamePosition(standingOn(2, 9, 0), service.findRespawnLocation(player));
		assertNull(playerService.getSpawnPoint(player, world));
	}

	/**
	 * A world that is empty.
	 * MockBukkit does not implement the collision of blocks and which chunks are generated, so this is added here.
	 * The collision is a simplification of the real one: slabs are half, fences one and a half and all other blocks that are collidable one block high.
	 */
	private static class EmptyWorld extends WorldMock {
		private final Map<Coordinate, BlockMock> blocks = new HashMap<>();
		private final Set<Long> notGeneratedChunks = new HashSet<>();

		EmptyWorld(NamespacedKey key) {
			super(new WorldCreator(key));
		}

		void markNotGenerated(int chunkX, int chunkZ) {
			notGeneratedChunks.add(chunkKey(chunkX, chunkZ));
		}

		private static long chunkKey(int chunkX, int chunkZ) {
			return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
		}

		@Override
		public boolean isChunkGenerated(int x, int z) {
			return !notGeneratedChunks.contains(chunkKey(x, z));
		}

		@Override
		public BlockMock getBlockAt(Coordinate coordinate) {
			return blocks.computeIfAbsent(coordinate, c -> new CollidingBlock(new Location(this, c.x, c.y, c.z)));
		}
	}

	private static class CollidingBlock extends BlockMock {
		CollidingBlock(Location location) {
			super(Material.AIR, location);
		}

		@Override
		public boolean isPassable() {
			return !getType().isCollidable();
		}

		@Override
		public VoxelShape getCollisionShape() {
			Material type = getType();
			double height = type.name().endsWith("_SLAB") ? 0.5 : type.name().endsWith("_FENCE") ? 1.5 : 1;
			Collection<BoundingBox> boxes = type.isCollidable() ? List.of(new BoundingBox(0, 0, 0, 1, height, 1)) : List.of();

			return new VoxelShape() {
				@Override
				public Collection<BoundingBox> getBoundingBoxes() {
					return boxes;
				}

				@Override
				public boolean overlaps(BoundingBox aabb) {
					return boxes.stream().anyMatch(box -> box.overlaps(aabb));
				}
			};
		}
	}
}
