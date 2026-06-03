package org.zeith.lux.client;

import com.zeitheron.hammercore.api.lighting.ColoredLight;
import com.zeitheron.hammercore.client.utils.gl.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.List;

public class LightSegment
{
	private GLBuffer lightUBO;
	
	public final int start, end, size;
	private final FloatBuffer uboData;
	private final IGLFloatBufferStream uboStream;
	
	public LightSegment(int start, int end)
	{
		this.start = start;
		this.end = end;
		this.size = end - start;
		uboData = BufferUtils.createFloatBuffer(size * ColoredLight.FLOAT_SIZE);
		uboStream = IGLFloatBufferStream.forBuffer(uboData);
	}
	
	FloatBuffer updateUBO()
	{
		List<ColoredLight> lights = ClientLightManager.lights;
		uboData.clear();
		int e = Math.min(lights.size(), end);
		for(int i = start; i < e; ++i) lights.get(i).writeFloats(uboStream);
		uboData.flip();
		return uboData;
	}
	
	public GLBuffer getUBO()
	{
		createUBO();
		return lightUBO;
	}
	
	void createUBO()
	{
		if(lightUBO != null)
			return;
		lightUBO = new GLBuffer();
		lightUBO.bufferData(updateUBO());
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
	}
	
	int refreshUBO()
	{
		createUBO();
		FloatBuffer fb = updateUBO();
		lightUBO.bufferData(fb);
		return fb.remaining();
	}
}