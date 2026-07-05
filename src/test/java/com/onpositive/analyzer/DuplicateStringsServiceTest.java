package com.onpositive.analyzer;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateStringsServiceTest {

    private static final String SAMPLE = new File("src/test/resources/HeapDumpSample.hprof").getAbsolutePath();

    @Test
    void sortsDuplicateGroupsByRequestedMetric() throws Exception {
        HeapDumpService service = new HeapDumpService();
        service.loadHeap(SAMPLE);

        HeapDumpService.DuplicateStringsPage byCount =
                service.getDuplicateStrings("duplicate_count", 0, 100, 200);
        HeapDumpService.DuplicateStringsPage byBytes =
                service.getDuplicateStrings("total_bytes", 0, 100, 200);

        assertFalse(byCount.items.isEmpty());
        assertEquals(byCount.totalGroups, byBytes.totalGroups);
        for (int i = 1; i < byCount.items.size(); i++) {
            assertTrue(byCount.items.get(i - 1).duplicateCount >= byCount.items.get(i).duplicateCount);
        }
        for (int i = 1; i < byBytes.items.size(); i++) {
            assertTrue(byBytes.items.get(i - 1).totalShallowBytes >= byBytes.items.get(i).totalShallowBytes);
        }
        for (HeapDumpService.DuplicateStringStats stats : byCount.items) {
            assertEquals(stats.occurrenceCount - 1, stats.duplicateCount);
            assertEquals(stats.stringShallowBytes + stats.backingArrayShallowBytes,
                    stats.totalShallowBytes);
            assertTrue(stats.distinctBackingArrayCount >= 1);
        }
    }

    @Test
    void rejectsInvalidArguments() throws Exception {
        HeapDumpService service = new HeapDumpService();
        assertThrows(IllegalStateException.class,
                () -> service.getDuplicateStrings("total_bytes", 0, 1, 20));
        service.loadHeap(SAMPLE);
        assertThrows(IllegalArgumentException.class,
                () -> service.getDuplicateStrings("unknown", 0, 1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> service.getDuplicateStrings("total_bytes", 2, 1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> service.getDuplicateStrings("total_bytes", 0, 1, -1));
    }

    @Test
    void reloadInvalidatesDuplicateAnalysis() throws Exception {
        HeapDumpService service = new HeapDumpService();
        service.loadHeap(SAMPLE);
        HeapDumpService.DuplicateStringStats first =
                service.getDuplicateStrings("duplicate_count", 0, 1, 20).items.getFirst();

        service.loadHeap(SAMPLE);
        HeapDumpService.DuplicateStringStats afterReload =
                service.getDuplicateStrings("duplicate_count", 0, 1, 20).items.getFirst();

        assertNotSame(first, afterReload);
        assertEquals(first.value, afterReload.value);
        assertEquals(first.occurrenceCount, afterReload.occurrenceCount);
    }
}
