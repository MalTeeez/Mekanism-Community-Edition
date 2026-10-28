package mekanism.generators.client.gui;

import mekanism.client.gui.GuiMekanism;
import mekanism.common.inventory.container.ContainerNull;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Shared drawing for the fission reactor GUIs, modelled on modern Mekanism's dark inner screens and bars. */
@SideOnly(Side.CLIENT)
public abstract class GuiFissionReactorBase extends GuiMekanism
{
	public static final int TEXT_COLOR = 0x404040;
	public static final int HEADING_COLOR = 0x202020;
	public static final int SCREEN_TEXT_COLOR = 0x3CFD87;

	private static final int BOX_BORDER = 0xFF373737;
	private static final int BOX_FILL = 0xFF141414;

	protected ResourceLocation background = MekanismUtils.getResource(ResourceType.GUI, "GuiNull.png");

	public TileEntityFissionCasing tileEntity;

	public GuiFissionReactorBase(InventoryPlayer inventory, TileEntityFissionCasing tentity)
	{
		super(tentity, new ContainerNull(inventory.player, tentity));
		tileEntity = tentity;
	}

	protected void drawPanel(int x, int y, int w, int h)
	{
		mc.renderEngine.bindTexture(background);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		drawTexturedModalRect(x, y, 0, 0, 4, 4);
		drawTexturedModalRect(x+w-4, y, 172, 0, 4, 4);
		drawTexturedModalRect(x, y+h-4, 0, 162, 4, 4);
		drawTexturedModalRect(x+w-4, y+h-4, 172, 162, 4, 4);

		for(int dx = 4; dx < w-4; dx += 160)
		{
			int tw = Math.min(160, w-4-dx);

			drawTexturedModalRect(x+dx, y, 4, 0, tw, 4);
			drawTexturedModalRect(x+dx, y+h-4, 4, 162, tw, 4);

			for(int dy = 4; dy < h-4; dy += 158)
			{
				drawTexturedModalRect(x+dx, y+dy, 4, 4, tw, Math.min(158, h-4-dy));
			}
		}

		for(int dy = 4; dy < h-4; dy += 158)
		{
			int th = Math.min(158, h-4-dy);

			drawTexturedModalRect(x, y+dy, 0, 4, 4, th);
			drawTexturedModalRect(x+w-4, y+dy, 172, 4, 4, th);
		}
	}

	protected void drawBox(int x, int y, int w, int h, int fill)
	{
		drawRect(x, y, x+w, y+h, BOX_BORDER);
		drawRect(x+1, y+1, x+w-1, y+h-1, fill);
	}

	protected void drawInnerScreen(int x, int y, int w, int h)
	{
		drawBox(x, y, w, h, BOX_FILL);
	}

	/** Horizontal bar whose fill shifts from green over yellow to red along its length. */
	protected void drawBar(int x, int y, int w, int h, double level)
	{
		drawBox(x, y, w, h, BOX_FILL);

		int fill = (int)Math.round(Math.max(0, Math.min(1, level))*(w-2));

		for(int i = 0; i < fill; i++)
		{
			float f = (float)i/(w-2);
			int r = (int)(f < 0.5F ? 0x2D+(0x7A-0x2D)*f*2 : 0x7A+(0x8A-0x7A)*(f-0.5F)*2);
			int g = (int)(f < 0.5F ? 0x7A : 0x7A*(1-(f-0.5F)*2));

			drawRect(x+1+i, y+1, x+2+i, y+h-1, 0xFF000000 | r << 16 | g << 8);
		}
	}

	protected boolean isOver(int xAxis, int yAxis, int x, int y, int w, int h)
	{
		return xAxis >= x && xAxis < x+w && yAxis >= y && yAxis < y+h;
	}
}
