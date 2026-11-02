package mekanism.common.integration.minetweaker;

import mekanism.api.gas.GasStack;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.machines.GasCentrifugeRecipe;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** mods.mekanism.IsotopicCentrifuge.addRecipe("uraniumHexafluoride", 1, "fissileFuel", 1); */
@ZenClass("mods.mekanism.IsotopicCentrifuge")
public class IsotopicCentrifugeTweaker
{
	@ZenMethod
	public static void addRecipe(String input, int inputAmount, String output, int outputAmount)
	{
		GasStack in = MekanismTweaks.gas(input, inputAmount);
		GasStack out = MekanismTweaks.gas(output, outputAmount);

		if(in != null && out != null)
		{
			MekanismTweaks.add(Recipe.ISOTOPIC_CENTRIFUGE, new GasCentrifugeRecipe(in, out));
		}
	}

	@ZenMethod
	public static void removeRecipe(String input)
	{
		MekanismTweaks.remove(Recipe.ISOTOPIC_CENTRIFUGE, input);
	}
}
