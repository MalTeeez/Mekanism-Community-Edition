package mekanism.common;

import mekanism.api.gas.GasStack;
import net.minecraft.world.World;

/**
 * Single seam for radioactive releases; a no-op until a radiation framework (u25core) exists.
 */
public final class RadiationHooks
{
	//ponytail: no-op; route to the shared radiation API once it exists (upstream also dumps the released tanks then)
	public static void release(World world, double x, double y, double z, GasStack stack) {}
}
