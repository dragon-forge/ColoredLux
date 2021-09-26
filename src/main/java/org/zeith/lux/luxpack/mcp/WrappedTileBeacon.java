package org.zeith.lux.luxpack.mcp;

import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.zeith.lux.luxpack.WrappedTile;

@SideOnly(Side.CLIENT)
public class WrappedTileBeacon
		extends WrappedTile<TileEntityBeacon>
{
	public int levels;
	public String customName;

	public WrappedTileBeacon(TileEntityBeacon tile)
	{
		super(tile);
	}

	@Override
	public void updateWrapper()
	{
		levels = tile.getLevels();
		customName = tile.getName();
	}
}