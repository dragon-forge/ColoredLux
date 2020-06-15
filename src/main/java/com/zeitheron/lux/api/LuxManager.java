package com.zeitheron.lux.api;

import com.google.common.base.Predicates;
import com.zeitheron.hammercore.api.lighting.LightingBlacklist;
import com.zeitheron.hammercore.api.lighting.impl.IGlowingBlock;
import com.zeitheron.hammercore.utils.FastNoise;
import com.zeitheron.lux.ColoredLux;
import com.zeitheron.lux.api.event.ReloadLuxManagerEvent;
import com.zeitheron.lux.api.light.ILightBlockHandler;
import com.zeitheron.lux.api.light.ILightEntityHandler;
import com.zeitheron.lux.client.json.JsonBlockLights;
import com.zeitheron.lux.client.json.JsonEntityLights;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class LuxManager
{
	public static final Map<Block, ILightBlockHandler> BLOCK_LUMINANCES = new HashMap<>();
	public static final Map<EntityEntry, ILightEntityHandler> ENTITY_LUMINANCES = new HashMap<>();

	public static void reload()
	{
		BLOCK_LUMINANCES.clear();
		ENTITY_LUMINANCES.clear();

		JsonBlockLights.reload();
		JsonEntityLights.reload();
		ColoredLux.proxy.reloadLuxManager();

		ForgeRegistries.BLOCKS.getValuesCollection().stream() //
				.filter(Predicates.instanceOf(IGlowingBlock.class)) //
				.forEach(blk ->
				{
					IGlowingBlock glow = (IGlowingBlock) blk;
					registerBlockLight(blk, (world, pos, state, event) -> event.add(glow.produceColoredLight(world, pos, state, 1F)));
				});

		MinecraftForge.EVENT_BUS.post(new ReloadLuxManagerEvent());
	}

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

	public static boolean blocksShader(TileEntity tile)
	{
		return LightingBlacklist.blocksShader(tile);
	}

	public static boolean blocksShader(Entity entity)
	{
		return LightingBlacklist.blocksShader(entity);
	}
}