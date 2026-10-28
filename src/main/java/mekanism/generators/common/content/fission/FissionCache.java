package mekanism.generators.common.content.fission;

import mekanism.api.MekanismConfig.generators;
import mekanism.api.gas.GasStack;
import mekanism.common.multiblock.MultiblockCache;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class FissionCache extends MultiblockCache<SynchronizedFissionData>
{
	public GasStack fuel;
	public GasStack waste;
	public FluidStack water;
	public FluidStack steam;
	public GasStack sodium;
	public GasStack superheatedSodium;

	public double temperature;
	public double damage;
	public boolean active;
	public boolean forceDisable;
	public double rateLimit = generators.fissionDefaultBurnRate;
	public double burnRemaining;
	public double partialWaste;

	@Override
	public void apply(SynchronizedFissionData data)
	{
		data.fuelTank.stored = fuel;
		data.wasteTank.stored = waste;
		data.waterStored = water;
		data.steamStored = steam;
		data.sodiumTank.stored = sodium;
		data.superheatedSodiumTank.stored = superheatedSodium;
		data.temperature = temperature;
		data.damage = damage;
		data.active = active;
		data.forceDisable = forceDisable;
		data.rateLimit = rateLimit;
		data.burnRemaining = burnRemaining;
		data.partialWaste = partialWaste;
	}

	@Override
	public void sync(SynchronizedFissionData data)
	{
		fuel = data.fuelTank.stored;
		waste = data.wasteTank.stored;
		water = data.waterStored;
		steam = data.steamStored;
		sodium = data.sodiumTank.stored;
		superheatedSodium = data.superheatedSodiumTank.stored;
		temperature = data.temperature;
		damage = data.damage;
		active = data.active;
		forceDisable = data.forceDisable;
		rateLimit = data.rateLimit;
		burnRemaining = data.burnRemaining;
		partialWaste = data.partialWaste;
	}

	@Override
	public void load(NBTTagCompound nbtTags)
	{
		if(nbtTags.hasKey("cachedFuel"))
		{
			fuel = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedFuel"));
		}

		if(nbtTags.hasKey("cachedWaste"))
		{
			waste = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedWaste"));
		}

		if(nbtTags.hasKey("cachedWater"))
		{
			water = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("cachedWater"));
		}

		if(nbtTags.hasKey("cachedSteam"))
		{
			steam = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("cachedSteam"));
		}

		if(nbtTags.hasKey("cachedSodium"))
		{
			sodium = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedSodium"));
		}

		if(nbtTags.hasKey("cachedSuperheatedSodium"))
		{
			superheatedSodium = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedSuperheatedSodium"));
		}

		temperature = nbtTags.getDouble("temperature");
		damage = nbtTags.getDouble("damage");
		active = nbtTags.getBoolean("active");
		forceDisable = nbtTags.getBoolean("forceDisable");
		rateLimit = nbtTags.getDouble("rateLimit");
		burnRemaining = nbtTags.getDouble("burnRemaining");
		partialWaste = nbtTags.getDouble("partialWaste");
	}

	@Override
	public void save(NBTTagCompound nbtTags)
	{
		if(fuel != null)
		{
			nbtTags.setTag("cachedFuel", fuel.write(new NBTTagCompound()));
		}

		if(waste != null)
		{
			nbtTags.setTag("cachedWaste", waste.write(new NBTTagCompound()));
		}

		if(water != null)
		{
			nbtTags.setTag("cachedWater", water.writeToNBT(new NBTTagCompound()));
		}

		if(steam != null)
		{
			nbtTags.setTag("cachedSteam", steam.writeToNBT(new NBTTagCompound()));
		}

		if(sodium != null)
		{
			nbtTags.setTag("cachedSodium", sodium.write(new NBTTagCompound()));
		}

		if(superheatedSodium != null)
		{
			nbtTags.setTag("cachedSuperheatedSodium", superheatedSodium.write(new NBTTagCompound()));
		}

		nbtTags.setDouble("temperature", temperature);
		nbtTags.setDouble("damage", damage);
		nbtTags.setBoolean("active", active);
		nbtTags.setBoolean("forceDisable", forceDisable);
		nbtTags.setDouble("rateLimit", rateLimit);
		nbtTags.setDouble("burnRemaining", burnRemaining);
		nbtTags.setDouble("partialWaste", partialWaste);
	}
}
