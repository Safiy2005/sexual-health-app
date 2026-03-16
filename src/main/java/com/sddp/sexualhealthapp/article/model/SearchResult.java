package com.sddp.sexualhealthapp.article.model;

import java.util.List;
import java.util.Map;

/**
 * Search result with article, score, and field breakdown. Sorts by descending relevance.
 */
public record SearchResult(
    Article article,
    double score,
    Map<String, Double> fieldScores,
    List<String> highlightedTags,
    List<String> preferredMatchedTags
) implements Comparable<SearchResult> {

    public SearchResult(Article article, double score, Map<String, Double> fieldScores) {
        this(article, score, fieldScores, List.of(), List.of());
    }

    public SearchResult {
        highlightedTags = highlightedTags == null ? List.of() : List.copyOf(highlightedTags);
        preferredMatchedTags = preferredMatchedTags == null ? List.of() : List.copyOf(preferredMatchedTags);
    }

    /** Compares by score descending. */
    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score);
    }

    /** Returns score as percentage (0-100). */
    public int getRelevancePercent() {
        return (int) Math.round(score * 100);
    }
}
