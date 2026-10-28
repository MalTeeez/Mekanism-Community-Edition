package mekanism.gametest;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;

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
}
