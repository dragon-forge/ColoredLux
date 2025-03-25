package org.zeith.lux.asm.minmixin.base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.zeith.lux.asm.minmixin.IMixin;
import org.zeith.lux.asm.minmixin.annotations.*;

import java.lang.reflect.Method;

import static org.zeith.lux.asm.minmixin.IMixin.*;

public class AccessorMixin
		implements IMixin
{
	protected final Class<?> accessor;
	
	public AccessorMixin(Class<?> accessor)
	{
		if(!accessor.isInterface()) throw new UnsupportedOperationException("Accessor must be an interface.");
		this.accessor = accessor;
	}
	
	@Override
	public void apply(ClassNode node, boolean obfuscatedEnv)
	{
		node.interfaces.add(accessor.getCanonicalName().replace('.', '/'));
		
		for(Method method : accessor.getDeclaredMethods())
		{
			if(method.isDefault() || java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
			
			Accessor ac = method.getDeclaredAnnotation(Accessor.class);
			Modifier md = method.getDeclaredAnnotation(Modifier.class);
			
			if(ac != null && md != null) throw new IllegalStateException("Method " + method + " has both @Accessor and @Modifier which is not allowed.");
			if(ac == null && md == null) throw new IllegalStateException("Method " + method + " has neither @Accessor nor @Modifier!");
			
			if(ac != null)
			{
				Class<?> rv = method.getReturnType();
				if(rv == void.class) throw new IllegalStateException(new NoSuchMethodException("Method " + method + " has void return type. This should not be the case for accessors."));
				
				FieldNode f = findField(node, ac.value());
				
				String retDesc = getTypeDescriptor(rv);
				if(!f.desc.equals(retDesc)) throw new IllegalStateException(new NoSuchMethodException("Method " + method + " return type does not match with " + f.desc));
				
				MethodNode m = createMethod(method);
				InsnList insn = new InsnList();
				insn.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this.
				insn.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, f.name, f.desc)); // f.name
				insn.add(new InsnNode(retOpc(rv)));
				m.instructions = insn;
				node.methods.add(m);
				
				continue;
			}
			
			if(md != null)
			{
				if(method.getParameterCount() != 1) throw new IllegalStateException("Method " + method + " has @Modifier, but the parameter count is not 1!");
				
				Class<?> rv = method.getReturnType();
				Class<?> par = method.getParameterTypes()[0];
				FieldNode f = findField(node, md.value());
				
				String retDesc = getTypeDescriptor(rv);
				String parDesc = getTypeDescriptor(par);
				boolean doVarStuff = rv != void.class;
				if(doVarStuff && !f.desc.equals(retDesc)) throw new IllegalStateException(new NoSuchMethodException("Method " + method + " return type is neither void nor " + f.desc));
				
				MethodNode m = createMethod(method);
				
				InsnList insn = new InsnList();
				if(doVarStuff)
				{
					insn.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this.
					insn.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, f.name, f.desc)); // get this[f.name]
					insn.add(new VarInsnNode(storeOpc(par), 2)); // var2 = this[f.name]
				}
				insn.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this.
				insn.add(new VarInsnNode(loadOpc(par), 1)); // newVal
				insn.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, f.name, f.desc)); // set this[f.name]
				if(doVarStuff)
					insn.add(new VarInsnNode(loadOpc(par), 2)); // load var2
				insn.add(new InsnNode(retOpc(rv))); // return var1
				m.instructions = insn;
				
				node.methods.add(m);
			}
		}
	}
}