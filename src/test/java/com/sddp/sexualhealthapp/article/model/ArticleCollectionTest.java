package com.sddp.sexualhealthapp.article.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ArticleCollection class.
 * Tests singleton behavior, article loading, and retrieval.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
class ArticleCollectionTest {

    private ArticleCollection articleCollection;

    @BeforeEach
    void setUp() {
        articleCollection = ArticleCollection.getInstance();
    }

    @Test
    void testGetInstance_ReturnsSameInstance() {
        ArticleCollection instance1 = ArticleCollection.getInstance();
        ArticleCollection instance2 = ArticleCollection.getInstance();

        assertSame(instance1, instance2, "getInstance should return the same singleton instance");
    }

    @Test
    void testGetInstance_NotNull() {
        ArticleCollection instance = ArticleCollection.getInstance();

        assertNotNull(instance, "getInstance should never return null");
    }

    @Test
    void testGetArticles_NotNull() {
        List<Article> articles = articleCollection.getArticles();

        assertNotNull(articles, "getArticles should never return null");
    }

    @Test
    void testGetArticles_LoadsArticles() {
        List<Article> articles = articleCollection.getArticles();

        assertTrue(articles.size() > 0, "Should load at least one article from resources");
    }

    @Test
    void testGetArticles_ReturnsUnmodifiableList() {
        List<Article> articles = articleCollection.getArticles();

        assertThrows(UnsupportedOperationException.class, () -> {
            articles.add(new Article("# Test Article\n\n## Section\n\nContent"));
        }, "getArticles should return an unmodifiable list");
    }

    @Test
    void testGetArticles_AllArticlesHaveTitles() {
        List<Article> articles = articleCollection.getArticles();

        for (Article article : articles) {
            assertNotNull(article.getTitle(), "Each article should have a title");
            assertFalse(article.getTitle().isEmpty(), "Article titles should not be empty");
        }
    }

    @Test
    void testGetArticles_AllArticlesHaveSections() {
        List<Article> articles = articleCollection.getArticles();

        for (Article article : articles) {
            assertNotNull(article.getSections(), "Each article should have a sections list");
        }
    }

    @Test
    void testGetArticles_ConsistentBetweenCalls() {
        List<Article> articles1 = articleCollection.getArticles();
        List<Article> articles2 = articleCollection.getArticles();

        assertEquals(articles1.size(), articles2.size(),
                "Multiple calls to getArticles should return the same number of articles");
    }

    @Test
    void testGetArticles_ContainsExpectedArticles() {
        List<Article> articles = articleCollection.getArticles();
        List<String> titles = articles.stream()
                .map(Article::getTitle)
                .toList();

        // Check for some known articles from the article-mds directory
        assertTrue(titles.stream().anyMatch(title -> title.toLowerCase().contains("chlamydia") ||
                title.toLowerCase().contains("gonorrhoea") ||
                title.toLowerCase().contains("herpes") ||
                title.toLowerCase().contains("hiv")),
                "Should contain at least one expected STI article");
    }

    @Test
    void testArticleCollection_LoadsMultipleArticles() {
        List<Article> articles = articleCollection.getArticles();

        // Based on the workspace structure, there should be at least 10+ articles
        assertTrue(articles.size() >= 10,
                "Should load multiple articles (expected at least 10)");
    }

    @Test
    void testGetArticles_NoDuplicateTitles() {
        List<Article> articles = articleCollection.getArticles();
        List<String> titles = articles.stream()
                .map(Article::getTitle)
                .toList();

        long uniqueTitles = titles.stream().distinct().count();

        assertEquals(titles.size(), uniqueTitles,
                "All article titles should be unique (no duplicates)");
    }
}
