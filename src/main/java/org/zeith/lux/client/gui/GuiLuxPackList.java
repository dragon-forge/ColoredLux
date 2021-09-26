package org.zeith.lux.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public abstract class GuiLuxPackList
		extends GuiListExtended
{
	protected final Minecraft mc;
	protected final List<LuxPackListEntry> resourcePackEntries;

	public GuiLuxPackList(Minecraft mcIn, int p_i45055_2_, int p_i45055_3_, List<LuxPackListEntry> p_i45055_4_)
	{
		super(mcIn, p_i45055_2_, p_i45055_3_, 32, p_i45055_3_ - 55 + 4, 36);
		this.mc = mcIn;
		this.resourcePackEntries = p_i45055_4_;
		this.centerListVertically = false;
		this.setHasListHeader(true, (int) ((float) mcIn.fontRenderer.FONT_HEIGHT * 1.5F));
	}

	/**
	 * Handles drawing a list's header row.
	 */
	@Override
	protected void drawListHeader(int insideLeft, int insideTop, Tessellator tessellatorIn)
	{
		String s = TextFormatting.UNDERLINE + "" + TextFormatting.BOLD + this.getListHeader();
		this.mc.fontRenderer.drawString(s, insideLeft + this.width / 2 - this.mc.fontRenderer.getStringWidth(s) / 2, Math.min(this.top + 3, insideTop), 16777215);
	}

	protected abstract String getListHeader();

	public List<LuxPackListEntry> getList()
	{
		return this.resourcePackEntries;
	}

	@Override
	protected int getSize()
	{
		return this.getList().size();
	}

	/**
	 * Gets the IGuiListEntry object for the given index
	 */
	@Override
	public LuxPackListEntry getListEntry(int index)
	{
		return (LuxPackListEntry) this.getList().get(index);
	}

	/**
	 * Gets the width of the list
	 */
	@Override
	public int getListWidth()
	{
		return this.width;
	}

	@Override
	protected int getScrollBarX()
	{
		return this.right - 6;
	}
}