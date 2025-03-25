package org.zeith.lux.asm.minmixin;

import org.apache.logging.log4j.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.*;

public interface IMixin
{
	Logger LOG = LogManager.getLogger("ThaumicAdditionsCore");
	
	void apply(ClassNode node, boolean obfuscatedEnv);
	
	static Predicate<AbstractInsnNode> instanceofNode(String type)
	{
		return i -> i instanceof TypeInsnNode
				&& i.getOpcode() == Opcodes.INSTANCEOF
				&& ((TypeInsnNode) i).desc.equals(type);
	}
	
	static Predicate<AbstractInsnNode> ldcNode(Object ldcValue)
	{
		return i -> i instanceof LdcInsnNode
				&& Objects.equals(((LdcInsnNode) i).cst, ldcValue);
	}
	
	static void findInsnNode(InsnList insn, Predicate<AbstractInsnNode> filter, Consumer<AbstractInsnNode> handler)
	{
		AbstractInsnNode i = insn.getFirst();
		while(i != null)
		{
			if(filter.test(i)) handler.accept(i);
			i = i.getNext();
		}
	}
	
	static Optional<AbstractInsnNode> findFirstInsnNode(InsnList insn, Predicate<AbstractInsnNode> filter)
	{
		AbstractInsnNode i = insn.getFirst();
		while(i != null)
		{
			if(filter.test(i)) return Optional.of(i);
			i = i.getNext();
		}
		return Optional.empty();
	}
	
	static void findMethod(ClassNode node, String name, String desc, Consumer<MethodNode> handler)
	{
		if(desc == null)
		{
			for(MethodNode method : node.methods)
				if(method.name.equals(name))
					handler.accept(method);
		} else
		{
			for(MethodNode method : node.methods)
				if(method.name.equals(name) && method.desc.equals(desc))
				{
					handler.accept(method);
					return;
				}
		}
	}
	
	static MethodNode createMethod(Method method)
	{
		MethodNode m = new MethodNode();
		m.exceptions = Collections.emptyList();
		m.desc = getMethodDescriptor(method);
		m.name = method.getName();
		m.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
		return m;
	}
	
	static int retOpc(Class<?> rv)
	{
		return !rv.isPrimitive()
			   ? Opcodes.ARETURN
			   : rv == long.class
				 ? Opcodes.LRETURN
				 : rv == float.class
				   ? Opcodes.FRETURN
				   : rv == double.class
					 ? Opcodes.DRETURN
					 : rv == void.class
					   ? Opcodes.RETURN
					   : Opcodes.IRETURN;
	}
	
	static int storeOpc(Class<?> rv)
	{
		return !rv.isPrimitive()
			   ? Opcodes.ASTORE
			   : rv == long.class
				 ? Opcodes.LSTORE
				 : rv == float.class
				   ? Opcodes.FSTORE
				   : rv == double.class
					 ? Opcodes.DSTORE
					 : Opcodes.ISTORE;
	}
	
	static int loadOpc(Class<?> rv)
	{
		return !rv.isPrimitive()
			   ? Opcodes.ALOAD
			   : rv == long.class
				 ? Opcodes.LLOAD
				 : rv == float.class
				   ? Opcodes.FLOAD
				   : rv == double.class
					 ? Opcodes.DLOAD
					 : Opcodes.ILOAD;
	}
	
	@Nonnull
	static FieldNode findField(ClassNode node, String[] names)
	{
		Set<String> n = new HashSet<>(Arrays.asList(names));
		for(FieldNode field : node.fields)
		{
			if(n.contains(field.name))
				return field;
		}
		throw new IllegalStateException(new NoSuchFieldException("No field with names " + n + " can be found in " + node.name));
	}
	
	static String getMethodDescriptor(Method method)
	{
		return getMethodDescriptor(method.getReturnType(), method.getParameterTypes());
	}
	
	static String getMethodDescriptor(Class<?> returnType, Class<?>... params)
	{
		StringBuilder descriptor = new StringBuilder();
		descriptor.append('(');
		
		for(Class<?> paramType : params)
			descriptor.append(getTypeDescriptor(paramType));
		
		descriptor.append(')');
		descriptor.append(getTypeDescriptor(returnType));
		
		return descriptor.toString();
	}
	
	static String getTypeDescriptor(Class<?> type)
	{
		if(type.isArray()) return '[' + getTypeDescriptor(type.getComponentType());
		if(type.isPrimitive())
		{
			if(type == void.class) return "V";
			if(type == int.class) return "I";
			if(type == boolean.class) return "Z";
			if(type == byte.class) return "B";
			if(type == char.class) return "C";
			if(type == short.class) return "S";
			if(type == long.class) return "J";
			if(type == float.class) return "F";
			if(type == double.class) return "D";
			throw new IllegalArgumentException("Unknown primitive type: " + type);
		}
		return 'L' + type.getName().replace('.', '/') + ';';
	}
}