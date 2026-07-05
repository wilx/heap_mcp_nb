package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import com.onpositive.analyzer.mcp.reflection.Tool;
import com.onpositive.analyzer.mcp.reflection.ToolsFactory;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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
            if (method.isAnnotationPresent(Tool.class)) {
                annotatedMethodCount++;
                Tool tool = method.getAnnotation(Tool.class);
                assertNotNull(tool.name(), "Tool name should not be null for method: " + method.getName());
                assertNotNull(tool.title(), "Tool title should not be null for method: " + method.getName());
                assertNotNull(tool.decription(), "Tool description should not be null for method: " + method.getName());
                assertFalse(tool.name().isEmpty(), "Tool name should not be empty for method: " + method.getName());
            }
        }
        assertTrue(annotatedMethodCount > 0, "Should have at least one @Tool annotated method");
    }

    @Test
    void testSpecificationsCreatedFromAnnotations() {
        List<SyncToolSpecification> specs = ToolsFactory.createToolSpecs(tools);
        assertFalse(specs.isEmpty(), "Should have at least one specification");
        
        Map<String, SyncToolSpecification> specsMap = specs.stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.tool().name(), s -> s));
        
        assertTrue(specsMap.containsKey("load_heap"), "Should have load_heap tool");
        assertTrue(specsMap.containsKey("get_classes_by_max_instances_count"), "Should have get_classes_by_max_instances_count tool");
        assertTrue(specsMap.containsKey("get_classes_by_max_instances_size"), "Should have get_classes_by_max_instances_size tool");
        assertTrue(specsMap.containsKey("get_biggest_objects"), "Should have get_biggest_objects tool");
        assertTrue(specsMap.containsKey("get_gc_roots"), "Should have get_gc_roots tool");
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
        List<SyncToolSpecification> specs = ToolsFactory.createToolSpecs(tools);
        
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
        assertEquals(0, property(inputSchema, "from").get("default"));
        assertEquals(0L, property(inputSchema, "from").get("minimum"));
        assertEquals(50, property(inputSchema, "to").get("default"));
        assertEquals(0L, property(inputSchema, "to").get("minimum"));

        SyncToolSpecification duplicateStringsSpec = specs.stream()
                .filter(s -> s.tool().name().equals("get_duplicate_strings"))
                .findFirst()
                .orElseThrow();

        inputSchema = duplicateStringsSpec.tool().inputSchema();
        assertEquals("total_bytes", property(inputSchema, "sort_by").get("default"));
        assertEquals(List.of("total_bytes", "duplicate_count"), property(inputSchema, "sort_by").get("enum"));
        assertEquals(200, property(inputSchema, "max_value_length").get("default"));
        assertEquals(0L, property(inputSchema, "max_value_length").get("minimum"));

        SyncToolSpecification duplicateStringBackingArraysSpec = specs.stream()
                .filter(s -> s.tool().name().equals("get_duplicate_string_backing_arrays"))
                .findFirst()
                .orElseThrow();

        inputSchema = duplicateStringBackingArraysSpec.tool().inputSchema();
        assertTrue(required(inputSchema).contains("representative_id"));
        assertEquals(200, property(inputSchema, "max_value_length").get("default"));
        assertEquals(0L, property(inputSchema, "max_value_length").get("minimum"));

        SyncToolSpecification biggestObjectsSpec = specs.stream()
                .filter(s -> s.tool().name().equals("get_biggest_objects"))
                .findFirst()
                .orElseThrow();

        inputSchema = biggestObjectsSpec.tool().inputSchema();
        assertTrue(required(inputSchema).contains("limit"));
        assertEquals(0L, property(inputSchema, "limit").get("minimum"));
    }

    @Test
    void testBackwardCompatibility() {
        ToolsGetter getter = new ToolsGetter(tools);
        assertNotNull(getter.loadHeapTool());
        assertNotNull(getter.getClassesByMaxInstancesCountTool());
        assertNotNull(getter.getClassesByMaxInstancesSizeTool());
        assertNotNull(getter.getBiggestObjectsTool());
        assertNotNull(getter.getGCRootsTool());
        assertNotNull(getter.getGCRootsPaginatedTool());
        assertNotNull(getter.getJavaClassByNameTool());
        assertNotNull(getter.getJavaClassByIdTool());
        assertNotNull(getter.getInstanceByIdTool());
        assertNotNull(getter.getInstanceRetainedSizeTool());
        assertNotNull(getter.getAllReferencesTool());
        assertNotNull(getter.getJavaClassesByRegExpTool());
        assertNotNull(getter.getSummaryTool());
        assertNotNull(getter.getSystemPropertiesTool());
        assertNotNull(getter.executeOqlTool());
        assertNotNull(getter.analyzeHeapTool());
        assertNotNull(getter.searchClassesTool());
        assertNotNull(getter.getDuplicateStringsTool());
        assertNotNull(getter.getDuplicateStringBackingArraysTool());
    }

    @Test
    void gcRootsPaginatedToolRemainsCompatibilityAlias() {
        ToolsGetter getter = new ToolsGetter(tools);
        SyncToolSpecification canonical = getter.getGCRootsTool();
        SyncToolSpecification alias = getter.getGCRootsPaginatedTool();

        assertEquals(canonical.tool().inputSchema(), alias.tool().inputSchema());
        assertTrue(canonical.tool().description().contains("Canonical tool"));
        assertTrue(alias.tool().description().contains("Compatibility alias for get_gc_roots"));
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
}
