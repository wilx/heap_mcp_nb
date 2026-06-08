# BM25 Class Index for Heap Dump Analysis

## 1. Overview

A BM25-based search index covering **class definitions** (names, superclass, interfaces) and **field signatures** (field names + field types). Instance data (stored string values) is **not** indexed at this level. The index enables rank-ordered class discovery by tokens extracted from class identity and structure alone.

Each indexed class produces one BM25 document with multiple typed fields. Search hits return ranked class names that can be passed to existing tools (`getJavaClassByName`, `getClassDetails`, etc.).

---

## 2. Scope — What Gets Indexed Per Class

For each non-skipped class, the document is built from:

| Source | Data | Example |
|--------|------|---------|
| `JavaClass.getName()` | Fully qualified name → tokenized | `com.example.service.UserAccountServiceImpl` |
| `JavaClass.getSuperClass().getName()` | Direct superclass FQN (if not null) | `com.example.base.BaseService` |
| `JavaClass.getSubClasses()` + interface detection | Directly implemented interface FQNs | `com.example.spi.AccountService` |
| `JavaClass.getFields()` → `Field.getName()` | All declared field names | `currentUser`, `m_userName` |
| `JavaClass.getFields()` → `Field.getType().getName()` | All declared field type FQNs | `com.example.model.User`, `java.lang.String` |

**Not indexed** (at this level):
- Instance string values
- Array contents
- Static field values
- Instance counts, sizes, GC roots

---

## 3. Skip Rules — Classes NOT Indexed

A class is **skipped entirely** (no document created) if its FQN matches any of these rules.

### 3.1 JDK / JVM Internal

```
java.*
javax.*
sun.*
jdk.*
oracle.*
com.sun.*
netscape.*
```
This includes all of `java.lang.*`, `java.util.*`, `java.io.*`, `java.net.*`, `java.nio.*`, `java.security.*`, `java.sql.*`, `java.rmi.*`, `javax.swing.*`, `javax.management.*`, `javax.xml.*`, etc.

### 3.2 Well-Known Library Root Packages

| Ecosystem | Packages |
|-----------|----------|
| Apache | `org.apache.*` (commons, http, xml, logging, tomcat, etc.) |
| Spring | `org.springframework.*`, `com.springsource.*` |
| Hibernate | `org.hibernate.*` |
| JBoss / WildFly | `org.jboss.*`, `org.wildfly.*` |
| Eclipse | `org.eclipse.*` |
| Google | `com.google.*` (guava, gson, common) |
| FasterXML / Jackson | `com.fasterxml.*` |
| Netty | `io.netty.*` |
| Reactor | `reactor.core.*`, `reactor.netty.*` |
| MicroProfile | `org.eclipse.microprofile.*` |
| Micrometer | `io.micrometer.*` |
| Micronaut | `io.micronaut.*` |
| Quarkus | `io.quarkus.*` |
| Lombok | `lombok.*`, `org.projectlombok.*` |
| ASM / cglib / javassist | `org.objectweb.asm.*`, `org.ow2.asm.*`, `net.sf.cglib.*`, `cglib.*`, `javassist.*`, `org.javassist.*` |
| Logging | `org.slf4j.*`, `ch.qos.logback.*`, `org.apache.logging.*`, `org.apache.log4j.*` |
| Testing | `org.junit.*`, `org.testng.*`, `org.mockito.*`, `org.assertj.*`, `org.hamcrest.*`, `org.jmock.*`, `org.easymock.*` |
| Serialization | `com.thoughtworks.xstream.*`, `org.codehaus.*`, `com.esotericsoftware.*` |
| BouncyCastle | `org.bouncycastle.*` |
| YAML | `org.yaml.*`, `com.fasterxml.jackson.dataformat.yaml.*` |
| Glassfish / Jersey | `org.glassfish.*`, `org.jvnet.*` |
| Undertow | `io.undertow.*` |
| RX | `rx.*` |
| Joda | `org.joda.*` |
| Jackson | `com.fasterxml.jackson.*` |
| Tomcat | `org.apache.tomcat.*`, `org.apache.catalina.*`, `org.apache.coyote.*` |
| Jetty | `org.eclipse.jetty.*` |

### 3.3 Proxy / Generated Classes

