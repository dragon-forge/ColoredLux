package org.zeith.lux.client.gui;

import com.zeitheron.hammercore.client.utils.UtilsFX;
import com.zeitheron.hammercore.utils.ReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.*;
import org.zeith.lux.ConfigCL;
import org.zeith.lux.proxy.ClientProxy;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class HOOKVideoSettings
{
	static Field buttonA, buttonB;
	public static List<String> toDrawTooltip;
	
	@SubscribeEvent
	public static void initGui(GuiScreenEvent.InitGuiEvent.Post e)
	{
		if(e.getGui() != null && ClientProxy.GuiPerformanceSettingsOF != null && ClientProxy.GuiPerformanceSettingsOF.isAssignableFrom(e.getGui().getClass()))
		{
			for(int i = 0; i < e.getButtonList().size(); ++i)
			{
				GuiButton btn = e.getButtonList().get(i);
				if(btn instanceof GuiOptionButton)
				{
					GuiOptionButton opts = (GuiOptionButton) btn;
					if(ClientProxy.customOptions.contains(opts.getOption()))
						e.getButtonList().set(i, convert((GuiOptionButton) btn));
				}
			}
		}
		
		if(e.getGui() instanceof GuiVideoSettings && !ClientProxy.OptifineInstalled)
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
				
				if(buttonB != null && prev.buttonB instanceof GuiOptionButton && ClientProxy.customOptions.contains(((GuiOptionButton) prev.buttonB).getOption()))
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
				
				if(buttonA != null && prev.buttonA instanceof GuiOptionButton && ClientProxy.customOptions.contains(((GuiOptionButton) prev.buttonA).getOption()))
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
				displayString = I18n.format(getOption().getTranslation()) +
				                (state != null ? (": " + (state ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED) + I18n.format("options.o" + (state ? "n" : "ff"))) : "") + TextFormatting.RESET;
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
	
	private static final Pattern OF_FILTER = Pattern.compile("\\{OF}(?<body>.*?)\\{/}", Pattern.DOTALL);
	
	private static void formatDescription(String str, List<String> desc)
	{
		if(ClientProxy.OptifineInstalled)
			str = OF_FILTER.matcher(str).replaceAll("${body}");
		else
			str = OF_FILTER.matcher(str).replaceAll("");
		desc.addAll(Arrays.asList(str.split("<br>")));
	}
	
	public static void describe(GameSettings.Options opt, List<String> desc)
	{
		if(opt == ClientProxy.LUX_ENABLE_LIGHTING)
			formatDescription(I18n.format("options.lux:lighting.desc"), desc);
		
		if(opt == ClientProxy.LUX_ENABLE_FOG)
			formatDescription(I18n.format("options.lux:fog.desc"), desc);
		
		if(opt == ClientProxy.LUX_REDUCED_REFRESH_RATE)
			formatDescription(I18n.format("options.lux:reduced_refresh_rate.desc"), desc);
		
		if(opt == ClientProxy.LUX_PACKS)
			formatDescription(I18n.format("options.lux:packs.desc"), desc);
	}
	
	public static Boolean getState(GameSettings.Options opt)
	{
		if(opt == ClientProxy.LUX_ENABLE_LIGHTING)
			return ConfigCL.enableColoredLighting;
		
		if(opt == ClientProxy.LUX_ENABLE_FOG)
			return ConfigCL.enableFog;
		
		if(opt == ClientProxy.LUX_REDUCED_REFRESH_RATE)
			return ConfigCL.reducedRefreshRate;
		
		if(opt == ClientProxy.LUX_PACKS)
			return null;
		
		return false;
	}
	
	public static void toggle(GameSettings.Options opt)
	{
		if(opt == ClientProxy.LUX_ENABLE_LIGHTING)
		{
			ConfigCL.enableColoredLighting = !ConfigCL.enableColoredLighting;
			ConfigCL.cfgs.get("Client-Side", "Colored Lighting", true).set(ConfigCL.enableColoredLighting);
			ConfigCL.cfgs.save();
		}
		
		if(opt == ClientProxy.LUX_ENABLE_FOG)
		{
			ConfigCL.enableFog = !ConfigCL.enableFog;
			ConfigCL.cfgs.get("Client-Side", "Enable Fog", true).set(ConfigCL.enableFog);
			ConfigCL.cfgs.save();
		}
		
		if(opt == ClientProxy.LUX_REDUCED_REFRESH_RATE)
		{
			ConfigCL.reducedRefreshRate = !ConfigCL.reducedRefreshRate;
			ConfigCL.cfgs.get("Client-Side", "Reduced Light Update Frequency", true).set(ConfigCL.reducedRefreshRate);
			ConfigCL.cfgs.save();
		}
		
		if(opt == ClientProxy.LUX_PACKS)
		{
			Minecraft.getMinecraft().displayGuiScreen(new GuiScreenLuxPacks(Minecraft.getMinecraft().currentScreen));
		}
	}
}