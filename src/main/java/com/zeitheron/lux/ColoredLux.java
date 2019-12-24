package com.zeitheron.lux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zeitheron.hammercore.HammerCore;
import com.zeitheron.lux.proxy.CommonProxy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "lux", name = "Colored Lux", version = "@VERSION@", certificateFingerprint = "9f5e2a811a8332a842b34f6967b7db0ac4f24856", updateJSON = "http://dccg.herokuapp.com/api/fmluc/347912", guiFactory = "com.zeitheron.lux.client.CLGuiFactory", dependencies = "required-after:hammercore")
public class ColoredLux
{
	public static final Logger LOG = LogManager.getLogger("ColoredLux");
	
	@SidedProxy(serverSide = "com.zeitheron.lux.proxy.CommonProxy", clientSide = "com.zeitheron.lux.proxy.ClientProxy")
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