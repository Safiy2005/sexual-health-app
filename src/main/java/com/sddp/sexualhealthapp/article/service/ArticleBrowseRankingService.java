package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ranks browse articles by user relevance.
 *
 * <p>
 * Ranking is TF-IDF first (recent-read similarity), with tags/preferences as
 * bounded secondary signals.
 * </p>
 */
public class ArticleBrowseRankingService {

    private static final double RECENT_SIGNAL_WEIGHT = 0.85;
    private static final double PREFERRED_TAG_WEIGHT = 0.15;
    private static final int RECENT_RESULT_LIMIT = 30;
    private static final int MAX_GENERAL_SECTION_SCORE = 6;
    private static final int MAX_GENERAL_HEADING_TOKEN_SCORE = 18;
    private static final int MAX_GENERAL_CONTENT_TOKEN_SCORE = 180;
    private static final int MAX_GENERAL_KEYWORD_SCORE = 6;
    private static final int MAX_GENERAL_TAG_SCORE = 4;
    private static final double GENERAL_SECTION_WEIGHT = 0.35;
    private static final double GENERAL_HEADING_WEIGHT = 0.25;
    private static final double GENERAL_CONTENT_WEIGHT = 0.15;
    private static final double GENERAL_KEYWORD_WEIGHT = 0.15;
    private static final double GENERAL_TAG_WEIGHT = 0.10;

    private static final Object preloadLock = new Object();
    private static volatile boolean preloadComplete = false;
    private static volatile Map<String, Double> cachedGeneralScoresById = Map.of();

    @FunctionalInterface
    interface QuerySearch {
        List<SearchResult> search(String query, double minScore);
    }

    private final QuerySearch querySearch;

    /** Production constructor. */
    public ArticleBrowseRankingService() {
        this(ArticleServiceRegistry.getArticleSearchService()::search);
    }

    /** Testing constructor. */
    ArticleBrowseRankingService(QuerySearch querySearch) {
        this.querySearch = querySearch;
    }

    /**
     * Warms TF-IDF browse ranking dependencies and precomputes the cheap general
     * fallback scores in the background.
     */
    public static void preload() {
        if (preloadComplete) {
            return;
        }

        synchronized (preloadLock) {
            if (preloadComplete) {
                return;
            }

            ArticleCollection collection = ArticleServiceRegistry.getArticleCollection();
            ArticleServiceRegistry.getArticleSearchService();
            cachedGeneralScoresById = computeGeneralScoresById(collection.getArticles());
            preloadComplete = true;
        }
    }

