package dev.yeetle.classicskyblock.islandmanagement.domain;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Generates an empty world.
 * Structures are still generated so that their spawn areas exist, but the decoration blocks for the structures are not placed.
 * This allows building farms, for example, for witches in a swamp hut.
 */
public final class VoidChunkGenerator extends ChunkGenerator {
	@Override
	public boolean shouldGenerateNoise() {
		return false;
	}

	@Override
	public boolean shouldGenerateSurface() {
		return false;
	}

	@Override
	public boolean shouldGenerateCaves() {
		return false;
	}

	@Override
	public boolean shouldGenerateDecorations() {
		return false;
	}

	@Override
	public boolean shouldGenerateMobs() {
		return false;
	}

	@Override
	public boolean shouldGenerateStructures() {
		return true;
	}

	@Override
	public void generateNoise(
		WorldInfo worldInfo,
		Random random,
		int chunkX,
		int chunkZ,
		ChunkData chunkData
	) {
		// empty on purpose – no terrain should be generated
	}
}
