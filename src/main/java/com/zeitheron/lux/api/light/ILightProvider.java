package com.zeitheron.lux.api.light;

import com.zeitheron.lux.api.event.GatherLightsEvent;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface ILightProvider
{
	void addLights(World world, GatherLightsEvent event);
}