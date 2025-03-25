package org.zeith.lux.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.zeith.lux.asm.minmixin.*;
import org.zeith.lux.asm.minmixin.base.AccessorMixin;
import org.zeith.lux.asm.mixins.ItemRendererMixin;

public class LuxTransformer
		implements IClassTransformer
{
	protected final TransformerSystem asm = new TransformerSystem();
	
	public LuxTransformer()
	{
		register(new ItemRendererMixin());
	}
	
	void register(IMixin handle)
	{
		asm.register(handle);
	}
	
	void register(IMixin handle, String... targets)
	{
		asm.register(handle, targets);
	}
	
	void registerAccessor(Class<?> accessor, String... targets)
	{
		asm.register(new AccessorMixin(accessor), targets);
	}
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		return asm.transform(name, transformedName, basicClass);
	}
}