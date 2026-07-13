#version 330 compatibility

out vec3 lcolor;
out float intens;
out float dist2Obj;

struct Light
{
	vec4 color;
	vec3 position;
	float radius;
};

uniform int chunkX;
uniform int chunkY;
uniform int chunkZ;
uniform int lightCount;

#variable getLight
#variable computeColor

void main()
{
	vec4 pos = gl_ModelViewProjectionMatrix * gl_Vertex;
	vec3 position = gl_Vertex.xyz + vec3(chunkX, chunkY, chunkZ);
	gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	gl_TexCoord[1] = gl_TextureMatrix[1] * gl_MultiTexCoord1;
	gl_Position = ftransform();
	gl_FrontColor = gl_Color;
    dist2Obj = length(gl_Position.xyz);
    lcolor = vec3(0);

    computeColor(position);
}