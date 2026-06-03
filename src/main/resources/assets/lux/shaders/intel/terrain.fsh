#version 120

varying vec3 position;
varying vec4 lcolor;
varying float intens;
varying float dist2Obj;

uniform vec3 worldTint;
uniform float worldTintIntensity;
uniform float saturation;

uniform sampler2D sampler;
uniform sampler2D lightmap;
uniform vec3 playerPos;
uniform int colMix;
uniform int vanillaTracing;
uniform float fogIntensity;

uniform float chunkAlpha;

float luma(vec3 color)
{
	return dot(color, vec3(0.299f, 0.587f, 0.114f));
}

vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.f, -1.f / 3.f, 2.f / 3.f, -1.f);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.f * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.f, 2.f / 3.f, 1.f / 3.f, 3.f);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.f - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.f, 1.f), c.y);
}

void main()
{
	vec3 lightdark = texture2D(lightmap,gl_TexCoord[1].st).rgb;
	lightdark = clamp(lightdark, 0.f, 1.f);
	vec3 lcolor_2 = clamp(lcolor.rgb * intens, 0.f, 1.f);
    if (vanillaTracing == 1) {
        float t = luma(lightdark);
        t *= t;
        lcolor_2 = lcolor_2 * t;
    }
	
	if(colMix == 1) lightdark = lightdark + lcolor_2;   //More washed-out, but more physically correct
	else lightdark = max(lightdark, lcolor_2); //Vivid but unrealistic
	
	vec4 baseColor = gl_Color * texture2D(sampler, gl_TexCoord[0].st);
	baseColor = baseColor * vec4(mix(vec3(1), worldTint, worldTintIntensity), 1.f);

	baseColor = baseColor * vec4(lightdark, 1);
	vec3 dv = position - playerPos;

	float dist = max(dist2Obj - gl_Fog.start, 0.f) / (gl_Fog.end - gl_Fog.start);
	float fog = gl_Fog.density * dist * fogIntensity;
	fog = 1.f - clamp(fog, 0.f, 1.f);
	baseColor = vec4(mix(vec3(gl_Fog.color), baseColor.xyz, fog).rgb, baseColor.a);

    vec3 hsv = rgb2hsv(baseColor.rgb);
    hsv.y *= saturation;
    gl_FragColor = vec4(hsv2rgb(hsv), baseColor.a * chunkAlpha);
}