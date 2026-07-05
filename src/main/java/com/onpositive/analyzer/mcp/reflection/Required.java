package com.onpositive.analyzer.mcp.reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Required {
    String value();
    String description() default "";
    long minimum() default Long.MIN_VALUE;
    String[] enumValues() default {};
}
