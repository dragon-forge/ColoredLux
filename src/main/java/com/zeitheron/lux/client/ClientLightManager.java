package com.zeitheron.lux.client;

import com.zeitheron.hammercore.api.lighting.ColoredLight;
import com.zeitheron.hammercore.api.lighting.ColoredLightManager;
import com.zeitheron.hammercore.client.render.shader.GlShaderStack;
import com.zeitheron.hammercore.client.utils.gl.GLBuffer;
import com.zeitheron.hammercore.utils.java.itf.QuadConsumer;
import com.zeitheron.hammercore.utils.java.itf.TriConsumer;
import com.zeitheron.lux.ConfigCL;
import com.zeitheron.lux.api.event.GatherLightsEvent;
import com.zeitheron.lux.api.light.ILightItem;
import com.zeitheron.lux.api.light.ILightProvider;
import com.zeitheron.lux.luxpack.apis.LuxPackAPIv2;
import com.zeitheron.lux.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.glGetUniformBlockIndex;
import static org.lwjgl.opengl.GL31.glUniformBlockBinding;

public class ClientLightManager
{
	public static Vec3d cameraPos;
	public static Frustum camera;
	public static ArrayList<ColoredLight> lights = new ArrayList<>();
	public static int debugLights, debugCulledLights;
	public static DistanceComparator distComparator = new DistanceComparator();

	@Deprecated
	public static void uploadLights()
	{
		int shader = GlShaderStack.glsActiveProgram();

		int size = debugCulledLights = Math.min(ConfigCL.maxLights, lights.size());
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "lightCount"), size);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "colMix"), ConfigCL.lightAddMode ? 1 : 0);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "vanillaTracing"), 0);
		debugLights = lights.size();

		for(int i = 0; i < size; i++)
		{
			if(i < lights.size())
			{
				ColoredLight l = lights.get(i);
				GL20.glUniform3f(GlShaderStack.glsGetActiveUniformLoc("lights[" + i + "].position"), l.x, l.y, l.z);
				GL20.glUniform4f(GlShaderStack.glsGetActiveUniformLoc("lights[" + i + "].color"), l.r, l.g, l.b, l.a);
				GL20.glUniform1f(GlShaderStack.glsGetActiveUniformLoc("lights[" + i + "].radius"), l.radius);
			}
		}
	}

	public static void uploadLightsUBO()
	{
		int shader = GlShaderStack.glsActiveProgram();
		int size = debugCulledLights = Math.min(ConfigCL.maxLights, debugLights = lights.size());
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "lightCount"), size);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "colMix"), ConfigCL.lightAddMode ? 1 : 0);
		GL20.glUniform1i(GL20.glGetUniformLocation(shader, "vanillaTracing"), 0);

//		if(segment == null) segment = new LightSegment(0, 2048);
//		GLBuffer glBuffer = segment.getUBO();
//		segment.getUBO().bindToShader(shader, 0, "lightBuffer0");
//
//		int UBO_TRANSFORM_INDEX = 0;
//
//		int bufIdx = glGetUniformBlockIndex(shader, "lightBuffer1");
//		glUniformBlockBinding(shader, bufIdx, UBO_TRANSFORM_INDEX);
//		glBindBufferBase(glBuffer.bufferKind, UBO_TRANSFORM_INDEX, glBuffer.buffer);
//
//		UBO_TRANSFORM_INDEX = 1;
//
//		bufIdx = glGetUniformBlockIndex(shader, "lightBuffer1");
//		glUniformBlockBinding(shader, bufIdx, UBO_TRANSFORM_INDEX);
//		glBindBufferBase(glBuffer.bufferKind, UBO_TRANSFORM_INDEX, glBuffer.buffer);

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
		return new Vec3d(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks, entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks, entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks);
	}

	public static void update(World world)
	{
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

		GatherLightsEvent event = new GatherLightsEvent(lights, ConfigCL.maxRenderDistance, cameraPos, camera, partialTicks);
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

		for(Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(cameraPos.x - maxDist, cameraPos.y - maxDist, cameraPos.z - maxDist, cameraPos.x + maxDist, cameraPos.y + maxDist, cameraPos.z + maxDist)))
		{
			if(e.getPositionVector().distanceTo(cameraPos) >= maxDist)
				continue;
			if(e instanceof ILightProvider)
				((ILightProvider) e).addLights(world, event);
			if(e instanceof EntityItem)
			{
				Item i;
				if((i = ((EntityItem) e).getItem().getItem()) instanceof ILightItem)
					((ILightItem) i).addLightsAsEntity((EntityItem) e, event);
			}
			if(e instanceof EntityLivingBase)
			{
				EntityLivingBase base = (EntityLivingBase) e;
				for(EntityEquipmentSlot slot : EntityEquipmentSlot.values())
				{
					ItemStack stack = base.getItemStackFromSlot(slot);
					if(!stack.isEmpty() && stack.getItem() instanceof ILightItem)
						((ILightItem) stack.getItem()).addLightsAsHeld(base, stack, slot, event);
				}
			}
			EntityEntry en = EntityRegistry.getEntry(e.getClass());
			if(en != null)
			{
				TriConsumer<World, Entity, Consumer<ColoredLight>> consumer = LuxPackAPIv2.CUSTOM_ENTITY_LIGHTS.get(en);
				if(consumer != null) consumer.accept(world, e, event::add);
			}
		}

		for(TileEntity t : world.loadedTileEntityList)
		{
			if(Math.sqrt(t.getPos().distanceSqToCenter(cameraPos.x, cameraPos.y, cameraPos.z)) >= ConfigCL.maxRenderDistance)
				continue;
			if(world.isBlockLoaded(t.getPos()))
			{
				if(t instanceof ILightProvider)
					((ILightProvider) t).addLights(world, event);

				QuadConsumer<World, BlockPos, TileEntity, Consumer<ColoredLight>> consumer = LuxPackAPIv2.CUSTOM_TILE_LIGHTS.get(t.getClass());
				if(consumer != null) consumer.accept(world, t.getPos(), t, event::add);
			}
		}

		lights.sort(distComparator);

		int segCount = getSegmentCount();
		for(int i = 0; i < segCount; ++i)
		{
			LightSegment s = getSegment(i);
			if(s != null) s.refreshUBO();
		}

//		if(segment == null) segment = new LightSegment(0, 2048);
//		segment.refreshUBO();
	}

	public static LightSegment segment;
	public static final List<LightSegment> lightSegments = new ArrayList<>();

	public static int getSegmentCount()
	{
		int lps = GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BLOCK_SIZE) / ColoredLight.FLOAT_SIZE / 4;
		int segments = Math.max(1, (int) Math.ceil(ClientProxy.UNIF_LIGHTS.getAsInt() / (double) lps));
		for(int i = 0; i < segments; ++i)
		{
			int start = lps * i;
			int end = start + lps;
			if(lightSegments.size() == i) lightSegments.add(new LightSegment(start, end));
		}
		return segments;
	}

	public static LightSegment getSegment(int i)
	{
		if(i >= 0 && i < lightSegments.size()) return lightSegments.get(i);
		return null;
	}

	public static class DistanceComparator
			implements Comparator<ColoredLight>
	{
		@Override
		public int compare(ColoredLight a, ColoredLight b)
		{
			double dist1 = cameraPos.squareDistanceTo(a.x, a.y, a.z);
			double dist2 = cameraPos.squareDistanceTo(b.x, b.y, b.z);
			return Double.compare(dist1, dist2);
		}
	}

	public static void clear()
	{
		lights.clear();
	}
}