    /**
     * Returns articles ordered by personalized browse relevance.
     */
    public List<Article> rankArticles(List<Article> articles,
            List<RecentlyReadEntry> recentEntries,
            ContentPreferences preferences) {
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        List<Article> candidates = new ArrayList<>(articles);
        Map<String, Article> byId = buildArticleMap(candidates);
        boolean hasRecent = recentEntries != null && !recentEntries.isEmpty();
        boolean hasPreferred = preferences != null && !preferences.preferredTags().isEmpty();

        Map<Article, Double> baseScores = hasRecent
                ? computeRecentScores(candidates, recentEntries, byId)
                : computeFallbackScores(candidates);
        Map<Article, Double> normalizedBase = normalizeScores(candidates, baseScores);

        List<Article> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator
                .comparingDouble((Article article) -> computeFinalScore(
                        article, normalizedBase, preferences, hasRecent, hasPreferred))
                .reversed()
                .thenComparing(article -> safeLower(article.getTitle())));
        return sorted;
    }

    private Map<String, Article> buildArticleMap(List<Article> articles) {
        Map<String, Article> byId = new HashMap<>();
        for (Article article : articles) {
            if (article.getFileName() != null) {
                byId.put(article.getFileName(), article);
            }
        }
        return byId;
    }

    private Map<Article, Double> computeRecentScores(List<Article> candidates,
            List<RecentlyReadEntry> recentEntries,
            Map<String, Article> byId) {
        Map<Article, Double> scores = initializeScores(candidates);
        for (int i = 0; i < recentEntries.size(); i++) {
            RecentlyReadEntry entry = recentEntries.get(i);
            Article source = byId.get(entry.articleId());
            if (source == null) {
                continue;
            }

            String query = buildRecentQuery(source, entry.lastReadSectionIndex());
            if (query.isBlank()) {
                continue;
            }

            double recencyWeight = 1.0 / (i + 1.0);
            List<SearchResult> results = querySearch.search(query, 0.0);
            for (int j = 0; j < results.size() && j < RECENT_RESULT_LIMIT; j++) {
                SearchResult result = results.get(j);
                Article target = result.article();
                if (target == null || target.equals(source) || !scores.containsKey(target)) {
                    continue;
                }
                scores.merge(target, result.score() * recencyWeight, Double::sum);
            }
        }
        return scores;
    }

    private Map<Article, Double> computeFallbackScores(List<Article> candidates) {
        Map<Article, Double> scores = initializeScores(candidates);
        Map<String, Double> generalScores = cachedGeneralScoresById;
        if (generalScores.isEmpty()) {
            generalScores = computeGeneralScoresById(candidates);
            cachedGeneralScoresById = generalScores;
        }

        for (Article article : candidates) {
            String articleId = article.getFileName();
            if (articleId == null) {
                scores.put(article, computeGeneralScore(article));
                continue;
            }
            scores.put(article, generalScores.getOrDefault(articleId, computeGeneralScore(article)));
        }
        return scores;
    }

    private Map<Article, Double> initializeScores(List<Article> articles) {
        Map<Article, Double> scores = new LinkedHashMap<>();
        for (Article article : articles) {
            scores.put(article, 0.0);
        }
        return scores;
    }

    private Map<Article, Double> normalizeScores(List<Article> candidates, Map<Article, Double> rawScores) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (Article article : candidates) {
            double value = rawScores.getOrDefault(article, 0.0);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        Map<Article, Double> normalized = new HashMap<>();
        if (max <= min) {
            for (Article article : candidates) {
                normalized.put(article, 0.0);
            }
            return normalized;
        }

        for (Article article : candidates) {
            double value = rawScores.getOrDefault(article, 0.0);
            normalized.put(article, (value - min) / (max - min));
        }
        return normalized;
    }

    private double computeFinalScore(Article article,
            Map<Article, Double> normalizedBase,
            ContentPreferences preferences,
            boolean hasRecent,
            boolean hasPreferred) {
        double preferredTagScore = computePreferredTagScore(article, preferences);
        double base = normalizedBase.getOrDefault(article, 0.0);

        if (hasRecent) {
            return RECENT_SIGNAL_WEIGHT * base + PREFERRED_TAG_WEIGHT * preferredTagScore;
        }
        if (hasPreferred) {
            return 0.70 * base + 0.30 * preferredTagScore;
        }
        return base;
    }

    private double computePreferredTagScore(Article article, ContentPreferences preferences) {
        List<String> matches = ArticlePersonalizationService.getPreferredMatchedTags(article, preferences);
        if (matches.isEmpty()) {
            return 0.0;
        }
        return Math.min(1.0, matches.size() / 2.0);
    }

    private String buildRecentQuery(Article article, int sectionIndex) {
        if (article.getSections().isEmpty()) {
            return clean(article.getTitle());
        }
        int safeIndex = Math.max(0, Math.min(sectionIndex, article.getSections().size() - 1));
        Article.Section section = article.getSections().get(safeIndex);
        return join(
                article.getTitle(),
                section.heading(),
                section.content());
    }

    private static Map<String, Double> computeGeneralScoresById(List<Article> articles) {
        Map<String, Double> scores = new HashMap<>();
        for (Article article : articles) {
            if (article.getFileName() != null) {
                scores.put(article.getFileName(), computeGeneralScore(article));
            }
        }
        return scores;
    }

    private static double computeGeneralScore(Article article) {
        if (article == null) {
            return 0.0;
        }

        int sectionCount = article.getSections().size();
        int headingTokenCount = TextPreprocessor.tokenize(article.getSections().stream()
                .map(Article.Section::heading)
                .collect(Collectors.joining(" "))).size();
        int contentTokenCount = TextPreprocessor.tokenize(article.getSections().stream()
                .map(Article.Section::content)
                .collect(Collectors.joining(" "))).size();
        long keywordCount = article.getKeywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .count();
        long tagCount = article.getTags().stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .filter(tag -> !ArticlePersonalizationService.EVERYONE_TAG.equalsIgnoreCase(tag))
                .count();

        double sectionScore = clampRatio(sectionCount, MAX_GENERAL_SECTION_SCORE);
        double headingScore = clampRatio(headingTokenCount, MAX_GENERAL_HEADING_TOKEN_SCORE);
        double contentScore = clampRatio(contentTokenCount, MAX_GENERAL_CONTENT_TOKEN_SCORE);
        double keywordScore = clampRatio(keywordCount, MAX_GENERAL_KEYWORD_SCORE);
        double tagScore = clampRatio(tagCount, MAX_GENERAL_TAG_SCORE);

        return GENERAL_SECTION_WEIGHT * sectionScore
                + GENERAL_HEADING_WEIGHT * headingScore
                + GENERAL_CONTENT_WEIGHT * contentScore
                + GENERAL_KEYWORD_WEIGHT * keywordScore
                + GENERAL_TAG_WEIGHT * tagScore;
    }

    private static double clampRatio(long value, long max) {
        if (max <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) value / (double) max);
    }

    private String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String cleaned = clean(part);
            if (cleaned.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(cleaned);
        }
        return builder.toString();
    }

    private String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text
                .replace(String.valueOf(Article.BOLD_START), "")
                .replace(String.valueOf(Article.BOLD_END), "")
                .replace('→', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
