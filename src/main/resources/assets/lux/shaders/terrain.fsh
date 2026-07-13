#version 330 compatibility

in vec3 lcolor;
in float intens;
in float dist2Obj;

out vec4 fragColor;

uniform vec3 worldTint;
uniform float worldTintIntensity;
uniform float saturation;

uniform sampler2D albedo;
uniform sampler2D lightmap;
uniform vec3 playerPos;
uniform int colMix;
uniform int vanillaTracing;
uniform float fogIntensity;

uniform float chunkAlpha;

#variable mixLights

float luma(vec3 color)
{
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main()
{
    vec3 mcLight = texture2D(lightmap, gl_TexCoord[1].st).rgb;
    vec3 luxLight = clamp(lcolor * intens, 0.0, 1.0);

    if (vanillaTracing == 1) {
        float t = luma(mcLight);
        t *= t;
        luxLight = luxLight * t;
    }

    vec4 baseColor = gl_Color * texture2D(albedo, gl_TexCoord[0].st);
    baseColor = baseColor * vec4(mix(vec3(1.0), worldTint, worldTintIntensity), 1.0);
    baseColor = baseColor * vec4(mixLights(colMix, mcLight, luxLight), 1.0);

    float dist = max(dist2Obj - gl_Fog.start, 0.0) / (gl_Fog.end - gl_Fog.start);
    float fog = gl_Fog.density * dist * fogIntensity;
    fog = 1.0 - clamp(fog, 0.0, 1.0);
    baseColor = vec4(mix(vec3(gl_Fog.color), baseColor.xyz, fog).rgb, baseColor.a);

    vec3 hsv = rgb2hsv(baseColor.rgb);
    hsv.y *= saturation;
    fragColor = vec4(hsv2rgb(hsv), baseColor.a * chunkAlpha);
}