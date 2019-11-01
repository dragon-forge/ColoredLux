package com.zeitheron.lux.api.light;

import com.zeitheron.lux.api.LuxManager;
import com.zeitheron.lux.api.event.GatherLightsEvent;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LightEntityWrapper
{
	public final Entity entity;
	
	public LightEntityWrapper(Entity entity, ILightEntityHandler handler)
	{
		this.entity = entity;
		this.handler = handler;
		this.handler.update(entity);
	}
	
	ILightEntityHandler handler;
	
	public void addLights(GatherLightsEvent e)
	{
		if(handler != null)
			handler.createLights(entity, e);
	}

	public void remove(int id)
	{
		handler.remove(id);
	}
}