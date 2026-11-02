package mekanism.client.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.guihook.IContainerInputHandler;
import mekanism.client.gui.GuiMekanism;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.awt.Point;

/** Recipe/usage key over a gas bar in a machine GUI opens NEI for that gas. */
public class GasGaugeInputHandler implements IContainerInputHandler
{
	@Override
	public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode)
	{
		boolean recipe = keyCode == NEIClientConfig.getKeyBinding("gui.recipe");

		if(!(gui instanceof GuiMekanism) || (!recipe && keyCode != NEIClientConfig.getKeyBinding("gui.usage")))
		{
			return false;
		}

		GuiMekanism guiMek = (GuiMekanism)gui;
		Point mouse = GuiDraw.getMousePosition();

		return BaseRecipeHandler.doGasLookup(guiMek.getGasAt(mouse.x - guiMek.getXPos(), mouse.y - guiMek.getYPos()), !recipe);
	}

	@Override
	public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) { return false; }

	@Override
	public void onKeyTyped(GuiContainer gui, char keyChar, int keyCode) {}

	@Override
	public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) { return false; }

	@Override
	public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

	@Override
	public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}

	@Override
	public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) { return false; }

	@Override
	public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

	@Override
	public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {}
}
