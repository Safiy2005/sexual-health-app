package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.SearchResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ArticleSearchService.
 * Uses the real ArticleCollection (simpler than mocking for this use case).
 */
public class ArticleSearchServiceTest {

    private static ArticleSearchService searchService;

    @BeforeAll
    static void setUp() {
        searchService = new ArticleSearchService();
    }

    @Test
    void testSearch_EmptyQuery_ReturnsEmpty() {
        List<SearchResult> results = searchService.search("");

        assertTrue(results.isEmpty(), "Empty query should return no results");
    }

    @Test
    void testSearch_NullQuery_ReturnsEmpty() {
        List<SearchResult> results = searchService.search(null);

        assertTrue(results.isEmpty(), "Null query should return no results");
    }

    @Test
    void testSearch_WhitespaceQuery_ReturnsEmpty() {
        List<SearchResult> results = searchService.search("   ");

        assertTrue(results.isEmpty(), "Whitespace-only query should return no results");
    }

    @Test
    void testSearch_Results_SortedByRelevance() {
        List<SearchResult> results = searchService.search("treatment", 0.0);

        if (results.size() > 1) {
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).score() >= results.get(i + 1).score(),
                    "Results should be sorted by score (descending)");
            }
        }
    }

    @Test
    void testSearch_CaseInsensitive() {
        List<SearchResult> lowerResults = searchService.search("chlamydia", 0.0);
        List<SearchResult> upperResults = searchService.search("CHLAMYDIA", 0.0);
        List<SearchResult> mixedResults = searchService.search("ChLaMyDiA", 0.0);

        assertEquals(lowerResults.size(), upperResults.size(),
            "Case should not affect result count");
        assertEquals(lowerResults.size(), mixedResults.size(),
            "Case should not affect result count");

        if (!lowerResults.isEmpty()) {
            assertEquals(lowerResults.get(0).score(), upperResults.get(0).score(), 0.001,
                "Case should not affect scores");
        }
    }

    @Test
    void testSearch_StopwordsRemoved() {
        List<SearchResult> withStopwords = searchService.search("the symptoms", 0.0);
        List<SearchResult> withoutStopwords = searchService.search("symptoms", 0.0);

        assertEquals(withStopwords.size(), withoutStopwords.size(),
            "Stopwords should not affect result count");

        if (!withStopwords.isEmpty()) {
            assertEquals(withStopwords.get(0).score(), withoutStopwords.get(0).score(), 0.001,
                "Stopwords should not affect scores");
        }
    }

    @Test
    void testSearchTop_LimitsResults() {
        List<SearchResult> allResults = searchService.search("health", 0.0);

        if (allResults.size() >= 2) {
            List<SearchResult> topTwo = searchService.searchTop("health", 2);

            assertTrue(topTwo.size() <= 2,
                "searchTop should limit results to specified count");
            assertEquals(2, topTwo.size(),
                "Should return exactly 2 results when enough matches exist");
        } else {
            List<SearchResult> topTwo = searchService.searchTop("health", 2);
            assertTrue(topTwo.size() <= allResults.size(),
                "Should not return more results than available");
        }
    }

    @Test
    void testSearchTop_ZeroLimit_ReturnsEmpty() {
        List<SearchResult> results = searchService.searchTop("test", 0);

        assertTrue(results.isEmpty(), "Zero limit should return empty results");
    }

    @Test
    void testSearchTop_NegativeLimit_ReturnsEmpty() {
        List<SearchResult> results = searchService.searchTop("test", -1);

        assertTrue(results.isEmpty(), "Negative limit should return empty results");
    }

    @Test
    void testSearch_WithCustomMinScore() {
        List<SearchResult> highThreshold = searchService.search("infection", 0.5);
        List<SearchResult> lowThreshold = searchService.search("infection", 0.01);

        assertTrue(lowThreshold.size() >= highThreshold.size(),
            "Lower threshold should return same or more results");
    }

    @Test
    void testSearch_MultiWordQuery() {
        List<SearchResult> results = searchService.search("chlamydia treatment", 0.0);

        assertNotNull(results, "Results should not be null");
    }

    @Test
    void testSearchResult_Comparable() {
        List<SearchResult> results = searchService.search("content", 0.0);

        if (results.size() >= 2) {
            assertTrue(results.get(0).compareTo(results.get(1)) <= 0,
                "SearchResult should implement Comparable correctly");
        }
    }

    @Test
    void testSearch_DefaultMinScore() {
        List<SearchResult> results = searchService.search("health");

        results.forEach(result ->
            assertTrue(result.score() >= 0.01,
                "Results with default min score should all be >= 0.01")
        );
    }

    @Test
    void testSearchResult_FieldScoresPresent() {
        List<SearchResult> results = searchService.search("chlamydia", 0.0);

        if (!results.isEmpty()) {
            SearchResult firstResult = results.get(0);

            assertNotNull(firstResult.fieldScores(),
                "Field scores should be present");
            assertTrue(firstResult.fieldScores().containsKey("title"),
                "Should have title field score");
            assertTrue(firstResult.fieldScores().containsKey("headings"),
                "Should have headings field score");
            assertTrue(firstResult.fieldScores().containsKey("content"),
                "Should have content field score");
        }
    }

    @Test
    void testSearchResult_RelevancePercent() {
        List<SearchResult> results = searchService.search("chlamydia", 0.0);

        if (!results.isEmpty()) {
            SearchResult result = results.get(0);
            int percent = result.getRelevancePercent();

            assertTrue(percent >= 0,
                "Relevance percent should be non-negative");
        }
    }
}
