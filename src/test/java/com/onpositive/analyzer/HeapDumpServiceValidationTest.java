package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeapDumpServiceValidationTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRangeCalls")
    void rejectsInvalidRanges(String name, ServiceCall call) throws Exception {
        HeapDumpService service = serviceWithHeap(emptyHeap());

        assertThrows(IllegalArgumentException.class, () -> call.invoke(service));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLimitCalls")
    void rejectsNegativeLimits(String name, ServiceCall call) throws Exception {
        HeapDumpService service = serviceWithHeap(emptyHeap());

        assertThrows(IllegalArgumentException.class, () -> call.invoke(service));
    }

    @Test
    void toIsExclusiveAndMayEqualFrom() throws Exception {
        HeapDumpService service = serviceWithHeap(emptyHeap());

        assertTrue(service.getClassesByMaxInstancesCount(0, 0).isEmpty());
        assertTrue(service.getGCRootsPaginated(0, 0).isEmpty());
        assertTrue(service.getJavaClassesByRegExpPaginated(".*", 0, 0).isEmpty());
        assertTrue(service.getInstancesByClassName("missing.Class", 0, 0).instances.isEmpty());
    }

    @Test
    void gcRootsSkipAdjacentDuplicatesBeforePagination() throws Exception {
        Heap heap = mock(Heap.class);
        List<GCRoot> roots = List.of(
                gcRoot(GCRoot.JNI_GLOBAL, 1L, "com.example.First"),
                gcRoot(GCRoot.JNI_GLOBAL, 1L, "com.example.First"),
                gcRoot(GCRoot.JNI_GLOBAL, 2L, "com.example.Second"),
                gcRoot(GCRoot.JNI_GLOBAL, 2L, "com.example.Second")
        );
        when(heap.getGCRoots()).thenReturn(roots);

        HeapDumpService service = serviceWithHeap(heap);

        List<HeapDumpService.GCRootInfo> page = service.getGCRootsPaginated(1, 2);

        assertEquals(1, page.size());
        assertEquals(2L, page.get(0).instanceId);
        assertEquals("com.example.Second", page.get(0).instanceClassName);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMcpRequests")
    void invalidValuesBecomeMcpArgumentErrors(String name, McpRequest request) {
        ToolsGetter toolsGetter = new ToolsGetter(new HeapDumpTools(new HeapDumpService()));

        McpSchema.CallToolResult result = request.invoke(toolsGetter);

        assertTrue(result.isError());
        assertTrue(content(result).contains("Invalid arguments:"), content(result));
    }

    private static Stream<Object[]> invalidRangeCalls() {
        return Stream.of(
                rangeCases("class count", (service, from, to) -> service.getClassesByMaxInstancesCount(from, to)),
                rangeCases("class size", (service, from, to) -> service.getClassesByMaxInstancesSize(from, to)),
                rangeCases("gc roots", (service, from, to) -> service.getGCRootsPaginated(from, to)),
                rangeCases("references", (service, from, to) -> service.getAllReferences(1L, from, to)),
                rangeCases("regexp classes", (service, from, to) -> service.getJavaClassesByRegExpPaginated(".*", from, to)),
                rangeCases("instances", (service, from, to) -> service.getInstancesByClassName("missing.Class", from, to)),
                rangeCases("duplicate strings", (service, from, to) -> service.getDuplicateStrings("total_bytes", from, to, 10))
        ).flatMap(stream -> stream);
    }

    private static Stream<Object[]> rangeCases(String name, RangeCall call) {
        return Stream.of(
                new Object[]{name + " negative from", (ServiceCall) service -> call.invoke(service, -1, 1)},
                new Object[]{name + " negative to", (ServiceCall) service -> call.invoke(service, 0, -1)},
                new Object[]{name + " from greater than to", (ServiceCall) service -> call.invoke(service, 2, 1)}
        );
    }

    private static Stream<Object[]> invalidLimitCalls() {
        return Stream.of(
                new Object[]{"biggest objects", (ServiceCall) service -> service.getBiggestObjectsByRetainedSize(-1)},
                new Object[]{"OQL max results", (ServiceCall) service -> service.executeOql("select 1", -1)},
                new Object[]{"top classes", (ServiceCall) service -> service.getTopClasses("missing.hprof", -1)},
                new Object[]{"BM25 top_n", (ServiceCall) service -> service.searchClassesBm25("query", -1, 0)},
                new Object[]{"BM25 from", (ServiceCall) service -> service.searchClassesBm25("query", 1, -1)},
                new Object[]{"duplicate max value length", (ServiceCall) service -> service.getDuplicateStrings("total_bytes", 0, 1, -1)}
        );
    }

    private static Stream<Object[]> invalidMcpRequests() {
        return Stream.of(
                new Object[]{"class page", (McpRequest) tools -> tools.getClassesByMaxInstancesCountTool().callHandler()
                        .apply(null, request("get_classes_by_max_instances_count", Map.of("from", -1, "to", 1)))},
                new Object[]{"biggest objects", (McpRequest) tools -> tools.getBiggestObjectsTool().callHandler()
                        .apply(null, request("get_biggest_objects", Map.of("limit", -1)))},
                new Object[]{"OQL", (McpRequest) tools -> tools.executeOqlTool().callHandler()
                        .apply(null, request("execute_oql", Map.of("query", "select 1", "max_results", -1)))},
                new Object[]{"BM25", (McpRequest) tools -> tools.searchClassesTool().callHandler()
                        .apply(null, request("search_classes", Map.of("query", "x", "top_n", -1, "from", 0)))},
                new Object[]{"one-shot analysis", (McpRequest) tools -> tools.analyzeHeapTool().callHandler()
                        .apply(null, request("analyze_heap_dump", Map.of("file_path", "missing.hprof", "limit", -1)))}
        );
    }

    private static McpSchema.CallToolRequest request(String name, Map<String, Object> arguments) {
        return new McpSchema.CallToolRequest(name, arguments);
    }

    private static String content(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    private static Heap emptyHeap() {
        Heap heap = mock(Heap.class);
        when(heap.getAllClasses()).thenReturn(List.of());
        when(heap.getGCRoots()).thenReturn(List.<GCRoot>of());
        when(heap.getJavaClassesByRegExp(".*")).thenReturn(List.of());
        when(heap.getBiggestObjectsByRetainedSize(0)).thenReturn(List.of());
        return heap;
    }

    private static GCRoot gcRoot(String kind, long instanceId, String className) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getName()).thenReturn(className);

        Instance instance = mock(Instance.class);
        when(instance.getInstanceId()).thenReturn(instanceId);
        when(instance.getJavaClass()).thenReturn(javaClass);

        GCRoot root = mock(GCRoot.class);
        when(root.getKind()).thenReturn(kind);
        when(root.getInstance()).thenReturn(instance);
        return root;
    }

    private static HeapDumpService serviceWithHeap(Heap heap) throws Exception {
        HeapDumpService service = new HeapDumpService();
        java.lang.reflect.Field field = HeapDumpService.class.getDeclaredField("heap");
        field.setAccessible(true);
        field.set(service, heap);
        return service;
    }

    @FunctionalInterface
    private interface ServiceCall {
        Object invoke(HeapDumpService service) throws Exception;
    }

    @FunctionalInterface
    private interface RangeCall {
        Object invoke(HeapDumpService service, int from, int to) throws Exception;
    }

    @FunctionalInterface
    private interface McpRequest {
        McpSchema.CallToolResult invoke(ToolsGetter toolsGetter);
    }
}
