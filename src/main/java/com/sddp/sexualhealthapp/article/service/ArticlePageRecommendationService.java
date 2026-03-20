package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import com.sddp.sexualhealthapp.settings.service.ContentPreferencesService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Recommends related articles for a specific section page in the reader.
 *
 * <p>
 * Ranking blends whole-article relevance with the currently visible section so
 * suggestions feel grounded in the article overall while still reacting to the
 * page the user is reading.
 * </p>
 */
public class ArticlePageRecommendationService {

    private static final int DEFAULT_MAX_RESULTS = 3;
    private static final double ARTICLE_CONTEXT_WEIGHT = 0.45;
    private static final double PAGE_CONTEXT_WEIGHT = 0.55;
    private static final double MIN_ABSOLUTE_MATCH_SCORE = 0.10;
    private static final double RELEVANCE_THRESHOLD = 0.48;
    private static final double ADDITIONAL_RESULT_MIN_SCORE = 0.44;
    private static final double ADDITIONAL_RESULT_TOP_RATIO = 0.82;
    private static final double TAG_BOOST_CAP = 0.03;
    private static final double KEYWORD_BOOST_CAP = 0.05;
    private static final double ARTICLE_QUERY_MIN_SCORE = 0.05;
    private static final double PAGE_QUERY_MIN_SCORE = 0.02;
    private static final int ARTICLE_HEADING_LIMIT = 8;
    private static final int PAGE_CONTENT_CHAR_LIMIT = 360;

    @FunctionalInterface
    interface QuerySearch {
        List<SearchResult> search(String query, double minScore);
    }

    private final QuerySearch articleContextSearch;
    private final QuerySearch pageContextSearch;
    private final Supplier<ContentPreferences> preferencesSupplier;

    /** Production constructor. */
    public ArticlePageRecommendationService() {
        this(new HybridSearchService()::search,
                new ArticleSearchService()::search,
                ContentPreferencesService.getInstance()::getPreferences);
    }

    /** Testing constructor. */
    ArticlePageRecommendationService(QuerySearch querySearch) {
        this(querySearch, querySearch, ContentPreferences::empty);
    }

    ArticlePageRecommendationService(QuerySearch articleContextSearch,
            QuerySearch pageContextSearch) {
        this(articleContextSearch, pageContextSearch, ContentPreferences::empty);
    }

    ArticlePageRecommendationService(QuerySearch articleContextSearch,
            QuerySearch pageContextSearch,
            Supplier<ContentPreferences> preferencesSupplier) {
        this.articleContextSearch = articleContextSearch;
        this.pageContextSearch = pageContextSearch;
        this.preferencesSupplier = preferencesSupplier;
    }

