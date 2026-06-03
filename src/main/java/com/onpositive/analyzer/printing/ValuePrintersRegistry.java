package com.onpositive.analyzer.printing;

import java.util.HashMap;
import java.util.Map;

public class ValuePrintersRegistry {

    private static final ValuePrintersRegistry INSTANCE = new ValuePrintersRegistry();

    private final Map<Class<?>, IValuePrinter> printers = new HashMap<>();

    private ValuePrintersRegistry() {
        register(org.netbeans.lib.profiler.heap.Instance.class, new InstancePrinter());
        register(org.netbeans.lib.profiler.heap.HeapSummary.class, new HeapSummaryPrinter());
        register(com.onpositive.analyzer.HeapDumpService.ClassStats.class, new ClassStatsListPrinter());
        register(com.onpositive.analyzer.HeapDumpService.GCRootInfo.class, new GCRootInfoListPrinter());
        register(com.onpositive.analyzer.HeapDumpService.InstanceInfo.class, new InstanceInfoPrinter());
        register(com.onpositive.analyzer.HeapDumpService.ReferenceInfo.class, new ReferenceInfoListPrinter());
        register(org.netbeans.lib.profiler.heap.JavaClass.class, new JavaClassPrinterWrapper());
        register(java.util.Properties.class, new PropertiesPrinter());
        register(com.onpositive.analyzer.search.Bm25Result.class, new Bm25ResultListPrinter());
    }

    public static ValuePrintersRegistry getInstance() {
        return INSTANCE;
    }

    public void register(Class<?> valueClass, IValuePrinter printer) {
        printers.put(valueClass, printer);
    }

    public IValuePrinter getPrinter(Class<?> valueClass) {
        return printers.get(valueClass);
    }

    public IValuePrinter getPrinterOrDefault(Class<?> valueClass) {
        IValuePrinter printer = printers.get(valueClass);
        return printer != null ? printer : IValuePrinter.DEFAULT;
    }

    public String print(Object object) {
        if (object == null) {
            return "";
        }
        Class<?> clazz = object.getClass();
        IValuePrinter printer = getPrinterOrDefault(clazz);
        return printer.print(object);
    }
}
