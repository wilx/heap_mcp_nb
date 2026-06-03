package com.onpositive.analyzer.search;

import java.util.List;

public interface Bm25Index {

    int getOrCreateDocId(String className);

    void indexTokens(int docId, Bm25Field field, List<String> tokens);

    List<Bm25Result> search(String query, int topN);

    int getDocumentCount();

    String getClassName(int docId);

    boolean isBuilt();
}
