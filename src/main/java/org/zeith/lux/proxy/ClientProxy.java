package org.zeith.lux.proxy;

import com.zeitheron.hammercore.api.events.*;
import com.zeitheron.hammercore.api.lighting.*;
import com.zeitheron.hammercore.client.render.shader.GlShaderStack;
import com.zeitheron.hammercore.client.utils.gl.shading.ShaderSource;
import com.zeitheron.hammercore.client.utils.gl.shading.VariableShaderProgram;
import com.zeitheron.hammercore.utils.ReflectionUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.EnumHelperClient;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.zeith.lux.ColoredLux;
import org.zeith.lux.ConfigCL;
import org.zeith.lux.api.HWSupport;
import org.zeith.lux.api.LuxManager;
import org.zeith.lux.api.event.CalculateFogIntensityEvent;
import org.zeith.lux.api.light.ILightBlockHandler;
import org.zeith.lux.api.light.ILightEntityHandler;
import org.zeith.lux.client.*;
import org.zeith.lux.client.commands.CommandLux;
import org.zeith.lux.client.json.JsonBlockLights;
import org.zeith.lux.client.json.JsonEntityLights;
import org.zeith.lux.luxpack.LuxPackRepository;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.IntSupplier;

public class ClientProxy
		extends CommonProxy
{
	public static final BUD BLOCK_UPDATE_DETECTOR = new BUD();
	public static final Map<BlockPos, ILightBlockHandler.LightBlockWrapper> EXISTING = Collections.synchronizedMap(new HashMap<>());
	public static final Map<Integer, ILightEntityHandler.Wrapper> EXISTING_ENTS = Collections.synchronizedMap(new HashMap<>());
	private static int ticks;
	public static VariableShaderProgram terrainProgram;
	public static VariableShaderProgram entityProgram;
	public static VariableShaderProgram guiProgram;
	public static boolean isGui = false;
	static final int runtimeCores = Runtime.getRuntime().availableProcessors();
	static final int lightTPSDivisor = runtimeCores <= 4 ? 8 : runtimeCores <= 8 ? 4 : 2;
	boolean postedLights = false;
	boolean precedesEntities = true;
	float fogIntensity;
	String section = "";
	Thread thread;
	private static int maxSessionLights = 1;
	public static final IntSupplier UNIF_LIGHTS = () ->
	{
		int cl = ClientLightManager.debugLights;
		if(ConfigCL.minLights > maxSessionLights)
			maxSessionLights = ConfigCL.minLights;
		if(cl > maxSessionLights)
			maxSessionLights = Math.max(maxSessionLights, Math.min(ConfigCL.maxLights, 2 * cl));
		return maxSessionLights;
	};
	public static final List<Options> customOptions = new ArrayList<>();
	public static final Options LUX_ENABLE_LIGHTING = EnumHelperClient.addOptions("LUX_ENABLE_LIGHTING", "options.lux:lighting", false, true);
	public static final Options LUX_ENABLE_FOG = EnumHelperClient.addOptions("LUX_ENABLE_FOG", "options.lux:fog", false, true);
	public static final Options LUX_PACKS = EnumHelperClient.addOptions("LUX_LUXPACKS", "options.lux:packs", false, true);
	public static final String GPU;

	static
	{
		GPU = "???" + File.separator + "???";
	}

	@Override
	public void preInit(FMLPreInitializationEvent e)
	{
		ReflectionUtil.setStaticFinalField(ClientProxy.class, "GPU", GL11.glGetString(GL11.GL_RENDERER));
		ProfilerEndStartEvent.enable();
		RenderEntityEvent.enable();
		RenderTileEntityEvent.enable();
		PreRenderChunkEvent.enable();
		MinecraftForge.EVENT_BUS.register(this);
		ColoredLightManager.UNIFORM_LIGHT_COUNT = UNIF_LIGHTS;

		ColoredLux.LOG.info("Found " + runtimeCores + " available processing threads. The light update frequency will be max(FPS/" + lightTPSDivisor + ", 1) Hz");

		customOptions.add(LUX_ENABLE_LIGHTING);
		customOptions.add(LUX_PACKS);
		customOptions.add(LUX_ENABLE_FOG);

		if(OptifineInstalled) for(Field f : GuiPerformanceSettingsOF.getDeclaredFields())
			if(Options[].class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) try
			{
				f.setAccessible(true);
				Options[] videoSettings = (Options[]) f.get(null);
				List<Options> got = new ArrayList<>(Arrays.asList(videoSettings));
				got.addAll(customOptions);
				if(Modifier.isFinal(f.getModifiers()))
					ReflectionUtil.setStaticFinalField(f, got.toArray(new Options[0]));
				else f.set(null, got.toArray(new Options[0]));
			} catch(IllegalArgumentException | IllegalAccessException e1)
			{
				e1.printStackTrace();
			}
			else ;
		else for(Field f : GuiVideoSettings.class.getDeclaredFields())
			if(Options[].class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) try
			{
				f.setAccessible(true);
				Options[] videoSettings = (Options[]) f.get(null);
				List<Options> got = new ArrayList<>(Arrays.asList(videoSettings));
				customOptions.forEach(o -> got.add(got.indexOf(Options.USE_VBO), o));
				ReflectionUtil.setStaticFinalField(f, got.toArray(new Options[0]));
			} catch(IllegalArgumentException | IllegalAccessException e1)
			{
				e1.printStackTrace();
			}

		File cfg = e.getSuggestedConfigurationFile();
		cfg = new File(cfg.getAbsolutePath().substring(0, cfg.getAbsolutePath().lastIndexOf(".")));
		if(!cfg.isDirectory())
			cfg.mkdirs();

		File old = new File(cfg, "lights.json");
		if(old.isFile())
			old.renameTo(new File(cfg, "lights-block.json"));
		JsonBlockLights.setup(new File(cfg, "lights-block.json"));
		JsonEntityLights.setup(new File(cfg, "lights-entity.json"));
		LuxPackRepository.getInstance().setup(new File(cfg, "luxpacks.json"));

		ColoredLightManager.registerOperator(() -> ConfigCL.enableColoredLighting, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				ClientLightManager.uploadLightsUBO();
				return true;
			}
			return false;
		}, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				ClientProxy.terrainProgram.bindShader();
				return true;
			}
			return false;
		}, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				ClientProxy.entityProgram.bindShader();
				return true;
			}
			return false;
		}, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				ClientProxy.terrainProgram.unbindShader();
				return true;
			}
			return false;
		}, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				ClientProxy.entityProgram.unbindShader();
				return true;
			}
			return false;
		});

		ClientCommandHandler.instance.registerCommand(new CommandLux());

		HWSupport.EnumShaderVersion shaderVersionEnum = HWSupport.getShaderVersionToLoad(GPU);
		String shaderVersion = shaderVersionEnum.getId();
		String shaders = "shaders/" + shaderVersion + "/";

		ColoredLux.LOG.info("----------------- Colored Lux Info -----------------");
		ColoredLux.LOG.info("Using shaders at: " + shaders);
		ColoredLux.LOG.info("Vendor compat: " + HWSupport.getCardCompatMessage(GPU));
		ColoredLux.LOG.info("----------------------------------------------------");

		ClientProxy.terrainProgram = new VariableShaderProgram()
				.id(new ResourceLocation("lux", "terrain"))
				.addVariable(new ShaderLightingVariable("getLight", "Light"))
				.linkFragmentSource(new ShaderSource(new ResourceLocation("lux", shaders + "terrain.fsh")))
				.linkVertexSource(new ShaderSource(new ResourceLocation("lux", shaders + "terrain.vsh")))
				.onCompilationFailed(VariableShaderProgram.ToastCompilationErrorHandler.INSTANCE)
				.onCompilationFailed(prog ->
				{
					ConfigCL.cfgs.get("Client-Side", "Colored Lighting", true).set(ConfigCL.enableColoredLighting = false);
					ConfigCL.cfgs.save();
				})
				.doGLLog(false)
				.subscribe4Events();

		ClientProxy.entityProgram = new VariableShaderProgram()
				.id(new ResourceLocation("lux", "entity"))
				.addVariable(new ShaderLightingVariable("getLight", "Light"))
				.linkFragmentSource(new ShaderSource(new ResourceLocation("lux", shaders + "entities.fsh")))
				.linkVertexSource(new ShaderSource(new ResourceLocation("lux", shaders + "entities.vsh")))
				.onCompilationFailed(VariableShaderProgram.ToastCompilationErrorHandler.INSTANCE)
				.onCompilationFailed(prog ->
				{
					ConfigCL.cfgs.get("Client-Side", "Colored Lighting", true).set(ConfigCL.enableColoredLighting = false);
					ConfigCL.cfgs.save();
				})
				.doGLLog(false)
				.subscribe4Events();

		ClientProxy.guiProgram = new VariableShaderProgram()
				.id(new ResourceLocation("lux", "gui"))
				.addVariable(new ShaderLightingVariable("getLight", "Light"))
				.linkFragmentSource(new ShaderSource(new ResourceLocation("lux", "shaders/gui/.fsh")))
				.linkVertexSource(new ShaderSource(new ResourceLocation("lux", "shaders/gui/.vsh")))
				.onCompilationFailed(VariableShaderProgram.ToastCompilationErrorHandler.INSTANCE)
				.doGLLog(false)
				.subscribe4Events();
	}

	@Override
	public void reloadLuxManager()
	{
		EXISTING.clear();
		EXISTING_ENTS.clear();
		LuxPackRepository.getInstance().reload();
	}

	public static final List<ColoredLight> lights = new ArrayList<>();

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void lightUpload(LightUniformEvent e)
	{
		lights.clear();
		lights.addAll(ClientLightManager.lights);
		ColoredLightManager.LAST_LIGHTS = lights.size();
	}

	public static boolean OptifineInstalled = false;
	public static Class GuiPerformanceSettingsOF, GuiButtonOF, GuiSliderOF;

	static
	{
		try
		{
			GuiPerformanceSettingsOF = Class.forName("net.optifine.gui.GuiPerformanceSettingsOF");
			GuiButtonOF = Class.forName("net.optifine.gui.GuiOptionButtonOF");
			GuiSliderOF = Class.forName("net.optifine.gui.GuiOptionSliderOF");
			OptifineInstalled = true;
		} catch(Throwable err)
		{
		}
	}

	public static ThreadTimer searchTimer = new ThreadTimer(60F);
	public static long luxCalcTimeMS;

	public void startThread()
	{
		// No need to start more threads
		if(thread != null && thread.isAlive()) return;

		thread = SidedThreadGroups.CLIENT.newThread(() ->
		{
			while(!thread.isInterrupted())
			{
				searchTimer.advanceTime();
				if(searchTimer.ticks > 0) try
				{
					long start = System.nanoTime();
					searchLoop();
					luxCalcTimeMS = (System.nanoTime() - start) / 10000L;
				} catch(Throwable error)
				{
					// Continue running
				}
				else try
				{
					Thread.sleep(1L);
				} catch(InterruptedException e)
				{
					// Thread termination
				}
			}
		});
		thread.setName("ColoredLuxLightSearch");
		thread.start();
	}

	private static void searchLoop()
	{
		// DO NOT loop for blocks while lighting disabled.
		if(!ConfigCL.enableColoredLighting)
		{
			EXISTING.clear();
			EXISTING_ENTS.clear();
			return;
		}

		if(Minecraft.getMinecraft().player == null) return;
		EntityPlayer player = Minecraft.getMinecraft().player;
		World reader;
		if((reader = Minecraft.getMinecraft().world) != null)
		{
			BlockPos playerPos = player.getPosition();
			int maxDistance = ConfigCL.maxSearchDistance;
			int r = maxDistance / 2;
			for(BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(playerPos.add(-r, -r, -r), playerPos.add(r, r, r)))
			{
				IBlockState state = reader.getBlockState(pos);
				ILightBlockHandler handler = LuxManager.BLOCK_LUMINANCES.get(state.getBlock());
				if(handler != null)
				{
					BlockPos ipos = pos.toImmutable();
					EXISTING.put(ipos, new ILightBlockHandler.LightBlockWrapper(reader, ipos, state.getBlock().getExtendedState(state, reader, pos), handler));
				} else
					EXISTING.remove(pos);
			}
			Iterator<Integer> iter = EXISTING_ENTS.keySet().iterator();
			while(iter.hasNext())
			{
				Integer id = iter.next();
				Entity ent = reader.getEntityByID(id);
				if(ent == null || ent.isDead)
				{
					EXISTING_ENTS.get(id).remove(id);
					iter.remove();
				}
			}
			for(Entity ent : reader.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(playerPos).grow(r)))
			{
				EntityEntry ee = EntityRegistry.getEntry(ent.getClass());
				if(ee != null)
				{
					ILightEntityHandler handler = LuxManager.ENTITY_LUMINANCES.get(ee);
					if(handler != null)
						EXISTING_ENTS.put(ent.getEntityId(), new ILightEntityHandler.Wrapper(ent, handler));
				}
			}
		}
	}

	@Override
	public void postInit()
	{
		JsonBlockLights.reload();
		JsonEntityLights.reload();
		LuxManager.reload();
	}

	// VR Fix is supposed to solve GUI flickering with Vivecraft, but it doesn't seem to work. :/
	/* START VR FIX */
	private boolean enableTerrain_gui, enableEntity_gui;

