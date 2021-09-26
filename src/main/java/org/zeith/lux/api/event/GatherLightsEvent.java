package org.zeith.lux.api.event;

import com.google.common.collect.ImmutableList;
import com.zeitheron.hammercore.api.lighting.ColoredLight;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.zeith.lux.ConfigCL;

import java.util.ArrayList;
import java.util.stream.Stream;

public class GatherLightsEvent
		extends Event
{
	private final ArrayList<ColoredLight> lights;
	private final float maxDistance;
	private final Vec3d cameraPosition;
	private final Frustum camera;
	private final float partialTicks;

	private double furthest;

	public GatherLightsEvent(ArrayList<ColoredLight> lights, float maxDistance, Vec3d cameraPosition, Frustum camera, float partialTicks)
	{
		this.lights = lights;
		this.maxDistance = maxDistance;
		this.cameraPosition = cameraPosition;
		this.camera = camera;
		this.partialTicks = partialTicks;
	}

	public float getPartialTicks()
	{
		return partialTicks;
	}

	public ImmutableList<ColoredLight> getLightList()
	{
		return ImmutableList.copyOf(lights);
	}

	public float getMaxDistance()
	{
		return maxDistance;
	}

	public Vec3d getCameraPosition()
	{
		return cameraPosition;
	}

	public ICamera getCamera()
	{
		return camera;
	}

	public void addAll(Stream<ColoredLight> lights)
	{
		lights.forEach(this::add);
	}

	public void addAll(Iterable<ColoredLight> lights)
	{
		lights.forEach(this::add);
	}

	public void add(ColoredLight light)
	{
		Integer rem = null;
		if(light == null)
			return;
		if(light.a <= 0F)
			return;
		float radius = light.radius;

		Float dist = null;
		if(cameraPosition != null)
		{
			dist = MathHelper.sqrt(cameraPosition.squareDistanceTo(light.x, light.y, light.z));
			if(dist > radius + maxDistance) return;
		}

		if(camera != null && !camera.isBoxInFrustum(light.x - radius, light.y - radius, light.z - radius, light.x + radius, light.y + radius, light.z + radius))
			return;

		// New optimization node: prevents furthest lights from being added when over the limit. (reduce sorting time)
		if(dist != null)
		{
			if(lights.size() >= ConfigCL.maxLights && dist - radius > furthest) return;
			if(dist - radius > furthest) furthest = Math.max(dist - radius, furthest);
		}

		lights.add(light);
	}

	public void add(ColoredLight.Builder builder)
	{
		if(builder == null)
			return;
		add(builder.build());
	}

	@Override
	public boolean isCancelable()
	{
		return false;
	}
}