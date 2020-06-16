package com.zeitheron.lux.luxpack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipLuxPack
		extends AbstractLuxPack
{
	ZipFile zip;

	public ZipLuxPack(File location) throws IOException
	{
		super(location);

		if(location.isFile() && (location.getName().endsWith(".zip") || location.getName().endsWith(".lux")))
		{
			this.zip = new ZipFile(location);
		} else throw new UnsupportedEncodingException(location.getName() + " is not a zip-type lux pack");
	}

	@Override
	public InputStream createInput(String path) throws IOException
	{
		ZipEntry ze = zip.getEntry(path);
		if(ze == null || ze.isDirectory()) return null;
		return zip.getInputStream(ze);
	}
	
	@Override
	public boolean doesFileExist(String path)
	{
		ZipEntry ze = zip.getEntry(path);
		return ze != null && !ze.isDirectory();
	}
	
	@Override
	public void close() throws IOException
	{
		if(zip != null)
		{
			zip.close();
			zip = null;
		}
	}
}