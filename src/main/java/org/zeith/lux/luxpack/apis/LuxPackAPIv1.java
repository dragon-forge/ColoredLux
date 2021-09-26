package org.zeith.lux.luxpack.apis;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.zeith.lux.api.LuxManager;
import org.zeith.lux.api.event.ReloadLuxManagerEvent;
import org.zeith.lux.luxpack.AbstractLuxPack;

import java.util.ArrayList;
import java.util.List;

public class LuxPackAPIv1
		implements ILuxPackAPI
{
	@SubscribeEvent
	public void reloadLux(ReloadLuxManagerEvent e)
	{
		for(AbstractLuxPack pack : packs)
		{
			LuxManager.BLOCK_LUMINANCES.putAll(pack.getBlockLights());
			LuxManager.ENTITY_LUMINANCES.putAll(pack.getEntityLights());
		}
	}
	
	protected final List<AbstractLuxPack> packs = new ArrayList<>();
	
	@Override
	public void hookLuxPack(AbstractLuxPack pack)
	{
		MinecraftForge.EVENT_BUS.register(this);
		packs.add(pack);
	}
	
	@Override
	public void unhookLuxPack(AbstractLuxPack pack)
	{
		MinecraftForge.EVENT_BUS.register(this);
		packs.remove(pack);
	}
}