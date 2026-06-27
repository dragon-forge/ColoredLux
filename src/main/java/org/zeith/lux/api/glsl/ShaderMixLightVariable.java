package org.zeith.lux.api.glsl;

import com.zeitheron.hammercore.client.utils.gl.shading.*;
import org.zeith.hammerlib.util.mcf.Resources;
import org.zeith.lux.ColoredLux;

public class ShaderMixLightVariable
		extends ResourceBasedShaderVar<Void>
{
	public static final ShaderResource RESOURCE = new ShaderResource(Resources.location(ColoredLux.MOD_ID, "shaders/mix_light.glsl"));
	
	public ShaderMixLightVariable(String key)
	{
		super(key);
	}
	
	@Override
	public String getFallbackCode()
	{
		return "vec3 mixLights(int colMix, vec3 mcLight, vec3 luxLight, float luxIntensity) {\n" +
		       "    return max(mcLight, luxLight);\n" +
		       "}";
	}
	
	@Override
	protected String postProcessCode(String code)
	{
		return code
				.replace("mixLights", key);
	}
	
	@Override
	public ShaderResource getResource()
	{
		return RESOURCE;
	}
	
	@Override
	protected Void getState()
	{
		return null;
	}
	
	@Override
	protected String compute(Void unused)
	{
		return code;
	}
}