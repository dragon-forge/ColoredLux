package org.zeith.lux.api.glsl;

import com.zeitheron.hammercore.client.utils.gl.shading.*;
import org.zeith.hammerlib.util.mcf.Resources;
import org.zeith.lux.ColoredLux;

public class ShaderLightComputeVariable
		extends ResourceBasedShaderVar<Float>
{
	public static final ShaderResource RESOURCE = new ShaderResource(Resources.location(ColoredLux.MOD_ID, "shaders/compute_light.glsl"));
	public static float LIGHT_MULTIPLIER = 1F;
	
	protected final String lightStruct, lightCount, lightColor, lightIntensity;
	protected final boolean isLightColorOutVec4;
	
	public ShaderLightComputeVariable(String key, String lightStructNameIn, String lightCountIn, String lightColorOut, boolean isLightColorOutVec4, String lightIntensityOut)
	{
		super(key);
		this.lightStruct = lightStructNameIn != null ? lightStructNameIn : "Light";
		this.lightCount = lightCountIn != null ? lightCountIn : "lightCount";
		this.lightColor = lightColorOut != null ? lightColorOut : "lcolor";
		this.isLightColorOutVec4 = isLightColorOutVec4;
		this.lightIntensity = lightIntensityOut != null ? lightIntensityOut : "intens";
	}
	
	@Override
	public String getFallbackCode()
	{
		return "void computeColor(vec3 position, vec3 norm) {}";
	}
	
	@Override
	public ShaderResource getResource()
	{
		return RESOURCE;
	}
	
	@Override
	protected String postProcessCode(String code)
	{
		if(!isLightColorOutVec4)
			code = code.replace("LIGHT_COLOR = vec4(accum, 1.0);", "LIGHT_COLOR = accum;");
		return code
				.replace("computeColor", key)
				.replace("LIGHT_STRUCT", lightStruct)
				.replace("LIGHT_COUNT", lightCount)
				.replace("LIGHT_COLOR", lightColor)
				.replace("LIGHT_INTENSITY", lightIntensity);
	}
	
	@Override
	protected Float getState()
	{
		return LIGHT_MULTIPLIER;
	}
	
	@Override
	protected String compute(Float accumMult)
	{
		return code.replace("ACCUM_MULTIPLIER", Float.toString(accumMult));
	}
}