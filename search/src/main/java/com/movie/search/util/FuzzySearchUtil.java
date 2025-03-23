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
    public static int calculateSimilarity(String input, String target) {
        int maxLength = Math.max(input.length(), target.length());
        int distance = levenshteinDistance(input, target);
        return (int) ((1 - (double) distance / maxLength) * 100);
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
}