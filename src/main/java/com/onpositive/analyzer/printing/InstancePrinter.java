package com.onpositive.analyzer.printing;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

import java.util.List;

public class InstancePrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof Instance instance)) {
            return object != null ? object.toString() : "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Instance ID: %d%n", instance.getInstanceId()));
        sb.append(String.format("Class: %s%n", instance.getJavaClass().getName()));
        sb.append(String.format("Size: %d%n", instance.getSize()));
        sb.append(String.format("Retained Size: %d%n", instance.getRetainedSize()));

        List<?> fieldValues = instance.getFieldValues();
        if (fieldValues != null && !fieldValues.isEmpty()) {
            sb.append("Fields:%n");
            for (Object fvObj : fieldValues) {
                FieldValue fv = (FieldValue) fvObj;
                String fieldName = fv.getField().getName();
                String fieldType = fv.getField().getType().getName();
                String valueStr = getValueStr(fv);

                sb.append(String.format("  %s %s = %s%n", fieldType, fieldName, valueStr));
            }
        }

        return sb.toString();
    }

    private static String getValueStr(FieldValue fv) {
        String valueStr = String.valueOf(fv.getValue());

        if (fv instanceof ObjectFieldValue ofv) {
            Instance refInstance = ofv.getInstance();
            if (refInstance != null) {
                valueStr = String.format("Instance[id=%d, class=%s]",
                        refInstance.getInstanceId(),
                        refInstance.getJavaClass().getName());
            }
        }
        return valueStr;
    }
}
