package org.zeith.lux.api.light;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.zeith.lux.api.LuxManager;
import org.zeith.lux.api.event.GatherLightsEvent;

public interface ILightBlockHandler
{
	void createLights(World world, BlockPos pos, IBlockState state, GatherLightsEvent e);
	
	default void update(IBlockState state, BlockPos pos)
	{
	}

	class LightBlockWrapper
	{
		public final World world;
		public final BlockPos pos;

		public LightBlockWrapper(World world, BlockPos pos, IBlockState state, ILightBlockHandler handler)
		{
			this.world = world;
			this.pos = pos;
			this.prevState = state;
			this.handler = handler;
			this.handler.update(state, pos);
		}

		IBlockState prevState;
		ILightBlockHandler handler;

		public void addLights(GatherLightsEvent e)
		{
			IBlockState state = world.getBlockState(pos);
			if(prevState != state)
			{
				prevState = state;
				handler = LuxManager.BLOCK_LUMINANCES.get(state.getBlock());
			}
			if(handler != null)
				handler.createLights(world, pos, prevState, e);
		}
	}
}