package com.onpositive.analyzer;

import com.onpositive.analyzer.printing.InstanceListPrinter;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeapDumpServiceBiggestObjectsTest {

    @Test
    void keepsBiggestObjectOrderAndPrintsRetainedSizes() throws Exception {
        Instance largest = instance(1L, "com.example.Largest", 300L);
        Instance second = instance(2L, "com.example.Second", 120L);
        Heap heap = mock(Heap.class);
        when(heap.getBiggestObjectsByRetainedSize(2)).thenReturn(List.of(largest, second));

        HeapDumpService service = serviceWithHeap(heap);
        List<HeapDumpService.RetainedInstance> result = service.getBiggestObjectsByRetainedSize(2);

        assertEquals(List.of(1L, 2L), result.stream().map(item -> item.instance().getInstanceId()).toList());
        assertEquals(List.of(300L, 120L), result.stream().map(HeapDumpService.RetainedInstance::retainedSize).toList());
        assertEquals("""
                ID: 1, Class: com.example.Largest, Retained Size: 300 bytes
                ID: 2, Class: com.example.Second, Retained Size: 120 bytes""",
                new InstanceListPrinter().print(result));
    }

    @Test
    void printsPerItemRetainedSizeFailureWithoutSubstitutingShallowSize() throws Exception {
        Instance instance = instance(3L, "com.example.Broken", 0L);
        when(instance.getRetainedSize()).thenThrow(new IllegalStateException("cannot compute retained size"));
        Heap heap = mock(Heap.class);
        when(heap.getBiggestObjectsByRetainedSize(1)).thenReturn(List.of(instance));

        HeapDumpService service = serviceWithHeap(heap);
        List<HeapDumpService.RetainedInstance> result = service.getBiggestObjectsByRetainedSize(1);

        assertNull(result.get(0).retainedSize());
        assertEquals("cannot compute retained size", result.get(0).retainedSizeError());
        assertEquals("ID: 3, Class: com.example.Broken, Retained Size Error: cannot compute retained size",
                new InstanceListPrinter().print(result));
    }

    private static HeapDumpService serviceWithHeap(Heap heap) throws Exception {
        HeapDumpService service = new HeapDumpService();
        java.lang.reflect.Field field = HeapDumpService.class.getDeclaredField("heap");
        field.setAccessible(true);
        field.set(service, heap);
        return service;
    }

    private static Instance instance(long id, String className, long retainedSize) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getName()).thenReturn(className);

        Instance instance = mock(Instance.class);
        when(instance.getInstanceId()).thenReturn(id);
        when(instance.getJavaClass()).thenReturn(javaClass);
        when(instance.getRetainedSize()).thenReturn(retainedSize);
        return instance;
    }
}
