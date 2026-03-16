package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArticlePersonalizationServiceTest {

    @Test
    void blockedTags_removeArticlesFromFilteredResults() {
        Article blocked = article("Blocked", List.of("STIs"));
        Article allowed = article("Allowed", List.of("Mental Health & Wellbeing"));

        List<Article> filtered = ArticlePersonalizationService.filterBlockedArticles(
                List.of(blocked, allowed),
                new ContentPreferences(List.of("STIs"), List.of()));

        assertEquals(List.of(allowed), filtered);
    }

    @Test
    void preferredTags_boostRelevantResults_withoutBoostingEveryone() {
        Article preferred = article("Preferred", List.of("LGBTQ+", "Everyone"));
        Article baseline = article("Baseline", List.of("Everyone"));

        List<SearchResult> personalized = ArticlePersonalizationService.personalizeResults(
                List.of(
                        new SearchResult(baseline, 0.50, Map.of()),
                        new SearchResult(preferred, 0.50, Map.of())),
                "support",
                new ContentPreferences(List.of(), List.of("LGBTQ+")));

        assertEquals("Preferred", personalized.get(0).article().getTitle());
        assertEquals(0.56, personalized.get(0).score(), 0.0001);
        assertEquals(0.50, personalized.get(1).score(), 0.0001);
        assertFalse(personalized.get(0).preferredMatchedTags().isEmpty());
    }

    @Test
    void queryMatchedTags_arePromotedIntoHighlightMetadata() {
        Article article = article("Query tags", List.of("STIs", "Contraception"));

        List<SearchResult> personalized = ArticlePersonalizationService.personalizeResults(
                List.of(new SearchResult(article, 0.40, Map.of())),
                "sti testing",
                ContentPreferences.empty());

        assertEquals(List.of("STIs"), personalized.get(0).highlightedTags());
    }

    private static Article article(String title, List<String> tags) {
        Article article = new Article("# " + title + "\n\n## Intro\n\nContent");
        article.setFileName(title + ".md");
        article.setTags(tags);
        return article;
    }
}
