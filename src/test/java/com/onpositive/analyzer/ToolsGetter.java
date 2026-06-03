package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;

public class ToolsGetter {

    private final HeapDumpTools tools;

    public ToolsGetter(HeapDumpTools tools) {
        this.tools = tools;
    }

    public List<McpServerFeatures.SyncToolSpecification> getSpecifications() {
        return com.onpositive.analyzer.mcp.reflection.ToolsFactory.createToolSpecs(tools);
    }

    public McpServerFeatures.SyncToolSpecification loadHeapTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("load_heap"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: load_heap"));
    }

    public McpServerFeatures.SyncToolSpecification getClassesByMaxInstancesCountTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_classes_by_max_instances_count"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_classes_by_max_instances_count"));
    }

    public McpServerFeatures.SyncToolSpecification getClassesByMaxInstancesSizeTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_classes_by_max_instances_size"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_classes_by_max_instances_size"));
    }

    public McpServerFeatures.SyncToolSpecification getBiggestObjectsTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_biggest_objects"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_biggest_objects"));
    }

    public McpServerFeatures.SyncToolSpecification getGCRootsTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_gc_roots"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_gc_roots"));
    }

    public McpServerFeatures.SyncToolSpecification getGCRootsPaginatedTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_gc_roots_paginated"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_gc_roots_paginated"));
    }

    public McpServerFeatures.SyncToolSpecification getJavaClassByNameTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_class_by_name"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_class_by_name"));
    }

    public McpServerFeatures.SyncToolSpecification getJavaClassByIdTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_class_by_id"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_class_by_id"));
    }

    public McpServerFeatures.SyncToolSpecification getInstanceByIdTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_instance_by_id"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_instance_by_id"));
    }

    public McpServerFeatures.SyncToolSpecification getAllReferencesTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_all_references"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_all_references"));
    }

    public McpServerFeatures.SyncToolSpecification getJavaClassesByRegExpTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_classes_by_regexp"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_classes_by_regexp"));
    }

    public McpServerFeatures.SyncToolSpecification getSummaryTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_summary"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_summary"));
    }

    public McpServerFeatures.SyncToolSpecification getSystemPropertiesTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("get_system_properties"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: get_system_properties"));
    }

    public McpServerFeatures.SyncToolSpecification executeOqlTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("execute_oql"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: execute_oql"));
    }

    public McpServerFeatures.SyncToolSpecification analyzeHeapTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("analyze_heap_dump"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: analyze_heap_dump"));
    }

    public McpServerFeatures.SyncToolSpecification searchClassesTool() {
        List<McpServerFeatures.SyncToolSpecification> specs = getSpecifications();
        return specs.stream()
                .filter(s -> s.tool().name().equals("search_classes"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool not found: search_classes"));
    }

}
