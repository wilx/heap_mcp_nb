package com.onpositive.analyzer.printing;

import org.netbeans.lib.profiler.heap.HeapSummary;

public class HeapSummaryPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof HeapSummary summary)) {
            return object != null ? object.toString() : "";
        }

        return String.format("Total Instances: %d\n Total allocated instances: %d\n, Total Size: %d bytes\nTotal allocated: %d bytes, Time: %d",
                summary.getTotalLiveInstances(), summary.getTotalAllocatedInstances(), summary.getTotalLiveBytes(), summary.getTotalAllocatedBytes(), summary.getTime());
    }
}
