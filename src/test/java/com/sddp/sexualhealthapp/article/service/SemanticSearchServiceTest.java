package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SemanticSearchService using real articles and the ONNX embedding model.
 */
public class SemanticSearchServiceTest {

    private static SemanticSearchService searchService;
    private static ArticleCollection articleCollection;

    @BeforeAll
    static void setUp() {
        articleCollection = ArticleCollection.getInstance();
        searchService = new SemanticSearchService(articleCollection);
    }

    @Test
    void testSearch_EmptyQuery_ReturnsEmpty() {
        Map<Article, Double> results = searchService.search("");

        assertTrue(results.isEmpty(), "Empty query should return no results");
    }

    @Test
    void testSearch_NullQuery_ReturnsEmpty() {
        Map<Article, Double> results = searchService.search(null);

        assertTrue(results.isEmpty(), "Null query should return no results");
    }

    @Test
    void testSearch_WhitespaceQuery_ReturnsEmpty() {
        Map<Article, Double> results = searchService.search("   ");

        assertTrue(results.isEmpty(), "Whitespace-only query should return no results");
    }

    @Test
    void testSearch_ZeroLimit_ReturnsEmpty() {
        Map<Article, Double> results = searchService.search("chlamydia", 0);

        assertTrue(results.isEmpty(), "Zero limit should return no results");
    }

    @Test
    void testSearch_NegativeLimit_ReturnsEmpty() {
        Map<Article, Double> results = searchService.search("chlamydia", -1);

        assertTrue(results.isEmpty(), "Negative limit should return no results");
    }

    @Test
    void testSearch_ExactTerm_FindsArticle() {
        Map<Article, Double> results = searchService.search("chlamydia");

        assertFalse(results.isEmpty(), "Should find articles for 'chlamydia'");

        boolean foundChlamydia = results.keySet().stream()
                .anyMatch(a -> a.getTitle().toLowerCase().contains("chlamydia"));

        assertTrue(foundChlamydia, "Should find chlamydia article");
    }

    @Test
    void testSearch_ScoresArePositive() {
        Map<Article, Double> results = searchService.search("symptoms");

        assertFalse(results.isEmpty(), "Should find articles about symptoms");

        results.values().forEach(score ->
                assertTrue(score > 0.0, "All scores should be positive"));
    }

    @Test
    void testSearch_RespectsMaxResults() {
        Map<Article, Double> results = searchService.search("infection", 3);

        assertTrue(results.size() <= 3, "Should return at most 3 results");
    }

    @Test
    void testSearch_SemanticSynonym_STD() {
        // "STD" is a synonym for "STI" — semantic search should understand this
        Map<Article, Double> results = searchService.search("STD");

        assertFalse(results.isEmpty(),
                "Semantic search should find results for 'STD' even if articles use 'STI'");
    }

    @Test
    void testSearch_SemanticDescription_BurningUrination() {
        // Colloquial description should match medical content
        Map<Article, Double> results = searchService.search("burning feeling when I pee");

        assertFalse(results.isEmpty(),
                "Semantic search should find articles matching symptom descriptions");
    }

    @Test
    void testPreload_InitializesModel() {
        SemanticSearchService freshService = new SemanticSearchService(articleCollection);
        freshService.preload();

        assertTrue(freshService.isReady(),
                "Service should be initialized after preload()");
    }

    @Test
    void testSearch_ResultsAreSortedByScore() {
        Map<Article, Double> results = searchService.search("treatment", 10);

        Double previousScore = Double.MAX_VALUE;
        for (Double score : results.values()) {
            assertTrue(score <= previousScore,
                    "Results should be sorted by descending score");
            previousScore = score;
        }
    }
}