//	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void renderScreenPre(GuiScreenEvent.DrawScreenEvent.Pre e)
	{
		Integer tp = terrainProgram.getProgramId();
		Integer ep = entityProgram.getProgramId();
		int active = GlShaderStack.glsActiveProgram();

		enableTerrain_gui = enableEntity_gui = false;

		if(tp != null && tp.equals(active))
		{
			enableTerrain_gui = true;
			terrainProgram.unbindShader();
		} else if(ep != null && ep.equals(active))
		{
			enableEntity_gui = true;
			entityProgram.unbindShader();
		}

		guiProgram.bindShader();
	}

//	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void renderScreenPost(GuiScreenEvent.DrawScreenEvent.Post e)
	{
		guiProgram.unbindShader();

		if(enableTerrain_gui)
		{
			enableTerrain_gui = false;
			terrainProgram.bindShader();
		}

		if(enableEntity_gui)
		{
			enableEntity_gui = false;
			entityProgram.bindShader();
		}
	}
	/* END VR FIX */

	@SubscribeEvent
	public void onProfilerChange(ProfilerEndStartEvent event)
	{
		section = event.getSection();
		if(ConfigCL.enableColoredLighting)
		{
			EntityPlayer player = Minecraft.getMinecraft().player;

			switch(event.getSection())
			{
				case "terrain":
				{
					float pt = Minecraft.getMinecraft().getRenderPartialTicks();

					float playerX = 0, playerY = 0, playerZ = 0;

					if(player != null)
					{
						playerX = (float) (player.prevPosX + (player.posX - player.prevPosX) * pt);
						playerY = (float) (player.prevPosZ + (player.posY - player.prevPosY) * pt);
						playerZ = (float) (player.prevPosZ + (player.posZ - player.prevPosZ) * pt);
					}

					isGui = false;
					precedesEntities = true;
					terrainProgram.bindShader();
//					terrainProgram.setUniform("ticks", ticks + pt);
					terrainProgram.setUniform("sampler", 0);
					terrainProgram.setUniform("lightmap", 1);
//					terrainProgram.setUniform("playerPos", playerX, playerY, playerZ);

					float wtR = WorldTintHandler.tintRed, wtG = WorldTintHandler.tintGreen, wtB = WorldTintHandler.tintBlue, wtInt = WorldTintHandler.tintIntensity;
					float saturation = WorldTintHandler.saturation;

					terrainProgram.setUniform("worldTint", wtR, wtG, wtB);
					terrainProgram.setUniform("worldTintIntensity", wtInt);
					terrainProgram.setUniform("saturation", saturation);
					terrainProgram.setUniform("fogIntensity", fogIntensity);

					if(!postedLights)
					{
						if(thread == null || !thread.isAlive())
							startThread();
						ClientLightManager.update(Minecraft.getMinecraft().world);
						GL20.glUseProgram(0);
						MinecraftForge.EVENT_BUS.post(new LightUniformEvent(ClientLightManager.lights));
						terrainProgram.bindShader();
						ClientLightManager.uploadLightsUBO();
						entityProgram.bindShader();
//						entityProgram.setUniform("ticks", ticks + Minecraft.getMinecraft().getRenderPartialTicks());
						entityProgram.setUniform("sampler", 0);
						entityProgram.setUniform("lightmap", 1);
						ClientLightManager.uploadLightsUBO();
//						entityProgram.setUniform("playerPos", playerX, playerY, playerZ);
						entityProgram.setUniform("worldTint", wtR, wtG, wtB);
						entityProgram.setUniform("worldTintIntensity", wtInt);
						entityProgram.setUniform("saturation", saturation);
						terrainProgram.bindShader();
						postedLights = true;
						ClientLightManager.clear();
					}
					break;
				}
				case "litParticles":
				{
					terrainProgram.bindShader();
					terrainProgram.setUniform("sampler", 0);
					terrainProgram.setUniform("lightmap", 1);
//					if(player != null)
//						terrainProgram.setUniform("playerPos", (float) player.posX, (float) player.posY, (float) player.posZ);
					terrainProgram.setUniform("chunkX", 0);
					terrainProgram.setUniform("chunkY", 0);
					terrainProgram.setUniform("chunkZ", 0);
					break;
				}
				case "particles":
				{
					entityProgram.bindShader();
					if(player != null)
						entityProgram.setUniform("entityPos", (float) player.posX, (float) player.posY, (float) player.posZ);
					entityProgram.setUniform("colorMult", 1F, 1F, 1F, 0F);
					break;
				}
				case "entities":
				{
					if(Minecraft.getMinecraft().isCallingFromMinecraftThread())
					{
						entityProgram.bindShader();
						entityProgram.setUniform("fogIntensity", fogIntensity);
					}
					break;
				}
				case "blockEntities":
					if(Minecraft.getMinecraft().isCallingFromMinecraftThread())
					{
						entityProgram.bindShader();
					}
					break;
				case "translucent":
					terrainProgram.bindShader();
					terrainProgram.setUniform("sampler", 0);
					terrainProgram.setUniform("lightmap", 1);
//					if(player != null)
//						terrainProgram.setUniform("playerPos", (float) player.posX, (float) player.posY, (float) player.posZ);
					break;
				case "hand":
					entityProgram.bindShader();
					if(player != null)
						entityProgram.setUniform("entityPos", (float) player.posX, (float) player.posY, (float) player.posZ);
					entityProgram.setUniform("colorMult", 1F, 1F, 1F, 0F);
					precedesEntities = true;
					break;
				case "sky":
				case "weather":
				case "outline":
				case "aboveClouds":
				case "destroyProgress":
					GL20.glUseProgram(0);
					break;
				case "gui":
					isGui = true;
					GL20.glUseProgram(0);
					break;
			}
		}
	}

	@Override
	public float getFogIntensity()
	{
		return fogIntensity;
	}

	@SubscribeEvent
	public void clientTick(ClientTickEvent e)
	{
		if(e.phase == Phase.START)
		{
			++ticks;
			WorldClient wc = Minecraft.getMinecraft().world;
			if(wc != null && !wc.eventListeners.contains(BLOCK_UPDATE_DETECTOR))
				wc.eventListeners.add(BLOCK_UPDATE_DETECTOR);
			searchTimer.setTPS(Math.max(Minecraft.getDebugFPS() / lightTPSDivisor, 1));
		}
	}

	@SubscribeEvent
	public void renderEntity(RenderEntityEvent e)
	{
		if(ConfigCL.enableColoredLighting)
		{
			if(LuxManager.blocksShader(e.getEntity()))
				GL20.glUseProgram(0);
			else if(section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities"))
				entityProgram.bindShader();
			if(entityProgram.isActive())
			{
				entityProgram.setUniform("entityPos", (float) e.getEntity().posX, (float) e.getEntity().posY + e.getEntity().height / 2.0f, (float) e.getEntity().posZ);
				entityProgram.setUniform("colorMult", 1F, 1F, 1F, 0F);
				if(e.getEntity() instanceof EntityLivingBase)
				{
					EntityLivingBase elb = (EntityLivingBase) e.getEntity();
					if(elb.hurtTime > 0 || elb.deathTime > 0)
						entityProgram.setUniform("colorMult", 1F, 0F, 0F, 0.35F);
				}
			}
		}
	}

	@SubscribeEvent
	public void renderTileEntity(RenderTileEntityEvent e)
	{
		if(ConfigCL.enableColoredLighting)
		{
			if(LuxManager.blocksShader(e.getTile()))
				GL20.glUseProgram(0);
			else if(section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities"))
				entityProgram.bindShader();
			if(entityProgram.isActive())
			{
				entityProgram.setUniform("entityPos", (float) e.getTile().getPos().getX(), (float) e.getTile().getPos().getY(), (float) e.getTile().getPos().getZ());
				entityProgram.setUniform("colorMult", 1F, 1F, 1F, 0F);
			}
		}
	}

	@SubscribeEvent
	public void preRenderChunk(PreRenderChunkEvent e)
	{
		if(ConfigCL.enableColoredLighting && terrainProgram.isActive())
		{
			BlockPos pos = e.getRenderChunk().getPosition();
			terrainProgram.setUniform("chunkX", pos.getX());
			terrainProgram.setUniform("chunkY", pos.getY());
			terrainProgram.setUniform("chunkZ", pos.getZ());
		}
	}

	@SubscribeEvent
	public void renderLast(RenderWorldLastEvent e)
	{
		postedLights = false;
		if(Minecraft.getMinecraft().isCallingFromMinecraftThread())
		{
			GlStateManager.disableLighting();
			GL20.glUseProgram(0);
		}

		World wld = Minecraft.getMinecraft().world;
		if(wld != null)
		{
			if(ConfigCL.enableFog)
			{
				CalculateFogIntensityEvent e2 = new CalculateFogIntensityEvent(wld, 1F);
				MinecraftForge.EVENT_BUS.post(e2);
				fogIntensity = e2.getValue();
			} else fogIntensity = 0;
		} else fogIntensity = 1F;
	}

	public static boolean renderF3;

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void addF3Info(RenderGameOverlayEvent.Pre event)
	{
		if(event.getType() == ElementType.DEBUG)
			renderF3 = true;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void addF3Info(RenderGameOverlayEvent.Text f3)
	{
		if(renderF3)
		{
			String s = "[" + TextFormatting.GREEN + "Lux" + TextFormatting.RESET + "] " + (ConfigCL.enableColoredLighting ? ("L: " + ClientLightManager.debugCulledLights + "/" + ClientLightManager.debugLights + "@" + (ConfigCL.maxLights) + " | ~" + luxCalcTimeMS + "ms") : "Colored lighting " + TextFormatting.RED + "disabled" + TextFormatting.RESET + ".");
			List<String> left = f3.getLeft();
			if(left.size() > 5)
				left.add(5, s);
			else
				left.add(s);
			renderF3 = false;
		}
	}
}