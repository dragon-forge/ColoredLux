#version 120

varying vec3 position;
varying vec4 lcolor;
varying float intens;

uniform vec3 worldTint;
uniform float worldTintIntensity;

uniform sampler2D sampler;
uniform sampler2D lightmap;
uniform vec3 playerPos;
uniform int colMix;
uniform int vanillaTracing;

float round(float f)
{
	if(fract(f) < 0.5f) return f - fract(f);
	else return f + 1.0f - fract(f);
}

float luma(vec3 color)
{
	return dot(color, vec3(0.299, 0.587, 0.114));
}

void main()
{
	vec3 lightdark = texture2D(lightmap,gl_TexCoord[1].st).rgb;
	lightdark = clamp(lightdark, 0.0f, 1.0f);
	vec3 lcolor_2 = clamp(lcolor.rgb * intens, 0.0f, 1.0f);
	if(vanillaTracing == 1) lcolor_2 = lcolor_2 * pow(luma(lightdark), 2);
	
	if(colMix == 1) lightdark = lightdark + lcolor_2;   //More washed-out, but more physically correct
	else lightdark = max(lightdark, lcolor_2); //Vivid but unrealistic
	
	vec4 baseColor = gl_Color * texture2D(sampler, gl_TexCoord[0].st);
	baseColor = baseColor * vec4(mix(vec3(1), worldTint, worldTintIntensity), 1.0f);

	baseColor = baseColor * vec4(lightdark, 1);
	vec3 dv = position - playerPos;
	float dist = max(sqrt(dv.x * dv.x + dv.y * dv.y + dv.z * dv.z) - gl_Fog.start, 0.0f) / (gl_Fog.end - gl_Fog.start);
	float fog = gl_Fog.density * dist;
	fog = 1.0f - clamp(fog, 0.0f, 1.0f);
	baseColor = vec4(mix(vec3(gl_Fog.color), baseColor.xyz, fog).rgb, baseColor.a);
	gl_FragColor = baseColor;
	// gl_FragColor = vec4(max(mix(baseColor.rgb * lightdark, baseColor.rgb * lcolor.rgb, intens), lightdark * baseColor.rgb), baseColor.a);
}