package mekanism.common.tile;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import mekanism.api.Coord4D;
import mekanism.api.MekanismConfig.usage;
import mekanism.api.Range4D;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.GasTransmission;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.IGasItem;
import mekanism.api.gas.IGasTankInfoProvider;
import mekanism.api.gas.ITubeConnection;
import mekanism.common.Mekanism;
import mekanism.common.Upgrade;
import mekanism.common.Upgrade.IUpgradeInfoHandler;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.base.IUpgradeTile;
import mekanism.common.block.BlockMachine.MachineType;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.GasCentrifugeRecipe;
import mekanism.common.security.ISecurityTile;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityIsotopicCentrifuge extends TileEntityNoisyElectricBlock implements IGasHandler, IGasTankInfoProvider, ITubeConnection, IRedstoneControl, IUpgradeTile, ISustainedData, IUpgradeInfoHandler, ITankManager, ISecurityTile, IBoundingBlock
{
	public GasTank inputTank = new GasTank(MAX_GAS);
	public GasTank outputTank = new GasTank(MAX_GAS);

	public static final int MAX_GAS = 10000;

	public int updateDelay;

	public int gasOutput = 256;

	public boolean isActive;

	public boolean clientActive;

	public double prevEnergy;

	public final double BASE_ENERGY_USAGE = usage.isotopicCentrifugeUsage;

	public double energyPerTick = BASE_ENERGY_USAGE;

	public GasCentrifugeRecipe cachedRecipe;

	public double clientEnergyUsed;

	public TileComponentUpgrade upgradeComponent = new TileComponentUpgrade(this, 3);
	public TileComponentSecurity securityComponent = new TileComponentSecurity(this);

	/** This machine's current RedstoneControl type. */
	public RedstoneControl controlType = RedstoneControl.DISABLED;

	public TileEntityIsotopicCentrifuge()
	{
		super("machine.centrifuge", "IsotopicCentrifuge", MachineType.ISOTOPIC_CENTRIFUGE.baseEnergy);

		inventory = new ItemStack[4];
		upgradeComponent.setSupported(Upgrade.MUFFLING);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if(worldObj.isRemote && updateDelay > 0)
		{
			updateDelay--;

			if(updateDelay == 0 && clientActive != isActive)
			{
				isActive = clientActive;
				MekanismUtils.updateBlock(worldObj, xCoord, yCoord, zCoord);
			}
		}

		if(!worldObj.isRemote)
		{
			if(updateDelay > 0)
			{
				updateDelay--;

				if(updateDelay == 0 && clientActive != isActive)
				{
					Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
				}
			}

			ChargeUtils.discharge(2, this);

			if(inventory[0] != null && (inputTank.getGas() == null || inputTank.getStored() < inputTank.getMaxGas()))
			{
				inputTank.receive(GasTransmission.removeGas(inventory[0], inputTank.getGasType(), inputTank.getNeeded()), true);
			}

			if(inventory[1] != null && outputTank.getGas() != null)
			{
				outputTank.draw(GasTransmission.addGas(inventory[1], outputTank.getGas()), true);
			}

			GasCentrifugeRecipe recipe = getRecipe();

			if(canOperate(recipe) && getEnergy() >= energyPerTick && MekanismUtils.canFunction(this))
			{
				setActive(true);

				int operations = operate(recipe);
				double prev = getEnergy();

				setEnergy(getEnergy() - energyPerTick*operations);
				clientEnergyUsed = prev-getEnergy();
			}
			else {
				if(prevEnergy >= getEnergy())
				{
					setActive(false);
				}
			}

			if(outputTank.getGas() != null)
			{
				GasStack toSend = new GasStack(outputTank.getGas().getGas(), Math.min(outputTank.getStored(), gasOutput));

				TileEntity tileEntity = Coord4D.get(this).getFromSide(ForgeDirection.getOrientation(facing)).getTileEntity(worldObj);

				if(tileEntity instanceof IGasHandler)
				{
					if(((IGasHandler)tileEntity).canReceiveGas(ForgeDirection.getOrientation(facing).getOpposite(), outputTank.getGas().getGas()))
					{
						outputTank.draw(((IGasHandler)tileEntity).receiveGas(ForgeDirection.getOrientation(facing).getOpposite(), toSend, true), true);
					}
				}
			}

			prevEnergy = getEnergy();
		}
	}

	public GasCentrifugeRecipe getRecipe()
	{
		GasInput input = getInput();

		if(cachedRecipe == null || !input.testEquality(cachedRecipe.getInput()))
		{
			cachedRecipe = RecipeHandler.getIsotopicCentrifugeRecipe(getInput());
		}

		return cachedRecipe;
	}

	public GasInput getInput()
	{
		return new GasInput(inputTank.getGas());
	}

	public boolean canOperate(GasCentrifugeRecipe recipe)
	{
		return recipe != null && recipe.canOperate(inputTank, outputTank);
	}

	public int operate(GasCentrifugeRecipe recipe)
	{
		int operations = Math.min(getUpgradedUsage(), Math.min(inputTank.getStored()/recipe.getInput().ingredient.amount, outputTank.getNeeded()/recipe.getOutput().output.amount));

		recipe.operate(inputTank, outputTank, operations);

		return operations;
	}

	public int getUpgradedUsage()
	{
		int possibleProcess = (int)Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED));

		return Math.min((int)(getEnergy()/energyPerTick), possibleProcess);
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);

		if(worldObj.isRemote)
		{
			isActive = dataStream.readBoolean();
			controlType = RedstoneControl.values()[dataStream.readInt()];
			clientEnergyUsed = dataStream.readDouble();

			if(dataStream.readBoolean())
			{
				inputTank.setGas(new GasStack(GasRegistry.getGas(dataStream.readInt()), dataStream.readInt()));
			}
			else {
				inputTank.setGas(null);
			}

			if(dataStream.readBoolean())
			{
				outputTank.setGas(new GasStack(GasRegistry.getGas(dataStream.readInt()), dataStream.readInt()));
			}
			else {
				outputTank.setGas(null);
			}

			MekanismUtils.updateBlock(worldObj, xCoord, yCoord, zCoord);
		}
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);

		data.add(isActive);
		data.add(controlType.ordinal());
		data.add(clientEnergyUsed);

		if(inputTank.getGas() != null)
		{
			data.add(true);
			data.add(inputTank.getGas().getGas().getID());
			data.add(inputTank.getStored());
		}
		else {
			data.add(false);
		}

		if(outputTank.getGas() != null)
		{
			data.add(true);
			data.add(outputTank.getGas().getGas().getID());
			data.add(outputTank.getStored());
		}
		else {
			data.add(false);
		}

		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		isActive = nbtTags.getBoolean("isActive");
		controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];

		inputTank.read(nbtTags.getCompoundTag("inputTank"));
		outputTank.read(nbtTags.getCompoundTag("outputTank"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setBoolean("isActive", isActive);
		nbtTags.setInteger("controlType", controlType.ordinal());

		nbtTags.setTag("inputTank", inputTank.write(new NBTTagCompound()));
		nbtTags.setTag("outputTank", outputTank.write(new NBTTagCompound()));
	}

	@Override
	public boolean canSetFacing(int i)
	{
		return i != 0 && i != 1;
	}

	@Override
	public void onPlace()
	{
		MekanismUtils.makeBoundingBlock(worldObj, Coord4D.get(this).getFromSide(ForgeDirection.UP), Coord4D.get(this));
	}

	@Override
	public void onBreak()
	{
		worldObj.setBlockToAir(xCoord, yCoord+1, zCoord);
		worldObj.setBlockToAir(xCoord, yCoord, zCoord);
	}

	/** Output through the front, input through any other side but the top. */
	public GasTank getTank(ForgeDirection side)
	{
		if(side == ForgeDirection.getOrientation(facing))
		{
			return outputTank;
		}
		else if(side != ForgeDirection.UP)
		{
			return inputTank;
		}

		return null;
	}

	@Override
	public void setActive(boolean active)
	{
		isActive = active;

		if(clientActive != active && updateDelay == 0)
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));

			updateDelay = 10;
			clientActive = active;
		}
	}

	@Override
	public boolean getActive()
	{
		return isActive;
	}

	@Override
	public boolean renderUpdate()
	{
		return false;
	}

	@Override
	public boolean lightUpdate()
	{
		return true;
	}

	@Override
	public boolean canTubeConnect(ForgeDirection side)
	{
		return getTank(side) != null;
	}

	@Override
	public boolean canReceiveGas(ForgeDirection side, Gas type)
	{
		return getTank(side) == inputTank && inputTank.canReceive(type) && Recipe.ISOTOPIC_CENTRIFUGE.containsRecipe(type);
	}

	@Override
	public boolean canDrawGas(ForgeDirection side, Gas type)
	{
		return getTank(side) == outputTank && outputTank.canDraw(type);
	}

	@Override
	public RedstoneControl getControlType()
	{
		return controlType;
	}

	@Override
	public void setControlType(RedstoneControl type)
	{
		controlType = type;
		MekanismUtils.saveChunk(this);
	}

	@Override
	public boolean canPulse()
	{
		return false;
	}

	@Override
	public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer)
	{
		if(canReceiveGas(side, stack != null ? stack.getGas() : null))
		{
			return getTank(side).receive(stack, doTransfer);
		}

		return 0;
	}

	@Override
	public int receiveGas(ForgeDirection side, GasStack stack)
	{
		return receiveGas(side, stack, true);
	}

	@Override
	public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer)
	{
		if(canDrawGas(side, null))
		{
			return getTank(side).draw(amount, doTransfer);
		}

		return null;
	}

	@Override
	public GasStack drawGas(ForgeDirection side, int amount)
	{
		return drawGas(side, amount, true);
	}

	@Override
	@Nonnull
	public GasTankInfo[] getTankInfo()
	{
		return new GasTankInfo[] {inputTank, outputTank};
	}

	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemstack)
	{
		return slotID == 2 && ChargeUtils.canBeDischarged(itemstack);
	}

	@Override
	public boolean canExtractItem(int slotID, ItemStack itemstack, int side)
	{
		if(slotID == 0)
		{
			return itemstack != null && itemstack.getItem() instanceof IGasItem && ((IGasItem)itemstack.getItem()).canReceiveGas(itemstack, null);
		}
		else if(slotID == 1)
		{
			return itemstack != null && itemstack.getItem() instanceof IGasItem && ((IGasItem)itemstack.getItem()).canProvideGas(itemstack, null);
		}
		else if(slotID == 2)
		{
			return ChargeUtils.canBeOutputted(itemstack, false);
		}

		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		if(side == MekanismUtils.getLeft(facing).ordinal())
		{
			return new int[] {0};
		}
		else if(side == MekanismUtils.getRight(facing).ordinal())
		{
			return new int[] {1};
		}
		else if(side == 0 || side == 1)
		{
			return new int[] {2};
		}

		return InventoryUtils.EMPTY;
	}

	@Override
	public void writeSustainedData(ItemStack itemStack)
	{
		if(inputTank.getGas() != null)
		{
			itemStack.stackTagCompound.setTag("inputTank", inputTank.getGas().write(new NBTTagCompound()));
		}

		if(outputTank.getGas() != null)
		{
			itemStack.stackTagCompound.setTag("outputTank", outputTank.getGas().write(new NBTTagCompound()));
		}
	}

	@Override
	public void readSustainedData(ItemStack itemStack)
	{
		inputTank.setGas(GasStack.readFromNBT(itemStack.stackTagCompound.getCompoundTag("inputTank")));
		outputTank.setGas(GasStack.readFromNBT(itemStack.stackTagCompound.getCompoundTag("outputTank")));
	}

	@Override
	public TileComponentUpgrade getComponent()
	{
		return upgradeComponent;
	}

	@Override
	public void recalculateUpgradables(Upgrade upgrade)
	{
		super.recalculateUpgradables(upgrade);

		switch(upgrade)
		{
			case ENERGY:
				maxEnergy = MekanismUtils.getMaxEnergy(this, BASE_MAX_ENERGY);
				energyPerTick = MekanismUtils.getBaseEnergyPerTick(this, BASE_ENERGY_USAGE);
			default:
				break;
		}
	}

	@Override
	public List<String> getInfo(Upgrade upgrade)
	{
		return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
	}

	@Override
	public Object[] getTanks()
	{
		return new Object[] {inputTank, outputTank};
	}

	@Override
	public TileComponentSecurity getSecurity()
	{
		return securityComponent;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}
}
