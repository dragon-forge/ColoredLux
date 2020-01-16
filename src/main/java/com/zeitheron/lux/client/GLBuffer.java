package com.zeitheron.lux.client;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static com.zeitheron.hammercore.client.utils.RenderUtil.glTask;
import static org.lwjgl.opengl.GL15.*;

public class GLBuffer
{
	public int buffer;

	public GLBuffer()
	{
		buffer = glTask(() -> glGenBuffers());
	}

	public void bindBuffer()
	{
		glBindBuffer(GL_ARRAY_BUFFER, buffer);
	}

	public void bufferData(FloatBuffer data)
	{
		glTask(() ->
		{
			bindBuffer();
			glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
		});
	}

	public void bufferData(DoubleBuffer data)
	{
		glTask(() ->
		{
			bindBuffer();
			glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
		});
	}
}