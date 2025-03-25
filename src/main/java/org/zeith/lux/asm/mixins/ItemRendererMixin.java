package org.zeith.lux.asm.mixins;

import com.zeitheron.hammercore.api.lighting.ColoredLightManager;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.opengl.GL20;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.zeith.lux.asm.minmixin.IMixin;
import org.zeith.lux.asm.minmixin.annotations.*;

@Debug
@MinMixin("net.minecraft.client.renderer.ItemRenderer")
public class ItemRendererMixin
		implements IMixin
{
	@Override
	public void apply(ClassNode node, boolean obfuscatedEnv)
	{
		IMixin.findMethod(node, obfuscatedEnv ? "b" : "renderOverlays", "(F)V", m ->
				renderOverlaysMixin(m)
		);
	}
	
	private void renderOverlaysMixin(MethodNode node)
	{
		node.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
				ItemRendererMixin.class.getCanonicalName().replace('.', '/'),
				"disableLux",
				"()V",
				false
		));
	}
	
	@SideOnly(Side.CLIENT)
	public static void disableLux()
	{
		if(ColoredLightManager.isColoredLightActive())
			GL20.glUseProgram(0);
	}
}