package org.zeith.lux.client.gui.repo;

import com.zeitheron.hammercore.lib.zlib.error.JSONException;
import com.zeitheron.hammercore.lib.zlib.error.NoVariableFoundInJSONException;
import com.zeitheron.hammercore.lib.zlib.json.JSONArray;
import com.zeitheron.hammercore.lib.zlib.json.JSONTokener;
import com.zeitheron.hammercore.lib.zlib.json.serapi.Jsonable;
import com.zeitheron.hammercore.lib.zlib.web.HttpRequest;

import java.util.*;
import java.util.stream.Stream;

public class GitLuxPackRepository
{
	private static long lastRequestTime = 0L;
	private static String lastRequestBody = null;
	private static final List<GitFile> files = new ArrayList<>();
	public static List<GitFile> fileView = Collections.unmodifiableList(files);

	public static Optional<JSONArray> getWebLuxPacks()
	{
		try
		{
			if(lastRequestBody == null || System.currentTimeMillis() / 1000L - lastRequestTime >= 300L)
			{
				lastRequestBody = HttpRequest.get("https://api.github.com/repos/dragon-forge/luxpacks/contents/1.12.2?ref=master")
						.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36")
						.body();
				lastRequestTime = System.currentTimeMillis() / 1000L;
			}

			return new JSONTokener(lastRequestBody).nextValueARR();
		} catch(JSONException e)
		{
			e.printStackTrace();
		}
		return Optional.empty();
	}

	public static void refreshWebLuxPacks()
	{
		files.clear();
		getWebLuxPacks().ifPresent(root ->
		{
			for(int i = 0; i < root.length(); ++i)
			{
				try
				{
					files.add(Jsonable.deserialize(root.getJSONObject(i), new GitFile()));
				} catch(NoVariableFoundInJSONException | JSONException e)
				{
				}
			}
		});
	}

	public static Stream<GitFile> search(String query)
	{
		return fileView
				.stream()
				.filter(file -> file.name.contains(query))
				.sorted(Comparator.comparing(a -> a.name));
	}

	public static class GitFile
			implements Jsonable
	{
		public String name;
		public String path;
		public String sha;
		public long size;
		public String url;
		public String html_url;
		public String git_url;
		public String download_url;
		public String type;

		public boolean isFile()
		{
			return Objects.equals(type, "file");
		}
	}
}