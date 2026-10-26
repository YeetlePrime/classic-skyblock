package dev.yeetle.classicskyblock.islandmanagement.domain;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Orientable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the starting island of an island nether.
 * The island is aligned so that its portal is where the player would have arrived through the nether portal.
 */
public class NetherGenerator {
	private NetherGenerator() {}
	/**
	 * The height of the bottom layer of the island.
	 */
	private static final int ISLAND_Y = 63;

	/**
	 * Builds the island, so that the center of its portal is at the given x and z.
	 */
	public static void generate(World world, int portalX, int portalZ) {
		Map<Character, Material> palette = Map.of(
			'r', Material.NETHERRACK,
			'w', Material.WARPED_NYLIUM,
			'c', Material.CRIMSON_NYLIUM,
			's', Material.SOUL_SAND,
			'n', Material.NETHER_WART,
			'o', Material.OBSIDIAN
		);

		Map<Character, Consumer<Location>> objects = Map.of(
			// The portal blocks extend along the x axis.
			'P', location -> {
				Orientable portalData = (Orientable) Material.NETHER_PORTAL.createBlockData();
				portalData.setAxis(Axis.X);
				location.getBlock().setBlockData(portalData, false);
			}
		);

		List<String[]> layers = List.of(
			new String[]{
				"....",
				"....",
				"oooo",
				"....",
				"....",
			},
			new String[]{
				"....",
				"....",
				"oPPo",
				"....",
				"....",
			},
			new String[]{
				"....",
				"....",
				"oPPo",
				"....",
				"....",
			},
			new String[]{
				"....",
				"n...",
				"oPPo",
				"...n",
				"....",
			},
			new String[]{
				"wwww",
				"swww",
				"oooo",
				"cccs",
				"cccc",
			},
			new String[]{
				"rrrr",
				"rrrr",
				"rrrr",
				"rrrr",
				"rrrr",
			},
			new String[]{
				"rrrr",
				"rrrr",
				"rrrr",
				"rrrr",
				"rrrr",
			}
		);

		IslandGenerator.place(world, layers, palette, objects, portalX + 1, ISLAND_Y, portalZ + 2);
	}
}
