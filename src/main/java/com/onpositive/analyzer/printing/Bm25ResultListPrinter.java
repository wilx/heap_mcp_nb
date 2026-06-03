package com.onpositive.analyzer.printing;

import com.onpositive.analyzer.search.Bm25Result;

import java.util.List;
import java.util.Locale;

public class Bm25ResultListPrinter implements IValuePrinter {

    @Override
    public String print(Object object) {
        if (!(object instanceof List)) {
            return "";
        }
        List<?> list = (List<?>) object;
        if (list.isEmpty()) {
            return "No results found.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%-5s | %-10s | %-60s | %-15s | %-20s%n",
                "Rank", "Score", "Class Name", "Top Field", "Matched Terms"));
        sb.append("-".repeat(115)).append("\n");
        int rank = 1;
        int shown = 0;
        for (Object item : list) {
            if (!(item instanceof Bm25Result)) {
                continue;
            }
            Bm25Result r = (Bm25Result) item;
            if (shown >= 250) {
                sb.append("... (truncated)\n");
                break;
            }
            sb.append(String.format(Locale.US, "%-5d | %-10.2f | %-60s | %-15s | %-20s%n",
                    rank++, r.score(),
                    truncate(r.className(), 60),
                    r.topField(),
                    r.matchedTerms()));
            shown++;
        }
        return sb.toString();
    }

    private static String truncate(String s, int len) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (s.length() <= len) {
            return s;
        }
        return s.substring(0, len - 3) + "...";
    }
}
