package com.onpositive.analyzer;

import com.onpositive.analyzer.mcp.HeapDumpTools;
import com.onpositive.analyzer.mcp.reflection.ToolInvoker;
import com.onpositive.analyzer.mcp.reflection.ToolInvoker.ToolInfo;

import java.util.*;

public final class CliMain {

    private CliMain() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit(null);
        }

        String command = args[0].toLowerCase();
        boolean helpMode = false;
        if (command.equals("help") || command.equals("-h") || command.equals("--help")) {
            if (args.length > 1) {
                command = args[1].toLowerCase();
                helpMode = true;
            } else {
                printUsageAndExit(null);
            }
        }

        HeapDumpService service = new HeapDumpService();
        HeapDumpTools tools = new HeapDumpTools(service);

        List<ToolInfo> allTools = ToolInvoker.listTools(tools);

        if (command.equals("tools") || command.equals("list")) {
            System.out.println("Available tools:");
            System.out.println("----------------");
            String format = "  %-35s %s%n";
            for (ToolInfo t : allTools) {
                System.out.printf(format, t.name, t.title);
            }
            System.out.println();
            System.out.println("Usage: java -jar heap_mcp_nb.jar <tool-name> [param1=value1 ...]");
            System.out.println("       java -jar heap_mcp_nb.jar help <tool-name>");
            return;
        }

        ToolInfo target = null;
        for (ToolInfo t : allTools) {
            if (t.name.equals(command)) {
                target = t;
                break;
            }
        }

        if (target == null) {
            System.err.println("Unknown tool: " + command);
            printUsageAndExit(command);
        }

        if (helpMode) {
            printToolHelp(target);
            return;
        }

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-h") || arg.equals("--help")) {
                printToolHelp(target);
                return;
            }
            int eq = arg.indexOf('=');
            if (eq > 0) {
                params.put(arg.substring(0, eq), arg.substring(eq + 1));
            } else if (eq == 0) {
                // skip
            } else {
                params.put(arg, "");
            }
        }

        try {
            Object[] methodArgs = ToolInvoker.prepareArgs(target.method, params);
            String result = ToolInvoker.invokeAndFormat(target.method, tools, methodArgs);
            System.out.println(result);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printToolHelp(target);
            System.exit(1);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String msg = cause != null && cause.getMessage() != null
                    ? cause.getMessage() : e.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = cause != null ? cause.getClass().getSimpleName() : e.getClass().getSimpleName();
            }
            System.err.println("Error: " + msg);
            System.exit(1);
        }
    }

    private static void printUsageAndExit(String unknown) {
        if (unknown != null && !unknown.isEmpty()) {
            System.err.println("Unknown command: " + unknown);
            System.err.println();
        }
        System.out.println("Usage: java -jar heap_mcp_nb.jar <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  tools | list          List all available tools");
        System.out.println("  <tool-name>           Execute a tool (params as key=value)");
        System.out.println("  help | -h | --help    Show this help");
        System.out.println("  help <tool-name>      Show tool-specific help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar heap_mcp_nb.jar load_heap file_path=/path/to/dump.hprof");
        System.out.println("  java -jar heap_mcp_nb.jar search_classes query=user top_n=10");
        System.out.println("  java -jar heap_mcp_nb.jar get_summary");
        System.exit(unknown != null ? 1 : 0);
    }

    private static void printToolHelp(ToolInfo tool) {
        System.out.println("Tool: " + tool.name);
        System.out.println("  " + tool.title);
        System.out.println();
        System.out.println("  " + tool.description);
        System.out.println();

        java.lang.reflect.Method method = tool.method;
        java.lang.annotation.Annotation[][] paramAnnots = method.getParameterAnnotations();
        Class<?>[] paramTypes = method.getParameterTypes();

        boolean hasParams = false;
        for (int i = 0; i < paramAnnots.length; i++) {
            for (java.lang.annotation.Annotation a : paramAnnots[i]) {
                if (a instanceof com.onpositive.analyzer.mcp.reflection.Required) {
                    String name = ((com.onpositive.analyzer.mcp.reflection.Required) a).value();
                    System.out.printf("  %s=<%s>  (required)%n", name, paramTypes[i].getSimpleName().toLowerCase());
                    hasParams = true;
                } else if (a instanceof com.onpositive.analyzer.mcp.reflection.Default) {
                    com.onpositive.analyzer.mcp.reflection.Default d =
                            (com.onpositive.analyzer.mcp.reflection.Default) a;
                    System.out.printf("  %s=<%s>  (default: %s)%n", d.name(),
                            paramTypes[i].getSimpleName().toLowerCase(), d.value());
                    hasParams = true;
                }
            }
        }
        if (!hasParams) {
            System.out.println("  (no parameters)");
        }
        System.out.println();
        System.out.println("Usage: java -jar heap_mcp_nb.jar " + tool.name
                + " [param1=value1 ...]");
    }
}
