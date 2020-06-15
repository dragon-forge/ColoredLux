package com.zeitheron.lux.luxpack;

import com.zeitheron.hammercore.lib.zlib.json.JSONArray;
import com.zeitheron.hammercore.lib.zlib.json.JSONObject;
import com.zeitheron.lux.ColoredLux;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LuxPackMeta
{
	public final int apiVersion;
	public final String name;
	public final String[] authors;
	public final String description;
	public final List<ArtifactVersion> dependenciesParsed;

	public LuxPackMeta(AbstractLuxPack pack, JSONObject root)
	{
		apiVersion = root.getInt("api");

		name = root.optString("name", pack.location.getName());

		JSONArray array = root.optJSONArray("authors");
		if(array == null) authors = new String[0];
		else
		{
			authors = new String[array.length()];
			for(int i = 0; i < authors.length; ++i) authors[i] = array.getString(i);
		}

		description = root.optString("description", "No description provided.");

		array = root.optJSONArray("dependencies");
		if(array == null) dependenciesParsed = Collections.emptyList();
		else
		{
			List<ArtifactVersion> deps = new ArrayList<>();
			for(int i = 0; i < array.length(); ++i)
			{
				String dep = array.getString(i);

				try
				{
					deps.add(VersionParser.parseVersionReference(dep));
				} catch(Throwable er)
				{
					ColoredLux.LOG.warn("Failed to parse mod dependency: " + JSONObject.quote(dep));
					er.printStackTrace();
				}
			}
			this.dependenciesParsed = Collections.unmodifiableList(deps);
		}
	}

	public List<String> getMissingMods()
	{
		List<String> list = new ArrayList<>();
		for(ArtifactVersion artifact : dependenciesParsed)
		{
			ModContainer mod = Loader.instance().getIndexedModList().get(artifact.getLabel());
			if(mod == null || !artifact.containsVersion(mod.getProcessedVersion()))
			{
				String name = mod != null ? mod.getName() : artifact.getLabel();
				list.add(name + " " + artifact.getRangeString());
			}
		}
		return list;
	}
}