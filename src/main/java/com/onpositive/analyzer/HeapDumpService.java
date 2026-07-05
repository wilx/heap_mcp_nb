package com.onpositive.analyzer;

import com.onpositive.analyzer.search.Bm25Index;
import com.onpositive.analyzer.search.Bm25Result;
import com.onpositive.analyzer.search.ClassNameTokenizer;
import com.onpositive.analyzer.search.DefaultClassSkippedPredicate;
import com.onpositive.analyzer.search.HeapDumpBm25Indexer;
import com.onpositive.analyzer.search.InMemoryBm25Index;
import com.onpositive.analyzer.util.ValueUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.v3.oas.annotations.media.Schema;
import org.netbeans.lib.profiler.heap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.onpositive.analyzer.util.ClassUtil.getClassName;

public class HeapDumpService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeapDumpService.class);

    private Heap heap;
    private OqlQueryExecutor oqlExecutor;
    private List<ClassStats> classesSortedByCount;
    private List<ClassStats> classesSortedBySize;
    private final Cache<String, List<JavaClass>> classesByRegexp = Caffeine.newBuilder()
            .maximumSize(10)
            .build();

    private Bm25Index bm25Index;
    private final Cache<String, List<Bm25Result>> bm25SearchCache = Caffeine.newBuilder()
            .maximumSize(20)
            .build();
    private DuplicateStringsAnalysis duplicateStringsAnalysis;

    public static class InstancePage {
        @Schema(description = "Page of instances returned for the requested class.")
        public final List<Instance> instances;
        @Schema(description = "Total number of instances available for the requested class.")
        public final long totalCount;
        @Schema(description = "Number of instances remaining after this page.")
        public final long remaining;

        public InstancePage(List<Instance> instances, long totalCount, long remaining) {
            this.instances = instances;
            this.totalCount = totalCount;
            this.remaining = remaining;
        }
    }

    public static class ClassStats {
        @Schema(description = "Fully qualified Java class name.")
        public String className;
        @Schema(description = "Number of live instances of this class in the heap dump.")
        public long instanceCount;
        @Schema(description = "Total shallow bytes used by all live instances of this class.")
        public long size;

        public ClassStats(String className, long instanceCount, long size) {
            this.className = className;
            this.instanceCount = instanceCount;
            this.size = size;
        }
    }

    public record RetainedInstance(Instance instance, Long retainedSize, String retainedSizeError) {
    }

    public static class DuplicateStringStats {
        @Schema(description = "Duplicate string value, truncated to the requested maximum length.")
        public final String value;
        @Schema(description = "Number of java.lang.String instances that have this value.")
        public final long occurrenceCount;
        @Schema(description = "Length of the original string value in UTF-16 code units.")
        public final int stringLength;
        @Schema(description = "Instance ID of a representative java.lang.String for this value group.")
        public final long representativeInstanceId;
        @Schema(description = "Total shallow bytes used by java.lang.String instances in this value group.")
        public final long stringShallowBytes;
        @Schema(description = "Number of distinct backing arrays referenced by strings in this value group.")
        public final int distinctBackingArrayCount;
        @Schema(description = "Total shallow bytes used by distinct backing arrays for this value group.")
        public final long backingArrayShallowBytes;
        @Schema(description = "Combined shallow bytes for strings and distinct backing arrays in this value group.")
        public final long totalShallowBytes;
        private final List<DuplicateStringBackingArray> backingArrays;

        private DuplicateStringStats(String value, MutableDuplicateStringStats stats) {
            this.value = value;
            this.occurrenceCount = stats.occurrenceCount;
            this.stringLength = value.length();
            this.representativeInstanceId = stats.representativeInstanceId;
            this.stringShallowBytes = stats.stringShallowBytes;
            this.distinctBackingArrayCount = stats.backingArrays.size();
            this.backingArrayShallowBytes = stats.backingArrays.values().stream()
                    .mapToLong(backingArray -> backingArray.shallowSize).sum();
            this.totalShallowBytes = stringShallowBytes + backingArrayShallowBytes;
            this.backingArrays = stats.backingArrays.values().stream()
                    .map(MutableDuplicateStringBackingArray::toImmutable)
                    .sorted(Comparator.comparingLong(item -> item.backingArrayId))
                    .toList();
        }
    }

    public static class DuplicateStringBackingArray {
        @Schema(description = "Instance ID of the backing char[] or byte[] array.")
        public final long backingArrayId;
        @Schema(description = "Shallow size in bytes of the backing array.")
        public final long shallowSize;
        @Schema(description = "IDs of java.lang.String instances that reference this backing array.")
        public final List<Long> stringInstanceIds;

        private DuplicateStringBackingArray(long backingArrayId, long shallowSize, List<Long> stringInstanceIds) {
            this.backingArrayId = backingArrayId;
            this.shallowSize = shallowSize;
            this.stringInstanceIds = stringInstanceIds;
        }
    }

    public static class DuplicateStringBackingArrays {
        @Schema(description = "Duplicate string value, truncated to the requested maximum length.")
        public final String value;
        @Schema(description = "Number of java.lang.String instances that have this value.")
        public final long occurrenceCount;
        @Schema(description = "Length of the original string value in UTF-16 code units.")
        public final int stringLength;
        @Schema(description = "Instance ID of the representative java.lang.String used to identify this value group.")
        public final long representativeInstanceId;
        @Schema(description = "Total shallow bytes used by java.lang.String instances in this value group.")
        public final long stringShallowBytes;
        @Schema(description = "Number of distinct backing arrays referenced by strings in this value group.")
        public final int distinctBackingArrayCount;
        @Schema(description = "Total shallow bytes used by distinct backing arrays for this value group.")
        public final long backingArrayShallowBytes;
        @Schema(description = "Combined shallow bytes for strings and distinct backing arrays in this value group.")
        public final long totalShallowBytes;
        @Schema(description = "Maximum number of characters included in the returned string value.")
        public final int maxValueLength;
        @Schema(description = "Backing arrays for this duplicate string value group.")
        public final List<DuplicateStringBackingArray> backingArrays;

        private DuplicateStringBackingArrays(DuplicateStringStats stats, int maxValueLength) {
            this.value = stats.value;
            this.occurrenceCount = stats.occurrenceCount;
            this.stringLength = stats.stringLength;
            this.representativeInstanceId = stats.representativeInstanceId;
            this.stringShallowBytes = stats.stringShallowBytes;
            this.distinctBackingArrayCount = stats.distinctBackingArrayCount;
            this.backingArrayShallowBytes = stats.backingArrayShallowBytes;
            this.totalShallowBytes = stats.totalShallowBytes;
            this.maxValueLength = maxValueLength;
            this.backingArrays = stats.backingArrays;
        }
    }

    public static class DuplicateStringsPage {
        @Schema(description = "Page of duplicate string value groups.")
        public final List<DuplicateStringStats> items;
        @Schema(description = "Number of java.lang.String instances scanned while building duplicate groups.")
        public final long stringsScanned;
        @Schema(description = "Number of string instances whose value could not be decoded.")
        public final long decodingFailures;
        @Schema(description = "Total number of duplicate string value groups available before pagination.")
        public final int totalGroups;
        @Schema(description = "Zero-based start offset of this page, inclusive.")
        public final int from;
        @Schema(description = "Zero-based end offset requested for this page, exclusive.")
        public final int to;
        @Schema(description = "Number of duplicate string value groups remaining after this page.")
        public final int remaining;
        @Schema(description = "Maximum number of characters included in each returned string value.")
        public final int maxValueLength;

        private DuplicateStringsPage(List<DuplicateStringStats> items, long stringsScanned,
                                     long decodingFailures, int totalGroups, int from, int to,
                                     int remaining, int maxValueLength) {
            this.items = items;
            this.stringsScanned = stringsScanned;
            this.decodingFailures = decodingFailures;
            this.totalGroups = totalGroups;
            this.from = from;
            this.to = to;
            this.remaining = remaining;
            this.maxValueLength = maxValueLength;
        }
    }

    private static class MutableDuplicateStringStats {
        long occurrenceCount;
        long representativeInstanceId = Long.MAX_VALUE;
        long stringShallowBytes;
        final Map<Long, MutableDuplicateStringBackingArray> backingArrays = new HashMap<>();
    }

    private static class MutableDuplicateStringBackingArray {
        final long backingArrayId;
        final long shallowSize;
        final List<Long> stringInstanceIds = new ArrayList<>();

        MutableDuplicateStringBackingArray(long backingArrayId, long shallowSize) {
            this.backingArrayId = backingArrayId;
            this.shallowSize = shallowSize;
        }

        DuplicateStringBackingArray toImmutable() {
            return new DuplicateStringBackingArray(
                    backingArrayId,
                    shallowSize,
                    stringInstanceIds.stream().sorted().toList());
        }
    }

    private static class DuplicateStringsAnalysis {
        final long stringsScanned;
        final long decodingFailures;
        final List<DuplicateStringStats> byTotalBytes;
        final List<DuplicateStringStats> byDuplicateCount;
        final Map<Long, DuplicateStringStats> byRepresentativeInstanceId;

        DuplicateStringsAnalysis(long stringsScanned, long decodingFailures,
                                 List<DuplicateStringStats> byTotalBytes,
                                 List<DuplicateStringStats> byDuplicateCount,
                                 Map<Long, DuplicateStringStats> byRepresentativeInstanceId) {
            this.stringsScanned = stringsScanned;
            this.decodingFailures = decodingFailures;
            this.byTotalBytes = byTotalBytes;
            this.byDuplicateCount = byDuplicateCount;
            this.byRepresentativeInstanceId = byRepresentativeInstanceId;
        }
    }

    public HeapSummary loadHeap(String filePath) throws IOException {
        File heapFile = new File(filePath);
        if (!heapFile.exists()) {
            throw new IOException("Heap dump file not found: " + filePath);
        }
        heap = HeapFactory.createHeap(heapFile);
        clearHeapDerivedState();
        return heap.getSummary();
    }

    private void clearHeapDerivedState() {
        oqlExecutor = null;
        classesSortedByCount = null;
        classesSortedBySize = null;
        classesByRegexp.invalidateAll();
        bm25Index = null;
        bm25SearchCache.invalidateAll();
        duplicateStringsAnalysis = null;
    }

    public DuplicateStringsPage getDuplicateStrings(String sortBy, int from, int to, int maxValueLength) {
        validateRange(from, to);
        validateNonNegative("max_value_length", maxValueLength);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        if (!"total_bytes".equals(sortBy) && !"duplicate_count".equals(sortBy)) {
            throw new IllegalArgumentException("sort_by must be 'total_bytes' or 'duplicate_count'");
        }
        if (duplicateStringsAnalysis == null) {
            duplicateStringsAnalysis = analyzeDuplicateStrings();
        }

        List<DuplicateStringStats> sorted = "duplicate_count".equals(sortBy)
                ? duplicateStringsAnalysis.byDuplicateCount
                : duplicateStringsAnalysis.byTotalBytes;
        int safeFrom = Math.min(from, sorted.size());
        int safeTo = Math.min(to, sorted.size());
        return new DuplicateStringsPage(
                List.copyOf(sorted.subList(safeFrom, safeTo)),
                duplicateStringsAnalysis.stringsScanned,
                duplicateStringsAnalysis.decodingFailures,
                sorted.size(), safeFrom, safeTo, sorted.size() - safeTo, maxValueLength);
    }

    public DuplicateStringBackingArrays getDuplicateStringBackingArrays(long representativeId, int maxValueLength) {
        validateNonNegative("max_value_length", maxValueLength);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        if (duplicateStringsAnalysis == null) {
            duplicateStringsAnalysis = analyzeDuplicateStrings();
        }

        DuplicateStringStats stats = duplicateStringsAnalysis.byRepresentativeInstanceId.get(representativeId);
        if (stats == null) {
            throw new IllegalArgumentException("No duplicate string group found for representative_id: " + representativeId);
        }
        return new DuplicateStringBackingArrays(stats, maxValueLength);
    }

    private DuplicateStringsAnalysis analyzeDuplicateStrings() {
        JavaClass stringClass = heap.getJavaClassByName("java.lang.String");
        if (stringClass == null) {
            return new DuplicateStringsAnalysis(0, 0, List.of(), List.of(), Map.of());
        }

        Map<String, MutableDuplicateStringStats> grouped = new HashMap<>();
        long scanned = 0;
        long failures = 0;
        Iterator<?> iterator = stringClass.getInstancesIterator();
        while (iterator.hasNext()) {
            scanned++;
            Object item = iterator.next();
            if (!(item instanceof Instance instance)) {
                failures++;
                continue;
            }
            ValueUtil.DecodedString decoded = ValueUtil.decodeString(instance);
            if (decoded == null) {
                failures++;
                continue;
            }

            MutableDuplicateStringStats stats = grouped.computeIfAbsent(
                    decoded.value(), ignored -> new MutableDuplicateStringStats());
            stats.occurrenceCount++;
            stats.stringShallowBytes += instance.getSize();
            stats.representativeInstanceId = Math.min(
                    stats.representativeInstanceId, instance.getInstanceId());
            PrimitiveArrayInstance backing = decoded.backingArray();
            stats.backingArrays.computeIfAbsent(
                    backing.getInstanceId(),
                    ignored -> new MutableDuplicateStringBackingArray(backing.getInstanceId(), backing.getSize()))
                    .stringInstanceIds.add(instance.getInstanceId());
        }

        List<DuplicateStringStats> duplicateGroups = grouped.entrySet().stream()
                .filter(entry -> entry.getValue().occurrenceCount >= 2)
                .map(entry -> new DuplicateStringStats(entry.getKey(), entry.getValue()))
                .toList();

        Comparator<DuplicateStringStats> stableTail = Comparator
                .comparing((DuplicateStringStats stats) -> stats.value)
                .thenComparingLong(stats -> stats.representativeInstanceId);
        Comparator<DuplicateStringStats> byTotalBytes = Comparator
                .comparingLong((DuplicateStringStats stats) -> stats.totalShallowBytes).reversed()
                .thenComparing(Comparator.comparingLong(
                        (DuplicateStringStats stats) -> stats.occurrenceCount).reversed())
                .thenComparing(stableTail);
        Comparator<DuplicateStringStats> byDuplicateCount = Comparator
                .comparingLong((DuplicateStringStats stats) -> stats.occurrenceCount).reversed()
                .thenComparing(Comparator.comparingLong(
                        (DuplicateStringStats stats) -> stats.totalShallowBytes).reversed())
                .thenComparing(stableTail);

        return new DuplicateStringsAnalysis(scanned, failures,
                duplicateGroups.stream().sorted(byTotalBytes).toList(),
                duplicateGroups.stream().sorted(byDuplicateCount).toList(),
                duplicateGroups.stream().collect(Collectors.toMap(
                        stats -> stats.representativeInstanceId,
                        stats -> stats)));
    }

    public List<ClassStats> getClassesByMaxInstancesCount(int from, int to) {
        validateRange(from, to);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        if (classesSortedByCount == null) {
            classesSortedByCount = ((Collection<JavaClass>) heap.getAllClasses()).stream()
                    .map(cls -> new ClassStats(cls.getName(), cls.getInstancesCount(), cls.getAllInstancesSize()))
                    .sorted(Comparator.comparingLong((ClassStats cs) -> cs.instanceCount).reversed())
                    .collect(Collectors.toList());
        }
        int safeTo = Math.min(to, classesSortedByCount.size());
        int safeFrom = Math.min(from, safeTo);
        return classesSortedByCount.subList(safeFrom, safeTo);
    }

    public List<ClassStats> getClassesByMaxInstancesSize(int from, int to) {
        validateRange(from, to);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        if (classesSortedBySize == null) {
            classesSortedBySize = ((Collection<JavaClass>) heap.getAllClasses()).stream()
                    .map(cls -> new ClassStats(cls.getName(), cls.getInstancesCount(), cls.getAllInstancesSize()))
                    .sorted(Comparator.comparingLong((ClassStats cs) -> cs.size).reversed())
                    .collect(Collectors.toList());
        }
        int safeTo = Math.min(to, classesSortedBySize.size());
        int safeFrom = Math.min(from, safeTo);
        return classesSortedBySize.subList(safeFrom, safeTo);
    }

    public List<RetainedInstance> getBiggestObjectsByRetainedSize(int limit) {
        validateNonNegative("limit", limit);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        // NetBeans can throw here for heaps with null GC-root instances; MCP callers receive it as an error result.
        // https://github.com/apache/netbeans/issues/6357
        List<?> biggestObjects = heap.getBiggestObjectsByRetainedSize(limit);
        List<RetainedInstance> result = new ArrayList<>();
        for (Object object : biggestObjects) {
            if (!(object instanceof Instance instance)) {
                throw new IllegalStateException("Heap returned a non-instance biggest object: " + object);
            }
            try {
                result.add(new RetainedInstance(instance, instance.getRetainedSize(), null));
            } catch (RuntimeException e) {
                String message = e.getMessage();
                if (message == null || message.isEmpty()) {
                    message = e.getClass().getSimpleName();
                }
                result.add(new RetainedInstance(instance, null, message));
            }
        }
        return result;
    }

    public static class GCRootInfo {
        @Schema(description = "GC root kind reported by the heap parser.")
        public String kind;
        @Schema(description = "Internal heap instance ID of the GC root object.")
        public long instanceId;
        @Schema(description = "Fully qualified Java class name of the GC root object.")
        public String instanceClassName;

        public GCRootInfo(String kind, long instanceId, String instanceClassName) {
            this.kind = kind;
            this.instanceId = instanceId;
            this.instanceClassName = instanceClassName;
        }
    }

    public List<GCRootInfo> getGCRootsPaginated(int from, int to) {
        validateRange(from, to);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        Collection<GCRoot> allRoots = heap.getGCRoots();
        List<GCRoot> rootsList = deduplicateAdjacentGCRoots(allRoots);
        int safeTo = Math.min(to, rootsList.size());
        int safeFrom = Math.min(from, safeTo);
        List<GCRoot> page = rootsList.subList(safeFrom, safeTo);
        List<GCRootInfo> result = new ArrayList<>();
        for (GCRoot root : page) {
            Instance inst = root.getInstance();
            if (inst != null) {
                result.add(new GCRootInfo(
                        root.getKind(),
                        inst.getInstanceId(),
                        getClassName(inst)
                ));
            }
        }
        return result;
    }

    private static List<GCRoot> deduplicateAdjacentGCRoots(Collection<GCRoot> roots) {
        List<GCRoot> deduplicated = new ArrayList<>();
        String previousKind = null;
        long previousInstanceId = Long.MIN_VALUE;
        boolean hasPrevious = false;

        for (GCRoot root : roots) {
            Instance instance = root.getInstance();
            if (instance == null) {
                continue;
            }

            String kind = root.getKind();
            long instanceId = instance.getInstanceId();
            if (hasPrevious && instanceId == previousInstanceId && java.util.Objects.equals(kind, previousKind)) {
                continue;
            }

            deduplicated.add(root);
            previousKind = kind;
            previousInstanceId = instanceId;
            hasPrevious = true;
        }
        return deduplicated;
    }


    public JavaClass getJavaClassByName(String name) {
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        return heap.getJavaClassByName(name);
    }

    public JavaClass getJavaClassById(long id) {
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        return heap.getJavaClassByID(id);
    }

    public long getInstanceRetainedSize(long id) {
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        Instance instance = heap.getInstanceByID(id);
        if (instance == null) throw new IllegalArgumentException("Instance not found: " + id);
        return instance.getRetainedSize();
    }

    public Instance getInstanceById(long id) {
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        return heap.getInstanceByID(id);
    }

    public static class ReferenceInfo {
        @Schema(description = "Internal heap instance ID of the object that references the target instance.")
        public long instanceId;
        @Schema(description = "Fully qualified Java class name of the referring object.")
        public String className;
        @Schema(description = "Referring field name or array index path that points to the target instance.")
        public String fieldName;

        public ReferenceInfo(long instanceId, String className, String fieldName) {
            this.instanceId = instanceId;
            this.className = className;
            this.fieldName = fieldName;
        }
    }

    public List<ReferenceInfo> getAllReferences(long instanceId, int from, int to) {
        validateRange(from, to);
        // Resolve the target instance before asking NetBeans for its incoming references.
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        Instance instance = heap.getInstanceByID(instanceId);
        if (instance == null) return new ArrayList<>();

        // Convert field and array-element references to their defining (referring) instances.
        Collection<?> references = instance.getReferences();
        List<ReferenceInfo> result = new ArrayList<>();
        for (Object refObj : references) {
            Instance definingInstance = null;
            String source = null;
            if (refObj instanceof ObjectFieldValue ofv) {
                definingInstance = ofv.getDefiningInstance();
                source = ofv.getField() != null ? ofv.getField().getName() : null;
            } else if (refObj instanceof ArrayItemValue arrayItem) {
                definingInstance = arrayItem.getDefiningInstance();
                source = "[" + arrayItem.getIndex() + "]";
            }
            if (definingInstance != null) {
                result.add(new ReferenceInfo(
                        definingInstance.getInstanceId(),
                        getClassName(definingInstance),
                        source
                ));
            }
        }

        // Apply pagination after collecting every supported kind of incoming reference.
        int safeTo = Math.min(to, result.size());
        int safeFrom = Math.min(from, safeTo);
        return new ArrayList<>(result.subList(safeFrom, safeTo));
    }

    public List<JavaClass> getJavaClassesByRegExpPaginated(String regexp, int from, int to) {
        validateRange(from, to);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        List<JavaClass> classesList = classesByRegexp.getIfPresent(regexp);
        if (classesList == null) {
            classesList = new ArrayList<>(heap.getJavaClassesByRegExp(regexp));
            classesByRegexp.put(regexp, classesList);
        }
        int safeTo = Math.min(to, classesList.size());
        int safeFrom = Math.min(from, safeTo);
        return classesList.subList(safeFrom, safeTo);
    }

    public InstancePage getInstancesByClassName(String className, int from, int to) {
        validateRange(from, to);
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        JavaClass javaClass = heap.getJavaClassByName(className);
        if (javaClass == null) return new InstancePage(new ArrayList<>(), 0, 0);

        Collection<Instance> allInstances = javaClass.getInstances();
        List<Instance> instancesList = new ArrayList<>(allInstances);
        long totalCount = instancesList.size();

        int safeTo = Math.min(to, instancesList.size());
        int safeFrom = Math.min(from, safeTo);
        long remaining = totalCount - safeTo;
        if (remaining < 0) remaining = 0;

        return new InstancePage(instancesList.subList(safeFrom, safeTo), totalCount, remaining);
    }

    public HeapSummary getSummary() {
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        return heap.getSummary();
    }

    public Properties getSystemProperties() {
        if (heap == null) throw new IllegalStateException("Heap not loaded");
        return heap.getSystemProperties();
    }

    public List<ClassStats> getTopClasses(String filePath, int limit) throws IOException {
        validateNonNegative("limit", limit);
        loadHeap(filePath);
        return getClassesByMaxInstancesCount(0, limit);
    }

    public OqlQueryExecutor.OqlResult executeOql(String query, int maxResults) throws Exception {
        validateNonNegative("max_results", maxResults);
        if (heap == null) {
            throw new IllegalStateException("Heap not loaded. Please load a heap dump first.");
        }
        if (oqlExecutor == null) {
            oqlExecutor = new OqlQueryExecutor(heap);
        }
        return oqlExecutor.executeOql(query, maxResults);
    }

    private void buildBm25Index() {
        if (heap == null) {
            throw new IllegalStateException("Heap not loaded");
        }
        InMemoryBm25Index index = new InMemoryBm25Index();
        HeapDumpBm25Indexer indexer = new HeapDumpBm25Indexer(
                index, new DefaultClassSkippedPredicate(), new ClassNameTokenizer());
        indexer.buildIndex(heap);
        this.bm25Index = index;
    }

    public List<Bm25Result> searchClassesBm25(String query, int topN, int from) {
        validateNonNegative("top_n", topN);
        validateNonNegative("from", from);
        if (heap == null) {
            throw new IllegalStateException("Heap not loaded");
        }
        if (bm25Index == null) {
            buildBm25Index();
        }

        String cacheKey = query + "|" + topN + "|" + from;
        List<Bm25Result> fullResults = bm25SearchCache.getIfPresent(cacheKey);
        if (fullResults == null) {
            fullResults = bm25Index.search(query, Math.max(topN + from, topN));
            for (Bm25Result result : fullResults) {
                try {
                    JavaClass jc = heap.getJavaClassByName(result.className());
                    if (jc != null) {
                        result.setInstanceCount(jc.getInstancesCount());
                        result.setTotalSize(jc.getAllInstancesSize());
                    }
                } catch (RuntimeException ex) {
                    LOGGER.debug("Failed to enrich BM25 result for class {}", result.className(), ex);
                }
            }
            bm25SearchCache.put(cacheKey, fullResults);
        }

        int safeFrom = Math.min(from, fullResults.size());
        int safeTo = Math.min(safeFrom + topN, fullResults.size());
        for (int i = safeFrom; i < safeTo; i++) {
            fullResults.get(i).setRank(i + 1);
        }
        return fullResults.subList(safeFrom, safeTo);
    }

    private static void validateRange(int from, int to) {
        if (from < 0 || to < from) {
            throw new IllegalArgumentException("Expected 0 <= from <= to, where to is exclusive");
        }
    }

    private static void validateNonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
