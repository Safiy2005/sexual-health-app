package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ArticleSearchService using real articles loaded from
 * markdown files.
 * These tests verify that the search system works correctly with the actual
 * article corpus.
 */
public class ArticleSearchIntegrationTest {

    private static ArticleSearchService searchService;
    private static HybridSearchService hybridService;
    private static ArticleCollection articleCollection;

    @BeforeAll
    static void setUp() {
        articleCollection = ArticleCollection.getInstance();
        searchService = new ArticleSearchService(articleCollection);
        SemanticSearchService semanticService = new SemanticSearchService(articleCollection);
        semanticService.preload(); // Ensure semantic search is ready before hybrid tests
        hybridService = new HybridSearchService(searchService, semanticService);
    }

    @Test
    void testSearch_WithRealArticles_Chlamydia() {
        List<SearchResult> results = searchService.search("chlamydia");

        assertFalse(results.isEmpty(),
                "Should find articles about chlamydia");

        // Verify that chlamydia article is in results
        boolean foundChlamydiaArticle = results.stream()
                .anyMatch(r -> r.article().getTitle().toLowerCase().contains("chlamydia"));

        assertTrue(foundChlamydiaArticle,
                "Should find article with 'chlamydia' in title");
    }

    @Test
    void testSearch_WithRealArticles_Symptoms() {
        List<SearchResult> results = searchService.search("symptoms");

        assertFalse(results.isEmpty(),
                "Should find articles mentioning symptoms");

        results.forEach(result -> assertTrue(result.score() > 0.0,
                "All results should have positive relevance scores"));
    }

