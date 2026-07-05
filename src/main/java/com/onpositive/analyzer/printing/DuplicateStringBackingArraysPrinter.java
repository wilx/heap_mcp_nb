package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.HeapDumpService.DuplicateStringBackingArray;
import com.onpositive.analyzer.HeapDumpService.DuplicateStringBackingArrays;

public class DuplicateStringBackingArraysPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof DuplicateStringBackingArrays details)) {
            return object != null ? object.toString() : "";
        }

        StringBuilder result = new StringBuilder();
        result.append("Duplicate string backing arrays")
                .append(": representative_id=").append(details.representativeInstanceId)
                .append(", occurrences=").append(details.occurrenceCount)
                .append(", length=").append(details.stringLength)
                .append(", string_bytes=").append(details.stringShallowBytes)
                .append(", backing_arrays=").append(details.distinctBackingArrayCount)
                .append(", backing_bytes=").append(details.backingArrayShallowBytes)
                .append(", total_bytes=").append(details.totalShallowBytes)
                .append(", value=\"")
                .append(DuplicateStringsPagePrinter.escapedPreview(details.value, details.maxValueLength))
                .append('\"');

        for (DuplicateStringBackingArray backingArray : details.backingArrays) {
            result.append("\n")
                    .append("backing_array_id=").append(backingArray.backingArrayId)
                    .append(", shallow_size=").append(backingArray.shallowSize)
                    .append(", string_instance_ids=").append(backingArray.stringInstanceIds);
        }
        return result.toString();
    }
}
