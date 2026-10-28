package mekanism.gametest.fission;

import mekanism.api.MekanismConfig.generators;
import mekanism.generators.common.GeneratorsBlocks;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import mekanism.generators.common.tile.fission.TileEntityFissionPort;

import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class FissionFormationTests
{
	@GameTest(timeoutTicks = 20)
	public static void formsMinimalReactor(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");

		helper.assertNotNull(casing.structure, "A 3x3x4 reactor with one fuel assembly under a control rod should form");
		helper.assertEquals(1, casing.structure.assemblies, "Fuel assembly count");
		helper.assertEquals(6, casing.structure.surfaceArea, "Surface area of a lone assembly (6 faces)");
		helper.assertEquals(36, casing.structure.volume, "Structure volume of a 3x3x4 box");
		helper.assertEquals(generators.fissionMaxFuelPerAssembly, casing.structure.getFuelCapacity(), "Fuel tank capacity of one assembly");

		TileEntityFissionPort port = FissionTestUtil.port(helper);
		helper.assertSame(casing.structure, port.structure, "The port should share the reactor structure");
		helper.assertSame(casing.structure, FissionTestUtil.adapter(helper).structure, "The logic adapter should share the reactor structure");
		helper.succeed();
	}

	@GameTest(timeoutTicks = 20)
	public static void stackedAssembliesShareFaces(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FFR");

		helper.assertNotNull(casing.structure, "A reactor with two stacked fuel assemblies under one rod should form");
		helper.assertEquals(2, casing.structure.assemblies, "Fuel assembly count");
		helper.assertEquals(10, casing.structure.surfaceArea, "Two stacked assemblies hide two faces: 6+6-2");
		helper.succeed();
	}

	@GameTest(timeoutTicks = 20)
	public static void doesNotFormWithoutControlRod(GameTestHelper helper)
	{
		assertNeverForms(helper, "F.", "reactor without a control rod");
	}

	@GameTest(timeoutTicks = 20)
	public static void doesNotFormWithGapInFuelColumn(GameTestHelper helper)
	{
		assertNeverForms(helper, "F.FR", "reactor with an air gap inside the fuel column");
	}

	@GameTest(timeoutTicks = 20)
	public static void doesNotFormWithTwoRodsInColumn(GameTestHelper helper)
	{
		assertNeverForms(helper, "FRR", "reactor with two control rods in one column");
	}

	@GameTest(timeoutTicks = 20)
	public static void doesNotFormWithoutFuel(GameTestHelper helper)
	{
		assertNeverForms(helper, "R.", "reactor without any fuel assembly");
	}

	@GameTest(timeoutTicks = 20)
	public static void formsWhenInteriorIsPlacedAfterTheShell(GameTestHelper helper)
	{
		//Five wide with air above and below the column, so no interior block touches the frame and no neighbour update reaches it
		TileEntityFissionCasing casing = FissionTestUtil.shell(helper, 5, 6);

		helper.startSequence()
			.thenIdle(2)
			.thenExecute(() -> {
				helper.assertNull(casing.structure, "An empty shell must not form");
				FissionTestUtil.interior(helper, 5, ".FR.");
				helper.assertNotNull(casing.structure, "Placing the last interior block should form the reactor without waiting for a rescan");
				helper.assertEquals(1, casing.structure.assemblies, "Fuel assembly count");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void breakingAFuelAssemblyUpdatesTheStructure(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, 5, ".FFR.");

		helper.assertEquals(2, casing.structure.assemblies, "Test precondition: two assemblies");
		helper.destroyBlock(2, 2, 2);
		helper.assertNotNull(casing.structure, "The remaining fuel assembly is still a valid column under the rod");
		helper.assertEquals(1, casing.structure.assemblies, "Fuel assembly count after breaking the lower assembly");
		helper.assertEquals(6, casing.structure.surfaceArea, "Surface area of the remaining assembly");
		helper.succeed();
	}

	@GameTest(timeoutTicks = 20)
	public static void breakingTheTopFuelAssemblyUnformsTheReactor(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, 5, ".FFR.");

		helper.assertNotNull(casing.structure, "Test precondition: formed");
		helper.destroyBlock(2, 3, 2);
		helper.assertNull(casing.structure, "A gap between the fuel and the control rod should unform the reactor");
		helper.succeed();
	}

	@GameTest(timeoutTicks = 20)
	public static void breakingAndReplacingTheControlRod(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, 5, ".FR.");

		helper.assertNotNull(casing.structure, "Test precondition: formed");
		helper.destroyBlock(2, 3, 2);
		helper.assertNull(casing.structure, "Breaking the control rod should unform the reactor");

		helper.setBlock(2, 3, 2, GeneratorsBlocks.FissionAssembly, 1);
		helper.assertNotNull(casing.structure, "Placing the control rod back should form the reactor again");
		helper.assertEquals(1, casing.structure.assemblies, "Fuel assembly count after re-forming");
		helper.succeed();
	}

	private static void assertNeverForms(GameTestHelper helper, String column, String what)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, column);

		//The tile's own ticker==5 check runs inside the window as well
		helper.onEachTick(() -> helper.assertNull(casing.structure, "An invalid " + what + " formed a structure"));
		helper.assertNull(casing.structure, "An invalid " + what + " formed a structure");
		helper.succeedAtTimeout();
	}
}
