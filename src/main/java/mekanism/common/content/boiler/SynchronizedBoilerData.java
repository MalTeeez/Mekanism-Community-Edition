package mekanism.common.content.boiler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mekanism.api.Coord4D;
import mekanism.api.IHeatTransfer;
import mekanism.api.MekanismConfig.general;
import mekanism.api.gas.GasTank;
import mekanism.api.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.common.content.tank.SynchronizedTankData.ValveData;
import mekanism.common.multiblock.SynchronizedData;
import mekanism.common.util.FluidContainerUtils.ContainerEditMode;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

public class SynchronizedBoilerData extends SynchronizedData<SynchronizedBoilerData> implements IHeatTransfer
{
	public static Map<String, Boolean> clientHotMap = new HashMap<String, Boolean>();
	
	public static double CASING_INSULATION_COEFFICIENT = 1;
	public static double CASING_INVERSE_CONDUCTION_COEFFICIENT = 1;
	public static double MODERN_INVERSE_INSULATION = 100000;
	public static double MODERN_INVERSE_CONDUCTION = 1;
	public static double BASE_BOIL_TEMP = 100-(TemperatureUnit.AMBIENT.zeroOffset-TemperatureUnit.CELSIUS.zeroOffset);
	
	public static double SODIUM_COOLING_EFFICIENCY = 0.4;
	//Upstream: 1 mB superheated sodium delivers 5 heat, and 1 steam costs 10/0.2 = 50 heat, so 5/50 steam
	public static double SODIUM_STEAM_PER_MB = 0.1;
	public static double HEATED_COOLANT_TEMP = 100000;
	
	public GasTank superheatedSodiumTank = new GasTank(0);
	public GasTank cooledSodiumTank = new GasTank(0);
	
	public FluidStack waterStored;
	public FluidStack prevWater;

	public FluidStack steamStored;
	public FluidStack prevSteam;
	
	public double lastEnvironmentLoss;
	public int lastBoilRate;
	public int lastMaxBoil;
	
	public boolean clientHot;

	public double temperature;

	public double heatToAbsorb;

	public double heatCapacity = 1000;
	
	public int superheatingElements;
	
	public int waterVolume;
	
	public int steamVolume;

	public ItemStack[] inventory = new ItemStack[2];
	
	public Coord4D upperRenderLocation;

	public Set<ValveData> valves = new HashSet<ValveData>();
	
	/**
	 * @return how much heat energy is needed to convert one unit of water into steam
	 */
	public static double getHeatEnthalpy()
	{
		return general.maxEnergyPerSteam/(general.steamEnergyEfficiency*general.energyPerHeat);
	}

	/**
	 * @return heat units needed to raise the whole boiler by one degree
	 */
	public double getHeatCapacity()
	{
		return locations.size()*general.boilerCasingHeatCapacity;
	}

	public double getHeatAvailable()
	{
		double heatAvailable = (temperature-BASE_BOIL_TEMP)*getHeatCapacity()*general.boilerWaterConductivity;
		return Math.min(heatAvailable, superheatingElements*general.superheatingHeatTransfer);
	}
	
	public boolean needsRenderUpdate()
	{
		if((waterStored == null && prevWater != null) || (waterStored != null && prevWater == null))
		{
			return true;
		}
		
		if(waterStored != null && prevWater != null)
		{
			if((waterStored.getFluid() != prevWater.getFluid()) || (waterStored.amount != prevWater.amount))
			{
				return true;
			}
		}
		
		if((steamStored == null && prevSteam != null) || (steamStored != null && prevSteam == null))
		{
			return true;
		}
		
		if(steamStored != null && prevSteam != null)
		{
			if((steamStored.getFluid() != prevSteam.getFluid()) || (steamStored.amount != prevSteam.amount))
			{
				return true;
			}
		}
		
		return false;
	}

	@Override
	public ItemStack[] getInventory()
	{
		return inventory;
	}

	@Override
	public double getTemp()
	{
		return temperature;
	}

	@Override
	public double getInverseConductionCoefficient()
	{
		return CASING_INVERSE_CONDUCTION_COEFFICIENT*locations.size();
	}

	@Override
	public double getInsulationCoefficient(ForgeDirection side)
	{
		return CASING_INSULATION_COEFFICIENT*locations.size();
	}

	@Override
	public void transferHeatTo(double heat)
	{
		heatToAbsorb += heat;
	}

	@Override
	public double[] simulateHeat()
	{
		double heatToTransfer;
		double temperatureLoss;

		if(general.legacyHeatBalance)
		{
			heatToTransfer = temperatureLoss = temperature / (IHeatTransfer.AIR_INVERSE_COEFFICIENT + (CASING_INSULATION_COEFFICIENT + CASING_INVERSE_CONDUCTION_COEFFICIENT)*locations.size());
		}
		else {
			//Upstream's casing insulation and conduction are fixed for the whole boiler instead of per casing block
			temperatureLoss = temperature / (IHeatTransfer.AIR_INVERSE_COEFFICIENT + MODERN_INVERSE_INSULATION + MODERN_INVERSE_CONDUCTION);
			heatToTransfer = temperatureLoss*getHeatCapacity();
		}

		transferHeatTo(-heatToTransfer);

		return new double[] {0, temperatureLoss};
	}

	@Override
	public double applyTemperatureChange()
	{
		if (heatToAbsorb < 0) { // Heat subtraction
			double newTemperature = temperature + (heatToAbsorb / getHeatCapacity());
			temperature = newTemperature >= 0.01 ? newTemperature : 0.0;
		} else {
			temperature += heatToAbsorb / getHeatCapacity();
		}
		heatToAbsorb = 0;

		return temperature;
	}

	@Override
	public boolean canConnectHeat(ForgeDirection side)
	{
		return false;
	}

	@Override
	public IHeatTransfer getAdjacent(ForgeDirection side)
	{
		return null;
	}
}
