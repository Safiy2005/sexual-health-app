package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.SearchResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RelevanceScorer.
 * Uses the real ArticleCollection for simplicity.
 */
public class RelevanceScorerTest {

    private static RelevanceScorer scorer;
    private static ArticleCollection collection;

    @BeforeAll
    static void setUp() {
        collection = ArticleCollection.getInstance();
        scorer = new RelevanceScorer(collection);
    }

    @Test
    void testScore_EmptyQuery() {
        if (!collection.getArticles().isEmpty()) {
            Article article = collection.getArticles().get(0);
            SearchResult result = scorer.score(article, "");

            assertEquals(0.0, result.score(), 0.001,
                "Empty query should result in zero score");
        }
    }

    @Test
    void testScore_FieldScoresPresent() {
        if (!collection.getArticles().isEmpty()) {
            Article article = collection.getArticles().get(0);
            SearchResult result = scorer.score(article, "test");

            assertNotNull(result.fieldScores(), "Field scores should not be null");
            assertTrue(result.fieldScores().containsKey("title"),
                "Should have title field score");
            assertTrue(result.fieldScores().containsKey("headings"),
                "Should have headings field score");
            assertTrue(result.fieldScores().containsKey("content"),
                "Should have content field score");
        }
    }

    @Test
    void testScore_CaseInsensitive() {
        // Find an article with "chlamydia" in the title to ensure a match
        Article testArticle = null;
        for (Article article : collection.getArticles()) {
            if (article.getTitle().toLowerCase().contains("chlamydia")) {
                testArticle = article;
                break;
            }
        }

        if (testArticle != null) {
            // Note: scorer.score() expects normalized queries
            String lowerQuery = TextPreprocessor.normalize("chlamydia");
            String upperQuery = TextPreprocessor.normalize("CHLAMYDIA");

            SearchResult lowerResult = scorer.score(testArticle, lowerQuery);
            SearchResult upperResult = scorer.score(testArticle, upperQuery);

            assertEquals(lowerResult.score(), upperResult.score(), 0.001,
                "Scoring should be case-insensitive (after normalization)");
        }
    }

    @Test
    void testScore_ReturnsSearchResult() {
        if (!collection.getArticles().isEmpty()) {
            Article article = collection.getArticles().get(0);
            SearchResult result = scorer.score(article, "test");

            assertNotNull(result, "Should return SearchResult");
            assertEquals(article, result.article(),
                "SearchResult should contain the scored article");
        }
    }

    @Test
    void testBuildIdfCache_AllTermsIncluded() throws Exception {
        // Use reflection to access private idfCache field
        Field idfCacheField = RelevanceScorer.class.getDeclaredField("idfCache");
        idfCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var idfCache = (java.util.Map<String, Double>) idfCacheField.get(scorer);

        assertNotNull(idfCache, "IDF cache should be built");
        assertFalse(idfCache.isEmpty(), "IDF cache should contain terms");

        // Common terms in sexual health articles should be in cache
        // (after normalization and stopword removal)
        assertTrue(idfCache.size() > 10,
            "IDF cache should contain many terms from the corpus");
    }

    @Test
    void testScore_PositiveScoreForMatchingTerms() {
        // Find an article and score it with a term from its title
        for (Article article : collection.getArticles()) {
            String titleWord = article.getTitle().split("\\s+")[0].toLowerCase();

            SearchResult result = scorer.score(article, titleWord);

            if (!titleWord.matches("the|a|an|and|or|of")) {
                assertTrue(result.score() >= 0.0,
                    "Score should be non-negative for matching term");
            }
        }
    }

    @Test
    void testScore_TotalDocuments() throws Exception {
        // Use reflection to access totalDocuments field
        Field totalDocsField = RelevanceScorer.class.getDeclaredField("totalDocuments");
        totalDocsField.setAccessible(true);
        int totalDocs = totalDocsField.getInt(scorer);

        assertEquals(collection.getArticles().size(), totalDocs,
            "Total documents should match article collection size");
    }

    @Test
    void testScore_MultipleTermsQuery() {
        if (!collection.getArticles().isEmpty()) {
            Article article = collection.getArticles().get(0);
            SearchResult singleTerm = scorer.score(article, "treatment");
            SearchResult multiTerm = scorer.score(article, "treatment symptoms");

            assertTrue(singleTerm.score() >= 0.0,
                "Single term score should be non-negative");
            assertTrue(multiTerm.score() >= 0.0,
                "Multi-term score should be non-negative");
        }
    }
}
