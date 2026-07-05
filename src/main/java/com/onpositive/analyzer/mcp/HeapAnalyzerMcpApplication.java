package com.onpositive.analyzer.mcp;

import com.onpositive.analyzer.HeapDumpService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.onpositive.analyzer")
public class HeapAnalyzerMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeapAnalyzerMcpApplication.class, args);
    }

    @Bean
    HeapDumpService heapDumpService() {
        return new HeapDumpService();
    }
}
