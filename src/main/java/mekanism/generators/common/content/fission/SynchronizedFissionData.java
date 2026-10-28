package mekanism.generators.common.content.fission;

import mekanism.api.IHeatTransfer;
import mekanism.api.MekanismConfig.generators;
import mekanism.api.gas.GasTank;
import mekanism.common.multiblock.SynchronizedData;
import net.minecraftforge.fluids.FluidStack;

public class SynchronizedFissionData extends SynchronizedData<SynchronizedFissionData>
{
	public static final int FUEL_PER_ASSEMBLY = 8000;
	public static final int WASTE_PER_ASSEMBLY = 8000;
	public static final int COOLANT_PER_VOLUME = 100000;
	public static final int HEATED_COOLANT_PER_VOLUME = 1000000;

	public static final double MIN_DAMAGE_TEMPERATURE = 1200;
	public static final double MAX_DAMAGE_TEMPERATURE = 1800;
	public static final double MAX_DAMAGE = 100;

	public static final double INVERSE_INSULATION_COEFFICIENT = 10000;
	public static final double INVERSE_CONDUCTION_COEFFICIENT = 10;
	public static final double WATER_CONDUCTIVITY = 0.5;
	public static final double WATER_THERMAL_ENTHALPY = 10;
	public static final double STEAM_ENERGY_EFFICIENCY = 0.2;
	//Upstream fissionMeltdownRadiationMultiplier default
	public static final double MELTDOWN_RADIATION_MULTIPLIER = 50;
	public static final double BASE_BOIL_TEMP = 373.15;
	public static final double SODIUM_CONDUCTIVITY = 1;
	public static final double SODIUM_ENTHALPY = 5;

	public GasTank fuelTank = new GasTank(0);
	public GasTank wasteTank = new GasTank(0);

	public GasTank sodiumTank = new GasTank(0);
	public GasTank superheatedSodiumTank = new GasTank(0);

	public FluidStack waterStored;
	public FluidStack steamStored;

	/** Absolute temperature in kelvin. */
	public double temperature = IHeatTransfer.AMBIENT_TEMP;
	public double heatCapacity;

	/** Average biome ambient temperature over the structure, computed on formation. */
	public double ambientTemp = IHeatTransfer.AMBIENT_TEMP;

	public double damage;
	public boolean active;
	public boolean forceDisable;

	public double rateLimit = generators.fissionDefaultBurnRate;
	public double lastBurnRate;
	public double burnRemaining;
	public double partialWaste;

	public double lastEnvironmentLoss;
	public int lastBoilRate;

	public int assemblies;
	public int surfaceArea;

	public int getCoolantCapacity()
	{
		return volume*COOLANT_PER_VOLUME;
	}

	public int getHeatedCoolantCapacity()
	{
		//Upstream uses longs here; tanks are ints, so cap huge reactors
		return (int)Math.min((long)volume*HEATED_COOLANT_PER_VOLUME, Integer.MAX_VALUE);
	}

	public int getFuelCapacity()
	{
		return assemblies*FUEL_PER_ASSEMBLY;
	}

	public int getWasteCapacity()
	{
		return assemblies*WASTE_PER_ASSEMBLY;
	}

	public double getMaxBurnRate()
	{
		return assemblies*generators.fissionBurnPerAssembly;
	}

	public double getBoilEfficiency()
	{
		if(assemblies == 0)
		{
			return 0;
		}

		return Math.min(1, ((double)surfaceArea/(double)assemblies)/generators.fissionSurfaceAreaTarget);
	}

	public boolean isBurning()
	{
		return lastBurnRate > 0;
	}
}
