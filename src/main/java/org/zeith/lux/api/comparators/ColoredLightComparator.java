package org.zeith.lux.api.comparators;

import com.zeitheron.hammercore.api.lighting.ColoredLight;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.function.Supplier;

public enum ColoredLightComparator
		implements Comparator<ColoredLight>
{
	RADIUS((a, b) -> Float.compare(a.radius, b.radius)),
	LUMINANCE((a, b) -> Float.compare(luminance(a.r, a.g, a.b), luminance(b.r, b.g, b.b))),
	ESTIMATE_BRIGHTNESS((a, b) -> Float.compare(luminance(a.r, a.g, a.b) * a.radius, luminance(b.r, b.g, b.b) * b.radius));

	final Comparator<ColoredLight> $;

	ColoredLightComparator(Comparator<ColoredLight> $)
	{
		this.$ = $;
	}

	@Override
	public int compare(ColoredLight o1, ColoredLight o2)
	{
		return $.compare(o1, o2);
	}

	public static Comparator<ColoredLight> byDistanceFrom(Vec3d point)
	{
		return (a, b) ->
		{
			double dist1 = point.squareDistanceTo(a.x, a.y, a.z);
			double dist2 = point.squareDistanceTo(b.x, b.y, b.z);
			return Double.compare(dist1, dist2);
		};
	}

	public static Comparator<ColoredLight> byDistanceFrom(Supplier<Vec3d> point)
	{
		return (a, b) ->
		{
			Vec3d vec = point.get();
			double dist1 = vec.squareDistanceTo(a.x, a.y, a.z);
			double dist2 = vec.squareDistanceTo(b.x, b.y, b.z);
			return Double.compare(dist1, dist2);
		};
	}

	static float luminance(float R, float G, float B)
	{
		return MathHelper.sqrt(0.299F * R * R + 0.587F * G * G + 0.114F * B * B);
	}
}