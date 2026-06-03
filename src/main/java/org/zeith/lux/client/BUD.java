package org.zeith.lux.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.zeith.lux.proxy.ClientProxy;

public class BUD
		implements IWorldEventListener
{
	public final World world;
	
	public BUD(World world)
	{
		this.world = world;
	}
	
	@Override
	public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags)
	{
		ClientProxy.scanSpot(worldIn, null, pos);
	}
	
	@Override
	public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
	{
		ClientProxy.enqueuedLightCheck = true;
//		this.markBlocksForUpdate(x1 - 1, y1 - 1, z1 - 1, x2 + 1, y2 + 1, z2 + 1);
	}
	
	private void markBlocksForUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
	{
		int minCx = MathHelper.intFloorDiv(minX, 16);
		int minCy = MathHelper.intFloorDiv(minY, 16);
		int minCz = MathHelper.intFloorDiv(minZ, 16);
		int maxCx = MathHelper.intFloorDiv(maxX, 16);
		int maxCy = MathHelper.intFloorDiv(maxY, 16);
		int maxCz = MathHelper.intFloorDiv(maxZ, 16);
	}
	
	@Override
	public void notifyLightSet(BlockPos pos)
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