package com.onpositive.analyzer;

import com.onpositive.analyzer.util.ClassUtil;
import com.onpositive.analyzer.util.ValueUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.util.ArrayList;
import java.util.List;

public final class InstanceFieldValues {

    public static final int MAX_ARRAY_ITEMS = 100;
    public static final int MAX_STRING_LENGTH = 200;

    private InstanceFieldValues() {
    }

    public static List<InstanceFieldValue> from(Instance instance) {
        return from(instance, ValueUtil.defaultUtf16ByteOrder());
    }

    public static List<InstanceFieldValue> from(Instance instance, ValueUtil.Utf16ByteOrder utf16ByteOrder) {
        List<?> rawFieldValues = instance.getFieldValues();
        if (rawFieldValues == null || rawFieldValues.isEmpty()) {
            return List.of();
        }

        List<InstanceFieldValue> values = new ArrayList<>(rawFieldValues.size());
        for (Object rawFieldValue : rawFieldValues) {
            if (rawFieldValue instanceof org.netbeans.lib.profiler.heap.FieldValue fieldValue) {
                values.add(from(fieldValue, utf16ByteOrder));
            }
        }
        return values;
    }

    public static List<String> arrayPreview(PrimitiveArrayInstance arrayInstance) {
        int to = Math.min(MAX_ARRAY_ITEMS, arrayInstance.getLength());
        List<?> values = arrayInstance.getValues();
        to = Math.min(to, values.size());

        List<String> preview = new ArrayList<>(to);
        for (int i = 0; i < to; i++) {
            preview.add(String.valueOf(values.get(i)));
        }
        return preview;
    }

    private static InstanceFieldValue from(org.netbeans.lib.profiler.heap.FieldValue fieldValue,
                                           ValueUtil.Utf16ByteOrder utf16ByteOrder) {
        Field field = fieldValue.getField();
        String fieldName = field != null && field.getName() != null ? field.getName() : "";
        String declaredType = field != null && field.getType() != null && field.getType().getName() != null
                ? field.getType().getName()
                : "";

        if (fieldValue instanceof ObjectFieldValue objectFieldValue) {
            Instance referencedInstance = objectFieldValue.getInstance();
            if (referencedInstance == null) {
                return new InstanceFieldValue(fieldName, declaredType, "null", null,
                        false, -1, -1L, "", -1, List.of());
            }

            String className = ClassUtil.getClassName(referencedInstance);
            if ("java.lang.String".equals(className)) {
                String value = ValueUtil.fastExtractStringValue(referencedInstance, utf16ByteOrder);
                String returnedValue = value;
                boolean truncated = value != null && value.length() > MAX_STRING_LENGTH;
                if (truncated) {
                    returnedValue = value.substring(0, MAX_STRING_LENGTH);
                }
                return new InstanceFieldValue(fieldName, declaredType, "string", returnedValue,
                        truncated, value != null ? value.length() : -1, referencedInstance.getInstanceId(),
                        className, -1, List.of());
            }

            if (referencedInstance instanceof PrimitiveArrayInstance arrayInstance) {
                return new InstanceFieldValue(fieldName, declaredType, "primitive_array", null,
                        false, -1, referencedInstance.getInstanceId(), className,
                        arrayInstance.getLength(), arrayPreview(arrayInstance));
            }

            return new InstanceFieldValue(fieldName, declaredType, "instance", null,
                    false, -1, referencedInstance.getInstanceId(), className, -1, List.of());
        }

        return new InstanceFieldValue(fieldName, declaredType, "primitive", fieldValue.getValue(),
                false, -1, -1L, "", -1, List.of());
    }

    public record InstanceFieldValue(
            @Schema(description = "Field name on the containing instance.")
            String name,
            @Schema(description = "Declared Java field type, or empty when unavailable.")
            String declaredType,
            @Schema(description = "Field value kind: primitive, string, primitive_array, instance, or null.")
            String kind,
            @Schema(description = "Primitive or string value; null for object and array references.")
            String value,
            @Schema(description = "Whether the returned string value was truncated.")
            boolean valueTruncated,
            @Schema(description = "Original string value length in UTF-16 code units, or -1 for non-string values.")
            int valueLength,
            @Schema(description = "Referenced heap instance ID, or -1 when the field does not reference an instance.")
            long referencedInstanceId,
            @Schema(description = "Heap class name of the referenced instance, or empty when not applicable.")
            String referencedClassName,
            @Schema(description = "Array length for primitive array references, or -1 for other values.")
            int arrayLength,
            @Schema(description = "Preview values for primitive array references, capped at the configured maximum.")
            List<String> arrayPreview) {
    }
}
