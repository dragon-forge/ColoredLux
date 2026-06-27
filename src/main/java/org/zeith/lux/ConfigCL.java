package org.zeith.lux;

import com.zeitheron.hammercore.cfg.HCModConfigurations;
import com.zeitheron.hammercore.cfg.IConfigReloadListener;
import com.zeitheron.hammercore.cfg.fields.ModConfigPropertyBool;
import com.zeitheron.hammercore.cfg.fields.ModConfigPropertyInt;
import net.minecraftforge.common.config.Configuration;
import org.zeith.lux.api.LuxManager;

@HCModConfigurations(modid = "lux", isModule = true, module = "main")
public class ConfigCL
		implements IConfigReloadListener
{
	@ModConfigPropertyBool(name = "Colored Lighting", category = "Client-Side", defaultValue = true, comment = "Enable colored lighting engine?")
	public static boolean enableColoredLighting = true;
	
	@ModConfigPropertyInt(name = "Max Render Distance", category = "Client-Side", defaultValue = 128, min = 1, max = 512, comment = "How far the lights would be culled after?")
	public static int maxRenderDistance = 128;
	
	@ModConfigPropertyInt(name = "Max Search Distance", category = "Client-Side", defaultValue = 48, min = 1, max = 128, comment = "What range should lighting blocks search? If your CPU has less cores, try reducing this value to 12-16 blocks.")
	public static int maxSearchDistance = 48;
	
	@ModConfigPropertyInt(name = "Min Lights", category = "Client-Side", defaultValue = 1024, min = 0, max = 2048, comment = "How many lights would the mod allocate to render into the world by default? When the limit is reached, you'll see a flicker. That indicates that the allocation has been doubled automatically.")
	public static int minLights = 1024;
	
	@ModConfigPropertyInt(name = "Max Lights", category = "Client-Side", defaultValue = 2048, min = 0, max = 131072, comment = "How many lights would the mod render into the world? Lights are sorted nearest-first, so further-away lights will be culled after nearer lights.")
	public static int maxLights = 131072;
	
	@ModConfigPropertyBool(name = "Light Add Mode", category = "Client-Side", defaultValue = true, comment = "Should the light sources add up or max with vanilla lighting?\ntrue: The light sources will add up with minecraft lighting engine. (More washed out, but more realistic)\nfalse: The total light value will be maximal brightness from minecraft and colored (or something inbetween) (Vivid but unrealistic)")
	public static boolean lightAddMode = true;
	
	@ModConfigPropertyBool(name = "Enable Fog", category = "Client-Side", defaultValue = true, comment = "Should colored lighting engine render fog?\ntrue: The fog will be rendered alongside the colored lighting.\nfalse: No fog should be visible when colored lighting is enabled.")
	public static boolean enableFog = true;
	
	@ModConfigPropertyBool(name = "Reduced Light Update Frequency", category = "Client-Side", defaultValue = true, comment = "When enabled, ColoredLux will gather lights every tick instead of every frame.")
	public static boolean reducedRefreshRate = true;
	
	public static Configuration cfgs;
	
	@Override
	public void reloadCustom(Configuration cfgs)
	{
		LuxManager.reload();
		ConfigCL.cfgs = cfgs;
	}
	
	public static void disableLighting()
	{
		cfgs.get("Client-Side", "Colored Lighting", true).set(ConfigCL.enableColoredLighting = false);
		cfgs.save();
	}
}