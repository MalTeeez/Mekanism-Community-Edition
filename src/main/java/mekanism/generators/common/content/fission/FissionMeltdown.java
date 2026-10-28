package mekanism.generators.common.content.fission;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

/** Upstream's 100 tick meltdown, used without u25core */
public class FissionMeltdown
{
	public static final int DURATION = 100;

	//ponytail: not persisted across restarts (upstream saves meltdowns), add if a restart mid-meltdown matters
	public static final List<FissionMeltdown> pending = new ArrayList<FissionMeltdown>();

	public final int dimension;
	public final int minX, minY, minZ, maxX, maxY, maxZ;
	public final double magnitude;
	public final float radius;

	private int ticksExisted;

	public FissionMeltdown(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, double magnitude, float radius)
	{
		dimension = world.provider.dimensionId;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.magnitude = magnitude;
		this.radius = radius;
	}

	public static void schedule(FissionMeltdown meltdown)
	{
		pending.add(meltdown);
	}

	public static void tickAll()
	{
		for(Iterator<FissionMeltdown> iter = pending.iterator(); iter.hasNext();)
		{
			FissionMeltdown meltdown = iter.next();
			World world = DimensionManager.getWorld(meltdown.dimension);

			if(world == null || meltdown.tick(world))
			{
				iter.remove();
			}
		}
	}

	/** @return true when finished */
	public boolean tick(World world)
	{
		ticksExisted++;

		if(world.rand.nextInt()%10 == 0 && world.rand.nextDouble() < magnitude*SynchronizedFissionData.EXPLOSION_CHANCE)
		{
			int x = minX+world.rand.nextInt(maxX-minX+1);
			int y = minY+world.rand.nextInt(maxY-minY+1);
			int z = minZ+world.rand.nextInt(maxZ-minZ+1);

			world.createExplosion(null, x, y, z, radius, true);
		}

		return ticksExisted >= DURATION;
	}
}
