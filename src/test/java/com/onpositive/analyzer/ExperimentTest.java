package com.onpositive.analyzer;

import com.onpositive.analyzer.printing.InstancePagePrinter;
import com.onpositive.analyzer.printing.InstancePrinter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Instance;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ExperimentTest {

    @Test
    @Disabled("Test for special absolute path hprof file to diagnmose problems when working with it")
    void testGetInstances() throws IOException {
        HeapDumpService service = new HeapDumpService();
        service.loadHeap("D:/work/heap_dump/heapdump-1780452136629.hprof");
        HeapDumpService.InstancePage instancesByClassName = service.getInstancesByClassName("com.intellij.openapi.diagnostic.RollingFileHandler", 0, 5);
        assertFalse(instancesByClassName.instances.isEmpty());
        InstancePagePrinter printer = new InstancePagePrinter();
        String printed = printer.print(instancesByClassName);
        assertNotEquals("No valid instances found", printed);
        Instance instance = service.getInstanceById(2247641296L);
        assertNotNull(instance);
        InstancePrinter instancePrinter = new InstancePrinter();
        String printedInstance = instancePrinter.print(instance);
        assertFalse(printedInstance.isEmpty());
        assertTrue(service.getInstanceRetainedSize(2247641296L) > 0);
    }
}
