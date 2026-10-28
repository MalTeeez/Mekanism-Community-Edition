package mekanism.client.render;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.ImmutableSet;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * GTNHLib looks blockstates up under the block's registry domain, which for Mekanism is the mixed-case mod id, but
 * vanilla resource packs only serve lowercase domains. This pack serves those blockstates from the lowercase folders.
 */
//ponytail: stopgap until GTNHLib lowercases the registry domain itself; then drop this and the BlockModelInfo overrides
@SideOnly(Side.CLIENT)
public class MixedCaseDomainPack implements IResourcePack
{
	private static final Set<String> DOMAINS = ImmutableSet.of("Mekanism", "MekanismGenerators");

	public static void register()
	{
		List<IResourcePack> packs = ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
		packs.add(new MixedCaseDomainPack());
	}

	@Override
	public InputStream getInputStream(ResourceLocation location) throws IOException
	{
		InputStream stream = resourceExists(location) ? MixedCaseDomainPack.class.getResourceAsStream(path(location)) : null;

		if(stream == null)
		{
			throw new FileNotFoundException(location.toString());
		}

		return stream;
	}

	@Override
	public boolean resourceExists(ResourceLocation location)
	{
		return DOMAINS.contains(location.getResourceDomain()) && location.getResourcePath().startsWith("blockstates/") && MixedCaseDomainPack.class.getResource(path(location)) != null;
	}

	@Override
	public Set getResourceDomains()
	{
		return DOMAINS;
	}

	@Override
	public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section)
	{
		return null;
	}

	@Override
	public BufferedImage getPackImage()
	{
		return null;
	}

	@Override
	public String getPackName()
	{
		return "Mekanism mixed-case blockstates";
	}

	private static String path(ResourceLocation location)
	{
		return "/assets/" + location.getResourceDomain().toLowerCase(Locale.ROOT) + "/" + location.getResourcePath();
	}
}
