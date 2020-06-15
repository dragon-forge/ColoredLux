package com.zeitheron.lux.luxpack;

import java.io.*;

public class FileLuxPack
		extends AbstractLuxPack
{
	public FileLuxPack(File location) throws IOException
	{
		super(location);
		if(!location.isDirectory())
			throw new UnsupportedEncodingException(location.getName() + " is not a folder lux pack");
	}

	@Override
	public InputStream createInput(String path) throws IOException
	{
		File target = new File(location, path.replaceAll("/", File.separator));
		if(target.isFile()) return new FileInputStream(target);
		return null;
	}

	@Override
	public void close() throws IOException
	{
	}
}
