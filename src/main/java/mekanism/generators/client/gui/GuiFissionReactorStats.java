package mekanism.generators.client.gui;

import java.util.ArrayList;
import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.MekanismConfig.general;
import mekanism.api.util.UnitDisplayUtils;
import mekanism.api.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.client.gui.element.GuiElement.IInfoHandler;
import mekanism.client.gui.element.GuiHeatInfo;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.api.util.ListUtils;
import mekanism.generators.client.gui.element.GuiTurbineTab;
import mekanism.generators.client.gui.element.GuiTurbineTab.TurbineTab;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiFissionReactorStats extends GuiFissionReactorBase
{
	private static final int FIELD_X = 77;
	private static final int FIELD_Y = 128;
	private static final int FIELD_W = 54;
	private static final int FIELD_H = 12;
	private static final int CHECK_SIZE = 10;

	public GuiTextField rateField;

	public GuiFissionReactorStats(InventoryPlayer inventory, TileEntityFissionCasing tentity)
	{
		super(inventory, tentity);

		guiElements.add(new GuiTurbineTab(this, tileEntity, TurbineTab.FISSION_MAIN, 6, background));
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
	}

	@Override
	public void initGui()
	{
		super.initGui();

		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;

		String prev = rateField != null ? rateField.getText() : "";

		rateField = new GuiTextField(fontRendererObj, guiWidth + FIELD_X, guiHeight + FIELD_Y, FIELD_W, FIELD_H);
		rateField.setMaxStringLength(8);
		rateField.setText(prev);
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();

		rateField.updateCursorCounter();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		SynchronizedFissionData structure = tileEntity.structure;
		String stats = LangUtils.localize("gui.fissionStats");

		fontRendererObj.drawString(stats, (xSize/2)-(fontRendererObj.getStringWidth(stats)/2), 6, TEXT_COLOR);

		int w = xSize-12;

		renderScaledText(LangUtils.localize("gui.fission.heatStats"), 6, 20, HEADING_COLOR, w);
		renderScaledText(LangUtils.localize("gui.fission.heatCapacity") + ": " + String.format("%,d", (long)structure.heatCapacity) + " J/K", 6, 32, TEXT_COLOR, w);
		renderScaledText(LangUtils.localize("gui.fission.fuelSurfaceArea") + ": " + String.format("%,d", structure.surfaceArea) + " m2", 6, 42, TEXT_COLOR, w);
		renderScaledText(LangUtils.localize("gui.fission.boilEfficiency") + ": " + Math.round(structure.getBoilEfficiency()*100)/100D, 6, 52, TEXT_COLOR, w);

		renderScaledText(LangUtils.localize("gui.fission.fuelStats"), 6, 68, HEADING_COLOR, w);
		renderScaledText(LangUtils.localize("gui.fission.maxBurnRate") + ": " + structure.getMaxBurnRate() + " mB/t", 6, 80, TEXT_COLOR, w);
		renderScaledText(LangUtils.localize("gui.fission.rateLimit") + ": " + Math.round(structure.rateLimit*100)/100D + " mB/t", 6, 90, TEXT_COLOR, w);

		renderScaledText(LangUtils.localize("gui.fission.currentBurnRate") + ":", 6, 104, TEXT_COLOR, w);
		renderScaledText(LangUtils.localize("gui.fission.setRateLimit") + ":", 6, 130, TEXT_COLOR, 69);

		super.drawGuiContainerForegroundLayer(mouseX, mouseY);

		int xAxis = mouseX - guiLeft;
		int yAxis = mouseY - guiTop;

		if(isOver(xAxis, yAxis, 5, 114, w+2, 8))
		{
			displayTooltip(Math.round(structure.lastBurnRate*100)/100D + " mB/t", xAxis, yAxis);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
	{
		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;

		SynchronizedFissionData structure = tileEntity.structure;

		drawPanel(guiWidth, guiHeight, xSize, ySize);
		drawBar(guiWidth+5, guiHeight+114, xSize-10, 8, Math.min(1, structure.lastBurnRate/structure.getMaxBurnRate()));

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);

		rateField.drawTextBox();
		drawCheckmark(rateField.xPosition+FIELD_W-CHECK_SIZE-1, rateField.yPosition+1, isOverCheckmark(mouseX-guiLeft, mouseY-guiTop));
	}

	private boolean isOverCheckmark(int xAxis, int yAxis)
	{
		return isOver(xAxis, yAxis, FIELD_X+FIELD_W-CHECK_SIZE-1, FIELD_Y+1, CHECK_SIZE, CHECK_SIZE);
	}

	private void drawCheckmark(int x, int y, boolean hover)
	{
		int color = hover ? 0xFFFFFFFF : 0xFF5C5C5C;
		int[][] pixels = {{1, 5}, {2, 6}, {3, 7}, {4, 6}, {5, 5}, {6, 4}, {7, 3}, {8, 2}};

		for(int[] p : pixels)
		{
			drawRect(x+p[0], y+p[1], x+p[0]+1, y+p[1]+2, color);
		}

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button)
	{
		super.mouseClicked(mouseX, mouseY, button);

		rateField.mouseClicked(mouseX, mouseY, button);

		if(button == 0 && isOverCheckmark(mouseX-guiLeft, mouseY-guiTop))
		{
			SoundHandler.playSound("gui.button.press");
			setRate();
		}
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
		try {
			double rate = Double.parseDouble(rateField.getText());

			if(rate >= 0 && rate <= tileEntity.structure.getMaxBurnRate())
			{
				ArrayList data = new ArrayList();
				data.add((byte)1);
				data.add(rate);

				Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));
				rateField.setText("");
			}
		} catch(NumberFormatException e) {}
	}
}
