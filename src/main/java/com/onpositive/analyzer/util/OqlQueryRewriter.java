package com.onpositive.analyzer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OqlQueryRewriter {

    private static final Pattern SELECT_STAR_FROM_PATTERN = Pattern.compile(
            "^select\\s+\\*\\s+from\\s+([a-zA-Z_][a-zA-Z0-9_.\\[\\]]*)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    public String rewrite(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }

        String trimmedQuery = query.trim();
        Matcher matcher = SELECT_STAR_FROM_PATTERN.matcher(trimmedQuery);

        if (matcher.matches()) {
            String qualifiedName = matcher.group(1);
            String alias = generateAlias(qualifiedName);
            return "select " + alias + " from " + qualifiedName + " " + alias;
        }

        return query;
    }

    private String generateAlias(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        String simpleName = parts[parts.length - 1];
        simpleName = simpleName.replaceAll("\\[\\]$", "");
        if (simpleName.isEmpty()) {
            return "o";
        }
        return String.valueOf(Character.toLowerCase(simpleName.charAt(0)));
    }
}
