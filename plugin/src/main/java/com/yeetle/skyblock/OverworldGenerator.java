package com.yeetle.skyblock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OverworldGenerator {
	private OverworldGenerator () {}
	/**
	 * The height of the bottom layer of the islands.
	 */
	private static final int ISLAND_Y = 65;

	/**
	 * Builds all islands.
	 */
	public static void generate(World world) {
		buildDirtIsland(world);
		buildSandIsland(world);
	}

	private static void buildDirtIsland(World world) {
		Map<Character, Material> palette = Map.of(
			'g', Material.GRASS_BLOCK,
			'd', Material.DIRT,
			'b', Material.BEDROCK
		);

		Map<Character, Consumer<Location>> objects = Map.of(
			'S', location -> world.setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
			'T', location -> world.generateTree(location, TreeType.TREE),
			'C', location -> IslandGenerator.placeChest(location, BlockFace.WEST,
				new ItemStack(Material.ICE),
				new ItemStack(Material.LAVA_BUCKET)
			)
		);
		
		List<String[]> layers = List.of(
			new String[]{
				"......",
				".S...C",
				"......",
				"......",
				"......",
				"T.....",
			},
			new String[]{
				"gggggg",
				"gggggg",
				"gggggg",
				"ggg...",
				"ggg...",
				"ggg...",
			},
			new String[]{
				"dddddd",
				"dddddd",
				"dddddd",
				"ddd...",
				"ddd...",
				"ddd...",
			},
			new String[]{
				"dddddd",
				"dbdddd",
				"dddddd",
				"ddd...",
				"ddd...",
				"ddd...",
			}
		);

		IslandGenerator.place(world, layers, palette, objects, -1, ISLAND_Y, -1);
	}

	private static void buildSandIsland(World world) {
		Map<Character, Material> palette = Map.of(
			's', Material.SAND,
			'c', Material.CACTUS
		);

		Map<Character, Consumer<Location>> objects = Map.of(
			// The chest must not touch the cactus, because the cactus breaks next to solid blocks.
			'C', location -> IslandGenerator.placeChest(location, BlockFace.NORTH,
				new ItemStack(Material.OBSIDIAN, 10),
				new ItemStack(Material.SWEET_BERRIES),
				new ItemStack(Material.GLOW_BERRIES),
				new ItemStack(Material.MELON_SLICE),
				new ItemStack(Material.PUMPKIN_SEEDS),
				new ItemStack(Material.SUGAR_CANE),
				new ItemStack(Material.COBWEB),
				new ItemStack(Material.COCOA_BEANS),
				new ItemStack(Material.MYCELIUM),
				new ItemStack(Material.GLOW_LICHEN),
				new ItemStack(Material.SUNFLOWER),
				new ItemStack(Material.ROSE_BUSH),
				new ItemStack(Material.LILAC),
				new ItemStack(Material.PEONY),
				new ItemStack(Material.BROWN_MUSHROOM)
			)
		);

		List<String[]> layers = List.of(
			new String[]{"...", ".C.", "..c"},
			new String[]{"sss", "sss", "sss"},
			new String[]{"sss", "sss", "sss"},
			new String[]{"sss", "sss", "sss"}
		);

		IslandGenerator.place(world, layers, palette, objects, -1, ISLAND_Y, 39);
	}
}
