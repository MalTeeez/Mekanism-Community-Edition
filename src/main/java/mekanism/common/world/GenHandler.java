package mekanism.common.world;

import java.util.Random;

import mekanism.api.MekanismConfig.general;
import mekanism.common.MekanismBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraft.world.gen.ChunkProviderHell;
import net.minecraft.world.gen.feature.WorldGenMinable;
import cpw.mods.fml.common.IWorldGenerator;

public class GenHandler implements IWorldGenerator
{
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
	{
		if(!(chunkGenerator instanceof ChunkProviderHell) && !(chunkGenerator instanceof ChunkProviderEnd))
		{
			for(int i = 0; i < general.osmiumPerChunk; i++)
			{
				int randPosX = (chunkX*16) + random.nextInt(16);
				int randPosY = random.nextInt(60);
				int randPosZ = (chunkZ*16) + random.nextInt(16);
				new WorldGenMinable(MekanismBlocks.OreBlock, 0, 8, Blocks.stone).generate(world, random, randPosX, randPosY, randPosZ);
			}

			for(int i = 0; i < general.copperPerChunk; i++)
			{
				int randPosX = (chunkX*16) + random.nextInt(16);
				int randPosY = random.nextInt(60);
				int randPosZ = (chunkZ*16) + random.nextInt(16);
				new WorldGenMinable(MekanismBlocks.OreBlock, 1, 8, Blocks.stone).generate(world, random, randPosX, randPosY, randPosZ);
			}

			for(int i = 0; i < general.tinPerChunk; i++)
			{
				int randPosX = (chunkX*16) + random.nextInt(16);
				int randPosY = random.nextInt(60);
				int randPosZ = (chunkZ*16) + random.nextInt(16);
				new WorldGenMinable(MekanismBlocks.OreBlock, 2, 8, Blocks.stone).generate(world, random, randPosX, randPosY, randPosZ);
			}
			
			if(general.leadGenerate)
			{
				generateOre(random, chunkX, chunkZ, world, 3, general.leadPerChunk, general.leadVeinSize, 0, 64, true);
			}

			if(general.uraniumGenerate)
			{
				generateOre(random, chunkX, chunkZ, world, 4, general.uraniumPerChunk, general.uraniumVeinSize, 0, 8, true);
				generateOre(random, chunkX, chunkZ, world, 4, general.uraniumBuriedPerChunk, general.uraniumBuriedVeinSize, 0, 56, true);
			}

			if(general.fluoriteGenerate)
			{
				generateOre(random, chunkX, chunkZ, world, 5, general.fluoritePerChunk, general.fluoriteVeinSize, 0, 23, false);
				generateOre(random, chunkX, chunkZ, world, 5, general.fluoriteBuriedPerChunk, general.fluoriteBuriedVeinSize, 0, 4, true);
			}

			for(int i = 0; i < general.saltPerChunk; i++)
			{
				int randPosX = (chunkX*16) + random.nextInt(16);
				int randPosZ = (chunkZ*16) + random.nextInt(16);
				int randPosY = world.getTopSolidOrLiquidBlock(randPosX, randPosZ);
				new WorldGenSalt(6).generate(world, random, randPosX, randPosY, randPosZ);
			}
		}
	}

	/** Heights are upstream's 1.20 ranges clamped to 1.7.10's 0 bottom; trapezoid is upstream's plateau-less triangle. */
	private static void generateOre(Random random, int chunkX, int chunkZ, World world, int meta, int perChunk, int veinSize, int minY, int maxY, boolean triangle)
	{
		for(int i = 0; i < perChunk; i++)
		{
			int range = maxY - minY + 1;
			int randPosX = (chunkX*16) + random.nextInt(16);
			int randPosY = minY + (triangle ? (random.nextInt(range) + random.nextInt(range)) / 2 : random.nextInt(range));
			int randPosZ = (chunkZ*16) + random.nextInt(16);
			new WorldGenMinable(MekanismBlocks.OreBlock, meta, veinSize, Blocks.stone).generate(world, random, randPosX, randPosY, randPosZ);
		}
	}
}
