package mekanism.gametest.config;

import mekanism.api.MekanismConfig.general;
import mekanism.api.MekanismConfig.generators;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class HeatBalanceTests
{
	@GameTest(timeoutTicks = 5)
	public static void modernBalanceUsesUpstreamValues(GameTestHelper helper)
	{
		helper.assertFalse(general.legacyHeatBalance, "These expectations assume LegacyHeatBalance=false, which is the default");
		helper.assertEquals(10.0, general.maxEnergyPerSteam, 1e-9, "general.maxEnergyPerSteam");
		helper.assertEquals(0.2, general.steamEnergyEfficiency, 1e-9, "general.steamEnergyEfficiency");
		helper.assertEquals(0.7, general.boilerWaterConductivity, 1e-9, "general.boilerWaterConductivity");
		helper.assertEquals(50/general.energyPerHeat, general.boilerCasingHeatCapacity, 1e-9, "general.boilerCasingHeatCapacity");
		helper.assertEquals(16000000/general.energyPerHeat, general.superheatingHeatTransfer, 1e-9, "general.superheatingHeatTransfer");
		helper.assertEquals(32000.0, generators.turbineVentGasFlow, 1e-9, "generators.turbineVentGasFlow");
		helper.assertEquals(1280.0, generators.turbineDisperserGasFlow, 1e-9, "generators.turbineDisperserGasFlow");
		helper.assertEquals(64000, generators.condenserRate, "generators.condenserRate");
		helper.succeed();
	}
}
