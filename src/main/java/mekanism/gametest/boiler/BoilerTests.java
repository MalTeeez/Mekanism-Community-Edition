package mekanism.gametest.boiler;

import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.common.MekanismBlocks;
import mekanism.common.content.boiler.SynchronizedBoilerData;
import mekanism.common.tile.TileEntityBoilerCasing;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.TileEntityGasTank;
import mekanism.common.tile.TileEntityBoilerValve;
import mekanism.common.tile.TileEntityBoilerValve.BoilerValveMode;
import mekanism.gametest.GameTestUtil;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class BoilerTests
{
	private static final int CASING = 7, VALVE = 8, ELEMENT = 5, DISPERSER = 6;

	/**
	 * Builds the smallest valid boiler, a 3x3x5 box with one interior column: air (water), superheating element,
	 * pressure disperser. Valves sit in the front face at (1,1,0) and (1,3,0).
	 * @return the casing in the (0,0,0) corner
	 */
	private static TileEntityBoilerCasing build(GameTestHelper helper)
	{
		for(int x = 0; x < 3; x++)
		{
			for(int y = 0; y < 5; y++)
			{
				for(int z = 0; z < 3; z++)
				{
					if(GameTestUtil.onShell(x, y, z, 3, 5, 3))
					{
						helper.setBlock(x, y, z, MekanismBlocks.BasicBlock2, z == 0 && x == 1 && (y == 1 || y == 3) ? VALVE : CASING);
					}
				}
			}
		}

		helper.setBlock(1, 2, 1, MekanismBlocks.BasicBlock2, ELEMENT);
		helper.setBlock(1, 3, 1, MekanismBlocks.BasicBlock2, DISPERSER);
		GameTestUtil.clearOnFinish(helper, 3, 5, 3);

		TileEntityBoilerCasing casing = GameTestUtil.tile(helper, TileEntityBoilerCasing.class, 0, 0, 0);
		casing.update();

		return casing;
	}

	@GameTest(timeoutTicks = 20)
	public static void smallestBoilerForms(GameTestHelper helper)
	{
		TileEntityBoilerCasing casing = build(helper);

		helper.assertNotNull(casing.structure, "A 3x3x5 boiler with one element and a disperser plane should form");
		helper.assertEquals(1, casing.structure.superheatingElements, "Superheating element count");
		helper.assertSame(casing.structure, GameTestUtil.tile(helper, TileEntityBoilerValve.class, 1, 1, 0).structure, "Valve should share the boiler structure");
		helper.succeed();
	}

	@GameTest(timeoutTicks = 60)
	public static void superheatedSodiumBoilsWaterAndReturnsCooledSodium(GameTestHelper helper)
	{
		TileEntityBoilerCasing casing = build(helper);
		TileEntityBoilerValve input = GameTestUtil.tile(helper, TileEntityBoilerValve.class, 1, 1, 0);
		TileEntityBoilerValve output = GameTestUtil.tile(helper, TileEntityBoilerValve.class, 1, 3, 0);
		SynchronizedBoilerData data = casing.structure;

		output.mode = BoilerValveMode.OUTPUT_COOLANT;

		helper.assertEquals(1000, input.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 1000), true), "INPUT valve should accept 1000 mB water");
		helper.assertEquals(100000, input.receiveGas(ForgeDirection.UNKNOWN, new GasStack(GasRegistry.getGas("superheatedSodium"), 100000), true), "INPUT valve should accept superheated sodium");

		helper.startSequence()
			.thenWaitUntil(40, () -> helper.assertTrue(data.steamStored != null && data.steamStored.amount > 0, "Superheated sodium should boil the water into steam (temperature " + data.temperature + ", water " + data.waterStored + ", sodium " + data.superheatedSodiumTank.getStored() + ")"))
			.thenExecute(() -> {
				helper.assertTrue(data.cooledSodiumTank.getStored() > 0, "Cooled sodium should be produced, tank holds " + data.cooledSodiumTank.getStored());
				helper.assertTrue(data.superheatedSodiumTank.getStored() < 100000, "Superheated sodium should be consumed, tank holds " + data.superheatedSodiumTank.getStored());
				helper.assertTrue(data.waterStored == null || data.waterStored.amount < 1000, "Water should be consumed, found " + data.waterStored);
				helper.assertNull(input.drawGas(ForgeDirection.UNKNOWN, 50, true), "INPUT valve must not hand out sodium");
				helper.assertEquals(0, output.receiveGas(ForgeDirection.UNKNOWN, new GasStack(GasRegistry.getGas("superheatedSodium"), 10), true), "OUTPUT_COOLANT valve must not accept superheated sodium");

				int stored = data.cooledSodiumTank.getStored();
				GasStack drawn = output.drawGas(ForgeDirection.UNKNOWN, 50, true);

				helper.assertTrue(drawn != null && drawn.getGas() == GasRegistry.getGas("sodium") && drawn.amount == Math.min(50, stored), "OUTPUT_COOLANT valve should draw sodium, got " + drawn);
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void steamValvePushesSteamIntoAdjacentFluidTank(GameTestHelper helper)
	{
		TileEntityBoilerCasing casing = build(helper);
		TileEntityFluidTank tank = GameTestUtil.fluidTank(helper, 1, 3, -1);

		casing.structure.steamStored = new FluidStack(FluidRegistry.getFluid("steam"), 500);
		GameTestUtil.tile(helper, TileEntityBoilerValve.class, 1, 3, 0).mode = BoilerValveMode.OUTPUT_STEAM;

		helper.startSequence()
			.thenWaitUntil(20, () -> helper.assertTrue(tank.fluidTank.getFluidAmount() > 0, "OUTPUT_STEAM valve should push steam into the fluid tank"))
			.thenExecute(() -> helper.assertTrue(tank.fluidTank.getFluid().getFluid() == FluidRegistry.getFluid("steam"), "Fluid tank should hold steam, got " + tank.fluidTank.getFluid()))
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void coolantValvePushesCooledSodiumIntoAdjacentGasTank(GameTestHelper helper)
	{
		TileEntityBoilerCasing casing = build(helper);
		TileEntityGasTank tank = GameTestUtil.gasTank(helper, 1, 3, -1);

		casing.structure.cooledSodiumTank.stored = new GasStack(GasRegistry.getGas("sodium"), 500);
		GameTestUtil.tile(helper, TileEntityBoilerValve.class, 1, 3, 0).mode = BoilerValveMode.OUTPUT_COOLANT;

		helper.startSequence()
			.thenWaitUntil(20, () -> helper.assertTrue(tank.gasTank.getStored() > 0, "OUTPUT_COOLANT valve should push cooled sodium into the gas tank"))
			.thenExecute(() -> helper.assertTrue(tank.gasTank.getGasType() == GasRegistry.getGas("sodium"), "Gas tank should hold sodium, got " + tank.gasTank.getGas()))
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void inputValvePushesNoGas(GameTestHelper helper)
	{
		TileEntityBoilerCasing casing = build(helper);
		TileEntityGasTank tank = GameTestUtil.gasTank(helper, 1, 1, -1);

		casing.structure.cooledSodiumTank.stored = new GasStack(GasRegistry.getGas("sodium"), 500);

		helper.startSequence()
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertEquals(0, tank.gasTank.getStored(), "INPUT valve must not push gas, tank holds " + tank.gasTank.getGas());
				helper.assertEquals(500, casing.structure.cooledSodiumTank.getStored(), "Cooled sodium must stay in the boiler");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void inputValvePushesNoSteam(GameTestHelper helper)
	{
		TileEntityBoilerCasing casing = build(helper);
		TileEntityFluidTank tank = GameTestUtil.fluidTank(helper, 1, 1, -1);

		casing.structure.steamStored = new FluidStack(FluidRegistry.getFluid("steam"), 500);

		helper.startSequence()
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertEquals(0, tank.fluidTank.getFluidAmount(), "INPUT valve must not push steam, tank holds " + tank.fluidTank.getFluid());
				helper.assertEquals(500, casing.structure.steamStored.amount, "Steam must stay in the boiler");
			})
			.thenSucceed();
	}
}
