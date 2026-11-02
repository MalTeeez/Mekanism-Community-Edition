package mekanism.common.integration.minetweaker;

import mekanism.api.gas.GasStack;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.machines.SolarNeutronRecipe;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** mods.mekanism.SolarNeutronActivator.addRecipe("lithium", 1, "tritium", 1); */
@ZenClass("mods.mekanism.SolarNeutronActivator")
public class SolarNeutronActivatorTweaker
{
	@ZenMethod
	public static void addRecipe(String input, int inputAmount, String output, int outputAmount)
	{
		GasStack in = MekanismTweaks.gas(input, inputAmount);
		GasStack out = MekanismTweaks.gas(output, outputAmount);

		if(in != null && out != null)
		{
			MekanismTweaks.add(Recipe.SOLAR_NEUTRON_ACTIVATOR, new SolarNeutronRecipe(in, out));
		}
	}

	@ZenMethod
	public static void removeRecipe(String input)
	{
		MekanismTweaks.remove(Recipe.SOLAR_NEUTRON_ACTIVATOR, input);
	}
}
