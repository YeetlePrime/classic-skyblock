package dev.yeetle.classic_skyblock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds islands from patterns.
 * A pattern is a list of layers of characters, from the top layer to the bottom layer.
 * In a layer, "up" (the first row) is north, "left" (the first column) is west.
 */
public class IslandGenerator {
	private IslandGenerator() {
	}

	/**
	 * Places the layers of a pattern.
	 * The origin is the top left block (north west) of the bottom layer, which is the last layer of the pattern.
	 * Every layer above is placed one block higher than the layer below.
	 * The top of each layer is north.
	 * Characters of the palette are placed as blocks, all other characters (like spaces and dots) are left empty.
	 * Characters of the objects are handled after all blocks are placed, so a tree finds its ground.
	 */
	public static void place(
		World world,
		List<String[]> layers,
		Map<Character, Material> palette,
		Map<Character, Consumer<Location>> objects,
		int originX,
		int originY,
		int originZ
	) {
		List<Runnable> placeObjects = new ArrayList<>();

		for (int layer = 0; layer < layers.size(); layer++) {
			String[] rows = layers.get(layer);

			for (int z = 0; z < rows.length; z++) {
				for (int x = 0; x < rows[z].length(); x++) {
					char symbol = rows[z].charAt(x);
					Location location = new Location(world, originX + x, originY + layers.size() - 1 - layer, originZ + z);

					if (objects.containsKey(symbol)) {
						placeObjects.add(() -> objects.get(symbol).accept(location));
					} else if (palette.containsKey(symbol)) {
						// Without physics, gravity blocks like sand stay in the air until a neighbouring block changes.
						location.getBlock().setType(palette.get(symbol), false);
					}
				}
			}
		}

		placeObjects.forEach(Runnable::run);
	}

	/**
	 * Places a chest that is filled with the given items.
	 *
	 * @param facing the direction the front of the chest points to
	 */
	public static void placeChest(Location location, BlockFace facing, ItemStack... items) {
		Block block = location.getBlock();
		block.setType(Material.CHEST);

		Directional data = (Directional) block.getBlockData();
		data.setFacing(facing);
		block.setBlockData(data);

		Chest chest = (Chest) block.getState();
		chest.getBlockInventory().addItem(items);
	}
}
