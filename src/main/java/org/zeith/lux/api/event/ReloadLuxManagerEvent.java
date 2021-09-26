package org.zeith.lux.api.event;

import net.minecraft.block.Block;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.EntityEntry;
import org.zeith.lux.api.LuxManager;
import org.zeith.lux.api.light.ILightBlockHandler;
import org.zeith.lux.api.light.ILightEntityHandler;

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