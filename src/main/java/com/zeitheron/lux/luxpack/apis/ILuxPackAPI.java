package com.zeitheron.lux.luxpack.apis;

import com.zeitheron.lux.luxpack.AbstractLuxPack;

public interface ILuxPackAPI
{
	void hookLuxPack(AbstractLuxPack pack);

	void unhookLuxPack(AbstractLuxPack pack);
}