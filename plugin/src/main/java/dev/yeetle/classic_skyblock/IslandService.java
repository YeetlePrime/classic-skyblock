package dev.yeetle.classic_skyblock;

import org.bukkit.*;
import dev.yeetle.classic_skyblock.data.IslandData;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.logging.Logger;

public class IslandService {
	private final Logger log;
	private final SaveDataManager saveDataManager;
	
	private static final String OVERWORLD_PREFIX = "island_overworld_";
	private static final String NETHER_PREFIX = "island_nether_";
	
	public IslandService(Logger logger, SaveDataManager saveDataManager) {
		this.log = logger;
		this.saveDataManager = saveDataManager;
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
		
		saveDataManager.saveWorld(player, islandId, overworld);
		
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
		UUID islandId = saveDataManager.findIslandId(player);
		IslandData islandData = saveDataManager.getIslandData(islandId);
		
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

	public void joinIsland(Player player, String ownerName) {
		IslandData island = saveDataManager.findIslandByOwnerName(ownerName);

		if (island == null || island.getOverworldKey() == null) {
			player.sendMessage("There is no island owned by %s.".formatted(ownerName));
			return;
		}

		World world = loadWorld(island.getOverworldKey());

		if (world == null) {
			player.sendMessage("The island of %s could not be loaded.".formatted(ownerName));
			return;
		}

		player.teleport(world.getSpawnLocation());
		player.sendMessage("You joined the island of %s.".formatted(ownerName));
	}

	private boolean deleteIsland(UUID islandId) {
		IslandData island = saveDataManager.getSaveData().getIslands().get(islandId);
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
			saveDataManager.removeIsland(islandId);
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
		
		NamespacedKey correspondingKey = getCorrespondingWorldKey(world.getKey());
		
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
	 * @param arrival the location where vanilla would send the player in the nether
	 */
	public void createNetherIfMissing(World overworld, Location arrival) {
		UUID islandId = saveDataManager.findIslandId(overworld.getKey());
		IslandData island = islandId == null ? null : saveDataManager.getIslandData(islandId);

		if (island == null || island.getNetherKey() != null) {
			return;
		}

		World nether = loadCorrespondingWorld(overworld);

		if (nether == null) {
			log.warning("The nether of %s could not be created.".formatted(overworld.getKey()));
			return;
		}

		NetherGenerator.generate(nether, arrival.getBlockX(), arrival.getBlockZ());
		saveDataManager.saveWorld(islandId, nether);
	}

	private NamespacedKey getCorrespondingWorldKey(NamespacedKey key) {
		if (!isIslandWorld(key)) {
			log.warning("Tried to get corresponding world for non-island.");
			return null;
		}
		
		String keyValue = key.getKey();
		if (keyValue.startsWith(OVERWORLD_PREFIX)) {
			String id = keyValue.substring(OVERWORLD_PREFIX.length());
			return new NamespacedKey(key.getNamespace(), NETHER_PREFIX + id);
		} else if (keyValue.startsWith(NETHER_PREFIX)) {
			String id = keyValue.substring(NETHER_PREFIX.length());
			return new NamespacedKey(key.getNamespace(), OVERWORLD_PREFIX + id);
		} else {
			log.warning("Tried to get corresponding world for non-island world that is part of the proper namespace.");
			return null;
		}
	}
}
