package mekanism.common.integration.minetweaker;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.MachineRecipe;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;

/** MineTweaker support for the machines ModTweaker doesn't know. Only touch this class when MineTweaker3 is loaded. */
public final class MekanismTweaks
{
	public static void register()
	{
		MineTweakerAPI.registerClass(IsotopicCentrifugeTweaker.class);
		MineTweakerAPI.registerClass(SolarNeutronActivatorTweaker.class);
	}

	/** Null (with a script error) when the gas doesn't exist or the amount isn't positive. */
	static GasStack gas(String name, int amount)
	{
		Gas gas = GasRegistry.getGas(name);

		if(gas == null)
		{
			MineTweakerAPI.logError("[Mekanism] Unknown gas: " + name);
			return null;
		}

		if(amount <= 0)
		{
			MineTweakerAPI.logError("[Mekanism] Gas amount must be positive: " + name + " x" + amount);
			return null;
		}

		return new GasStack(gas, amount);
	}

	static void add(Recipe type, MachineRecipe recipe)
	{
		MineTweakerAPI.apply(new Change(type, (GasInput)recipe.getInput(), recipe));
	}

	static void remove(Recipe type, String inputGas)
	{
		GasStack input = gas(inputGas, 1);

		if(input != null)
		{
			MineTweakerAPI.apply(new Change(type, new GasInput(input), null));
		}
	}

	/** Puts (recipe != null) or removes the recipe for one input; undo restores whatever was there before. */
	private static class Change implements IUndoableAction
	{
		private final Recipe type;
		private final GasInput input;
		private final MachineRecipe recipe;
		private MachineRecipe previous;

		Change(Recipe type, GasInput input, MachineRecipe recipe)
		{
			this.type = type;
			this.input = input;
			this.recipe = recipe;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void apply()
		{
			previous = (MachineRecipe)(recipe != null ? type.get().put(input, recipe) : type.get().remove(input));

			if(recipe == null && previous == null)
			{
				MineTweakerAPI.logWarning("[Mekanism] No " + type.getRecipeName() + " recipe for " + input.ingredient.getGas().getName() + " to remove");
			}
		}

		@Override
		public boolean canUndo()
		{
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void undo()
		{
			if(previous != null)
			{
				type.get().put(input, previous);
			}
			else {
				type.get().remove(input);
			}
		}

		@Override
		public String describe()
		{
			return (recipe != null ? "Adding " : "Removing ") + type.getRecipeName() + " recipe for " + input.ingredient.getGas().getName();
		}

		@Override
		public String describeUndo()
		{
			return "Reverting " + type.getRecipeName() + " recipe for " + input.ingredient.getGas().getName();
		}

		@Override
		public Object getOverrideKey()
		{
			return null;
		}
	}
}
