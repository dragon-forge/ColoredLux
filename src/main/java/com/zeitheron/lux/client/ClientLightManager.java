package com.zeitheron.lux.client;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import com.zeitheron.hammercore.api.lighting.ColoredLight;
import com.zeitheron.hammercore.api.lighting.ColoredLightManager;
import com.zeitheron.hammercore.client.render.shader.GlShaderStack;
import com.zeitheron.lux.ConfigCL;
import com.zeitheron.lux.api.event.GatherLightsEvent;
import com.zeitheron.lux.api.light.ILightItem;
import com.zeitheron.lux.api.light.ILightProvider;
import com.zeitheron.lux.api.light.Light;
import com.zeitheron.lux.proxy.ClientProxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class ClientLightManager
{
	public static Vec3d cameraPos;
	public static ICamera camera;
	public static ArrayList<ColoredLight> lights = new ArrayList<>();
	public static int debugLights, debugCulledLights;
	public static DistanceComparator distComparator = new DistanceComparator();
	
	public static void uploadLights()
	{
		int size = debugCulledLights = Math.min(ConfigCL.maxLights, lights.size());
		GL20.glUniform1i(GlShaderStack.glsGetActiveUniformLoc("lightCount"), size);
		GL20.glUniform1i(GlShaderStack.glsGetActiveUniformLoc("colMix"), ConfigCL.lightAddMode ? 1 : 0);
		GL20.glUniform1i(GlShaderStack.glsGetActiveUniformLoc("vanillaTracing"), 0);
		debugLights = lights.size();
		
		int program = GlShaderStack.glsActiveProgram();
		int uniformBlockLight = GL31.glGetUniformBlockIndex(program, "LightBlock");
//		GL31.glUniformBlockBinding(program, uniformBlockLight, 0);
		UBO();
		
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
	
	private static Integer lightUBO;
	
	private static int uboSize;
	private static FloatBuffer uboData;
	
	private static FloatBuffer updateUBO()
	{
		if(uboData == null || uboSize < lights.size())
		{
			uboSize = lights.size();
			uboData = BufferUtils.createFloatBuffer(uboSize * Light.FLOAT_SIZE);
		}
		uboData.clear();
		for(ColoredLight l : lights)
			for(int i = 0; i < 8; ++i)
				uboData.put(Light.get(l, i));
		uboData.flip();
		return uboData;
	}
	
	private static void createUBO()
	{
		if(lightUBO != null)
			return;
		
		lightUBO = GL15.glGenBuffers();
		
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, lightUBO);
		
		GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, updateUBO(), GL15.GL_STREAM_DRAW);
		
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
		
		GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, 0, lightUBO, 0, uboSize);
	}
	
	private static void UBO()
	{
		createUBO();
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, lightUBO);
		GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, updateUBO(), GL15.GL_STREAM_DRAW);
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
		
		GatherLightsEvent event = new GatherLightsEvent(lights, ConfigCL.maxDistance, cameraPos, camera, partialTicks);
		ColoredLightManager.generate(partialTicks).forEach(event::add);
		ClientProxy.EXISTING.values().forEach(m -> m.addLights(event));
		ClientProxy.EXISTING_ENTS.values().forEach(m -> m.addLights(event));
		MinecraftForge.EVENT_BUS.post(event);
		
		int maxDist = ConfigCL.maxDistance;
		
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
		}
		
		for(TileEntity t : world.loadedTileEntityList)
		{
			if(Math.sqrt(t.getPos().distanceSqToCenter(cameraPos.x, cameraPos.y, cameraPos.z)) >= ConfigCL.maxDistance)
				continue;
			if(world.isBlockLoaded(t.getPos()))
				if(t instanceof ILightProvider)
					((ILightProvider) t).addLights(world, event);
		}
		
		lights.sort(distComparator);
	}
	
	public static class DistanceComparator implements Comparator<ColoredLight>
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