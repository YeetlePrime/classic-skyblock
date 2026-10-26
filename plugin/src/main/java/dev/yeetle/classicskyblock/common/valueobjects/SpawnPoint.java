package dev.yeetle.classicskyblock.common.valueobjects;

import org.bukkit.Location;
import org.bukkit.World;

public class SpawnPoint {
	private final double x;
	private final double y;
	private final double z;
	private final float yaw;
	private final float pitch;
	
	public SpawnPoint(double x, double y, double z, float yaw, float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public SpawnPoint(Location location) {
		this(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
	}

	public Location toLocation(World world) {
		return new Location(world, x, y, z, yaw, pitch);
	}
}
