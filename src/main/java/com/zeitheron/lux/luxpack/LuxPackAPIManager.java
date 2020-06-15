package com.zeitheron.lux.luxpack;

import com.zeitheron.lux.luxpack.apis.ILuxPackAPI;
import com.zeitheron.lux.luxpack.apis.LuxPackAPIv1;

import java.util.HashMap;
import java.util.Map;

public class LuxPackAPIManager
{
	private static final Map<Integer, ILuxPackAPI> APIS = new HashMap<>();
	private static final ILuxPackAPI newestAPI;
	private static final int newestAPIVersion;

	static
	{
		APIS.put(1, new LuxPackAPIv1());

		newestAPIVersion = 1;
		newestAPI = APIS.get(newestAPIVersion);
	}

	public static ILuxPackAPI getAPI(int api)
	{
		return APIS.getOrDefault(api, newestAPI);
	}

	public static ILuxPackAPI getNewestAPI()
	{
		return newestAPI;
	}

	public static int getNewestAPIVersion()
	{
		return newestAPIVersion;
	}
}