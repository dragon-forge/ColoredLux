package com.zeitheron.lux.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zeitheron.hammercore.api.lighting.LightingBlacklist;
import com.zeitheron.hammercore.utils.FastNoise;
import com.zeitheron.lux.api.light.ILightBlockHandler;
import com.zeitheron.lux.api.light.ILightEntityHandler;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.EntityEntry;

public class LuxManager
{
	public static final Map<Block, ILightBlockHandler> BLOCK_LUMINANCES = new HashMap<>();
	public static final Map<EntityEntry, ILightEntityHandler> ENTITY_LUMINANCES = new HashMap<>();
	
	public static float generateFlickering(double x, double z)
	{
		return FastNoise.noise(x, z + (System.currentTimeMillis() % 72000L) / 2000F, 6) / 127F;
	}
	
	public static void registerBlockLight(Block blk, ILightBlockHandler handler)
	{
		BLOCK_LUMINANCES.put(blk, handler);
	}
	
	public static void registerEntityLight(EntityEntry ent, ILightEntityHandler handler)
	{
		ENTITY_LUMINANCES.put(ent, handler);
	}
	
	public static void registerShadedEntity(Class<? extends Entity> blacklist)
	{
		LightingBlacklist.registerShadedEntity(blacklist);
	}
	
	public static void registerShadedTile(Class<? extends TileEntity> blacklist)
	{
		LightingBlacklist.registerShadedTile(blacklist);
	}
	
	public static List<Class<? extends TileEntity>> getTileBlacklist()
	{
		return LightingBlacklist.getTileBlacklist();
	}
	
	public static List<Class<? extends Entity>> getEntitiesBlacklist()
	{
		return LightingBlacklist.getEntitiesBlacklist();
	}
	
	public static boolean blocksShader(TileEntity tile)
	{
		return LightingBlacklist.blocksShader(tile);
	}
	
	public static boolean blocksShader(Entity entity)
	{
		return LightingBlacklist.blocksShader(entity);
	}
}