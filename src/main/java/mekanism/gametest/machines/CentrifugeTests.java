package mekanism.gametest.machines;

import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.common.MekanismBlocks;
import mekanism.common.tile.TileEntityIsotopicCentrifuge;
import mekanism.gametest.GameTestUtil;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class CentrifugeTests
{
	/** Places a powered centrifuge facing south, with its bounding block above like ItemBlockMachine would. */
	private static TileEntityIsotopicCentrifuge place(GameTestHelper helper)
	{
		helper.setBlock(1, 1, 1, MekanismBlocks.MachineBlock4, 0);
		GameTestUtil.clearOnFinish(helper, 3, 4, 3);

		TileEntityIsotopicCentrifuge tile = GameTestUtil.tile(helper, TileEntityIsotopicCentrifuge.class, 1, 1, 1);
		tile.facing = 3;
		tile.onPlace();
		tile.setEnergy(tile.getMaxEnergy());

		helper.assertFalse(helper.getWorld().isAirBlock(tile.xCoord, tile.yCoord+1, tile.zCoord), "Centrifuge should have a bounding block above it");

		return tile;
	}

	private static void feed(GameTestHelper helper, TileEntityIsotopicCentrifuge tile, String gas, int amount)
	{
		int accepted = tile.receiveGas(ForgeDirection.EAST, new GasStack(GasRegistry.getGas(gas), amount), true);
		helper.assertEquals(amount, accepted, "Centrifuge should accept " + amount + " mB " + gas + " on its input side");
	}

	@GameTest(timeoutTicks = 40)
	public static void enrichesUraniumHexafluorideToFissileFuel(GameTestHelper helper)
	{
		TileEntityIsotopicCentrifuge tile = place(helper);
		double energy = tile.getEnergy();

		feed(helper, tile, "uraniumHexafluoride", 10);

		helper.startSequence()
			.thenWaitUntil(30, () -> helper.assertEquals(0, tile.inputTank.getStored(), "Input UF6 should be consumed, still " + tile.inputTank.getStored() + " mB"))
			.thenExecute(() -> {
				GasStack out = tile.outputTank.getGas();

				helper.assertTrue(out != null && out.getGas() == GasRegistry.getGas("fissileFuel"), "Output should be fissileFuel, got " + out);
				helper.assertEquals(10, out.amount, "UF6 -> fissile fuel should be 1:1");
				helper.assertTrue(tile.getEnergy() < energy, "Centrifuge should have used energy");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 60)
	public static void separatesPlutoniumFromNuclearWaste(GameTestHelper helper)
	{
		TileEntityIsotopicCentrifuge tile = place(helper);

		feed(helper, tile, "nuclearWaste", 10);

		helper.startSequence()
			.thenWaitUntil(40, () -> helper.assertEquals(0, tile.inputTank.getStored(), "Input waste should be consumed, still " + tile.inputTank.getStored() + " mB"))
			.thenExecute(() -> {
				GasStack out = tile.outputTank.getGas();

				helper.assertTrue(out != null && out.getGas() == GasRegistry.getGas("plutonium"), "Output should be plutonium, got " + out);
				helper.assertEquals(1, out.amount, "10 nuclear waste -> 1 plutonium");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void doesNothingWithoutEnergy(GameTestHelper helper)
	{
		TileEntityIsotopicCentrifuge tile = place(helper);

		tile.setEnergy(0);
		feed(helper, tile, "uraniumHexafluoride", 10);

		helper.onEachTick(() -> helper.assertEquals(10, tile.inputTank.getStored(), "An unpowered centrifuge consumed input"));
		helper.succeedAtTimeout();
	}
}
