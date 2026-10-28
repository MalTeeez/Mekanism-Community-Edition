package mekanism.generators.common.tile.fission;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.common.integration.IComputerIntegration;
import mekanism.common.util.LangUtils;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import dan200.computercraft.api.lua.LuaException;

public class TileEntityFissionLogicAdapter extends TileEntityFissionCasing implements IComputerIntegration
{
	public FissionLogic logicType = FissionLogic.DISABLED;

	public boolean prevOutputting;

	public TileEntityFissionLogicAdapter()
	{
		super("FissionLogicAdapter");
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if(!worldObj.isRemote)
		{
			boolean outputting = checkMode();

			if(outputting != prevOutputting)
			{
				prevOutputting = outputting;
				worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
			}
		}
	}

	public boolean checkMode()
	{
		if(worldObj.isRemote)
		{
			return prevOutputting;
		}

		if(structure == null)
		{
			return false;
		}

		switch(logicType)
		{
			case TEMPERATURE:
				return structure.temperature >= SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE;
			case EXCESS_WASTE:
				return structure.wasteTank.getNeeded() == 0;
			case DAMAGED:
				return structure.damage >= SynchronizedFissionData.MAX_DAMAGE;
			case DEPLETED:
				return structure.fuelTank.getStored() == 0;
			default:
				return false;
		}
	}

	@Override
	public void onPowerChange()
	{
		super.onPowerChange();

		if(!worldObj.isRemote && structure != null && logicType == FissionLogic.ACTIVATION)
		{
			if(!isPowered())
			{
				structure.active = false;
			}
			else if(!structure.forceDisable && structure.getFuelCapacity() > 0)
			{
				structure.active = true;
			}

			markDirty();
		}
	}

	@Override
	public boolean onActivate(EntityPlayer player)
	{
		if(!player.isSneaking() && structure != null)
		{
			player.openGui(MekanismGenerators.instance, 16, worldObj, xCoord, yCoord, zCoord);

			return true;
		}

		return false;
	}

	@Override
	public String getInventoryName()
	{
		return LangUtils.localize("gui.fissionLogicAdapter");
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		logicType = FissionLogic.values()[Math.max(0, Math.min(nbtTags.getInteger("logicType"), FissionLogic.values().length-1))];
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setInteger("logicType", logicType.ordinal());
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		if(!worldObj.isRemote)
		{
			//Peek so the superclass still sees its own packet types
			if(dataStream.getByte(dataStream.readerIndex()) == 10)
			{
				dataStream.readByte();
				int mode = dataStream.readInt();

				if(mode >= 0 && mode < FissionLogic.values().length)
				{
					logicType = FissionLogic.values()[mode];
					markDirty();
				}

				return;
			}

			super.handlePacketData(dataStream);

			return;
		}

		super.handlePacketData(dataStream);

		logicType = FissionLogic.values()[dataStream.readInt()];
		prevOutputting = dataStream.readBoolean();
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);

		data.add(logicType.ordinal());
		data.add(prevOutputting);

		return data;
	}

	private static final String[] methods = new String[] {"isActive", "getTemperature", "getDamage", "getBurnRate", "getMaxBurnRate", "setBurnRate", "getFuel", "getWaste", "getLogicMode"};

	@Override
	public String[] getMethods()
	{
		return methods;
	}

	@Override
	public Object[] invoke(int method, Object[] arguments) throws LuaException, InterruptedException
	{
		if(structure == null)
		{
			return new Object[] {"Unformed."};
		}

		switch(method)
		{
			case 0:
				return new Object[] {structure.active};
			case 1:
				return new Object[] {structure.temperature};
			case 2:
				return new Object[] {structure.damage};
			case 3:
				return new Object[] {structure.lastBurnRate};
			case 4:
				return new Object[] {structure.getMaxBurnRate()};
			case 5:
				if(arguments.length > 0 && arguments[0] instanceof Double && !((Double)arguments[0]).isNaN())
				{
					structure.rateLimit = Math.max(0, Math.min((Double)arguments[0], structure.getMaxBurnRate()));
					return new Object[] {"Burn rate set."};
				}
				else {
					return new Object[] {"Invalid parameters."};
				}
			case 6:
				return new Object[] {structure.fuelTank.getStored()};
			case 7:
				return new Object[] {structure.wasteTank.getStored()};
			case 8:
				return new Object[] {logicType.name()};
			default:
				return new Object[] {"Unknown command."};
		}
	}

	public static enum FissionLogic
	{
		DISABLED("disabled", new ItemStack(Items.gunpowder)),
		ACTIVATION("activation", new ItemStack(Items.flint_and_steel)),
		TEMPERATURE("temperature", new ItemStack(Items.redstone)),
		EXCESS_WASTE("excessWaste", new ItemStack(Items.redstone)),
		DAMAGED("damaged", new ItemStack(Items.redstone)),
		DEPLETED("depleted", new ItemStack(Items.redstone));

		private String name;
		private ItemStack renderStack;

		private FissionLogic(String s, ItemStack stack)
		{
			name = s;
			renderStack = stack;
		}

		public ItemStack getRenderStack()
		{
			return renderStack;
		}

		public String getLocalizedName()
		{
			return LangUtils.localize("fissionLogic." + name);
		}

		public String getDescription()
		{
			return LangUtils.localize("fissionLogic." + name + ".desc");
		}
	}
}
