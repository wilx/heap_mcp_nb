package com.onpositive.analyzer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OqlQueryRewriterTest {

    private OqlQueryRewriter rewriter;

    @BeforeEach
    void setUp() {
        rewriter = new OqlQueryRewriter();
    }

    @Test
    void rewrite_shouldConvertSelectStarFromToSelectWithAlias() {
        String input = "select * from java.lang.String";
        String expected = "select s from java.lang.String s";
        assertEquals(expected, rewriter.rewrite(input));
    }

    @Test
    void rewrite_shouldHandleQualifiedClassNames() {
        String input = "select * from com.example.MyClass";
        String expected = "select m from com.example.MyClass m";
        assertEquals(expected, rewriter.rewrite(input));
    }

    @Test
    void rewrite_shouldHandleCaseInsensitiveSelect() {
        String input = "SELECT * FROM java.lang.String";
        String expected = "select s from java.lang.String s";
        assertEquals(expected, rewriter.rewrite(input));
    }

    @Test
    void rewrite_shouldPreserveAlreadyCorrectQuery() {
        String input = "select s from java.lang.String s";
        assertEquals(input, rewriter.rewrite(input));
    }

    @Test
    void rewrite_shouldPreserveQueryWithWhereClause() {
        String input = "select * from java.lang.String where s.length > 10";
        assertEquals(input, rewriter.rewrite(input));
    }

    @Test
    void rewrite_shouldHandleQueryWithExtraWhitespace() {
        String input = "select  *  from  java.lang.String";
        String expected = "select s from java.lang.String s";
        assertEquals(expected, rewriter.rewrite(input));
    }

    @Test
    void rewrite_shouldReturnSameForNull() {
        assertNull(rewriter.rewrite(null));
    }

    @Test
    void rewrite_shouldReturnSameForEmpty() {
        assertEquals("", rewriter.rewrite(""));
    }

    @Test
    void rewrite_shouldReturnSameForBlank() {
        assertEquals("   ", rewriter.rewrite("   "));
    }

    @Test
    void rewrite_shouldHandlePrimitiveArray() {
        String input = "select * from int[]";
        String expected = "select i from int[] i";
        assertEquals(expected, rewriter.rewrite(input));
    }

    @Test
    void rewrite_shouldHandleObjectArray() {
        String input = "select * from java.lang.String[]";
        String expected = "select s from java.lang.String[] s";
        assertEquals(expected, rewriter.rewrite(input));
    }
}
