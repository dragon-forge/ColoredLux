package org.zeith.lux.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiLuxPackAvailable
		extends GuiLuxPackList
{
	public GuiLuxPackAvailable(Minecraft mcIn, int p_i45054_2_, int p_i45054_3_, List<LuxPackListEntry> p_i45054_4_)
	{
		super(mcIn, p_i45054_2_, p_i45054_3_, p_i45054_4_);
	}

	@Override
	protected String getListHeader()
	{
		return I18n.format("luxPack.available.title");
	}
}