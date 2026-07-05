package com.onpositive.analyzer.util;

import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ValueUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValueUtil.class);

    public record DecodedString(String value, PrimitiveArrayInstance backingArray) {
    }

    public static String fastExtractStringValue(Instance stringInstance) {
        DecodedString decoded = decodeString(stringInstance);
        return decoded != null ? decoded.value() : null;
    }

    /**
     * Decodes the common java.lang.String layouts found in heap dumps.
     * Returns {@code null} when the instance does not expose a supported layout.
     */
    public static DecodedString decodeString(Instance stringInstance) {
        if (stringInstance == null) return null;
        Object valueField = stringInstance.getValueOfField("value");
        if (!(valueField instanceof PrimitiveArrayInstance array)) return null;

        String typeName = array.getJavaClass().getName();
        List<?> values = array.getValues();
        if (values == null) return null;

        try {
            if ("char[]".equals(typeName)) {
                int offset = intField(stringInstance, "offset", 0);
                int count = intField(stringInstance, "count", values.size() - offset);
                if (offset < 0 || count < 0 || offset > values.size() - count) return null;

                StringBuilder sb = new StringBuilder(count);
                for (int i = offset; i < offset + count; i++) {
                    Object value = values.get(i);
                    if (value instanceof Character character) {
                        sb.append(character.charValue());
                    } else if (value instanceof Number number) {
                        sb.append((char) number.intValue());
                    } else if (value != null && value.toString().length() == 1) {
                        sb.append(value);
                    } else {
                        return null;
                    }
                }
                return new DecodedString(sb.toString(), array);
            }

            if ("byte[]".equals(typeName)) {
                byte[] bytes = new byte[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    Object val = values.get(i);
                    if (val instanceof Number) {
                        bytes[i] = ((Number) val).byteValue();
                    } else if (val != null) {
                        bytes[i] = Byte.parseByte(val.toString());
                    } else {
                        return null;
                    }
                }
                Object coder = stringInstance.getValueOfField("coder");
                int coderValue = (coder instanceof Number) ? ((Number) coder).intValue() : 0;
                if (coderValue == 1) {
                    if ((bytes.length & 1) != 0) return null;
                    StringBuilder sb = new StringBuilder(bytes.length / 2);
                    for (int i = 0; i < bytes.length; i += 2) {
                        char c = (char) (((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF));
                        sb.append(c);
                    }
                    return new DecodedString(sb.toString(), array);
                }
                if (coderValue == 0) {
                    return new DecodedString(new String(bytes, StandardCharsets.ISO_8859_1), array);
                }
                return null;
            }
        } catch (RuntimeException ex) {
            LOGGER.debug("Cannot decode String instance {}", stringInstance.getInstanceId(), ex);
            return null;
        }
        return null;
    }

    private static int intField(Instance instance, String name, int defaultValue) {
        Object value = instance.getValueOfField(name);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }
}
