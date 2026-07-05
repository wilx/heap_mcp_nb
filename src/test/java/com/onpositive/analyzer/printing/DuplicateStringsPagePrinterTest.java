package com.onpositive.analyzer.printing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuplicateStringsPagePrinterTest {

    @Test
    void escapesControlCharactersAndQuotes() {
        assertEquals("a\\n\\t\\\"\\\\b", DuplicateStringsPagePrinter.escapedPreview("a\n\t\"\\b", 20));
    }

    @Test
    void truncatesWithoutSplittingSurrogatePairs() {
        assertEquals("a...", DuplicateStringsPagePrinter.escapedPreview("a😀b", 2));
        assertEquals("a😀...", DuplicateStringsPagePrinter.escapedPreview("a😀b", 3));
        assertEquals("...", DuplicateStringsPagePrinter.escapedPreview("value", 0));
    }
}
