package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import com.onpositive.analyzer.printing.Bm25ResultListPrinter;
import com.onpositive.analyzer.printing.ClassStatsListPrinter;
import com.onpositive.analyzer.printing.DuplicateStringBackingArraysPrinter;
import com.onpositive.analyzer.printing.DuplicateStringsPagePrinter;
import com.onpositive.analyzer.printing.GCRootInfoListPrinter;
import com.onpositive.analyzer.printing.HeapSummaryPrinter;
import com.onpositive.analyzer.printing.InstanceListPrinter;
import com.onpositive.analyzer.printing.JavaClassListPrinter;
import com.onpositive.analyzer.printing.JavaClassPrinterWrapper;
import com.onpositive.analyzer.printing.PropertiesPrinter;
import com.onpositive.analyzer.printing.ReferenceInfoListPrinter;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ToolsFactoryConsistencyTest {

    private HeapDumpTools tools;

    @BeforeEach
    void setUp() {
        HeapDumpService service = new HeapDumpService();
        tools = new HeapDumpTools(service);
    }

    @Test
    void testAllMethodsHaveToolAnnotation() {
        Method[] methods = HeapDumpTools.class.getDeclaredMethods();
        int annotatedMethodCount = 0;
        for (Method method : methods) {
            if (method.isAnnotationPresent(McpTool.class)) {
                annotatedMethodCount++;
                McpTool tool = method.getAnnotation(McpTool.class);
                assertNotNull(tool.name(), "Tool name should not be null for method: " + method.getName());
                assertNotNull(tool.title(), "Tool title should not be null for method: " + method.getName());
                assertNotNull(tool.description(), "Tool description should not be null for method: " + method.getName());
                assertFalse(tool.name().isEmpty(), "Tool name should not be empty for method: " + method.getName());
            }
        }
        assertTrue(annotatedMethodCount > 0, "Should have at least one @Tool annotated method");
    }

    @Test
    void testSpecificationsCreatedFromAnnotations() {
        List<SyncToolSpecification> specs = createToolSpecs();
        assertFalse(specs.isEmpty(), "Should have at least one specification");
        
        Map<String, SyncToolSpecification> specsMap = specs.stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.tool().name(), s -> s));
        
        assertTrue(specsMap.containsKey("load_heap"), "Should have load_heap tool");
        assertTrue(specsMap.containsKey("get_classes_by_max_instances_count"), "Should have get_classes_by_max_instances_count tool");
        assertTrue(specsMap.containsKey("get_classes_by_max_instances_size"), "Should have get_classes_by_max_instances_size tool");
        assertTrue(specsMap.containsKey("get_biggest_objects"), "Should have get_biggest_objects tool");
        assertTrue(specsMap.containsKey("get_gc_roots"), "Should have get_gc_roots tool");
        assertFalse(specsMap.containsKey("get_gc_roots_paginated"), "Should not expose legacy get_gc_roots_paginated alias");
        assertTrue(specsMap.containsKey("get_summary"), "Should have get_summary tool");
        assertTrue(specsMap.containsKey("get_class_by_name"), "Should have get_class_by_name tool");
        assertTrue(specsMap.containsKey("get_instance_by_id"), "Should have get_instance_by_id tool");
        assertTrue(specsMap.containsKey("get_instance_retained_size"), "Should have get_instance_retained_size tool");
        assertTrue(specsMap.containsKey("get_classes_by_regexp"), "Should have get_classes_by_regexp tool");
        assertTrue(specsMap.containsKey("execute_oql"), "Should have execute_oql tool");
        assertTrue(specsMap.containsKey("analyze_heap_dump"), "Should have analyze_heap_dump tool");
        assertTrue(specsMap.containsKey("search_classes"), "Should have search_classes tool");
        assertTrue(specsMap.containsKey("get_duplicate_strings"), "Should have get_duplicate_strings tool");
        assertTrue(specsMap.containsKey("get_duplicate_string_backing_arrays"), "Should have get_duplicate_string_backing_arrays tool");
    }

    @Test
    void testToolSpecificationsHaveCorrectInputSchemas() {
        List<SyncToolSpecification> specs = createToolSpecs();
        
        SyncToolSpecification loadHeapSpec = specs.stream()
                .filter(s -> s.tool().name().equals("load_heap"))
                .findFirst()
                .orElseThrow();
        
        Map<String, Object> inputSchema = loadHeapSpec.tool().inputSchema();
        assertEquals("object", inputSchema.get("type"));
        assertTrue(required(inputSchema).contains("file_path"));
        assertEquals("Path to the .hprof heap dump file.",
                property(inputSchema, "file_path").get("description"));
        
        SyncToolSpecification getClassesSpec = specs.stream()
                .filter(s -> s.tool().name().equals("get_classes_by_max_instances_count"))
                .findFirst()
                .orElseThrow();
        
        inputSchema = getClassesSpec.tool().inputSchema();
        assertEquals("object", inputSchema.get("type"));
        assertFalse(required(inputSchema).contains("from"), "from should not be required");
        assertFalse(required(inputSchema).contains("to"), "to should not be required");
        assertTrue(properties(inputSchema).containsKey("from"));
        assertTrue(properties(inputSchema).containsKey("to"));

        SyncToolSpecification duplicateStringsSpec = specs.stream()
                .filter(s -> s.tool().name().equals("get_duplicate_strings"))
                .findFirst()
                .orElseThrow();

        inputSchema = duplicateStringsSpec.tool().inputSchema();
        assertTrue(properties(inputSchema).containsKey("sort_by"));
        assertTrue(properties(inputSchema).containsKey("max_value_length"));

        SyncToolSpecification duplicateStringBackingArraysSpec = specs.stream()
                .filter(s -> s.tool().name().equals("get_duplicate_string_backing_arrays"))
                .findFirst()
                .orElseThrow();

        inputSchema = duplicateStringBackingArraysSpec.tool().inputSchema();
        assertTrue(required(inputSchema).contains("representative_id"));
        assertTrue(properties(inputSchema).containsKey("max_value_length"));

        SyncToolSpecification biggestObjectsSpec = specs.stream()
                .filter(s -> s.tool().name().equals("get_biggest_objects"))
                .findFirst()
                .orElseThrow();

        inputSchema = biggestObjectsSpec.tool().inputSchema();
        assertTrue(required(inputSchema).contains("limit"));
    }

    @Test
    void typedToolsExposeOutputSchemas() {
        Map<String, SyncToolSpecification> specs = createToolSpecs().stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.tool().name(), s -> s));

        assertNotNull(specs.get("load_heap").tool().outputSchema());
        assertNotNull(specs.get("get_summary").tool().outputSchema());
        assertNotNull(specs.get("get_classes_by_max_instances_count").tool().outputSchema());
        assertNotNull(specs.get("get_biggest_objects").tool().outputSchema());
        assertNotNull(specs.get("get_instance_retained_size").tool().outputSchema());
        assertNotNull(specs.get("get_classes_by_regexp").tool().outputSchema());
        assertNotNull(specs.get("get_instances").tool().outputSchema());
        assertNotNull(specs.get("get_duplicate_strings").tool().outputSchema());

        assertNull(specs.get("execute_oql").tool().outputSchema(),
                "Free-form OQL output remains text-only");
    }

    @Test
    void mcpSchemasDescribeObjectPropertiesWhereAvailable() {
        List<String> missingDescriptions = new ArrayList<>();

        for (SyncToolSpecification spec : createToolSpecs()) {
            assertPropertiesHaveDescriptions(spec.tool().name() + ".input", spec.tool().inputSchema(), missingDescriptions);
            Map<String, Object> outputSchema = spec.tool().outputSchema();
            if (outputSchema != null) {
                assertPropertiesHaveDescriptions(spec.tool().name() + ".output", outputSchema, missingDescriptions);
            }
        }

        assertTrue(missingDescriptions.isEmpty(), "Missing schema descriptions: " + missingDescriptions);
    }

    @Test
    void testPrinterImplementationsExist() {
        assertDoesNotThrow(HeapSummaryPrinter::new);
        assertDoesNotThrow(ClassStatsListPrinter::new);
        assertDoesNotThrow(GCRootInfoListPrinter::new);
        assertDoesNotThrow(InstanceListPrinter::new);
        assertDoesNotThrow(JavaClassPrinterWrapper::new);
        assertDoesNotThrow(JavaClassListPrinter::new);
        assertDoesNotThrow(PropertiesPrinter::new);
        assertDoesNotThrow(ReferenceInfoListPrinter::new);
        assertDoesNotThrow(Bm25ResultListPrinter::new);
        assertDoesNotThrow(DuplicateStringsPagePrinter::new);
        assertDoesNotThrow(DuplicateStringBackingArraysPrinter::new);
    }

    @Test
    void testClassStatsListPrinter() {
        ClassStatsListPrinter printer = new ClassStatsListPrinter();
        List<HeapDumpService.ClassStats> stats = List.of(
                new HeapDumpService.ClassStats("TestClass", 100, 1000),
                new HeapDumpService.ClassStats("AnotherClass", 50, 500)
        );
        String result = printer.print(stats);
        assertTrue(result.contains("TestClass"));
        assertTrue(result.contains("100"));
        assertTrue(result.contains("AnotherClass"));
        assertTrue(result.contains("50"));
    }

    @Test
    void testGCRootInfoListPrinter() {
        GCRootInfoListPrinter printer = new GCRootInfoListPrinter();
        List<HeapDumpService.GCRootInfo> roots = List.of(
                new HeapDumpService.GCRootInfo("Thread", 123L, "java.lang.Thread")
        );
        String result = printer.print(roots);
        assertTrue(result.contains("Thread"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("java.lang.Thread"));
    }

    @Test
    void testReferenceInfoListPrinter() {
        ReferenceInfoListPrinter printer = new ReferenceInfoListPrinter();
        List<HeapDumpService.ReferenceInfo> refs = List.of(
                new HeapDumpService.ReferenceInfo(456L, "ReferencedClass", "fieldName"),
                new HeapDumpService.ReferenceInfo(789L, "java.lang.Object[]", "[7]")
        );
        String result = printer.print(refs);
        assertTrue(result.contains("Instance ID: 456, Class: ReferencedClass, Via: fieldName"));
        assertTrue(result.contains("Instance ID: 789, Class: java.lang.Object[], Via: [7]"));
    }

    @Test
    void testPropertiesPrinter() {
        PropertiesPrinter printer = new PropertiesPrinter();
        java.util.Properties props = new java.util.Properties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");
        String result = printer.print(props);
        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2=value2"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(Map<String, Object> schema, String name) {
        return (Map<String, Object>) properties(schema).get(name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }

    @SuppressWarnings("unchecked")
    private static List<String> required(Map<String, Object> schema) {
        return (List<String>) schema.get("required");
    }

    @SuppressWarnings("unchecked")
    private static void assertPropertiesHaveDescriptions(String path, Map<String, Object> schema, List<String> missingDescriptions) {
        Object propertiesObject = schema.get("properties");
        if (propertiesObject instanceof Map<?, ?> rawProperties) {
            for (Map.Entry<?, ?> entry : rawProperties.entrySet()) {
                String propertyName = String.valueOf(entry.getKey());
                Map<String, Object> propertySchema = (Map<String, Object>) entry.getValue();
                if (!hasText(propertySchema.get("description"))) {
                    missingDescriptions.add(path + "." + propertyName);
                }
                assertPropertiesHaveDescriptions(path + "." + propertyName, propertySchema, missingDescriptions);
            }
        }

        Object itemsObject = schema.get("items");
        if (itemsObject instanceof Map<?, ?> rawItems) {
            assertPropertiesHaveDescriptions(path + "[]", (Map<String, Object>) rawItems, missingDescriptions);
        }
    }

    private static boolean hasText(Object value) {
        return value instanceof String text && !text.isBlank();
    }

    private List<SyncToolSpecification> createToolSpecs() {
        return new SyncMcpToolProvider(List.of(tools)).getToolSpecifications();
    }
}
