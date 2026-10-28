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
	 * Builds a square reactor shell around a single interior column in its centre, height blocks tall. A port sits at (1,1,0) and a logic adapter at (1,2,0).
	 * @return the casing in the (0,0,0) corner
	 */
	public static TileEntityFissionCasing shell(GameTestHelper helper, int width, int height)
	{
		return shell(helper, width, height, false);
	}

	/** With glass, every face block that is not on an edge is Reactor Glass, like a typical player build. */
	public static TileEntityFissionCasing shell(GameTestHelper helper, int width, int height, boolean glass)
	{
		for(int x = 0; x < width; x++)
		{
			for(int y = 0; y < height; y++)
			{
				for(int z = 0; z < width; z++)
				{
					if(GameTestUtil.onShell(x, y, z, width, height, width))
					{
						int meta = x == 1 && y == 1 && z == 0 ? PORT : x == 1 && y == 2 && z == 0 ? ADAPTER : CASING;
						int edges = (x == 0 || x == width-1 ? 1 : 0)+(y == 0 || y == height-1 ? 1 : 0)+(z == 0 || z == width-1 ? 1 : 0);

						if(glass && meta == CASING && edges == 1)
						{
							helper.setBlock(x, y, z, GeneratorsBlocks.ReactorGlass, 0);
						}
						else {
							helper.setBlock(x, y, z, GeneratorsBlocks.Reactor, meta);
						}
					}
				}
			}
		}

		GameTestUtil.clearOnFinish(helper, width, height, width);

		return GameTestUtil.tile(helper, TileEntityFissionCasing.class, 0, 0, 0);
	}

	/** Fills the interior column, read bottom to top: F fuel assembly, R control rod, . air. */
	public static void interior(GameTestHelper helper, int width, String column)
	{
		for(int i = 0; i < column.length(); i++)
		{
			char c = column.charAt(i);

			if(c != '.')
			{
				helper.setBlock(width/2, i+1, width/2, GeneratorsBlocks.FissionAssembly, c == 'F' ? 0 : 1);
			}
		}
	}

	/** Builds the shell, then the interior; placing the interior makes the reactor form on its own. */
	public static TileEntityFissionCasing form(GameTestHelper helper, String column)
	{
		return form(helper, 3, column);
	}

	public static TileEntityFissionCasing form(GameTestHelper helper, int width, String column)
	{
		TileEntityFissionCasing casing = shell(helper, width, column.length()+2);
		interior(helper, width, column);

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
