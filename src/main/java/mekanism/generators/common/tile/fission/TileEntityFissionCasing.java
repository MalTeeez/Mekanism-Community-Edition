package mekanism.generators.common.tile.fission;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.IHeatTransfer;
import mekanism.api.MekanismConfig;
import mekanism.api.MekanismConfig.generators;
import mekanism.api.Pos3D;
import mekanism.api.Range4D;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.client.sound.ISoundSource;
import mekanism.common.RadiationHooks;
import mekanism.common.Mekanism;
import mekanism.common.base.IHasSound;
import mekanism.common.base.SoundWrapper;
import mekanism.common.multiblock.MultiblockCache;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.multiblock.UpdateProtocol;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityMultiblock;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.content.fission.FissionCache;
import mekanism.generators.common.content.fission.FissionUpdateProtocol;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityFissionCasing extends TileEntityMultiblock<SynchronizedFissionData> implements IHasSound, ISoundSource
{
	@SideOnly(Side.CLIENT)
	public SoundWrapper sound;

	public TileEntityFissionCasing()
	{
		this("FissionCasing");
	}

	public TileEntityFissionCasing(String name)
	{
		super(name);
	}

	@Override
	public void validate()
	{
		super.validate();

		if(worldObj.isRemote)
		{
			try {
				sound = new SoundWrapper(this, this);
			} catch(Throwable t) {}
		}
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if(worldObj.isRemote && sound != null && shouldPlaySound() && sound.canRestart() && MekanismConfig.client.enableMachineSounds)
		{
			sound.reset();
			sound.play();
		}

		if(!worldObj.isRemote)
		{
			if(structure != null)
			{
				if(structure.waterStored != null && structure.waterStored.amount <= 0)
				{
					structure.waterStored = null;
					markDirty();
				}

				if(structure.steamStored != null && structure.steamStored.amount <= 0)
				{
					structure.steamStored = null;
					markDirty();
				}

				if(isRendering)
				{
					if(structure.active)
					{
						burnFuel();
					}
					else {
						structure.lastBurnRate = 0;
					}

					handleCoolant();

					double envLoss = (structure.temperature-structure.ambientTemp)/(IHeatTransfer.AIR_INVERSE_COEFFICIENT+SynchronizedFissionData.INVERSE_INSULATION_COEFFICIENT+SynchronizedFissionData.INVERSE_CONDUCTION_COEFFICIENT);
					structure.temperature -= envLoss;
					structure.lastEnvironmentLoss = Math.max(envLoss*structure.heatCapacity, 0);

					handleDamage();

					MekanismUtils.saveChunk(this);

					//Interior blocks do not notify the multiblock when removed, so periodically revalidate
					if(ticker % 100 == 0)
					{
						update();
					}
				}
			}
		}
	}

	private void burnFuel()
	{
		double storedFuel = structure.fuelTank.getStored()+structure.burnRemaining;
		double toBurn = Math.min(Math.min(structure.rateLimit, storedFuel), structure.getMaxBurnRate());

		storedFuel -= toBurn;

		int wholeFuel = (int)storedFuel;
		structure.burnRemaining = storedFuel-wholeFuel;

		if(wholeFuel <= 0)
		{
			structure.fuelTank.stored = null;
		}
		else if(structure.fuelTank.stored != null)
		{
			structure.fuelTank.stored.amount = wholeFuel;
		}

		structure.temperature += toBurn*generators.fissionEnergyPerFuel/structure.heatCapacity;

		structure.partialWaste += toBurn;
		int newWaste = (int)Math.floor(structure.partialWaste);

		if(newWaste > 0)
		{
			structure.partialWaste -= newWaste;
			int stored = structure.wasteTank.receive(new GasStack(GasRegistry.getGas("nuclearWaste"), newWaste), true);

			if(stored < newWaste)
			{
				RadiationHooks.release(worldObj, xCoord+0.5, yCoord+0.5, zCoord+0.5, new GasStack(GasRegistry.getGas("nuclearWaste"), newWaste-stored));
			}
		}

		structure.lastBurnRate = toBurn;
	}

	private void handleCoolant()
	{
		structure.lastBoilRate = 0;

		double heat = structure.getBoilEfficiency()*(structure.temperature-SynchronizedFissionData.BASE_BOIL_TEMP)*structure.heatCapacity;

		if(structure.waterStored != null && heat > 0)
		{
			double caseCoolantHeat = heat*SynchronizedFissionData.WATER_CONDUCTIVITY;
			int boiled = (int)Math.min(SynchronizedFissionData.STEAM_ENERGY_EFFICIENCY*caseCoolantHeat/SynchronizedFissionData.WATER_THERMAL_ENTHALPY, structure.waterStored.amount);

			if(boiled > 0)
			{
				structure.waterStored.amount -= boiled;

				if(structure.waterStored.amount == 0)
				{
					structure.waterStored = null;
				}

				//Like upstream, steam that doesn't fit is dumped but still cools the reactor
				if(structure.steamStored == null)
				{
					structure.steamStored = new FluidStack(FluidRegistry.getFluid("steam"), 0);
				}

				structure.steamStored.amount = (int)Math.min((long)structure.steamStored.amount+boiled, structure.getHeatedCoolantCapacity());

				structure.temperature -= boiled*SynchronizedFissionData.WATER_THERMAL_ENTHALPY/SynchronizedFissionData.STEAM_ENERGY_EFFICIENCY/structure.heatCapacity;
				structure.lastBoilRate = boiled;
			}
		}
		else if(structure.sodiumTank.getStored() > 0 && heat > 0)
		{
			double caseCoolantHeat = heat*SynchronizedFissionData.SODIUM_CONDUCTIVITY;
			int heated = (int)Math.min(caseCoolantHeat/SynchronizedFissionData.SODIUM_ENTHALPY, structure.sodiumTank.getStored());

			if(heated > 0)
			{
				structure.sodiumTank.draw(heated, true);
				structure.superheatedSodiumTank.receive(new GasStack(GasRegistry.getGas("superheatedSodium"), heated), true);

				structure.temperature -= heated*SynchronizedFissionData.SODIUM_ENTHALPY/structure.heatCapacity;
				structure.lastBoilRate = heated;
			}
		}
	}

	private void handleDamage()
	{
		double temp = structure.temperature;

		if(temp > SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE)
		{
			structure.damage += Math.min(temp, SynchronizedFissionData.MAX_DAMAGE_TEMPERATURE)/(SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE*10);
		}
		else {
			structure.damage = Math.max(0, structure.damage-(SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE-temp)/(SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE*100));
		}

		if(structure.damage >= SynchronizedFissionData.MAX_DAMAGE && temp >= SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE)
		{
			if(structure.forceDisable && generators.fissionMeltdownsEnabled)
			{
				structure.forceDisable = false;
				meltdown();
			}
			else if(worldObj.rand.nextDouble() < (structure.damage/SynchronizedFissionData.MAX_DAMAGE)*generators.fissionMeltdownChance)
			{
				if(generators.fissionMeltdownsEnabled)
				{
					meltdown();
				}
				else {
					structure.forceDisable = true;
					structure.active = false;
				}
			}
		}
		else if(structure.damage < SynchronizedFissionData.MAX_DAMAGE && temp < SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE)
		{
			structure.forceDisable = false;
		}
	}

	private void meltdown()
	{
		int minX = structure.minLocation.xCoord, minY = structure.minLocation.yCoord, minZ = structure.minLocation.zCoord;
		int maxX = structure.maxLocation.xCoord, maxY = structure.maxLocation.yCoord, maxZ = structure.maxLocation.zCoord;
		double chance = structure.temperature*structure.heatCapacity*SynchronizedFissionData.EXPLOSION_CHANCE;

		double centerX = (minX+maxX)/2D+0.5, centerY = (minY+maxY)/2D+0.5, centerZ = (minZ+maxZ)/2D+0.5;

		if(structure.fuelTank.stored != null)
		{
			RadiationHooks.release(worldObj, centerX, centerY, centerZ, structure.fuelTank.stored.copy());
		}

		if(structure.wasteTank.stored != null)
		{
			RadiationHooks.release(worldObj, centerX, centerY, centerZ, structure.wasteTank.stored.copy());
		}

		//Reset first; the explosions may tear the structure down
		structure.active = false;
		structure.damage = generators.fissionPostMeltdownDamage;
		structure.burnRemaining = 0;
		structure.partialWaste = 0;
		structure.temperature = structure.ambientTemp;
		structure.steamStored = null;
		structure.superheatedSodiumTank.stored = null;
		structure.lastBurnRate = 0;

		//ponytail: upstream spreads this over a 100 tick Meltdown object; resolved at once here
		for(int i = 0; i < 100; i++)
		{
			if(worldObj.rand.nextInt(10) == 0 && worldObj.rand.nextDouble() < chance)
			{
				worldObj.createExplosion(null, minX+worldObj.rand.nextInt(maxX-minX+1)+0.5, minY+worldObj.rand.nextInt(maxY-minY+1)+0.5, minZ+worldObj.rand.nextInt(maxZ-minZ+1)+0.5, generators.fissionMeltdownRadius, true);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public SoundWrapper getSound()
	{
		return sound;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldPlaySound()
	{
		return !isInvalid() && isRendering && structure != null && structure.isBurning();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ResourceLocation getSoundLocation()
	{
		return new ResourceLocation("mekanism", "tile.fission");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getVolume()
	{
		return 1F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getPitch()
	{
		return 1F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Pos3D getSoundPosition()
	{
		return new Pos3D(xCoord+0.5, yCoord+0.5, zCoord+0.5);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldRepeat()
	{
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRepeatDelay()
	{
		return 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AttenuationType getAttenuation()
	{
		return AttenuationType.LINEAR;
	}

	@Override
	public String getInventoryName()
	{
		return LangUtils.localize("gui.fissionReactor");
	}

	@Override
	public boolean onActivate(EntityPlayer player)
	{
		if(!player.isSneaking() && structure != null)
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
			player.openGui(MekanismGenerators.instance, 8, worldObj, xCoord, yCoord, zCoord);

			return true;
		}

		return false;
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);

		if(structure != null)
		{
			data.add(structure.volume);
			data.add(structure.assemblies);
			data.add(structure.surfaceArea);
			data.add(structure.temperature);
			data.add(structure.heatCapacity);
			data.add(structure.damage);
			data.add(structure.active);
			data.add(structure.forceDisable);
			data.add(structure.rateLimit);
			data.add(structure.lastBurnRate);
			data.add(structure.lastBoilRate);
			data.add(structure.lastEnvironmentLoss);

			writeGas(data, structure.fuelTank.stored);
			writeGas(data, structure.wasteTank.stored);
			writeFluid(data, structure.waterStored);
			writeFluid(data, structure.steamStored);
			writeGas(data, structure.sodiumTank.stored);
			writeGas(data, structure.superheatedSodiumTank.stored);
		}

		return data;
	}

	private void writeGas(ArrayList data, GasStack stack)
	{
		if(stack != null)
		{
			data.add(1);
			data.add(stack.getGas().getID());
			data.add(stack.amount);
		}
		else {
			data.add(0);
		}
	}

	private void writeFluid(ArrayList data, FluidStack stack)
	{
		if(stack != null)
		{
			data.add(1);
			data.add(stack.getFluidID());
			data.add(stack.amount);
		}
		else {
			data.add(0);
		}
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		if(!worldObj.isRemote)
		{
			if(structure != null)
			{
				byte type = dataStream.readByte();

				if(type == 0)
				{
					if(structure.active)
					{
						structure.active = false;
					}
					else if(!structure.forceDisable && structure.getFuelCapacity() > 0)
					{
						structure.active = true;
					}
				}
				else if(type == 1)
				{
					double rate = dataStream.readDouble();

					if(!Double.isNaN(rate))
					{
						structure.rateLimit = Math.max(0, Math.min(rate, structure.getMaxBurnRate()));
					}
				}

				markDirty();
			}

			return;
		}

		super.handlePacketData(dataStream);

		if(clientHasStructure)
		{
			structure.volume = dataStream.readInt();
			structure.assemblies = dataStream.readInt();
			structure.surfaceArea = dataStream.readInt();
			structure.temperature = dataStream.readDouble();
			structure.heatCapacity = dataStream.readDouble();
			structure.damage = dataStream.readDouble();
			structure.active = dataStream.readBoolean();
			structure.forceDisable = dataStream.readBoolean();
			structure.rateLimit = dataStream.readDouble();
			structure.lastBurnRate = dataStream.readDouble();
			structure.lastBoilRate = dataStream.readInt();
			structure.lastEnvironmentLoss = dataStream.readDouble();

			structure.fuelTank.setMaxGas(structure.getFuelCapacity());
			structure.wasteTank.setMaxGas(structure.getWasteCapacity());
			structure.fuelTank.stored = readGas(dataStream);
			structure.wasteTank.stored = readGas(dataStream);
			structure.waterStored = readFluid(dataStream);
			structure.steamStored = readFluid(dataStream);
			structure.sodiumTank.setMaxGas(structure.getCoolantCapacity());
			structure.superheatedSodiumTank.setMaxGas(structure.getHeatedCoolantCapacity());
			structure.sodiumTank.stored = readGas(dataStream);
			structure.superheatedSodiumTank.stored = readGas(dataStream);
		}
	}

	private GasStack readGas(ByteBuf dataStream)
	{
		return dataStream.readInt() == 1 ? new GasStack(dataStream.readInt(), dataStream.readInt()) : null;
	}

	private FluidStack readFluid(ByteBuf dataStream)
	{
		return dataStream.readInt() == 1 ? new FluidStack(FluidRegistry.getFluid(dataStream.readInt()), dataStream.readInt()) : null;
	}

	@Override
	protected SynchronizedFissionData getNewStructure()
	{
		return new SynchronizedFissionData();
	}

	@Override
	public MultiblockCache<SynchronizedFissionData> getNewCache()
	{
		return new FissionCache();
	}

	@Override
	protected UpdateProtocol<SynchronizedFissionData> getProtocol()
	{
		return new FissionUpdateProtocol(this);
	}

	@Override
	public MultiblockManager<SynchronizedFissionData> getManager()
	{
		return MekanismGenerators.fissionManager;
	}
}
