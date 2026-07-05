package com.onpositive.analyzer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Instance;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ExperimentTest {

    @Test
    @Disabled("Manual diagnostic test for a developer-local absolute-path heap dump; not suitable for CI")
    void testGetInstances() throws IOException {
        HeapDumpService service = new HeapDumpService();
        service.loadHeap("D:/work/heap_dump/heapdump-1780452136629.hprof");
        HeapDumpService.InstancePage instancesByClassName = service.getInstancesByClassName("com.intellij.openapi.diagnostic.RollingFileHandler", 0, 5);
        assertFalse(instancesByClassName.instances.isEmpty());
        Instance instance = service.getInstanceById(2247641296L);
        assertNotNull(instance);
        assertTrue(service.getInstanceRetainedSize(2247641296L) > 0);
    }
}
