package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.HeapDumpService;
import org.netbeans.lib.profiler.heap.Instance;

import java.util.List;

public class InstanceListPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (object instanceof List<?> list) {
            if (list.isEmpty()) {
                return "No instances found";
            }

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Object item : list) {
                if (item instanceof HeapDumpService.RetainedInstance retainedInstance) {
                    sb.append(formatRetainedInstance(retainedInstance)).append("\n");
                    count++;
                } else if (item instanceof Instance inst) {
                    try {
                        sb.append(InstanceQuickPrinter.quickPrint(inst));
                        String fields = InstanceQuickPrinter.formatFieldsShort(inst);
                        if (!fields.isEmpty()) {
                            sb.append(", Fields: ").append(fields);
                        }
                        sb.append("\n");
                        count++;
                    } catch (Exception e) {
                        // Skip objects with invalid instance references
                    }
                }
            }
            if (count == 0) {
                return "No valid instances found";
            }
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }

        if (object instanceof Instance instance) {
            try {
                StringBuilder sb = new StringBuilder(InstanceQuickPrinter.quickPrint(instance));
                String fields = InstanceQuickPrinter.formatFieldsShort(instance);
                if (!fields.isEmpty()) {
                    sb.append(", Fields: ").append(fields);
                }
                return sb.toString();
            } catch (Exception e) {
                return "Invalid instance";
            }
        }

        return object != null ? object.toString() : "";
    }

    private String formatRetainedInstance(HeapDumpService.RetainedInstance retainedInstance) {
        StringBuilder sb = new StringBuilder(InstanceQuickPrinter.quickPrint(retainedInstance.instance()));
        if (retainedInstance.retainedSizeError() != null) {
            sb.append(", Retained Size Error: ").append(retainedInstance.retainedSizeError());
        } else {
            sb.append(", Retained Size: ").append(retainedInstance.retainedSize()).append(" bytes");
        }
        String fields = InstanceQuickPrinter.formatFieldsShort(retainedInstance.instance());
        if (!fields.isEmpty()) {
            sb.append(", Fields: ").append(fields);
        }
        return sb.toString();
    }
}
