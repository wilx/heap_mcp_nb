package com.onpositive.analyzer.util;

import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.nio.ByteOrder;
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
        ValueUtil.Utf16ByteOrder defaultByteOrder = ValueUtil.defaultUtf16ByteOrder();
        Instance utf16 = string(Map.of(
                "value", array("byte[]", List.of(
                        utf16Byte('A', defaultByteOrder.hiByteShift()),
                        utf16Byte('A', defaultByteOrder.loByteShift()),
                        utf16Byte('\u0100', defaultByteOrder.hiByteShift()),
                        utf16Byte('\u0100', defaultByteOrder.loByteShift()))),
                "coder", (byte) 1));

        assertEquals("hi", ValueUtil.fastExtractStringValue(latin1));
        assertEquals("A\u0100", ValueUtil.fastExtractStringValue(utf16));
    }

    @Test
    void decodesCompactUtf16UsingDumpedHeapByteOrder() {
        JavaClass utf16Class = javaClass("java.lang.StringUTF16");
        when(utf16Class.getValueOfStaticField("HI_BYTE_SHIFT")).thenReturn(0);
        when(utf16Class.getValueOfStaticField("LO_BYTE_SHIFT")).thenReturn(8);
        Heap heap = mock(Heap.class);
        when(heap.getJavaClassByName("java.lang.StringUTF16")).thenReturn(utf16Class);
        Instance utf16 = string(Map.of(
                "value", array("byte[]", List.of((byte) 'A', (byte) 0, (byte) 0, (byte) 1)),
                "coder", (byte) 1));

        ValueUtil.Utf16ByteOrder byteOrder = ValueUtil.utf16ByteOrder(heap);

        assertEquals("A\u0100", ValueUtil.fastExtractStringValue(utf16, byteOrder));
    }

    @Test
    void defaultUtf16ByteOrderMatchesRunningJvmNativeOrder() {
        ValueUtil.Utf16ByteOrder expected = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
                ? new ValueUtil.Utf16ByteOrder(8, 0)
                : new ValueUtil.Utf16ByteOrder(0, 8);

        assertEquals(expected, ValueUtil.defaultUtf16ByteOrder());
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

    private static byte utf16Byte(char c, int shift) {
        return (byte) (c >> shift);
    }
}
