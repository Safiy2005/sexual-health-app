package com.sddp.sexualhealthapp.article.model;

import java.util.Map;

/**
 * Search result with article, score, and field breakdown. Sorts by descending relevance.
 */
public record SearchResult(
    Article article,
    double score,
    Map<String, Double> fieldScores
) implements Comparable<SearchResult> {

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
