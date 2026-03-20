package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleBrowseRankingServiceTest {

    @Test
    void rankArticles_recentReadsAreTfidfPrimarySignal() {
        Article source = article("source.md", "Source", List.of("Support"), "Needle Safety", "sterile needle hygiene");
        Article needle = article("needle.md", "Needle Guide", List.of("Support"), "Needle", "needle hygiene");
        Article prep = article("prep.md", "PrEP Guide", List.of("Support"), "PrEP", "prep adherence");

        ArticleBrowseRankingService service = new ArticleBrowseRankingService((query, minScore) -> {
            if (query.toLowerCase().contains("sterile needle hygiene")) {
                return List.of(
                        new SearchResult(needle, 0.90, Map.of()),
                        new SearchResult(prep, 0.20, Map.of()));
            }
            return List.of();
        });

        List<Article> ranked = service.rankArticles(
                List.of(source, prep, needle),
                List.of(new RecentlyReadEntry(source.getFileName(), 0, Instant.parse("2026-03-20T10:00:00Z"))),
                ContentPreferences.empty());

        assertTrue(ranked.indexOf(needle) < ranked.indexOf(prep));
    }

    @Test
    void rankArticles_preferredTagsAffectOrderingWithoutOverridingRecentSignal() {
        Article source = article("source.md", "Source", List.of("Support"), "Needle Safety", "sterile needle hygiene");
        Article stronger = article("stronger.md", "Stronger Match", List.of("Support"), "Needle", "needle hygiene");
        Article preferred = article("preferred.md", "Preferred Match", List.of("LGBTQ+"), "Needle", "needle hygiene");

        ArticleBrowseRankingService service = new ArticleBrowseRankingService((query, minScore) -> List.of(
                new SearchResult(stronger, 0.88, Map.of()),
                new SearchResult(preferred, 0.86, Map.of())));

        List<Article> ranked = service.rankArticles(
                List.of(source, stronger, preferred),
                List.of(new RecentlyReadEntry(source.getFileName(), 0, Instant.parse("2026-03-20T10:00:00Z"))),
                new ContentPreferences(List.of(), List.of("LGBTQ+")));

        assertTrue(ranked.indexOf(preferred) < ranked.indexOf(stronger));
    }

    @Test
    void rankArticles_noRecentUsesFallbackAndPreferences() {
        Article preferred = article("preferred.md", "Preferred", List.of("LGBTQ+"), "One", "A");
        Article baseline = article("baseline.md", "Baseline", List.of("Support"), "One", "A");

        ArticleBrowseRankingService service = new ArticleBrowseRankingService((query, minScore) -> List.of());

        List<Article> ranked = service.rankArticles(
                List.of(baseline, preferred),
                List.of(),
                new ContentPreferences(List.of(), List.of("LGBTQ+")));

        assertEquals(preferred, ranked.get(0));
    }

    @Test
    void rankArticles_noSignalsUsesGeneralFallback() {
        AtomicInteger queryCount = new AtomicInteger();
        Article alpha = article("alpha.md", "Alpha", List.of("Support"), List.of("general"),
                "One", "Brief");
        Article beta = multiSectionArticle(
                "beta.md",
                "Beta",
                List.of("Support", "Relationships"),
                List.of("general", "safer sex"),
                List.of(
                        new Article.Section("One", "Detailed body text"),
                        new Article.Section("Two", "More detailed body text")));
        Article gamma = article("gamma.md", "Gamma", List.of("Support"), List.of(),
                "One", "Brief");

        ArticleBrowseRankingService service = new ArticleBrowseRankingService((query, minScore) -> {
            queryCount.incrementAndGet();
            return List.of();
        });

        List<Article> ranked = service.rankArticles(
                List.of(alpha, gamma, beta),
                List.of(),
                ContentPreferences.empty());

        assertEquals(beta, ranked.get(0));
        assertEquals(0, queryCount.get());
    }

    private static Article article(String fileName, String title, List<String> tags, List<String> keywords,
            String heading, String content) {
        Article article = new Article("""
                # %s

                ## %s

                %s
                """.formatted(title, heading, content));
        article.setFileName(fileName);
        article.setTags(tags);
        article.setKeywords(keywords);
        return article;
    }

    private static Article multiSectionArticle(String fileName, String title, List<String> tags, List<String> keywords,
            List<Article.Section> sections) {
        StringBuilder markdown = new StringBuilder("# ").append(title).append("\n\n");
        for (Article.Section section : sections) {
            markdown.append("## ").append(section.heading()).append("\n\n")
                    .append(section.content()).append("\n\n");
        }
        Article article = new Article(markdown.toString());
        article.setFileName(fileName);
        article.setTags(tags);
        article.setKeywords(keywords);
        return article;
    }

    private static Article article(String fileName, String title, List<String> tags, String heading, String content) {
        return article(fileName, title, tags, List.of(), heading, content);
    }
}
