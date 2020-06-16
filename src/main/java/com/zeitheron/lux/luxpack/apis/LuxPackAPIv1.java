package com.zeitheron.lux.luxpack.apis;

import com.zeitheron.lux.api.LuxManager;
import com.zeitheron.lux.api.event.ReloadLuxManagerEvent;
import com.zeitheron.lux.luxpack.AbstractLuxPack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class LuxPackAPIv1
		implements ILuxPackAPI
{
	{
		MinecraftForge.EVENT_BUS.register(this);
	}
	
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
		packs.add(pack);
	}
	
	@Override
	public void unhookLuxPack(AbstractLuxPack pack)
	{
		packs.remove(pack);
	}
}