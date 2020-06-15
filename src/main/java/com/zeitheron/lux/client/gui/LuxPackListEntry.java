package com.zeitheron.lux.client.gui;

import com.zeitheron.hammercore.lib.zlib.utils.Joiner;
import com.zeitheron.lux.luxpack.LuxPackAPIManager;
import com.zeitheron.lux.luxpack.LuxPackRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Arrays;
import java.util.List;

public class LuxPackListEntry
		implements GuiListExtended.IGuiListEntry
{
	private static final ResourceLocation RESOURCE_PACKS_TEXTURE = new ResourceLocation("textures/gui/resource_packs.png");
	private static final ITextComponent INCOMPATIBLE = new TextComponentTranslation("resourcePack.incompatible");
	private static final ITextComponent INCOMPATIBLE_API = new TextComponentTranslation("luxPack.incompatible.api");
	private static final ITextComponent INCOMPATIBLE_MODS = new TextComponentTranslation("luxPack.incompatible.mods");
	protected final Minecraft mc;
	protected final GuiScreenLuxPacks luxPacksGUI;
	private final LuxPackRepository.Entry luxPackEntry;

	public LuxPackListEntry(GuiScreenLuxPacks luxPacksGUIIn, LuxPackRepository.Entry entry)
	{
		this.luxPacksGUI = luxPacksGUIIn;
		this.mc = Minecraft.getMinecraft();
		this.luxPackEntry = entry;
	}

	protected void bindLuxPackIcon()
	{
		this.luxPackEntry.bindTexturePackIcon(this.mc.getTextureManager());
	}

	protected int getLuxPackFormat()
	{
		return this.luxPackEntry.getPackFormat();
	}

	protected String getLuxPackDescription()
	{
		return this.luxPackEntry.getTexturePackDescription();
	}

	protected String getLuxPackName()
	{
		return this.luxPackEntry.getResourcePackName();
	}

	public LuxPackRepository.Entry getLuxPackEntry()
	{
		return this.luxPackEntry;
	}

	@Override
	public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks)
	{
		int i = this.getLuxPackFormat();

		boolean missingMods = luxPackEntry.pack == null || !luxPackEntry.pack.getMissingMods().isEmpty();

		if(i > LuxPackAPIManager.getNewestAPIVersion() || missingMods)
		{
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			Gui.drawRect(x - 1, y - 1, x + listWidth - 9, y + slotHeight + 1, -8978432);
		}

		this.bindLuxPackIcon();
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 32, 32, 32.0F, 32.0F);
		String s = this.getLuxPackName();
		String s1 = this.getLuxPackDescription();

		if(this.showHoverOverlay() && (this.mc.gameSettings.touchscreen || isSelected))
		{
			this.mc.getTextureManager().bindTexture(RESOURCE_PACKS_TEXTURE);
			Gui.drawRect(x, y, x + 32, y + 32, -1601138544);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			int j = mouseX - x;
			int k = mouseY - y;

			if(i > LuxPackAPIManager.getNewestAPIVersion() || missingMods)
			{
				s = INCOMPATIBLE.getFormattedText();
				s1 = (missingMods ? INCOMPATIBLE_MODS : INCOMPATIBLE_API).getFormattedText();
			}

			if(this.canMoveRight())
			{
				if(j < 32)
				{
					Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 32.0F, 32, 32, 256.0F, 256.0F);
				} else
				{
					Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 32, 32, 256.0F, 256.0F);
				}
			} else
			{
				if(this.canMoveLeft())
				{
					if(j < 16)
					{
						Gui.drawModalRectWithCustomSizedTexture(x, y, 32.0F, 32.0F, 32, 32, 256.0F, 256.0F);
					} else
					{
						Gui.drawModalRectWithCustomSizedTexture(x, y, 32.0F, 0.0F, 32, 32, 256.0F, 256.0F);
					}
				}

				if(this.canMoveUp())
				{
					if(j < 32 && j > 16 && k < 16)
					{
						Gui.drawModalRectWithCustomSizedTexture(x, y, 96.0F, 32.0F, 32, 32, 256.0F, 256.0F);
					} else
					{
						Gui.drawModalRectWithCustomSizedTexture(x, y, 96.0F, 0.0F, 32, 32, 256.0F, 256.0F);
					}
				}

				if(this.canMoveDown())
				{
					if(j < 32 && j > 16 && k > 16)
					{
						Gui.drawModalRectWithCustomSizedTexture(x, y, 64.0F, 32.0F, 32, 32, 256.0F, 256.0F);
					} else
					{
						Gui.drawModalRectWithCustomSizedTexture(x, y, 64.0F, 0.0F, 32, 32, 256.0F, 256.0F);
					}
				}
			}
		}

		int i1 = this.mc.fontRenderer.getStringWidth(s);

		if(i1 > 157)
		{
			s = this.mc.fontRenderer.trimStringToWidth(s, 157 - this.mc.fontRenderer.getStringWidth("...")) + "...";
		}

		this.mc.fontRenderer.drawStringWithShadow(s, (float) (x + 32 + 2), (float) (y + 1), 16777215);
		List<String> list = this.mc.fontRenderer.listFormattedStringToWidth(s1, 157);

		for(int l = 0; l < 2 && l < list.size(); ++l)
		{
			this.mc.fontRenderer.drawStringWithShadow(list.get(l), (float) (x + 32 + 2), (float) (y + 11 + 12 * l), 8421504);
		}

		if(isSelected)
		{
			luxPacksGUI.tooltipOffX = -mouseX + x + 22;
			luxPacksGUI.tooltipOffY = -mouseY + y + 23;
			List<String> text = luxPacksGUI.tooltip;
			text.addAll(mc.fontRenderer.listFormattedStringToWidth(getLuxPackDescription(), 157));
			text.add("");
			if(luxPackEntry.pack != null)
			{
				text.add(String.format("Author%s: %s", luxPackEntry.pack.authors.length > 1 ? "s" : "", Joiner.on(", ").join(Arrays.stream(luxPackEntry.pack.authors))));
				List<String> mm = luxPackEntry.pack.getMissingMods();
				if(!mm.isEmpty())
				{
					text.add("");
					text.add(String.format("Missing %,d mod%s", mm.size(), mm.size() > 1 ? "s" : ""));
					for(int j = 0; j < mm.size(); ++j) text.add("  " + mm.get(j));
				}
				if(luxPackEntry.pack.apiVersion > LuxPackAPIManager.getNewestAPIVersion())
				{
					text.add("");
					text.add("This pack uses API v" + luxPackEntry.pack.apiVersion);
					text.add("  ...but the latest one available is v" + LuxPackAPIManager.getNewestAPIVersion());
				}
			}
		}
	}

	protected boolean showHoverOverlay()
	{
		return true;
	}

	protected boolean canMoveRight()
	{
		return !this.luxPacksGUI.hasResourcePackEntry(this);
	}

	protected boolean canMoveLeft()
	{
		return this.luxPacksGUI.hasResourcePackEntry(this);
	}

	protected boolean canMoveUp()
	{
		List<LuxPackListEntry> list = this.luxPacksGUI.getListContaining(this);
		int i = list.indexOf(this);
		return i > 0 && list.get(i - 1).showHoverOverlay();
	}

	protected boolean canMoveDown()
	{
		List<LuxPackListEntry> list = this.luxPacksGUI.getListContaining(this);
		int i = list.indexOf(this);
		return i >= 0 && i < list.size() - 1 && list.get(i + 1).showHoverOverlay();
	}

	/**
	 * Called when the mouse is clicked within this entry. Returning true means that something within this entry was
	 * clicked and the list should not be dragged.
	 */
	@Override
	public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY)
	{
		if(this.showHoverOverlay() && relativeX <= 32)
		{
			if(this.canMoveRight())
			{
				this.luxPacksGUI.markChanged();
				int l = this.getLuxPackFormat();

				if(l <= LuxPackAPIManager.getNewestAPIVersion() && (luxPackEntry.pack != null && luxPackEntry.pack.getMissingMods().isEmpty()))
				{
					this.luxPacksGUI.getListContaining(this).remove(this);
					this.luxPacksGUI.getSelectedResourcePacks().add(0, this);
				} else
				{
					String s = I18n.format("luxPack.incompatible.confirm.title");
					String s1 = I18n.format("luxPack.incompatible.confirm." + (l > LuxPackAPIManager.getNewestAPIVersion() ? "api" : "mods"));
					this.mc.displayGuiScreen(new GuiYesNo((result, id) ->
					{
						List<LuxPackListEntry> list2 = luxPacksGUI.getListContaining(LuxPackListEntry.this);
						mc.displayGuiScreen(luxPacksGUI);

						if(result)
						{
							list2.remove(LuxPackListEntry.this);
							luxPacksGUI.getSelectedResourcePacks().add(0, LuxPackListEntry.this);
						}
					}, s, s1, 0));
				}

				return true;
			}

			if(relativeX < 16 && this.canMoveLeft())
			{
				this.luxPacksGUI.getListContaining(this).remove(this);
				this.luxPacksGUI.getAvailableResourcePacks().add(0, this);
				this.luxPacksGUI.markChanged();
				return true;
			}

			if(relativeX > 16 && relativeY < 16 && this.canMoveUp())
			{
				List<LuxPackListEntry> list1 = this.luxPacksGUI.getListContaining(this);
				int k = list1.indexOf(this);
				list1.remove(this);
				list1.add(k - 1, this);
				this.luxPacksGUI.markChanged();
				return true;
			}

			if(relativeX > 16 && relativeY > 16 && this.canMoveDown())
			{
				List<LuxPackListEntry> list = this.luxPacksGUI.getListContaining(this);
				int i = list.indexOf(this);
				list.remove(this);
				list.add(i + 1, this);
				this.luxPacksGUI.markChanged();
				return true;
			}
		}

		return false;
	}

	@Override
	public void updatePosition(int slotIndex, int x, int y, float partialTicks)
	{
	}

	/**
	 * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY
	 */
	@Override
	public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY)
	{
	}

	public boolean isServerPack()
	{
		return false;
	}
}