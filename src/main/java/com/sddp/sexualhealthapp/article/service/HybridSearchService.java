package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid search combining TF-IDF lexical scores with semantic vector similarity.
 * Formula: hybrid = 0.4 × normalized_tfidf + 0.6 × adjusted_semantic
 *
 * Semantic scores are adjusted by subtracting a baseline floor because sentence
 * embedding models produce non-zero cosine similarity (~0.5) even for unrelated text.
 */
public class HybridSearchService {

    static final double TFIDF_WEIGHT = 0.4;
    static final double SEMANTIC_WEIGHT = 0.6;
    /** Cosine similarity baseline for unrelated text in all-MiniLM-L6-v2. */
    static final double SEMANTIC_FLOOR = 0.45;
    private static final double DEFAULT_MIN_SCORE = 0.05;

    private final ArticleSearchService tfidfService;
    private final SemanticSearchService semanticService;

    /** Production: creates default services. */
    public HybridSearchService() {
        this(new ArticleSearchService(), new SemanticSearchService());
    }

    /** Testing: accepts injected services. */
    public HybridSearchService(ArticleSearchService tfidfService, SemanticSearchService semanticService) {
        this.tfidfService = tfidfService;
        this.semanticService = semanticService;
    }

    /** Search with default min score. */
    public List<SearchResult> search(String query) {
        return search(query, DEFAULT_MIN_SCORE);
    }

    /** Search with custom minimum score threshold. */
    public List<SearchResult> search(String query, double minScore) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Get results from both engines
        List<SearchResult> tfidfResults = tfidfService.search(query);
        Map<Article, Double> semanticScores = semanticService.search(query);

        // Build raw score maps
        Map<Article, Double> tfidfScoreMap = tfidfResults.stream()
                .collect(Collectors.toMap(SearchResult::article, SearchResult::score));

        // Collect all matched articles (union)
        Set<Article> allArticles = new LinkedHashSet<>();
        allArticles.addAll(tfidfScoreMap.keySet());
        allArticles.addAll(semanticScores.keySet());

        if (allArticles.isEmpty()) {
            return Collections.emptyList();
        }

        // Normalize TF-IDF to [0,1] (arbitrary scale needs normalization).
        // Cosine similarity is already [0,1] and meaningful — use raw values.
        double[] tfidfRange = getRange(tfidfScoreMap.values());

        // Compute hybrid scores
        List<SearchResult> results = new ArrayList<>();

        for (Article article : allArticles) {
            double rawTfidf = tfidfScoreMap.getOrDefault(article, 0.0);
            double rawSemantic = semanticScores.getOrDefault(article, 0.0);

            double normTfidf = normalize(rawTfidf, tfidfRange[0], tfidfRange[1]);

            // Subtract baseline floor and rescale to [0,1].
            // Cosine similarity from MiniLM is ~0.45-0.55 even for unrelated text.
            double adjustedSemantic = Math.max(0.0, (rawSemantic - SEMANTIC_FLOOR) / (1.0 - SEMANTIC_FLOOR));

            double hybridScore = TFIDF_WEIGHT * normTfidf + SEMANTIC_WEIGHT * adjustedSemantic;

            if (hybridScore >= minScore) {
                Map<String, Double> fieldScores = Map.of(
                        "tfidf", rawTfidf,
                        "semantic", rawSemantic
                );
                results.add(new SearchResult(article, hybridScore, fieldScores));
            }
        }

        results.sort(Comparator.comparingDouble(SearchResult::score).reversed());
        return Collections.unmodifiableList(results);
    }

    /** Return top N results by hybrid relevance. */
    public List<SearchResult> searchTop(String query, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        return search(query).stream()
                .limit(limit)
                .toList();
    }

    /** Min-max normalize a value to [0,1]. Returns 1.0 if min == max (all scores equal). */
    static double normalize(double value, double min, double max) {
        if (max == min) {
            return value > 0 ? 1.0 : 0.0;
        }
        return (value - min) / (max - min);
    }

    /** Returns [min, max] of a collection of values. Returns [0,0] for empty collections. */
    static double[] getRange(Collection<Double> values) {
        if (values.isEmpty()) {
            return new double[]{0.0, 0.0};
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        return new double[]{min, max};
    }
}
