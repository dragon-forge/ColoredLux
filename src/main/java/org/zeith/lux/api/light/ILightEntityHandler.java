package org.zeith.lux.api.light;

import net.minecraft.entity.Entity;
import org.zeith.lux.api.event.GatherLightsEvent;

public interface ILightEntityHandler
{
	void createLights(Entity entity, GatherLightsEvent e);
	
	default void remove(int persistent)
	{
	}
	
	default void update(Entity entity)
	{
	}

	class Wrapper
	{
		public final Entity entity;

		public Wrapper(Entity entity, ILightEntityHandler handler)
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
}