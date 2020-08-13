package com.zeitheron.lux.api.fx;

import com.zeitheron.hammercore.client.utils.gl.shading.ShaderVar;
import com.zeitheron.hammercore.lib.zlib.utils.PriorityList;
import com.zeitheron.lux.api.HWSupport;

public class ShaderVarFX
		extends ShaderVar
{
	final PriorityList<FX> l = new PriorityList<>();

	{
		l.setPriority(FX::priority);
	}

	final HWSupport.EnumShaderVersion version;

	public ShaderVarFX(String key, HWSupport.EnumShaderVersion version)
	{
		super(key);
		this.version = version;
	}

	@Override
	protected String getCurrentValue()
	{
		StringBuilder sb = new StringBuilder();

		return sb.toString();
	}
}