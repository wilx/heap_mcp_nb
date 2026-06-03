package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.JavaClassPrinter;
import org.netbeans.lib.profiler.heap.JavaClass;

public class JavaClassPrinterWrapper implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof JavaClass javaClass)) {
            return object != null ? object.toString() : "";
        }

        JavaClassPrinter.ClassDetails details = JavaClassPrinter.getClassDetails(javaClass);
        return JavaClassPrinter.printClassDetails(details);
    }
}
