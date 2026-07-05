package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BiggestObjectsMcpResultTest {

    @Test
    void biggestObjectsSuccessIncludesRetainedSizeFields() throws Exception {
        Instance instance = instance(11L, "com.example.Largest", 512L);
        Heap heap = mock(Heap.class);
        when(heap.getBiggestObjectsByRetainedSize(1)).thenReturn(List.of(instance));

        McpSchema.CallToolResult result = biggestObjectsResult(heap, 1);

        assertFalse(result.isError());
        Map<String, Object> item = content(result);
        assertEquals(11L, ((Number) item.get("id")).longValue());
        assertEquals("com.example.Largest", item.get("className"));
        assertEquals(512L, ((Number) item.get("retainedSize")).longValue());
    }

    @Test
    void retainedSizeFailureStaysInSuccessfulMcpPayload() throws Exception {
        Instance instance = instance(12L, "com.example.Broken", 0L);
        when(instance.getRetainedSize()).thenThrow(new IllegalStateException("retained graph failed"));
        Heap heap = mock(Heap.class);
        when(heap.getBiggestObjectsByRetainedSize(1)).thenReturn(List.of(instance));

        McpSchema.CallToolResult result = biggestObjectsResult(heap, 1);

        assertFalse(result.isError());
        Map<String, Object> item = content(result);
        assertEquals(12L, ((Number) item.get("id")).longValue());
        assertEquals("com.example.Broken", item.get("className"));
        assertEquals("retained graph failed", item.get("retainedSizeError"));
    }

    private static McpSchema.CallToolResult biggestObjectsResult(Heap heap, int limit) throws Exception {
        HeapDumpService service = new HeapDumpService();
        java.lang.reflect.Field field = HeapDumpService.class.getDeclaredField("heap");
        field.setAccessible(true);
        field.set(service, heap);

        TestMcpTools tools = TestMcpTools.from(new HeapDumpTools(service));
        return tools.call("get_biggest_objects", Map.of("limit", limit));
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

    private static Map<String, Object> content(McpSchema.CallToolResult result) {
        return TestMcpContent.list(result).get(0);
    }
}