    /** Returns up to three related articles for the given article page. */
    public List<SearchResult> recommendForPage(Article currentArticle, int sectionIndex, int maxResults) {
        if (currentArticle == null || currentArticle.getSections().isEmpty() || maxResults <= 0) {
            return Collections.emptyList();
        }

        int limit = Math.min(maxResults, DEFAULT_MAX_RESULTS);
        int safeSectionIndex = clampSectionIndex(currentArticle, sectionIndex);
        Article.Section currentSection = currentArticle.getSections().get(safeSectionIndex);

        String articleQuery = buildArticleQuery(currentArticle);
        String pageQuery = buildPageQuery(currentArticle, currentSection);
        String highlightQuery = buildHighlightQuery(currentArticle, currentSection);

        if (articleQuery.isBlank() && pageQuery.isBlank()) {
            return Collections.emptyList();
        }

        List<SearchResult> articleResults = articleQuery.isBlank()
                ? List.of()
                : articleContextSearch.search(articleQuery, ARTICLE_QUERY_MIN_SCORE);
        List<SearchResult> pageResults = pageQuery.isBlank()
                ? List.of()
                : pageContextSearch.search(pageQuery, PAGE_QUERY_MIN_SCORE);

        if (articleResults.isEmpty() && pageResults.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Article, Double> articleScoreMap = toScoreMap(articleResults, currentArticle);
        Map<Article, Double> pageScoreMap = toScoreMap(pageResults, currentArticle);

        double articleTop = topScore(articleScoreMap);
        double pageTop = topScore(pageScoreMap);

        Set<Article> candidates = new LinkedHashSet<>();
        candidates.addAll(articleScoreMap.keySet());
        candidates.addAll(pageScoreMap.keySet());

        List<SearchResult> combined = new ArrayList<>();
        for (Article candidate : candidates) {
            double rawArticleScore = articleScoreMap.getOrDefault(candidate, 0.0);
            double rawPageScore = pageScoreMap.getOrDefault(candidate, 0.0);
            double strongestRawSignal = Math.max(rawArticleScore, rawPageScore);

            if (strongestRawSignal < MIN_ABSOLUTE_MATCH_SCORE) {
                continue;
            }

            double normalizedArticleScore = normalizeToTop(rawArticleScore, articleTop);
            double normalizedPageScore = normalizeToTop(rawPageScore, pageTop);
            double baseScore = ARTICLE_CONTEXT_WEIGHT * normalizedArticleScore
                    + PAGE_CONTEXT_WEIGHT * normalizedPageScore;
            if (baseScore <= 0.0) {
                continue;
            }

            double tagBoost = computeExactOverlapBoost(
                    currentArticle.getTags(),
                    candidate.getTags(),
                    ArticlePersonalizationService::canonicalTagKey,
                    TAG_BOOST_CAP);
            double keywordBoost = computeExactOverlapBoost(
                    currentArticle.getKeywords(),
                    candidate.getKeywords(),
                    ArticlePageRecommendationService::canonicalKeywordKey,
                    KEYWORD_BOOST_CAP);

            double finalScore = baseScore + tagBoost + keywordBoost;
            Map<String, Double> fieldScores = Map.of(
                    "articleContext", rawArticleScore,
                    "pageContext", rawPageScore,
                    "tagBoost", tagBoost,
                    "keywordBoost", keywordBoost);
            combined.add(new SearchResult(candidate, finalScore, fieldScores));
        }

        if (combined.isEmpty()) {
            return Collections.emptyList();
        }

        combined.sort(Comparator.comparingDouble(SearchResult::score).reversed());

        List<SearchResult> personalized = ArticlePersonalizationService.personalizeResults(
                combined,
                highlightQuery,
                preferencesSupplier.get());

        if (personalized.isEmpty()) {
            return Collections.emptyList();
        }

        SearchResult top = personalized.get(0);
        if (top.score() < RELEVANCE_THRESHOLD) {
            return Collections.emptyList();
        }

        List<SearchResult> strict = new ArrayList<>();
        strict.add(top);

        for (int i = 1; i < personalized.size() && strict.size() < limit; i++) {
            SearchResult candidate = personalized.get(i);
            boolean strongAbsolute = candidate.score() >= ADDITIONAL_RESULT_MIN_SCORE;
            boolean closeToTop = candidate.score() >= top.score() * ADDITIONAL_RESULT_TOP_RATIO;
            if (strongAbsolute && closeToTop) {
                strict.add(candidate);
            }
        }

        return List.copyOf(strict);
    }

    private static int clampSectionIndex(Article article, int sectionIndex) {
        return Math.max(0, Math.min(sectionIndex, article.getSections().size() - 1));
    }

    private static Map<Article, Double> toScoreMap(List<SearchResult> results, Article currentArticle) {
        Map<Article, Double> map = new HashMap<>();
        for (SearchResult result : results) {
            if (result.article() == null || result.article().equals(currentArticle)) {
                continue;
            }
            map.merge(result.article(), result.score(), Math::max);
        }
        return map;
    }

    private static double topScore(Map<Article, Double> scoreMap) {
        return scoreMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    private static double normalizeToTop(double value, double top) {
        if (value <= 0.0 || top <= 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value / top);
    }

    private static double computeExactOverlapBoost(List<String> sourceTerms,
            List<String> candidateTerms,
            Function<String, String> canonicalizer,
            double boostCap) {
        if (sourceTerms == null || sourceTerms.isEmpty() || candidateTerms == null || candidateTerms.isEmpty()) {
            return 0.0;
        }

        Set<String> sourceKeys = buildCanonicalSet(sourceTerms, canonicalizer);
        Set<String> candidateKeys = buildCanonicalSet(candidateTerms, canonicalizer);
        if (sourceKeys.isEmpty() || candidateKeys.isEmpty()) {
            return 0.0;
        }

        long sharedCount = sourceKeys.stream()
                .filter(candidateKeys::contains)
                .count();
        if (sharedCount == 0) {
            return 0.0;
        }

        double ratio = (double) sharedCount / Math.max(1, sourceKeys.size());
        return Math.min(boostCap, ratio * boostCap);
    }

    private static Set<String> buildCanonicalSet(List<String> terms, Function<String, String> canonicalizer) {
        Set<String> canonical = new LinkedHashSet<>();
        for (String term : terms) {
            String key = canonicalizer.apply(term);
            if (!key.isBlank()) {
                canonical.add(key);
            }
        }
        return canonical;
    }

    private static String buildArticleQuery(Article article) {
        String headings = article.getSections().stream()
                .limit(ARTICLE_HEADING_LIMIT)
                .map(Article.Section::heading)
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
        return joinParts(
                article.getTitle(),
                headings);
    }

    private static String buildPageQuery(Article article, Article.Section currentSection) {
        return joinParts(
                currentSection.heading(),
                trimSectionContent(currentSection.content()));
    }

    private static String buildHighlightQuery(Article article, Article.Section currentSection) {
        return joinParts(
                currentSection.heading(),
                article.getTitle(),
                trimSectionContent(currentSection.content()));
    }

    private static String joinTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return "";
        }
        return terms.stream()
                .filter(term -> term != null && !term.isBlank())
                .distinct()
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private static String joinParts(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String cleaned = cleanQueryText(part);
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

    private static String trimSectionContent(String content) {
        String cleaned = cleanQueryText(content);
        if (cleaned.length() <= PAGE_CONTENT_CHAR_LIMIT) {
            return cleaned;
        }
        return cleaned.substring(0, PAGE_CONTENT_CHAR_LIMIT).trim();
    }

    private static String cleanQueryText(String text) {
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

    private static String canonicalKeywordKey(String keyword) {
        return TextPreprocessor.normalize(keyword);
    }
}
