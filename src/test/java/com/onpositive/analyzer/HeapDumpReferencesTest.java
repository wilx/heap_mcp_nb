package com.onpositive.analyzer;

import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeapDumpReferencesTest {

    @Test
    void returnsDefiningObjectsForFieldAndArrayReferences() throws Exception {
        Instance fieldOwner = instance(101, "example.FieldOwner", List.of());
        Instance arrayOwner = instance(202, "java.lang.Object[]", List.of());
        ObjectFieldValue fieldReference = fieldReference(fieldOwner, "payload");
        ArrayItemValue arrayReference = arrayReference(arrayOwner, 7);
        Instance target = instance(303, "example.Target", List.of(fieldReference, arrayReference));

        HeapDumpService service = serviceWithHeap(target);
        List<HeapDumpService.ReferenceInfo> references = service.getAllReferences(303, 0, 10);

        assertEquals(2, references.size());
        assertEquals(101, references.get(0).instanceId);
        assertEquals("example.FieldOwner", references.get(0).className);
        assertEquals("payload", references.get(0).fieldName);
        assertEquals(202, references.get(1).instanceId);
        assertEquals("java.lang.Object[]", references.get(1).className);
        assertEquals("[7]", references.get(1).fieldName);

        List<HeapDumpService.ReferenceInfo> secondPage = service.getAllReferences(303, 1, 2);
        assertEquals(1, secondPage.size());
        assertEquals(202, secondPage.getFirst().instanceId);
    }

    private static HeapDumpService serviceWithHeap(Instance target) throws Exception {
        Heap heap = mock(Heap.class);
        when(heap.getInstanceByID(target.getInstanceId())).thenReturn(target);
        HeapDumpService service = new HeapDumpService();
        java.lang.reflect.Field heapField = HeapDumpService.class.getDeclaredField("heap");
        heapField.setAccessible(true);
        heapField.set(service, heap);
        return service;
    }

    private static Instance instance(long id, String className, List<?> references) {
        Instance instance = mock(Instance.class);
        JavaClass javaClass = javaClass(className);
        when(instance.getInstanceId()).thenReturn(id);
        when(instance.getJavaClass()).thenReturn(javaClass);
        when(instance.getReferences()).thenReturn(references);
        return instance;
    }

    private static ObjectFieldValue fieldReference(Instance owner, String fieldName) {
        org.netbeans.lib.profiler.heap.Field field = mock(org.netbeans.lib.profiler.heap.Field.class);
        when(field.getName()).thenReturn(fieldName);
        ObjectFieldValue reference = mock(ObjectFieldValue.class);
        when(reference.getDefiningInstance()).thenReturn(owner);
        when(reference.getField()).thenReturn(field);
        return reference;
    }

    private static ArrayItemValue arrayReference(Instance owner, int index) {
        ArrayItemValue reference = mock(ArrayItemValue.class);
        when(reference.getDefiningInstance()).thenReturn(owner);
        when(reference.getIndex()).thenReturn(index);
        return reference;
    }

    private static JavaClass javaClass(String name) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getName()).thenReturn(name);
        return javaClass;
    }
}
