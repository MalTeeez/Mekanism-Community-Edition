package mekanism.gametest.fission;

import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.TileEntityGasTank;
import mekanism.gametest.GameTestUtil;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import mekanism.generators.common.tile.fission.TileEntityFissionPort.PortMode;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class FissionOutputTests
{
	/** The port sits at (1,1,0), so (1,1,-1) is its only neighbour outside the reactor. */
	private static final int NX = 1, NY = 1, NZ = -1;

	@GameTest(timeoutTicks = 30)
	public static void wastePortPushesWasteIntoAdjacentGasTank(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		TileEntityGasTank tank = GameTestUtil.gasTank(helper, NX, NY, NZ);

		casing.structure.wasteTank.stored = new GasStack(GasRegistry.getGas("nuclearWaste"), 500);
		FissionTestUtil.port(helper).mode = PortMode.OUTPUT_WASTE;

		helper.startSequence()
			.thenWaitUntil(20, () -> helper.assertTrue(tank.gasTank.getStored() > 0, "OUTPUT_WASTE port should push waste into the gas tank"))
			.thenExecute(() -> {
				helper.assertTrue(tank.gasTank.getGasType() == GasRegistry.getGas("nuclearWaste"), "Gas tank should hold nuclearWaste, got " + tank.gasTank.getGas());
				helper.assertEquals(500, tank.gasTank.getStored()+casing.structure.wasteTank.getStored(), "Waste should only move, not appear or vanish");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void coolantPortPushesSteamIntoAdjacentFluidTank(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		TileEntityFluidTank tank = GameTestUtil.fluidTank(helper, NX, NY, NZ);

		casing.structure.steamStored = new FluidStack(FluidRegistry.getFluid("steam"), 500);
		FissionTestUtil.port(helper).mode = PortMode.OUTPUT_COOLANT;

		helper.startSequence()
			.thenWaitUntil(20, () -> helper.assertTrue(tank.fluidTank.getFluidAmount() > 0, "OUTPUT_COOLANT port should push steam into the fluid tank"))
			.thenExecute(() -> {
				helper.assertTrue(tank.fluidTank.getFluid().getFluid() == FluidRegistry.getFluid("steam"), "Fluid tank should hold steam, got " + tank.fluidTank.getFluid());
				helper.assertEquals(500, tank.fluidTank.getFluidAmount()+(casing.structure.steamStored != null ? casing.structure.steamStored.amount : 0), "Steam should only move, not appear or vanish");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void coolantPortPushesSuperheatedSodiumIntoAdjacentGasTank(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		TileEntityGasTank tank = GameTestUtil.gasTank(helper, NX, NY, NZ);

		casing.structure.superheatedSodiumTank.stored = new GasStack(GasRegistry.getGas("superheatedSodium"), 500);
		FissionTestUtil.port(helper).mode = PortMode.OUTPUT_COOLANT;

		helper.startSequence()
			.thenWaitUntil(20, () -> helper.assertTrue(tank.gasTank.getStored() > 0, "OUTPUT_COOLANT port should push superheated sodium into the gas tank"))
			.thenExecute(() -> helper.assertTrue(tank.gasTank.getGasType() == GasRegistry.getGas("superheatedSodium"), "Gas tank should hold superheatedSodium, got " + tank.gasTank.getGas()))
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void inputPortPushesNoGas(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		SynchronizedFissionData data = casing.structure;
		TileEntityGasTank tank = GameTestUtil.gasTank(helper, NX, NY, NZ);

		data.wasteTank.stored = new GasStack(GasRegistry.getGas("nuclearWaste"), 500);
		data.superheatedSodiumTank.stored = new GasStack(GasRegistry.getGas("superheatedSodium"), 500);

		helper.startSequence()
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertEquals(0, tank.gasTank.getStored(), "INPUT port must not push gas, tank holds " + tank.gasTank.getGas());
				helper.assertEquals(500, data.wasteTank.getStored(), "Waste must stay in the reactor");
				helper.assertEquals(500, data.superheatedSodiumTank.getStored(), "Superheated sodium must stay in the reactor");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 30)
	public static void inputPortPushesNoSteam(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		SynchronizedFissionData data = casing.structure;
		TileEntityFluidTank tank = GameTestUtil.fluidTank(helper, NX, NY, NZ);

		data.steamStored = new FluidStack(FluidRegistry.getFluid("steam"), 500);

		helper.startSequence()
			.thenIdle(10)
			.thenExecute(() -> {
				helper.assertEquals(0, tank.fluidTank.getFluidAmount(), "INPUT port must not push steam, tank holds " + tank.fluidTank.getFluid());
				helper.assertEquals(500, data.steamStored.amount, "Steam must stay in the reactor");
			})
			.thenSucceed();
	}
}
