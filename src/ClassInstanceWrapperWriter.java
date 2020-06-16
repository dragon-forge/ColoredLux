import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class ClassInstanceWrapperWriter
{
	public static void main(String[] args)
	{
		System.out.println(generate(World.class, "Test"));
	}
	
	public static String generate(Class<?> type, String className)
	{
		StringBuilder builder = new StringBuilder();
		
//		Set<String> imports = new HashSet<>();
		Set<String> fields = new HashSet<>();
		Set<String> methods = new HashSet<>();
		Set<String> ctorInitializers = new HashSet<>();
		
		Set<String> ignore = new HashSet<>();
		ignore.add("wait");
		ignore.add("notify");
		ignore.add("notifyAll");
		ignore.add("getClass");
		
		for(Field f : type.getFields())
		{
			if(Modifier.isPublic(f.getModifiers()))
			{
				StringBuilder fb = new StringBuilder();
				fb.append("public ");
				if(Modifier.isStatic(f.getModifiers())) fb.append("static ");
				if(Modifier.isFinal(f.getModifiers())) fb.append("final ");
				fb.append(f.getGenericType().getTypeName());
				fb.append(" ").append(f.getName());
				if(Modifier.isStatic(f.getModifiers()))
					fb.append(" = ").append(type.getSimpleName()).append(".").append(f.getName());
				fb.append(";");
				fields.add(fb.toString());
				if(!Modifier.isStatic(f.getModifiers()))
					ctorInitializers.add(f.getName() + " = instance." + f.getName() + ";");
			}
		}
		
		for(Method m : type.getMethods())
		{
			if(Modifier.isPublic(m.getModifiers()) && !ignore.contains(m.getName()))
			{
				StringBuilder mb = new StringBuilder();
				mb.append("public ");
				if(Modifier.isStatic(m.getModifiers())) mb.append("static ");
				if(Modifier.isFinal(m.getModifiers())) mb.append("final ");
				mb.append(m.getGenericReturnType().getTypeName()).append(" ").append(m.getName());
				mb.append("(");
				
				Type[] params = m.getGenericParameterTypes();
				for(int i = 0; i < params.length; ++i)
					mb.append(params[i].getTypeName()).append(" par").append(i).append(i < params.length - 1 ? ", " : "");
				
				mb.append(")\n");
				mb.append("\t{\n\t\t");
				
				if(!void.class.isAssignableFrom(m.getReturnType()))
				{
					mb.append("return ");
				}
				
				if(Modifier.isStatic(m.getModifiers()))
				{
					mb.append(type.getSimpleName()).append(".").append(m.getName()).append("(");
					for(int i = 0; i < params.length; ++i)
						mb.append("par").append(i).append(i < params.length - 1 ? ", " : "");
					mb.append(");\n");
				} else
				{
					mb.append("instance.").append(m.getName()).append("(");
					for(int i = 0; i < params.length; ++i)
						mb.append("par").append(i).append(i < params.length - 1 ? ", " : "");
					mb.append(");\n");
				}
				mb.append("\t}");
				
				methods.add(mb.toString());
			}
		}
		
//		for(String imp : imports)
//			builder.append("import ").append(imp).append(";\n");
		
		builder
				.append("\npublic class ")
				.append(className)
				.append("\n{\n\tpublic final ")
				.append(type.getSimpleName())
				.append(" instance;\n");
		
		for(String var : fields) builder.append("\t").append(var).append("\n");
		
		builder.append("\n\tpublic ")
				.append(className)
				.append("(")
				.append(type.getSimpleName())
				.append(" instance)\n\t{\n\t\tthis.instance = instance;\n");
		for(String ctor : ctorInitializers) builder.append("\t\t").append(ctor).append("\n");
		builder.append("\t}\n");
		
		for(String method : methods)
			builder.append("\n\t").append(method).append("\n");
		
		return builder.append("}").toString();
	}
}