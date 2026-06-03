package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.HeapDumpService;
import com.onpositive.analyzer.HeapDumpService.ClassStats;

import java.util.List;

public class ClassStatsListPrinter implements IValuePrinter {

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
            if (item instanceof ClassStats cs) {
                sb.append(cs.className)
                  .append(" (Count: ")
                  .append(cs.instanceCount)
                  .append(", Size: ")
                  .append(cs.size)
                  .append(")\n");
            }
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }
}
