package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.ArticleCollection;

/**
 * Shared article infrastructure so expensive corpus/search setup is reused.
 */
public final class ArticleServiceRegistry {

    private static final Object lock = new Object();

    private static volatile ArticleCollection articleCollection;
    private static volatile ArticleSearchService articleSearchService;
    private static volatile SemanticSearchService semanticSearchService;
    private static volatile HybridSearchService hybridSearchService;

    private ArticleServiceRegistry() {
    }

    public static ArticleCollection getArticleCollection() {
        ArticleCollection local = articleCollection;
        if (local == null) {
            synchronized (lock) {
                local = articleCollection;
                if (local == null) {
                    local = ArticleCollection.getInstance();
                    articleCollection = local;
                }
            }
        }
        return local;
    }

    public static ArticleSearchService getArticleSearchService() {
        ArticleSearchService local = articleSearchService;
        if (local == null) {
            synchronized (lock) {
                local = articleSearchService;
                if (local == null) {
                    local = new ArticleSearchService(getArticleCollection());
                    articleSearchService = local;
                }
            }
        }
        return local;
    }

    public static SemanticSearchService getSemanticSearchService() {
        SemanticSearchService local = semanticSearchService;
        if (local == null) {
            synchronized (lock) {
                local = semanticSearchService;
                if (local == null) {
                    local = new SemanticSearchService(getArticleCollection());
                    semanticSearchService = local;
                }
            }
        }
        return local;
    }

    public static HybridSearchService getHybridSearchService() {
        HybridSearchService local = hybridSearchService;
        if (local == null) {
            synchronized (lock) {
                local = hybridSearchService;
                if (local == null) {
                    local = new HybridSearchService(getArticleSearchService(), getSemanticSearchService());
                    hybridSearchService = local;
                }
            }
        }
        return local;
    }

    /**
     * Warms the shared article collection and both search layers.
     * Safe to call repeatedly.
     */
    public static void preloadSearchInfrastructure() {
        getArticleCollection();
        getArticleSearchService();
        getSemanticSearchService().preload();
        getHybridSearchService();
    }
}
