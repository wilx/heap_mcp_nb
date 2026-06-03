package com.onpositive.analyzer.search;

import com.onpositive.analyzer.printing.Bm25ResultListPrinter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Bm25ResultListPrinterTest {

    private final Bm25ResultListPrinter printer = new Bm25ResultListPrinter();

    @Test
    void print_emptyList() {
        String result = printer.print(Collections.emptyList());
        assertTrue(result.contains("No results found"));
    }

    @Test
    void print_singleResult() {
        List<Bm25Result> results = List.of(
                new Bm25Result("com.example.UserService", 12.5, "user", "className", 42, 8192)
        );
        String result = printer.print(results);
        assertTrue(result.contains("com.example.UserService"));
        assertTrue(result.contains("12"));
        assertTrue(result.contains("className"));
        assertTrue(result.contains("user"));
    }

    @Test
    void print_multipleResults() {
        List<Bm25Result> results = Arrays.asList(
                new Bm25Result("com.example.UserService", 15.0, "user", "className", 100, 20000),
                new Bm25Result("com.example.OrderService", 8.3, "order", "fieldNames", 50, 5000),
                new Bm25Result("com.example.PaymentHandler", 4.1, "payment", "fieldTypes", 10, 1000)
        );
        String result = printer.print(results);
        assertTrue(result.contains("com.example.UserService"));
        assertTrue(result.contains("com.example.OrderService"));
        assertTrue(result.contains("com.example.PaymentHandler"));
        assertTrue(result.contains("15.00"));
        assertTrue(result.contains("8.30"));
        assertTrue(result.contains("4.10"));
    }

    @Test
    void print_nonListInput_returnsEmpty() {
        assertEquals("", printer.print("not a list"));
        assertEquals("", printer.print(null));
    }

    @Test
    void print_longClassName_truncated() {
        String longName = "com.example.service.impl.manager.handler.VeryLongClassNameThatExceedsLimit";
        Bm25Result r = new Bm25Result(longName, 1.0, "test", "className", 1, 100);
        String result = printer.print(List.of(r));
        assertTrue(result.contains("..."));
    }
}
