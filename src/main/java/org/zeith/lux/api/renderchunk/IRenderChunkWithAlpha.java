package org.zeith.lux.api.renderchunk;

/**
 * Allow mods override chunk alpha by mixing into {@link net.minecraft.client.renderer.chunk.RenderChunk} and implementing this condition
 */
public interface IRenderChunkWithAlpha
{
	float getRenderChunkAlpha();
	
	default void setRenderChunkAlpha(float alpha) {}
}