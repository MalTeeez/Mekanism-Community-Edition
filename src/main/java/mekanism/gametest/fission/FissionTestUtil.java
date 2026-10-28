package mekanism.gametest.fission;

import mekanism.gametest.GameTestUtil;
import mekanism.generators.common.GeneratorsBlocks;
import mekanism.generators.common.tile.fission.TileEntityFissionCasing;
import mekanism.generators.common.tile.fission.TileEntityFissionLogicAdapter;
import mekanism.generators.common.tile.fission.TileEntityFissionPort;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;

public final class FissionTestUtil
{
	public static final int CASING = 5, PORT = 6, ADAPTER = 9;

	private FissionTestUtil() {}

	/**
	 * Builds a 3x3 reactor shell around a single interior column. The column is read bottom to top:
	 * F fuel assembly, R control rod, . air. A port sits at (1,1,0) and a logic adapter at (1,2,0).
	 * @return the casing in the (0,0,0) corner
	 */
	public static TileEntityFissionCasing build(GameTestHelper helper, String column)
	{
		int height = column.length()+2;

		//Interior first: the shell forms the structure as soon as it is complete, and interior blocks do not retrigger the check
		for(int i = 0; i < column.length(); i++)
		{
			char c = column.charAt(i);

			if(c != '.')
			{
				helper.setBlock(1, i+1, 1, GeneratorsBlocks.FissionAssembly, c == 'F' ? 0 : 1);
			}
		}

		for(int x = 0; x < 3; x++)
		{
			for(int y = 0; y < height; y++)
			{
				for(int z = 0; z < 3; z++)
				{
					if(GameTestUtil.onShell(x, y, z, 3, height, 3))
					{
						int meta = x == 1 && y == 1 && z == 0 ? PORT : x == 1 && y == 2 && z == 0 ? ADAPTER : CASING;
						helper.setBlock(x, y, z, GeneratorsBlocks.Reactor, meta);
					}
				}
			}
		}

		GameTestUtil.clearOnFinish(helper, 3, height, 3);

		return GameTestUtil.tile(helper, TileEntityFissionCasing.class, 0, 0, 0);
	}

	/** Builds the reactor and runs the structure check right away. */
	public static TileEntityFissionCasing form(GameTestHelper helper, String column)
	{
		TileEntityFissionCasing casing = build(helper, column);
		casing.update();

		return casing;
	}

	public static TileEntityFissionPort port(GameTestHelper helper)
	{
		return GameTestUtil.tile(helper, TileEntityFissionPort.class, 1, 1, 0);
	}

	public static TileEntityFissionLogicAdapter adapter(GameTestHelper helper)
	{
		return GameTestUtil.tile(helper, TileEntityFissionLogicAdapter.class, 1, 2, 0);
	}
}
