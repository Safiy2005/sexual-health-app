package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Article search with TF-IDF semantic relevance scoring.
 * Usage: {@code new ArticleSearchService().search("chlamydia symptoms")}
 */
public class ArticleSearchService {

    /** Default minimum score threshold for search results */
    private static final double DEFAULT_MIN_SCORE = 0.01;

    /** The article collection to search */
    private final ArticleCollection articleCollection;

    /** The relevance scorer for calculating TF-IDF scores */
    private final RelevanceScorer scorer;

    /** Production: uses singleton collection. */
    public ArticleSearchService() {
        this(ArticleCollection.getInstance());
    }

    /** Testing: accepts injected collection. */
    public ArticleSearchService(ArticleCollection articleCollection) {
        this.articleCollection = articleCollection;
        this.scorer = new RelevanceScorer(articleCollection);
    }

    /** Search with default min score (0.01). */
    public List<SearchResult> search(String query) {
        return search(query, DEFAULT_MIN_SCORE);
    }

    /** Search with custom minimum score threshold. */
    public List<SearchResult> search(String query, double minScore) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = TextPreprocessor.normalize(query);

        if (normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        return articleCollection.getArticles().stream()
            .map(article -> scorer.score(article, normalizedQuery))
            .filter(result -> result.score() >= minScore)
            .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
            .toList();
    }

    /** Return top N results by relevance. */
    public List<SearchResult> searchTop(String query, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        return search(query).stream()
            .limit(limit)
            .toList();
    }
}
