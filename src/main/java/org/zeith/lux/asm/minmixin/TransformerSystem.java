package org.zeith.lux.asm.minmixin;

import org.apache.logging.log4j.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.zeith.lux.asm.minmixin.annotations.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class TransformerSystem
{
	static final Logger LOG = LogManager.getLogger("ColoredLuxCore");
	private final Map<String, List<IMixin>> hooks = new HashMap<>();
	private final ExecutorService SAVE_THREAD = Executors.newSingleThreadExecutor();
	private final File CLASS_SAVE_DIR = new File("asm", "minmixins");
	String currentClass, transformedCurrentClass;
	private String indentstr = "";
	private int indents = 0;
	
	void saveClass(byte[] original, byte[] modified, String clazz)
	{
		final byte[] o = original.clone();
		final byte[] m = modified.clone();
		
		SAVE_THREAD.submit(() ->
		{
			if(!CLASS_SAVE_DIR.isDirectory())
				CLASS_SAVE_DIR.mkdirs();
			try(ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(new File(CLASS_SAVE_DIR, clazz.replace("/", "_").replace(".", "_") + ".zip").toPath())))
			{
				zos.putNextEntry(new ZipEntry("origin.class"));
				zos.write(original);
				
				if(!Arrays.equals(modified, original))
				{
					zos.putNextEntry(new ZipEntry("modified.class"));
					zos.write(modified);
				}
				
				zos.putNextEntry(new ZipEntry("methods.txt"));
				{
					ClassNode cn = ObjectWebUtils.loadClass(m);
					for(MethodNode mn : cn.methods)
						zos.write((mn.name + " " + mn.desc + "\n").getBytes());
				}
			} catch(IOException e)
			{
				info("Failed to save ASM class for " + clazz);
				e.printStackTrace();
			}
		});
	}
	
	public void push()
	{
		if(indents < 15)
			++indents;
		indentstr = "";
		for(int i = 0; i < indents; ++i)
			indentstr += "  ";
	}
	
	public void pop()
	{
		if(indents > 0)
			--indents;
		indentstr = "";
		for(int i = 0; i < indents; ++i)
			indentstr += "  ";
	}
	
	public void info(String text, Object... format)
	{
		if(indents == 0)
			LOG.info(text, format);
		else
			LOG.info(indentstr + '-' + text, format);
	}
	
	public void register(IMixin hook)
	{
		MinMixin mixin = hook.getClass().getAnnotation(MinMixin.class);
		for(String target : mixin.value())
			hooks.computeIfAbsent(target, k -> new ArrayList<>())
					.add(hook);
	}
	
	public void register(IMixin hook, String... targets)
	{
		for(String target : targets)
			hooks.computeIfAbsent(target, k -> new ArrayList<>())
					.add(hook);
	}
	
	public String getCurrentClass()
	{
		return currentClass;
	}
	
	public String getTransformedCurrentClass()
	{
		return transformedCurrentClass;
	}
	
	private boolean obf;
	
	public byte[] transform(String name, String transformedName, byte[] data)
	{
		currentClass = name;
		transformedCurrentClass = transformedName;
		
		if(!obf)
			obf = !name.equals(transformedName);
		
		byte[] origin = data;
		boolean l = false;
		
		for(IMixin h : hooks.getOrDefault(transformedName, Collections.emptyList()))
		{
			if(!l)
			{
				l = true;
				info("Transforming " + transformedName + " (" + name + ")...");
			}
			
			ClassNode node = ObjectWebUtils.loadClass(data);
			
			currentClass = name;
			transformedCurrentClass = transformedName;
			
			push();
			info("Applying {} into {}", h.getClass().getName(), node.name);
			h.apply(node, obf);
			if(h.getClass().isAnnotationPresent(Debug.class))
			{
				info("Saving {}", node.name);
				saveClass(data, ObjectWebUtils.writeClassToByteArray(node), transformedName);
			}
			pop();
			
			data = ObjectWebUtils.writeClassToByteArray(node);
			
			pop();
		}
		
		currentClass = null;
		transformedCurrentClass = null;
		
		return data;
	}
	
	public static class ObjectWebUtils
	{
		public static ClassNode loadClass(byte[] data)
		{
			ClassReader reader = new ClassReader(data);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);
			return node;
		}
		
		public static ClassNode loadClass(InputStream stream)
				throws IOException
		{
			ClassReader reader = new ClassReader(stream);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);
			return node;
		}
		
		public static ClassNode loadClass(File file)
				throws IOException
		{
			FileInputStream stream = new FileInputStream(file);
			ClassNode node = loadClass(stream);
			stream.close();
			return node;
		}
		
		public static ClassNode loadClass(URL url)
				throws IOException
		{
			URLConnection conn = url.openConnection();
			try
			{
				conn.setDoInput(true);
				conn.connect();
			} catch(Throwable er)
			{
			}
			InputStream stream = conn.getInputStream();
			ClassNode node = loadClass(stream);
			stream.close();
			return node;
		}
		
		public static byte[] writeClassToByteArray(ClassNode node)
		{
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			return writer.toByteArray();
		}
		
		public static void writeClassToOutputStream(ClassNode node, OutputStream stream)
				throws IOException
		{
			stream.write(writeClassToByteArray(node));
		}
		
		public static void writeClassToFile(ClassNode node, File file)
				throws IOException
		{
			FileOutputStream os = new FileOutputStream(file);
			writeClassToOutputStream(node, os);
			os.close();
		}
		
		public static void writeClassToURL(ClassNode node, URL url)
				throws IOException
		{
			URLConnection conn = url.openConnection();
			try
			{
				conn.setDoOutput(true);
				conn.connect();
			} catch(Throwable er)
			{
			}
			OutputStream os = conn.getOutputStream();
			writeClassToOutputStream(node, os);
			os.close();
		}
	}
}