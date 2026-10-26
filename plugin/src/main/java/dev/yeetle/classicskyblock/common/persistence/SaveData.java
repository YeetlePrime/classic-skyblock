package dev.yeetle.classicskyblock.common.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SaveData {
	private final Map<UUID, PlayerData> players = new HashMap<>();
	private final Map<UUID, IslandData> islands = new HashMap<>();

	public Map<UUID, PlayerData> getPlayers() {
		return players;
	}

	public Map<UUID, IslandData> getIslands() {
		return islands;
	}
}
