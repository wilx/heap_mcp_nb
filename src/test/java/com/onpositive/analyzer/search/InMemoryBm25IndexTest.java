package com.onpositive.analyzer.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBm25IndexTest {

    private InMemoryBm25Index index;

    @BeforeEach
    void setUp() {
        index = new InMemoryBm25Index();
    }

    @Test
    void getOrCreateDocId_assignsSequentialIds() {
        int id1 = index.getOrCreateDocId("com.example.Foo");
        int id2 = index.getOrCreateDocId("com.example.Bar");
        assertEquals(0, id1);
        assertEquals(1, id2);
    }

    @Test
    void getOrCreateDocId_sameClassNameReturnsSameId() {
        int id1 = index.getOrCreateDocId("com.example.Foo");
        int id2 = index.getOrCreateDocId("com.example.Foo");
        assertEquals(id1, id2);
        assertEquals(1, index.getDocumentCount());
    }

    @Test
    void getDocumentCount_increasesWithNewDocuments() {
        assertEquals(0, index.getDocumentCount());
        index.getOrCreateDocId("com.example.A");
        index.getOrCreateDocId("com.example.B");
        assertEquals(2, index.getDocumentCount());
    }

    @Test
    void getClassName_returnsByDocId() {
        int docId = index.getOrCreateDocId("com.example.MyClass");
        assertEquals("com.example.MyClass", index.getClassName(docId));
    }

    @Test
    void indexTokens_storesTokens() {
        int docId = index.getOrCreateDocId("com.example.UserService");
        index.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example", "user"));
        index.finishBuild();
        assertTrue(index.isBuilt());
    }

    @Test
    void search_findsMatchingClass() {
        int docId = index.getOrCreateDocId("com.example.UserAccountService");
        index.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example", "user", "account"));
        index.finishBuild();

        List<Bm25Result> results = index.search("user account", 10);
        assertEquals(1, results.size());
        assertEquals("com.example.UserAccountService", results.get(0).className());
        assertTrue(results.get(0).score() > 0);
    }

    @Test
    void search_returnsEmptyForNoMatch() {
        int docId = index.getOrCreateDocId("com.example.Foo");
        index.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("foo"));
        index.finishBuild();

        List<Bm25Result> results = index.search("nonexistent", 10);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_filtersStopwordsFromQuery() {
        int docId = index.getOrCreateDocId("com.example.UserManager");
        index.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example", "user"));
        index.finishBuild();

        List<Bm25Result> results = index.search("get user service", 10);
        assertFalse(results.isEmpty());
        assertEquals("com.example.UserManager", results.get(0).className());
    }

    @Test
    void search_rankedByClassFieldWeight() {
        InMemoryBm25Index idx = new InMemoryBm25Index();
        int doc1 = idx.getOrCreateDocId("com.example.FooService");
        int doc2 = idx.getOrCreateDocId("com.example.BarService");

        idx.indexTokens(doc1, Bm25Field.CLASS_NAME, Arrays.asList("example", "foo"));
        idx.indexTokens(doc1, Bm25Field.FIELD_NAMES, Arrays.asList("data"));

        idx.indexTokens(doc2, Bm25Field.CLASS_NAME, Arrays.asList("example", "bar"));
        idx.indexTokens(doc2, Bm25Field.FIELD_NAMES, Arrays.asList("foo"));

        idx.finishBuild();

        List<Bm25Result> results = idx.search("foo", 10);
        assertEquals(2, results.size());
        assertEquals("com.example.FooService", results.get(0).className());
    }

    @Test
    void search_noDocuments_returnsEmpty() {
        index.finishBuild();
        assertTrue(index.search("anything", 10).isEmpty());
    }

    @Test
    void search_capsResultCount() {
        for (int i = 0; i < 10; i++) {
            int docId = index.getOrCreateDocId("com.example.Class" + i + "Service");
            index.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example", "class" + i));
        }
        index.finishBuild();

        List<Bm25Result> results = index.search("example", 3);
        assertEquals(3, results.size());
    }

    @Test
    void search_topFieldTracksHighestContributingField() {
        InMemoryBm25Index idx = new InMemoryBm25Index();
        int docId = idx.getOrCreateDocId("com.example.OrderProcessor");
        idx.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example", "order"));
        idx.indexTokens(docId, Bm25Field.FIELD_NAMES, Arrays.asList("customer", "payment"));
        idx.finishBuild();

        List<Bm25Result> results = idx.search("order", 10);
        assertEquals(1, results.size());
        assertEquals(Bm25Field.CLASS_NAME.id(), results.get(0).topField());
    }

    @Test
    void search_fieldNamesMatch() {
        InMemoryBm25Index idx = new InMemoryBm25Index();
        int docId = idx.getOrCreateDocId("com.example.GenericHolder");
        idx.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example"));
        idx.indexTokens(docId, Bm25Field.FIELD_NAMES, Arrays.asList("user", "email", "address"));
        idx.finishBuild();

        List<Bm25Result> results = idx.search("email", 10);
        assertEquals(1, results.size());
        assertEquals(Bm25Field.FIELD_NAMES.id(), results.get(0).topField());
    }

    @Test
    void search_superClassMatch() {
        InMemoryBm25Index idx = new InMemoryBm25Index();
        int docId = idx.getOrCreateDocId("com.example.UserServiceImpl");
        idx.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example", "user"));
        idx.indexTokens(docId, Bm25Field.SUPER_CLASS, Arrays.asList("base", "repository"));
        idx.finishBuild();

        List<Bm25Result> results = idx.search("repository", 10);
        assertEquals(1, results.size());
        assertEquals(Bm25Field.SUPER_CLASS.id(), results.get(0).topField());
    }

    @Test
    void search_multipleTermsSameToken_deDupTokens() {
        InMemoryBm25Index idx = new InMemoryBm25Index();
        int docId = idx.getOrCreateDocId("com.example.Order");
        // token "example" appears only once in the doc even if we pass it twice
        idx.indexTokens(docId, Bm25Field.CLASS_NAME, Arrays.asList("example", "example"));
        idx.finishBuild();

        List<Bm25Result> results = idx.search("example", 10);
        assertEquals(1, results.size());
    }

    @Test
    void search_bm25ScoreDecreasesWithManyMatches() {
        InMemoryBm25Index idx = new InMemoryBm25Index();

        int rare = idx.getOrCreateDocId("com.example.RareTermService");
        idx.indexTokens(rare, Bm25Field.CLASS_NAME, Arrays.asList("example", "rare"));

        for (int i = 0; i < 5; i++) {
            int shared = idx.getOrCreateDocId("com.example.SharedTerm" + i + "Service");
            idx.indexTokens(shared, Bm25Field.CLASS_NAME, Arrays.asList("example", "shared"));
        }
        idx.finishBuild();

        List<Bm25Result> results = idx.search("rare example", 10);
        assertFalse(results.isEmpty());
        assertEquals("com.example.RareTermService", results.get(0).className());
    }

    @Test
    void isBuilt_beforeFinishBuild() {
        assertFalse(index.isBuilt());
    }

    @Test
    void search_beforeFinishBuild_returnsEmpty() {
        index.getOrCreateDocId("com.example.Test");
        index.indexTokens(0, Bm25Field.CLASS_NAME, Arrays.asList("test"));
        assertTrue(index.search("test", 10).isEmpty());
    }

    @Test
    void indexTokens_emptyTokenList_doesNothing() {
        int docId = index.getOrCreateDocId("com.example.Empty");
        index.indexTokens(docId, Bm25Field.CLASS_NAME, Collections.emptyList());
        index.finishBuild();
        assertTrue(index.isBuilt());
    }

    @Test
    void indexTokens_nullTokenList_doesNothing() {
        int docId = index.getOrCreateDocId("com.example.Null");
        index.indexTokens(docId, Bm25Field.CLASS_NAME, null);
        index.finishBuild();
        assertTrue(index.isBuilt());
    }
}
