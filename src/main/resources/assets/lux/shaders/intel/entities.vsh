#version 150 compatibility

out vec4 lcolor;
out float intens;
out float dist2Obj;

struct Light
{
	vec4 color;
	vec3 position;
	float radius;
};

uniform vec3 entityPos;
uniform int lightCount;

#variable getLight

void main()
{
	vec3 position = gl_Vertex.xyz + entityPos;
	gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	gl_TexCoord[1] = gl_TextureMatrix[1] * gl_MultiTexCoord1;
	gl_Position = ftransform();
	gl_FrontColor = gl_Color;
	dist2Obj = length(gl_Position);
	lcolor = vec4(0, 0, 0, 1.0f);

	float sumR = 0.0f;
	float sumG = 0.0f;
	float sumB = 0.0f;
	float count = 0.0f;
	float maxIntens = 0.0f;
	float totalIntens = 0.0f;
	for(int i = 0; i < lightCount; i++)
	{
        Light l = getLight(i);
		float radius = pow(l.radius, 2);
		float intensity = pow(max(0.0f, 1.0f - distance(l.position, position) / l.radius), 2);
		totalIntens += intensity;
		maxIntens = max(maxIntens, intensity);
	}
	for(int i = 0; i < lightCount; i++)
	{
        Light l = getLight(i);
		float radius = pow(l.radius, 2);
		float intensity = pow(max(0.0f, 1.0f - distance(l.position, position) / l.radius), 2);
		sumR += l.color.r * (intensity / totalIntens);
		sumG += l.color.g * (intensity / totalIntens);
		sumB += l.color.b * (intensity / totalIntens);
	}
	lcolor = vec4(max(sumR * 1.5f, 0.0f), max(sumG * 1.5f, 0.0f), max(sumB * 1.5f, 0.0f), 1.0f);
	intens = min(1.0f, maxIntens);
}