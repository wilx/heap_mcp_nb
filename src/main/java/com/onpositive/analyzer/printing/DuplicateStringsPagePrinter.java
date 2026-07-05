package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.HeapDumpService.DuplicateStringStats;
import com.onpositive.analyzer.HeapDumpService.DuplicateStringsPage;

public class DuplicateStringsPagePrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof DuplicateStringsPage page)) {
            return object != null ? object.toString() : "";
        }

        StringBuilder result = new StringBuilder();
        result.append("Duplicate string groups: ").append(page.totalGroups)
                .append(", strings scanned: ").append(page.stringsScanned)
                .append(", decoding failures: ").append(page.decodingFailures)
                .append(", range: [").append(page.from).append(", ").append(page.to).append(")")
                .append(", remaining: ").append(page.remaining);

        if (page.items.isEmpty()) {
            return result.append("\nNo duplicate strings found in this range.").toString();
        }

        int rank = page.from + 1;
        for (DuplicateStringStats stats : page.items) {
            result.append("\n[").append(rank++).append("] ")
                    .append("occurrences=").append(stats.occurrenceCount)
                    .append(", duplicates=").append(stats.duplicateCount)
                    .append(", length=").append(stats.stringLength)
                    .append(", representative_id=").append(stats.representativeInstanceId)
                    .append(", string_bytes=").append(stats.stringShallowBytes)
                    .append(", backing_arrays=").append(stats.distinctBackingArrayCount)
                    .append(", backing_bytes=").append(stats.backingArrayShallowBytes)
                    .append(", total_bytes=").append(stats.totalShallowBytes)
                    .append(", value=\"").append(escapedPreview(stats.value, page.maxValueLength)).append('\"');
        }
        return result.toString();
    }

    static String escapedPreview(String value, int maxLength) {
        int end = Math.min(maxLength, value.length());
        if (end > 0 && end < value.length()
                && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }

        StringBuilder result = new StringBuilder(end + 16);
        for (int i = 0; i < end; i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> result.append("\\\\");
                case '\"' -> result.append("\\\"");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                default -> {
                    if (Character.isISOControl(character)) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        if (end < value.length()) {
            result.append("...");
        }
        return result.toString();
    }
}
