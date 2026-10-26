package dev.yeetle.classicskyblock.islandmanagement.domain;

import dev.yeetle.classicskyblock.common.persistence.IslandData;
import dev.yeetle.classicskyblock.common.valueobjects.Globals;
import dev.yeetle.classicskyblock.islandmanagement.persistence.IslandRepository;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class IslandService {
	private final Logger log;
	private final IslandRepository islandRepository;

	private static final String OVERWORLD_PREFIX = "island_overworld_";
	private static final String NETHER_PREFIX = "island_nether_";

	public IslandService(Logger logger, IslandRepository islandRepository) {
		this.log = logger;
		this.islandRepository = islandRepository;
	}

	public boolean isIslandWorld(World world) {
		return isIslandWorld(world.getKey());
	}

	public boolean isIslandWorld(NamespacedKey key) {
		return key.getNamespace().equals(Globals.NAMESPACE);
	}

	public World createIslandForPlayer(Player player) {
		UUID islandId = UUID.randomUUID();

		World overworld = createIslandOverworld(islandId);
		if (overworld == null) {
			throw new RuntimeException("Die Welt konnte nicht erzeugt werden.");
		}

		islandRepository.saveOverworld(islandId, player.getUniqueId(), overworld.getKey());

		player.teleport(overworld.getSpawnLocation());
		log.info("A new island was created for player %s.".formatted(player.getName()));

		return overworld;
	}

	/**
	 * Deletes the island of the player, if there is one, and creates a new one.
	 * Everyone who is currently on the old island is moved to the default world first.
	 *
	 * @return the new overworld or null if the old island could not be deleted
	 */
	public World regenerateIslandForPlayer(Player player) {
		UUID islandId = islandRepository.findIslandIdByOwner(player.getUniqueId());
		IslandData islandData = islandRepository.findIsland(islandId);

		World oldOverworld = islandData.getOverworldKey() == null ? null : loadWorld(islandData.getOverworldKey());
		// An unloaded nether must not be loaded (and thereby created) only to move players out of it.
		World oldNether = islandData.getNetherKey() == null ? null : Bukkit.getWorld(islandData.getNetherKey());

		World newOverworld = createIslandForPlayer(player);

		if (oldOverworld != null) {
			for (Player p : oldOverworld.getPlayers()) {
				p.teleport(newOverworld.getSpawnLocation());
			}
		}
		if (oldNether != null) {
			for (Player p : oldNether.getPlayers()) {
				p.teleport(newOverworld.getSpawnLocation());
			}
		}

		if (islandId != null && !deleteIsland(islandId)) {
			player.sendMessage("The old island could not be deleted completely.");
			return null;
		}

		return newOverworld;
	}

	public boolean ownsAnIsland(Player player) {
		return islandRepository.ownsAnIsland(player.getUniqueId());
	}

	/**
	 * Finds the overworld of the island owned by the player with the given name.
	 *
	 * @return the key of the overworld or null if no player of that name owns an island with an overworld
	 */
	public NamespacedKey findOverworldKey(String ownerName) {
		IslandData island = islandRepository.findIslandByOwnerName(ownerName);

		return island != null ? island.getOverworldKey() : null;
	}

	public List<String> getIslandOwnerNames() {
		return islandRepository.findIslandOwnerNames();
	}

	private boolean deleteIsland(UUID islandId) {
		IslandData island = islandRepository.findIsland(islandId);
		World fallback = Bukkit.getWorlds().getFirst();
		boolean deleted = true;

		for (NamespacedKey key : new NamespacedKey[]{island.getOverworldKey(), island.getNetherKey()}) {
			World world = key != null ? Bukkit.getWorld(key) : null;

			if (world == null) {
				continue;
			}

			for (Player occupant : world.getPlayers()) {
				occupant.teleport(fallback.getSpawnLocation());
			}

			File folder = world.getWorldFolder();

			if (!Bukkit.unloadWorld(world, false)) {
				log.warning("World %s could not be unloaded.".formatted(key));
				deleted = false;
				continue;
			}

			deleted &= deleteRecursively(folder.toPath());
		}

		if (deleted) {
			islandRepository.removeIsland(islandId);
		}

		return deleted;
	}

	private boolean deleteRecursively(Path folder) {
		if (!Files.exists(folder)) {
			return true;
		}

		try (Stream<Path> paths = Files.walk(folder)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
			return true;
		} catch (IOException e) {
			log.warning("Could not delete %s: %s".formatted(folder, e.getMessage()));
			return false;
		}
	}

	public World loadCorrespondingWorld(World world) {
		if (!isIslandWorld(world)) {
			log.warning("Tried to load corresponding world of default world %s.\nThis is a programming error.".formatted(world.getKey().toString()));
			return null;
		}

		NamespacedKey correspondingKey = getCorrespondingWorldKey(world);

		return loadWorld(correspondingKey);
	}

	public World loadWorld(NamespacedKey key) {
		if (!isIslandWorld(key)) {
			log.warning("Tried to load or create an island with invalid Namespace %s.\nThis is a programming error.".formatted(key.getNamespace()));
			return Bukkit.createWorld(new WorldCreator(key));
		}

		WorldCreator creator = new WorldCreator(key)
			.generator(new VoidChunkGenerator());

		if (key.getKey().startsWith(OVERWORLD_PREFIX)) {
			return creator.environment(World.Environment.NORMAL).createWorld();
		} else if (key.getKey().startsWith(NETHER_PREFIX)) {
			return creator.environment(World.Environment.NETHER).createWorld();
		}

		log.warning("Tried to load or create an island, but the key is not supported.\n".formatted(key.getNamespace()));
		return null;
	}

	private World createIslandOverworld(UUID islandId) {
		NamespacedKey key = new NamespacedKey(Globals.NAMESPACE, OVERWORLD_PREFIX + islandId.toString());
		WorldCreator creator = new WorldCreator(key)
			.generator(new VoidChunkGenerator())
			.environment(World.Environment.NORMAL);

		World world = creator.createWorld();

		if (world == null) {
			log.warning("Overworld island %s could not be created.".formatted(key.toString()));
			return null;
		}

		OverworldGenerator.generate(world);

		return world;
	}

	/**
	 * Creates the nether of an island and builds its island, if the island has no nether key yet.
	 * The island is built around the position where the player would have arrived through the portal.
	 *
	 * @param overworld the overworld of the island
	 * @param arrival   the location where vanilla would send the player in the nether
	 */
	public void createNetherIfMissing(World overworld, Location arrival) {
		UUID islandId = islandRepository.findIslandIdByWorld(overworld.getKey());
		IslandData island = islandId == null ? null : islandRepository.findIsland(islandId);

		if (island == null || island.getNetherKey() != null) {
			return;
		}

		World nether = loadCorrespondingWorld(overworld);

		if (nether == null) {
			log.warning("The nether of %s could not be created.".formatted(overworld.getKey()));
			return;
		}

		NetherGenerator.generate(nether, arrival.getBlockX(), arrival.getBlockZ());
		islandRepository.saveNether(islandId, nether.getKey());
	}

	private NamespacedKey getCorrespondingWorldKey(World world) {
		NamespacedKey key = world.getKey();
		if (!isIslandWorld(key)) {
			return null;
		}

		UUID islandId = islandRepository.findIslandIdByWorld(key);
		if (islandId == null) {
			return null;
		}
		
		IslandData islandData = islandRepository.findIsland(islandId);

		if (World.Environment.NORMAL.equals(world.getEnvironment())) {
			if (islandData.getNetherKey() != null) {
				return islandData.getNetherKey();
			}
			return new NamespacedKey(Globals.NAMESPACE, NETHER_PREFIX + islandId.toString());
		} else if (World.Environment.NETHER.equals(world.getEnvironment())) {
			if (islandData.getOverworldKey() != null) {
				return islandData.getOverworldKey();
			}
			return new NamespacedKey(Globals.NAMESPACE, OVERWORLD_PREFIX + islandId.toString());
		} else {
			log.warning("The environment %s is not supported on islands.".formatted(world.getEnvironment()));
			return null;
		}
	}
}
