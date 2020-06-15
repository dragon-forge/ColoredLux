package com.zeitheron.lux.api.event;

import com.zeitheron.lux.api.LuxManager;
import com.zeitheron.lux.api.light.ILightBlockHandler;
import com.zeitheron.lux.api.light.ILightEntityHandler;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.EntityEntry;

/**
 * Subscribe to this event to register entity and block lights
 */
public class ReloadLuxManagerEvent
		extends Event
{
	public void registerBlockLight(Block blk, ILightBlockHandler handler)
	{
		LuxManager.registerBlockLight(blk, handler);
	}

	public void registerEntityLight(EntityEntry ent, ILightEntityHandler handler)
	{
		LuxManager.registerEntityLight(ent, handler);
	}
}