package com.onpositive.analyzer.printing;

import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;

public class JavaClassListPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof List<?> list)) {
            return object != null ? object.toString() : "";
        }

        if (list.isEmpty()) {
            return "No classes found";
        }

        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (item instanceof JavaClass cls) {
                sb.append(cls.getName())
                  .append(" (Instances: ")
                  .append(cls.getInstancesCount())
                  .append(")\n");
            }
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }
}
