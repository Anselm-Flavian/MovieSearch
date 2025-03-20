package com.movie.search.util;

import org.apache.commons.text.similarity.LevenshteinDistance;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FuzzySearchUtil {
    private static final LevenshteinDistance levenshtein = new LevenshteinDistance();
    private static final int DEFAULT_THRESHOLD = 3;

    public static List<Map<String, Object>> fuzzyFilterMovies(String query, List<Map<String, Object>> movies) {
        if (query == null || query.trim().isEmpty() || movies == null) {
            System.out.println("Fuzzy filter: Empty query or movies, returning empty list");
            return List.of();
        }
        String lowerQuery = query.toLowerCase();
        List<Map<String, Object>> filtered = movies.stream()
                .filter(movie -> {
                    String title = (String) movie.get("title");
                    if (title == null) {
                        System.out.println("Fuzzy filter: Movie title is null for movie: " + movie);
                        return false;
                    }
                    String lowerTitle = title.toLowerCase();
                    int distance = levenshtein.apply(lowerQuery, lowerTitle);
                    boolean matches = distance <= DEFAULT_THRESHOLD;
                    System.out.println("Fuzzy filter: Query=" + lowerQuery + ", Title=" + title +
                            ", Distance=" + distance + ", Threshold=" + DEFAULT_THRESHOLD +
                            ", Matches=" + matches);
                    return matches;
                })
                .sorted((m1, m2) -> {
                    String title1 = (String) m1.get("title");
                    String title2 = (String) m2.get("title");
                    int dist1 = levenshtein.apply(lowerQuery, title1.toLowerCase());
                    int dist2 = levenshtein.apply(lowerQuery, title2.toLowerCase());
                    return Integer.compare(dist1, dist2); // Sort by closest match
                })
                .collect(Collectors.toList());
        System.out.println("Fuzzy filter: Filtered results=" + filtered.size() + " items");
        return filtered;
    }
}