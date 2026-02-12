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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final int CACHE_VERSION = 1;

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

    /** Search with default max results (10). */
    public Map<Article, Double> search(String query) {
        return search(query, DEFAULT_MAX_RESULTS);
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
                    EmbeddingModel model = new AllMiniLmL6V2QuantizedEmbeddingModel();
                    InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

                    // Try loading from cache first
                    boolean loaded = loadFromDisk(store);

                    if (!loaded) {
                        System.out.println("Generating new embeddings...");
                        embedArticlesAndSave(model, store);
                    } else {
                        System.out.println("Loaded embeddings from cache.");
                    }

                    // Set static fields last so other threads see fully initialized state
                    embeddingStore = store;
                    SemanticSearchService.embeddingModel = model;
                }
            }
        }
    }

    /**
     * Chunks articles, generates embeddings, adds them to the store, and saves to
     * disk.
     */
    private void embedArticlesAndSave(EmbeddingModel model, InMemoryEmbeddingStore<TextSegment> store) {
        List<Article> articles = articleCollection.getArticles();
        List<TextSegment> segments = new ArrayList<>();
        List<Embedding> embeddings = new ArrayList<>();

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);

            for (Article.Section section : article.getSections()) {
                String chunkText = article.getTitle() + " - " + section.heading() + "\n" + section.content();
                Metadata metadata = new Metadata().put(ARTICLE_INDEX_KEY, i);
                TextSegment segment = TextSegment.from(chunkText, metadata);
                Embedding embedding = model.embed(segment).content();

                segments.add(segment);
                embeddings.add(embedding);
            }
        }

        store.addAll(embeddings, segments);
        saveToDisk(embeddings, segments);
    }

    /** Saves embeddings and segments to a compressed binary file. */
    private void saveToDisk(List<Embedding> embeddings, List<TextSegment> segments) {
        Path cachePath = Paths.get(CACHE_FILE_NAME);
        try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(cachePath)))) {
            dos.writeInt(CACHE_VERSION);
            dos.writeInt(articleCollection.getArticles().size()); // Save article count for validation
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
     * Invalidates cache if article count differs.
     */
    private boolean loadFromDisk(InMemoryEmbeddingStore<TextSegment> store) {
        Path cachePath = Paths.get(CACHE_FILE_NAME);
        if (!Files.exists(cachePath)) {
            return false;
        }

        List<Embedding> embeddings = new ArrayList<>();
        List<TextSegment> segments = new ArrayList<>();

        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(Files.newInputStream(cachePath)))) {
            int version = dis.readInt();
            if (version != CACHE_VERSION)
                return false;

            int cachedArticleCount = dis.readInt();
            if (cachedArticleCount != articleCollection.getArticles().size()) {
                System.out.println("Article count changed (" + cachedArticleCount + " vs "
                        + articleCollection.getArticles().size() + "). Invalidating cache.");
                return false;
            }

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
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("Failed to load embeddings cache: " + e.getMessage());
            return false;
        }
    }

    /** Visible for testing: checks if the service has been initialized. */
    boolean isInitialized() {
        return embeddingModel != null;
    }

    /** Visible for testing: resets static state. */
    static void reset() {
        embeddingModel = null;
        embeddingStore = null;
    }
}
