package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the persistent caching logic in SemanticSearchService.
 * Uses real ArticleCollection to avoid Mockito Java 23 incompatibility.
 */
class SemanticSearchCacheTest {

    private static final String CACHE_FILE_NAME = "semantic_embeddings.cache";
    private static final int CACHE_VERSION = 2;
    private Path cachePath;
    private ArticleCollection realArticleCollection;

    @BeforeEach
    void setUp() throws IOException {
        SemanticSearchService.reset();
        cachePath = Paths.get(CACHE_FILE_NAME);
        // Ensure clean state
        Files.deleteIfExists(cachePath);
        realArticleCollection = ArticleCollection.getInstance();
    }

    @AfterEach
    void tearDown() throws IOException {
        SemanticSearchService.reset();
        // Cleanup
        Files.deleteIfExists(cachePath);
    }

    @Test
    void testCacheIsCreatedOnFirstRun() {
        SemanticSearchService service = new SemanticSearchService(realArticleCollection);

        // Trigger initialization
        service.search("test");

        assertTrue(Files.exists(cachePath), "Cache file should be created after first initialization");
        try {
            assertTrue(Files.size(cachePath) > 0, "Cache file should not be empty");
        } catch (IOException e) {
            fail("Failed to check cache file size");
        }
    }

    @Test
    void testSearchWorksAfterCacheCreation() {
        SemanticSearchService service = new SemanticSearchService(realArticleCollection);

        // First run generates cache
        service.search("test");
        assertTrue(Files.exists(cachePath));

        // Create new service instance (should load from cache)
        SemanticSearchService.reset(); // Clear static state to force reload
        SemanticSearchService service2 = new SemanticSearchService(realArticleCollection);
        Map<Article, Double> results = service2.search("Chlamydia");

        assertFalse(results.isEmpty(), "Search should return results when loaded from cache");
    }

    @Test
    void testCacheInvalidationOnContentChange() throws IOException {
        // 1. Generate valid cache first
        SemanticSearchService service = new SemanticSearchService(realArticleCollection);
        service.search("test");
        assertTrue(Files.exists(cachePath));

        long originalSize = Files.size(cachePath);

        // 2. Overwrite cache with fake invalid data (wrong content hash)
        try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(cachePath)))) {
            dos.writeInt(CACHE_VERSION);
            dos.writeUTF("fake_hash_that_does_not_match"); // Fake content hash
            dos.writeInt(0); // 0 embeddings
        }

        // Verify we changed the file
        assertNotEquals(originalSize, Files.size(cachePath));

        // 3. Initialize new service. It should detect hash mismatch and REGENERATE the
        // cache.
        SemanticSearchService.reset();
        SemanticSearchService service2 = new SemanticSearchService(realArticleCollection);
        service2.search("test");

        // 4. Verify cache is back to a reasonable size (regenerated)
        assertTrue(Files.size(cachePath) > 100, "Cache should have been regenerated and be non-trivial size");

        // Check functionality
        assertFalse(service2.search("Chlamydia").isEmpty());
    }
}
