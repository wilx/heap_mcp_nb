package com.onpositive.analyzer;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.File;

class DuplicateStringsMetadataPrintTest {

    @Test
    void printDuplicateStringsToolMetadata() throws Exception {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ServerParameters params = ServerParameters.builder(javaBin)
                .args("-cp", System.getProperty("java.class.path"), "com.onpositive.analyzer.mcp.HeapAnalyzerMcpApplication")
                .build();

        tools.jackson.databind.json.JsonMapper jsonMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        JacksonMcpJsonMapper mapper = new JacksonMcpJsonMapper(jsonMapper);

        try (McpSyncClient client = McpClient.sync(new StdioClientTransport(params, mapper)).build()) {
            client.initialize();
            McpSchema.Tool tool = client.listTools(null).tools().stream()
                    .filter(candidate -> "get_duplicate_strings".equals(candidate.name()))
                    .findFirst()
                    .orElseThrow();

            System.out.println("name:");
            System.out.println(tool.name());
            System.out.println();
            System.out.println("description:");
            System.out.println(tool.description());
            System.out.println();
            System.out.println("inputSchema:");
            System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tool.inputSchema()));
            System.out.println();
            System.out.println("outputSchema:");
            System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tool.outputSchema()));
        }
    }
}
