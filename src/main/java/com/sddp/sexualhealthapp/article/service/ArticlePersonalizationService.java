package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies blocked-tag filtering and preferred-tag ranking boosts consistently.
 */
public final class ArticlePersonalizationService {

    public static final String EVERYONE_TAG = "Everyone";
    private static final double PREFERRED_TAG_BOOST = 0.06;
    private static final double MAX_PREFERRED_BOOST = 0.18;

    private ArticlePersonalizationService() {
    }

    public static List<Article> filterBlockedArticles(List<Article> articles, ContentPreferences preferences) {
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        return articles.stream()
                .filter(article -> !isBlocked(article, preferences))
                .toList();
    }

    public static List<SearchResult> personalizeResults(List<SearchResult> results, String searchQuery,
            ContentPreferences preferences) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<SearchResult> personalized = new ArrayList<>();
        for (SearchResult result : results) {
            if (isBlocked(result.article(), preferences)) {
                continue;
            }

            List<String> preferredMatches = getPreferredMatchedTags(result.article(), preferences);
            List<String> queryMatches = getQueryMatchedTags(result.article(), searchQuery);
            List<String> highlighted = mergeHighlightedTags(queryMatches, preferredMatches);

            double boost = Math.min(MAX_PREFERRED_BOOST, preferredMatches.size() * PREFERRED_TAG_BOOST);
            personalized.add(new SearchResult(
                    result.article(),
                    result.score() + boost,
                    result.fieldScores(),
                    highlighted,
                    preferredMatches));
        }

        return personalized.stream()
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .toList();
    }

    public static boolean isBlocked(Article article, ContentPreferences preferences) {
        if (article == null || preferences == null || preferences.blockedTags().isEmpty()) {
            return false;
        }

        Set<String> blocked = preferences.blockedTags().stream()
                .map(ArticlePersonalizationService::canonicalTagKey)
                .collect(Collectors.toSet());

        for (String tag : article.getTags()) {
            if (isEveryoneTag(tag)) {
                continue;
            }
            if (blocked.contains(canonicalTagKey(tag))) {
                return true;
            }
        }

        String title = article.getTitle();
        if (title != null && !title.isBlank()) {
            String titleWords = " " + title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ") + " ";
            for (String blockedTag : preferences.blockedTags()) {
                if (blockedTag == null || blockedTag.isBlank()) continue;
                String blockedWords = " " + blockedTag.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ") + " ";
                if (titleWords.contains(blockedWords)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static List<String> getPreferredMatchedTags(Article article, ContentPreferences preferences) {
        if (article == null || preferences == null || preferences.preferredTags().isEmpty()) {
            return List.of();
        }

        Set<String> preferred = preferences.preferredTags().stream()
                .map(ArticlePersonalizationService::canonicalTagKey)
                .collect(Collectors.toSet());

        return article.getTags().stream()
                .filter(tag -> !isEveryoneTag(tag))
                .filter(tag -> preferred.contains(canonicalTagKey(tag)))
                .distinct()
                .toList();
    }

    public static List<String> getQueryMatchedTags(Article article, String searchQuery) {
        if (article == null || searchQuery == null || searchQuery.isBlank()) {
            return List.of();
        }

        return article.getTags().stream()
                .filter(tag -> !isEveryoneTag(tag))
                .filter(tag -> isTagRelevantToQuery(tag, searchQuery))
                .distinct()
                .toList();
    }

    public static List<String> buildCuratedTagList(Collection<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> countsByKey = new LinkedHashMap<>();
        Map<String, String> displayByKey = new LinkedHashMap<>();

        for (Article article : articles) {
            for (String tag : article.getTags()) {
                if (isEveryoneTag(tag)) {
                    continue;
                }

                String key = canonicalTagKey(tag);
                countsByKey.merge(key, 1, Integer::sum);
                displayByKey.merge(key, tag, ArticlePersonalizationService::pickPreferredDisplay);
            }
        }

        return countsByKey.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> displayByKey.get(entry.getKey()), String.CASE_INSENSITIVE_ORDER))
                .map(entry -> displayByKey.get(entry.getKey()))
                .toList();
    }

    private static List<String> mergeHighlightedTags(List<String> queryMatches, List<String> preferredMatches) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(preferredMatches);
        merged.addAll(queryMatches);
        return List.copyOf(merged);
    }

    private static String pickPreferredDisplay(String current, String candidate) {
        int currentScore = displayQualityScore(current);
        int candidateScore = displayQualityScore(candidate);

        if (candidateScore > currentScore) {
            return candidate;
        }
        if (candidateScore == currentScore && candidate.length() > current.length()) {
            return candidate;
        }
        return current;
    }

    private static int displayQualityScore(String value) {
        int score = 0;
        if (value.contains("&")) {
            score += 2;
        }
        if (value.contains("'")) {
            score += 1;
        }
        if (!value.equals(value.toLowerCase(Locale.ROOT))) {
            score += 1;
        }
        return score;
    }

    private static boolean isEveryoneTag(String tag) {
        return canonicalTagKey(tag).equals(canonicalTagKey(EVERYONE_TAG));
    }

    public static String canonicalTagKey(String tag) {
        if (tag == null) {
            return "";
        }

        String normalized = Normalizer.normalize(tag, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
        return normalized;
    }

    public static boolean isTagRelevantToQuery(String tag, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank() || tag == null || tag.isBlank()) {
            return false;
        }

        String tagLower = tag.toLowerCase(Locale.ROOT);
        String queryLower = searchQuery.toLowerCase(Locale.ROOT).trim();

        String[] queryWords = queryLower.split("\\s+");
        for (String word : queryWords) {
            if (word.length() >= 2 && (tagLower.contains(word) || word.contains(tagLower))) {
                return true;
            }
        }

        return false;
    }
}
