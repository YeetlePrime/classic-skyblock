package dev.yeetle.classic_skyblock.data;

import org.bukkit.NamespacedKey;

import java.util.UUID;

public class IslandData {
	private UUID owner;
	private NamespacedKey overworldKey;
	private NamespacedKey netherKey;

	public IslandData(UUID owner) {
		this.owner = owner;
		this.overworldKey = null;
		this.netherKey = null;
	}

	public UUID getOwner() {
		return owner;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	public NamespacedKey getOverworldKey() {
		return overworldKey;
	}

	public void setOverworldKey(NamespacedKey overworldKey) {
		this.overworldKey = overworldKey;
	}

	public NamespacedKey getNetherKey() {
		return netherKey;
	}

	public void setNetherKey(NamespacedKey netherKey) {
		this.netherKey = netherKey;
	}
}
