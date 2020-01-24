package com.zeitheron.lux.proxy;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.zeitheron.hammercore.api.events.PreRenderChunkEvent;
import com.zeitheron.hammercore.api.events.ProfilerEndStartEvent;
import com.zeitheron.hammercore.api.events.RenderEntityEvent;
import com.zeitheron.hammercore.api.events.RenderTileEntityEvent;
import com.zeitheron.hammercore.api.lighting.ColoredLight;
import com.zeitheron.hammercore.api.lighting.ColoredLightManager;
import com.zeitheron.hammercore.api.lighting.LightUniformEvent;
import com.zeitheron.hammercore.api.lighting.WorldTintHandler;
import com.zeitheron.hammercore.api.lighting.impl.IGlowingBlock;
import com.zeitheron.hammercore.client.render.shader.GlShaderStack;
import com.zeitheron.hammercore.client.utils.UtilsFX;
import com.zeitheron.hammercore.lib.zlib.json.JSONObject;
import com.zeitheron.hammercore.utils.ReflectionUtil;
import com.zeitheron.lux.ColoredLux;
import com.zeitheron.lux.ConfigCL;
import com.zeitheron.lux.api.LuxManager;
import com.zeitheron.lux.api.event.CalculateFogIntensityEvent;
import com.zeitheron.lux.api.event.GatherLightsEvent;
import com.zeitheron.lux.api.light.ILightBlockHandler;
import com.zeitheron.lux.api.light.ILightEntityHandler;
import com.zeitheron.lux.api.light.LightBlockWrapper;
import com.zeitheron.lux.api.light.LightEntityWrapper;
import com.zeitheron.lux.client.ClientLightManager;
import com.zeitheron.lux.client.SmartShaderProgram;
import com.zeitheron.lux.client.SmartShaderProgram.SmartShaderVariables;
import com.zeitheron.lux.client.SmartShaderProgram.SmartVariable;
import com.zeitheron.lux.client.ThreadTimer;
import com.zeitheron.lux.client.json.JsonBlockLights;
import com.zeitheron.lux.client.json.JsonEntityLights;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.GuiOptionsRowList.Row;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.EnumHelperClient;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.server.command.CommandTreeBase;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;

