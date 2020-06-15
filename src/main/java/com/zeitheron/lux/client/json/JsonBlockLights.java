package com.zeitheron.lux.client.json;

import com.google.common.base.Predicates;
import com.zeitheron.hammercore.api.lighting.ColoredLight;
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
import com.zeitheron.lux.api.event.ReloadLuxManagerEvent;
import com.zeitheron.lux.api.light.ILightBlockHandler;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class JsonBlockLights
{
	static File file;

	public static void setup(File file)
	{
		JsonBlockLights.file = file;

		if(!file.isFile())
		{
			try(FileOutputStream fos = new FileOutputStream(file))
			{
				fos.write("{\n\t\"#comment\": \"Check out https://gist.github.com/Zeitheron/af88ef85b495532f309f5dd9986760cb for example!\"\n}".getBytes());
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static final Map<Block, ILightBlockHandler> handlers = new HashMap<>();

	public static void reload()
	{
		if(file == null)
			return;

		handlers.clear();

		try(FileInputStream in = new FileInputStream(file))
		{
			JSONObject root = (JSONObject) new JSONTokener(new String(IOUtils.pipeOut(in))).nextValue();
			handlers.putAll(parse(root));
		} catch(IOException | JSONException ioe)
		{
			ioe.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void reloadLuxManager(ReloadLuxManagerEvent e)
	{
		handlers.forEach(LuxManager.BLOCK_LUMINANCES::put);
	}

	public static Map<Block, ILightBlockHandler> parse(JSONObject root)
	{
		Map<Block, ILightBlockHandler> h = new HashMap<>();
		for(String key : root.keySet())
		{
			if(key.startsWith("#"))
				continue;
			Block blk = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key));
			if(blk != null && blk != Blocks.AIR)
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
				h.put(blk, new PresedLightBlockHandler(lights.stream().map(ParsedLight::new).collect(Collectors.toList())));
			}
		}
		return h;
	}

	public static class ParsedLight
	{
		public Predicate<IBlockState> states;
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

			JSONObject state = obj.optJSONObject("state");

			if(state != null)
			{
				List<String> keys = new ArrayList<>(state.keySet());
				List<String> values = new ArrayList<>();
				for(int i = 0; i < keys.size(); ++i)
					values.add(Objects.toString(state.opt(keys.get(i))));
				states = s ->
				{
					Map<String, String> kvs = new HashMap<>();

					s.getProperties().forEach((key, value1) -> kvs.put(key.getName(), ((IProperty) key).getName(value1)));
					if(s instanceof IExtendedBlockState)
						((IExtendedBlockState) s).getUnlistedProperties().forEach((key, value1) -> value1.ifPresent(trueValue -> kvs.put(key.getName(), value1.get().toString())));

					for(int i = 0; i < keys.size(); ++i)
					{
						String p = keys.get(i);
						String r = values.get(i);

						String val = kvs.getOrDefault(p, "undefined");
						if(!Objects.equals(r, val))
							return false;
					}

					return true;
				};
			} else
				states = Predicates.alwaysTrue();
		}

		public ColoredLight.Builder build(BlockPos pos)
		{
			ExprFlicker flick = new ExprFlicker(pos);
			try
			{
				ColoredLight.Builder b = ColoredLight.builder().pos(pos).radius(radius.apply(flick)).color(red.apply(flick), green.apply(flick), blue.apply(flick));
				float a = alpha.apply(flick);
				if(a > 0F && a <= 1F) return b.alpha(a);
			} catch(RuntimeException e)
			{
				System.out.println(e.getMessage());
			}
			return null;
		}
	}

	public static class ExprFlicker
			extends ExpressionFunction
	{
		BlockPos pos;
		Random rand;

		public ExprFlicker(BlockPos pos)
		{
			super("flicker");
			this.pos = pos;
			this.rand = new Random(pos.toLong());
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
				return FastNoise.noise(pos.getX(), pos.getZ() + ((System.nanoTime() / 100000D) % (x * 1000)) / (float) x, 5) / 127F;
			if(functionName.compareToIgnoreCase("rng") == 0)
				return rand.nextDouble() * x;
			return x;
		}
	}

	public static class PresedLightBlockHandler
			implements ILightBlockHandler
	{
		public final List<ParsedLight> lights;

		public PresedLightBlockHandler(List<ParsedLight> lights)
		{
			this.lights = lights;
		}

		Long2ObjectArrayMap<List<ColoredLight.Builder>> builtLights = new Long2ObjectArrayMap<>();
		Long2ObjectArrayMap<List<ColoredLight.Builder>> builtCache = new Long2ObjectArrayMap<>();

		@Override
		public void update(IBlockState state, BlockPos pos)
		{
			List<ColoredLight.Builder> builtLights = this.builtLights.computeIfAbsent(pos.toLong(), l -> new ArrayList<>());
			List<ColoredLight.Builder> builtCache = this.builtCache.computeIfAbsent(pos.toLong(), l -> new ArrayList<>());

			builtCache.clear();
			final List<ColoredLight.Builder> ccache = builtCache;
			lights.forEach(l ->
			{
				if(l.states.test(state))
					ccache.add(l.build(pos));
			});

			this.builtLights.put(pos.toLong(), builtCache);
			this.builtCache.put(pos.toLong(), builtLights);
		}

		@Override
		public void createLights(World world, BlockPos pos, IBlockState state, GatherLightsEvent e)
		{
			builtLights.getOrDefault(pos.toLong(), Collections.emptyList()).forEach(e::add);
		}
	}
}