package com.onpositive.analyzer.util;

import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValueUtilTest {

    @Test
    void decodesLegacyCharArraySpan() {
        PrimitiveArrayInstance backing = array("char[]", List.of('a', 'b', 'c', 'd', 'e', 'f'));
        Instance string = string(Map.of("value", backing, "offset", 2, "count", 3));

        ValueUtil.DecodedString decoded = ValueUtil.decodeString(string);

        assertEquals("cde", decoded.value());
        assertSame(backing, decoded.backingArray());
    }

    @Test
    void decodesCompactLatin1AndUtf16Strings() {
        Instance latin1 = string(Map.of(
                "value", array("byte[]", List.of((byte) 'h', (byte) 'i')),
                "coder", (byte) 0));
        Instance utf16 = string(Map.of(
                "value", array("byte[]", List.of((byte) 0, (byte) 'A', (byte) 1, (byte) 0)),
                "coder", (byte) 1));

        assertEquals("hi", ValueUtil.fastExtractStringValue(latin1));
        assertEquals("A\u0100", ValueUtil.fastExtractStringValue(utf16));
    }

    @Test
    void rejectsUnsupportedOrInvalidLayouts() {
        assertNull(ValueUtil.decodeString(string(Map.of())));
        assertNull(ValueUtil.decodeString(string(Map.of(
                "value", array("byte[]", List.of((byte) 0)), "coder", (byte) 1))));
        assertNull(ValueUtil.decodeString(string(Map.of(
                "value", array("char[]", List.of('a')), "offset", 2, "count", 1))));
    }

    private static Instance string(Map<String, Object> fields) {
        Instance instance = mock(Instance.class);
        JavaClass javaClass = javaClass("java.lang.String");
        when(instance.getValueOfField(anyString()))
                .thenAnswer(invocation -> fields.get(invocation.getArgument(0, String.class)));
        when(instance.getJavaClass()).thenReturn(javaClass);
        when(instance.getInstanceId()).thenReturn(1L);
        when(instance.getSize()).thenReturn(1L);
        return instance;
    }

    private static PrimitiveArrayInstance array(String className, List<?> values) {
        PrimitiveArrayInstance array = mock(PrimitiveArrayInstance.class);
        JavaClass javaClass = javaClass(className);
        when(array.getJavaClass()).thenReturn(javaClass);
        when(array.getValues()).thenReturn(values);
        when(array.getLength()).thenReturn(values.size());
        when(array.getInstanceId()).thenReturn(2L);
        when(array.getSize()).thenReturn(2L);
        return array;
    }

    private static JavaClass javaClass(String name) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getName()).thenReturn(name);
        return javaClass;
    }
}
