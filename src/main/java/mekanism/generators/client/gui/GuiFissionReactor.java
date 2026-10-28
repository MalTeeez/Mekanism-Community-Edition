package mekanism.generators.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.MekanismConfig.general;
import mekanism.api.gas.GasTank;
import mekanism.api.util.UnitDisplayUtils;
import mekanism.api.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiElement.IInfoHandler;
import mekanism.client.gui.element.GuiFluidGauge;
import mekanism.client.gui.element.GuiFluidGauge.IFluidInfoHandler;
import mekanism.client.gui.element.GuiGasGauge;
import mekanism.client.gui.element.GuiGasGauge.IGasInfoHandler;
import mekanism.client.gui.element.GuiGauge.Type;
import mekanism.client.gui.element.GuiGraph;
import mekanism.client.gui.element.GuiGraph.GraphDataHandler;
import mekanism.client.gui.element.GuiHeatInfo;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.api.util.ListUtils;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.client.gui.element.GuiTurbineTab;
import mekanism.generators.client.gui.element.GuiTurbineTab.TurbineTab;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fluids.FluidTank;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiFissionReactor extends GuiFissionReactorBase
{
	private static final int BUTTON_Y = 75;
	private static final int BUTTON_W = 81;
	private static final int BUTTON_H = 16;
	private static final int ACTIVATE_X = 6;
	private static final int SCRAM_X = 89;

	private List<GuiElement> fluidGauges;
	private List<GuiElement> gasGauges;
	private boolean gasCoolant;

	private GuiGraph heatGraph;

	public GuiFissionReactor(InventoryPlayer inventory, TileEntityFissionCasing tentity)
	{
		super(inventory, tentity);
		xSize = 195;

		guiElements.add(new GuiTurbineTab(this, tileEntity, TurbineTab.FISSION_STAT, 6, background));
		guiElements.add(new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.fuelTank;
			}
		}, Type.STANDARD, this, background, 25, 13));
		guiElements.add(new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.wasteTank;
			}
		}, Type.STANDARD, this, background, 171, 13));
		fluidGauges = Arrays.<GuiElement>asList(new GuiFluidGauge(new IFluidInfoHandler()
		{
			@Override
			public FluidTank getTank()
			{
				return new FluidTank(tileEntity.structure.waterStored, tileEntity.structure.getCoolantCapacity());
			}
		}, Type.STANDARD, this, background, 6, 13), new GuiFluidGauge(new IFluidInfoHandler()
		{
			@Override
			public FluidTank getTank()
			{
				return new FluidTank(tileEntity.structure.steamStored, tileEntity.structure.getHeatedCoolantCapacity());
			}
		}, Type.STANDARD, this, background, 152, 13));
		gasGauges = Arrays.<GuiElement>asList(new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.sodiumTank;
			}
		}, Type.STANDARD, this, background, 6, 13), new GuiGasGauge(new IGasInfoHandler()
		{
			@Override
			public GasTank getTank()
			{
				return tileEntity.structure.superheatedSodiumTank;
			}
		}, Type.STANDARD, this, background, 152, 13));
		guiElements.addAll(fluidGauges);
		guiElements.add(new GuiHeatInfo(new IInfoHandler()
		{
			@Override
			public List<String> getInfo()
			{
				TemperatureUnit unit = TemperatureUnit.values()[general.tempUnit.ordinal()];
				String environment = UnitDisplayUtils.getDisplayShort(tileEntity.structure.lastEnvironmentLoss*unit.intervalSize, false, unit);
				return ListUtils.asList(LangUtils.localize("gui.dissipated") + ": " + environment + "/t");
			}
		}, this, background));
		guiElements.add(heatGraph = new GuiGraph(this, background, 5, 123, xSize-10, 38, new GraphDataHandler()
		{
			@Override
			public String getDataDisplay(int data)
			{
				return LangUtils.localize("gui.fission.temp") + ": " + data;
			}
		}));
		heatGraph.currentScale = 1600;
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();

		boolean sodium = tileEntity.structure.sodiumTank.getStored() > 0 || tileEntity.structure.superheatedSodiumTank.getStored() > 0;

		if(sodium != gasCoolant)
		{
			gasCoolant = sodium;
			guiElements.removeAll(gasCoolant ? fluidGauges : gasGauges);
			guiElements.addAll(gasCoolant ? gasGauges : fluidGauges);
		}

		heatGraph.addData((int)tileEntity.structure.temperature);
	}

	private boolean canActivate()
	{
		return !tileEntity.structure.active && !tileEntity.structure.forceDisable;
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button)
	{
		super.mouseClicked(mouseX, mouseY, button);

		int xAxis = mouseX - guiLeft;
		int yAxis = mouseY - guiTop;

		if(button == 0 && ((canActivate() && isOver(xAxis, yAxis, ACTIVATE_X, BUTTON_Y, BUTTON_W, BUTTON_H)) ||
				(tileEntity.structure.active && isOver(xAxis, yAxis, SCRAM_X, BUTTON_Y, BUTTON_W, BUTTON_H))))
		{
			ArrayList data = new ArrayList();
			data.add((byte)0);

			Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
			SoundHandler.playSound("gui.button.press");
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		String name = tileEntity.getInventoryName();
		fontRendererObj.drawString(name, (xSize/2)-(fontRendererObj.getStringWidth(name)/2), 5, TEXT_COLOR);

		SynchronizedFissionData structure = tileEntity.structure;

		String status = structure.forceDisable ? "gui.fission.scram" : (structure.active ? "gui.fission.active" : "gui.fission.inactive");
		String[] lines = {
				LangUtils.localize("gui.fission.status") + ": " + LangUtils.localize(status),
				LangUtils.localize("gui.fission.burnRate") + ": " + Math.round(structure.lastBurnRate*100)/100D + " mB/t",
				LangUtils.localize("gui.fission.heatingRate") + ": " + String.format("%,d", structure.lastBoilRate) + " mB/t",
				LangUtils.localize("gui.fission.temp") + ": " + MekanismUtils.getTemperatureDisplay(structure.temperature, TemperatureUnit.KELVIN),
				LangUtils.localize("gui.fission.damage") + ": " + Math.min(100, Math.round(structure.damage/SynchronizedFissionData.MAX_DAMAGE*1000)/10D) + "%"
		};

		for(int i = 0; i < lines.length; i++)
		{
			renderScaledText(lines[i], 50, 21+i*10, SCREEN_TEXT_COLOR, 95);
		}

		drawButtonText("gui.fission.activate", ACTIVATE_X, canActivate());
		drawButtonText("gui.fission.deactivate", SCRAM_X, structure.active);

		fontRendererObj.drawString(LangUtils.localize("gui.fission.temperature") + ":", 6, 93, TEXT_COLOR);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.heatGraph") + ":", 6, 114, TEXT_COLOR);

		super.drawGuiContainerForegroundLayer(mouseX, mouseY);

		int xAxis = mouseX - guiLeft;
		int yAxis = mouseY - guiTop;

		if(structure.forceDisable && !structure.active && isOver(xAxis, yAxis, ACTIVATE_X, BUTTON_Y, BUTTON_W, BUTTON_H))
		{
			displayTooltip(LangUtils.localize("gui.fission.forceDisabled"), xAxis, yAxis);
		}
		else if(isOver(xAxis, yAxis, 5, 102, xSize-10, 8))
		{
			displayTooltip(MekanismUtils.getTemperatureDisplay(structure.temperature, TemperatureUnit.KELVIN), xAxis, yAxis);
		}
	}

	private void drawButtonText(String key, int x, boolean enabled)
	{
		String text = LangUtils.localize(key);

		int textX = x+(BUTTON_W-fontRendererObj.getStringWidth(text))/2;

		fontRendererObj.drawStringWithShadow(text, textX, BUTTON_Y+4, enabled ? 0xFFFFFF : 0x9E9E9E);
	}

	/** Upstream style: black border, lighter top-left and darker bottom-right bevel around a dark fill */
	private void drawButton(int x, int y, boolean enabled, boolean hover, int fill, int light, int dark)
	{
		drawBox(x, y, BUTTON_W, BUTTON_H, 0xFF000000);
		drawBox(x+1, y+1, BUTTON_W-2, BUTTON_H-2, dark);
		drawBox(x+1, y+1, BUTTON_W-3, BUTTON_H-3, light);
		drawBox(x+2, y+2, BUTTON_W-4, BUTTON_H-4, enabled && hover ? light : fill);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
	{
		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;
		int xAxis = mouseX - guiWidth;
		int yAxis = mouseY - guiHeight;

		SynchronizedFissionData structure = tileEntity.structure;

		drawPanel(guiWidth, guiHeight, xSize, ySize);
		drawInnerScreen(guiWidth+45, guiHeight+17, 105, 56);

		drawButton(guiWidth+ACTIVATE_X, guiHeight+BUTTON_Y, canActivate(), isOver(xAxis, yAxis, ACTIVATE_X, BUTTON_Y, BUTTON_W, BUTTON_H), 0xFF0A160B, 0xFF0F2110, 0xFF071008);
		drawButton(guiWidth+SCRAM_X, guiHeight+BUTTON_Y, structure.active, isOver(xAxis, yAxis, SCRAM_X, BUTTON_Y, BUTTON_W, BUTTON_H), 0xFF2E0207, 0xFF45020B, 0xFF220105);
		drawBox(guiWidth+173, guiHeight+76, 14, 14, structure.active ? 0xFF2EAA5A : 0xFF173A24);

		drawBar(guiWidth+5, guiHeight+102, xSize-10, 8, Math.min(1, structure.temperature/SynchronizedFissionData.MAX_DAMAGE_TEMPERATURE));
		drawBox(guiWidth+4, guiHeight+122, xSize-8, 40, 0xFF000000);

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
	}
}
