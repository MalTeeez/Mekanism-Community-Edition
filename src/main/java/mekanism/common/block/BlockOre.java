package mekanism.common.block;

import java.util.List;
import java.util.Random;

import mekanism.common.Mekanism;
import mekanism.common.MekanismBlocks;
import mekanism.common.MekanismItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Block class for handling multiple ore block IDs.
 * 0: Osmium Ore
 * 1: Copper Ore
 * 2: Tin Ore
 * 3: Lead Ore
 * 4: Uranium Ore
 * 5: Fluorite Ore
 * @author AidanBrady
 *
 */
public class BlockOre extends Block
{
	public IIcon[] icons = new IIcon[256];

	public BlockOre(String type)
	{
		super(Material.rock);
		setHardness(type == "deepslate" ? 4.5F : 3F);
		setResistance(5F);
		setCreativeTab(Mekanism.tabMekanism);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register)
	{
		if(this == MekanismBlocks.OreBlock)
		{
			icons[0] = register.registerIcon("mekanism:OsmiumOre");
			icons[1] = register.registerIcon("mekanism:CopperOre");
			icons[2] = register.registerIcon("mekanism:TinOre");
			icons[3] = register.registerIcon("mekanism:LeadOre");
			icons[4] = register.registerIcon("mekanism:UraniumOre");
			icons[5] = register.registerIcon("mekanism:FluoriteOre");
		}
		if(this == MekanismBlocks.DeepslateOreBlock) {
			icons[0] = register.registerIcon("mekanism:DeepslateOsmiumOre");
			icons[1] = register.registerIcon("mekanism:DeepslateCopperOre");
			icons[2] = register.registerIcon("mekanism:DeepslateTinOre");
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta)
	{
		return icons[meta];
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune)
	{
		return meta == 5 ? MekanismItems.FluoriteGem : super.getItemDropped(meta, random, fortune);
	}

	@Override
	public int quantityDropped(int meta, int fortune, Random random)
	{
		if(meta != 5)
		{
			return 1;
		}

		//Upstream: 2-4 gems with the vanilla ore fortune bonus
		int bonus = fortune > 0 ? Math.max(0, random.nextInt(fortune+2)-1) : 0;

		return (2+random.nextInt(3))*(bonus+1);
	}

	@Override
	public int damageDropped(int i)
	{
		return i == 5 ? 0 : i;
	}

	@Override
	public int getExpDrop(IBlockAccess world, int meta, int fortune)
	{
		return meta == 5 ? MathHelper.getRandomIntegerInRange(new Random(), 1, 4) : 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs creativetabs, List list)
	{
		list.add(new ItemStack(item, 1, 0));
		list.add(new ItemStack(item, 1, 1));
		list.add(new ItemStack(item, 1, 2));

		if(this == MekanismBlocks.OreBlock)
		{
			list.add(new ItemStack(item, 1, 3));
			list.add(new ItemStack(item, 1, 4));
			list.add(new ItemStack(item, 1, 5));
		}
	}
}
