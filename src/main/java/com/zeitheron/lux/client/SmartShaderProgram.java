package com.zeitheron.lux.client;

import com.zeitheron.hammercore.client.render.shader.GlShaderStack;
import com.zeitheron.hammercore.client.utils.RenderUtil;
import com.zeitheron.hammercore.lib.zlib.io.IOUtils;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SmartShaderProgram
{
	private static SmartShaderProgram currentShader = null;
	private static int currentProgram = -1;

	protected final SmartShaderVariables vars;
	protected int program;

	IntSupplier programGenerator;

	public SmartShaderProgram(ResourceLocation shader, IResourceManager resourceManager, SmartShaderVariables vars)
	{
		this.vars = vars;
		programGenerator = () -> loadProgram(String.format("%s:shaders/%s.vsh", shader.getNamespace(), shader.getPath()), String.format("%s:shaders/%s.fsh", shader.getNamespace(), shader.getPath()), resourceManager, vars);
		program = programGenerator.getAsInt();
	}

	public void reload()
	{
		RenderUtil.glTask(() ->
		{
			GL20.glDeleteProgram(program);
			program = programGenerator.getAsInt();
		});
	}

	protected void stop()
	{
	}

	public static SmartShaderProgram getCurrentShader()
	{
		return currentShader;
	}

	public static void stopShader()
	{
		if(currentProgram != 0)
		{
			if(currentShader != null)
				currentShader.stop();
			GL20.glUseProgram(0);
			currentProgram = 0;
			currentShader = null;
		}
	}

	public static boolean isCurrentShader(SmartShaderProgram shader)
	{
		return shader != null && GlShaderStack.glsActiveProgram() == shader.program;
	}

	public void useShader()
	{
		if(!isCurrentShader(this))
		{
			if(vars.hasChanged())
			{
				GL20.glDeleteProgram(program);
				program = programGenerator.getAsInt();
			}

			GL20.glUseProgram(program);
			currentProgram = program;
			currentShader = this;
		}
	}

	final Object2IntArrayMap<String> uniformCache = new Object2IntArrayMap<>();

	public int getUniformLocation(String location)
	{
		if(!uniformCache.containsKey(location))
			uniformCache.put(location, GL20.glGetUniformLocation(program, location));
		return uniformCache.getInt(location);
	}

	public void setUniform(String uniform, int value)
	{
		if(isCurrentShader(this))
			GL20.glUniform1i(getUniformLocation(uniform), value);
	}

	public void setUniform(String uniform, boolean value)
	{
		if(isCurrentShader(this))
			GL20.glUniform1i(getUniformLocation(uniform), value ? 1 : 0);
	}

	public void setUniform(String uniform, float value)
	{
		if(isCurrentShader(this))
			GL20.glUniform1f(getUniformLocation(uniform), value);
	}

	public void setUniform(String uniform, int v1, int v2)
	{
		if(isCurrentShader(this))
			GL20.glUniform2i(getUniformLocation(uniform), v1, v2);
	}

	public void setUniform(String uniform, int v1, int v2, int v3)
	{
		if(isCurrentShader(this))
			GL20.glUniform3i(getUniformLocation(uniform), v1, v2, v3);
	}

	public void setUniform(String uniform, float v1, float v2)
	{
		if(isCurrentShader(this))
			GL20.glUniform2f(getUniformLocation(uniform), v1, v2);
	}

	public void setUniform(String uniform, float v1, float v2, float v3)
	{
		if(isCurrentShader(this))
			GL20.glUniform3f(getUniformLocation(uniform), v1, v2, v3);
	}

	public void setUniform(String uniform, float v1, float v2, float v3, float v4)
	{
		if(isCurrentShader(this))
			GL20.glUniform4f(getUniformLocation(uniform), v1, v2, v3, v4);
	}

	public static int loadProgram(String vsh, String fsh, IResourceManager manager, SmartShaderVariables vars)
	{
		int vertexShader = createShader(vsh, GL20.GL_VERTEX_SHADER, manager, vars);
		int fragmentShader = createShader(fsh, GL20.GL_FRAGMENT_SHADER, manager, vars);
		int program = GL20.glCreateProgram();
		GL20.glAttachShader(program, vertexShader);
		GL20.glAttachShader(program, fragmentShader);
		GL20.glLinkProgram(program);
		String s = GL20.glGetProgramInfoLog(program, 32768);
		if(!s.isEmpty()) System.out.println("GL LOG: " + s);
		GL20.glDeleteShader(vertexShader);
		GL20.glDeleteShader(fragmentShader);
		return program;
	}

	public static int createShader(String filename, int shaderType, IResourceManager manager, SmartShaderVariables vars)
	{
		int shader = GL20.glCreateShader(shaderType);
		if(shader == 0)
			return 0;
		try(BufferedInputStream bis = new BufferedInputStream(manager.getResource(new ResourceLocation(filename)).getInputStream()))
		{
			byte[] abyte = vars.handle(IOUtils.pipeOut(bis));
			ByteBuffer buffer = BufferUtils.createByteBuffer(abyte.length);
			buffer.put(abyte);
			buffer.position(0);
			OpenGlHelper.glShaderSource(shader, buffer);
		} catch(Exception e)
		{
			e.printStackTrace();
		}
		GL20.glCompileShader(shader);
		if(GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
			throw new RuntimeException("Error creating shader \"" + filename + "\": " + getLogInfo(shader));
		return shader;
	}

	public static String getLogInfo(int obj)
	{
		return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
	}

	public static String readFileAsString(String filename, IResourceManager manager) throws Exception
	{
		System.out.println("Loading shader [" + filename + "]...");
		InputStream in = null;
		try
		{
			IResource resource = manager.getResource(new ResourceLocation(filename));
			in = resource.getInputStream();
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}

		String s = "";

		if(in != null)
		{
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8")))
			{
				s = reader.lines().collect(Collectors.joining("\n"));
			}
		}
		return s;
	}

	public static class SmartVariable
	{
		public String name, value;
		public Supplier<String> getter;

		public SmartVariable(String name, Supplier<String> get)
		{
			this.name = name;
			this.getter = get;
		}

		public boolean hasChanged()
		{
			String v = getter.get();
			if(!Objects.equals(v, value))
			{
				this.value = v;
				return true;
			}
			return false;
		}

		public String getValue()
		{
			if(value == null)
				value = getter.get();
			return value;
		}
	}

	public static class SmartShaderVariables
	{
		final List<SmartVariable> vars = new ArrayList<>();

		public SmartShaderVariables(SmartVariable... variables)
		{
			vars.addAll(Arrays.asList(variables));
		}

		boolean changed = false;

		public boolean hasChanged()
		{
			for(SmartVariable v : vars)
				if(v.hasChanged())
					changed = true;
			return changed;
		}

		public byte[] handle(byte[] abyte)
		{
			String str = new String(abyte);
			for(SmartVariable v : vars)
				str = str.replaceAll("%" + v.name + "%", v.getValue());
			changed = false;
			return str.getBytes();
		}
	}
}