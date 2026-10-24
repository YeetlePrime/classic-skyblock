package dev.yeetle.classic_skyblock.data;

import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {
	private String name;
	private NamespacedKey lastWorld;
	private Map<NamespacedKey, SpawnPoint> spawnPoints;

	public PlayerData(String name) {
		this.name = name;
		this.lastWorld = null;
		this.spawnPoints = new HashMap<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The spawn points of the player by world.
	 * Never null, even for save data that was written before spawn points existed.
	 */
	public Map<NamespacedKey, SpawnPoint> getSpawnPoints() {
		if (spawnPoints == null) {
			spawnPoints = new HashMap<>();
		}

		return spawnPoints;
	}

	public NamespacedKey getLastWorld() {
		return lastWorld;
	}

	public void setLastWorld(NamespacedKey lastWorld) {
		this.lastWorld = lastWorld;
	}
}
