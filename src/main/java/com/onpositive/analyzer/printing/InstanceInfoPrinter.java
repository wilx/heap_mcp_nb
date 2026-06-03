package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.HeapDumpService.FieldInfo;
import com.onpositive.analyzer.HeapDumpService.InstanceInfo;

public class InstanceInfoPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof InstanceInfo instance)) {
            return object != null ? object.toString() : "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Instance ID: %d%n", instance.instanceId));
        sb.append(String.format("Class: %s%n", instance.className));
        sb.append(String.format("Size: %d%n", instance.size));
        sb.append(String.format("Retained Size: %d%n", instance.retainedSize));
        sb.append("Field Values:\n");

        if (instance.fields != null) {
            for (FieldInfo field : instance.fields) {
                if (field.objectInstanceId != null) {
                    sb.append(String.format("  %s: %s (Instance ID: %d)%n",
                            field.name, field.value, field.objectInstanceId));
                } else {
                    sb.append(String.format("  %s: %s%n", field.name, field.value));
                }
            }
        }

        return sb.toString();
    }
}
