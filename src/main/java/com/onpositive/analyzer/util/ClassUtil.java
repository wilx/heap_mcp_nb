package com.onpositive.analyzer.util;

import org.netbeans.lib.profiler.heap.Instance;

public class ClassUtil {

    public static String getClassName(Instance inst) {
        if (inst.getJavaClass() == null || inst.getJavaClass().getName() == null) {
            return "";
        }
        String name = inst.getJavaClass().getName();
        if (name.equals("java.lang.Class")) {
            Object clzName = inst.getValueOfField("name");
            if (clzName != null) {
                return name + "<" + clzName + ">";
            }
        }
        return name;
    }
}
