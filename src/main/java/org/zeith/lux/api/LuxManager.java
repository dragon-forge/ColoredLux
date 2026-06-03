package org.zeith.lux.api;

import com.zeitheron.hammercore.api.lighting.*;
import com.zeitheron.hammercore.api.lighting.impl.*;
import com.zeitheron.hammercore.utils.FastNoise;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.*;
import org.zeith.lux.ColoredLux;
import org.zeith.lux.api.comparators.ColoredLightComparator;
import org.zeith.lux.api.event.*;
import org.zeith.lux.api.light.*;
import org.zeith.lux.client.ClientLightManager;
import org.zeith.lux.client.json.*;

import javax.annotation.Nullable;
import java.util.*;

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
		
		ForgeRegistries.BLOCKS.getValuesCollection().stream()
				.filter(IGlowingBlock.class::isInstance)
				.forEach(blk ->
				{
					IGlowingBlock glow = (IGlowingBlock) blk;
					registerBlockLight(blk, (world, pos, state, event) -> event.add(glow.produceColoredLight(world, pos, state, 1F)));
				});
		
		MinecraftForge.EVENT_BUS.post(new ReloadLuxManagerEvent());
	}
	
	/**
	 * Gets the brightest perceived light from a given block.
	 */
	public static Optional<ColoredLight> getLight(World world, BlockPos pos, IBlockState state, @Nullable TileEntity tile, float partialTicks)
	{
		ArrayList<ColoredLight> lights = getLights(world, pos, state, tile, partialTicks);
		
		// this way, the brightest light will always be last in the list
		lights.sort(ColoredLightComparator.ESTIMATE_BRIGHTNESS);
		
		return lights.isEmpty() ? Optional.empty() : Optional.of(lights.get(lights.size() - 1));
	}
	
	/**
	 * Gets all lights from a given block.
	 */
	public static ArrayList<ColoredLight> getLights(World world, BlockPos pos, IBlockState state, @Nullable TileEntity tile, float partialTicks)
	{
		ArrayList<ColoredLight> lights = new ArrayList<>();
		
		if(tile instanceof IGlowingEntity)
		{
			ColoredLight light = ((IGlowingEntity) tile).produceColoredLight(partialTicks);
			if(light != null) lights.add((light));
		}
		
		if(state.getBlock() instanceof IGlowingBlock)
		{
			ColoredLight light = ((IGlowingBlock) state.getBlock()).produceColoredLight(world, pos, state, partialTicks);
			if(light != null) lights.add((light));
		}
		
		if(BLOCK_LUMINANCES.containsKey(state.getBlock()))
		{
			GatherLightsEvent e = ClientLightManager.gatherLightsEvent(world, lights, partialTicks);
			BLOCK_LUMINANCES.get(state.getBlock()).createLights(world, pos, state, e);
		}
		
		return lights;
	}
	
	public static ColoredLight.Builder unbuild(ColoredLight light)
	{
		return new ColoredLight.Builder().radius(light.radius).color(light.r, light.g, light.b, light.a)
				.pos(light.x, light.y, light.z);
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