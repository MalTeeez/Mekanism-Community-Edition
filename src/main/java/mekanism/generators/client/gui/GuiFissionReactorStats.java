package mekanism.generators.client.gui;

import mekanism.client.gui.GuiMekanism;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.generators.client.gui.element.GuiTurbineTab;
import mekanism.generators.client.gui.element.GuiTurbineTab.TurbineTab;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiFissionReactorStats extends GuiMekanism
{
	public TileEntityFissionCasing tileEntity;

	public GuiFissionReactorStats(InventoryPlayer inventory, TileEntityFissionCasing tentity)
	{
		super(tentity, new ContainerNull(inventory.player, tentity));
		tileEntity = tentity;
		guiElements.add(new GuiTurbineTab(this, tileEntity, TurbineTab.FISSION_MAIN, 6, MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png")));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		SynchronizedFissionData structure = tileEntity.structure;
		String stats = LangUtils.localize("gui.fissionStats");

		fontRendererObj.drawString(stats, (xSize/2)-(fontRendererObj.getStringWidth(stats)/2), 6, 0x404040);

		fontRendererObj.drawString(LangUtils.localize("gui.fission.assemblies") + ": " + structure.assemblies, 8, 22, 0x404040);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.surfaceArea") + ": " + structure.surfaceArea, 8, 33, 0x404040);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.boilEfficiency") + ": " + Math.round(structure.getBoilEfficiency()*1000)/10D + "%", 8, 44, 0x404040);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.heatCapacity") + ": " + Math.round(structure.heatCapacity*100)/100D, 8, 55, 0x404040);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.maxBurnRate") + ": " + structure.getMaxBurnRate() + " mB/t", 8, 66, 0x404040);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.coolantCapacity") + ": " + structure.getCoolantCapacity() + " mB", 8, 77, 0x404040);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.boilRate") + ": " + structure.lastBoilRate + " mB/t", 8, 88, 0x404040);
		fontRendererObj.drawString(LangUtils.localize("gui.fission.environmentLoss") + ": " + Math.round(structure.lastEnvironmentLoss*100)/100D, 8, 99, 0x404040);

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
	}
}
