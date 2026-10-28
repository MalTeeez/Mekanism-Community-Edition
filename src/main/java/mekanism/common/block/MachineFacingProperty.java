package mekanism.common.block;

import mekanism.common.tile.TileEntityBasicBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;

/** GTNHLib blockstate property exposing a machine tile's facing, for JSON modelled machines. */
public class MachineFacingProperty implements DirectionBlockProperty
{
	@Override
	public String getName()
	{
		return "facing";
	}

	@Override
	public boolean hasTrait(BlockPropertyTrait trait)
	{
		return trait == BlockPropertyTrait.SupportsWorld;
	}

	@Override
	public ForgeDirection getValue(IBlockAccess world, int x, int y, int z)
	{
		TileEntity tile = world.getTileEntity(x, y, z);

		return tile instanceof TileEntityBasicBlock ? ForgeDirection.getOrientation(((TileEntityBasicBlock)tile).facing) : ForgeDirection.NORTH;
	}
}
