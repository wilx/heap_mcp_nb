package com.onpositive.analyzer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OqlQueryRewriterTest {

    private OqlQueryRewriter rewriter;

    @BeforeEach
    void setUp() {
        rewriter = new OqlQueryRewriter();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("rewriteCases")
    void rewrite_shouldProduceExpectedQuery(String input, String expected) {
        assertEquals(expected, rewriter.rewrite(input));
    }

    static Stream<Arguments> rewriteCases() {
        return Stream.of(
                Arguments.of("select * from java.lang.String", "select s from java.lang.String s"),
                Arguments.of("select * from com.example.MyClass", "select m from com.example.MyClass m"),
                Arguments.of("SELECT * FROM java.lang.String", "select s from java.lang.String s"),
                Arguments.of("select s from java.lang.String s", "select s from java.lang.String s"),
                Arguments.of("select * from java.lang.String where s.length > 10",
                        "select * from java.lang.String where s.length > 10"),
                Arguments.of("select  *  from  java.lang.String", "select s from java.lang.String s"),
                Arguments.of("select * from int[]", "select i from int[] i"),
                Arguments.of("select * from java.lang.String[]", "select s from java.lang.String[] s")
        );
    }

    @ParameterizedTest(name = "[{index}] preserves null or blank input")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rewrite_shouldPreserveNullOrBlankInput(String input) {
        assertEquals(input, rewriter.rewrite(input));
    }
}
