package com.onpositive.analyzer.mcp.reflection;

import com.onpositive.analyzer.printing.IValuePrinter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify which class to use for printing returned object into a human-readable format
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Printer {

    Class<? extends IValuePrinter> impl();

}
