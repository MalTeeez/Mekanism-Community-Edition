package mekanism.common.content.boiler;

import mekanism.api.gas.GasStack;
import mekanism.common.multiblock.MultiblockCache;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class BoilerCache extends MultiblockCache<SynchronizedBoilerData>
{
	public FluidStack water;
	public FluidStack steam;
	
	public GasStack superheatedSodium;
	public GasStack cooledSodium;
	
	public double temperature;

	@Override
	public void apply(SynchronizedBoilerData data)
	{
		data.waterStored = water;
		data.steamStored = steam;
		data.superheatedSodiumTank.stored = superheatedSodium;
		data.cooledSodiumTank.stored = cooledSodium;
		data.temperature = temperature;
	}

	@Override
	public void sync(SynchronizedBoilerData data)
	{
		water = data.waterStored;
		steam = data.steamStored;
		superheatedSodium = data.superheatedSodiumTank.stored;
		cooledSodium = data.cooledSodiumTank.stored;
		temperature = data.temperature;
	}

	@Override
	public void load(NBTTagCompound nbtTags)
	{
		if(nbtTags.hasKey("cachedWater"))
		{
			water = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("cachedWater"));
		}
		
		if(nbtTags.hasKey("cachedSteam"))
		{
			steam = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("cachedSteam"));
		}
		
		if(nbtTags.hasKey("cachedSuperheatedSodium"))
		{
			superheatedSodium = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedSuperheatedSodium"));
		}
		
		if(nbtTags.hasKey("cachedCooledSodium"))
		{
			cooledSodium = GasStack.readFromNBT(nbtTags.getCompoundTag("cachedCooledSodium"));
		}
		
		temperature = nbtTags.getDouble("temperature");
	}

	@Override
	public void save(NBTTagCompound nbtTags)
	{
		if(water != null)
		{
			nbtTags.setTag("cachedWater", water.writeToNBT(new NBTTagCompound()));
		}
		
		if(steam != null)
		{
			nbtTags.setTag("cachedSteam", steam.writeToNBT(new NBTTagCompound()));
		}
		
		if(superheatedSodium != null)
		{
			nbtTags.setTag("cachedSuperheatedSodium", superheatedSodium.write(new NBTTagCompound()));
		}
		
		if(cooledSodium != null)
		{
			nbtTags.setTag("cachedCooledSodium", cooledSodium.write(new NBTTagCompound()));
		}
		
		nbtTags.setDouble("temperature", temperature);
	}
}
