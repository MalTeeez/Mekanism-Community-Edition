package mekanism.generators.common.content.fission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import mekanism.api.Coord4D;
import mekanism.api.IHeatTransfer;
import mekanism.api.MekanismConfig.generators;
import mekanism.common.multiblock.MultiblockCache;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.multiblock.UpdateProtocol;
import mekanism.generators.common.GeneratorsBlocks;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.block.BlockReactor.ReactorBlockType;
import mekanism.generators.common.tile.fission.TileEntityControlRodAssembly;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import mekanism.generators.common.tile.fission.TileEntityFissionFuelAssembly;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class FissionUpdateProtocol extends UpdateProtocol<SynchronizedFissionData>
{
	public FissionUpdateProtocol(TileEntityFissionCasing tileEntity)
	{
		super(tileEntity);
	}

	@Override
	protected boolean isValidFrame(int x, int y, int z)
	{
		if(pointer.getWorldObj().getBlock(x, y, z) == GeneratorsBlocks.Reactor)
		{
			ReactorBlockType type = ReactorBlockType.get(GeneratorsBlocks.Reactor, pointer.getWorldObj().getBlockMetadata(x, y, z));

			return type == ReactorBlockType.FISSION_CASING || type == ReactorBlockType.FISSION_PORT || type == ReactorBlockType.FISSION_LOGIC_ADAPTER;
		}

		return false;
	}

	@Override
	protected boolean isValidInnerNode(int x, int y, int z)
	{
		if(super.isValidInnerNode(x, y, z))
		{
			return true;
		}

		TileEntity tile = pointer.getWorldObj().getTileEntity(x, y, z);

		return tile instanceof TileEntityFissionFuelAssembly || tile instanceof TileEntityControlRodAssembly;
	}

	@Override
	protected boolean canForm(SynchronizedFissionData structure)
	{
		Map<Long, Column> columns = new HashMap<Long, Column>();
		Set<Coord4D> fuelCoords = new HashSet<Coord4D>();
		int assemblies = 0;
		int surfaceArea = 0;

		for(Coord4D coord : innerNodes)
		{
			TileEntity tile = coord.getTileEntity(pointer.getWorldObj());
			long key = (((long)coord.xCoord) << 32) | (coord.zCoord & 0xFFFFFFFFL);
			Column column = columns.get(key);

			if(column == null)
			{
				column = new Column();
				columns.put(key, column);
			}

			if(tile instanceof TileEntityFissionFuelAssembly)
			{
				column.fuel.add(coord.yCoord);
				assemblies++;
				surfaceArea += 6;

				for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
				{
					if(fuelCoords.contains(coord.getFromSide(side)))
					{
						surfaceArea -= 2;
					}
				}

				fuelCoords.add(coord);
			}
			else if(tile instanceof TileEntityControlRodAssembly)
			{
				//Only one control rod per column
				if(column.rod != null)
				{
					return false;
				}

				column.rod = coord.yCoord;
			}
		}

		//Require at least one fuel assembly
		if(columns.isEmpty())
		{
			return false;
		}

		for(Column column : columns.values())
		{
			if(column.rod == null || column.fuel.isEmpty())
			{
				return false;
			}

			//Fuel must be one contiguous stack with the control rod directly on top
			if(column.fuel.last()-column.fuel.first()+1 != column.fuel.size() || column.rod != column.fuel.last()+1)
			{
				return false;
			}
		}

		structure.assemblies = assemblies;
		structure.surfaceArea = surfaceArea;

		return true;
	}

	@Override
	protected MultiblockCache<SynchronizedFissionData> getNewCache()
	{
		return new FissionCache();
	}

	@Override
	protected SynchronizedFissionData getNewStructure()
	{
		return new SynchronizedFissionData();
	}

	@Override
	protected MultiblockManager<SynchronizedFissionData> getManager()
	{
		return MekanismGenerators.fissionManager;
	}

	@Override
	protected void mergeCaches(List<ItemStack> rejectedItems, MultiblockCache<SynchronizedFissionData> cache, MultiblockCache<SynchronizedFissionData> merge)
	{
		FissionCache c = (FissionCache)cache;
		FissionCache m = (FissionCache)merge;

		if(c.fuel == null)
		{
			c.fuel = m.fuel;
		}
		else if(m.fuel != null && c.fuel.isGasEqual(m.fuel))
		{
			c.fuel.amount += m.fuel.amount;
		}

		if(c.waste == null)
		{
			c.waste = m.waste;
		}
		else if(m.waste != null && c.waste.isGasEqual(m.waste))
		{
			c.waste.amount += m.waste.amount;
		}

		if(c.water == null)
		{
			c.water = m.water;
		}
		else if(m.water != null && c.water.isFluidEqual(m.water))
		{
			c.water.amount += m.water.amount;
		}

		if(c.steam == null)
		{
			c.steam = m.steam;
		}
		else if(m.steam != null && c.steam.isFluidEqual(m.steam))
		{
			c.steam.amount += m.steam.amount;
		}

		if(c.sodium == null)
		{
			c.sodium = m.sodium;
		}
		else if(m.sodium != null && c.sodium.isGasEqual(m.sodium))
		{
			c.sodium.amount += m.sodium.amount;
		}

		if(c.superheatedSodium == null)
		{
			c.superheatedSodium = m.superheatedSodium;
		}
		else if(m.superheatedSodium != null && c.superheatedSodium.isGasEqual(m.superheatedSodium))
		{
			c.superheatedSodium.amount += m.superheatedSodium.amount;
		}

		c.temperature = Math.max(c.temperature, m.temperature);
		c.damage = Math.max(c.damage, m.damage);
		c.active = c.active && m.active;
		c.forceDisable = c.forceDisable || m.forceDisable;
		c.burnRemaining += m.burnRemaining;
		c.partialWaste += m.partialWaste;
	}

	@Override
	protected void onFormed()
	{
		super.onFormed();

		SynchronizedFissionData data = structureFound;

		data.heatCapacity = generators.fissionCasingHeatCapacity*data.locations.size();
		data.ambientTemp = getAmbientTemp(data);

		if(data.temperature <= 0)
		{
			data.temperature = data.ambientTemp;
		}

		data.fuelTank.setMaxGas(data.getFuelCapacity());
		data.wasteTank.setMaxGas(data.getWasteCapacity());
		data.fuelTank.setGas(data.fuelTank.stored);
		data.wasteTank.setGas(data.wasteTank.stored);

		data.sodiumTank.setMaxGas(data.getCoolantCapacity());
		data.superheatedSodiumTank.setMaxGas(data.getHeatedCoolantCapacity());
		data.sodiumTank.setGas(data.sodiumTank.stored);
		data.superheatedSodiumTank.setGas(data.superheatedSodiumTank.stored);

		if(data.waterStored != null)
		{
			data.waterStored.amount = Math.min(data.waterStored.amount, data.getCoolantCapacity());
		}

		if(data.steamStored != null)
		{
			data.steamStored.amount = Math.min(data.steamStored.amount, data.getHeatedCoolantCapacity());
		}

		data.rateLimit = Math.min(data.rateLimit, data.getMaxBurnRate());
	}

	/** Average biome temperature over the 8 corners of the structure, as in upstream HeatAPI.getAmbientTemp. */
	private double getAmbientTemp(SynchronizedFissionData data)
	{
		double sum = 0;

		for(int i = 0; i < 8; i++)
		{
			int x = (i & 1) == 0 ? data.minLocation.xCoord : data.maxLocation.xCoord;
			int y = (i & 2) == 0 ? data.minLocation.yCoord : data.maxLocation.yCoord;
			int z = (i & 4) == 0 ? data.minLocation.zCoord : data.maxLocation.zCoord;

			sum += pointer.getWorldObj().getBiomeGenForCoords(x, z).getFloatTemperature(x, y, z);
		}

		return IHeatTransfer.AMBIENT_TEMP+25*(Math.max(-5, Math.min(5, sum/8))-0.8);
	}

	private static class Column
	{
		public TreeSet<Integer> fuel = new TreeSet<Integer>();
		public Integer rod;
	}
}
