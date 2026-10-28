package mekanism.generators.client.gui;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.generators.common.tile.fission.TileEntityFissionLogicAdapter;
import mekanism.generators.common.tile.fission.TileEntityFissionLogicAdapter.FissionLogic;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiFissionReactorLogicAdapter extends GuiMekanism
{
	private static final int DISPLAY_COUNT = 4;

	public TileEntityFissionLogicAdapter tileEntity;

	private int scroll;

	public GuiFissionReactorLogicAdapter(InventoryPlayer inventory, final TileEntityFissionLogicAdapter tentity)
	{
		super(new ContainerNull(inventory.player, tentity));

		tileEntity = tentity;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);

		fontRendererObj.drawString(tileEntity.getInventoryName(), (xSize/2)-(fontRendererObj.getStringWidth(tileEntity.getInventoryName())/2), 6, 0x404040);
		renderScaledText(LangUtils.localize("gui.redstoneOutputMode") + ": " + EnumColor.RED + tileEntity.logicType.getLocalizedName(), 23, 123, 0x404040, 130);

		String status = tileEntity.logicType == FissionLogic.ACTIVATION && tileEntity.isPowered() ? "powered" : (tileEntity.checkMode() ? "outputting" : "idle");
		String text = LangUtils.localize("gui.status") + ": " + EnumColor.RED + LangUtils.localize("gui." + status);
		fontRendererObj.drawString(text, (xSize/2)-(fontRendererObj.getStringWidth(text)/2), 136, 0x404040);

		for(int i = 0; i < DISPLAY_COUNT; i++)
		{
			FissionLogic type = FissionLogic.values()[scroll+i];

			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_LIGHTING);
			itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), type.getRenderStack(), 27, 35 + (22*i));
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glPopMatrix();

			fontRendererObj.drawString(EnumColor.WHITE + type.getLocalizedName(), 46, 34+(22*i), 0x404040);

			if(xAxis >= 24 && xAxis <= 152 && yAxis >= 32+(22*i) && yAxis <= 32+22+(22*i))
			{
				displayTooltips(MekanismUtils.splitTooltip(type.getDescription(), null), xAxis, yAxis);
			}
		}

		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
	{
		mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "GuiReactorLogicAdapter.png"));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;
		drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);

		for(int i = 0; i < DISPLAY_COUNT; i++)
		{
			MekanismRenderer.color(EnumColor.RED);

			drawTexturedModalRect(guiWidth + 24, guiHeight + 32+(22*i), 0, 166+(FissionLogic.values()[scroll+i] == tileEntity.logicType ? 22 : 0), 128, 22);

			MekanismRenderer.resetColor();
		}

		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
	}

	@Override
	public void handleMouseInput()
	{
		super.handleMouseInput();

		int wheel = Mouse.getEventDWheel();

		if(wheel != 0)
		{
			scroll = Math.max(0, Math.min(scroll + (wheel > 0 ? -1 : 1), FissionLogic.values().length-DISPLAY_COUNT));
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button)
	{
		super.mouseClicked(mouseX, mouseY, button);

		if(button == 0)
		{
			int xAxis = (mouseX - (width - xSize) / 2);
			int yAxis = (mouseY - (height - ySize) / 2);

			for(int i = 0; i < DISPLAY_COUNT; i++)
			{
				FissionLogic type = FissionLogic.values()[scroll+i];

				if(type != tileEntity.logicType && xAxis >= 24 && xAxis <= 152 && yAxis >= 32+(22*i) && yAxis <= 32+22+(22*i))
				{
					SoundHandler.playSound("gui.button.press");

					ArrayList data = new ArrayList();
					data.add((byte)10);
					data.add(type.ordinal());

					Mekanism.packetHandler.sendToServer(new TileEntityMessage(Coord4D.get(tileEntity), data));

					return;
				}
			}
		}
	}
}
