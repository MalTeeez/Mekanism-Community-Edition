package mekanism.gametest.machines;

import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.common.MekanismBlocks;
import mekanism.common.tile.TileEntitySolarNeutronActivator;
import mekanism.gametest.GameTestUtil;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.Weather;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class SolarNeutronActivatorTests
{
	//World time and weather are global, so the day and night tests need their own batches to not run side by side
	private static TileEntitySolarNeutronActivator place(GameTestHelper helper, long time, Weather weather)
	{
		//The cell pre-clear is only one block tall, so drop anything left above the machine from earlier runs
		for(int y = 2; y < 10; y++)
		{
			helper.destroyBlock(1, y, 1);
		}

		helper.fixWorldTime(time);
		helper.setWeather(weather);
		helper.setBlock(1, 1, 1, MekanismBlocks.MachineBlock3, 1);
		GameTestUtil.clearOnFinish(helper, 3, 3, 3);

		TileEntitySolarNeutronActivator tile = GameTestUtil.tile(helper, TileEntitySolarNeutronActivator.class, 1, 1, 1);
		tile.facing = 3;
		tile.inputTank.receive(new GasStack(GasRegistry.getGas("nuclearWaste"), 10), true);

		return tile;
	}

	@GameTest(timeoutTicks = 60, batch = "daylight")
	public static void convertsNuclearWasteToPoloniumInDaylight(GameTestHelper helper)
	{
		TileEntitySolarNeutronActivator tile = place(helper, 6000, Weather.CLEAR);

		helper.startSequence()
			.thenIdle(30)
			.thenExecute(() -> {
				GasStack out = tile.outputTank.getGas();
				String state = "daytime=" + helper.getWorld().isDaytime() + ", raining=" + helper.getWorld().isRaining() + ", sky=" + helper.getWorld().canBlockSeeTheSky(tile.xCoord, tile.yCoord+1, tile.zCoord) + ", input=" + tile.inputTank.getStored();

				helper.assertTrue(out != null && out.getGas() == GasRegistry.getGas("polonium"), "Output should be polonium, got " + out + " (" + state + ")");
				helper.assertEquals(1, out.amount, "10 nuclear waste -> 1 polonium");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30, batch = "night")
	public static void idlesAtNight(GameTestHelper helper)
	{
		TileEntitySolarNeutronActivator tile = place(helper, 18000, Weather.CLEAR);

		helper.onEachTick(() -> helper.assertEquals(10, tile.inputTank.getStored(), "Activator processed waste at night"));
		helper.succeedAtTimeout();
	}
}
