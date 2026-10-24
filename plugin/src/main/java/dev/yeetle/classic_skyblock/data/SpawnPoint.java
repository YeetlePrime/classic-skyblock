package dev.yeetle.classic_skyblock.data;

import org.bukkit.Location;
import org.bukkit.World;

public class SpawnPoint {
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;

	public SpawnPoint(Location location) {
		this.x = location.getX();
		this.y = location.getY();
		this.z = location.getZ();
		this.yaw = location.getYaw();
		this.pitch = location.getPitch();
	}

	public Location toLocation(World world) {
		return new Location(world, x, y, z, yaw, pitch);
	}
}
