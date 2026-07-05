package com.onpositive.analyzer.util;

import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        return (Instance) Proxy.newProxyInstance(
                ValueUtilTest.class.getClassLoader(), new Class<?>[]{Instance.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getValueOfField" -> fields.get((String) args[0]);
                    case "getJavaClass" -> javaClass("java.lang.String");
                    case "getInstanceId", "getSize" -> 1L;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static PrimitiveArrayInstance array(String className, List<?> values) {
        return (PrimitiveArrayInstance) Proxy.newProxyInstance(
                ValueUtilTest.class.getClassLoader(), new Class<?>[]{PrimitiveArrayInstance.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getJavaClass" -> javaClass(className);
                    case "getValues" -> values;
                    case "getLength" -> values.size();
                    case "getInstanceId", "getSize" -> 2L;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static JavaClass javaClass(String name) {
        return (JavaClass) Proxy.newProxyInstance(
                ValueUtilTest.class.getClassLoader(), new Class<?>[]{JavaClass.class},
                (proxy, method, args) -> "getName".equals(method.getName())
                        ? name : defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
