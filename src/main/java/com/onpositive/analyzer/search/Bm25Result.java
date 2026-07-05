package com.onpositive.analyzer.search;

import io.swagger.v3.oas.annotations.media.Schema;

public class Bm25Result {

    @Schema(description = "Fully qualified Java class name matched by the BM25 search.")
    private final String className;
    @Schema(description = "BM25 relevance score for this class.")
    private final double score;
    @Schema(description = "Query terms that matched this class, including indexed fields where available.")
    private final String matchedTerms;
    @Schema(description = "Indexed field that contributed the strongest match for this result.")
    private final String topField;
    @Schema(description = "Number of live instances of this class in the heap dump.")
    private long instanceCount;
    @Schema(description = "Total shallow bytes used by all live instances of this class.")
    private long totalSize;
    @Schema(description = "One-based rank of this result in the BM25 result page.")
    private int rank;

    public Bm25Result(String className, double score, String matchedTerms,
                      String topField, long instanceCount, long totalSize) {
        this.className = className;
        this.score = score;
        this.matchedTerms = matchedTerms;
        this.topField = topField;
        this.instanceCount = instanceCount;
        this.totalSize = totalSize;
    }

    public String className() {
        return className;
    }

    public double score() {
        return score;
    }

    public String matchedTerms() {
        return matchedTerms;
    }

    public String topField() {
        return topField;
    }

    public long instanceCount() {
        return instanceCount;
    }

    public long totalSize() {
        return totalSize;
    }

    public int rank() {
        return rank;
    }

    public void setInstanceCount(long instanceCount) {
        this.instanceCount = instanceCount;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "Bm25Result{" +
                "className='" + className + '\'' +
                ", score=" + score +
                ", matchedTerms='" + matchedTerms + '\'' +
                ", topField='" + topField + '\'' +
                ", instanceCount=" + instanceCount +
                ", totalSize=" + totalSize +
                ", rank=" + rank +
                '}';
    }
}
