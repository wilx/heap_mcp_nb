package com.onpositive.analyzer.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.onpositive.analyzer")
public class HeapAnalyzerMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeapAnalyzerMcpApplication.class, args);
    }
}
