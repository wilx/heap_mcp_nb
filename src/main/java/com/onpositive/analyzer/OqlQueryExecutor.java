package com.onpositive.analyzer;

import com.onpositive.analyzer.util.ClassUtil;
import com.onpositive.analyzer.util.ValueUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;

import javax.script.ScriptEngine;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class OqlQueryExecutor {

    private final Heap heap;
    private final ValueUtil.Utf16ByteOrder utf16ByteOrder;
    private OQLEngine oqlEngine;

    public OqlQueryExecutor(Heap heap) {
        this.heap = heap;
        this.utf16ByteOrder = ValueUtil.utf16ByteOrder(heap);
    }

    public OqlResult executeOql(String query, int maxResults) throws Exception {
        if (oqlEngine == null) {
            oqlEngine = new OQLEngine(heap);
            patchOqlEngineForFastString(oqlEngine);
            heap.getJavaClassByName("java.lang.String");
            try {
                heap.getJavaClassByName("java.lang.StringUTF16");
            } catch (Exception ignored) {
            }
        }

        List<OqlRow> rows = new ArrayList<>();
        boolean[] truncated = {false};

        oqlEngine.executeQuery(query, new OQLEngine.ObjectVisitor() {
            int count = 0;

            @Override
            public boolean visit(Object o) {
                count++;
                if (count > maxResults) {
                    truncated[0] = true;
                    return false;
                }

                rows.add(toRow(count, o));
                return true;
            }
        });

        return new OqlResult(rows, rows.size(), maxResults, truncated[0]);
    }

    private OqlRow toRow(int index, Object value) {
        if (value == null) {
            return new OqlRow(index, "null", "", "null", -1L, "", -1L, false, -1, List.of(), List.of());
        }
        if (value instanceof PrimitiveArrayInstance arrayInstance) {
            String className = ClassUtil.getClassName(arrayInstance);
            return new OqlRow(index, "primitive_array", className, "Array:" + className,
                    arrayInstance.getInstanceId(), className, arrayInstance.getSize(), safeIsGCRoot(arrayInstance),
                    arrayInstance.getLength(), InstanceFieldValues.arrayPreview(arrayInstance), List.of());
        }
        if (value instanceof Instance instance) {
            String className = ClassUtil.getClassName(instance);
            String kind = "java.lang.String".equals(className) ? "string" : "instance";
            String displayValue = "java.lang.String".equals(className)
                    ? String.valueOf(ValueUtil.fastExtractStringValue(instance, utf16ByteOrder))
                    : className;
            List<InstanceFieldValues.InstanceFieldValue> fields = "java.lang.String".equals(className)
                    ? List.of()
                    : InstanceFieldValues.from(instance, utf16ByteOrder);
            return new OqlRow(index, kind, className, displayValue, instance.getInstanceId(), className,
                    instance.getSize(), safeIsGCRoot(instance), -1, List.of(), fields);
        }
        return new OqlRow(index, "value", value.getClass().getName(), String.valueOf(value),
                -1L, "", -1L, false, -1, List.of(), List.of());
    }

    private static boolean safeIsGCRoot(Instance instance) {
        try {
            return instance.isGCRoot();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public record OqlResult(
            @Schema(description = "Rows returned by the OQL query.")
            List<OqlRow> rows,
            @Schema(description = "Number of rows returned.")
            int returnedCount,
            @Schema(description = "Maximum number of rows requested.")
            int maxResults,
            @Schema(description = "Whether additional rows were available but not returned because maxResults was reached.")
            boolean truncated) {
    }

    public record OqlRow(
            @Schema(description = "One-based row index in the OQL result.")
            int index,
            @Schema(description = "Result kind: string, primitive_array, instance, value, or null.")
            String kind,
            @Schema(description = "Java type or heap class name for the row value.")
            String valueType,
            @Schema(description = "Short display value for scalar values, strings, arrays, or instances.")
            String displayValue,
            @Schema(description = "Internal heap instance ID, or -1 when the row is not a heap instance.")
            long instanceId,
            @Schema(description = "Heap class name for instance and array rows, or empty for scalar values.")
            String className,
            @Schema(description = "Shallow size in bytes for instance and array rows, or -1 for scalar values.")
            long shallowSize,
            @Schema(description = "Whether the row value is directly reported as a GC root.")
            boolean gcRoot,
            @Schema(description = "Array length for primitive array rows, or -1 for other row kinds.")
            int arrayLength,
            @Schema(description = "Preview values for primitive array rows, capped at the configured maximum.")
            List<String> arrayPreview,
            @Schema(description = "Structured field values for object instance rows, or empty for other row kinds.")
            List<InstanceFieldValues.InstanceFieldValue> fields) {
    }

    private void patchOqlEngineForFastString(OQLEngine oqlEngine) {
        try {
            Field delegateField = OQLEngine.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(oqlEngine);

            Field engineField = delegate.getClass().getDeclaredField("engine");
            engineField.setAccessible(true);
            ScriptEngine scriptEngine = (ScriptEngine) engineField.get(delegate);

            String jsPatch =
                "var FastStr = Java.type(\"com.onpositive.analyzer.util.ValueUtil\");" +
                "var Utf16ByteOrder = Java.type(\"com.onpositive.analyzer.util.ValueUtil$Utf16ByteOrder\");" +
                "var utf16ByteOrder = new Utf16ByteOrder(" + utf16ByteOrder.hiByteShift() + "," + utf16ByteOrder.loByteShift() + ");" +
                "JavaObjectWrapper = function(instance) {" +
                "  var things = instance.fieldValues;" +
                "  var fldValueCache = new Array();" +
                "  return new JSAdapter() {" +
                "    __getIds__ : function() {" +
                "      var res = new Array(things.size());" +
                "      for(var j=0;j<things.size();j++) { res[j] = things.get(j).field.name; }" +
                "      return res;" +
                "    }," +
                "    __has__ : function(name) {" +
                "      for (var i=0;i<things.size();i++) { if (name == things.get(i).field.name) return true; }" +
                "      return name == 'clazz' || name == 'toString' || name == 'id' || name == 'wrapped-object' || name == 'statics';" +
                "    }," +
                "    __get__ : function(name) {" +
                "      if (name == 'clazz') {" +
                "        if (fldValueCache[name] == undefined) { fldValueCache[name] = wrapJavaObject(instance.javaClass); }" +
                "        return fldValueCache[name];" +
                "      } else if (name == 'statics') {" +
                "        if (fldValueCache[name] == undefined) { var clz = wrapJavaObject(instance.javaClass); fldValueCache[name] = clz != undefined ? clz.statics : null; }" +
                "        return fldValueCache[name];" +
                "      } else if (name == 'id') {" +
                "        if (fldValueCache[name] == undefined) { fldValueCache[name] = instance.instanceId; }" +
                "        return fldValueCache[name];" +
                "      } else if (name == 'wrapped-object') { return instance; }" +
                "      else { if (fldValueCache['_$'+name] == undefined) { fldValueCache['_$'+name] = wrapJavaObject(instance.getValueOfField(name)); } return fldValueCache['_$'+name]; }" +
                "    }," +
                "    __call__: function(name) {" +
                "      if (name == 'toString') {" +
                "        if (instance.javaClass.name == 'java.lang.String') { return FastStr.fastExtractStringValue(instance, utf16ByteOrder); }" +
                "        return instance.toString();" +
                "      } else { return undefined; }" +
                "    }" +
                "  };" +
                "};" +
                "JavaValueArrayWrapper = function(array) {" +
                "  var elements = array.values;" +
                "  var fldValueCache = new Array();" +
                "  return new JSAdapter() {" +
                "    __getIds__ : function() { var r = new Array(elements.size()); for (var i = 0; i < elements.size(); i++) { r[i] = String(i); } return r; }," +
                "    __has__: function(name) { return (name >= 0 && name < elements.size()) || name == 'length' || name == 'clazz' || name == 'toString' || name == 'wrapped-object'; }," +
                "    __get__: function(name) {" +
                "      if (name >= 0 && name < elements.size()) { return elements.get(name); }" +
                "      if (name == 'length') { if (fldValueCache['len'] == undefined) { fldValueCache['len'] = elements.size(); } return fldValueCache['len']; }" +
                "      else if (name == 'wrapped-object') { return array; }" +
                "      else if (name == 'clazz') { if (fldValueCache[name] == undefined) { fldValueCache[name] = wrapJavaObject(array.javaClass); } return fldValueCache[name]; }" +
                "      else { return undefined; }" +
                "    }," +
                "    __call__: function(name) {" +
                "      if (name == 'toString') {" +
                "        if (array.javaClass.name == 'char[]') { return FastStr.fastExtractStringValue(array); }" +
                "        return array.toString();" +
                "      } else { return undefined; }" +
                "    }" +
                "  };" +
                "};";

            scriptEngine.eval(jsPatch);
        } catch (Exception e) {
            // If patching fails, fall back to original slow path
        }
    }
}
