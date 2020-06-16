package com.zeitheron.lux.luxpack;

import com.zeitheron.lux.luxpack.apis.ILuxPackAPI;
import com.zeitheron.lux.luxpack.apis.LuxPackAPIv1;
import com.zeitheron.lux.luxpack.apis.LuxPackAPIv2;

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
		APIS.put(2, new LuxPackAPIv2());

		newestAPIVersion = 2;
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