package org.zeith.lux.luxpack;

import com.zeitheron.hammercore.lib.zlib.error.JSONException;
import com.zeitheron.hammercore.lib.zlib.io.IOUtils;
import com.zeitheron.hammercore.lib.zlib.json.*;
import com.zeitheron.hammercore.lib.zlib.utils.IndexedMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.*;
import org.zeith.lux.ColoredLux;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class LuxPackRepository
{
	public static final LuxPackRepository INSTANCE = new LuxPackRepository();
	private static final ResourceLocation UNKNOWN_PACK_TEXTURE = new ResourceLocation("lux", "textures/misc/unknown_pack.png");
	
	public IndexedMap<Entry, AbstractLuxPack> enabledPacks = new IndexedMap<>();
	public final List<Entry> availablePacks = new ArrayList<>();
	public final List<Entry> activePacks = new ArrayList<>();
	
	public static LuxPackRepository getInstance()
	{
		return INSTANCE;
	}
	
	public void refreshAvailablePacks()
	{
		File dir = getLuxPackDir();
		for(Entry e : availablePacks) Minecraft.getMinecraft().addScheduledTask(e::cleanup);
		List<File> fileActives = activePacks.stream().map(e -> e.location).collect(Collectors.toList());
		availablePacks.clear();
		activePacks.clear();
		for(File candidate : dir.listFiles())
		{
			AbstractLuxPack decoded = AbstractLuxPack.decode(candidate);
			if(decoded != null) try
			{
				Entry e = new Entry(decoded.getPackMeta(), candidate, decoded.getPackIcon());
				availablePacks.add(e);
				if(fileActives.contains(e.location)) activePacks.add(e);
				decoded.close();
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public List<Entry> getRepositoryEntriesAll()
	{
		return availablePacks;
	}
	
	public List<Entry> getRepositoryEntries()
	{
		return activePacks;
	}
	
	public void setRepositories(List<Entry> list)
	{
		activePacks.clear();
		activePacks.addAll(list);
		saveConfig();
	}
	
	public File getLuxPackDir()
	{
		File dir = new File(Minecraft.getMinecraft().gameDir, "luxpacks");
		if(dir.isFile()) dir.delete();
		if(!dir.isDirectory())
		{
			dir.mkdirs();
			
			try(InputStream in = ColoredLux.class.getResourceAsStream("/assets/lux/mc.lux"); FileOutputStream out = new FileOutputStream(new File(dir, "Minecraft.zip")))
			{
				IOUtils.pipeData(in, out);
			} catch(IOException e)
			{
				ColoredLux.LOG.warn("Failed to extract vanilla lux pack!");
				e.printStackTrace();
			}
		}
		return dir;
	}
	
	File enabledPacksJSON;
	
	public void setup(File configFile)
	{
		if(enabledPacksJSON == null)
		{
			enabledPacksJSON = configFile;
			loadConfig();
		}
	}
	
	List<String> enableOrder = Collections.emptyList();
	
	public void loadConfig()
	{
		JSONObject obj = new JSONObject();
		obj.put("#comment", "This file is managed automatically from within the game, please don't touch unless you know what you are doing!");
		obj.put("enabled", new JSONArray());
		if(enabledPacksJSON.isFile())
		{
			try(FileInputStream in = new FileInputStream(enabledPacksJSON))
			{
				JSONObject root = new JSONTokener(in).nextValueOBJ().orElse(null);
				root.getJSONArray("enabled");
				obj = root;
			} catch(IOException e)
			{
				e.printStackTrace();
			} catch(JSONException e)
			{
				ColoredLux.LOG.warn("Corrupted " + enabledPacksJSON.getName() + "! Reverting to default.");
				e.printStackTrace();
				try(FileOutputStream out = new FileOutputStream(enabledPacksJSON))
				{
					out.write(obj.toString(2).getBytes());
				} catch(IOException e2)
				{
					e2.printStackTrace();
				}
			} catch(NullPointerException npe)
			{
				if(enabledPacksJSON.delete())
				{
					loadConfig();
					return;
				} else
				{
					NullPointerException n = new NullPointerException("Failed to load config JSON file.");
					n.setStackTrace(npe.getStackTrace());
					throw n;
				}
			}
		} else
		{
			try(FileOutputStream out = new FileOutputStream(enabledPacksJSON))
			{
				out.write(obj.toString(2).getBytes());
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
		JSONArray enabledJA = obj.getJSONArray("enabled");
		List<File> files = new ArrayList<>();
		
		File dir = getLuxPackDir();
		
		availablePacks.clear();
		activePacks.clear();
		
		enableOrder = new ArrayList<>(enabledJA.length());
		for(int i = 0; i < enabledJA.length(); ++i)
		{
			files.add(new File(dir, enabledJA.getString(i)));
			enableOrder.add(enabledJA.getString(i));
		}
		enableOrder = Collections.unmodifiableList(enableOrder);
		
		Map<String, Entry> all = new HashMap<>();
		for(File candidate : dir.listFiles())
		{
			AbstractLuxPack decoded = AbstractLuxPack.decode(candidate);
			if(decoded != null) try
			{
				Entry e = new Entry(decoded.getPackMeta(), candidate, decoded.getPackIcon());
				availablePacks.add(e);
				all.put(candidate.getName(), e);
				decoded.close();
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
		for(File f : files)
			if(all.containsKey(f.getName()))
				activePacks.add(all.get(f.getName()));
	}
	
	public List<String> getEnableOrder()
	{
		return enableOrder;
	}
	
	public void saveConfig()
	{
		JSONObject obj = new JSONObject();
		obj.put("#comment", "This file is managed automatically from within the game, please don't touch unless you know what you are doing!");
		JSONArray enabled = new JSONArray();
		for(Entry e : activePacks)
			enabled.put(e.location.getName());
		obj.put("enabled", enabled);
		try(FileOutputStream out = new FileOutputStream(enabledPacksJSON))
		{
			out.write(obj.toString(2).getBytes());
		} catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void reload()
	{
		if(enabledPacksJSON == null) return;
		
		for(AbstractLuxPack pack : enabledPacks.getValues())
		{
			pack.disable();
			try
			{
				pack.close();
			} catch(IOException e)
			{
			}
		}
		
		enabledPacks.clear();
		loadConfig();
		
		for(Entry e : activePacks)
		{
			AbstractLuxPack decoded = AbstractLuxPack.decode(e.location);
			if(decoded != null)
			{
				decoded.enable();
				enabledPacks.put(e, decoded);
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public class Entry
	{
		public final LuxPackMeta pack;
		public final File location;
		public final BufferedImage image;
		
		public Entry(LuxPackMeta pack, File location, BufferedImage image)
		{
			this.pack = pack;
			this.location = location;
			this.image = image;
		}
		
		private ResourceLocation locationTexturePackIcon;
		
		public void bindTexturePackIcon(TextureManager mgr)
		{
			if(this.locationTexturePackIcon == null)
			{
				BufferedImage bufferedimage = image;
				
				if(bufferedimage == null)
				{
					try
					{
						bufferedimage = TextureUtil.readBufferedImage(Minecraft.getMinecraft().getResourceManager().getResource(UNKNOWN_PACK_TEXTURE).getInputStream());
					} catch(IOException ioexception)
					{
						throw new Error("Couldn't bind resource pack icon", ioexception);
					}
				}
				
				this.locationTexturePackIcon = mgr.getDynamicTextureLocation("luxpackicon", new DynamicTexture(bufferedimage));
			}
			
			mgr.bindTexture(this.locationTexturePackIcon);
		}
		
		public void cleanup()
		{
			if(this.locationTexturePackIcon != null)
				Minecraft.getMinecraft().getTextureManager().deleteTexture(this.locationTexturePackIcon);
		}
		
		public String getResourcePackName()
		{
			return pack != null ? pack.name : location.getName();
		}
		
		public String getTexturePackDescription()
		{
			return pack == null ? TextFormatting.RED + "Invalid pack.json (or missing 'pack' section)" : pack.description;
		}
		
		public int getPackFormat()
		{
			return pack == null ? 0 : pack.apiVersion;
		}
		
		@Override
		public boolean equals(Object p_equals_1_)
		{
			if(this == p_equals_1_)
			{
				return true;
			} else
			{
				return p_equals_1_ instanceof Entry && location.equals(((Entry) p_equals_1_).location);
			}
		}
		
		@Override
		public int hashCode()
		{
			return this.toString().hashCode();
		}
		
		@Override
		public String toString()
		{
			return "LuxEntry{" +
					"name=" + location.getName() +
					'}';
		}
	}
}