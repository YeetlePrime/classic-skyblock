package dev.yeetle.classicskyblock.locations.domain;

import dev.yeetle.classicskyblock.islandmanagement.domain.IslandService;
import dev.yeetle.classicskyblock.player.domain.PlayerService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class TeleportationService {
	private static final Set<Material> HAZARDOUS_MATERIALS = Set.of(
		Material.LAVA,
		Material.WATER,
		Material.BUBBLE_COLUMN,
		Material.MAGMA_BLOCK,
		Material.CACTUS,
		Material.SWEET_BERRY_BUSH,
		Material.WITHER_ROSE,
		Material.POWDER_SNOW,
		Material.COBWEB,
		Material.POINTED_DRIPSTONE,
		Material.NETHER_PORTAL,
		Material.END_PORTAL,
		Material.END_GATEWAY
	);

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

	public boolean saveSpawnPoint(Player player, Location location) {
		Location previousLocation = playerService.getSpawnPoint(player, location.getWorld());
		playerService.saveSpawnPoint(player, location);

		return previousLocation == null || previousLocation.getX() != location.getX() || previousLocation.getY() != location.getY() || previousLocation.getZ() != location.getZ();
	}

	/**
	 * Finds the location where the player respawns after dying in an island world.
	 * The spawn point of the death world is preferred over the one of the overworld.
	 * Without a valid spawn point, the closest safe location near the spawn of the overworld is used.
	 * The spawn of the overworld itself is the last resort.
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
			Location spawn = world != null ? findValidSpawnPointNearBedOrAnchor(player, world) : null;

			if (spawn != null) {
				return spawn;
			}
		}

		if (overworld == null) {
			return null;
		}

		Location worldSpawn = overworld.getSpawnLocation();
		Location safeSpawn = findValidSpawnPointNearSpawn(worldSpawn);

		return safeSpawn != null ? safeSpawn : worldSpawn;
	}

	/**
	 * Gets the stored spawn point of the player in the world.
	 * A spawn point whose bed or respawn anchor is gone is removed.
	 */
	private Location findValidSpawnPointNearBedOrAnchor(Player player, World world) {
		Location spawn = playerService.getSpawnPoint(player, world);

		if (spawn == null) {
			return null;
		}

		Block block = spawn.getBlock();
		
		
		if (Tag.BEDS.isTagged(block.getType())) {
			Location respawnLocation = spawn.clone();
			respawnLocation.setX(spawn.getX() + 0.5);
			respawnLocation.setY(spawn.getY() + 1);
			respawnLocation.setZ(spawn.getZ() + 0.5);

			return respawnLocation;
		} else if (block.getBlockData() instanceof RespawnAnchor respawnAnchor && respawnAnchor.getCharges() > 0) {
			Location respawnLocation = spawn.clone();
			respawnLocation.setX(spawn.getX() + 0.5);
			respawnLocation.setY(spawn.getY() + 1);
			respawnLocation.setZ(spawn.getZ() + 0.5);
			respawnAnchor.setCharges(respawnAnchor.getCharges() - 1);
			block.setBlockData(respawnAnchor);

			return respawnLocation;
			
		} else {
			playerService.removeSpawnPoint(player, world);
			return null;
		}
	}

	/**
	 * Gets a valid and safe spawn location near the spawn point.
	 * The search covers the full height of the world and a square around the spawn point with the half edge length of the respawn radius game rule.
	 * A location is safe if it is on a non-hazardous block with a collision and has two non-hazardous blocks without collision above it.
	 * The closest safe location to the spawn point wins.
	 * If there is no safe location, the closest hazardous location is used, which only differs from a safe one by hazardous blocks.
	 *
	 * @return a valid spawn location near the spawn point. null if there is no such spawn location
	 */
	private Location findValidSpawnPointNearSpawn(Location spawnPoint) {
		if (spawnPoint == null || spawnPoint.getWorld() == null) {
			return null;
		}

		World world = spawnPoint.getWorld();
		Integer configuredRadius = world.getGameRuleValue(GameRules.RESPAWN_RADIUS);

		int radius = Math.max(0, configuredRadius);

		Location bestSafe = null;
		double bestSafeDistanceSquared = Double.MAX_VALUE;
		Location bestHazardous = null;
		double bestHazardousDistanceSquared = Double.MAX_VALUE;

		for (Column column : findColumnsByDistance(spawnPoint, radius)) {
			// A hazardous location never beats a safe one, so only the safe distance can rule out the remaining columns.
			if (column.horizontalDistanceSquared() >= bestSafeDistanceSquared) {
				break;
			}

			// Chunks that are not generated yet are skipped to not generate them during a respawn.
			if (!world.isChunkGenerated(column.x() >> 4, column.z() >> 4)) {
				continue;
			}

			for (int y = world.getMinHeight(); y < world.getMaxHeight() - 2; y++) {
				Block floor = world.getBlockAt(column.x(), y, column.z());
				Location candidate = toStandingLocation(floor);

				if (candidate == null) {
					continue;
				}

				double distanceSquared = candidate.distanceSquared(spawnPoint);

				if (isSafe(floor)) {
					if (distanceSquared < bestSafeDistanceSquared) {
						bestSafe = candidate;
						bestSafeDistanceSquared = distanceSquared;
					}
				} else if (distanceSquared < bestHazardousDistanceSquared) {
					bestHazardous = candidate;
					bestHazardousDistanceSquared = distanceSquared;
				}
			}
		}

		return bestSafe != null ? bestSafe : bestHazardous;
	}

	/**
	 * Gets all block columns in the square around the spawn point, the closest column first.
	 */
	private List<Column> findColumnsByDistance(Location spawnPoint, int radius) {
		int centerX = spawnPoint.getBlockX();
		int centerZ = spawnPoint.getBlockZ();
		List<Column> columns = new ArrayList<>();

		for (int x = centerX - radius; x <= centerX + radius; x++) {
			for (int z = centerZ - radius; z <= centerZ + radius; z++) {
				double dx = x + 0.5 - spawnPoint.getX();
				double dz = z + 0.5 - spawnPoint.getZ();

				columns.add(new Column(x, z, dx * dx + dz * dz));
			}
		}

		columns.sort(Comparator.comparingDouble(Column::horizontalDistanceSquared));

		return columns;
	}

	private record Column(int x, int z, double horizontalDistanceSquared) {
	}

	/**
	 * Checks whether the block and the two blocks above it are free of hazards.
	 */
	private boolean isSafe(Block floor) {
		Block feet = floor.getRelative(BlockFace.UP);
		Block head = feet.getRelative(BlockFace.UP);

		return !isHazardous(floor) && !isHazardous(feet) && !isHazardous(head);
	}

	/**
	 * Gets the location on top of the block if a player fits there.
	 * Hazards are not considered.
	 *
	 * @return the location or null if the player does not fit there
	 */
	private Location toStandingLocation(Block floor) {
		if (floor.isPassable()) {
			return null;
		}

		Block feet = floor.getRelative(BlockFace.UP);
		Block head = feet.getRelative(BlockFace.UP);

		if (!feet.isPassable() || !head.isPassable()) {
			return null;
		}

		double floorHeight = floor.getCollisionShape().getBoundingBoxes().stream()
			.mapToDouble(BoundingBox::getMaxY)
			.max()
			.orElse(0);

		// Blocks like fences are higher than one block and would collide with the player.
		if (floorHeight <= 0 || floorHeight > 1) {
			return null;
		}

		return new Location(floor.getWorld(), floor.getX() + 0.5, floor.getY() + floorHeight, floor.getZ() + 0.5);
	}

	private boolean isHazardous(Block block) {
		Material type = block.getType();

		return HAZARDOUS_MATERIALS.contains(type)
			|| Tag.FIRE.isTagged(type)
			|| Tag.CAMPFIRES.isTagged(type)
			|| block.isLiquid();
	}
}
