package com.onpositive.analyzer.mcp;

import com.onpositive.analyzer.HeapDumpService;
import com.onpositive.analyzer.printing.InstanceQuickPrinter;
import com.onpositive.analyzer.util.ClassUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.netbeans.lib.profiler.heap.HeapSummary;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;

public final class McpDtos {

    private McpDtos() {
    }

    public record HeapSummaryDto(
            @Schema(description = "Timestamp recorded in the heap dump summary.")
            long time,
            @Schema(description = "Total bytes allocated by the profiled JVM before the heap dump was written.")
            long totalAllocatedBytes,
            @Schema(description = "Total number of objects allocated by the profiled JVM before the heap dump was written.")
            long totalAllocatedInstances,
            @Schema(description = "Total bytes occupied by live objects in the heap dump.")
            long totalLiveBytes,
            @Schema(description = "Total number of live objects in the heap dump.")
            long totalLiveInstances) {

        static HeapSummaryDto from(HeapSummary summary) {
            return new HeapSummaryDto(
                    summary.getTime(),
                    summary.getTotalAllocatedBytes(),
                    summary.getTotalAllocatedInstances(),
                    summary.getTotalLiveBytes(),
                    summary.getTotalLiveInstances());
        }
    }

    public record JavaClassDto(
            @Schema(description = "Internal heap class ID.")
            long id,
            @Schema(description = "Fully qualified Java class name.")
            String name,
            @Schema(description = "Whether this class represents an array type.")
            boolean array,
            @Schema(description = "Shallow instance size in bytes for objects of this class.")
            int instanceSize,
            @Schema(description = "Number of live instances of this class in the heap dump.")
            int instancesCount,
            @Schema(description = "Total shallow bytes used by all live instances of this class.")
            long allInstancesSize,
            @Schema(description = "Retained size attributed to this class, or -1 when not computed.")
            long retainedSizeByClass,
            @Schema(description = "Fully qualified superclass name, or empty when no superclass is available.")
            String superClassName) {

        static JavaClassDto from(JavaClass javaClass) {
            if (javaClass == null) {
                return null;
            }
            JavaClass superClass = javaClass.getSuperClass();
            return new JavaClassDto(
                    javaClass.getJavaClassId(),
                    javaClass.getName(),
                    javaClass.isArray(),
                    javaClass.getInstanceSize(),
                    javaClass.getInstancesCount(),
                    javaClass.getAllInstancesSize(),
                    -1L,
                    superClass != null ? superClass.getName() : "");
        }

        static JavaClassDto summary(JavaClass javaClass) {
            if (javaClass == null) {
                return null;
            }
            return new JavaClassDto(
                    javaClass.getJavaClassId(),
                    javaClass.getName(),
                    javaClass.isArray(),
                    javaClass.getInstanceSize(),
                    javaClass.getInstancesCount(),
                    javaClass.getAllInstancesSize(),
                    -1L,
                    javaClass.getSuperClass() != null ? javaClass.getSuperClass().getName() : "");
        }
    }

    public record InstanceDto(
            @Schema(description = "Internal heap instance ID.")
            long id,
            @Schema(description = "Instance sequence number assigned by the heap parser.")
            int instanceNumber,
            @Schema(description = "Fully qualified Java class name for this instance.")
            String className,
            @Schema(description = "Shallow size in bytes of this instance.")
            long shallowSize,
            @Schema(description = "Whether this instance is directly reported as a GC root.")
            boolean gcRoot,
            @Schema(description = "Short field-value summary for this instance.")
            String fields) {

        static InstanceDto from(Instance instance) {
            if (instance == null) {
                return null;
            }
            return new InstanceDto(
                    instance.getInstanceId(),
                    instance.getInstanceNumber(),
                    ClassUtil.getClassName(instance),
                    instance.getSize(),
                    instance.isGCRoot(),
                    InstanceQuickPrinter.formatFieldsShort(instance));
        }

        static InstanceDto summary(Instance instance) {
            if (instance == null) {
                return null;
            }
            return new InstanceDto(
                    instance.getInstanceId(),
                    instance.getInstanceNumber(),
                    ClassUtil.getClassName(instance),
                    instance.getSize(),
                    instance.isGCRoot(),
                    "");
        }
    }

    public record InstancePageDto(
            @Schema(description = "Page of instances for the requested class.")
            List<InstanceDto> instances,
            @Schema(description = "Total number of instances available for the requested class.")
            long totalCount,
            @Schema(description = "Number of instances remaining after this page.")
            long remaining) {

        static InstancePageDto from(HeapDumpService.InstancePage page) {
            return new InstancePageDto(
                    page.instances.stream().map(InstanceDto::summary).toList(),
                    page.totalCount,
                    page.remaining);
        }
    }

    public record RetainedInstanceDto(
            @Schema(description = "Internal heap instance ID.")
            long id,
            @Schema(description = "Fully qualified Java class name for this instance.")
            String className,
            @Schema(description = "Shallow size in bytes of this instance.")
            long shallowSize,
            @Schema(description = "Retained size in bytes, or -1 when retained-size computation failed.")
            long retainedSize,
            @Schema(description = "Retained-size computation error message, or empty when computation succeeded.")
            String retainedSizeError) {

        static RetainedInstanceDto from(HeapDumpService.RetainedInstance retainedInstance) {
            Instance instance = retainedInstance.instance();
            return new RetainedInstanceDto(
                    instance.getInstanceId(),
                    ClassUtil.getClassName(instance),
                    instance.getSize(),
                    retainedInstance.retainedSize() != null ? retainedInstance.retainedSize() : -1L,
                    retainedInstance.retainedSizeError() != null ? retainedInstance.retainedSizeError() : "");
        }
    }

    public record InstanceRetainedSizeDto(
            @Schema(description = "Internal heap instance ID.")
            long id,
            @Schema(description = "Retained size in bytes for the requested instance.")
            long retainedSize) {
    }
}
