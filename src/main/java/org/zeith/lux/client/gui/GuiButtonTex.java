package org.zeith.lux.client.gui;

import com.zeitheron.hammercore.client.utils.RenderUtil;
import com.zeitheron.hammercore.client.utils.UtilsFX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class GuiButtonTex
		extends GuiButton
{
	final ResourceLocation texture;
	final int inset;

	public GuiButtonTex(int buttonId, int x, int y, ResourceLocation texture, int inset)
	{
		super(buttonId, x, y, 20, 20, "");
		this.texture = texture;
		this.inset = inset;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
	{
		super.drawButton(mc, mouseX, mouseY, partialTicks);

		UtilsFX.bindTexture(texture);
		RenderUtil.drawFullTexturedModalRect(x + inset, y + inset, width - inset * 2, height - inset * 2);
	}
}