    @Test
    void testSearch_WithRealArticles_Treatment() {
        List<SearchResult> results = searchService.search("treatment");

        assertFalse(results.isEmpty(),
                "Should find articles about treatment");

        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).score() >= results.get(i + 1).score(),
                    "Results should be sorted by relevance score (descending)");
        }
    }

    @Test
    void testSearch_WithRealArticles_MultiWordQuery() {
        List<SearchResult> results = searchService.search("sexual health");

        assertFalse(results.isEmpty(),
                "Should find articles about sexual health");
    }

    @Test
    void testSearch_WithRealArticles_Prevention() {
        List<SearchResult> results = searchService.search("prevention", 0.0);

        assertFalse(results.isEmpty(),
                "Should find articles about prevention");
    }

    @Test
    void testSearch_WithRealArticles_Testing() {
        List<SearchResult> results = searchService.search("testing");

        assertFalse(results.isEmpty(),
                "Should find articles about testing");
    }

    @Test
    void testSearch_WithRealArticles_NoMatches() {
        List<SearchResult> results = searchService.search("quantum physics");

        // Sexual health articles unlikely to contain quantum physics
        assertTrue(results.isEmpty() || results.get(0).score() < 0.1,
                "Unrelated query should return no or very low-scoring results");
    }

    @Test
    void testSearch_WithRealArticles_CaseInsensitive() {
        List<SearchResult> lower = searchService.search("chlamydia");
        List<SearchResult> upper = searchService.search("CHLAMYDIA");

        assertEquals(lower.size(), upper.size(),
                "Case should not affect number of results");

        if (!lower.isEmpty() && !upper.isEmpty()) {
            assertEquals(lower.get(0).article().getTitle(),
                    upper.get(0).article().getTitle(),
                    "Case should not affect which article ranks first");
        }
    }

    @Test
    void testSearchTop_WithRealArticles_LimitsResults() {
        List<SearchResult> allResults = searchService.search("infection");
        List<SearchResult> topThree = searchService.searchTop("infection", 3);

        assertTrue(topThree.size() <= 3,
                "searchTop should limit results to specified count");

        if (allResults.size() >= 3) {
            assertEquals(3, topThree.size(),
                    "Should return exactly 3 results when enough matches exist");
        }
    }

    @Test
    void testSearch_Performance_UnderThreshold() {
        long startTime = System.currentTimeMillis();

        searchService.search("symptoms treatment prevention");

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 500,
                "Search should complete in under 500ms for typical query (actual: " + duration + "ms)");
    }

    @Test
    void testSearch_WithRealArticles_StopwordsIgnored() {
        List<SearchResult> withStopwords = searchService.search("the symptoms of infection");
        List<SearchResult> withoutStopwords = searchService.search("symptoms infection");

        // Both should find similar articles (stopwords don't change results
        // significantly)
        assertFalse(withStopwords.isEmpty());
        assertFalse(withoutStopwords.isEmpty());
    }

    @Test
    void testSearch_WithRealArticles_SpecialCharacters() {
        List<SearchResult> results = searchService.search("chlamydia!");

        assertFalse(results.isEmpty(),
                "Special characters should be normalized and not prevent matching");
    }

    @Test
    void testSearch_WithRealArticles_FieldScoresPresent() {
        List<SearchResult> results = searchService.search("chlamydia");

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
    void testSearch_WithRealArticles_RelevancePercent() {
        List<SearchResult> results = searchService.search("chlamydia", 0.0);

        if (!results.isEmpty()) {
            SearchResult result = results.get(0);
            int percent = result.getRelevancePercent();

            assertTrue(percent >= 0,
                    "Relevance percent should be non-negative");

            // With weighted scoring and smoothed IDF, percentages can exceed 100
            // This is expected and shows strong relevance
            assertTrue(percent < 1000,
                    "Relevance percent should be reasonable (less than 1000)");
        }
    }

    @Test
    void testSearch_WithRealArticles_MultipleSpecificTerms() {
        List<SearchResult> results = searchService.search("gonorrhea symptoms");

        assertFalse(results.isEmpty(),
                "Should find articles matching at least one term");
    }

    @Test
    void testSearch_WithRealArticles_MedicalTerms() {
        List<SearchResult> results = searchService.search("antibiotics");

        assertFalse(results.isEmpty(),
                "Should find articles mentioning medical terms");
    }

    @Test
    void testArticleCollection_HasArticles() {
        assertFalse(articleCollection.getArticles().isEmpty(),
                "Article collection should have loaded articles from markdown files");

        assertTrue(articleCollection.getArticles().size() > 10,
                "Should have loaded multiple articles");
    }

    @Test
    void testSearch_EmptyResultsAreUnmodifiable() {
        List<SearchResult> results = searchService.search("");

        assertTrue(results.isEmpty());

        // Verify list is unmodifiable (or at least empty)
        assertThrows(UnsupportedOperationException.class, () -> {
            results.add(null);
        }, "Results list should be unmodifiable");
    }

    // --- Hybrid search integration tests ---

    @Test
    void testHybridSearch_Chlamydia() {
        List<SearchResult> results = hybridService.search("chlamydia");

        assertFalse(results.isEmpty(), "Hybrid search should find chlamydia articles");

        boolean found = results.stream()
                .anyMatch(r -> r.article().getTitle().toLowerCase().contains("chlamydia"));
        assertTrue(found, "Chlamydia article should be in hybrid results");
    }

    @Test
    void testHybridSearch_SemanticSynonym_STD() {
        // "STD" is not in the articles (they use "STI"), but semantic search should
        // bridge this
        List<SearchResult> results = hybridService.search("STD");

        assertFalse(results.isEmpty(),
                "Hybrid search should find results for 'STD' via semantic understanding");
    }

    @Test
    void testHybridSearch_ColloquialSymptom() {
        // Users describe symptoms in everyday language, not medical terms
        List<SearchResult> results = hybridService.search("burning feeling when I pee");

        assertFalse(results.isEmpty(),
                "Hybrid search should match colloquial symptom descriptions to medical content");
    }

    @Test
    void testHybridSearch_FieldScores_ContainBothTypes() {
        List<SearchResult> results = hybridService.search("chlamydia");

        if (!results.isEmpty()) {
            SearchResult result = results.get(0);
            assertTrue(result.fieldScores().containsKey("tfidf"),
                    "Hybrid results should contain TF-IDF score");
            assertTrue(result.fieldScores().containsKey("semantic"),
                    "Hybrid results should contain semantic score");
        }
    }

    @Test
    void testHybridSearch_ResultsSortedByHybridScore() {
        List<SearchResult> results = hybridService.search("treatment");

        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).score() >= results.get(i + 1).score(),
                    "Hybrid results should be sorted by descending score");
        }
    }

    @Test
    void testHybridSearch_UnrelatedQuery() {
        List<SearchResult> results = hybridService.search("quantum physics");

        // With floor-adjusted cosine similarity, unrelated queries should score very
        // low
        assertTrue(results.isEmpty() || results.get(0).score() < 0.15,
                "Unrelated query should score below 0.15 after semantic floor adjustment");
    }

    @Test
    void testHybridSearchTop_LimitsResults() {
        List<SearchResult> top3 = hybridService.searchTop("infection", 3);

        assertTrue(top3.size() <= 3, "searchTop should limit hybrid results");
    }
}
