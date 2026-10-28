package mekanism.generators.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.gas.GasTank;
import mekanism.api.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiFluidGauge;
import mekanism.client.gui.element.GuiFluidGauge.IFluidInfoHandler;
import mekanism.client.gui.element.GuiGasGauge;
import mekanism.client.gui.element.GuiGasGauge.IGasInfoHandler;
import mekanism.client.gui.element.GuiGauge.Type;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.generators.client.gui.element.GuiTurbineTab;
import mekanism.generators.client.gui.element.GuiTurbineTab.TurbineTab;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fluids.FluidTank;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiFissionReactor extends GuiMekanism
{
	public TileEntityFissionCasing tileEntity;

	public GuiTextField rateField;

	private List<GuiElement> fluidGauges;
	private List<GuiElement> gasGauges;
	private boolean gasCoolant;

	private GuiButton toggleButton;
	private GuiButton setButton;

	public GuiFissionReactor(InventoryPlayer inventory, TileEntityFissionCasing tentity)
	{
		super(tentity, new ContainerNull(inventory.player, tentity));
		tileEntity = tentity;
		guiElements.add(new GuiTurbineTab(this, tileEntity, TurbineTab.FISSION_STAT, 6, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png")));
		guiElements.add(new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.fuelTank;
			}
		}, Type.STANDARD, this, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png"), 8, 16));
		guiElements.add(new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.wasteTank;
			}
		}, Type.STANDARD, this, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png"), 28, 16));
		fluidGauges = Arrays.<GuiElement>asList(new GuiFluidGauge(new IFluidInfoHandler()
		{
			@Override
			public FluidTank getTank()
			{
				return new FluidTank(tileEntity.structure.waterStored, tileEntity.structure.getCoolantCapacity());
			}
		}, Type.STANDARD, this, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png"), 130, 16), new GuiFluidGauge(new IFluidInfoHandler()
		{
			@Override
			public FluidTank getTank()
			{
				return new FluidTank(tileEntity.structure.steamStored, tileEntity.structure.getHeatedCoolantCapacity());
			}
		}, Type.STANDARD, this, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png"), 150, 16));
		gasGauges = Arrays.<GuiElement>asList(new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.sodiumTank;
			}
		}, Type.STANDARD, this, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png"), 130, 16), new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.superheatedSodiumTank;
			}
		}, Type.STANDARD, this, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png"), 150, 16));
		guiElements.addAll(fluidGauges);
	}

	@Override
	public void initGui()
	{
		super.initGui();

		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;

		String prev = rateField != null ? rateField.getText() : "";

		rateField = new GuiTextField(fontRendererObj, guiWidth + 52, guiHeight + 112, 50, 11);
		rateField.setMaxStringLength(8);
		rateField.setText(prev);

		buttonList.clear();
		buttonList.add(toggleButton = new GuiButton(0, guiWidth + 48, guiHeight + 82, 80, 20, ""));
		buttonList.add(setButton = new GuiButton(1, guiWidth + 106, guiHeight + 109, 30, 17, LangUtils.localize("gui.set")));
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();

		rateField.updateCursorCounter();

		boolean sodium = tileEntity.structure.sodiumTank.getStored() > 0 || tileEntity.structure.superheatedSodiumTank.getStored() > 0;

		if(sodium != gasCoolant)
		{
			gasCoolant = sodium;
			guiElements.removeAll(gasCoolant ? fluidGauges : gasGauges);
			guiElements.addAll(gasCoolant ? gasGauges : fluidGauges);
		}

		toggleButton.displayString = LangUtils.localize(tileEntity.structure.active ? "gui.fission.deactivate" : "gui.fission.activate");
		toggleButton.enabled = tileEntity.structure.active || !tileEntity.structure.forceDisable;
	}

	@Override
	protected void actionPerformed(GuiButton button)
	{
		if(button == toggleButton)
		{
			ArrayList data = new ArrayList();
			data.add((byte)0);

			Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
		}
		else if(button == setButton)
		{
			setRate();
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		String name = tileEntity.getInventoryName();
		fontRendererObj.drawString(name, (xSize/2)-(fontRendererObj.getStringWidth(name)/2), 5, 0x404040);

		SynchronizedFissionData structure = tileEntity.structure;

		String status = structure.forceDisable ? "gui.fission.scram" : (structure.active ? "gui.fission.active" : "gui.fission.inactive");

		renderScaledText(LangUtils.localize("gui.fission.status") + ": " + LangUtils.localize(status), 50, 18, 0x00CD00, 78);
		renderScaledText(LangUtils.localize("gui.fission.temperature") + ": " + MekanismUtils.getTemperatureDisplay(structure.temperature, TemperatureUnit.KELVIN), 50, 28, 0x00CD00, 78);
		renderScaledText(LangUtils.localize("gui.fission.damage") + ": " + Math.min(100, Math.round(structure.damage/SynchronizedFissionData.MAX_DAMAGE*1000)/10D) + "%", 50, 38, 0x00CD00, 78);
		renderScaledText(LangUtils.localize("gui.fission.burnRate") + ": " + Math.round(structure.lastBurnRate*100)/100D + " mB/t", 50, 48, 0x00CD00, 78);
		renderScaledText(LangUtils.localize("gui.fission.rateLimit") + ": " + Math.round(structure.rateLimit*100)/100D + " / " + structure.getMaxBurnRate(), 50, 58, 0x00CD00, 78);
		renderScaledText(LangUtils.localize("gui.fission.boilRate") + ": " + structure.lastBoilRate + " mB/t", 50, 68, 0x00CD00, 78);

		fontRendererObj.drawString(LangUtils.localize("gui.fission.rateLimit") + ":", 8, 114, 0x404040);

		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
	{
		mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png"));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;
		drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);

		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);

		rateField.drawTextBox();
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button)
	{
		super.mouseClicked(mouseX, mouseY, button);

		rateField.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void keyTyped(char c, int i)
	{
		if(!rateField.isFocused() || i == Keyboard.KEY_ESCAPE)
		{
			super.keyTyped(c, i);
		}

		if(i == Keyboard.KEY_RETURN && rateField.isFocused())
		{
			setRate();
		}

		if(Character.isDigit(c) || c == '.' || isTextboxKey(c, i))
		{
			rateField.textboxKeyTyped(c, i);
		}
	}

	private void setRate()
	{
		try
		{
			double rate = Double.parseDouble(rateField.getText());

			ArrayList data = new ArrayList();
			data.add((byte)1);
			data.add(rate);

			Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
		}
		catch(NumberFormatException e) {}

		rateField.setText("");
	}
}
