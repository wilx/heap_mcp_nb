package com.onpositive.analyzer.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassNameTokenizerTest {

    private final ClassNameTokenizer tokenizer = new ClassNameTokenizer();

    @Test
    void tokenizeNull_returnsEmpty() {
        assertTrue(tokenizer.tokenize(null).isEmpty());
    }

    @Test
    void tokenizeEmpty_returnsEmpty() {
        assertTrue(tokenizer.tokenize("").isEmpty());
    }

    @Test
    void camelCase_splitTransitions() {
        List<String> tokens = tokenizer.tokenize("UserAccountServiceImpl");
        assertTrue(tokens.contains("user"));
        assertTrue(tokens.contains("account"));
        assertFalse(tokens.contains("service"));
        assertFalse(tokens.contains("impl"));
    }

    @Test
    void acronymHandling_URLParser() {
        List<String> tokens = tokenizer.tokenize("URLParserFactory");
        assertTrue(tokens.contains("url"));
        assertTrue(tokens.contains("parser"));
        assertFalse(tokens.contains("factory"));
    }

    @Test
    void snakeCase_splitsUnderscore() {
        List<String> tokens = tokenizer.tokenize("MAX_RETRY_COUNT");
        assertTrue(tokens.contains("max"));
        assertTrue(tokens.contains("retry"));
    }

    @Test
    void fullyQualifiedName_splitsByDot() {
        List<String> tokens = tokenizer.tokenize("com.example.service.UserAccountServiceImpl");
        assertTrue(tokens.contains("example"));
        assertTrue(tokens.contains("user"));
        assertTrue(tokens.contains("account"));
        assertFalse(tokens.contains("com"));
        assertFalse(tokens.contains("service"));
    }

    @Test
    void packageNoiseStopwords_removed() {
        List<String> tokens = tokenizer.tokenize("com.org.net.io.User");
        assertFalse(tokens.contains("com"));
        assertFalse(tokens.contains("org"));
        assertFalse(tokens.contains("net"));
        assertFalse(tokens.contains("io"));
        assertTrue(tokens.contains("user"));
    }

    @Test
    void crudVerbStopwords_removed() {
        List<String> tokens = tokenizer.tokenize("CreateGetUpdateDeleteFindSearch");
        assertFalse(tokens.contains("create"));
        assertFalse(tokens.contains("get"));
        assertFalse(tokens.contains("update"));
        assertFalse(tokens.contains("delete"));
        assertFalse(tokens.contains("find"));
        assertFalse(tokens.contains("search"));
    }

    @Test
    void wellKnownTypeStopwords_removed() {
        List<String> tokens = tokenizer.tokenize("StringIntegerBooleanVoid");
        assertFalse(tokens.contains("string"));
        assertFalse(tokens.contains("integer"));
        assertFalse(tokens.contains("boolean"));
        assertFalse(tokens.contains("void"));
    }

    @Test
    void collectionStopwords_removed() {
        List<String> tokens = tokenizer.tokenize("HashMapArrayListLinkedListHashSet");
        assertFalse(tokens.contains("hashmap"));
        assertFalse(tokens.contains("arraylist"));
        assertFalse(tokens.contains("linkedlist"));
        assertFalse(tokens.contains("hashset"));
    }

    @Test
    void singleCharTokens_removed() {
        List<String> tokens = tokenizer.tokenize("a_b_c_X_y_z_ab");
        assertTrue(tokens.stream().noneMatch(t -> t.length() < 2));
        assertTrue(tokens.contains("ab"));
    }

    @Test
    void digitTransitions_handled() {
        List<String> tokens = tokenizer.tokenize("MD5HashProvider");
        assertTrue(tokens.contains("md5"));
        assertTrue(tokens.contains("hash"));
    }

    @Test
    void innerClass_splitByDollar() {
        List<String> tokens = tokenizer.tokenize("OuterClass$InnerClass");
        assertTrue(tokens.contains("outer"));
        assertTrue(tokens.contains("inner"));
    }

    @Test
    void simpleNameToken_afterLastDot() {
        String token = tokenizer.simpleNameToken("com.example.service.UserAccountServiceImpl");
        assertEquals("useraccountserviceimpl", token);
    }

    @Test
    void simpleNameToken_noPackage() {
        String token = tokenizer.simpleNameToken("UserAccountServiceImpl");
        assertEquals("useraccountserviceimpl", token);
    }

    @Test
    void simpleNameToken_null() {
        assertNull(tokenizer.simpleNameToken(null));
    }

    @Test
    void simpleNameToken_empty() {
        assertNull(tokenizer.simpleNameToken(""));
    }

    @Test
    void stopwordDetection_crudVerbs() {
        assertTrue(tokenizer.isStopword("create"));
        assertTrue(tokenizer.isStopword("read"));
        assertTrue(tokenizer.isStopword("update"));
        assertTrue(tokenizer.isStopword("delete"));
        assertTrue(tokenizer.isStopword("save"));
        assertTrue(tokenizer.isStopword("find"));
        assertTrue(tokenizer.isStopword("fetch"));
        assertTrue(tokenizer.isStopword("query"));
        assertTrue(tokenizer.isStopword("insert"));
        assertTrue(tokenizer.isStopword("select"));
        assertTrue(tokenizer.isStopword("get"));
        assertTrue(tokenizer.isStopword("set"));
        assertTrue(tokenizer.isStopword("put"));
        assertTrue(tokenizer.isStopword("add"));
        assertTrue(tokenizer.isStopword("remove"));
    }

    @Test
    void stopwordDetection_applicationTokens_passed() {
        assertFalse(tokenizer.isStopword("user"));
        assertFalse(tokenizer.isStopword("account"));
        assertFalse(tokenizer.isStopword("order"));
        assertFalse(tokenizer.isStopword("payment"));
        assertFalse(tokenizer.isStopword("customer"));
    }

    @Test
    void underscorePrefixedFieldNames_prefixRemoved() {
        List<String> tokens = tokenizer.tokenize("_firstName");
        assertTrue(tokens.contains("first"));
        assertTrue(tokens.contains("name"));
    }

    @Test
    void m_prefixFieldName() {
        List<String> tokens = tokenizer.tokenize("m_userName");
        assertTrue(tokens.contains("user"));
        assertTrue(tokens.contains("name"));
    }
}
