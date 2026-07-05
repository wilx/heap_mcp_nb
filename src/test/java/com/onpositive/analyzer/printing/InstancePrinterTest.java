package com.onpositive.analyzer.printing;

import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.Type;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstancePrinterTest {

    private final InstancePrinter printer = new InstancePrinter();

    @Test
    void printsOrdinaryInstanceWithIdentityShallowSizeAndFields() {
        Instance instance = instance(123L, "com.example.Widget", 64L);
        FieldValue fieldValue = fieldValue("int", "count", "7");
        when(instance.getFieldValues()).thenReturn(List.of(fieldValue));

        assertEquals("""
                Instance ID: 123
                Class: com.example.Widget
                Shallow Size: 64
                Fields:
                  int count = 7
                """, printer.print(instance));
    }

    @Test
    void printsStringWithIdentityShallowSizeAndDecodedValue() {
        Instance string = string(234L, 24L, Map.of(
                "value", primitiveArray(235L, "byte[]", 16L, List.of((byte) 'h', (byte) 'i')),
                "coder", (byte) 0));

        assertEquals("""
                Instance ID: 234
                Class: java.lang.String
                Shallow Size: 24
                Value: hi
                """, printer.print(string));
    }

    @Test
    void printsPrimitiveArrayWithIdentityShallowSizeAndValues() {
        PrimitiveArrayInstance array = primitiveArray(345L, "int[]", 32L, List.of(1, 2, 3));

        assertEquals("""
                Instance ID: 345
                Class: int[]
                Shallow Size: 32
                Array:int[]
                Length: 3
                Values: [1,2,3]
                """, printer.print(array));
    }

    private static Instance string(long id, long shallowSize, Map<String, Object> fields) {
        Instance instance = instance(id, "java.lang.String", shallowSize);
        when(instance.getValueOfField(anyString()))
                .thenAnswer(invocation -> fields.get(invocation.getArgument(0, String.class)));
        return instance;
    }

    private static Instance instance(long id, String className, long shallowSize) {
        Instance instance = mock(Instance.class);
        JavaClass javaClass = javaClass(className);
        when(instance.getInstanceId()).thenReturn(id);
        when(instance.getJavaClass()).thenReturn(javaClass);
        when(instance.getSize()).thenReturn(shallowSize);
        return instance;
    }

    private static PrimitiveArrayInstance primitiveArray(long id, String className, long shallowSize, List<?> values) {
        PrimitiveArrayInstance array = mock(PrimitiveArrayInstance.class);
        JavaClass javaClass = javaClass(className);
        when(array.getInstanceId()).thenReturn(id);
        when(array.getJavaClass()).thenReturn(javaClass);
        when(array.getSize()).thenReturn(shallowSize);
        when(array.getLength()).thenReturn(values.size());
        when(array.getValues()).thenReturn(values);
        return array;
    }

    private static FieldValue fieldValue(String typeName, String name, String value) {
        Type type = mock(Type.class);
        when(type.getName()).thenReturn(typeName);

        Field field = mock(Field.class);
        when(field.getName()).thenReturn(name);
        when(field.getType()).thenReturn(type);

        FieldValue fieldValue = mock(FieldValue.class);
        when(fieldValue.getField()).thenReturn(field);
        when(fieldValue.getValue()).thenReturn(value);
        return fieldValue;
    }

    private static JavaClass javaClass(String name) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getName()).thenReturn(name);
        return javaClass;
    }
}
