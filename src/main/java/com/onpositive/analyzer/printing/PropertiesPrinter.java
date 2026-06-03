package com.onpositive.analyzer.printing;

import java.util.Properties;

public class PropertiesPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof Properties props)) {
            return object != null ? object.toString() : "";
        }

        StringBuilder sb = new StringBuilder();
        props.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
        return sb.toString();
    }
}
