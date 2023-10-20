package org.zeith.lux;

import com.zeitheron.hammercore.HammerCore;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.*;
import org.zeith.lux.proxy.CommonProxy;

@Mod(
		modid = "lux",
		name = "Colored Lux",
		version = "@VERSION@",
		certificateFingerprint = "9f5e2a811a8332a842b34f6967b7db0ac4f24856",
		updateJSON = "https://api.modrinth.com/updates/mEr4VarV/forge_updates.json",
		guiFactory = "org.zeith.lux.client.CLGuiFactory",
		dependencies = "required-after:hammercore"
)
public class ColoredLux
{
	public static final Logger LOG = LogManager.getLogger("ColoredLux");
	
	@SidedProxy(serverSide = "org.zeith.lux.proxy.CommonProxy", clientSide = "org.zeith.lux.proxy.ClientProxy")
	public static CommonProxy proxy;
	
	@EventHandler
	public void fingerprintViolated(FMLFingerprintViolationEvent e)
	{
		LOG.warn("*****************************");
		LOG.warn("WARNING: Somebody has been tampering with ColoredLux jar!");
		LOG.warn("It is highly recommended that you redownload mod from https://dccg.herokuapp.com/api/fmlhp/347912 !");
		LOG.warn("*****************************");
		HammerCore.invalidCertificate = true;
		HammerCore.invalidCertificates.put("lux", "https://dccg.herokuapp.com/api/fmlhp/347912");
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent e)
	{
		proxy.preInit(e);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent e)
	{
		proxy.postInit();
	}
}