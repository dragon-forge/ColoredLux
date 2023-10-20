package org.zeith.lux.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import org.zeith.lux.api.LuxManager;
import org.zeith.lux.api.light.ILightBlockHandler;
import org.zeith.lux.proxy.ClientProxy;

import java.lang.ref.WeakReference;

public class BUD
		implements IWorldEventListener
{
	@Override
	public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags)
	{
		ILightBlockHandler handler = LuxManager.BLOCK_LUMINANCES.get(newState.getBlock());
		if(handler != null)
		{
			BlockPos ipos = pos.toImmutable();
			newState = newState.getBlock().getExtendedState(newState, worldIn, pos);
			ClientProxy.EXISTING.put(ipos, new ILightBlockHandler.LightBlockWrapper(new WeakReference<>(worldIn), ipos, newState, handler));
		} else
			ClientProxy.EXISTING.remove(pos);
	}

	@Override
	public void notifyLightSet(BlockPos pos)
	{
	}

	@Override
	public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
	{
	}

	@Override
	public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x, double y, double z, float volume, float pitch)
	{
	}

	@Override
	public void playRecord(SoundEvent soundIn, BlockPos pos)
	{
	}

	@Override
	public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
	{
	}

	@Override
	public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters)
	{
	}

	@Override
	public void onEntityAdded(Entity entityIn)
	{
	}

	@Override
	public void onEntityRemoved(Entity entityIn)
	{
	}

	@Override
	public void broadcastSound(int soundID, BlockPos pos, int data)
	{
	}

	@Override
	public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data)
	{
	}

	@Override
	public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress)
	{
	}
}