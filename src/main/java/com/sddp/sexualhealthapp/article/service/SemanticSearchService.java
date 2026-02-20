package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Semantic search using local ONNX vector embeddings (all-MiniLM-L6-v2).
 * Embeds article sections and searches by cosine similarity.
 *
 * <p>
 * The ONNX model and embedding store are static (shared across instances)
 * so that {@link #preload()} from any instance warms the cache for all.
 * </p>
 *
 * <p>
 * Updated to support persistent caching of embeddings to reduce startup time.
 * </p>
 */
public class SemanticSearchService {

    private static final String ARTICLE_INDEX_KEY = "articleIndex";
    private static final int DEFAULT_MAX_RESULTS = 10;
    private static final String CACHE_FILE_NAME = "semantic_embeddings.cache";
    private static final int CACHE_VERSION = 2;

    private final ArticleCollection articleCollection;

    private static volatile EmbeddingModel embeddingModel;
    private static volatile InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private static final Object initLock = new Object();

    /** Production: uses singleton collection. */
    public SemanticSearchService() {
        this(ArticleCollection.getInstance());
    }

    /** Testing: accepts injected collection. */
    public SemanticSearchService(ArticleCollection articleCollection) {
        this.articleCollection = articleCollection;
    }

    /** Search with default max results (10). Blocks until initialized. */
    public Map<Article, Double> search(String query) {
        return search(query, DEFAULT_MAX_RESULTS);
    }

    /**
     * Non-blocking search: returns results if the model is ready, or an empty
     * map if still loading. Used by {@link HybridSearchService} so TF-IDF
     * results can be returned immediately while embeddings load in the background.
     */
    public Map<Article, Double> searchIfReady(String query, int maxResults) {
        if (!isReady()) {
            return Collections.emptyMap();
        }
        return search(query, maxResults);
    }

    /**
     * Search articles by semantic similarity.
     * Returns a map of Article → best cosine similarity score across its sections.
     */
    public Map<Article, Double> search(String query, int maxResults) {
        if (query == null || query.trim().isEmpty() || maxResults <= 0) {
            return Collections.emptyMap();
        }

        ensureInitialized();

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // Request more results than maxResults since multiple sections may belong to
        // same article
        int sectionsToRetrieve = maxResults * 3;
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(sectionsToRetrieve)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        // Aggregate: for each article, keep the highest section score
        List<Article> articles = articleCollection.getArticles();
        Map<Article, Double> articleScores = new LinkedHashMap<>();

        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            int articleIndex = match.embedded().metadata().getInteger(ARTICLE_INDEX_KEY);
            if (articleIndex >= 0 && articleIndex < articles.size()) {
                Article article = articles.get(articleIndex);
                double score = match.score();
                articleScores.merge(article, score, Math::max);
            }
        }

        // Sort by score descending and limit to maxResults
        Map<Article, Double> sorted = new LinkedHashMap<>();
        articleScores.entrySet().stream()
                .sorted(Map.Entry.<Article, Double>comparingByValue().reversed())
                .limit(maxResults)
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));

        return sorted;
    }

    /** Pre-loads the ONNX model and embeds all articles. Thread-safe. */
    public void preload() {
        ensureInitialized();
    }

    /** Loads ONNX model and embeds all article sections if not already done. */
    private void ensureInitialized() {
        if (embeddingModel == null) {
            synchronized (initLock) {
                if (embeddingModel == null) {
                    long totalStart = System.currentTimeMillis();

                    System.out.println("[Semantic] Loading ONNX model (all-MiniLM-L6-v2)...");
                    long modelStart = System.currentTimeMillis();
                    EmbeddingModel model = new AllMiniLmL6V2QuantizedEmbeddingModel();
                    System.out.printf("[Semantic] Model loaded in %.1fs%n",
                            (System.currentTimeMillis() - modelStart) / 1000.0);

                    InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

                    // Try loading from cache first
                    boolean loaded = loadFromDisk(store);

                    if (!loaded) {
                        embedArticlesAndSave(model, store);
                    }

                    System.out.printf("[Semantic] Ready in %.1fs%n",
                            (System.currentTimeMillis() - totalStart) / 1000.0);

                    // Set static fields last so other threads see fully initialized state
                    embeddingStore = store;
                    SemanticSearchService.embeddingModel = model;
                }
            }
        }
    }

    /**
     * Chunks articles, generates embeddings in batches, adds them to the store,
     * and saves to disk. Uses {@code embedAll()} to batch all sections of each
     * article into a single ONNX call for faster inference.
     */
    private void embedArticlesAndSave(EmbeddingModel model, InMemoryEmbeddingStore<TextSegment> store) {
        List<Article> articles = articleCollection.getArticles();
        int totalSections = articles.stream().mapToInt(a -> a.getSections().size()).sum();

        System.out.printf("[Semantic] Generating embeddings for %d articles (%d sections)...%n",
                articles.size(), totalSections);

        List<TextSegment> allSegments = new ArrayList<>();
        List<Embedding> allEmbeddings = new ArrayList<>();

        long genStart = System.currentTimeMillis();
        int sectionsProcessed = 0;

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);

            // Build all segments for this article
            List<TextSegment> articleSegments = new ArrayList<>();
            for (Article.Section section : article.getSections()) {
                String chunkText = article.getTitle() + " - " + section.heading() + "\n" + section.content();
                Metadata metadata = new Metadata().put(ARTICLE_INDEX_KEY, i);
                articleSegments.add(TextSegment.from(chunkText, metadata));
            }

            // Batch-embed all sections of this article in one ONNX call
            List<Embedding> articleEmbeddings = model.embedAll(articleSegments).content();

            allSegments.addAll(articleSegments);
            allEmbeddings.addAll(articleEmbeddings);
            sectionsProcessed += articleSegments.size();

            printProgress(sectionsProcessed, totalSections, genStart);
        }

        long genElapsed = System.currentTimeMillis() - genStart;
        System.out.printf("\r[Semantic] Embedded %d sections in %.1fs (%.0f sections/sec)%n",
                totalSections, genElapsed / 1000.0,
                totalSections / (genElapsed / 1000.0));

        store.addAll(allEmbeddings, allSegments);

        long saveStart = System.currentTimeMillis();
        saveToDisk(allEmbeddings, allSegments);
        System.out.printf("[Semantic] Cache saved in %.1fs%n",
                (System.currentTimeMillis() - saveStart) / 1000.0);
    }

    /** Prints a progress bar with ETA (overwrites current line via \\r). */
    private static void printProgress(int done, int total, long startMs) {
        double fraction = (double) done / total;
        int percent = (int) (fraction * 100);
        long elapsed = System.currentTimeMillis() - startMs;
        String eta;
        if (done > 0) {
            long remaining = (long) (elapsed / fraction) - elapsed;
            eta = String.format("%ds left", Math.max(0, remaining / 1000));
        } else {
            eta = "calculating...";
        }

        int barWidth = 30;
        int filled = (int) (barWidth * fraction);
        StringBuilder bar = new StringBuilder();
        bar.append("\u2588".repeat(filled));
        bar.append("\u2591".repeat(barWidth - filled));

        System.out.printf("\r[Semantic] %s %3d%% (%d/%d) %s",
                bar, percent, done, total, eta);

        if (done == total) {
            System.out.println();
        }
    }

    /** Saves embeddings and segments to a compressed binary file. */
    private void saveToDisk(List<Embedding> embeddings, List<TextSegment> segments) {
        Path cachePath = Paths.get(CACHE_FILE_NAME);
        try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(cachePath)))) {
            dos.writeInt(CACHE_VERSION);
            dos.writeUTF(computeContentHash()); // Content hash for invalidation
            dos.writeInt(embeddings.size());

            for (int i = 0; i < embeddings.size(); i++) {
                Embedding emb = embeddings.get(i);
                TextSegment seg = segments.get(i);

                // Write Embedding (vector)
                float[] vector = emb.vector();
                dos.writeInt(vector.length);
                for (float v : vector) {
                    dos.writeFloat(v);
                }

                // Write TextSegment (text + metadata)
                dos.writeUTF(seg.text());
                int articleIndex = seg.metadata().getInteger(ARTICLE_INDEX_KEY);
                dos.writeInt(articleIndex);
            }
        } catch (IOException e) {
            System.err.println("Failed to save embeddings cache: " + e.getMessage());
            // Non-fatal, just won't cache
        }
    }

    /**
     * Tries to load embeddings from disk. Returns true if successful.
     * Invalidates cache if article content hash differs.
     */
    private boolean loadFromDisk(InMemoryEmbeddingStore<TextSegment> store) {
        Path cachePath = Paths.get(CACHE_FILE_NAME);
        if (!Files.exists(cachePath)) {
            System.out.println("[Semantic] No cache file found — first run");
            return false;
        }

        long fileSize;
        try {
            fileSize = Files.size(cachePath);
        } catch (IOException e) {
            fileSize = -1;
        }
        System.out.printf("[Semantic] Found cache (%s), validating...%n", formatBytes(fileSize));

        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> segments = new ArrayList<>();

        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(Files.newInputStream(cachePath)))) {
            int version = dis.readInt();
            if (version != CACHE_VERSION) {
                System.out.printf("[Semantic] Cache version mismatch (found v%d, expected v%d) — regenerating%n",
                        version, CACHE_VERSION);
                return false;
            }

            String cachedHash = dis.readUTF();
            String currentHash = computeContentHash();
            if (!cachedHash.equals(currentHash)) {
                System.out.println("[Semantic] Article content hash mismatch — regenerating");
                System.out.println("[Semantic]   cached:  " + cachedHash.substring(0, 16) + "...");
                System.out.println("[Semantic]   current: " + currentHash.substring(0, 16) + "...");
                return false;
            }

            long loadStart = System.currentTimeMillis();
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                // Read Embedding
                int vectorLength = dis.readInt();
                float[] vector = new float[vectorLength];
                for (int j = 0; j < vectorLength; j++) {
                    vector[j] = dis.readFloat();
                }
                embeddings.add(Embedding.from(vector));

                // Read TextSegment
                String text = dis.readUTF();
                int articleIndex = dis.readInt();
                Metadata metadata = new Metadata().put(ARTICLE_INDEX_KEY, articleIndex);
                segments.add(TextSegment.from(text, metadata));
            }

            store.addAll(embeddings, segments);
            System.out.printf("[Semantic] Loaded %d embeddings from cache in %.1fs%n",
                    count, (System.currentTimeMillis() - loadStart) / 1000.0);
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("[Semantic] Cache read failed: " + e.getMessage() + " — regenerating");
            return false;
        }
    }

    /** Formats a byte count as a human-readable string. */
    private static String formatBytes(long bytes) {
        if (bytes < 0) return "unknown size";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Computes a SHA-256 hash of all article content (titles + sections).
     * Used to detect when article text has changed so stale embeddings are
     * invalidated.
     */
    private String computeContentHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Article article : articleCollection.getArticles()) {
                digest.update(article.getTitle().getBytes(StandardCharsets.UTF_8));
                for (Article.Section section : article.getSections()) {
                    digest.update(section.heading().getBytes(StandardCharsets.UTF_8));
                    digest.update(section.content().getBytes(StandardCharsets.UTF_8));
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Checks if the ONNX model and embedding store are ready for queries. */
    public boolean isReady() {
        return embeddingModel != null;
    }

    /** Visible for testing: resets static state. */
    static void reset() {
        embeddingModel = null;
        embeddingStore = null;
    }
}
