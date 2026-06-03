package com.onpositive.analyzer.printing;

public interface IValuePrinter {

    /**
     * Implement this method to print passed object in a human and LLM-readable form
     * @param object Object to print
     * @return Readable string representation
     */
    String print(Object object);

    /**
     * Default {@link IValuePrinter}. If argument is not null - call toString() for it, otherwise return empty string
     */
    public static IValuePrinter DEFAULT = object -> {
        return object != null ? object.toString() : "";
    };
}
