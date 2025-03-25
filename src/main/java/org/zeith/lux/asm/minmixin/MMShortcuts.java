package org.zeith.lux.asm.minmixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class MMShortcuts
{
	public static InsnList returnFalseIfArgIsNull(int argLocal)
	{
		LabelNode l1 = new LabelNode();
		
		InsnList insn = new InsnList();
		insn.add(new VarInsnNode(Opcodes.ALOAD, argLocal));
		insn.add(new JumpInsnNode(Opcodes.IFNONNULL, l1));
		insn.add(new InsnNode(Opcodes.ICONST_0));
		insn.add(new InsnNode(Opcodes.IRETURN));
		insn.add(l1);
		
		return insn;
	}
	
	public static InsnList returnVoidIfArgIsNull(int argLocal)
	{
		LabelNode l1 = new LabelNode();
		
		InsnList insn = new InsnList();
		insn.add(new VarInsnNode(Opcodes.ALOAD, argLocal));
		insn.add(new JumpInsnNode(Opcodes.IFNONNULL, l1));
		insn.add(new InsnNode(Opcodes.RETURN));
		insn.add(l1);
		
		return insn;
	}
}