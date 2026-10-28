package mekanism.generators.common.tile.fission;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nonnull;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.Range4D;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.GasTransmission;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.IGasTankInfoProvider;
import mekanism.api.gas.ITubeConnection;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.PipeUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntityFissionPort extends TileEntityFissionCasing implements IFluidHandler, IGasHandler, IGasTankInfoProvider, ITubeConnection, IConfigurable
{
	public PortMode mode = PortMode.INPUT;

	public TileEntityFissionPort()
	{
		super("FissionPort");
	}

	@Override
	public boolean canUpdate()
	{
		return true;
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if(!worldObj.isRemote && structure != null)
		{
			if(mode == PortMode.OUTPUT_WASTE && structure.wasteTank.getStored() > 0)
			{
				structure.wasteTank.draw(GasTransmission.emit(structure.wasteTank.getGas(), this, Arrays.asList(ForgeDirection.VALID_DIRECTIONS)), true);
			}

			if(mode == PortMode.OUTPUT_COOLANT && structure.steamStored != null)
			{
				int emitted = PipeUtils.emit(Arrays.asList(ForgeDirection.VALID_DIRECTIONS), structure.steamStored, this);
				structure.steamStored.amount -= emitted;

				if(structure.steamStored.amount <= 0)
				{
					structure.steamStored = null;
				}
			}

			if(mode == PortMode.OUTPUT_COOLANT && structure.superheatedSodiumTank.getStored() > 0)
			{
				structure.superheatedSodiumTank.draw(GasTransmission.emit(structure.superheatedSodiumTank.getGas(), this, Arrays.asList(ForgeDirection.VALID_DIRECTIONS)), true);
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		mode = PortMode.values()[Math.max(0, Math.min(nbtTags.getInteger("portMode"), PortMode.values().length-1))];
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setInteger("portMode", mode.ordinal());
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
			mode = PortMode.values()[dataStream.readInt()];
		}
	}

	@Override
	public boolean onSneakRightClick(EntityPlayer player, int side)
	{
		if(!worldObj.isRemote)
		{
			mode = mode.next();
			player.addChatMessage(new ChatComponentText(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + LangUtils.localize("tooltip.configurator.fissionPortMode") + " " + EnumColor.INDIGO + LangUtils.localize("gui.fissionPortMode." + mode.name()) + "."));

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

	private boolean hasStructure()
	{
		return structure != null;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if(resource == null || !canFill(from, resource.getFluid()))
		{
			return 0;
		}

		int toFill = Math.min(structure.getCoolantCapacity()-(structure.waterStored != null ? structure.waterStored.amount : 0), resource.amount);

		if(toFill > 0 && doFill)
		{
			if(structure.waterStored == null)
			{
				structure.waterStored = new FluidStack(FluidRegistry.WATER, toFill);
			}
			else {
				structure.waterStored.amount += toFill;
			}
		}

		return Math.max(toFill, 0);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		if(resource != null && canDrain(from, resource.getFluid()))
		{
			return drain(from, resource.amount, doDrain);
		}

		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		if(!canDrain(from, null) || structure.steamStored == null)
		{
			return null;
		}

		FluidStack ret = new FluidStack(structure.steamStored.getFluid(), Math.min(structure.steamStored.amount, maxDrain));

		if(doDrain)
		{
			structure.steamStored.amount -= ret.amount;

			if(structure.steamStored.amount <= 0)
			{
				structure.steamStored = null;
			}
		}

		return ret.amount > 0 ? ret : null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return hasStructure() && mode == PortMode.INPUT && fluid == FluidRegistry.WATER && structure.sodiumTank.getStored() == 0;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return hasStructure() && mode == PortMode.OUTPUT_COOLANT && (fluid == null || fluid == FluidRegistry.getFluid("steam"));
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		if(!hasStructure())
		{
			return PipeUtils.EMPTY;
		}

		return new FluidTankInfo[] {new FluidTankInfo(structure.waterStored, structure.getCoolantCapacity()), new FluidTankInfo(structure.steamStored, structure.getHeatedCoolantCapacity())};
	}

	@Override
	public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer)
	{
		if(!canReceiveGas(side, stack.getGas()))
		{
			return 0;
		}

		return stack.getGas() == GasRegistry.getGas("sodium") ? structure.sodiumTank.receive(stack, doTransfer) : structure.fuelTank.receive(stack, doTransfer);
	}

	@Override
	public int receiveGas(ForgeDirection side, GasStack stack)
	{
		return receiveGas(side, stack, true);
	}

	@Override
	public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer)
	{
		if(!canDrawGas(side, null))
		{
			return null;
		}

		return mode == PortMode.OUTPUT_WASTE ? structure.wasteTank.draw(amount, doTransfer) : structure.superheatedSodiumTank.draw(amount, doTransfer);
	}

	@Override
	public GasStack drawGas(ForgeDirection side, int amount)
	{
		return drawGas(side, amount, true);
	}

	@Override
	public boolean canReceiveGas(ForgeDirection side, Gas type)
	{
		if(!hasStructure() || mode != PortMode.INPUT)
		{
			return false;
		}

		//Only one kind of coolant at a time
		return type == GasRegistry.getGas("fissileFuel") || (type == GasRegistry.getGas("sodium") && structure.waterStored == null);
	}

	@Override
	public boolean canDrawGas(ForgeDirection side, Gas type)
	{
		if(!hasStructure())
		{
			return false;
		}

		return (mode == PortMode.OUTPUT_WASTE && (type == null || type == GasRegistry.getGas("nuclearWaste"))) || (mode == PortMode.OUTPUT_COOLANT && (type == null || type == GasRegistry.getGas("superheatedSodium")));
	}

	@Override
	@Nonnull
	public GasTankInfo[] getTankInfo()
	{
		return hasStructure() ? new GasTankInfo[] {structure.fuelTank, structure.wasteTank, structure.sodiumTank, structure.superheatedSodiumTank} : IGasTankInfoProvider.NONE;
	}

	@Override
	public boolean canTubeConnect(ForgeDirection side)
	{
		return hasStructure();
	}

	public static enum PortMode
	{
		INPUT,
		OUTPUT_WASTE,
		OUTPUT_COOLANT;

		public PortMode next()
		{
			return values()[(ordinal()+1)%values().length];
		}
	}
}