Any class whose simple name matches these patterns is skipped:
- `*$Proxy*` — JDK dynamic proxies
- `*$$EnhancerByCGLIB$$*`, `*$$FastClassByCGLIB$$*` — CGLIB proxies
- `*$$_javassist*` — Javassist-generated
- `*_$$_jvst*` — Javassist-generated (alternate naming)
- `*_Stub` — RMI stubs
- `*__Impl` (double underscore)
- `*$HibernateProxy$*`
- Classes matching `^\$+[A-Za-z]` (anonymous proxy naming)

> Application classes that extend or implement library classes are still indexed. Only the library **classes themselves** are skipped.
>
> If the user's application lives under `com.mycompany.*`, only `com.mycompany.*` classes would be indexed by default. The skip-list above catches the overwhelming majority of third-party library classes that would otherwise pollute the index.

### 3.4 Package-Prefix Configuration (Recommended)

Rather than maintaining an exhaustive skip list, the indexer should accept a **list of application package prefixes** (e.g., `["com.mycompany", "org.myapp"]`) and treat **everything else as a library/JDK class** to be skipped. This is simpler and more maintainable. The skip list above serves as the default fallback when no application prefix is provided.

---

## 4. Tokenization

### 4.1 Character Filter

All tokens are:
1. Lowercased
2. Stripped of any character that is not a valid Java identifier part character: `[^a-z0-9_$]` is removed
3. Empty tokens (after stripping) are discarded

Valid Java identifier part characters retained: letters `a-z`, digits `0-9`, underscore `_`, dollar sign `$`.

### 4.2 Name Splitting Pipeline

For each qualified name source (class name, superclass name, interface name, field type name):

1. **Split on package delimiters** `.`
   - `com.example.service.Foo` → [`com`, `example`, `service`, `Foo`]

2. **Split on inner-class delimiter** `$`
   - `Foo$Inner` → [`Foo`, `Inner`] (applied to each segment from step 1)

3. **Split on underscores** (snake_case)
   - `MAX_RETRY_COUNT` → [`MAX`, `RETRY`, `COUNT`] (applied to each segment from step 2)

4. **Split on CamelCase boundaries** (applied to each segment from step 3):
   - Transition `lowercase → UPPERCASE`: `camelCase` → `camel`, `Case`
   - Transition `UPPERCASE → UPPERCASE+lowercase`: `URLParser` → `URL`, `Parser`
   - Transition `digits → letters`: `MD5Hash` → `MD5`, `Hash`
   - Consecutive uppercase letters preceding a lowercase letter are treated as an acronym chunk

   Examples:
   ```
   UserAccountServiceImpl → [user, account, service, impl]
   XMLParserFactory      → [xml, parser, factory]
   OAuth2TokenService    → [o, auth, token, service]
   AbstractBeanFactory   → [abstract, bean, factory]
   ```

5. **Simple class name (full name after last dot)** is also kept as a single token (after step 1):
   - `com.example.service.UserAccountServiceImpl` → token `useraccountserviceimpl`
   - This enables direct search by simple class name without needing CamelCase expansion.

### 4.3 De-duplication

