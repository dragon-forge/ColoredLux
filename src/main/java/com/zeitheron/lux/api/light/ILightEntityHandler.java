package com.zeitheron.lux.api.light;

import java.util.UUID;

import com.zeitheron.lux.api.event.GatherLightsEvent;

import net.minecraft.entity.Entity;

public interface ILightEntityHandler
{
	void createLights(Entity entity, GatherLightsEvent e);
	
	default void remove(int persistent)
	{
	}
	
	default void update(Entity entity)
	{
	}
}