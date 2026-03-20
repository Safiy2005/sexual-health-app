package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticlePageRecommendationServiceTest {

    @Test
    void recommendForPage_ReturnsAtMostThreeRecommendations() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("Everyone"),
                List.of("general"),
                "Overview", "General overview content");
        Article a = article("a.md", "A", List.of("Everyone"), List.of("general"), "One", "A content");
        Article b = article("b.md", "B", List.of("Everyone"), List.of("general"), "One", "B content");
        Article c = article("c.md", "C", List.of("Everyone"), List.of("general"), "One", "C content");
        Article d = article("d.md", "D", List.of("Everyone"), List.of("general"), "One", "D content");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService((query, minScore) -> List.of(
                new SearchResult(current, 0.99, Map.of()),
                new SearchResult(a, 0.92, Map.of()),
                new SearchResult(b, 0.90, Map.of()),
                new SearchResult(c, 0.88, Map.of()),
                new SearchResult(d, 0.86, Map.of())));

        List<SearchResult> recommendations = service.recommendForPage(current, 0, 10);

        assertEquals(3, recommendations.size());
    }

    @Test
    void recommendForPage_ExcludesCurrentArticle() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("Everyone"),
                List.of("general"),
                "Overview", "General overview content");
        Article related = article(
                "related.md",
                "Related Guide",
                List.of("Everyone"),
                List.of("general"),
                "Overview", "Related content");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService((query, minScore) -> List.of(
                new SearchResult(current, 0.99, Map.of()),
                new SearchResult(related, 0.90, Map.of())));

        List<SearchResult> recommendations = service.recommendForPage(current, 0, 3);

        assertEquals(1, recommendations.size());
        assertEquals("Related Guide", recommendations.get(0).article().getTitle());
    }

    @Test
    void recommendForPage_ReturnsEmptyWhenContextScoresAreTooWeak() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("Everyone"),
                List.of("general"),
                "Overview", "General overview content");
        Article weak = article(
                "weak.md",
                "Weak Match",
                List.of("Everyone"),
                List.of("general"),
                "Overview", "Weak content");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService((query, minScore) -> List.of(
                new SearchResult(weak, 0.08, Map.of())));

        List<SearchResult> recommendations = service.recommendForPage(current, 0, 3);

        assertTrue(recommendations.isEmpty());
    }

    @Test
    void recommendForPage_PageContextCanOutrankBroaderArticleMatch() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("Everyone"),
                List.of("injection safety"),
                "Overview", "General health overview.",
                "Needle Safety", "The page unique phrase is about sterile needles and injection safety.");
        Article broader = article(
                "broader.md",
                "General STI Basics",
                List.of("Everyone"),
                List.of("screening"),
                "Overview", "General advice");
        Article specific = article(
                "specific.md",
                "Safer Injecting Guide",
                List.of("Everyone"),
                List.of("injection safety"),
                "Overview", "Specific advice");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService((query, minScore) -> {
            String normalized = query.toLowerCase();
            if (normalized.contains("sterile needles and injection safety")) {
                return List.of(
                        new SearchResult(specific, 0.95, Map.of()),
                        new SearchResult(broader, 0.20, Map.of()));
            }
            return List.of(
                    new SearchResult(broader, 0.90, Map.of()),
                    new SearchResult(specific, 0.80, Map.of()));
        });

        List<SearchResult> recommendations = service.recommendForPage(current, 1, 3);

        assertFalse(recommendations.isEmpty());
        assertEquals("Safer Injecting Guide", recommendations.get(0).article().getTitle());
    }

    @Test
    void recommendForPage_TagAndKeywordOverlapBreakTies() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("STIs", "Support"),
                List.of("pep", "post exposure"),
                "Overview", "General overview content");
        Article overlap = article(
                "overlap.md",
                "Matched Metadata Guide",
                List.of("STIs", "Testing"),
                List.of("pep", "clinic"),
                "Overview", "Relevant content");
        Article baseline = article(
                "baseline.md",
                "Baseline Guide",
                List.of("Mental Health & Wellbeing"),
                List.of("support"),
                "Overview", "Relevant content");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService((query, minScore) -> List.of(
                new SearchResult(baseline, 0.88, Map.of()),
                new SearchResult(overlap, 0.88, Map.of())));

        List<SearchResult> recommendations = service.recommendForPage(current, 0, 3);

        assertFalse(recommendations.isEmpty());
        assertEquals("Matched Metadata Guide", recommendations.get(0).article().getTitle());
    }

    @Test
    void recommendForPage_BlockedTagsAreRespected() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("Everyone"),
                List.of("general"),
                "Overview", "General overview content");
        Article blocked = article(
                "blocked.md",
                "Blocked Guide",
                List.of("STIs"),
                List.of("screening"),
                "Overview", "Blocked content");
        Article allowed = article(
                "allowed.md",
                "Allowed Guide",
                List.of("Mental Health & Wellbeing"),
                List.of("support"),
                "Overview", "Allowed content");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService(
                (query, minScore) -> List.of(
                        new SearchResult(blocked, 0.92, Map.of()),
                        new SearchResult(allowed, 0.86, Map.of())),
                (query, minScore) -> List.of(
                        new SearchResult(blocked, 0.92, Map.of()),
                        new SearchResult(allowed, 0.86, Map.of())),
                () -> new ContentPreferences(List.of("STIs"), List.of()));

        List<SearchResult> recommendations = service.recommendForPage(current, 0, 3);

        assertEquals(1, recommendations.size());
        assertEquals("Allowed Guide", recommendations.get(0).article().getTitle());
    }

    @Test
    void recommendForPage_PreferredTagsCanImproveOrdering() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("Everyone"),
                List.of("general"),
                "Overview", "General overview content");
        Article preferred = article(
                "preferred.md",
                "Preferred Guide",
                List.of("LGBTQ+"),
                List.of("identity"),
                "Overview", "Preferred content");
        Article baseline = article(
                "baseline.md",
                "Baseline Guide",
                List.of("Mental Health & Wellbeing"),
                List.of("support"),
                "Overview", "Baseline content");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService(
                (query, minScore) -> List.of(
                        new SearchResult(baseline, 0.85, Map.of()),
                        new SearchResult(preferred, 0.82, Map.of())),
                (query, minScore) -> List.of(
                        new SearchResult(baseline, 0.85, Map.of()),
                        new SearchResult(preferred, 0.82, Map.of())),
                () -> new ContentPreferences(List.of(), List.of("LGBTQ+")));

        List<SearchResult> recommendations = service.recommendForPage(current, 0, 3);

        assertFalse(recommendations.isEmpty());
        assertEquals("Preferred Guide", recommendations.get(0).article().getTitle());
    }

    @Test
    void recommendForPage_DifferentSectionsCanProduceDifferentTopResults() {
        Article current = article(
                "current.md",
                "Current Guide",
                List.of("Support"),
                List.of("health"),
                "Needle Safety", "sterile needles and injection hygiene",
                "PrEP Review", "daily prep adherence and clinic review");
        Article needleMatch = article(
                "needle.md",
                "Needle Safety Guide",
                List.of("Support"),
                List.of("health"),
                "Overview", "needle support");
        Article prepMatch = article(
                "prep.md",
                "PrEP Review Guide",
                List.of("Support"),
                List.of("health"),
                "Overview", "prep support");

        ArticlePageRecommendationService service = new ArticlePageRecommendationService(
                (query, minScore) -> List.of(
                        new SearchResult(needleMatch, 0.80, Map.of()),
                        new SearchResult(prepMatch, 0.80, Map.of())),
                (query, minScore) -> {
                    String normalized = query.toLowerCase();
                    if (normalized.contains("sterile needles")) {
                        return List.of(new SearchResult(needleMatch, 0.95, Map.of()));
                    }
                    if (normalized.contains("daily prep adherence")) {
                        return List.of(new SearchResult(prepMatch, 0.95, Map.of()));
                    }
                    return List.of();
                });

        List<SearchResult> firstPageRecommendations = service.recommendForPage(current, 0, 3);
        List<SearchResult> secondPageRecommendations = service.recommendForPage(current, 1, 3);

        assertFalse(firstPageRecommendations.isEmpty());
        assertFalse(secondPageRecommendations.isEmpty());
        assertEquals("Needle Safety Guide", firstPageRecommendations.get(0).article().getTitle());
        assertEquals("PrEP Review Guide", secondPageRecommendations.get(0).article().getTitle());
    }

    private static Article article(String fileName,
            String title,
            List<String> tags,
            List<String> keywords,
            String... sections) {
        StringBuilder markdown = new StringBuilder("# ").append(title).append("\n\n");
        for (int i = 0; i < sections.length; i += 2) {
            markdown.append("## ").append(sections[i]).append("\n\n")
                    .append(sections[i + 1]).append("\n\n");
        }

        Article article = new Article(markdown.toString());
        article.setFileName(fileName);
        article.setTags(tags);
        article.setKeywords(keywords);
        return article;
    }
}