public class ClientProxy
		extends CommonProxy
		implements ISelectiveResourceReloadListener
{
	public static final Map<BlockPos, LightBlockWrapper> EXISTING = Collections.synchronizedMap(new HashMap<>());
	public static final Map<Integer, LightEntityWrapper> EXISTING_ENTS = Collections.synchronizedMap(new HashMap<>());

	private static int ticks;
	public static SmartShaderProgram terrainProgram;
	public static SmartShaderProgram entityProgram;
	public static boolean isGui = false;

	static final int runtimeCores = Runtime.getRuntime().availableProcessors();
	static final int lightTPSDivisor = runtimeCores <= 4 ? 8 : runtimeCores <= 8 ? 4 : 2;

	boolean postedLights = false;
	boolean precedesEntities = true;
	String section = "";
	Thread thread;

	private static int maxSessionLights = 1;
	public static final SmartVariable LIGHT_COUNT = new SmartVariable("LIGHTS", () ->
	{
		int cl = ClientLightManager.lights.size();
		if(ConfigCL.minLights > maxSessionLights)
			maxSessionLights = ConfigCL.minLights;
		if(cl > maxSessionLights)
			maxSessionLights = Math.max(maxSessionLights, Math.min(ConfigCL.maxLights, 2 * cl));
		return Integer.toString(maxSessionLights);
	});

	public static final List<Options> customOptions = new ArrayList<>();
	public static final Options LUX_ENABLE_LIGHTING = EnumHelperClient.addOptions("LUX_ENABLE_LIGHTING", "options.lux:lighting", false, true);

	@Override
	public void preInit(FMLPreInitializationEvent e)
	{
		ProfilerEndStartEvent.enable();
		RenderEntityEvent.enable();
		RenderTileEntityEvent.enable();
		PreRenderChunkEvent.enable();
		MinecraftForge.EVENT_BUS.register(this);

		ColoredLux.LOG.info("Found " + runtimeCores + " available processing threads. The light update frequency will be max(FPS/" + lightTPSDivisor + ", 1) Hz");

		customOptions.add(LUX_ENABLE_LIGHTING);
		if(OptifineInstalled) for(Field f : GuiPerformanceSettingsOF.getDeclaredFields())
			if(Options[].class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) try
			{
				f.setAccessible(true);
				Options[] videoSettings = Options[].class.cast(f.get(null));
				List<Options> got = new ArrayList<>(Arrays.asList(videoSettings));
				customOptions.forEach(o -> got.add(o));
				if(Modifier.isFinal(f.getModifiers()))
					ReflectionUtil.setStaticFinalField(f, got.toArray(new Options[got.size()]));
				else f.set(null, got.toArray(new Options[got.size()]));
			} catch(IllegalArgumentException | IllegalAccessException e1)
			{
				e1.printStackTrace();
			}
			else ;
		else for(Field f : GuiVideoSettings.class.getDeclaredFields())
			if(Options[].class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) try
			{
				f.setAccessible(true);
				Options[] videoSettings = Options[].class.cast(f.get(null));
				List<Options> got = new ArrayList<>(Arrays.asList(videoSettings));
				customOptions.forEach(o -> got.add(got.indexOf(Options.USE_VBO), o));
				ReflectionUtil.setStaticFinalField(f, got.toArray(new Options[got.size()]));
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

		ColoredLightManager.registerOperator(() -> ConfigCL.enableColoredLighting, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				glUniform1i(GlShaderStack.glsGetActiveUniformLoc("lightCount"), lights.size());
				glUniform1i(GlShaderStack.glsGetActiveUniformLoc("colMix"), 0);

				int ll = Math.min(ConfigCL.maxLights, lights.size());

				for(int i = 0; i < ll; i++)
				{
					if(i < lights.size())
					{
						ColoredLight l = lights.get(i);
						glUniform3f(GlShaderStack.glsGetActiveUniformLoc("lights[" + i + "].position"), l.x, l.y, l.z);
						glUniform4f(GlShaderStack.glsGetActiveUniformLoc("lights[" + i + "].color"), l.r, l.g, l.b, l.a);
						glUniform1f(GlShaderStack.glsGetActiveUniformLoc("lights[" + i + "].radius"), l.radius);
					}
				}

				return true;
			}
			return false;
		}, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				ClientProxy.terrainProgram.useShader();
				return true;
			}
			return false;
		}, () ->
		{
			if(ConfigCL.enableColoredLighting)
			{
				SmartShaderProgram.stopShader();
				return true;
			}
			return false;
		});

		ClientCommandHandler.instance.registerCommand(new CommandTreeBase()
		{
			{
				addSubcommand(new CommandBase()
				{
					@Override
					public String getUsage(ICommandSender sender)
					{
						return "lux reload";
					}

					@Override
					public String getName()
					{
						return "reload";
					}

					@Override
					public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
					{
						sender.sendMessage(new TextComponentString("Reloading lights-block.json"));
						JsonBlockLights.reload();
						EXISTING.clear();
						sender.sendMessage(new TextComponentString("Reloading lights-entity.json"));
						JsonEntityLights.reload();
						EXISTING_ENTS.clear();
						sender.sendMessage(new TextComponentString("Reloading shaders"));
						terrainProgram.reload();
						entityProgram.reload();
						sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Lux reloaded!"));
					}
				});

				addSubcommand(new CommandBase()
				{
					@Override
					public String getUsage(ICommandSender sender)
					{
						return "lux pickstate";
					}

					@Override
					public String getName()
					{
						return "pickstate";
					}

					@Override
					public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
					{
						RayTraceResult obj = Minecraft.getMinecraft().objectMouseOver;
						if(obj != null && obj.typeOfHit == RayTraceResult.Type.BLOCK)
						{
							BlockPos pos = obj.getBlockPos();
							World wld = Minecraft.getMinecraft().world;
							IBlockState state = wld.getBlockState(pos).getActualState(wld, pos);
							final StringBuilder sb = new StringBuilder().append("\"state\": { ").append(Joiner.on(", ").join(state.getProperties().entrySet().stream().map(entry -> JSONObject.quote(entry.getKey().getName()) + ": " + JSONObject.quote(Objects.toString(entry.getValue()))).collect(Collectors.toList())));
							if(state instanceof IExtendedBlockState)
							{
								sb.append(", ");
								final ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties = ((IExtendedBlockState) state).getUnlistedProperties();
								List<String> bleh = new ArrayList<>();
								unlistedProperties.forEach((key, value) -> value.ifPresent(trueValue -> bleh.add(JSONObject.quote(key.getName()) + ": " + JSONObject.quote(Objects.toString(trueValue)))));
								sb.append(Joiner.on(", ").join(bleh));
							}
							sb.append(" }");
							sender.sendMessage(new TextComponentString(sb.toString()));
							GuiScreen.setClipboardString(sb.toString());
							sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Copied to clipboard!"));
						}
					}
				});
			}

			@Override
			public String getUsage(ICommandSender sender)
			{
				return "/lux";
			}

			@Override
			public String getName()
			{
				return "lux";
			}

			@Override
			public boolean checkPermission(MinecraftServer server, ICommandSender sender)
			{
				return true;
			}

			@Override
			public int getRequiredPermissionLevel()
			{
				return 0;
			}
		});
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

	Field buttonA, buttonB;

	public List<String> toDrawTooltip;

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void initGui(InitGuiEvent.Post e)
	{
		if(e.getGui() != null && GuiPerformanceSettingsOF != null && GuiPerformanceSettingsOF.isAssignableFrom(e.getGui().getClass()))
		{
			for(int i = 0; i < e.getButtonList().size(); ++i)
			{
				GuiButton btn = e.getButtonList().get(i);
				if(btn instanceof GuiOptionButton)
				{
					GuiOptionButton opts = (GuiOptionButton) btn;
					if(customOptions.contains(opts.getOption()))
						e.getButtonList().set(i, convert((GuiOptionButton) btn));
				}
			}
		}

		if(e.getGui() instanceof GuiVideoSettings && !OptifineInstalled)
		{
			GuiVideoSettings settings = (GuiVideoSettings) e.getGui();

			GuiOptionsRowList options = (GuiOptionsRowList) settings.optionsRowList;
			for(int i = 0; i < options.options.size(); ++i)
			{
				Row prev = options.options.get(i);

				if(buttonA == null && prev.buttonA != null && prev.buttonA != prev.buttonB)
				{
					for(Field f : Row.class.getDeclaredFields())
						if(GuiButton.class.isAssignableFrom(f.getType()))
						{
							f.setAccessible(true);
							try
							{
								if(f.get(prev) == prev.buttonA)
									buttonA = f;
							} catch(IllegalArgumentException | IllegalAccessException e1)
							{
								e1.printStackTrace();
							}
						}
				}

				if(buttonB == null && prev.buttonB != null && prev.buttonA != prev.buttonB)
				{
					for(Field f : Row.class.getDeclaredFields())
						if(GuiButton.class.isAssignableFrom(f.getType()))
						{
							f.setAccessible(true);
							try
							{
								if(f.get(prev) == prev.buttonB)
									buttonB = f;
							} catch(IllegalArgumentException | IllegalAccessException e1)
							{
								e1.printStackTrace();
							}
						}
				}

				if(buttonB != null && prev.buttonB instanceof GuiOptionButton && customOptions.contains(((GuiOptionButton) prev.buttonB).getOption()))
				{
					try
					{
						GuiOptionButton nbtn = convert((GuiOptionButton) prev.buttonB);
						if(Modifier.isFinal(buttonB.getModifiers()) && !ReflectionUtil.setFinalField(buttonB, prev, nbtn))
							System.out.println("Failed to override final value of " + buttonB.getName());
						else if(!Modifier.isFinal(buttonB.getModifiers()))
							buttonB.set(prev, nbtn);
						else
							System.out.println("Failed to override final value of " + buttonB.getName());
					} catch(ReflectiveOperationException e1)
					{
						e1.printStackTrace();
					}
				}

				if(buttonA != null && prev.buttonA instanceof GuiOptionButton && customOptions.contains(((GuiOptionButton) prev.buttonA).getOption()))
				{
					try
					{
						GuiOptionButton nbtn = convert((GuiOptionButton) prev.buttonA);
						if(Modifier.isFinal(buttonA.getModifiers()) && !ReflectionUtil.setFinalField(buttonA, prev, nbtn))
							System.out.println("Failed to override final value of " + buttonA.getName());
						else if(!Modifier.isFinal(buttonA.getModifiers()))
							buttonA.set(prev, nbtn);
						else
							System.out.println("Failed to override final value of " + buttonA.getName());
					} catch(ReflectiveOperationException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		}
	}

	private GuiOptionButton convert(GuiOptionButton btn)
	{
		return new GuiOptionButton(btn.id, btn.x, btn.y, btn.getOption(), btn.displayString)
		{
			Long hoverTime;

			@Override
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
			{
				boolean state = getState(getOption());
				displayString = I18n.format(getOption().getTranslation()) + ": " + (state ? TextFormatting.DARK_GREEN : TextFormatting.DARK_RED) + I18n.format("options.o" + (state ? "n" : "ff")) + TextFormatting.RESET;
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(isMouseOver())
				{
					if(hoverTime == null)
						hoverTime = System.currentTimeMillis();
					else if(System.currentTimeMillis() - hoverTime.longValue() >= 1500L)
					{
						toDrawTooltip = new ArrayList<String>();
						toDrawTooltip.add("Colored Lux:");
						toDrawTooltip.add("");
						describe(getOption(), toDrawTooltip);
					}
				} else
					hoverTime = null;
			}

			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
			{
				boolean p = super.mousePressed(mc, mouseX, mouseY);
				if(p)
					toggle(getOption());
				return p;
			}
		};
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void renderGui(DrawScreenEvent.Post e)
	{
		if(toDrawTooltip != null)
		{
			FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
			int x = e.getMouseX(), y = e.getMouseY();
			if(y + fr.FONT_HEIGHT * toDrawTooltip.size() >= e.getGui().height - 6)
				y = e.getGui().height - 6 - fr.FONT_HEIGHT * toDrawTooltip.size();
			int mw = 0;
			for(String s : toDrawTooltip)
				mw = Math.max(mw, fr.getStringWidth(s));
			if(x + mw > e.getGui().width - 16)
				x = e.getGui().width - mw - 16;
			UtilsFX.drawCustomTooltip(e.getGui(), Minecraft.getMinecraft().getRenderItem(), fr, toDrawTooltip, x, y, TextFormatting.AQUA.getColorIndex());
			toDrawTooltip = null;
		}
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

	public void describe(Options opt, List<String> desc)
	{
		if(opt == LUX_ENABLE_LIGHTING)
		{
			String str = I18n.format("options.lux:lighting.desc");
			while(str.contains("{OF}") && str.contains("{/}"))
			{
				int ofi = str.indexOf("{OF}");
				int ofe = str.indexOf("{/}", ofi);
				String inner = str.substring(ofi + 4, ofe);
				if(OptifineInstalled)
					str = str.replace("{OF}" + inner + "{/}", inner);
				else
					str = str.replace("{OF}" + inner + "{/}", "");
			}
			desc.addAll(Arrays.asList(str.split("<br>")));
		}
	}

	public boolean getState(Options opt)
	{
		if(opt == LUX_ENABLE_LIGHTING)
			return ConfigCL.enableColoredLighting;

		return false;
	}

	public void toggle(Options opt)
	{
		if(opt == LUX_ENABLE_LIGHTING)
		{
			ConfigCL.enableColoredLighting = !ConfigCL.enableColoredLighting;
			ConfigCL.cfgs.get("Client-Side", "Colored Lighting", true).set(ConfigCL.enableColoredLighting);
			ConfigCL.cfgs.save();
		}
	}

	public static ThreadTimer searchTimer = new ThreadTimer(60F);
	public static long luxCalcTimeMS;

	public void startThread()
	{
		// No need to start more threads
		if(thread != null && thread.isAlive()) return;

		thread = new Thread(() ->
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
		}, "ColoredLuxLightSearch");
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
			int maxDistance = ConfigCL.maxDistance;
			int r = maxDistance / 2;
			for(BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(playerPos.add(-r, -r, -r), playerPos.add(r, r, r)))
			{
				IBlockState state = reader.getBlockState(pos);
				ILightBlockHandler handler = LuxManager.BLOCK_LUMINANCES.get(state.getBlock());
				if(handler != null)
				{
					BlockPos ipos = pos.toImmutable();
					EXISTING.put(ipos, new LightBlockWrapper(reader, ipos, state.getBlock().getExtendedState(state, reader, pos), handler));
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
						EXISTING_ENTS.put(ent.getEntityId(), new LightEntityWrapper(ent, handler));
				}
			}
		}
	}

	@Override
	public void postInit()
	{
		((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
		JsonBlockLights.reload();
		ForgeRegistries.BLOCKS.getValuesCollection().stream() //
				.filter(Predicates.instanceOf(IGlowingBlock.class)) //
				.forEach(blk ->
				{
					IGlowingBlock glow = (IGlowingBlock) blk;
					LuxManager.registerBlockLight(blk, (world, pos, state, event) -> event.add(glow.produceColoredLight(world, pos, state, 1F)));
				});
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
	{
		if(resourcePredicate.test(VanillaResourceType.SHADERS))
		{
			terrainProgram = new SmartShaderProgram(new ResourceLocation("lux", "terrain"), resourceManager, new SmartShaderVariables(LIGHT_COUNT));

			entityProgram = new SmartShaderProgram(new ResourceLocation("lux", "entities"), resourceManager, new SmartShaderVariables(LIGHT_COUNT));
		}
	}

	@SubscribeEvent
	public void onProfilerChange(ProfilerEndStartEvent event)
	{
		section = event.getSection();
		if(ConfigCL.enableColoredLighting)
		{
			if(event.getSection().compareTo("terrain") == 0)
			{
				float pt = Minecraft.getMinecraft().getRenderPartialTicks();
				EntityPlayer player = Minecraft.getMinecraft().player;

				float playerX = 0, playerY = 0, playerZ = 0;

				if(player != null)
				{
					playerX = (float) (player.prevPosX + (player.posX - player.prevPosX) * pt);
					playerY = (float) (player.prevPosZ + (player.posY - player.prevPosY) * pt);
					playerZ = (float) (player.prevPosZ + (player.posZ - player.prevPosZ) * pt);
				}

				isGui = false;
				precedesEntities = true;
				terrainProgram.useShader();
				terrainProgram.setUniform("ticks", ticks + pt);
				terrainProgram.setUniform("sampler", 0);
				terrainProgram.setUniform("lightmap", 1);
				terrainProgram.setUniform("playerPos", (float) Minecraft.getMinecraft().player.posX, (float) Minecraft.getMinecraft().player.posY, (float) Minecraft.getMinecraft().player.posZ);

				float wtR = WorldTintHandler.tintRed, wtG = WorldTintHandler.tintGreen, wtB = WorldTintHandler.tintBlue, wtInt = WorldTintHandler.tintIntensity;

				terrainProgram.setUniform("worldTint", wtR, wtG, wtB);
				terrainProgram.setUniform("worldTintIntensity", wtInt);

				if(!postedLights)
				{
					if(thread == null || !thread.isAlive())
						startThread();
					ClientLightManager.update(Minecraft.getMinecraft().world);
					SmartShaderProgram.stopShader();
					MinecraftForge.EVENT_BUS.post(new LightUniformEvent(ClientLightManager.lights));
					terrainProgram.useShader();
					ClientLightManager.uploadLights();
					entityProgram.useShader();
					entityProgram.setUniform("ticks", ticks + Minecraft.getMinecraft().getRenderPartialTicks());
					entityProgram.setUniform("sampler", 0);
					entityProgram.setUniform("lightmap", 1);
					ClientLightManager.uploadLights();

					entityProgram.setUniform("playerPos", playerX, playerY, playerZ);

					entityProgram.setUniform("worldTint", wtR, wtG, wtB);
					entityProgram.setUniform("worldTintIntensity", wtInt);

					entityProgram.setUniform("lightingEnabled", GL11.glIsEnabled(GL11.GL_LIGHTING));
					terrainProgram.useShader();
					postedLights = true;
					ClientLightManager.clear();
				}
			}
			if(event.getSection().compareTo("sky") == 0)
			{
				SmartShaderProgram.stopShader();
			}
			if(event.getSection().compareTo("litParticles") == 0)
			{
				terrainProgram.useShader();
				terrainProgram.setUniform("sampler", 0);
				terrainProgram.setUniform("lightmap", 1);
				terrainProgram.setUniform("playerPos", (float) Minecraft.getMinecraft().player.posX, (float) Minecraft.getMinecraft().player.posY, (float) Minecraft.getMinecraft().player.posZ);
				terrainProgram.setUniform("chunkX", 0);
				terrainProgram.setUniform("chunkY", 0);
				terrainProgram.setUniform("chunkZ", 0);
			}
			if(event.getSection().compareTo("particles") == 0)
			{
				entityProgram.useShader();
				entityProgram.setUniform("entityPos", (float) Minecraft.getMinecraft().player.posX, (float) Minecraft.getMinecraft().player.posY, (float) Minecraft.getMinecraft().player.posZ);
				entityProgram.setUniform("colorMult", 1F, 1F, 1F, 0F);
			}
			if(event.getSection().compareTo("weather") == 0)
			{
				SmartShaderProgram.stopShader();
			}
			if(event.getSection().compareTo("entities") == 0)
			{
				if(Minecraft.getMinecraft().isCallingFromMinecraftThread())
				{
					entityProgram.useShader();
					entityProgram.setUniform("lightingEnabled", true);
					World wld = Minecraft.getMinecraft().world;
					CalculateFogIntensityEvent e = new CalculateFogIntensityEvent(wld, wld.provider.getDimensionType() == DimensionType.NETHER ? 0.015625f : 1.0f);
					MinecraftForge.EVENT_BUS.post(e);
					entityProgram.setUniform("fogIntensity", e.getValue());
				}
			}
			if(event.getSection().compareTo("blockEntities") == 0)
			{
				if(Minecraft.getMinecraft().isCallingFromMinecraftThread())
				{
					entityProgram.useShader();
					entityProgram.setUniform("lightingEnabled", true);
				}
			}
			if(event.getSection().compareTo("outline") == 0)
			{
				SmartShaderProgram.stopShader();
			}
			if(event.getSection().compareTo("aboveClouds") == 0)
			{
				SmartShaderProgram.stopShader();
			}
			if(event.getSection().compareTo("destroyProgress") == 0)
			{
				SmartShaderProgram.stopShader();
			}
			if(event.getSection().compareTo("translucent") == 0)
			{
				terrainProgram.useShader();
				terrainProgram.setUniform("sampler", 0);
				terrainProgram.setUniform("lightmap", 1);
				terrainProgram.setUniform("playerPos", (float) Minecraft.getMinecraft().player.posX, (float) Minecraft.getMinecraft().player.posY, (float) Minecraft.getMinecraft().player.posZ);
			}
			if(event.getSection().compareTo("hand") == 0)
			{
				entityProgram.useShader();
				entityProgram.setUniform("entityPos", (float) Minecraft.getMinecraft().player.posX, (float) Minecraft.getMinecraft().player.posY, (float) Minecraft.getMinecraft().player.posZ);
				entityProgram.setUniform("colorMult", 1F, 1F, 1F, 0F);
				precedesEntities = true;
			}
			if(event.getSection().compareTo("gui") == 0)
			{
				isGui = true;
				SmartShaderProgram.stopShader();
			}
		}
	}

	@SubscribeEvent
	public void clientTick(ClientTickEvent e)
	{
		if(e.phase == Phase.START)
		{
			++ticks;
			WorldClient wc = Minecraft.getMinecraft().world;
			if(wc != null && !wc.eventListeners.contains(INSTANCE))
				wc.eventListeners.add(INSTANCE);
			searchTimer.setTPS(Math.max(Minecraft.getDebugFPS() / lightTPSDivisor, 1));
		}
	}

	@SubscribeEvent
	public void renderEntity(RenderEntityEvent e)
	{
		if(ConfigCL.enableColoredLighting)
		{
			if(LuxManager.blocksShader(e.getEntity()))
				SmartShaderProgram.stopShader();
			else if(section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities"))
				entityProgram.useShader();
			if(SmartShaderProgram.isCurrentShader(entityProgram))
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
				SmartShaderProgram.stopShader();
			else if(section.equalsIgnoreCase("entities") || section.equalsIgnoreCase("blockEntities"))
				entityProgram.useShader();
			if(SmartShaderProgram.isCurrentShader(entityProgram))
			{
				entityProgram.setUniform("entityPos", (float) e.getTile().getPos().getX(), (float) e.getTile().getPos().getY(), (float) e.getTile().getPos().getZ());
				entityProgram.setUniform("colorMult", 1F, 1F, 1F, 0F);
			}
		}
	}

	@SubscribeEvent
	public void preRenderChunk(PreRenderChunkEvent e)
	{
		if(ConfigCL.enableColoredLighting && SmartShaderProgram.isCurrentShader(terrainProgram))
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
			SmartShaderProgram.stopShader();
		}
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
			String s = "[" + TextFormatting.GREEN + "Lux" + TextFormatting.RESET + "] " + (ConfigCL.enableColoredLighting ? ("L: " + ClientLightManager.debugCulledLights + "/" + ClientLightManager.debugLights + "|" + (GL11.glGetInteger(GL20.GL_MAX_VERTEX_UNIFORM_COMPONENTS) / 4 / (4 + 3 + 1)) + " | ~" + luxCalcTimeMS + "ms") : "Colored lighting " + TextFormatting.RED + "disabled" + TextFormatting.RESET + ".");
			List<String> left = f3.getLeft();
			if(left.size() > 5)
				left.add(5, s);
			else
				left.add(s);
			renderF3 = false;
		}
	}

	public static final BUD INSTANCE = new BUD();

	public static class BUD
			implements IWorldEventListener
	{
		@Override
		public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags)
		{
			int maxDistance = ConfigCL.maxDistance;
			int r = maxDistance / 2;
			Vec3d cameraPosition = ClientLightManager.cameraPos;
			ICamera camera = ClientLightManager.camera;
			ArrayList<ColoredLight> lights = new ArrayList<>();
			GatherLightsEvent lightsEvent = new GatherLightsEvent(lights, maxDistance, cameraPosition, camera, Minecraft.getMinecraft().getRenderPartialTicks());
			ILightBlockHandler handler = LuxManager.BLOCK_LUMINANCES.get(newState.getBlock());
			if(handler != null)
			{
				BlockPos ipos = pos.toImmutable();
				EXISTING.put(ipos, new LightBlockWrapper(worldIn, ipos, newState, handler));
			} else
				EXISTING.remove(pos);
		}

		@Override
		public void notifyLightSet(BlockPos pos)
		{
		}

		@Override
		public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
		{
		}

		@Override
		public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x, double y, double z, float volume, float pitch)
		{
		}

		@Override
		public void playRecord(SoundEvent soundIn, BlockPos pos)
		{
		}

		@Override
		public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
		{
		}

		@Override
		public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters)
		{
		}

		@Override
		public void onEntityAdded(Entity entityIn)
		{
		}

		@Override
		public void onEntityRemoved(Entity entityIn)
		{
		}

		@Override
		public void broadcastSound(int soundID, BlockPos pos, int data)
		{
		}

		@Override
		public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data)
		{
		}

		@Override
		public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress)
		{
		}
	}
}