package com.onpositive.analyzer.search;

public enum Bm25Field {

    CLASS_NAME("className", 5.0),
    SUPER_CLASS("superClass", 2.0),
    INTERFACES("interfaces", 2.0),
    FIELD_NAMES("fieldNames", 3.0),
    FIELD_TYPES("fieldTypes", 3.0);

    private final String id;
    private final double weight;

    Bm25Field(String id, double weight) {
        this.id = id;
        this.weight = weight;
    }

    public String id() {
        return id;
    }

    public double weight() {
        return weight;
    }
}
