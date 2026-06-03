package com.onpositive.analyzer.printing;

import org.netbeans.lib.profiler.heap.Instance;

import java.util.List;

public class InstanceListPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (object instanceof List<?> list) {
            if (list.isEmpty()) {
                return "No instances found";
            }

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Object item : list) {
                if (item instanceof Instance inst) {
                    try {
                        long instanceId = inst.getInstanceId();
                        String className = inst.getJavaClass().getName();
                        long retainedSize = inst.getRetainedSize();
                        sb.append(String.format("ID: %d, Class: %s, Retained Size: %d\n",
                                instanceId, className, retainedSize));
                        count++;
                    } catch (Exception e) {
                        // Skip objects with invalid instance references
                    }
                }
            }
            if (count == 0) {
                return "No valid instances found";
            }
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }

        if (object instanceof Instance instance) {
            StringBuilder sb = new StringBuilder();
            try {
                sb.append(String.format("ID: %d, Class: %s, Retained Size: %d\n",
                        instance.getInstanceId(),
                        instance.getJavaClass().getName(),
                        instance.getRetainedSize()));
            } catch (Exception e) {
                return "Invalid instance";
            }
            return sb.toString();
        }

        return object != null ? object.toString() : "";
    }
}
