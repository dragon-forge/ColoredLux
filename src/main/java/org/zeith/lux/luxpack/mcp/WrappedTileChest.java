package org.zeith.lux.luxpack.mcp;

import net.minecraft.tileentity.TileEntityChest;
import org.zeith.lux.luxpack.WrappedTile;

public class WrappedTileChest
		extends WrappedTile<TileEntityChest>
{
	public float lidAngle;
	public float prevLidAngle;
	public int numPlayersUsing;

	public WrappedTileChest(TileEntityChest tile)
	{
		super(tile);
	}

	@Override
	public void updateWrapper()
	{
		lidAngle = tile.lidAngle;
		prevLidAngle = tile.prevLidAngle;
		numPlayersUsing = tile.numPlayersUsing;
	}
}