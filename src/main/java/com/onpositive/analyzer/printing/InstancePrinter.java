package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.util.ClassUtil;
import com.onpositive.analyzer.util.ValueUtil;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.util.List;

public class InstancePrinter implements IValuePrinter {

    public static final int MAX_ARRAY_ITEMS = 100;

    @Override
    public String print(Object object) {
        if (!(object instanceof Instance instance)) {
            return object != null ? object.toString() : "";
        }

        String className = ClassUtil.getClassName(instance);
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, instance, className);
        if ("java.lang.String".equals(className)) {
            sb.append(String.format("Value: %s%n", ValueUtil.fastExtractStringValue(instance)));
            return sb.toString();
        }
        if (object instanceof PrimitiveArrayInstance arrayInstance) {
            sb.append("Array:").append(className).append("\n");
            sb.append(String.format("Length: %d%n", arrayInstance.getLength()));
            sb.append("Values: [");
            boolean addEllipsis = arrayInstance.getLength() > MAX_ARRAY_ITEMS;
            int to = Math.min(MAX_ARRAY_ITEMS, arrayInstance.getLength());
            List values = arrayInstance.getValues();
            for (int i = 0; i < to; i++) {
                sb.append(values.get(i).toString());
                if (i < to - 1) {
                    sb.append(",");
                }
            }
            if (addEllipsis) {
                sb.append("...");
            }
            sb.append("]\n");
            return sb.toString();
        }

        List<?> fieldValues = instance.getFieldValues();
        if (fieldValues != null && !fieldValues.isEmpty()) {
            sb.append(String.format("Fields:%n"));
            for (Object fvObj : fieldValues) {
                FieldValue fv = (FieldValue) fvObj;
                String fieldName = fv.getField().getName();
                String fieldType = fv.getField().getType().getName();
                String valueStr = InstanceQuickPrinter.formatFieldValue(fv);

                sb.append(String.format("  %s %s = %s%n", fieldType, fieldName, valueStr));
            }
        }

        return sb.toString();
    }

    private void appendHeader(StringBuilder sb, Instance instance, String className) {
        sb.append(String.format("Instance ID: %d%n", instance.getInstanceId()));
        sb.append(String.format("Class: %s%n", className));
        sb.append(String.format("Shallow Size: %d%n", instance.getSize()));
    }

}
