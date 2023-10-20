package org.zeith.lux;

import com.zeitheron.hammercore.annotations.RegistryName;
import com.zeitheron.hammercore.api.lighting.ColoredLight;
import com.zeitheron.hammercore.api.lighting.impl.IGlowingBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//@SimplyRegister
public class BlockGlowingTest
		extends Block
		implements IGlowingBlock
{
	@RegistryName("test")
	public static final BlockGlowingTest TEST_GLOWING = new BlockGlowingTest();
	
	public BlockGlowingTest()
	{
		super(Material.ROCK);
	}
	
	@Override
	public ColoredLight produceColoredLight(World world, BlockPos pos, IBlockState state, float partialTicks)
	{
		return ColoredLight.builder()
				.pos(pos)
				.color(1F, 0F, 1F)
				.radius(5)
				.build();
	}
}