package org.zeith.lux.client;

import com.zeitheron.hammercore.api.lighting.*;
import com.zeitheron.hammercore.client.render.shader.*;
import com.zeitheron.hammercore.client.utils.gl.GLBuffer;
import com.zeitheron.hammercore.client.utils.gl.shading.VariableShaderProgram;
import com.zeitheron.hammercore.utils.AABBUtils;
import com.zeitheron.hammercore.utils.java.itf.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.*;
import org.lwjgl.opengl.GL20;
import org.zeith.lux.ConfigCL;
import org.zeith.lux.api.comparators.ColoredLightComparator;
import org.zeith.lux.api.event.GatherLightsEvent;
import org.zeith.lux.luxpack.apis.LuxPackAPIv2;
import org.zeith.lux.proxy.ClientProxy;

import java.util.*;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.*;

public class ClientLightManager
{
	public static Vec3d cameraPos = Vec3d.ZERO;
	public static Frustum camera;
	public static ArrayList<ColoredLight> lights = new ArrayList<>();
	public static int debugLights, debugCulledLights, debugBytesOfData;
	public static final Comparator<ColoredLight> distComparator = ColoredLightComparator.byDistanceFrom(() -> cameraPos);
	public static boolean doLightSort = false;
	
	public static void uploadLightsUBO()
	{
		int shader = GlShaderStack.glsActiveProgram();
		int size = debugCulledLights = Math.min(ConfigCL.maxLights, debugLights = lights.size());
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "lightCount"), size);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "colMix"), ConfigCL.lightAddMode ? 1 : 0);
//		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "colMix"), 1);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "vanillaTracing"), 0);
		
		int segCount = getSegmentCount();
		for(int i = 0; i < segCount; ++i)
		{
			LightSegment seg = getSegment(i);
			if(seg != null)
			{
				GLBuffer glBuffer = seg.getUBO();
				glUniformBlockBinding(shader, glGetUniformBlockIndex(shader, "lightBuffer" + i), i);
				glBindBufferBase(glBuffer.bufferKind, i, glBuffer.buffer);
			}
		}
	}
	
	public static void uploadLightsUBO(VariableShaderProgram vrs)
	{
		Integer shader = vrs.getProgramId();
		if(shader == null) return;
		
		int size = debugCulledLights = Math.min(ConfigCL.maxLights, debugLights = lights.size());
		vrs.setUniform("lightCount", size);
		vrs.setUniform("colMix", ConfigCL.lightAddMode ? 1 : 0);
		vrs.setUniform("vanillaTracing", 0);
		
		int segCount = getSegmentCount();
		for(int i = 0; i < segCount; ++i)
		{
			LightSegment seg = getSegment(i);
			if(seg != null)
			{
				GLBuffer glBuffer = seg.getUBO();
				glUniformBlockBinding(shader, glGetUniformBlockIndex(shader, "lightBuffer" + i), i);
				glBindBufferBase(glBuffer.bufferKind, i, glBuffer.buffer);
			}
		}
	}
	
	private static Vec3d getCurrentPosition(Entity entity, float partialTicks)
	{
		return new Vec3d(
				entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
				entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks,
				entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
		);
	}
	
	public static GatherLightsEvent gatherLightsEvent(World world, ArrayList<ColoredLight> lights, float partialTicks)
	{
		return new GatherLightsEvent(world, lights, ConfigCL.maxRenderDistance, cameraPos, camera, partialTicks);
	}
	
	public static void gatherLights(World world)
	{
		lights.clear();
		
		if(world == null) return;
		
		Minecraft mc = Minecraft.getMinecraft();
		Entity cameraEntity = mc.getRenderViewEntity();
		float partialTicks = mc.getRenderPartialTicks();
		
		if(cameraEntity != null)
		{
			cameraPos = getCurrentPosition(cameraEntity, partialTicks);
			camera = new Frustum();
			camera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
		} else
		{
			if(cameraPos == null)
				cameraPos = new Vec3d(0, 0, 0);
			camera = null;
			return;
		}
		
		GatherLightsEvent event = gatherLightsEvent(world, lights, partialTicks);
		try
		{
			ColoredLightManager.generate(partialTicks).forEach(event::add);
		} catch(ConcurrentModificationException ignored)
		{
			// might cause flickering, don't care.
		}
		ClientProxy.EXISTING.values().forEach(m -> m.addLights(event));
		ClientProxy.EXISTING_ENTS.values().forEach(m -> m.addLights(event));
		MinecraftForge.EVENT_BUS.post(event);
		
		int maxDist = ConfigCL.maxRenderDistance;
		
		AxisAlignedBB area = new AxisAlignedBB(
				cameraPos.x - maxDist,
				cameraPos.y - maxDist,
				cameraPos.z - maxDist,
				cameraPos.x + maxDist,
				cameraPos.y + maxDist,
				cameraPos.z + maxDist
		);
		
		EntityEntry en;
		for(Entity e : world.getEntitiesWithinAABB(Entity.class, area))
			if((en = EntityRegistry.getEntry(e.getClass())) != null && e.isAddedToWorld())
			{
				if(e.isInvisibleToPlayer(mc.player))
					continue;
				TriConsumer<World, Entity, Consumer<ColoredLight>> consumer = LuxPackAPIv2.CUSTOM_ENTITY_LIGHTS.get(en);
				if(consumer != null) consumer.accept(world, e, event::add);
			}
		
		try
		{
			for(TileEntity t : world.loadedTileEntityList)
			{
				if(t == null || t.getPos() == null) // Man, mods are broken as fuck sometimes...
					continue;
				
				BlockPos pos = t.getPos();
				if(!AABBUtils.contains(area, pos))
					continue;
				
				if(world.isBlockLoaded(pos))
				{
					QuadConsumer<World, BlockPos, TileEntity, Consumer<ColoredLight>> consumer = LuxPackAPIv2.CUSTOM_TILE_LIGHTS.get(t.getClass());
					if(consumer != null) consumer.accept(world, pos, t, event::add);
				}
			}
		} catch(ConcurrentModificationException ignored)
		{
			// might cause flickering, don't care.
		}
		
		if(doLightSort)
			lights.sort(distComparator);
		
		int bytes = 0;
		int segCount = getSegmentCount();
		for(int i = 0; i < segCount; ++i)
		{
			LightSegment s = getSegment(i);
			if(s != null) bytes += s.refreshUBO();
		}
		debugBytesOfData = bytes * 4;
	}
	
	public static final List<LightSegment> lightSegments = new ArrayList<>();
	
	public static int getSegmentCount()
	{
		int lps = ShaderLimits.getMaxUniformBlockSize() / ColoredLight.BYTE_SIZE;
		int segments = Math.max(1, (int) Math.ceil(ClientProxy.UNIF_LIGHTS.getAsInt() / (double) lps));
		
		for(int i = 0; i < segments; ++i)
		{
			int start = lps * i;
			int end = start + lps;
			if(lightSegments.size() == i)
				lightSegments.add(new LightSegment(start, end));
		}
		
		return segments;
	}
	
	public static LightSegment getSegment(int i)
	{
		if(i >= 0 && i < lightSegments.size()) return lightSegments.get(i);
		return null;
	}
}