Duplicate tokens from the same field are collapsed (BM25 uses term frequency within a field, not within a token list — a token either appears in the field or it doesn't, for the per-field term frequency calculation).

### 4.4 Standalone Token Extraction for Field Names

Field names are tokenized using the same CamelCase/snake_case splitting (steps 2–4). Underscore-prefixed field names (`_foo`, `m_bar`) lose the prefix character:
- `_firstName` → `first`, `name`
- `m_userName` → `user`, `name`

### 4.5 Token Summary Example

For class `com.example.service.UserAccountServiceImpl`:
- Package-split segments: `com`, `example`, `service`, `UserAccountServiceImpl`
- Simple name token: `useraccountserviceimpl`
- After CamelCase split + stopword filtering: `example`, `user`, `account`

---

## 5. Stopword List

Tokens matching any entry below are removed from the index AND from queries.

### 5.1 Package / Module Noise

```
com       org       net       io
java      javax     jdk       sun
lang      util      utils     impl
internal  api       spi       service
module    package   src       main
test      spec      gen       generated
```

### 5.2 Well-Known Types (Primitives & JDK Core)

```
object    string    class     enum       record
boolean   byte      char      short      int
integer   long      float     double     void
```

### 5.3 Collection / Data Structure Names

```
list        set         map         array
collection  collections iterator   iterable
comparable  comparator  entry
hashmap     hashset     hashtable
arraylist   linkedlist  vector
treemap     treeset
linkedhashmap   linkedhashset
priorityqueue   deque   queue   stack
sortedmap   sortedset
navigablemap    navigableset
enummap     enumset
properties  dictionary
```

### 5.4 CRUD / Bean-Operation Verbs

```
get     set     put     add     remove
delete  create  update  save    find
fetch   query   insert  select  merge
patch   post    list    search  count
exists  has     is      are     was
were    do      does    did     will
would   can     could   shall   should
may     might   must    need    check
```

### 5.5 Common Low-Value Suffixes & Patterns

```
abstract    base      default     simple
generic     common    core
factory     builder   helper      util     utils
manager     provider  handler     listener
adapter     processor mapper      converter
transformer generator parser      renderer
writer      reader    loader      saver
filter      interceptor    aspect advice
proxy       template  delegate    strategy
registry    cache     pool        context
scope       session   transaction
```

### 5.6 Length-Based Filtering

Tokens with length == 1 (single character) are removed after all other stopword filtering.

---

## 6. Index Structure (Per Document)

Each class is a BM25 document with these fields:

| Field ID | Content | Weight | Description |
|----------|---------|--------|-------------|
| `className` | Simple class name token + CamelCase-split tokens from the class's own FQN, minus stopwords | 5.0 | Match on the class's own name |
| `superClass` | Simple name token + CamelCase-split tokens from the direct superclass FQN, minus stopwords | 2.0 | Class hierarchy context — parent class |
| `interfaces` | Simple name tokens + CamelCase-split tokens from implemented interface FQNs, minus stopwords | 2.0 | Class hierarchy context — interfaces |
| `fieldNames` | CamelCase/snake_case tokens from all declared field names, minus stopwords | 3.0 | Structural search — find classes by field naming |
| `fieldTypes` | Simple name tokens + CamelCase-split tokens from all declared field type FQNs, minus stopwords | 3.0 | Structural search — find classes by field types |

### 6.1 Document Length Normalization

`avgdl` is computed per field (not globally). Shorter fields get higher per-match scores, which is desirable:
- `className` is typically short (2–8 tokens) → a match here is very significant
- `fieldTypes` can be long (10+ tokens) → a match here needs the IDF boost

---

## 7. BM25 Configuration

**Formula** (standard OKAPI BM25):

```
score(D, Q) = Σ over matched fields f [ weight_f * Σ over query terms t IDF(t) * TF(t, D_f) ]

where:
  TF(t, D_f) = (f(t, D_f) * (k1 + 1)) / (f(t, D_f) + k1 * (1 - b + b * |D_f| / avgdl_f))
  IDF(t) = ln(1 + (N - n(t) + 0.5) / (n(t) + 0.5))
```

| Parameter | Value | Notes |
|-----------|-------|-------|
| `k1` | 1.5 | Standard saturation |
| `b` | 0.75 | Standard length normalization |
| `weight_className` | 5.0 | Class name match is most relevant |
| `weight_fieldNames` | 3.0 | Field name match is structural |
| `weight_fieldTypes` | 3.0 | Field type match is structural |
| `weight_superClass` | 2.0 | Parent class is contextual |
| `weight_interfaces` | 2.0 | Interface is contextual |

---

## 8. Query Processing

### 8.1 User Query to BM25 Query

1. Same tokenization pipeline as indexing (lowercase, strip non-identifier, split CamelCase, remove stopwords)
2. Each resulting token is treated as a separate query term (BM25 multi-term query, OR semantics)
3. Terms are matched against all document fields simultaneously
4. No phrase/ordered search — BM25 is a bag-of-words scorer

### 8.2 Empty Query Handling

If after stopword removal the query has zero terms, return empty result set (no results).

### 8.3 Result Ranking

Results are sorted by descending BM25 score. Ties are broken by:
1. Higher `className` field contribution
2. Lexicographic class name order (stable)

---

## 9. Result Format

Each search result entry:

```json
{
  "rank": 1,
  "score": 14.23,
  "className": "com.example.service.UserAccountServiceImpl",
  "matchedTerms": ["account", "user"],
  "topField": "className",
  "instanceCount": 42,
  "totalSize": 1048576
}
```

The `topField` indicates which document field contributed the most to the score, helping the user understand *why* the class matched.

---

## 10. MCP Tool Interface

### 10.1 `searchClasses`

```
searchClasses(query: string, topN?: int = 25, from?: int = 0)
```

Returns a ranked, paginated list of class search results.

- `query` — free-form text query (tokenized as described above)
- `topN` — max results to return (capped at 250)
- `from` — offset for pagination (for use with `topN`)

Caching: Results for a given query are cached (LRU, capacity 20) to support efficient pagination via `from` parameter.

### 10.2 Existing Tool Integration

The `className` in each result can be passed directly to:
- `getClassByName(name)` → full class details with fields, superclass
- `getClassInstances(name, from, to)` → specific instances
- OQL queries targeting the class

---

## 11. Edge Cases

### 11.1 Inner Classes

`com.example.OuterClass$InnerClass`:
- Simple name token: `outerclass$innerclass` — retained as-is
- CamelCase split: `outer`, `class`, `inner`, `class` — after stopwords: `outer`, `inner`

### 11.2 Anonymous Classes

`com.example.Foo$1`, `com.example.Foo$2`:
- Skipped entirely — simple name matches pattern `\$[0-9]+$`

### 11.3 Dynamic / Lambda Classes

`com.example.Foo$$Lambda$1234/0x0000000800c00040`:
- Skipped entirely — matches `\$\$Lambda` pattern

### 11.4 Classes Extending Skipped Classes

`com.myapp.service.MyBaseService` (extends `com.framework.BaseService` which is a library class and skipped):
- `MyBaseService` IS indexed (it is an application class)
- The superClass field for `MyBaseService` contains tokens from `BaseService` → `base`, `service`
- The actual library class `com.framework.BaseService` itself is NOT indexed (no document)

### 11.5 Java.lang.Object as Superclass

`java.lang.Object` is both in the skip list (as `java.*` → no document) and stopword list (`object` → token removed). When a class extends only `Object`, the `superClass` field will be empty (no tokens contributed). This is correct — `extends Object` carries no structural information.

### 11.6 Class Hierarchy Depth

Only the **direct** superclass is indexed. The full ancestor chain is not traversed — doing so would bloat documents and dilute BM25 precision. Chaining can be done by re-searching on the superclass name.

### 11.7 Fields Inherited from Superclass

Only fields **declared directly** on the class (per `JavaClass.getFields()`) are indexed. Inherited fields are not duplicated. The NetBeans API separates declared vs. inherited fields; if it only returns declared fields, this is already correct.

### 11.8 Interface Detection

The NetBeans `JavaClass` API may not expose implemented interfaces directly. If the API lacks a method like `getImplementedInterfaces()`, this field is omitted from the document (set to empty). The index still functions with the remaining four fields.

---

## 12. Performance Considerations

### 12.1 Index Build Time

Building the index requires a single pass over `heap.getAllClasses()` (typically 5k–50k classes). Each class requires:
- Reading its name, superclass, and fields
- Tokenization (fast, CPU-local)

Estimated time: negligible (< 1 second even for large heaps) since no instance data is traversed.

### 12.2 Memory Footprint

The inverted index is a `Map<String, List<Posting>>` where:
- Key: token (String, average length 6–10 chars)
- Value: list of (classId, termFrequency, fieldId)

With 50k classes and ~10 tokens per class (on average), the index holds ~500k postings. Estimated memory: 20–50 MB.

### 12.3 Query Latency

BM25 scoring requires:
1. Looking up each query term in the inverted index (O(1) per term)
2. Merging posting lists (O(N) where N = total postings matched)
3. Computing per-document scores across fields
4. Sorting by score (O(M log M) where M = matched documents)

For top-25 results from 50k indexed classes: estimated < 10 ms.

### 12.4 Caching

Query results are cached (LRU, 20 entries) to support pagination without re-scoring. The cache key is the raw query string (pre-tokenization).

---

## 13. Implementation Outline

```
src/main/java/com/onpositive/analyzer/search/
├── Bm25Token.java              // single token with field association
├── Bm25Document.java           // document = one class, holds all field token lists
├── Bm25Field.java              // field metadata (id, weight, avgdl, N, docCount per term)
├── Bm25Index.java              // inverted index + BM25 scorer
├── Bm25Query.java              // tokenized query
├── Bm25Result.java             // scored result (classId, score, matchedTerms, topField)
├── ClassNameTokenizer.java     // CamelCase + snake_case + identifier filter + stopwords
├── ClassSkippedPredicate.java  // skip rule evaluation (JDK prefixes, proxy patterns)
└── HeapDumpBm25Indexer.java    // walks Heap, builds Bm25Index from non-skipped classes
```

Integration point: `HeapDumpService` holds a reference to `Bm25Index` (built lazily on first search) and exposes `searchClassesBm25(query, topN, from)`.
