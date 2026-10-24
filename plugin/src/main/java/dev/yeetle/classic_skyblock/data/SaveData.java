package dev.yeetle.classic_skyblock.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SaveData {
	private Map<UUID, PlayerData> players = new HashMap<>();
	private Map<UUID, IslandData> islands = new HashMap<>();

	public Map<UUID, PlayerData> getPlayers() {
		return players;
	}

	public Map<UUID, IslandData> getIslands() {
		return islands;
	}
}
