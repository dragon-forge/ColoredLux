package org.zeith.lux.api.light;

import net.minecraft.entity.Entity;
import org.zeith.lux.api.event.GatherLightsEvent;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;

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
		public final WeakReference<Entity> entity;
		
		@Nonnull
		protected ILightEntityHandler handler;
		
		public Wrapper(Entity entity, @Nonnull ILightEntityHandler handler)
		{
			this.entity = new WeakReference<>(entity);
			this.handler = handler;
			this.handler.update(entity);
		}
		
		
		public void addLights(GatherLightsEvent e)
		{
			Entity ent = entity.get();
			if(ent != null)
				handler.createLights(ent, e);
		}
		
		public void remove(int id)
		{
			handler.remove(id);
		}
	}
}