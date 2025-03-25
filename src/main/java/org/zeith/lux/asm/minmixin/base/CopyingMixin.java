package org.zeith.lux.asm.minmixin.base;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.zeith.lux.asm.minmixin.IMixin;
import org.zeith.lux.asm.minmixin.TransformerSystem.ObjectWebUtils;
import org.zeith.lux.asm.minmixin.annotations.Copy;

import java.util.*;

public abstract class CopyingMixin
		implements IMixin
{
	protected static final String COPY_DESC = String.format("L%s;", Copy.class.getCanonicalName().replace('.', '/'));
	protected String innerPrefix = "org/zeith/lux/asm/mixins/inner/";
	
	protected String innerName;
	protected HashSet<MethodNode> copyMethods;
	
	
	public CopyingMixin()
	{
	}
	
	@Override
	public void apply(final ClassNode node, boolean obfuscatedEnv)
	{
		if(node.name.startsWith(innerPrefix))
		{
			innerName = node.name;
			
			ClassNode node1 = ObjectWebUtils.loadClass(ObjectWebUtils.writeClassToByteArray(node));
			for(MethodNode method : node1.methods)
			{
				if(method.invisibleAnnotations == null || method.invisibleAnnotations.stream().noneMatch(an -> an.desc.equals(COPY_DESC))) continue;
				
				if(copyMethods == null) copyMethods = new HashSet<>();
				copyMethods.add(method);
			}
			
			return;
		}
		
		initInner();
		
		if(innerName == null)
		{
			LOG.warn("Unable to locate inner class to merge into {}", node.name);
			return;
		}
		
		if(copyMethods != null)
			for(MethodNode method : copyMethods)
			{
				method.localVariables.replaceAll(n ->
				{
					if(n.name.equals("this"))
					{
						n.desc = String.format("L%s;", node.name);
					}
					return n;
				});
				
				ListIterator<AbstractInsnNode> it = method.instructions.iterator();
				while(it.hasNext())
				{
					AbstractInsnNode in = it.next();
					if(in instanceof MethodInsnNode && ((MethodInsnNode) in).owner.equals(innerName))
					{
						MethodInsnNode min = (MethodInsnNode) in;
						if(in.getOpcode() == Opcodes.INVOKESTATIC && copyMethods.stream().noneMatch(mn -> mn.name.equals(min.name) && mn.desc.equals(min.desc)))
							continue; // method is not copied, thus probably static and does not need to be remapped
						min.owner = node.name; // replace owner name from inner to current name
					}
				}
				
				node.methods.add(method);
			}
	}
	
	protected abstract void initInner();
}