#version 120

varying float intens;
varying vec4 lcolor;
varying float dist2Obj;
varying vec3 Normal;

uniform vec3 worldTint;
uniform float worldTintIntensity;
uniform float saturation;

uniform sampler2D sampler;
uniform sampler2D lightmap;
uniform vec4 colorMult;
uniform int vanillaTracing;
uniform int colMix;
uniform float fogIntensity;

const vec3 NormalLightDir = normalize(vec3(0.5f, 1.0f, 0.5f));

float luma(vec3 color)
{
    return dot(color, vec3(0.299f, 0.587f, 0.114f));
}

vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.0f, -1.0f / 3.0f, 2.0f / 3.0f, -1.0f);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0f * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0f, 2.0f / 3.0f, 1.0f / 3.0f, 3.0f);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0f - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0f, 1.0f), c.y);
}

void main()
{
    vec3 lightdark = texture2D(lightmap, gl_TexCoord[1].st).rgb;
    lightdark = clamp(lightdark, 0.0f, 1.0f);
    vec3 lcolor_2 = clamp(lcolor.rgb * intens, 0.0f, 1.0f);
    if (vanillaTracing == 1) lcolor_2 = lcolor_2 * pow(luma(lightdark), 2);

    if (colMix == 1) lightdark = lightdark + lcolor_2;//More washed-out, but more physically correct
    else lightdark = max(lightdark, lcolor_2);//Vivid but unrealistic

    vec4 baseColor = gl_Color * texture2D(sampler, gl_TexCoord[0].st);
    baseColor = baseColor * vec4(mix(vec3(1.0f), worldTint, worldTintIntensity), 1.0f);

    baseColor = baseColor * vec4(lightdark, 1.0f);

    float dist = max(dist2Obj - gl_Fog.start, 0.0f) / (gl_Fog.end - gl_Fog.start);
    float fog = gl_Fog.density * dist * fogIntensity;
    fog = 1.0f - clamp(fog, 0.0f, 1.0f);
    baseColor = vec4(mix(vec3(gl_Fog.color), baseColor.rgb, fog).rgb, baseColor.a);

    vec4 amult = vec4(vec3(1.0f) * (1.0f - colorMult.a) + colorMult.rgb * colorMult.a, 1.0f);
    baseColor = baseColor * amult;

    vec3 hsv = rgb2hsv(baseColor.rgb);
    hsv.y *= saturation;

    float brightessInfluence = mix(0.5f, 0.1f, intens);
    float brightness = clamp((1.0f - brightessInfluence) + max(dot(normalize(Normal), NormalLightDir), 0.0f) * brightessInfluence, 0.5f, 1.0f);

    gl_FragColor = vec4(hsv2rgb(hsv) * brightness, baseColor.a);
}