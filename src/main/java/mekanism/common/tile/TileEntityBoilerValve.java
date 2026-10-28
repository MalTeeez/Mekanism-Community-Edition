package mekanism.common.tile;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.Range4D;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTransmission;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import mekanism.common.Mekanism;
import mekanism.common.content.boiler.BoilerSteamTank;
import mekanism.common.content.boiler.BoilerTank;
import mekanism.common.content.boiler.BoilerWaterTank;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.PipeUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntityBoilerValve extends TileEntityBoilerCasing implements IFluidHandler, IGasHandler, ITubeConnection, IConfigurable
{
	public BoilerValveMode mode = BoilerValveMode.INPUT;

	//True for valves saved before modes existed; the mode is picked from the valve's position once the structure is known
	private boolean legacyMode;

	public BoilerTank waterTank;
	public BoilerTank steamTank;

	public TileEntityBoilerValve()
	{
		super("BoilerValve");
		
		waterTank = new BoilerWaterTank(this);
		steamTank = new BoilerSteamTank(this);
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		if(!worldObj.isRemote)
		{
			if(legacyMode && structure != null && structure.upperRenderLocation != null)
			{
				legacyMode = false;
				mode = yCoord >= structure.upperRenderLocation.yCoord-1 ? BoilerValveMode.OUTPUT_STEAM : BoilerValveMode.INPUT;
				Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
				markDirty();
			}

			if(structure != null && mode == BoilerValveMode.OUTPUT_COOLANT && structure.cooledSodiumTank.getStored() > 0)
			{
				structure.cooledSodiumTank.draw(GasTransmission.emit(structure.cooledSodiumTank.getGas(), this, Arrays.asList(ForgeDirection.VALID_DIRECTIONS)), true);
			}
			
			if(structure != null && mode == BoilerValveMode.OUTPUT_STEAM)
			{
				if(structure.steamStored != null && structure.steamStored.amount > 0)
				{
					for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
					{
						TileEntity tile = Coord4D.get(this).getFromSide(side).getTileEntity(worldObj);
						
						if(tile instanceof IFluidHandler && !(tile instanceof TileEntityBoilerValve))
						{
							if(((IFluidHandler)tile).canFill(side.getOpposite(), structure.steamStored.getFluid()))
							{
								structure.steamStored.amount -= ((IFluidHandler)tile).fill(side.getOpposite(), structure.steamStored, true);
								
								if(structure.steamStored.amount <= 0)
								{
									structure.steamStored = null;
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		if((!worldObj.isRemote && structure != null) || (worldObj.isRemote && clientHasStructure))
		{
			if(mode == BoilerValveMode.OUTPUT_STEAM)
			{
				return new FluidTankInfo[] {steamTank.getInfo()};
			}
			else {
				return new FluidTankInfo[] {waterTank.getInfo()};
			}
		}
		
		return PipeUtils.EMPTY;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if(structure != null && mode == BoilerValveMode.INPUT)
		{
			return waterTank.fill(resource, doFill);
		}
		
		return 0;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		if(structure != null && mode == BoilerValveMode.OUTPUT_STEAM)
		{
			if(structure.steamStored != null)
			{
				if(resource.getFluid() == structure.steamStored.getFluid())
				{
					return steamTank.drain(resource.amount, doDrain);
				}
			}
		}

		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		if(structure != null && mode == BoilerValveMode.OUTPUT_STEAM)
		{
			return steamTank.drain(maxDrain, doDrain);
		}

		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		if((!worldObj.isRemote && structure != null) || (worldObj.isRemote && clientHasStructure))
		{
			return mode == BoilerValveMode.INPUT;
		}
		
		return false;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		if((!worldObj.isRemote && structure != null) || (worldObj.isRemote && clientHasStructure))
		{
			return mode == BoilerValveMode.OUTPUT_STEAM;
		}
		
		return false;
	}
	
	@Override
	public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer)
	{
		return canReceiveGas(side, stack.getGas()) ? structure.superheatedSodiumTank.receive(stack, doTransfer) : 0;
	}

	@Override
	public int receiveGas(ForgeDirection side, GasStack stack)
	{
		return receiveGas(side, stack, true);
	}

	@Override
	public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer)
	{
		return canDrawGas(side, null) ? structure.cooledSodiumTank.draw(amount, doTransfer) : null;
	}

	@Override
	public GasStack drawGas(ForgeDirection side, int amount)
	{
		return drawGas(side, amount, true);
	}

	@Override
	public boolean canReceiveGas(ForgeDirection side, Gas type)
	{
		return structure != null && mode == BoilerValveMode.INPUT && type == GasRegistry.getGas("superheatedSodium");
	}

	@Override
	public boolean canDrawGas(ForgeDirection side, Gas type)
	{
		return structure != null && mode == BoilerValveMode.OUTPUT_COOLANT && (type == null || type == GasRegistry.getGas("sodium"));
	}

	@Override
	public boolean canTubeConnect(ForgeDirection side)
	{
		return structure != null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		legacyMode = !nbtTags.hasKey("valveMode");
		mode = BoilerValveMode.values()[Math.max(0, Math.min(nbtTags.getInteger("valveMode"), BoilerValveMode.values().length-1))];
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		if(!legacyMode)
		{
			nbtTags.setInteger("valveMode", mode.ordinal());
		}
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);

		data.add(mode.ordinal());

		return data;
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);

		if(worldObj.isRemote)
		{
			mode = BoilerValveMode.values()[dataStream.readInt()];
		}
	}

	@Override
	public boolean onSneakRightClick(EntityPlayer player, int side)
	{
		if(!worldObj.isRemote)
		{
			legacyMode = false;
			mode = mode.next();
			player.addChatMessage(new ChatComponentText(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + LangUtils.localize("tooltip.configurator.boilerValveMode") + " " + EnumColor.INDIGO + LangUtils.localize("gui.boilerValveMode." + mode.name()) + "."));

			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
			markDirty();
		}

		return true;
	}

	@Override
	public boolean onRightClick(EntityPlayer player, int side)
	{
		return false;
	}

	public static enum BoilerValveMode
	{
		INPUT,
		OUTPUT_STEAM,
		OUTPUT_COOLANT;

		public BoilerValveMode next()
		{
			return values()[(ordinal()+1)%values().length];
		}
	}
}
