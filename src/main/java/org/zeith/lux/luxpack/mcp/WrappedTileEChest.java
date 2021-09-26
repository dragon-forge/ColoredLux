package org.zeith.lux.luxpack.mcp;

import net.minecraft.tileentity.TileEntityEnderChest;
import org.zeith.lux.luxpack.WrappedTile;

public class WrappedTileEChest
		extends WrappedTile<TileEntityEnderChest>
{
	public float lidAngle;
	public float prevLidAngle;
	public int numPlayersUsing;

	public WrappedTileEChest(TileEntityEnderChest tile)
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