package com.zeitheron.lux.luxpack;

import com.zeitheron.hammercore.lib.nashorn.JSSource;
import com.zeitheron.hammercore.lib.zlib.error.JSONException;
import com.zeitheron.hammercore.lib.zlib.io.IOUtils;
import com.zeitheron.hammercore.lib.zlib.json.JSONObject;
import com.zeitheron.hammercore.lib.zlib.json.JSONTokener;
import com.zeitheron.lux.ColoredLux;
import com.zeitheron.lux.api.event.CreateLuxPackEvent;
import com.zeitheron.lux.api.light.ILightBlockHandler;
import com.zeitheron.lux.api.light.ILightEntityHandler;
import com.zeitheron.lux.client.json.JsonBlockLights;
import com.zeitheron.lux.client.json.JsonEntityLights;
import com.zeitheron.lux.luxpack.apis.ILuxPackAPI;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.EntityEntry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractLuxPack
		implements Closeable
{
	public final File location;
	private ILuxPackAPI api;
	
	public AbstractLuxPack(File location) throws IOException
	{
		if(!location.exists()) throw new FileNotFoundException(location.getAbsolutePath());
		this.location = location;
	}
	
	public static AbstractLuxPack decode(File location)
	{
		AbstractLuxPack pack = null;
		
		try
		{
			if(location.isDirectory()) pack = new FileLuxPack(location);
			
			if(location.isFile() && (location.getName().endsWith(".zip") || location.getName().endsWith(".lux")))
				pack = new ZipLuxPack(location);
		} catch(IOException e)
		{
			e.printStackTrace();
		}
		
		if(pack == null)
		{
			// Let anyone have their respective pack decoders
			CreateLuxPackEvent event = new CreateLuxPackEvent(location);
			MinecraftForge.EVENT_BUS.post(event);
			pack = event.getPack();
		}
		
		if(pack != null && pack.getPackMeta() == null)
			pack = null;
		
		return pack;
	}
	
	/**
	 * Returns an input created from the pack.
	 *
	 * @param path The slash-separated path to file.
	 * @return input steam with data, or null, if path does not point to a valid file.
	 */
	public abstract InputStream createInput(String path) throws IOException;
	
	public abstract boolean doesFileExist(String path);
	
	protected LuxPackMeta meta;
	
	public JSSource createJSSource(String path)
	{
		return new JSSource()
		{
			@Override
			public String read()
			{
				byte[] data = new byte[0];
				try(InputStream in = createInput(path))
				{
					if(in != null) data = IOUtils.pipeOut(in);
				} catch(IOException ignored)
				{
				}
				return new String(data);
			}
			
			@Override
			public boolean exists()
			{
				return doesFileExist(path);
			}
		};
	}
	
	protected void loadPackMeta() throws IOException, JSONException
	{
		try(InputStream in = createInput("pack.json"))
		{
			if(in == null) return;
			JSONObject root = new JSONTokener(in).nextValueOBJ().orElse(null);
			meta = new LuxPackMeta(this, root);
			api = LuxPackAPIManager.getAPI(meta.apiVersion);
			if(meta.apiVersion > LuxPackAPIManager.getNewestAPIVersion())
			{
				api = null;
				ColoredLux.LOG.warn("LuxPack " + location.getName() + " uses unsupported API version " + meta.apiVersion);
			}
		}
	}
	
	public LuxPackMeta getPackMeta()
	{
		if(meta == null)
		{
			try
			{
				loadPackMeta();
			} catch(IOException | JSONException e)
			{
				ColoredLux.LOG.warn("Failed to load meta from pack.json in LuxPack " + location.getName());
				e.printStackTrace();
			}
		}
		
		return meta;
	}
	
	boolean loadBlockLights = true, loadEntityLights = true;
	Map<Block, ILightBlockHandler> loadedBlockLights = Collections.emptyMap();
	Map<EntityEntry, ILightEntityHandler> loadedEntityLights = Collections.emptyMap();
	
	public Map<Block, ILightBlockHandler> getBlockLights()
	{
		if(loadBlockLights)
		{
			loadBlockLights = false;
			try(InputStream in = createInput("blocks.json"))
			{
				if(in != null)
				{
					JSONObject root = new JSONTokener(in).nextValueOBJ().orElse(null);
					if(root != null) loadedBlockLights = JsonBlockLights.parse(root);
				}
			} catch(IOException e)
			{
				ColoredLux.LOG.warn("Failed to load blocks.json for lux pack " + getLuxPackName());
				e.printStackTrace();
			}
		}
		return loadedBlockLights;
	}
	
	public Map<EntityEntry, ILightEntityHandler> getEntityLights()
	{
		if(loadEntityLights)
		{
			loadEntityLights = false;
			try(InputStream in = createInput("entities.json"))
			{
				if(in != null)
				{
					JSONObject root = new JSONTokener(in).nextValueOBJ().orElse(null);
					if(root != null) loadedEntityLights = JsonEntityLights.parse(root);
				}
			} catch(IOException e)
			{
				ColoredLux.LOG.warn("Failed to load entities.json for lux pack " + getLuxPackName());
				e.printStackTrace();
			}
		}
		return loadedEntityLights;
	}
	
	protected String getLuxPackName()
	{
		LuxPackMeta meta = getPackMeta();
		return meta != null ? meta.name : location.getName();
	}
	
	public void enable()
	{
		if(api != null)
			api.hookLuxPack(this);
	}
	
	public void disable()
	{
		if(api != null)
			api.unhookLuxPack(this);
	}
	
	/**
	 * Reads a pack icon to display in lux pack list.
	 *
	 * @return the image of this pack, or null, if it does not exist or meet 64x64 requirements.
	 */
	public final BufferedImage getPackIcon() throws IOException
	{
		try(InputStream in = createInput("icon.png"))
		{
			if(in == null) return null;
			PngSizeInfo info = new PngSizeInfo(in);
			if(info.pngHeight != 128 || info.pngWidth != 128)
			{
				ColoredLux.LOG.warn("");
				return null;
			}
		}
		try(InputStream in2 = createInput("icon.png"))
		{
			return ImageIO.read(in2);
		}
	}
	
	@Override
	public String toString()
	{
		return "AbstractLuxPack{" +
				"location=" + location +
				'}';
	}
}