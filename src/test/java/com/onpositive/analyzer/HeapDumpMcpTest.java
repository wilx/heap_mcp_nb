package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HeapDumpMcpTest {

    private TestMcpTools tools;
    private String samplePath;
    private HeapDumpService service;

    @BeforeEach
    void setUp() {
        service = new HeapDumpService();
        HeapDumpTools tools = new HeapDumpTools(service);
        this.tools = TestMcpTools.from(tools);
        File sampleFile = new File("src/test/resources/HeapDumpSample.hprof");
        assertTrue(sampleFile.exists(), "Sample heap dump file not found at " + sampleFile.getAbsolutePath());
        samplePath = sampleFile.getAbsolutePath();
    }

    @Test
    void testLoadHeapAndGetSummary() {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        McpSchema.CallToolResult result = tools.call(request);
        assertFalse(result.isError());
        Map<String, Object> summary = TestMcpContent.map(result);
        assertTrue(((Number) summary.get("totalLiveInstances")).longValue() > 0);
        assertTrue(((Number) summary.get("totalLiveBytes")).longValue() > 0);
    }

    @Test
    void testGetClassesByMaxInstancesCount() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);
        
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get_classes_by_max_instances_count", Map.of("from", 0, "to", 50));
        McpSchema.CallToolResult result = tools.call(request);
        assertFalse(result.isError());
        List<Map<String, Object>> classes = TestMcpContent.list(result);
        assertFalse(classes.isEmpty(), "Class list should not be empty");
        assertTrue(classes.stream().anyMatch(item -> "java.lang.String".equals(item.get("className"))),
                "Should contain java.lang.String");
    }

    @Test
    void testGetGCRootsAfterLoad() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);
        
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get_gc_roots", Map.of());
        McpSchema.CallToolResult result = tools.call(request);
        assertFalse(result.isError());
        assertNotNull(result.structuredContent());
    }

    @Test
    void testGetClassByNameAfterLoad() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);
        
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get_class_by_name", Map.of("name", "java.lang.String"));
        McpSchema.CallToolResult result = tools.call(request);
        assertFalse(result.isError(), result.toString());
        Map<String, Object> cls = TestMcpContent.map(result);
        assertEquals("java.lang.String", cls.get("name"));
        assertTrue(((Number) cls.get("instancesCount")).longValue() > 0);
        assertTrue(((Number) cls.get("allInstancesSize")).longValue() > 0);
    }

    @Test
    void testGetClassesByRegexpAfterLoad() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        
        McpSchema.CallToolResult result = tools.call(new McpSchema.CallToolRequest("get_classes_by_regexp", Map.of("regexp", "java\\.util\\..*")));
        assertFalse(result.isError());
        assertTrue(TestMcpContent.list(result).stream()
                .map(item -> (String) item.get("name"))
                .anyMatch(name -> name.contains("java.util.")));
    }

    @Test
    void testGetSummaryAfterLoad() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        
        McpSchema.CallToolResult result = tools.call(new McpSchema.CallToolRequest("get_summary", Map.of()));
        assertFalse(result.isError());
        Map<String, Object> summary = TestMcpContent.map(result);
        assertTrue(((Number) summary.get("totalLiveInstances")).longValue() > 0);
        assertTrue(((Number) summary.get("totalLiveBytes")).longValue() > 0);
    }

    @Test
    void testGetSystemPropertiesAfterLoad() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        
        McpSchema.CallToolResult result = tools.call(new McpSchema.CallToolRequest("get_system_properties", Map.of()));
        assertFalse(result.isError());
        assertNotNull(TestMcpContent.map(result));
    }

    @Test
    void testGetDuplicateStringsAfterLoad() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));

        McpSchema.CallToolResult result = tools.call(
                new McpSchema.CallToolRequest("get_duplicate_strings", Map.of(
                        "sort_by", "duplicate_count", "from", 0, "to", 5,
                        "max_value_length", 20)));

        assertFalse(result.isError());
        Map<String, Object> page = TestMcpContent.map(result);
        assertTrue(page.containsKey("items"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) page.get("items");
        assertFalse(items.isEmpty());
        assertTrue(items.get(0).containsKey("occurrenceCount"));
        assertTrue(items.get(0).containsKey("representativeInstanceId"));
        assertTrue(items.get(0).containsKey("totalShallowBytes"));
    }

    @Test
    void testGetDuplicateStringBackingArraysAfterLoad() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        HeapDumpService.DuplicateStringStats first = service
                .getDuplicateStrings("duplicate_count", 0, 1, 20).items.getFirst();

        McpSchema.CallToolResult result = tools.call(
                new McpSchema.CallToolRequest("get_duplicate_string_backing_arrays", Map.of(
                        "representative_id", Long.toString(first.representativeInstanceId),
                        "max_value_length", 20)));

        assertFalse(result.isError());
        Map<String, Object> arrays = TestMcpContent.map(result);
        assertEquals(first.representativeInstanceId, ((Number) arrays.get("representativeInstanceId")).longValue());
        List<Map<String, Object>> backingArrays = (List<Map<String, Object>>) arrays.get("backingArrays");
        assertFalse(backingArrays.isEmpty());
        assertTrue(backingArrays.get(0).containsKey("backingArrayId"));
        assertTrue(backingArrays.get(0).containsKey("stringInstanceIds"));
    }

    @Test
    void testGetDuplicateStringsRejectsInvalidSort() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));

        McpSchema.CallToolResult result = tools.call(
                new McpSchema.CallToolRequest("get_duplicate_strings", Map.of("sort_by", "retained_size")));

        assertTrue(result.isError());
        String content = TestMcpContent.text(result);
        assertTrue(content.contains("sort_by"));
    }

    @Test
    void testAnalyzeHeapDump() {
        McpSchema.CallToolResult result = tools.call(new McpSchema.CallToolRequest("analyze_heap_dump", Map.of(
                "file_path", samplePath,
                "limit", 5
        )));
        assertFalse(result.isError());
        String content = TestMcpContent.text(result);
        assertTrue(content.contains("Top"));
        assertTrue(content.contains("Class Name"));
    }

    @Test
    void testGetJavaClassByIdAfterLoad() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        
        McpSchema.CallToolResult result = tools.call(new McpSchema.CallToolRequest("get_class_by_id", Map.of("id", 1L)));
        if (!result.isError()) {
            Map<String, Object> cls = TestMcpContent.map(result);
            assertTrue(cls.containsKey("instancesCount"));
            assertTrue(cls.containsKey("allInstancesSize"));
        }
    }

    @Test
    void testChainedOperationsLoadGetClassesSummary() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        
        McpSchema.CallToolResult classesResult = tools.call(new McpSchema.CallToolRequest("get_classes_by_max_instances_count", Map.of("from", 0, "to", 50)));
        assertFalse(classesResult.isError());
        
        McpSchema.CallToolResult summaryResult = tools.call(new McpSchema.CallToolRequest("get_summary", Map.of()));
        assertFalse(summaryResult.isError());
        
        McpSchema.CallToolResult gcRootsResult = tools.call(new McpSchema.CallToolRequest("get_gc_roots", Map.of()));
        assertFalse(gcRootsResult.isError());
    }

    @Test
    void testMultipleLoadsReturnConsistentResults() {
        McpSchema.CallToolResult result1 = tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        assertFalse(result1.isError());
        Map<String, Object> content1 = TestMcpContent.map(result1);
        
        McpSchema.CallToolResult result2 = tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        assertFalse(result2.isError());
        Map<String, Object> content2 = TestMcpContent.map(result2);
        
        assertEquals(content1, content2, "Multiple loads should return consistent results");
    }

    @Test
    void testGetClassByNameNonExistent() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        
        McpSchema.CallToolResult result = tools.call(new McpSchema.CallToolRequest("get_class_by_name", Map.of("name", "com.nonexistent.Class")));
        assertTrue(result.isError(), "Should return error for non-existent class");
    }

    @Test
    void testGetClassesByRegexpNoMatch() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));
        
        McpSchema.CallToolResult result = tools.call(new McpSchema.CallToolRequest("get_classes_by_regexp", Map.of("regexp", "^[xyz].*")));
        assertFalse(result.isError());
        assertTrue(TestMcpContent.list(result).stream()
                .noneMatch(item -> ((String) item.get("name")).contains("java.")),
                "Should not contain java classes for regex ^[xyz].*");
    }

    @Test
    void testExecuteOqlWithoutLoadingHeap() {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("execute_oql", Map.of("query", "SELECT * FROM javax.swing.JFrame", "max_results", 10));
        McpSchema.CallToolResult result = tools.call(request);
        assertTrue(result.isError(), "Should return error when executing OQL without loading heap first");
        String content = TestMcpContent.text(result);
        assertTrue(content.contains("Heap not loaded") || content.contains("not loaded"), 
            "Error message should indicate heap is not loaded. Got: " + content);
    }

    @Test
    void testExecuteOqlAfterLoadingHeap() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);
        
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("execute_oql", Map.of("query", "select s from java.lang.String s", "max_results", 10));
        McpSchema.CallToolResult result = tools.call(request);
        assertFalse(result.isError(), result.toString());
        Map<String, Object> content = TestMcpContent.map(result);
        assertTrue(((Number) content.get("returnedCount")).intValue() > 0,
            "Should return valid rows when executing OQL after loading heap. Got: " + content);
        assertTrue(oqlRows(content).stream().anyMatch(row -> "string".equals(row.get("kind"))));
    }

    @Test
    void testOqlGetFieldAfterLoadingHeap() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("execute_oql", Map.of("query", "select s.value from java.lang.String s", "max_results", 10));
        McpSchema.CallToolResult result = tools.call(request);
        assertFalse(result.isError(), result.toString());
        Map<String, Object> content = TestMcpContent.map(result);
        assertTrue(oqlRows(content).stream()
                        .anyMatch(row -> "primitive_array".equals(row.get("kind"))
                                && "char[]".equals(row.get("className"))),
                "Should return char[] rows when executing OQL after loading heap. Got: " + content);
    }

    @Test
    void testExecuteOqlWithQualifiedClassName() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);
        
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("execute_oql", Map.of("query", "select f from javax.swing.JFrame f", "max_results", 10));
        McpSchema.CallToolResult result = tools.call(request);
        if (!result.isError()) {
            Map<String, Object> content = TestMcpContent.map(result);
            assertNotNull(content.get("rows"), "Should handle OQL query with qualified class name. Got: " + content);
        }
    }

    @Test
    void testGetAllReferencesAfterLoad() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);

        McpSchema.CallToolRequest oqlRequest = new McpSchema.CallToolRequest("execute_oql", Map.of("query", "select s from java.lang.String s where s.value != null", "max_results", 1));
        McpSchema.CallToolResult oqlResult = tools.call(oqlRequest);
        assertFalse(oqlResult.isError(), oqlResult.toString());
        List<Map<String, Object>> rows = oqlRows(TestMcpContent.map(oqlResult));

        if (!rows.isEmpty()) {
            long instanceId = ((Number) rows.get(0).get("instanceId")).longValue();
            McpSchema.CallToolRequest refsRequest = new McpSchema.CallToolRequest("get_all_references", Map.of("id", instanceId, "from", 0, "to", 10));
            McpSchema.CallToolResult refsResult = tools.call(refsRequest);
            assertFalse(refsResult.isError(), "get_all_references should not return error: " + refsResult);
        }
    }

    @Test
    void testGetAllReferencesInvalidId() {
        tools.call(new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath)));

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get_all_references", Map.of("id", 999999999L, "from", 0, "to", 10));
        McpSchema.CallToolResult result = tools.call(request);
        assertFalse(result.isError(), "Should handle non-existent instance gracefully");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> oqlRows(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("rows");
    }

    @Test
    @Disabled("HeapDumpSample.hprof triggers upstream NetBeans HprofGCRoot.getGCRoot() NullPointerException while computing retained sizes; covered by HeapDumpServiceBiggestObjectsTest")
    void testGetBiggestObjectsAfterLoad() {
        McpSchema.CallToolRequest loadRequest = new McpSchema.CallToolRequest("load_heap", Map.of("file_path", samplePath));
        tools.call(loadRequest);
        
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get_biggest_objects", Map.of("limit", 10));
        McpSchema.CallToolResult result = tools.call(request);
        
        assertFalse(result.isError(), "get_biggest_objects should not throw exception when heap is loaded. Error: " + 
            (result.content() != null && !result.content().isEmpty() ? TestMcpContent.text(result) : "unknown"));
        
        String content = TestMcpContent.text(result);
        assertNotNull(content, "Content should not be null");
    }
}
