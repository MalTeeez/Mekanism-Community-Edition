package mekanism.common;

import mekanism.api.gas.GasStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.sxmaa.u25core.api.radiation.RadiationAPI;
import cpw.mods.fml.common.Loader;

/**
 * Seam to the u25core radiation API. Every call is a no-op when u25core is absent.
 */
public final class RadiationHooks
{
	private static final boolean LOADED = Loader.isModLoaded("u25core");

	/** u25core is installed, so it handles meltdowns */
	public static boolean isLoaded()
	{
		return LOADED;
	}

	/** Radiation is active; only then consume material you would otherwise release */
	public static boolean isEnabled()
	{
		return LOADED && U25.isEnabled();
	}

	/** @return true if the gas became contamination, in which case the caller should consume it */
	public static boolean release(World world, double x, double y, double z, GasStack stack)
	{
		return LOADED && stack != null && U25.release(world, x, y, z, stack);
	}

	/** Sv/h of the whole stack */
	public static double getRadioactivity(GasStack stack)
	{
		return LOADED && stack != null ? U25.getRadioactivity(stack) : 0;
	}

	public static void exposeArea(World world, AxisAlignedBB box, double svPerHour, int ticks)
	{
		if(LOADED)
		{
			U25.exposeArea(world, box, svPerHour, ticks);
		}
	}

	public static void scheduleMeltdown(World world, AxisAlignedBB bounds, float explosionPower, double contamination, int durationTicks)
	{
		if(LOADED)
		{
			U25.scheduleMeltdown(world, bounds, explosionPower, contamination, durationTicks);
		}
	}

	/** Mekanism 10's radioactivity values, Sv/h per mB */
	public static void registerDefaults()
	{
		if(LOADED)
		{
			U25.register("nuclearWaste", 0.01);
			U25.register("spentNuclearWaste", 0.01);
			U25.register("plutonium", 0.02);
			U25.register("polonium", 0.05);
		}
	}

	//Separate class so RadiationAPI is only loaded when u25core is present
	private static final class U25
	{
		private static String key(GasStack stack)
		{
			return "gas:" + stack.getGas().getName();
		}

		private static boolean isEnabled()
		{
			return RadiationAPI.isEnabled();
		}

		private static boolean release(World world, double x, double y, double z, GasStack stack)
		{
			return RadiationAPI.release(world, x, y, z, key(stack), stack.amount);
		}

		private static double getRadioactivity(GasStack stack)
		{
			return RadiationAPI.getRadioactivity(key(stack))*stack.amount;
		}

		private static void exposeArea(World world, AxisAlignedBB box, double svPerHour, int ticks)
		{
			RadiationAPI.exposeArea(world, box, svPerHour, ticks);
		}

		private static void scheduleMeltdown(World world, AxisAlignedBB bounds, float explosionPower, double contamination, int durationTicks)
		{
			RadiationAPI.scheduleMeltdown(world, bounds, explosionPower, contamination, durationTicks);
		}

		private static void register(String gas, double svPerHour)
		{
			RadiationAPI.registerRadioactive("gas:" + gas, svPerHour);
		}
	}
}
