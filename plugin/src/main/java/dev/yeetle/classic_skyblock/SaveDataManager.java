package dev.yeetle.classic_skyblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.yeetle.classic_skyblock.data.IslandData;
import dev.yeetle.classic_skyblock.data.PlayerData;
import dev.yeetle.classic_skyblock.data.SaveData;
import dev.yeetle.classic_skyblock.data.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SaveDataManager {
	private final Path file;
	private final Path tempFile;
	private final Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.registerTypeAdapter(NamespacedKey.class, new NamespacedKeyAdapter().nullSafe())
		.create();

	private boolean saveDataLoaded;

	private SaveData saveData = new SaveData();

	public SaveDataManager(JavaPlugin plugin) {
		Path dataFolder = plugin.getDataFolder().toPath();
		this.saveDataLoaded = false;
		this.file = dataFolder.resolve("savedata.json");
		this.tempFile = dataFolder.resolve("savedata.json.tmp");
	}

	/**
	 * Loads the SaveData from the json file.
	 * A missing file results in empty SaveData.
	 * A corrupt file results in an exception and leaves the file untouched.
	 */
	public void load() {
		if (!Files.exists(file)) {
			saveData = new SaveData();
			saveDataLoaded = true;
			return;
		}

		try {
			SaveData loaded = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), SaveData.class);
			saveData = loaded != null ? loaded : new SaveData();
			saveDataLoaded = true;
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + file, e);
		} catch (JsonParseException e) {
			throw new IllegalStateException("Could not parse " + file, e);
		}
	}

	/**
	 * Writes the SaveData to the json file.
	 * The data is written to a temporary file first, so a failed write does not destroy the existing file.
	 */
	public void save() {
		if (!saveDataLoaded) {
			return;
		}

		try {
			Files.createDirectories(file.getParent());
			Files.writeString(tempFile, gson.toJson(saveData), StandardCharsets.UTF_8);
			Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write " + file, e);
		}
	}

	public SaveData getSaveData() {
		return saveData;
	}

	public void saveLastWorld(Player player) {
		PlayerData playerData = saveData.getPlayers()
			.computeIfAbsent(player.getUniqueId(), id -> new PlayerData(player.getName()));

		playerData.setName(player.getName());
		playerData.setLastWorld(player.getWorld().getKey());

		save();
	}

	public void saveWorld(Player player, UUID islandId, World world) {
		IslandData islandData = saveData.getIslands()
			.computeIfAbsent(islandId, id -> new IslandData(player.getUniqueId()));

		setWorldKey(islandData, world);

		save();
	}

	/**
	 * Saves the world of an island that already exists.
	 */
	public void saveWorld(UUID islandId, World world) {
		IslandData islandData = saveData.getIslands().get(islandId);

		if (islandData == null) {
			throw new IllegalArgumentException("Es gibt keine Insel mit der Id " + islandId);
		}

		setWorldKey(islandData, world);

		save();
	}

	private void setWorldKey(IslandData islandData, World world) {
		switch (world.getEnvironment()) {
			case NORMAL -> islandData.setOverworldKey(world.getKey());
			case NETHER -> islandData.setNetherKey(world.getKey());
			default -> throw new IllegalStateException("Nur Nether und Overworld können gespeichert werden");
		}
	}

	/**
	 * Finds the id of the island with the given world.
	 *
	 * @return the island id or null if there is no island with that world
	 */
	public UUID findIslandId(NamespacedKey worldKey) {
		return saveData.getIslands().entrySet().stream()
			.filter(entry -> worldKey.equals(entry.getValue().getOverworldKey()) || worldKey.equals(entry.getValue().getNetherKey()))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	public NamespacedKey getLastWorld(Player player) {
		PlayerData playerData = saveData.getPlayers().get(player.getUniqueId());

		return playerData != null ? playerData.getLastWorld() : null;
	}

	public boolean ownsAnIsland(Player player) {
		Collection<IslandData> islands = saveData.getIslands().values();

		return islands.stream()
			.anyMatch(island -> island.getOwner().equals(player.getUniqueId()));
	}

	/**
	 * Finds the id of the island owned by the player.
	 *
	 * @return the island id or null if the player owns no island
	 */
	public UUID findIslandId(Player player) {
		return saveData.getIslands().entrySet().stream()
			.filter(entry -> entry.getValue().getOwner().equals(player.getUniqueId()))
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
		return saveData.getIslands().values().stream()
			.filter(island -> {
				PlayerData owner = saveData.getPlayers().get(island.getOwner());
				return owner != null && owner.getName().equalsIgnoreCase(ownerName);
			})
			.findFirst()
			.orElse(null);
	}

	public List<String> getIslandOwnerNames() {
		return saveData.getIslands().values().stream()
			.map(island -> saveData.getPlayers().get(island.getOwner()))
			.filter(Objects::nonNull)
			.map(PlayerData::getName)
			.toList();
	}

	public IslandData getIslandData(UUID islandId) {
		return saveData.getIslands().getOrDefault(islandId, null);
	}

	public void removeIsland(UUID islandId) {
		IslandData island = saveData.getIslands().remove(islandId);

		if (island != null) {
			saveData.getPlayers().values().forEach(playerData -> {
				playerData.getSpawnPoints().remove(island.getOverworldKey());
				playerData.getSpawnPoints().remove(island.getNetherKey());
			});
		}

		save();
	}

	public void saveSpawnPoint(Player player, Location location) {
		PlayerData playerData = saveData.getPlayers()
			.computeIfAbsent(player.getUniqueId(), id -> new PlayerData(player.getName()));

		playerData.getSpawnPoints().put(location.getWorld().getKey(), new SpawnPoint(location));

		save();
	}

	public void removeSpawnPoint(Player player, World world) {
		PlayerData playerData = saveData.getPlayers().get(player.getUniqueId());

		if (playerData != null && playerData.getSpawnPoints().remove(world.getKey()) != null) {
			save();
		}
	}

	/**
	 * Gets the spawn point of the player in the given world.
	 *
	 * @return the spawn point or null if the player has none in that world
	 */
	public Location getSpawnPoint(Player player, World world) {
		PlayerData playerData = saveData.getPlayers().get(player.getUniqueId());
		SpawnPoint spawnPoint = playerData != null ? playerData.getSpawnPoints().get(world.getKey()) : null;

		return spawnPoint != null ? spawnPoint.toLocation(world) : null;
	}

	private static class NamespacedKeyAdapter extends TypeAdapter<NamespacedKey> {
		@Override
		public void write(JsonWriter out, NamespacedKey value) throws IOException {
			out.value(value.toString());
		}

		@Override
		public NamespacedKey read(JsonReader in) throws IOException {
			String value = in.nextString();
			NamespacedKey key = NamespacedKey.fromString(value);

			if (key == null) {
				throw new JsonParseException("Invalid namespaced key: " + value);
			}

			return key;
		}
	}
}
