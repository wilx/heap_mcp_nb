package com.onpositive.analyzer;

import com.onpositive.analyzer.search.Bm25Index;
import com.onpositive.analyzer.search.Bm25Result;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.profiler.heap.Heap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeapDumpServiceSearchPaginationTest {

    @Test
    void laterPageIsNotServedFromUndersizedFirstPageCache() throws Exception {
        Bm25Index index = mock(Bm25Index.class);
        Heap heap = mock(Heap.class);
        List<Bm25Result> allResults = List.of(
                result("First"), result("Second"), result("Third"), result("Fourth"));
        when(index.search("query", 2)).thenReturn(allResults.subList(0, 2));
        when(index.search("query", 4)).thenReturn(allResults);

        HeapDumpService service = new HeapDumpService();
        setField(service, "heap", heap);
        setField(service, "bm25Index", index);

        List<Bm25Result> firstPage = service.searchClassesBm25("query", 2, 0);
        List<Bm25Result> secondPage = service.searchClassesBm25("query", 2, 2);

        assertEquals(List.of("First", "Second"), classNames(firstPage));
        assertEquals(List.of("Third", "Fourth"), classNames(secondPage));
    }

    private static Bm25Result result(String className) {
        return new Bm25Result(className, 1.0, "query", "className", 0, 0);
    }

    private static List<String> classNames(List<Bm25Result> results) {
        return results.stream().map(Bm25Result::className).toList();
    }

    private static void setField(HeapDumpService service, String name, Object value) throws Exception {
        java.lang.reflect.Field field = HeapDumpService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
