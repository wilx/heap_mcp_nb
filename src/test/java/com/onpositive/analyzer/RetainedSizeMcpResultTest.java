package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetainedSizeMcpResultTest {

    @Test
    void retainedSizeSuccessIsSuccessfulMcpResult() throws Exception {
        Instance instance = mock(Instance.class);
        when(instance.getRetainedSize()).thenReturn(256L);
        Heap heap = mock(Heap.class);
        when(heap.getInstanceByID(42L)).thenReturn(instance);

        McpSchema.CallToolResult result = retainedSizeResult(heap, "42");

        assertFalse(result.isError());
        Map<String, Object> content = TestMcpContent.map(result);
        assertEquals(42L, ((Number) content.get("id")).longValue());
        assertEquals(256L, ((Number) content.get("retainedSize")).longValue());
    }

    @Test
    void missingInstanceIsErrorMcpResult() throws Exception {
        Heap heap = mock(Heap.class);
        when(heap.getInstanceByID(99L)).thenReturn(null);

        McpSchema.CallToolResult result = retainedSizeResult(heap, "99");

        assertTrue(result.isError());
        assertTrue(content(result).contains("Instance not found: 99"));
    }

    @Test
    void retainedSizeComputationFailureIsErrorMcpResult() throws Exception {
        Instance instance = mock(Instance.class);
        when(instance.getRetainedSize()).thenThrow(new IllegalStateException("retained graph failed"));
        Heap heap = mock(Heap.class);
        when(heap.getInstanceByID(7L)).thenReturn(instance);

        McpSchema.CallToolResult result = retainedSizeResult(heap, "7");

        assertTrue(result.isError());
        assertTrue(content(result).contains("retained graph failed"));
    }

    private static McpSchema.CallToolResult retainedSizeResult(Heap heap, String id) throws Exception {
        HeapDumpService service = new HeapDumpService();
        java.lang.reflect.Field field = HeapDumpService.class.getDeclaredField("heap");
        field.setAccessible(true);
        field.set(service, heap);

        TestMcpTools tools = TestMcpTools.from(new HeapDumpTools(service));
        return tools.call("get_instance_retained_size", Map.of("id", id));
    }

    private static String content(McpSchema.CallToolResult result) {
        return TestMcpContent.text(result);
    }
}
