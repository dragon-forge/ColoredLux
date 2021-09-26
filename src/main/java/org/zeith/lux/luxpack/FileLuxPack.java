package org.zeith.lux.luxpack;

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
		File target = new File(location, path.replace('/', File.separatorChar));
		if(target.isFile()) return new FileInputStream(target);
		return null;
	}
	
	@Override
	public boolean doesFileExist(String path)
	{
		File target = new File(location, path.replace('/', File.separatorChar));
		return target.isFile();
	}
	
	@Override
	public void close() throws IOException
	{
	}
}
