package com.zeitheron.lux.client.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Predicates;
import com.zeitheron.hammercore.lib.zlib.error.JSONException;
import com.zeitheron.hammercore.lib.zlib.io.IOUtils;
import com.zeitheron.hammercore.lib.zlib.json.JSONArray;
import com.zeitheron.hammercore.lib.zlib.json.JSONObject;
import com.zeitheron.hammercore.lib.zlib.json.JSONTokener;
import com.zeitheron.hammercore.utils.FastNoise;
import com.zeitheron.hammercore.utils.math.ExpressionEvaluator;
import com.zeitheron.hammercore.utils.math.functions.ExpressionFunction;
import com.zeitheron.lux.api.LuxManager;
import com.zeitheron.lux.api.event.GatherLightsEvent;
import com.zeitheron.lux.api.light.ILightEntityHandler;
import com.zeitheron.lux.api.light.Light;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class JsonEntityLights
{
	static File file;
	
	public static void setup(File file)
	{
		JsonEntityLights.file = file;
		
		if(!file.isFile())
		{
			try(FileOutputStream fos = new FileOutputStream(file))
			{
				fos.write("{\n\t\"#comment\": \"Check out https://gist.github.com/Zeitheron/d3da4b5f8891c90106c0dc6535357259 for example!\"\n}".getBytes());
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static final Map<EntityEntry, ILightEntityHandler> handlers = new HashMap<>();
	
	public static void reload()
	{
		if(file == null)
			return;
		
		if(!handlers.isEmpty())
		{
			handlers.keySet().forEach(LuxManager.ENTITY_LUMINANCES::remove);
			handlers.clear();
		}
		
		try(FileInputStream in = new FileInputStream(file))
		{
			JSONObject root = (JSONObject) new JSONTokener(new String(IOUtils.pipeOut(in))).nextValue();
			
			for(String key : root.keySet())
			{
				if(key.startsWith("#"))
					continue;
				EntityEntry blk = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(key));
				if(blk != null)
				{
					Object o = root.get(key);
					List<JSONObject> lights = new ArrayList<>();
					if(o instanceof JSONArray)
					{
						JSONArray a = (JSONArray) o;
						for(int i = 0; i < a.length(); ++i)
							lights.add(a.getJSONObject(i));
					} else if(o instanceof JSONObject)
						lights.add((JSONObject) o);
					handlers.put(blk, new PresetLightEntityHandler(lights.stream().map(ParsedLight::new).collect(Collectors.toList())));
				}
			}
		} catch(IOException | JSONException ioe)
		{
			ioe.printStackTrace();
		}
		
		handlers.forEach(LuxManager.ENTITY_LUMINANCES::put);
	}
	
	public static class ParsedLight
	{
		public boolean valid;
		public Predicate<Entity> entities;
		public Function<ExprFlicker, Float> red, green, blue, alpha, radius;
		
		public ParsedLight(JSONObject obj)
		{
			JSONObject color = obj.optJSONObject("color");
			
			String radius = obj.optString("radius");
			if(radius == null)
				this.radius = p -> 16F;
			else
				this.radius = p -> (float) ExpressionEvaluator.evaluateDouble(radius, p);
			
			if(color != null)
			{
				String rf = color.optString("red");
				String gf = color.optString("green");
				String bf = color.optString("blue");
				String af = color.optString("alpha");
				
				if(rf == null || rf.isEmpty())
					red = p -> 0F;
				else
					red = p -> (float) ExpressionEvaluator.evaluateDouble(rf, p);
				
				if(gf == null || gf.isEmpty())
					green = p -> 0F;
				else
					green = p -> (float) ExpressionEvaluator.evaluateDouble(gf, p);
				
				if(bf == null || bf.isEmpty())
					blue = p -> 0F;
				else
					blue = p -> (float) ExpressionEvaluator.evaluateDouble(bf, p);
				
				if(af == null || af.isEmpty())
					alpha = p -> 1F;
				else
					alpha = p -> (float) ExpressionEvaluator.evaluateDouble(af, p);
			} else
				red = green = blue = alpha = p -> 1F;
			
			JSONObject entity = obj.optJSONObject("entity");
			
			if(entity != null)
			{
				List<String> keys = new ArrayList<>(entity.keySet());
				List<String> values = new ArrayList<>();
				for(int i = 0; i < keys.size(); ++i)
					values.add(Objects.toString(entity.opt(keys.get(i))));
				entities = s ->
				{
					NBTTagCompound comp = s.serializeNBT();
					/*
					for(int i = 0; i < keys.size(); ++i)
					{
						String p = keys.get(i);
						String r = values.get(i);
						
					}
					*/
					return true;
				};
			} else
			{
				entities = Predicates.alwaysTrue();
				valid = false;
			}
		}
		
		public Light.Builder build(Entity pos)
		{
			ExprFlicker flick = new ExprFlicker(pos);
			try
			{
				Light.Builder b = Light.builder().pos(pos, 1F).radius(radius.apply(flick)).color(red.apply(flick), green.apply(flick), blue.apply(flick));
				float a = alpha.apply(flick);
				if(a > 0F && a <= 1F)
					return b.alpha(a);
			} catch(RuntimeException e)
			{
				System.out.println(e.getMessage());
			}
			return null;
		}
	}
	
	public static class ExprFlicker extends ExpressionFunction
	{
		Entity entity;
		Random rand;
		
		public ExprFlicker(Entity entity)
		{
			super("flicker");
			this.entity = entity;
			this.rand = new Random(entity.getUniqueID().getMostSignificantBits());
		}
		
		@Override
		public boolean accepts(String functionName, double x)
		{
			return super.accepts(functionName, x) || functionName.compareToIgnoreCase("rng") == 0;
		}
		
		@Override
		public double apply(String functionName, double x)
		{
			if(functionName.compareToIgnoreCase("flicker") == 0)
				return FastNoise.noise((float) entity.getUniqueID().getMostSignificantBits(), ((float) entity.getUniqueID().getLeastSignificantBits()) + ((System.nanoTime() / 100000D) % (x * 1000)) / (float) x, 5) / 127F;
			if(functionName.compareToIgnoreCase("rng") == 0)
				return rand.nextDouble() * x;
			return x;
		}
	}
	
	public static class PresetLightEntityHandler implements ILightEntityHandler
	{
		public final List<ParsedLight> lights;
		
		public PresetLightEntityHandler(List<ParsedLight> lights)
		{
			this.lights = lights;
		}
		
		Map<Integer, List<Light.Builder>> builtLights = new HashMap<>();
		Map<Integer, List<Light.Builder>> builtCache = new HashMap<>();
		
		@Override
		public void update(Entity entity)
		{
			List<Light.Builder> builtLights = this.builtLights.computeIfAbsent(entity.getEntityId(), l -> new ArrayList<>());
			List<Light.Builder> builtCache = this.builtCache.computeIfAbsent(entity.getEntityId(), l -> new ArrayList<>());
			builtCache.clear();
			final List<Light.Builder> ccache = builtCache;
			lights.forEach(l ->
			{
				if(l.entities.test(entity))
					ccache.add(l.build(entity));
			});
			this.builtLights.put(entity.getEntityId(), builtCache);
			this.builtCache.put(entity.getEntityId(), builtLights);
		}
		
		@Override
		public void remove(int persistent)
		{
			builtCache.remove(persistent);
			builtLights.remove(persistent);
		}
		
		@Override
		public void createLights(Entity entity, GatherLightsEvent e)
		{
			builtLights.getOrDefault(entity.getEntityId(), Collections.emptyList()).stream().peek(b -> b.pos(entity, e.getPartialTicks())).forEach(e::add);
		}
	}
}