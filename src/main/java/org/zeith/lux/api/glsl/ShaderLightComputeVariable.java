package org.zeith.lux.api.glsl;

import com.zeitheron.hammercore.client.utils.gl.shading.ShaderVar;
import com.zeitheron.hammercore.lib.zlib.io.IOUtils;
import net.minecraft.client.resources.*;
import org.zeith.hammerlib.util.mcf.Resources;
import org.zeith.lux.ColoredLux;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ShaderLightComputeVariable
		extends ShaderVar<Void>
{
	protected final String functionName, lightStruct, lightCount, lightColor, lightIntensity;
	protected final boolean isLightColorOutVec4;
	
	private String code;
	
	public ShaderLightComputeVariable(String key, String lightStructNameIn, String lightCountIn, String lightColorOut, boolean isLightColorOutVec4, String lightIntensityOut)
	{
		super(key);
		this.functionName = key;
		this.lightStruct = lightStructNameIn != null ? lightStructNameIn : "Light";
		this.lightCount = lightCountIn != null ? lightCountIn : "lightCount";
		this.lightColor = lightColorOut != null ? lightColorOut : "lcolor";
		this.isLightColorOutVec4 = isLightColorOutVec4;
		this.lightIntensity = lightIntensityOut != null ? lightIntensityOut : "intens";
	}
	
	@Override
	public void onReload(IResourceManager resources)
	{
		try(IResource res = resources.getResource(Resources.location(ColoredLux.MOD_ID, "shaders/compute_light.glsl")); InputStream in = res.getInputStream())
		{
			String str = new String(IOUtils.pipeOut(in), StandardCharsets.UTF_8)
					.replace("computeColor", functionName)
					.replace("LIGHT_STRUCT", lightStruct)
					.replace("LIGHT_COUNT", lightCount);
			if(!isLightColorOutVec4)
				str = str.replace("LIGHT_COLOR = vec4(accum, 1.0);", "LIGHT_COLOR = accum;");
			code = str
					.replace("LIGHT_COLOR", lightColor)
					.replace("LIGHT_INTENSITY", lightIntensity);
		} catch(Exception e)
		{
			code = "void computeColor(vec3 position, vec3 norm) {}";
		}
		super.onReload(resources);
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