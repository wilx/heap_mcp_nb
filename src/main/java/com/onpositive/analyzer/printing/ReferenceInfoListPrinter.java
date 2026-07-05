package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.HeapDumpService.ReferenceInfo;

import java.util.List;

public class ReferenceInfoListPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof List<?> list)) {
            return object != null ? object.toString() : "";
        }

        if (list.isEmpty()) {
            return "No references found";
        }

        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (item instanceof ReferenceInfo ref) {
                sb.append("Instance ID: ")
                  .append(ref.instanceId)
                  .append(", Class: ")
                  .append(ref.className);
                if (ref.fieldName != null) {
                    sb.append(", Via: ").append(ref.fieldName);
                }
                sb.append("\n");
            }
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }
}
