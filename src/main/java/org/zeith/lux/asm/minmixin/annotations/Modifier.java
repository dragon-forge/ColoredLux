package org.zeith.lux.asm.minmixin.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Modifier
{
	String[] value();
}