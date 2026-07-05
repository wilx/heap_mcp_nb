package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        String content = content(result);
        assertTrue(content.contains("ID: 11"), content);
        assertTrue(content.contains("Class: com.example.Largest"), content);
        assertTrue(content.contains("Retained Size: 512 bytes"), content);
    }

    @Test
    void retainedSizeFailureStaysInSuccessfulMcpPayload() throws Exception {
        Instance instance = instance(12L, "com.example.Broken", 0L);
        when(instance.getRetainedSize()).thenThrow(new IllegalStateException("retained graph failed"));
        Heap heap = mock(Heap.class);
        when(heap.getBiggestObjectsByRetainedSize(1)).thenReturn(List.of(instance));

        McpSchema.CallToolResult result = biggestObjectsResult(heap, 1);

        assertFalse(result.isError());
        String content = content(result);
        assertTrue(content.contains("ID: 12"), content);
        assertTrue(content.contains("Class: com.example.Broken"), content);
        assertTrue(content.contains("Retained Size Error: retained graph failed"), content);
    }

    private static McpSchema.CallToolResult biggestObjectsResult(Heap heap, int limit) throws Exception {
        HeapDumpService service = new HeapDumpService();
        java.lang.reflect.Field field = HeapDumpService.class.getDeclaredField("heap");
        field.setAccessible(true);
        field.set(service, heap);

        ToolsGetter toolsGetter = new ToolsGetter(new HeapDumpTools(service));
        return toolsGetter.getBiggestObjectsTool().callHandler()
                .apply(null, new McpSchema.CallToolRequest("get_biggest_objects", Map.of("limit", limit)));
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

    private static String content(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}
