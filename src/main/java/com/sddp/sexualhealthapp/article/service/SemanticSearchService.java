package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.*;

/**
 * Semantic search using local ONNX vector embeddings (all-MiniLM-L6-v2).
 * Embeds article sections and searches by cosine similarity.
 *
 * <p>The ONNX model and embedding store are static (shared across instances)
 * so that {@link #preload()} from any instance warms the cache for all.</p>
 */
public class SemanticSearchService {

    private static final String ARTICLE_INDEX_KEY = "articleIndex";
    private static final int DEFAULT_MAX_RESULTS = 10;

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

        // Request more results than maxResults since multiple sections may belong to same article
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
            Article article = articles.get(articleIndex);
            double score = match.score();

            articleScores.merge(article, score, Math::max);
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
                    embeddingStore = new InMemoryEmbeddingStore<>();
                    EmbeddingModel model = new AllMiniLmL6V2QuantizedEmbeddingModel();
                    embedArticles(model);
                    // Set embeddingModel last so other threads see fully initialized state
                    SemanticSearchService.embeddingModel = model;
                }
            }
        }
    }

    /** Chunks articles by section with title prefix, then embeds each chunk. */
    private void embedArticles(EmbeddingModel model) {
        List<Article> articles = articleCollection.getArticles();

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);

            for (Article.Section section : article.getSections()) {
                String chunkText = article.getTitle() + " - " + section.heading() + "\n" + section.content();
                Metadata metadata = new Metadata().put(ARTICLE_INDEX_KEY, i);
                TextSegment segment = TextSegment.from(chunkText, metadata);

                Embedding embedding = model.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
        }
    }

    /** Visible for testing: checks if the service has been initialized. */
    boolean isInitialized() {
        return embeddingModel != null;
    }
}
