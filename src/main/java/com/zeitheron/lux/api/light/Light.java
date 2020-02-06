package com.zeitheron.lux.api.light;

import java.awt.Color;

import com.zeitheron.hammercore.api.lighting.ColoredLight;
import com.zeitheron.hammercore.utils.color.ColorHelper;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Light
{
	public static final int FLOAT_SIZE = 3 + 4 + 1;
	
	public float x, y, z;
	public float r, g, b, a;
	public float rad;
	
	protected ColoredLight wrapper;
	
	public Light(float x, float y, float z, float r, float g, float b, float a, float rad)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		this.rad = rad;
		this.wrapper = new ColoredLight(x, y, z, r, g, b, a, rad);
	}
	
	public ColoredLight getWrapper()
	{
		wrapper.x = x;
		wrapper.y = y;
		wrapper.z = z;
		wrapper.r = r;
		wrapper.g = g;
		wrapper.b = b;
		wrapper.a = a;
		wrapper.radius = rad;
		return wrapper;
	}
	
	public static float get(ColoredLight l, int i)
	{
		if(i == 0)
			return l.r;
		if(i == 1)
			return l.g;
		if(i == 2)
			return l.b;
		if(i == 3)
			return l.a;
		if(i == 4)
			return l.x;
		if(i == 5)
			return l.y;
		if(i == 6)
			return l.z;
		if(i == 7)
			return l.radius;
		return Float.NaN;
	}
	
	public float get(int i)
	{
		if(i == 0)
			return x;
		if(i == 1)
			return y;
		if(i == 2)
			return z;
		if(i == 3)
			return r;
		if(i == 4)
			return g;
		if(i == 5)
			return b;
		if(i == 6)
			return a;
		if(i == 7)
			return rad;
		return Float.NaN;
	}
	
	public static class Builder
	{
		private float x = Float.NaN, y = Float.NaN, z = Float.NaN;
		private float r = 1F, g = 1F, b = 1F, a = 1F;
		private float rad = Float.NaN;
		
		private Builder()
		{
		}
		
		public Builder pos(Vec3d pos)
		{
			return pos((float) pos.x, (float) pos.y, (float) pos.z);
		}
		
		public Builder pos(Vec3i pos)
		{
			return pos(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
		}
		
		public Builder pos(Entity entity, float partialTime)
		{
			return pos((float) (entity.prevPosX + (entity.posX - entity.prevPosX) * partialTime), //
			        (float) (entity.prevPosY + (entity.posY - entity.prevPosY) * partialTime), //
			        (float) (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTime));
		}
		
		public Builder pos(float x, float y, float z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}
		
		public Builder radius(float rad)
		{
			this.rad = rad;
			return this;
		}
		
		public Builder color(Color color)
		{
			return color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
		}
		
		public Builder color(int rgba, boolean alpha)
		{
			return color(ColorHelper.getRed(rgba), ColorHelper.getGreen(rgba), ColorHelper.getBlue(rgba), alpha ? ColorHelper.getAlpha(rgba) : 1F);
		}
		
		public Builder color(int rgba)
		{
			return color(ColorHelper.getRed(rgba), ColorHelper.getGreen(rgba), ColorHelper.getBlue(rgba), ColorHelper.getAlpha(rgba));
		}
		
		public Builder color(int red, int green, int blue)
		{
			return color(red / 255F, green / 255F, blue / 255F, 1F);
		}
		
		public Builder color(int red, int green, int blue, int alpha)
		{
			return color(red / 255F, green / 255F, blue / 255F, alpha / 255F);
		}
		
		public Builder alpha(int alpha)
		{
			return alpha(alpha / 255F);
		}
		
		public Builder alpha(float alpha)
		{
			this.a = alpha;
			return this;
		}
		
		public Builder color(float red, float green, float blue)
		{
			return color(red, green, blue, 1F);
		}
		
		public Builder color(float red, float green, float blue, float alpha)
		{
			this.r = red;
			this.g = green;
			this.b = blue;
			this.a = alpha;
			return this;
		}
		
		public Light build()
		{
			if(Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
				throw new IllegalArgumentException("Position has not been set.");
			if(Float.isNaN(rad))
				throw new IllegalArgumentException("Radius has not been set.");
			return new Light(x, y, z, r, g, b, a, rad);
		}
	}
	
	public static Light.Builder builder()
	{
		return new Builder();
	}
}