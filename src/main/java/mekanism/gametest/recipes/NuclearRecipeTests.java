package mekanism.gametest.recipes;

import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.common.MekanismItems;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.AdvancedMachineInput;
import mekanism.common.recipe.inputs.ChemicalPairInput;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.inputs.PressurizedInput;
import mekanism.common.recipe.machines.ChemicalInfuserRecipe;
import mekanism.common.recipe.machines.DissolutionRecipe;
import mekanism.common.recipe.machines.EnrichmentRecipe;
import mekanism.common.recipe.machines.GasCentrifugeRecipe;
import mekanism.common.recipe.machines.InjectionRecipe;
import mekanism.common.recipe.machines.OxidationRecipe;
import mekanism.common.recipe.machines.PressurizedRecipe;
import mekanism.common.recipe.machines.SolarNeutronRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class NuclearRecipeTests
{
	private static GasStack gas(String name, int amount)
	{
		return new GasStack(GasRegistry.getGas(name), amount);
	}

	private static void assertGas(GameTestHelper helper, String what, String expectedGas, int expectedAmount, GasStack actual)
	{
		helper.assertNotNull(actual, what + ": expected " + expectedAmount + " " + expectedGas + " but the recipe has no gas output");
		helper.assertTrue(actual.getGas() == GasRegistry.getGas(expectedGas) && actual.amount == expectedAmount, what + ": expected " + expectedAmount + " " + expectedGas + " but got " + actual.amount + " " + actual.getGas().getName());
	}

	private static void assertItem(GameTestHelper helper, String what, Item expectedItem, int expectedCount, ItemStack actual)
	{
		helper.assertNotNull(actual, what + ": expected " + expectedCount + "x " + expectedItem.getUnlocalizedName() + " but the recipe has no item output");
		helper.assertTrue(actual.getItem() == expectedItem && actual.stackSize == expectedCount, what + ": expected " + expectedCount + "x " + expectedItem.getUnlocalizedName() + " but got " + actual.stackSize + "x " + actual.getUnlocalizedName());
	}

	@GameTest(timeoutTicks = 5)
	public static void oxidizerTurnsYellowCakeIntoUraniumOxide(GameTestHelper helper)
	{
		OxidationRecipe recipe = RecipeHandler.getOxidizerRecipe(new ItemStackInput(new ItemStack(MekanismItems.YellowCakeUranium)));

		helper.assertNotNull(recipe, "No oxidizer recipe for yellow cake");
		assertGas(helper, "Yellow cake oxidation", "uraniumOxide", 250, recipe.getOutput().output);
		helper.succeed();
	}

	@GameTest(timeoutTicks = 5)
	public static void oxidizerTurnsFissileFragmentIntoFissileFuel(GameTestHelper helper)
	{
		OxidationRecipe recipe = RecipeHandler.getOxidizerRecipe(new ItemStackInput(new ItemStack(MekanismItems.ReprocessedFissileFragment)));

		helper.assertNotNull(recipe, "No oxidizer recipe for reprocessed fissile fragments");
		assertGas(helper, "Fissile fragment oxidation", "fissileFuel", 2000, recipe.getOutput().output);
		helper.succeed();
	}

	@GameTest(timeoutTicks = 5)
	public static void infuserMakesUraniumHexafluoride(GameTestHelper helper)
	{
		ChemicalInfuserRecipe recipe = RecipeHandler.getChemicalInfuserRecipe(new ChemicalPairInput(gas("hydrofluoricAcid", 1), gas("uraniumOxide", 1)));

		helper.assertNotNull(recipe, "No chemical infuser recipe for hydrofluoric acid + uranium oxide");
		assertGas(helper, "HF + UO infusion", "uraniumHexafluoride", 2, recipe.getOutput().output);
		helper.succeed();
	}

	@GameTest(timeoutTicks = 5)
	public static void enrichmentTurnsUraniumIngotIntoYellowCake(GameTestHelper helper)
	{
		EnrichmentRecipe recipe = (EnrichmentRecipe)RecipeHandler.getRecipe(new ItemStackInput(new ItemStack(MekanismItems.Ingot, 1, 9)), Recipe.ENRICHMENT_CHAMBER.get());

		helper.assertNotNull(recipe, "No enrichment chamber recipe for uranium ingots");
		assertItem(helper, "Uranium ingot enrichment", MekanismItems.YellowCakeUranium, 2, recipe.getOutput().output);
		helper.succeed();
	}

	@GameTest(timeoutTicks = 5)
	public static void dissolutionTurnsFluoriteIntoHydrofluoricAcid(GameTestHelper helper)
	{
		DissolutionRecipe recipe = RecipeHandler.getDissolutionRecipe(new ItemStackInput(new ItemStack(MekanismItems.FluoriteGem)));

		helper.assertNotNull(recipe, "No dissolution chamber recipe for fluorite");
		assertGas(helper, "Fluorite dissolution", "hydrofluoricAcid", 1000, recipe.getOutput().output);
		helper.succeed();
	}

	@GameTest(timeoutTicks = 5)
	public static void injectionMakesFissileFragmentsFromPlutoniumPellet(GameTestHelper helper)
	{
		AdvancedMachineInput input = new AdvancedMachineInput(new ItemStack(MekanismItems.PlutoniumPellet), GasRegistry.getGas("hydrogenChloride"));
		InjectionRecipe recipe = (InjectionRecipe)RecipeHandler.getRecipe(input, Recipe.CHEMICAL_INJECTION_CHAMBER.get());

		helper.assertNotNull(recipe, "No injection chamber recipe for plutonium pellet + hydrogen chloride");
		assertItem(helper, "Plutonium pellet injection", MekanismItems.ReprocessedFissileFragment, 4, recipe.getOutput().output);
		helper.succeed();
	}

	@GameTest(timeoutTicks = 5)
	public static void reactionChamberPressesPellets(GameTestHelper helper)
	{
		ItemStack dust = new ItemStack(MekanismItems.OtherDust, 1, 7);
		PressurizedRecipe plutonium = RecipeHandler.getPRCRecipe(new PressurizedInput(dust, new FluidStack(FluidRegistry.WATER, 1000), gas("plutonium", 1000)));
		PressurizedRecipe polonium = RecipeHandler.getPRCRecipe(new PressurizedInput(dust, new FluidStack(FluidRegistry.WATER, 1000), gas("polonium", 1000)));

		helper.assertNotNull(plutonium, "No PRC recipe for plutonium pellets");
		assertItem(helper, "Plutonium pellet", MekanismItems.PlutoniumPellet, 1, plutonium.getOutput().getItemOutput());
		assertGas(helper, "Plutonium pellet byproduct", "spentNuclearWaste", 1000, plutonium.getOutput().getGasOutput());
		helper.assertNotNull(polonium, "No PRC recipe for polonium pellets");
		assertItem(helper, "Polonium pellet", MekanismItems.PoloniumPellet, 1, polonium.getOutput().getItemOutput());
		assertGas(helper, "Polonium pellet byproduct", "spentNuclearWaste", 1000, polonium.getOutput().getGasOutput());
		helper.succeed();
	}

	@GameTest(timeoutTicks = 5)
	public static void neutronActivatorAndCentrifugeRecipes(GameTestHelper helper)
	{
		SolarNeutronRecipe polonium = RecipeHandler.getSolarNeutronRecipe(new GasInput(gas("nuclearWaste", 10)));
		GasCentrifugeRecipe fuel = RecipeHandler.getIsotopicCentrifugeRecipe(new GasInput(gas("uraniumHexafluoride", 1)));
		GasCentrifugeRecipe plutonium = RecipeHandler.getIsotopicCentrifugeRecipe(new GasInput(gas("nuclearWaste", 10)));

		helper.assertNotNull(polonium, "No solar neutron activator recipe for nuclear waste");
		assertGas(helper, "Solar neutron activation", "polonium", 1, polonium.getOutput().output);
		helper.assertNotNull(fuel, "No isotopic centrifuge recipe for uranium hexafluoride");
		assertGas(helper, "UF6 centrifuging", "fissileFuel", 1, fuel.getOutput().output);
		helper.assertNotNull(plutonium, "No isotopic centrifuge recipe for nuclear waste");
		assertGas(helper, "Waste centrifuging", "plutonium", 1, plutonium.getOutput().output);
		helper.succeed();
	}
}
