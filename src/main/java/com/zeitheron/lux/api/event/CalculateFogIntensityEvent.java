package com.zeitheron.lux.api.event;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import net.minecraft.world.World;

public class CalculateFogIntensityEvent extends WorldEvent
{
	private float value;
	
	public CalculateFogIntensityEvent(World world, float value)
	{
		super(world);
		this.value = value;
	}
	
	public float getValue()
	{
		return value;
	}
	
	public void setValue(float value)
	{
		this.value = value;
	}
}