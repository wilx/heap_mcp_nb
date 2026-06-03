package com.onpositive.analyzer.search;

@FunctionalInterface
public interface ClassSkippedPredicate {

    boolean shouldSkip(String fullyQualifiedClassName);
}
