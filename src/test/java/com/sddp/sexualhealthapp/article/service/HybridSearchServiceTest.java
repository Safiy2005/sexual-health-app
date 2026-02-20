package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HybridSearchService.
 * Tests normalization utilities directly and hybrid behavior with real
 * services.
 */
@Tag("Slow")
public class HybridSearchServiceTest {

    private static HybridSearchService hybridService;

    @BeforeAll
    static void setUp() {
        ArticleCollection collection = ArticleCollection.getInstance();
        hybridService = new HybridSearchService(
                new ArticleSearchService(collection),
                new SemanticSearchService(collection));
    }

    // --- Normalization utility tests (static methods, no services needed) ---

    @Test
    void testNormalize_StandardRange() {
        assertEquals(0.0, HybridSearchService.normalize(0.0, 0.0, 1.0), 0.001);
        assertEquals(0.5, HybridSearchService.normalize(0.5, 0.0, 1.0), 0.001);
        assertEquals(1.0, HybridSearchService.normalize(1.0, 0.0, 1.0), 0.001);
    }

    @Test
    void testNormalize_CustomRange() {
        assertEquals(0.0, HybridSearchService.normalize(2.0, 2.0, 6.0), 0.001);
        assertEquals(0.5, HybridSearchService.normalize(4.0, 2.0, 6.0), 0.001);
        assertEquals(1.0, HybridSearchService.normalize(6.0, 2.0, 6.0), 0.001);
    }

    @Test
    void testNormalize_EqualMinMax_PositiveValue() {
        assertEquals(1.0, HybridSearchService.normalize(5.0, 5.0, 5.0), 0.001);
    }

    @Test
    void testNormalize_EqualMinMax_ZeroValue() {
        assertEquals(0.0, HybridSearchService.normalize(0.0, 0.0, 0.0), 0.001);
    }

    @Test
    void testGetRange_EmptyCollection() {
        double[] range = HybridSearchService.getRange(List.of());
        assertEquals(0.0, range[0], 0.001);
        assertEquals(0.0, range[1], 0.001);
    }

    @Test
    void testGetRange_SingleValue() {
        double[] range = HybridSearchService.getRange(List.of(5.0));
        assertEquals(5.0, range[0], 0.001);
        assertEquals(5.0, range[1], 0.001);
    }

    @Test
    void testGetRange_MultipleValues() {
        double[] range = HybridSearchService.getRange(List.of(1.0, 5.0, 3.0));
        assertEquals(1.0, range[0], 0.001);
        assertEquals(5.0, range[1], 0.001);
    }

    @Test
    void testWeights_SumToOne() {
        assertEquals(1.0, HybridSearchService.TFIDF_WEIGHT + HybridSearchService.SEMANTIC_WEIGHT, 0.001,
                "TF-IDF and semantic weights should sum to 1.0");
    }

    // --- Hybrid search behavior tests with real services ---

    @Test
    void testSearch_EmptyQuery_ReturnsEmpty() {
        assertTrue(hybridService.search("").isEmpty());
    }

    @Test
    void testSearch_NullQuery_ReturnsEmpty() {
        assertTrue(hybridService.search(null).isEmpty());
    }

    @Test
    void testSearch_WhitespaceQuery_ReturnsEmpty() {
        assertTrue(hybridService.search("   ").isEmpty());
    }

    @Test
    void testSearch_ReturnsResults() {
        List<SearchResult> results = hybridService.search("chlamydia");

        assertFalse(results.isEmpty(), "Should find results for 'chlamydia'");
    }

    @Test
    void testSearch_ResultsContainBothScoreTypes() {
        List<SearchResult> results = hybridService.search("chlamydia");

        if (!results.isEmpty()) {
            SearchResult result = results.get(0);
            assertTrue(result.fieldScores().containsKey("tfidf"),
                    "Field scores should contain 'tfidf' key");
            assertTrue(result.fieldScores().containsKey("semantic"),
                    "Field scores should contain 'semantic' key");
        }
    }

    @Test
    void testSearch_ResultsSortedByScore() {
        List<SearchResult> results = hybridService.search("treatment");

        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).score() >= results.get(i + 1).score(),
                    "Results should be sorted by descending hybrid score");
        }
    }

    @Test
    void testSearch_HybridScoresInValidRange() {
        List<SearchResult> results = hybridService.search("symptoms");

        results.forEach(result -> {
            assertTrue(result.score() >= 0.0, "Hybrid score should be non-negative");
            assertTrue(result.score() <= 1.0, "Hybrid score should not exceed 1.0");
        });
    }

    @Test
    void testSearchTop_LimitsResults() {
        List<SearchResult> results = hybridService.searchTop("infection", 2);

        assertTrue(results.size() <= 2, "searchTop should limit to 2 results");
    }

    @Test
    void testSearchTop_ZeroLimit_ReturnsEmpty() {
        assertTrue(hybridService.searchTop("test", 0).isEmpty());
    }

    @Test
    void testSearchTop_NegativeLimit_ReturnsEmpty() {
        assertTrue(hybridService.searchTop("test", -1).isEmpty());
    }
}
