package com.zeitheron.lux.luxpack.apis.js;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class JSBlockPos extends BlockPos
{
	public JSBlockPos(int x, int y, int z)
	{
		super(x, y, z);
	}
	
	public JSBlockPos(double x, double y, double z)
	{
		super(x, y, z);
	}
	
	public JSBlockPos(Entity source)
	{
		super(source);
	}
	
	public JSBlockPos(Vec3d vec)
	{
		super(vec);
	}
	
	public JSBlockPos(Vec3i source)
	{
		super(source);
	}
	
	public int x()
	{
		return getX();
	}
	
	public int y()
	{
		return getY();
	}
	
	public int z()
	{
		return getZ();
	}
}