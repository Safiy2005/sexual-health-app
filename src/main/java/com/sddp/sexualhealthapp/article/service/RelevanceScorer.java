package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TF-IDF scoring with weighted fields: title (3.0x) > headings (1.5x) > content (1.0x).
 * Uses smoothed IDF for small corpora.
 */
public class RelevanceScorer {

    /** Weight multiplier for title matches */
    private static final double TITLE_WEIGHT = 3.0;

    /** Weight multiplier for heading matches */
    private static final double HEADINGS_WEIGHT = 1.5;

    /** Weight multiplier for content matches */
    private static final double CONTENT_WEIGHT = 1.0;

    /** Cache of IDF scores for all terms in the corpus */                     
    private final Map<String, Double> idfCache;

    /** Total number of documents in the corpus */
    private final int totalDocuments;

    /** Initialize scorer and build IDF cache. */
    public RelevanceScorer(ArticleCollection articles) {
        this.totalDocuments = articles.getArticles().size();
        this.idfCache = buildIdfCache(articles);
    }

    /** Score article against normalized query. */
    public SearchResult score(Article article, String normalizedQuery) {
        List<String> queryTerms = TextPreprocessor.tokenize(normalizedQuery);

        if (queryTerms.isEmpty()) {
            return new SearchResult(article, 0.0, Map.of());
        }

        Map<String, String> fields = TextPreprocessor.extractFields(article);

        double titleScore = calculateFieldScore(
            TextPreprocessor.normalize(fields.get("title")),
            queryTerms,
            TITLE_WEIGHT
        );

        double headingsScore = calculateFieldScore(
            TextPreprocessor.normalize(fields.get("headings")),
            queryTerms,
            HEADINGS_WEIGHT
        );

        double contentScore = calculateFieldScore(
            TextPreprocessor.normalize(fields.get("content")),
            queryTerms,
            CONTENT_WEIGHT
        );

        double totalScore = titleScore + headingsScore + contentScore;

        // Store individual field scores to enable debugging and explain which fields contributed
        Map<String, Double> fieldScores = Map.of(
            "title", titleScore,
            "headings", headingsScore,
            "content", contentScore
        );

        return new SearchResult(article, totalScore, fieldScores);
    }

    /** Weighted TF-IDF for field. */
    private double calculateFieldScore(String fieldText, List<String> queryTerms, double weight) {
        if (fieldText == null || fieldText.isEmpty()) {
            return 0.0;
        }

        List<String> tokens = TextPreprocessor.tokenize(fieldText);

        if (tokens.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        for (String term : queryTerms) {
            double tf = calculateTF(term, tokens);
            double idf = idfCache.getOrDefault(term, 0.0);
            score += tf * idf;
        }

        return score * weight;
    }

    /** Term frequency = count / total. */
    private double calculateTF(String term, List<String> tokens) {
        if (tokens.isEmpty()) {
            return 0.0;
        }

        long count = tokens.stream()
            .filter(token -> token.equals(term))
            .count();

        return (double) count / tokens.size();
    }

    /** Build IDF cache for all corpus terms. */
    private Map<String, Double> buildIdfCache(ArticleCollection articles) {
        Map<String, Integer> documentFrequency = new HashMap<>();

        for (Article article : articles.getArticles()) {
            Set<String> uniqueTerms = new HashSet<>();
            Map<String, String> fields = TextPreprocessor.extractFields(article);

            uniqueTerms.addAll(TextPreprocessor.tokenize(
                TextPreprocessor.normalize(fields.get("title"))
            ));
            uniqueTerms.addAll(TextPreprocessor.tokenize(
                TextPreprocessor.normalize(fields.get("headings"))
            ));
            uniqueTerms.addAll(TextPreprocessor.tokenize(
                TextPreprocessor.normalize(fields.get("content"))
            ));

            for (String term : uniqueTerms) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        Map<String, Double> idfScores = new HashMap<>();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            String term = entry.getKey();
            int docCount = entry.getValue();

            // Smoothed IDF: log((N+1)/(df+1)) + 1 ensures positive scores for common terms
            // in small specialized corpora (e.g., sexual health articles with "symptoms" in all docs)
            double idf = Math.log((double) (totalDocuments + 1) / (docCount + 1)) + 1.0;
            idfScores.put(term, idf);
        }

        return idfScores;
    }
}
