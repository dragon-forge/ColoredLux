package com.zeitheron.lux.api.event;

import com.zeitheron.lux.luxpack.AbstractLuxPack;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.io.File;

public class CreateLuxPackEvent
		extends Event
{
	public final File location;
	private AbstractLuxPack pack;

	public CreateLuxPackEvent(File location)
	{
		this.location = location;
	}

	public void setPack(AbstractLuxPack pack)
	{
		this.pack = pack;
	}

	public AbstractLuxPack getPack()
	{
		return pack;
	}
}