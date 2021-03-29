package com.zeitheron.lux.client.gui;

import com.zeitheron.hammercore.client.utils.UtilsFX;
import com.zeitheron.hammercore.utils.ReflectionUtil;
import com.zeitheron.lux.ConfigCL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.zeitheron.lux.proxy.ClientProxy.*;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class HOOKVideoSettings
{
	static Field buttonA, buttonB;
	public static List<String> toDrawTooltip;
	
	@SubscribeEvent
	public static void initGui(GuiScreenEvent.InitGuiEvent.Post e)
	{
		if(e.getGui() != null && GuiPerformanceSettingsOF != null && GuiPerformanceSettingsOF.isAssignableFrom(e.getGui().getClass()))
		{
			for(int i = 0; i < e.getButtonList().size(); ++i)
			{
				GuiButton btn = e.getButtonList().get(i);
				if(btn instanceof GuiOptionButton)
				{
					GuiOptionButton opts = (GuiOptionButton) btn;
					if(customOptions.contains(opts.getOption()))
						e.getButtonList().set(i, convert((GuiOptionButton) btn));
				}
			}
		}
		
		if(e.getGui() instanceof GuiVideoSettings && !OptifineInstalled)
		{
			GuiVideoSettings settings = (GuiVideoSettings) e.getGui();
			
			GuiOptionsRowList options = (GuiOptionsRowList) settings.optionsRowList;
			for(int i = 0; i < options.options.size(); ++i)
			{
				GuiOptionsRowList.Row prev = options.options.get(i);
				
				if(buttonA == null && prev.buttonA != null && prev.buttonA != prev.buttonB)
				{
					for(Field f : GuiOptionsRowList.Row.class.getDeclaredFields())
						if(GuiButton.class.isAssignableFrom(f.getType()))
						{
							f.setAccessible(true);
							try
							{
								if(f.get(prev) == prev.buttonA)
									buttonA = f;
							} catch(IllegalArgumentException | IllegalAccessException e1)
							{
								e1.printStackTrace();
							}
						}
				}
				
				if(buttonB == null && prev.buttonB != null && prev.buttonA != prev.buttonB)
				{
					for(Field f : GuiOptionsRowList.Row.class.getDeclaredFields())
						if(GuiButton.class.isAssignableFrom(f.getType()))
						{
							f.setAccessible(true);
							try
							{
								if(f.get(prev) == prev.buttonB)
									buttonB = f;
							} catch(IllegalArgumentException | IllegalAccessException e1)
							{
								e1.printStackTrace();
							}
						}
				}
				
				if(buttonB != null && prev.buttonB instanceof GuiOptionButton && customOptions.contains(((GuiOptionButton) prev.buttonB).getOption()))
				{
					try
					{
						GuiOptionButton nbtn = convert((GuiOptionButton) prev.buttonB);
						if(Modifier.isFinal(buttonB.getModifiers()) && !ReflectionUtil.setFinalField(buttonB, prev, nbtn))
							System.out.println("Failed to override final value of " + buttonB.getName());
						else if(!Modifier.isFinal(buttonB.getModifiers()))
							buttonB.set(prev, nbtn);
						else
							System.out.println("Failed to override final value of " + buttonB.getName());
					} catch(ReflectiveOperationException e1)
					{
						e1.printStackTrace();
					}
				}
				
				if(buttonA != null && prev.buttonA instanceof GuiOptionButton && customOptions.contains(((GuiOptionButton) prev.buttonA).getOption()))
				{
					try
					{
						GuiOptionButton nbtn = convert((GuiOptionButton) prev.buttonA);
						if(Modifier.isFinal(buttonA.getModifiers()) && !ReflectionUtil.setFinalField(buttonA, prev, nbtn))
							System.out.println("Failed to override final value of " + buttonA.getName());
						else if(!Modifier.isFinal(buttonA.getModifiers()))
							buttonA.set(prev, nbtn);
						else
							System.out.println("Failed to override final value of " + buttonA.getName());
					} catch(ReflectiveOperationException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		}
	}
	
	private static GuiOptionButton convert(GuiOptionButton btn)
	{
		return new GuiOptionButton(btn.id, btn.x, btn.y, btn.getOption(), btn.displayString)
		{
			Long hoverTime;
			
			@Override
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
			{
				Boolean state = getState(getOption());
				displayString = I18n.format(getOption().getTranslation()) + (state != null ? (": " + (state ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED) + I18n.format("options.o" + (state ? "n" : "ff"))) : "") + TextFormatting.RESET;
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(isMouseOver())
				{
					if(hoverTime == null)
						hoverTime = System.currentTimeMillis();
					else if(System.currentTimeMillis() - hoverTime.longValue() >= 1500L)
					{
						toDrawTooltip = new ArrayList<>();
						toDrawTooltip.add("Colored Lux:");
						toDrawTooltip.add("");
						describe(getOption(), toDrawTooltip);
					}
				} else
					hoverTime = null;
			}
			
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
			{
				boolean p = super.mousePressed(mc, mouseX, mouseY);
				if(p)
					toggle(getOption());
				return p;
			}
		};
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public static void renderGui(GuiScreenEvent.DrawScreenEvent.Post e)
	{
		if(toDrawTooltip != null)
		{
			FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
			int x = e.getMouseX(), y = e.getMouseY();
			if(y + fr.FONT_HEIGHT * toDrawTooltip.size() >= e.getGui().height - 6)
				y = e.getGui().height - 6 - fr.FONT_HEIGHT * toDrawTooltip.size();
			int mw = 0;
			for(String s : toDrawTooltip)
				mw = Math.max(mw, fr.getStringWidth(s));
			if(x + mw > e.getGui().width - 16)
				x = e.getGui().width - mw - 16;
			UtilsFX.drawCustomTooltip(e.getGui(), Minecraft.getMinecraft().getRenderItem(), fr, toDrawTooltip, x, y, TextFormatting.AQUA.getColorIndex());
			toDrawTooltip = null;
		}
	}
	
	public static void describe(GameSettings.Options opt, List<String> desc)
	{
		if(opt == LUX_ENABLE_LIGHTING)
		{
			String str = I18n.format("options.lux:lighting.desc");
			while(str.contains("{OF}") && str.contains("{/}"))
			{
				int ofi = str.indexOf("{OF}");
				int ofe = str.indexOf("{/}", ofi);
				String inner = str.substring(ofi + 4, ofe);
				if(OptifineInstalled)
					str = str.replace("{OF}" + inner + "{/}", inner);
				else
					str = str.replace("{OF}" + inner + "{/}", "");
			}
			desc.addAll(Arrays.asList(str.split("<br>")));
		}

		if(opt == LUX_ENABLE_FOG)
		{
			String str = I18n.format("options.lux:fog.desc");
			while(str.contains("{OF}") && str.contains("{/}"))
			{
				int ofi = str.indexOf("{OF}");
				int ofe = str.indexOf("{/}", ofi);
				String inner = str.substring(ofi + 4, ofe);
				if(OptifineInstalled)
					str = str.replace("{OF}" + inner + "{/}", inner);
				else
					str = str.replace("{OF}" + inner + "{/}", "");
			}
			desc.addAll(Arrays.asList(str.split("<br>")));
		}
		
		if(opt == LUX_PACKS)
		{
			String str = I18n.format("options.lux:packs.desc");
			desc.addAll(Arrays.asList(str.split("<br>")));
		}
	}
	
	public static Boolean getState(GameSettings.Options opt)
	{
		if(opt == LUX_ENABLE_LIGHTING)
			return ConfigCL.enableColoredLighting;

		if(opt == LUX_ENABLE_FOG)
			return ConfigCL.enableFog;
		
		if(opt == LUX_PACKS)
			return null;
		
		return false;
	}
	
	public static void toggle(GameSettings.Options opt)
	{
		if(opt == LUX_ENABLE_LIGHTING)
		{
			ConfigCL.enableColoredLighting = !ConfigCL.enableColoredLighting;
			ConfigCL.cfgs.get("Client-Side", "Colored Lighting", true).set(ConfigCL.enableColoredLighting);
			ConfigCL.cfgs.save();
		}

		if(opt == LUX_ENABLE_FOG)
		{
			ConfigCL.enableFog = !ConfigCL.enableFog;
			ConfigCL.cfgs.get("Client-Side", "Enable Fog", true).set(ConfigCL.enableFog);
			ConfigCL.cfgs.save();
		}
		
		if(opt == LUX_PACKS)
		{
			Minecraft.getMinecraft().displayGuiScreen(new GuiScreenLuxPacks(Minecraft.getMinecraft().currentScreen));
		}
	}
}