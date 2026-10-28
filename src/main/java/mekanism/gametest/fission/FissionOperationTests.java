package mekanism.gametest.fission;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import mekanism.api.MekanismConfig.generators;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.generators.common.content.fission.SynchronizedFissionData;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import mekanism.generators.common.tile.fission.TileEntityFissionLogicAdapter;
import mekanism.generators.common.tile.fission.TileEntityFissionLogicAdapter.FissionLogic;
import mekanism.generators.common.tile.fission.TileEntityFissionPort;
import mekanism.generators.common.tile.fission.TileEntityFissionPort.PortMode;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("mekanism")
public class FissionOperationTests
{
	private static GasStack fuel(int amount)
	{
		return new GasStack(GasRegistry.getGas("fissileFuel"), amount);
	}

	private static void sendPacket(TileEntityFissionCasing casing, int type, double value)
	{
		ByteBuf buf = Unpooled.buffer();
		buf.writeByte(type);

		if(type == 1)
		{
			buf.writeDouble(value);
		}

		casing.handlePacketData(buf);
	}

	@GameTest(timeoutTicks = 60)
	public static void burnsFuelIntoWasteAndSteam(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		TileEntityFissionPort port = FissionTestUtil.port(helper);
		SynchronizedFissionData start = casing.structure;

		helper.assertEquals(1000, port.receiveGas(ForgeDirection.UNKNOWN, fuel(1000), true), "Port in INPUT mode should accept 1000 mB fissile fuel");
		helper.assertEquals(1000000, port.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 1000000), true), "Port in INPUT mode should accept water");

		sendPacket(casing, 1, 0.5);
		sendPacket(casing, 0, 0);
		helper.assertTrue(start.active, "The activate packet should switch the reactor on");
		helper.assertEquals(0.5, start.rateLimit, 1e-9, "Burn rate limit after the rate packet");

		helper.startSequence()
			.thenIdle(40)
			.thenExecute(() -> {
				SynchronizedFissionData data = casing.structure;
				int fuelLeft = data.fuelTank.getStored();
				int waste = data.wasteTank.getStored();
				double burned = 1000-fuelLeft-data.burnRemaining;

				helper.assertTrue(fuelLeft < 1000, "Fuel should decrease while burning, but is still " + fuelLeft);
				helper.assertTrue(waste > 0, "Waste should be produced while burning, but tank holds " + waste);
				helper.assertEquals(burned, waste+data.partialWaste, 1e-6, "Waste should be produced 1:1 with fuel burned (burned " + burned + ", waste " + (waste+data.partialWaste) + ")");
				helper.assertTrue(data.lastBurnRate > 0 && data.lastBurnRate <= data.rateLimit+1e-9, "lastBurnRate should be in (0, rateLimit=" + data.rateLimit + "] but is " + data.lastBurnRate);
				helper.assertTrue(data.steamStored != null && data.steamStored.amount > 0, "Steam should be produced from the water, found " + data.steamStored);
				helper.assertTrue(data.waterStored.amount < 1000000, "Water should be consumed, found " + data.waterStored.amount);
				helper.assertTrue(data.temperature > data.ambientTemp, "Temperature " + data.temperature + " should be above ambient " + data.ambientTemp);
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void onlyOneCasingDrivesTheReactor(GameTestHelper helper)
	{
		//Every casing that thinks it is the renderer would run its own burn, so the reactor would burn once per casing
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");

		helper.startSequence()
			.thenIdle(10)
			.thenExecute(() -> {
				int rendering = 0;

				for(int x = 0; x < 3; x++)
				{
					for(int y = 0; y < 4; y++)
					{
						for(int z = 0; z < 3; z++)
						{
							if(x != 1 || z != 1)
							{
								TileEntityFissionCasing tile = (TileEntityFissionCasing)helper.getWorld().getTileEntity(casing.xCoord+x, casing.yCoord+y, casing.zCoord+z);

								if(tile.isRendering)
								{
									rendering++;
								}
							}
						}
					}
				}

				helper.assertEquals(1, rendering, "Exactly one casing should drive the reactor, but " + rendering + " have isRendering set");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void burnRateIsClamped(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");

		sendPacket(casing, 1, 1000);
		helper.assertEquals(casing.structure.getMaxBurnRate(), casing.structure.rateLimit, 1e-9, "Rate limit should clamp to assemblies*burnPerAssembly");
		sendPacket(casing, 1, -5);
		helper.assertEquals(0, casing.structure.rateLimit, 1e-9, "Negative rate limit should clamp to 0");
		helper.succeed();
	}

	@GameTest(timeoutTicks = 40)
	public static void keepsStateWhenStructureReforms(GameTestHelper helper)
	{
		//Regression for 434f57842: the reformed structure used to lose active and the rate limit
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		TileEntityFissionPort port = FissionTestUtil.port(helper);
		SynchronizedFissionData before = casing.structure;

		port.receiveGas(ForgeDirection.UNKNOWN, fuel(1000), true);
		before.rateLimit = 0.05;
		before.active = true;

		helper.startSequence()
			.thenIdle(3)
			.thenExecute(() -> {
				casing.update();
				SynchronizedFissionData after = casing.structure;

				helper.assertNotNull(after, "Reactor should still be formed after revalidation");
				helper.assertNotSame(before, after, "Test precondition: update() should have built a new structure object");
				helper.assertTrue(after.active, "Reactor should stay active after the structure reforms");
				helper.assertEquals(0.05, after.rateLimit, 1e-9, "Rate limit should survive the structure reforming");
				helper.assertTrue(after.fuelTank.getStored() > 900, "Fuel should survive the structure reforming, found " + after.fuelTank.getStored());
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void keepsInputFromTheSameTickWhenStructureReforms(GameTestHelper helper)
	{
		//Regression: a reform rebuilt from a cache synced earlier in the tick, dropping input made since
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		TileEntityFissionPort port = FissionTestUtil.port(helper);

		helper.startSequence()
			.thenIdle(3)
			.thenExecute(() -> {
				port.receiveGas(ForgeDirection.UNKNOWN, fuel(100), true);
				casing.update();

				helper.assertEquals(100, casing.structure.fuelTank.getStored(), "Fuel inserted in the same tick as a reform should be kept");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 80)
	public static void overheatsAndTakesDamageWithoutCoolant(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		SynchronizedFissionData start = casing.structure;

		FissionTestUtil.port(helper).receiveGas(ForgeDirection.UNKNOWN, fuel(1000), true);
		start.rateLimit = start.getMaxBurnRate();
		start.active = true;

		helper.startSequence()
			.thenIdle(70)
			.thenExecute(() -> {
				SynchronizedFissionData data = casing.structure;
				helper.assertTrue(data.temperature >= SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE, "Without coolant the reactor should pass " + SynchronizedFissionData.MIN_DAMAGE_TEMPERATURE + " K, but is at " + data.temperature);
				helper.assertTrue(data.damage > 0, "Overheated reactor should accrue damage, found " + data.damage);
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void forceDisablesInsteadOfMeltingWhenMeltdownsAreOff(GameTestHelper helper)
	{
		boolean enabled = generators.fissionMeltdownsEnabled;
		double chance = generators.fissionMeltdownChance;

		helper.afterTest(() -> {
			generators.fissionMeltdownsEnabled = enabled;
			generators.fissionMeltdownChance = chance;
		});

		generators.fissionMeltdownsEnabled = false;
		generators.fissionMeltdownChance = 1;

		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		SynchronizedFissionData start = casing.structure;

		start.active = true;
		start.temperature = 1500;
		start.damage = SynchronizedFissionData.MAX_DAMAGE-0.01;

		helper.startSequence()
			.thenIdle(5)
			.thenExecute(() -> {
				SynchronizedFissionData data = casing.structure;
				helper.assertTrue(data.forceDisable, "Overdamaged reactor with meltdowns off should be force-disabled (damage " + data.damage + ", temp " + data.temperature + ")");
				helper.assertFalse(data.active, "Force-disabled reactor must not stay active");
				helper.assertNotNull(casing.structure, "Reactor should not have exploded");
				helper.assertBlockPresent(casing.getBlockType(), 5, 0, 0, 0);
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void depletedLogicAdapterEmitsWhenEmpty(GameTestHelper helper)
	{
		TileEntityFissionLogicAdapter adapter = depletedAdapter(helper);

		helper.startSequence()
			.thenIdle(3)
			.thenExecute(() -> {
				helper.assertEquals(15, power(helper, adapter), "DEPLETED adapter should emit redstone with an empty fuel tank");
				helper.assertTrue(adapter.prevOutputting, "Adapter should have latched its output after a tick");
			})
			.thenSucceed();
	}

	@GameTest(timeoutTicks = 20)
	public static void depletedLogicAdapterIsSilentWhenFuelled(GameTestHelper helper)
	{
		TileEntityFissionLogicAdapter adapter = depletedAdapter(helper);

		//Fuel goes in before the first tick: flipping the adapter's output mid-run reforms the reactor, and a reform drops anything stored since the last cache sync
		helper.assertEquals(100, FissionTestUtil.port(helper).receiveGas(ForgeDirection.UNKNOWN, fuel(100), true), "Port should accept fuel");

		helper.startSequence()
			.thenIdle(3)
			.thenExecute(() -> {
				helper.assertEquals(0, power(helper, adapter), "DEPLETED adapter should be silent while fuel is stored (" + adapter.structure.fuelTank.getStored() + " mB)");
				helper.assertFalse(adapter.prevOutputting, "Adapter output latch should stay clear while fuel is stored");
			})
			.thenSucceed();
	}

	private static TileEntityFissionLogicAdapter depletedAdapter(GameTestHelper helper)
	{
		FissionTestUtil.form(helper, "FR");

		TileEntityFissionLogicAdapter adapter = FissionTestUtil.adapter(helper);
		adapter.logicType = FissionLogic.DEPLETED;

		return adapter;
	}

	private static int power(GameTestHelper helper, TileEntityFissionLogicAdapter adapter)
	{
		return adapter.getBlockType().isProvidingWeakPower(helper.getWorld(), adapter.xCoord, adapter.yCoord, adapter.zCoord, 1);
	}

	@GameTest(timeoutTicks = 20)
	public static void portModesGateGasAndFluid(GameTestHelper helper)
	{
		TileEntityFissionCasing casing = FissionTestUtil.form(helper, "FR");
		TileEntityFissionPort port = FissionTestUtil.port(helper);
		SynchronizedFissionData data = casing.structure;

		data.wasteTank.stored = new GasStack(GasRegistry.getGas("nuclearWaste"), 500);

		port.mode = PortMode.INPUT;
		helper.assertNull(port.drawGas(ForgeDirection.UNKNOWN, 100, true), "INPUT port must refuse to hand out waste");
		helper.assertEquals(500, data.wasteTank.getStored(), "Waste must be untouched by a refused draw");
		helper.assertEquals(100, port.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 100), true), "INPUT port should accept water");
		helper.assertEquals(100, port.receiveGas(ForgeDirection.UNKNOWN, fuel(100), true), "INPUT port should accept fuel");

		port.mode = PortMode.OUTPUT_WASTE;
		GasStack drawn = port.drawGas(ForgeDirection.UNKNOWN, 100, true);
		helper.assertTrue(drawn != null && drawn.amount == 100 && drawn.getGas() == GasRegistry.getGas("nuclearWaste"), "OUTPUT_WASTE port should draw 100 nuclearWaste, got " + drawn);
		helper.assertEquals(400, data.wasteTank.getStored(), "Waste left after drawing 100 of 500");
		helper.assertEquals(0, port.receiveGas(ForgeDirection.UNKNOWN, fuel(100), true), "OUTPUT_WASTE port must refuse fuel");
		helper.assertEquals(0, port.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 100), true), "OUTPUT_WASTE port must refuse water");
		helper.succeed();
	}
}
