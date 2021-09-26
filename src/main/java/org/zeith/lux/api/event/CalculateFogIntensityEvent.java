package org.zeith.lux.api.event;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

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