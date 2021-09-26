package org.zeith.lux.luxpack.apis;

import com.zeitheron.hammercore.api.lighting.ColoredLight;
import com.zeitheron.hammercore.lib.nashorn.JSSource;
import com.zeitheron.hammercore.lib.zlib.json.JSONObject;
import com.zeitheron.hammercore.utils.java.itf.QuadConsumer;
import com.zeitheron.hammercore.utils.java.itf.TriConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.zeith.lux.ColoredLux;
import org.zeith.lux.api.event.ReloadLuxManagerEvent;
import org.zeith.lux.luxpack.AbstractLuxPack;
import org.zeith.lux.luxpack.WrappedTile;
import org.zeith.lux.luxpack.apis.js.JSBlockPos;
import org.zeith.lux.luxpack.apis.js.JSWorld;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class LuxPackAPIv2
		extends LuxPackAPIv1
{
	public static Map<Class<? extends TileEntity>, QuadConsumer<World, BlockPos, TileEntity, Consumer<ColoredLight>>> CUSTOM_TILE_LIGHTS = new HashMap<>();
	public static Map<EntityEntry, TriConsumer<World, Entity, Consumer<ColoredLight>>> CUSTOM_ENTITY_LIGHTS = new HashMap<>();
	private final Map<File, Map<Class<? extends TileEntity>, Tuple<String, JSSource>>> tsources = new HashMap<>();
	private final Map<File, Map<EntityEntry, Tuple<String, JSSource>>> esources = new HashMap<>();

	@SubscribeEvent
	public void reloadLux(ReloadLuxManagerEvent e)
	{
		CUSTOM_TILE_LIGHTS.clear();
		super.reloadLux(e);

		tsources.values().forEach(map -> map.forEach((type, src) ->
		{
			try
			{
				ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
				engine.eval(src.getSecond().read());
				Invocable root = (Invocable) engine;
				CUSTOM_TILE_LIGHTS.put(type, new QuadLightGen(src.getFirst(), root));
			} catch(Throwable err)
			{
				ColoredLux.LOG.error("Failed to load tile light script " + src.getFirst() + "!", err);
			}
		}));

		esources.values().forEach(map -> map.forEach((type, src) ->
		{
			try
			{
				ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
				engine.eval(src.getSecond().read());
				Invocable root = (Invocable) engine;
				CUSTOM_ENTITY_LIGHTS.put(type, new TriLightGen(src.getFirst(), root));
			} catch(Throwable err)
			{
				ColoredLux.LOG.error("Failed to load tile light script " + src.getFirst() + "!", err);
			}
		}));
	}

	@Override
	public void hookLuxPack(AbstractLuxPack pack)
	{
		super.hookLuxPack(pack);

		JSSource src = pack.createJSSource("main.js");

		tsources.put(pack.location, new HashMap<>());
		esources.put(pack.location, new HashMap<>());

		if(src.exists())
			try
			{
				src = src.inheritClassMethods(LuxPackJSInternal.class);

				LuxPackJSInternal.contextPack = pack;
				LuxPackJSInternal.api = this;

				ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
				engine.eval(src.read());
				Invocable root = (Invocable) engine;
				root.invokeFunction("main");

				LuxPackJSInternal.contextPack = null;
				LuxPackJSInternal.api = null;
			} catch(Throwable err)
			{
				err.printStackTrace();
			}
	}

	@Override
	public void unhookLuxPack(AbstractLuxPack pack)
	{
		super.unhookLuxPack(pack);
		esources.remove(pack.location);
		tsources.remove(pack.location);
	}

	public static class TriLightGen
			implements TriConsumer<World, Entity, Consumer<ColoredLight>>
	{
		public final String path;
		public final Invocable root;
		public int errors = 0;

		public TriLightGen(String path, Invocable root)
		{
			this.path = path;
			this.root = root;
		}

		@Override
		public void accept(World world, Entity entity, Consumer<ColoredLight> c)
		{
			if(errors >= 5) return;

			try
			{
				TScriptJSInternal.context = new TLightContext(c, b -> b.pos(entity, Minecraft.getMinecraft().getRenderPartialTicks()).build());
				root.invokeFunction("light", new JSWorld(world), entity);
				TScriptJSInternal.context = null;
			} catch(Throwable err)
			{
				ColoredLux.LOG.error("Error in script " + path + "!", err);
				++errors;
				if(errors >= 5)
					ColoredLux.LOG.error("Script " + path + " has generated too many errors, it is going to be disabled until it gets reloaded.");
			}
		}
	}

	public static class QuadLightGen
			implements QuadConsumer<World, BlockPos, TileEntity, Consumer<ColoredLight>>
	{
		public final String path;
		public final Invocable root;
		public int errors = 0;

		public QuadLightGen(String path, Invocable root)
		{
			this.path = path;
			this.root = root;
		}

		@Override
		public void accept(World world, BlockPos pos, TileEntity tile, Consumer<ColoredLight> c)
		{
			if(errors >= 5) return;

			try
			{
				TScriptJSInternal.context = new TLightContext(c, b -> b.pos(pos).build());
				root.invokeFunction("light", new JSWorld(world), new JSBlockPos(pos), WrappedTile.wrapTile(tile), tile);
				TScriptJSInternal.context = null;
			} catch(Throwable err)
			{
				ColoredLux.LOG.error("Error in script " + path + "!", err);
				++errors;
				if(errors >= 5)
					ColoredLux.LOG.error("Script " + path + " has generated too many errors, it is going to be disabled until it gets reloaded.");
			}
		}
	}

	public static class TLightContext
	{
		final Consumer<ColoredLight> thePipe;
		final Function<ColoredLight.Builder, ColoredLight> finisher;

		public TLightContext(Consumer<ColoredLight> thePipe, Function<ColoredLight.Builder, ColoredLight> finisher)
		{
			this.thePipe = thePipe;
			this.finisher = finisher;
		}

		public void add(ColoredLight light)
		{
			thePipe.accept(light);
		}

		public void add(ColoredLight.Builder light)
		{
			add(finisher.apply(light));
		}
	}

	public static class TScriptJSInternal
	{
		public static TLightContext context;

		public static void add(ColoredLight light)
		{
			if(context != null) context.add(light);
		}

		public static void add(ColoredLight.Builder light)
		{
			if(context != null) context.add(light);
		}

		public static double interp(double prev, double cur)
		{
			float pt = Minecraft.getMinecraft().getRenderPartialTicks();
			return prev + (cur - prev) * pt;
		}

		public static float interp(float prev, float cur)
		{
			float pt = Minecraft.getMinecraft().getRenderPartialTicks();
			return prev + (cur - prev) * pt;
		}

		public static String dump(Object f, int indent)
		{
			String dumpId = UUID.randomUUID().toString();
			if(f != null)
			{
				StringBuilder indentation = new StringBuilder();
				for(int i = 0; i < indent; ++i) indentation.append(' ');

				ColoredLux.LOG.info(indentation + "--------------------DUMP--------------------");
				if(indent < 2) ColoredLux.LOG.info(indentation + "DUMP " + dumpId + ".");

				if(f instanceof TileEntity)
				{
					Object w = WrappedTile.wrapTile((TileEntity) f);
					if(w instanceof WrappedTile)
					{
						ColoredLux.LOG.info(indentation + "-----------------WRAP FOR USE IN JS-----------------");
						dump(w, indent + 4);
						ColoredLux.LOG.info(indentation + "----------------------END WRAP----------------------");
					}
				}

				ColoredLux.LOG.info(indentation + "-------------------METHOD-------------------");

				for(Method fe : f.getClass().getMethods())
				{
					StringBuilder sb = new StringBuilder();
					Type[] params = fe.getGenericParameterTypes();
					for(int i = 0; i < params.length; ++i)
						sb.append(params[i].getTypeName()).append(" ").append(fe.getParameters()[i].getName()).append(i < params.length - 1 ? ", " : "");
					String paramsStr = sb.toString();
					ColoredLux.LOG.info(indentation + "- " + Modifier.toString(fe.getModifiers()) + " " + fe.getGenericReturnType().getTypeName() + " " + fe.getName() + "(" + paramsStr + ");");
				}

				ColoredLux.LOG.info(indentation + "--------------------------------------------");
				ColoredLux.LOG.info(indentation + "-------------------FIELDS-------------------");
				ColoredLux.LOG.info(indentation + "--------------------------------------------");

				for(Field fe : f.getClass().getFields())
				{
					fe.setAccessible(true);
					try
					{
						Object o = fe.get(f);
						if(o instanceof String)
							o = JSONObject.quote(o.toString());

						ColoredLux.LOG.info(indentation + "- " + Modifier.toString(fe.getModifiers()) + " " + fe.getGenericType().getTypeName() + " " + fe.getName() + " = " + o + ";");
					} catch(Throwable e)
					{
						ColoredLux.LOG.info(indentation + "- " + Modifier.toString(fe.getModifiers()) + " " + fe.getGenericType().getTypeName() + " " + fe.getName() + " = <UNABLE TO FETCH>;");
					}
				}

				ColoredLux.LOG.info(indentation + "--------------------------------------------");
				if(indent < 2) ColoredLux.LOG.info(indentation + "DUMP " + dumpId + ".");
				ColoredLux.LOG.info(indentation + "------------------END DUMP------------------");
			}
			return dumpId;
		}
	}

	public static class LuxPackJSInternal
	{
		public static AbstractLuxPack contextPack;
		public static LuxPackAPIv2 api;

		public static void addTileScript(String typeString, String path) throws ClassNotFoundException
		{
			Class<?> c = Class.forName(typeString);
			if(TileEntity.class.isAssignableFrom(c))
			{
				Class<? extends TileEntity> type = c.asSubclass(TileEntity.class);
				JSSource src = contextPack.createJSSource(path);
				if(src.exists())
				{
					src = src
							.processImports()
							.inheritClassMethods(TScriptJSInternal.class)
							.addClassPointer(ColoredLight.class, "Light")
							.addClassPointer(JSBlockPos.class, "BlockPos");

					api.tsources.get(contextPack.location).put(type, new Tuple<>(contextPack.location.getName() + "/" + path, src));
				}
			}
		}

		public static void addEntityScript(String typeString, String path) throws ClassNotFoundException
		{
			EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(typeString));
			if(entry == null) throw new ClassNotFoundException("Unable to find entity " + typeString);
			else
			{
				JSSource src = contextPack.createJSSource(path);
				if(src.exists())
				{
					src = src
							.processImports()
							.inheritClassMethods(TScriptJSInternal.class)
							.addClassPointer(ColoredLight.class, "Light")
							.addClassPointer(JSBlockPos.class, "BlockPos");

					api.esources.get(contextPack.location).put(entry, new Tuple<>(contextPack.location.getName() + "/" + path, src));
				}
			}
		}
	}
}