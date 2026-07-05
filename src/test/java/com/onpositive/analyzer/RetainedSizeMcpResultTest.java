package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;

import java.util.Map;

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
        assertTrue(content(result).contains("Instance 42 retained size: 256 bytes"));
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

        ToolsGetter toolsGetter = new ToolsGetter(new HeapDumpTools(service));
        return toolsGetter.getInstanceRetainedSizeTool().callHandler()
                .apply(null, new McpSchema.CallToolRequest("get_instance_retained_size", Map.of("id", id)));
    }

    private static String content(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }
}
