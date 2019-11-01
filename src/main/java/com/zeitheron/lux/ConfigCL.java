package com.zeitheron.lux;

import javax.annotation.PropertyKey;

import com.zeitheron.hammercore.cfg.HCModConfigurations;
import com.zeitheron.hammercore.cfg.IConfigReloadListener;
import com.zeitheron.hammercore.cfg.fields.ModConfigPropertyBool;
import com.zeitheron.hammercore.cfg.fields.ModConfigPropertyInt;
import com.zeitheron.lux.client.json.JsonBlockLights;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Configuration;

@HCModConfigurations(modid = "lux", isModule = true, module = "main")
public class ConfigCL implements IConfigReloadListener
{
	@ModConfigPropertyBool(name = "Colored Lighting", category = "Client-Side", defaultValue = true, comment = "Enable colored lighting engine?")
	public static boolean enableColoredLighting = true;
	
	@ModConfigPropertyInt(name = "Max Distance", category = "Client-Side", defaultValue = 64, min = 1, max = 512, comment = "How far the lights would be culled after?")
	public static int maxDistance = 64;
	
	@ModConfigPropertyInt(name = "Min Lights", category = "Client-Side", defaultValue = 40, min = 0, max = 1000, comment = "How many lights would the mod allocate to render into the world? When the limit is reached, you'll see a flicker. That indicates that the allocation has been doubled.")
	public static int minLights = 40;
	
	@ModConfigPropertyInt(name = "Max Lights", category = "Client-Side", defaultValue = 100, min = 0, max = 1000, comment = "How many lights would the mod render into the world? Lights are sorted nearest-first, so further-away lights will be culled after nearer lights.")
	public static int maxLights = 100;
	
	@ModConfigPropertyBool(name = "Light Add Mode", category = "Client-Side", defaultValue = true, comment = "Should the light sources add up or max with vanilla lighting?\ntrue: The light sources will add up with minecraft lighting engine. (More washed out, but more realistic)\nfalse: The total light value will be maximal brightness from minecraft and colored (or something inbetween) (Vivid but unrealistic)")
	public static boolean lightAddMode = true;
	
	public static Configuration cfgs;
	
	@Override
	public void reloadCustom(Configuration cfgs)
	{
		JsonBlockLights.reload();
		ConfigCL.cfgs = cfgs;
	}
}