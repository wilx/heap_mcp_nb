package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class TestMcpTools {

    private final Map<String, SyncToolSpecification> specs;

    private TestMcpTools(HeapDumpTools tools) {
        this.specs = new SyncMcpToolProvider(List.of(tools)).getToolSpecifications().stream()
                .collect(Collectors.toMap(spec -> spec.tool().name(), spec -> spec));
    }

    static TestMcpTools from(HeapDumpTools tools) {
        return new TestMcpTools(tools);
    }

    Map<String, SyncToolSpecification> specifications() {
        return specs;
    }

    SyncToolSpecification specification(String name) {
        SyncToolSpecification spec = specs.get(name);
        if (spec == null) {
            throw new IllegalStateException("Tool not found: " + name);
        }
        return spec;
    }

    McpSchema.CallToolResult call(String name, Map<String, Object> arguments) {
        return call(new McpSchema.CallToolRequest(name, arguments));
    }

    McpSchema.CallToolResult call(McpSchema.CallToolRequest request) {
        return specification(request.name()).callHandler().apply(null, request);
    }
}
