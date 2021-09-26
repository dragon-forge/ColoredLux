package org.zeith.lux.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.zeith.lux.proxy.ClientProxy;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class HWSupport
{
	private static boolean warningHandled = false;
	
	public static String getCard()
	{
		return ClientProxy.GPU;
	}
	
	/**
	 * Whether or not the ColoredLux has handled displaying the warning to end user in the current game instance.
	 * Show your warning IF AND ONLY IF this method returns false!!!
	 */
	public static boolean hasHandledWarning()
	{
		return warningHandled;
	}
	
	public static void setHandledWarning()
	{
		warningHandled = true;
	}
	
	@Nonnull
	public static EnumGPUCompat getCardCompat(String gpu)
	{
		EnumShaderVersion shdr = getShaderVersionToLoad(gpu);
		
		if(shdr == EnumShaderVersion.NVIDIA) return EnumGPUCompat.FULL;
		if(shdr == EnumShaderVersion.INTEL) return EnumGPUCompat.UNTESTED;
		if(shdr == EnumShaderVersion.AMD) return EnumGPUCompat.GLITCHY;
		
		return EnumGPUCompat.ABSENT;
	}
	
	public static String getCardCompatMessage(String gpu)
	{
		return getCardCompat(gpu).getMessage(gpu);
	}
	
	@Nonnull
	public static EnumShaderVersion getShaderVersionToLoad(String gpu)
	{
		if(gpu.contains("NVIDIA") || gpu.contains("GeForce") || gpu.contains("Quadro") || gpu.contains("Tesla"))
			return EnumShaderVersion.NVIDIA;
		
		if(gpu.contains("Intel"))
			return EnumShaderVersion.INTEL;
		
		if(gpu.contains("AMD") || gpu.contains("Radeon") || gpu.contains("Vega"))
			return EnumShaderVersion.AMD;
		
		return EnumShaderVersion.UNKNOWN;
	}
	
	public enum EnumShaderVersion
	{
		NVIDIA("nv"),
		AMD("amd"),
		INTEL("intel"),
		UNKNOWN(null);
		final String id;
		
		EnumShaderVersion(String id)
		{
			this.id = id;
		}
		
		public String getId()
		{
			return id == null ? NVIDIA.id : id;
		}
	}
	
	public enum EnumGPUCompat
	{
		FULL("%s should be fully supported by Colored Lux!"),
		PARTIAL("%s should run Colored Lux partially, but some features may be missing!"),
		GLITCHY("%s should run Colored Lux, but might be unstable, have glitches all over the place, or even crash."),
		UNTESTED("%s may or may not run Colored Lux, but please don't put your hopes too high."),
		ABSENT("%s is an unknown GPU to Colored Lux, and so it is going to default to NVIDIA rendering, the mod may or may not work.");
		final String message;
		
		EnumGPUCompat(String message)
		{
			this.message = message;
		}
		
		public String getMessage(String gpu)
		{
			return String.format(message, gpu);
		}
	}
}