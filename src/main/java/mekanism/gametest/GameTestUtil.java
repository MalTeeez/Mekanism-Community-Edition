package mekanism.gametest;

import mekanism.api.transmitters.TransmissionType;
import mekanism.common.MekanismBlocks;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.TileEntityGasTank;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;

public final class GameTestUtil
{
	private GameTestUtil() {}

	public static boolean onShell(int x, int y, int z, int sx, int sy, int sz)
	{
		return x == 0 || y == 0 || z == 0 || x == sx-1 || y == sy-1 || z == sz-1;
	}

	/** Clears the given box once the test ends so the cell is left empty. */
	public static void clearOnFinish(GameTestHelper helper, int sx, int sy, int sz)
	{
		helper.afterTest(() -> {
			for(int x = 0; x < sx; x++)
			{
				for(int y = 0; y < sy; y++)
				{
					for(int z = 0; z < sz; z++)
					{
						helper.destroyBlock(x, y, z);
					}
				}
			}
		});
	}

	public static <T extends TileEntity> T tile(GameTestHelper helper, Class<T> type, int x, int y, int z)
	{
		return helper.assertTileEntityPresent(type, x, y, z);
	}

	/** Places a gas tank that takes gas from its south side and does not eject, removed again when the test ends. */
	public static TileEntityGasTank gasTank(GameTestHelper helper, int x, int y, int z)
	{
		helper.setBlock(x, y, z, MekanismBlocks.GasTank, 0);
		helper.afterTest(() -> helper.destroyBlock(x, y, z));

		TileEntityGasTank tank = tile(helper, TileEntityGasTank.class, x, y, z);
		tank.configComponent.setEjecting(TransmissionType.GAS, false);

		for(int facing = 2; facing < 6 && !tank.canReceiveGas(ForgeDirection.SOUTH, null); facing++)
		{
			tank.facing = facing;
		}

		helper.assertTrue(tank.canReceiveGas(ForgeDirection.SOUTH, null), "Test precondition: the gas tank should accept gas on its south side");

		return tank;
	}

	/** Places a basic fluid tank, removed again when the test ends. */
	public static TileEntityFluidTank fluidTank(GameTestHelper helper, int x, int y, int z)
	{
		helper.setBlock(x, y, z, MekanismBlocks.MachineBlock2, 11);
		helper.afterTest(() -> helper.destroyBlock(x, y, z));

		return tile(helper, TileEntityFluidTank.class, x, y, z);
	}
}
