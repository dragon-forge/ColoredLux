package org.zeith.lux.luxpack.apis;

import org.zeith.lux.luxpack.AbstractLuxPack;

public interface ILuxPackAPI
{
	void hookLuxPack(AbstractLuxPack pack);

	void unhookLuxPack(AbstractLuxPack pack);
}