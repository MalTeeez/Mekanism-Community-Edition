package mekanism.gametest.machines;

import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.common.MekanismBlocks;
import mekanism.common.tile.TileEntitySolarNeutronActivator;
import mekanism.gametest.GameTestUtil;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class SolarNeutronActivatorTests
{
	/**
	 * Time and weather are global and other tests run at the same time, so they are only changed for the duration of this synchronous call.
	 * Nothing else can run in between, and the sky light level is recomputed because isDaytime reads it instead of the time.
	 */
	private static void withSky(GameTestHelper helper, long time, Runnable action)
	{
		World world = helper.getWorld();
		long oldTime = world.getWorldTime();
		int oldSky = world.skylightSubtracted;
		float rain = world.rainingStrength, prevRain = world.prevRainingStrength;
		float thunder = world.thunderingStrength, prevThunder = world.prevThunderingStrength;

		try {
			world.setWorldTime(time);
			world.rainingStrength = world.prevRainingStrength = 0;
			world.thunderingStrength = world.prevThunderingStrength = 0;
			world.skylightSubtracted = world.calculateSkylightSubtracted(1F);

			action.run();
		} finally {
			world.setWorldTime(oldTime);
			world.skylightSubtracted = oldSky;
			world.rainingStrength = rain;
			world.prevRainingStrength = prevRain;
			world.thunderingStrength = thunder;
			world.prevThunderingStrength = prevThunder;
		}
	}

	private static TileEntitySolarNeutronActivator place(GameTestHelper helper)
	{
		//In a normal world there is terrain, trees and so on above the cell, so open the whole column to the sky.
		//Top-down, because the heightmap only drops when the removed block is the current top one
		World world = helper.getWorld();

		for(int y = world.getHeight()-1; y >= helper.getOriginY()+2; y--)
		{
			world.setBlockToAir(helper.getOriginX()+1, y, helper.getOriginZ()+1);
		}

		helper.setBlock(1, 1, 1, MekanismBlocks.MachineBlock3, 1);
		GameTestUtil.clearOnFinish(helper, 3, 3, 3);

		TileEntitySolarNeutronActivator tile = GameTestUtil.tile(helper, TileEntitySolarNeutronActivator.class, 1, 1, 1);
		tile.facing = 3;
		tile.inputTank.receive(new GasStack(GasRegistry.getGas("nuclearWaste"), 10), true);

		return tile;
	}

	private static void run(TileEntitySolarNeutronActivator tile, int ticks)
	{
		for(int i = 0; i < ticks; i++)
		{
			tile.onUpdate();
		}
	}

	@GameTest(timeoutTicks = 60)
	public static void convertsNuclearWasteToPoloniumInDaylight(GameTestHelper helper)
	{
		TileEntitySolarNeutronActivator tile = place(helper);

		withSky(helper, 6000, () -> {
			run(tile, 30);

			GasStack out = tile.outputTank.getGas();
			String state = "daytime=" + helper.getWorld().isDaytime() + ", raining=" + helper.getWorld().isRaining() + ", sky=" + helper.getWorld().canBlockSeeTheSky(tile.xCoord, tile.yCoord+1, tile.zCoord) + ", input=" + tile.inputTank.getStored();

			helper.assertTrue(out != null && out.getGas() == GasRegistry.getGas("polonium"), "Output should be polonium, got " + out + " (" + state + ")");
			helper.assertEquals(1, out.amount, "10 nuclear waste -> 1 polonium");
		});

		helper.succeed();
	}

	@GameTest(timeoutTicks = 30)
	public static void idlesAtNight(GameTestHelper helper)
	{
		TileEntitySolarNeutronActivator tile = place(helper);

		withSky(helper, 18000, () -> {
			run(tile, 30);
			helper.assertEquals(10, tile.inputTank.getStored(), "Activator processed waste at night");
		});

		helper.succeed();
	}
}
