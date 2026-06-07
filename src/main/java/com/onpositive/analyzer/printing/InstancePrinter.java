package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.util.ClassUtil;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class InstancePrinter implements IValuePrinter {

    public static final int MAX_ARRAY_ITEMS = 100;

    @Override
    public String print(Object object) {
        if (!(object instanceof Instance instance)) {
            return object != null ? object.toString() : "";
        }

        String className = ClassUtil.getClassName(instance);
        if ("java.lang.String".equals(className)) {
             return  extractStringValue(instance);
        }
        if (object instanceof PrimitiveArrayInstance arrayInstance) {
            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("Array:").append(className).append(" values:[");
            boolean addEllipsis = arrayInstance.getLength() > MAX_ARRAY_ITEMS;
            int to = Math.min(100, arrayInstance.getLength());
            List values = arrayInstance.getValues();
            for (int i = 0; i < to; i++) {
                resultBuilder.append(values.get(i).toString());
                if (i < to - 1) {
                    resultBuilder.append(",");
                }
            }
            if (addEllipsis) {
                resultBuilder.append("...");
            }
            resultBuilder.append("]\n");
            return resultBuilder.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Instance ID: %d%n", instance.getInstanceId()));
        sb.append(String.format("Class: %s%n", className));
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
        if (fv instanceof ObjectFieldValue ofv) {
            Instance refInstance = ofv.getInstance();
            if (refInstance != null) {
                return String.format("Instance[id=%d, class=%s]",
                        refInstance.getInstanceId(),
                        refInstance.getJavaClass().getName());
            }
        }
        return fv.getValue();
    }

    private static String extractStringValue(Instance stringInstance) {
        Object valueField = stringInstance.getValueOfField("value");
        if (valueField == null) return "null";

        if (valueField instanceof PrimitiveArrayInstance array) {
            String typeName = array.getJavaClass().getName();
            List values = array.getValues();
            if (values == null) return "null";

            if ("char[]".equals(typeName)) {
                StringBuilder sb = new StringBuilder(values.size());
                for (Object v : values) {
                    if (v != null) {
                        if (v instanceof Character) {
                            sb.append((char) v);
                        } else {
                            sb.append(v.toString());
                        }
                    } else {
                        sb.append('?');
                    }
                }
                return sb.toString();
            } else if ("byte[]".equals(typeName)) {
                byte[] bytes = new byte[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    bytes[i] = ((Number) values.get(i)).byteValue();
                }
                Object coder = stringInstance.getValueOfField("coder");
                int coderValue = (coder instanceof Number) ? ((Number) coder).intValue() : 0;
                if (coderValue == 1) {
                    StringBuilder sb = new StringBuilder(bytes.length / 2);
                    for (int i = 0; i < bytes.length - 1; i += 2) {
                        char c = (char) (((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF));
                        sb.append(c);
                    }
                    return sb.toString();
                } else {
                    return new String(bytes, StandardCharsets.ISO_8859_1);
                }
            }
        }
        return String.valueOf(valueField);
    }
}
