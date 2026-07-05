package com.onpositive.analyzer.mcp;

import com.onpositive.analyzer.CliMain;
import com.onpositive.analyzer.HeapDumpService;
import com.onpositive.analyzer.mcp.reflection.ToolsFactory;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public class McpServerLauncher {

    public static McpSyncServer createServer() {
        HeapDumpService heapDumpService = new HeapDumpService();

        // 2. Initialize MCP Adapter Layer
        HeapDumpTools heapDumpTools = new HeapDumpTools(heapDumpService);
        List<McpServerFeatures.SyncToolSpecification> specifications = ToolsFactory.createToolSpecs(heapDumpTools);

        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(
                jsonMapper);

        return McpServer.sync(transportProvider)
                .serverInfo("java-heap-analyzer", BuildInfo.version())
                .validateToolInputs(true)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .logging()
                        .build())
                .tools(
                        specifications.toArray(new McpServerFeatures.SyncToolSpecification[0])
                )
                .instructions("This MCP server is aimed at Java heap dump .hprof file analysis")
                .build();
    }

    public static void main(String[] args) {
        if (args.length > 0 && !args[0].equals("serve")) {
            CliMain.main(args);
        } else {
            createServer();
        }
    }
}
