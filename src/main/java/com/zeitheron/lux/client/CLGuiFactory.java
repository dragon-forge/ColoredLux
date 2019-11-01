package com.zeitheron.lux.client;

import java.util.Set;

import com.zeitheron.hammercore.cfg.gui.HCConfigGui;
import com.zeitheron.lux.ConfigCL;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

public class CLGuiFactory implements IModGuiFactory
{
	@Override
	public void initialize(Minecraft minecraftInstance)
	{
	}

	@Override
	public boolean hasConfigGui()
	{
		return true;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen)
	{
		return new HCConfigGui(parentScreen, ConfigCL.cfgs, "lux");
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
	{
		return null;
	}
}