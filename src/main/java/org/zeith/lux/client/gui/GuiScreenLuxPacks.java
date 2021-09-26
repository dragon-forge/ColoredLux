package org.zeith.lux.client.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import org.zeith.lux.api.LuxManager;
import org.zeith.lux.luxpack.LuxPackRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiScreenLuxPacks
		extends GuiScreen
{
	private final GuiScreen parentScreen;
	/**
	 * List of available resource packs
	 */
	private List<LuxPackListEntry> availableResourcePacks;
	/**
	 * List of selected resource packs
	 */
	private List<LuxPackListEntry> selectedResourcePacks;
	/**
	 * List component that contains the available resource packs
	 */
	private GuiLuxPackAvailable availableResourcePacksList;
	/**
	 * List component that contains the selected resource packs
	 */
	private GuiLuxPackSelected selectedResourcePacksList;
	private boolean changed;

	public List<String> tooltip = new ArrayList<>();
	public int tooltipOffX, tooltipOffY;

	public GuiScreenLuxPacks(GuiScreen parentScreenIn)
	{
		this.parentScreen = parentScreenIn;
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
	 * window resizes, the buttonList is cleared beforehand.
	 */
	@Override
	public void initGui()
	{
		this.buttonList.add(new GuiButtonTex(0, this.width / 2 + 158, this.height - 48, new ResourceLocation("lux", "textures/misc/question.png"), 0));
		this.buttonList.add(new GuiOptionButton(1, this.width / 2 + 4, this.height - 48, I18n.format("gui.done")));
		this.buttonList.add(new GuiOptionButton(2, this.width / 2 - 154, this.height - 48, I18n.format("luxPack.openFolder")));
		this.buttonList.add(new GuiButtonTex(3, this.width / 2 - 178, this.height - 48, new ResourceLocation("lux", "textures/misc/web.png"), 2));

		if(!this.changed)
		{
			this.availableResourcePacks = Lists.newArrayList();
			this.selectedResourcePacks = Lists.newArrayList();
			LuxPackRepository repo = LuxPackRepository.getInstance();

			repo.refreshAvailablePacks();

			List<LuxPackRepository.Entry> list = Lists.newArrayList(repo.getRepositoryEntriesAll());
			list.removeAll(repo.getRepositoryEntries());

			for(LuxPackRepository.Entry e : list)
				this.availableResourcePacks.add(new LuxPackListEntry(this, e));

			List<String> str = repo.getEnableOrder();
			list = repo.getRepositoryEntries();
			list.sort((a, b) ->
			{
				if(str.contains(a.location.getName()) && str.contains(b.location.getName()))
					return str.indexOf(b.location.getName()) - str.indexOf(a.location.getName());
				return 0;
			});

			for(LuxPackRepository.Entry e : list)
				this.selectedResourcePacks.add(new LuxPackListEntry(this, e));
		}

		this.availableResourcePacksList = new GuiLuxPackAvailable(this.mc, 200, this.height, this.availableResourcePacks);
		this.availableResourcePacksList.setSlotXBoundsFromLeft(this.width / 2 - 4 - 200);
		this.availableResourcePacksList.registerScrollButtons(7, 8);
		this.selectedResourcePacksList = new GuiLuxPackSelected(this.mc, 200, this.height, this.selectedResourcePacks);
		this.selectedResourcePacksList.setSlotXBoundsFromLeft(this.width / 2 + 4);
		this.selectedResourcePacksList.registerScrollButtons(7, 8);
	}

	/**
	 * Handles mouse input.
	 */
	@Override
	public void handleMouseInput() throws IOException
	{
		super.handleMouseInput();
		this.selectedResourcePacksList.handleMouseInput();
		this.availableResourcePacksList.handleMouseInput();
	}

	public boolean hasResourcePackEntry(LuxPackListEntry resourcePackEntry)
	{
		return this.selectedResourcePacks.contains(resourcePackEntry);
	}

	/**
	 * Returns the list containing the resource pack entry, returns the selected list if it is selected, otherwise
	 * returns the available list
	 */
	public List<LuxPackListEntry> getListContaining(LuxPackListEntry resourcePackEntry)
	{
		return this.hasResourcePackEntry(resourcePackEntry) ? this.selectedResourcePacks : this.availableResourcePacks;
	}

	/**
	 * Returns a list containing the available resource packs
	 */
	public List<LuxPackListEntry> getAvailableResourcePacks()
	{
		return this.availableResourcePacks;
	}

	/**
	 * Returns a list containing the selected resource packs
	 */
	public List<LuxPackListEntry> getSelectedResourcePacks()
	{
		return this.selectedResourcePacks;
	}

	/**
	 * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
	 */
	@Override
	protected void actionPerformed(GuiButton button) throws IOException
	{
		if(button.enabled)
		{
			if(button.id == 2)
			{
				File dir = new File(Minecraft.getMinecraft().gameDir, "luxpacks");
				if(dir.isFile()) dir.delete();
				if(!dir.isDirectory()) dir.mkdirs();
				OpenGlHelper.openFile(dir);
			} else if(button.id == 1)
			{
				if(this.changed)
				{
					List<LuxPackRepository.Entry> list = Lists.newArrayList();

					for(LuxPackListEntry e : this.selectedResourcePacks)
						if(e != null)
							list.add(e.getLuxPackEntry());

					Collections.reverse(list);
					LuxPackRepository.getInstance().setRepositories(list);
					LuxManager.reload();
				}

				this.mc.displayGuiScreen(this.parentScreen);
			} else if(button.id == 0)
			{
				ITextComponent txt = new TextComponentString(" ");
				txt.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/dragon-forge/LuxPacks/wiki/Creating-lux-pack"));
				handleComponentClick(txt);
			} else if(button.id == 3)
			{
				ITextComponent txt = new TextComponentString(" ");
				txt.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/dragon-forge/LuxPacks"));
				handleComponentClick(txt);
//				GitLuxPackRepository.refreshWebLuxPacks();
			}
		}
	}

	/**
	 * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
	 */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
	{
		super.mouseClicked(mouseX, mouseY, mouseButton);
		this.availableResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
		this.selectedResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
	}

	/**
	 * Draws the screen and all the components in it.
	 */
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		tooltip.clear();
		tooltipOffX = tooltipOffY = 0;
		this.drawBackground(0);
		this.availableResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
		this.selectedResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
		this.drawCenteredString(this.fontRenderer, I18n.format("luxPack.title"), this.width / 2, 16, 16777215);
		this.drawCenteredString(this.fontRenderer, I18n.format("luxPack.folderInfo"), this.width / 2 - 77, this.height - 26, 8421504);
		super.drawScreen(mouseX, mouseY, partialTicks);
		if(!tooltip.isEmpty())
		{
			drawHoveringText(tooltip, mouseX + tooltipOffX, mouseY + tooltipOffY);
		}
	}

	/**
	 * Marks the selected resource packs list as changed to trigger a resource reload when the screen is closed
	 */
	public void markChanged()
	{
		this.changed = true;
	}
}