package com.onpositive.analyzer.mcp;

import com.onpositive.analyzer.HeapDumpService;
import com.onpositive.analyzer.OqlQueryExecutor;
import com.onpositive.analyzer.search.Bm25Result;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HeapDumpTools {

    private final HeapDumpService heapDumpService;

    public HeapDumpTools(HeapDumpService heapDumpService) {
        this.heapDumpService = heapDumpService;
    }

    @McpTool(name = "load_heap", title = "Load Heap Dump", description = "Loads a .hprof heap dump file and returns its summary.", generateOutputSchema = true)
    public McpDtos.HeapSummaryDto loadHeap(
        @McpToolParam(required = true, description = "Path to the .hprof heap dump file.") String file_path) throws Exception {
        return McpDtos.HeapSummaryDto.from(heapDumpService.loadHeap(file_path));
    }

    @McpTool(name = "get_classes_by_max_instances_count", title = "Get Classes By Max Instances Count", description = "Returns a sorted list of classes by instance count (descending) with pagination.", generateOutputSchema = true)
    public List<HeapDumpService.ClassStats> getClassesByMaxInstancesCount(
        @McpToolParam(required = false, description = "Zero-based start offset, inclusive.") @Schema(defaultValue = "0", minimum = "0") Integer from,
        @McpToolParam(required = false, description = "Zero-based end offset, exclusive.") @Schema(defaultValue = "50", minimum = "0") Integer to) {
        return heapDumpService.getClassesByMaxInstancesCount(defaultInt(from, 0), defaultInt(to, 50));
    }

    @McpTool(name = "get_classes_by_max_instances_size", title = "Get Classes By Max Instances Size", description = "Returns a sorted list of classes by total instance size (descending) with pagination.", generateOutputSchema = true)
    public List<HeapDumpService.ClassStats> getClassesByMaxInstancesSize(
        @McpToolParam(required = false, description = "Zero-based start offset, inclusive.") @Schema(defaultValue = "0", minimum = "0") Integer from,
        @McpToolParam(required = false, description = "Zero-based end offset, exclusive.") @Schema(defaultValue = "50", minimum = "0") Integer to) {
        return heapDumpService.getClassesByMaxInstancesSize(defaultInt(from, 0), defaultInt(to, 50));
    }

    @McpTool(name = "get_biggest_objects", title = "Get Biggest Objects", description = "Returns the biggest objects by retained size.", generateOutputSchema = true)
    public List<McpDtos.RetainedInstanceDto> getBiggestObjects(
        @McpToolParam(required = true, description = "Maximum number of retained-size-ranked objects to return.") @Schema(minimum = "0") int limit) {
        return heapDumpService.getBiggestObjectsByRetainedSize(limit).stream()
            .map(McpDtos.RetainedInstanceDto::from)
            .toList();
    }

    @McpTool(name = "get_gc_roots", title = "Get GC Roots", description = "Returns GC roots with pagination, including kind and instance information.", generateOutputSchema = true)
    public List<HeapDumpService.GCRootInfo> getGCRoots(
        @McpToolParam(required = false, description = "Zero-based start offset, inclusive.") @Schema(defaultValue = "0", minimum = "0") Integer from,
        @McpToolParam(required = false, description = "Zero-based end offset, exclusive.") @Schema(defaultValue = "50", minimum = "0") Integer to) {
        return heapDumpService.getGCRootsPaginated(defaultInt(from, 0), defaultInt(to, 50));
    }

    @McpTool(name = "get_class_by_name", title = "Get Class By Name", description = "Returns class details by its full name.", generateOutputSchema = true)
    public McpDtos.JavaClassDto getJavaClassByName(
        @McpToolParam(required = true, description = "Fully qualified Java class name.") String name) {
        return McpDtos.JavaClassDto.from(heapDumpService.getJavaClassByName(name));
    }

    @McpTool(name = "get_class_by_id", title = "Get Class By ID", description = "Returns class details by its internal ID.", generateOutputSchema = true)
    public McpDtos.JavaClassDto getJavaClassById(
        @McpToolParam(required = true, description = "Internal Java class ID, decimal or hexadecimal.") String id) {
        return McpDtos.JavaClassDto.from(heapDumpService.getJavaClassById(parseId(id)));
    }

    @McpTool(name = "get_instance_by_id", title = "Get Instance By ID", description = "Returns instance details by its internal ID, including class, shallow size, and field values.", generateOutputSchema = true)
    public McpDtos.InstanceDto getInstanceById(
        @McpToolParam(required = true, description = "Internal instance ID, decimal or hexadecimal.") String id) {
        return McpDtos.InstanceDto.from(
            heapDumpService.getInstanceById(parseId(id)),
            heapDumpService.getUtf16ByteOrder());
    }

    @McpTool(name = "get_instance_retained_size", title = "Get Instance Retained Size", description = "Returns the retained size of an instance by its ID. Computing retained size can be costly and may fail for complex heap graphs.", generateOutputSchema = true)
    public McpDtos.InstanceRetainedSizeDto getInstanceRetainedSize(
        @McpToolParam(required = true, description = "Internal instance ID, decimal or hexadecimal.") String id) {
        long instanceId = parseId(id);
        long retainedSize = heapDumpService.getInstanceRetainedSize(instanceId);
        return new McpDtos.InstanceRetainedSizeDto(instanceId, retainedSize);
    }

    @McpTool(name = "get_all_references", title = "Get All References", description = "Returns incoming field and array-element references to an instance with pagination. Each result includes Via with the referring field name or array index.", generateOutputSchema = true)
    public List<HeapDumpService.ReferenceInfo> getAllReferences(
        @McpToolParam(required = true, description = "Target internal instance ID, decimal or hexadecimal.") String id,
        @McpToolParam(required = false, description = "Zero-based start offset, inclusive.") @Schema(defaultValue = "0", minimum = "0") Integer from,
        @McpToolParam(required = false, description = "Zero-based end offset, exclusive.") @Schema(defaultValue = "50", minimum = "0") Integer to) {
        return heapDumpService.getAllReferences(parseId(id), defaultInt(from, 0), defaultInt(to, 50));
    }

    private static long parseId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return 0L;
        }
        id = id.trim();
        try {
            return Long.decode(id);
        }
        catch (NumberFormatException e1) {
            try {
                return Long.parseLong(id, 16);
            }
            catch (NumberFormatException e2) {
                throw new IllegalArgumentException("Cannot parse ID: " + id);
            }
        }
    }

    @McpTool(name = "get_classes_by_regexp", title = "Get Classes By RegExp", description = "Returns classes matching the regular expression with pagination.", generateOutputSchema = true)
    public List<McpDtos.JavaClassDto> getJavaClassesByRegExp(
        @McpToolParam(required = true, description = "Regular expression matched against Java class names.") String regexp,
        @McpToolParam(required = false, description = "Zero-based start offset, inclusive.") @Schema(defaultValue = "0", minimum = "0") Integer from,
        @McpToolParam(required = false, description = "Zero-based end offset, exclusive.") @Schema(defaultValue = "50", minimum = "0") Integer to) {
        return heapDumpService.getJavaClassesByRegExpPaginated(regexp, defaultInt(from, 0), defaultInt(to, 50)).stream()
            .map(McpDtos.JavaClassDto::summary)
            .toList();
    }

    @McpTool(name = "get_instances", title = "Get Instances", description = "Returns instances of a class by class name with pagination, 10 per page.", generateOutputSchema = true)
    public McpDtos.InstancePageDto getInstances(
        @McpToolParam(required = true, description = "Fully qualified Java class name whose instances should be listed.") String class_name,
        @McpToolParam(required = false, description = "Zero-based start offset, inclusive.") @Schema(defaultValue = "0", minimum = "0") Integer from,
        @McpToolParam(required = false, description = "Zero-based end offset, exclusive.") @Schema(defaultValue = "10", minimum = "0") Integer to) {
        return McpDtos.InstancePageDto.from(
            heapDumpService.getInstancesByClassName(class_name, defaultInt(from, 0), defaultInt(to, 10)));
    }

    @McpTool(name = "get_summary", title = "Get Heap Summary", description = "Returns the summary of the loaded heap.", generateOutputSchema = true)
    public McpDtos.HeapSummaryDto getSummary() {
        return McpDtos.HeapSummaryDto.from(heapDumpService.getSummary());
    }

    @McpTool(name = "get_duplicate_strings", title = "Get Duplicate Strings", description = "Returns duplicate java.lang.String values with occurrence counts and associated shallow String/backing-array footprint. Sort by total_bytes or duplicate_count.", generateOutputSchema = true)
    public HeapDumpService.DuplicateStringsPage getDuplicateStrings(
        @McpToolParam(required = false, description = "Duplicate-string sort order.") @Schema(defaultValue = "total_bytes", allowableValues = { "total_bytes", "duplicate_count" }) String sort_by,
        @McpToolParam(required = false, description = "Zero-based start offset, inclusive.") @Schema(defaultValue = "0", minimum = "0") Integer from,
        @McpToolParam(required = false, description = "Zero-based end offset, exclusive.") @Schema(defaultValue = "50", minimum = "0") Integer to,
        @McpToolParam(required = false, description = "Maximum number of characters to show for each duplicate value.") @Schema(defaultValue = "200", minimum = "0") Integer max_value_length) {
        return heapDumpService.getDuplicateStrings(defaultString(sort_by, "total_bytes"), defaultInt(from, 0),
            defaultInt(to, 50), defaultInt(max_value_length, 200));
    }

    @McpTool(name = "get_duplicate_string_backing_arrays", title = "Get Duplicate String Backing Arrays", description = "Returns all unique backing arrays for a duplicate string group identified by representative_id, including referencing String instance IDs.", generateOutputSchema = true)
    public HeapDumpService.DuplicateStringBackingArrays getDuplicateStringBackingArrays(
        @McpToolParam(required = true, description = "Representative java.lang.String instance ID from get_duplicate_strings, decimal or hexadecimal.") String representative_id,
        @McpToolParam(required = false, description = "Maximum number of characters to show for the duplicate value.") @Schema(defaultValue = "200", minimum = "0") Integer max_value_length) {
        return heapDumpService.getDuplicateStringBackingArrays(parseId(representative_id),
            defaultInt(max_value_length, 200));
    }

    @McpTool(name = "get_system_properties", title = "Get System Properties", description = "Returns system properties from the heap dump.", generateOutputSchema = true)
    public java.util.Properties getSystemProperties() {
        return heapDumpService.getSystemProperties();
    }

    @McpTool(name = "execute_oql", title = "Execute OQL Query", description = "Executes an OQL query on the heap dump. Query syntax example: 'select s.value from java.lang.String s'", generateOutputSchema = true)
    public OqlQueryExecutor.OqlResult executeOql(
        @McpToolParam(required = true, description = "OQL query to execute against the loaded heap.") String query,
        @McpToolParam(required = false, description = "Maximum number of OQL rows to return.") @Schema(defaultValue = "100", minimum = "0") Integer max_results) throws Exception {
        return heapDumpService.executeOql(query, defaultInt(max_results, 100));
    }

    @McpTool(name = "search_classes", title = "Search Classes (BM25)", description = "Searches heap dump classes using BM25 full-text ranking on class names, superclass, field names and field types. Splits CamelCase and snake_case, filters stopwords.", generateOutputSchema = true)
    public List<Bm25Result> searchClasses(
        @McpToolParam(required = true, description = "Search query for class names, fields, and superclass names.") String query,
        @McpToolParam(required = false, description = "Number of BM25 results to return.") @Schema(defaultValue = "25", minimum = "0") Integer top_n,
        @McpToolParam(required = false, description = "Zero-based result offset.") @Schema(defaultValue = "0", minimum = "0") Integer from) {
        return heapDumpService.searchClassesBm25(query, defaultInt(top_n, 25), defaultInt(from, 0));
    }

    @McpTool(name = "analyze_heap_dump", title = "Analyze Heap Dump", description = "Parses a .hprof heap dump file and returns the top classes by instance count.")
    public String analyzeHeap(
        @McpToolParam(required = true, description = "Path to the .hprof heap dump file.") String file_path,
        @McpToolParam(required = false, description = "Maximum number of top classes to return.") @Schema(defaultValue = "10", minimum = "0") Integer limit) throws Exception {
        List<HeapDumpService.ClassStats> stats = heapDumpService.getTopClasses(file_path, defaultInt(limit, 10));
        StringBuilder sb = new StringBuilder();
        sb.append("Top ").append(stats.size()).append(" Classes in Heap Dump:\n");
        sb.append(String.format("%-50s | %-10s | %-10s%n", "Class Name", "Count", "Size"));
        sb.append("-".repeat(75)).append("\n");
        for (HeapDumpService.ClassStats stat : stats) {
            sb.append(String.format("%-50s | %-10d | %-10d%n",
                truncate(stat.className, 50), stat.instanceCount, stat.size));
        }
        return sb.toString();
    }

    private String truncate(String str, int len) {
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len - 3) + "...";
    }

    private static int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

}
