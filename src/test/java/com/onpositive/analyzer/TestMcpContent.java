package com.onpositive.analyzer;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class TestMcpContent {

    private TestMcpContent() {
    }

    static String text(McpSchema.CallToolResult result) {
        assertFalse(result.content().isEmpty(), "Expected text content");
        return assertInstanceOf(McpSchema.TextContent.class, result.content().get(0)).text();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(McpSchema.CallToolResult result) {
        assertNotNull(result.structuredContent(), "Expected structured content");
        return assertInstanceOf(Map.class, result.structuredContent());
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> list(McpSchema.CallToolResult result) {
        assertNotNull(result.structuredContent(), "Expected structured content");
        return (List<Map<String, Object>>) assertInstanceOf(List.class, result.structuredContent());
    }
}
