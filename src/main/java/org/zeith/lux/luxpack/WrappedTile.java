package org.zeith.lux.luxpack;

import com.zeitheron.hammercore.utils.base.Cast;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.GenericEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.zeith.lux.luxpack.mcp.WrappedTileBeacon;
import org.zeith.lux.luxpack.mcp.WrappedTileChest;
import org.zeith.lux.luxpack.mcp.WrappedTileEChest;

/**
 * Useful for v2 lux packs access to tile's code.
 * Wrappers are used to avoid stupid obfuscation.
 */
@SideOnly(Side.CLIENT)
public abstract class WrappedTile<T extends TileEntity>
{
	public final T tile;

	public WrappedTile(T tile)
	{
		this.tile = tile;
		updateWrapper();
	}

	public abstract void updateWrapper();

	public static Object wrapTile(TileEntity tile)
	{
		if(tile instanceof TileEntityBeacon)
		{
			return new WrappedTileBeacon((TileEntityBeacon) tile);
		} else if(tile instanceof TileEntityEnderChest)
		{
			return new WrappedTileEChest((TileEntityEnderChest) tile);
		} else if(tile instanceof TileEntityChest)
		{
			return new WrappedTileChest((TileEntityChest) tile);
		} else
		{
			WrapTileEntityEvent<?> e = new WrapTileEntityEvent<>(tile);
			MinecraftForge.EVENT_BUS.post(e);
			if(e.isWrapped()) return e.getWrap();
		}
		return tile;
	}

	public static class WrapTileEntityEvent<T extends TileEntity>
			extends GenericEvent<T>
	{
		public final TileEntity tile;
		private WrappedTile<T> wrap;

		public WrapTileEntityEvent(T tile)
		{
			super(Cast.cast(tile.getClass()));
			this.tile = tile;
		}

		public void wrap(WrappedTile<T> wrap)
		{
			this.wrap = wrap;
		}

		public boolean isWrapped()
		{
			return wrap != null;
		}

		public WrappedTile<T> getWrap()
		{
			return wrap;
		}
	}
}