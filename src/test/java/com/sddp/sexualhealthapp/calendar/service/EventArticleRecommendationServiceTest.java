package com.sddp.sexualhealthapp.calendar.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import com.sddp.sexualhealthapp.calendar.model.CalendarEvent;
import com.sddp.sexualhealthapp.calendar.model.EventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventArticleRecommendationServiceTest {

    @Test
    void recommendForEvent_TitleSignalDominatesDescription() {
        Article prep = article("PrEP and HIV prevention", List.of("STIs"));
        Article anxiety = article("Anxiety and wellbeing", List.of("Mental Health & Wellbeing"));

        EventArticleRecommendationService service = new EventArticleRecommendationService((query, minScore) -> {
            String q = query.toLowerCase();
            if (q.contains("prep")) {
                return List.of(
                        new SearchResult(prep, 0.90, Map.of()),
                        new SearchResult(anxiety, 0.20, Map.of()));
            }
            if (q.contains("anxious")) {
                return List.of(
                        new SearchResult(anxiety, 0.92, Map.of()),
                        new SearchResult(prep, 0.10, Map.of()));
            }
            return List.of();
        });

        CalendarEvent event = new CalendarEvent(
                "PrEP review",
                LocalDate.of(2026, 3, 10),
                null,
                EventType.TEST,
                "Feeling anxious before clinic visit",
                null);

        List<EventArticleRecommendationService.Recommendation> recommendations = service.recommendForEvent(event, 3);

        assertFalse(recommendations.isEmpty());
        assertEquals("PrEP and HIV prevention", recommendations.get(0).article().getTitle());
    }

    @Test
    void recommendForEvent_ReturnsAtMostThreeRecommendations() {
        Article a = article("A", List.of("STIs"));
        Article b = article("B", List.of("STIs"));
        Article c = article("C", List.of("STIs"));
        Article d = article("D", List.of("STIs"));

        EventArticleRecommendationService service = new EventArticleRecommendationService((query, minScore) -> List.of(
                new SearchResult(a, 0.90, Map.of()),
                new SearchResult(b, 0.82, Map.of()),
                new SearchResult(c, 0.80, Map.of()),
                new SearchResult(d, 0.78, Map.of())));

        CalendarEvent event = new CalendarEvent(
                "STI screening",
                LocalDate.of(2026, 3, 5),
                null,
                EventType.TEST,
                "Routine test",
                null);

        List<EventArticleRecommendationService.Recommendation> recommendations = service.recommendForEvent(event, 10);

        assertTrue(recommendations.size() <= 3);
        assertEquals(3, recommendations.size());
    }

    @Test
    void recommendForEvent_FallbackReturnsOneWhenThresholdFiltersOut() {
        Article only = article("General support", List.of("Everyone"));

        EventArticleRecommendationService service = new EventArticleRecommendationService((query, minScore) -> {
            if (query.toLowerCase().contains("context")) {
                return List.of(new SearchResult(only, 0.40, Map.of()));
            }
            return List.of();
        });

        CalendarEvent event = new CalendarEvent(
                "   ",
                LocalDate.of(2026, 3, 8),
                null,
                EventType.APPOINTMENT,
                "context only",
                null);

        List<EventArticleRecommendationService.Recommendation> recommendations = service.recommendForEvent(event, 3);

        assertEquals(1, recommendations.size());
        assertEquals("General support", recommendations.get(0).article().getTitle());
    }

    private static Article article(String title, List<String> tags) {
        Article article = new Article("# " + title + "\n\n## Intro\n\nContent");
        article.setFileName(title + ".md");
        article.setTags(tags);
        return article;
    }
}
