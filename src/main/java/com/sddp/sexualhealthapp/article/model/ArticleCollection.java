package com.sddp.sexualhealthapp.article.model;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.sddp.sexualhealthapp.util.AppConstants;

public class ArticleCollection {
    private static ArticleCollection instance;
    private List<Article> articles;

    private ArticleCollection() {
        articles = new ArrayList<>();
        loadArticles();
    }

    public static ArticleCollection getInstance() {
        if (instance == null) {
            instance = new ArticleCollection();
        }
        return instance;
    }

    private void loadArticles() {
        try {
            URI uri = ClassLoader.getSystemResource(AppConstants.ARTICLE_MARKDOWN_PATH).toURI();
            Path articlesPath = Paths.get(uri);

            try (Stream<Path> paths = Files.walk(articlesPath, 1)) {
                paths.filter(path -> path.toString().endsWith(".md"))
                        .forEach(this::loadArticle);
            }
        } catch (URISyntaxException | IOException e) {
            System.err.println("Error loading articles: " + e.getMessage());
        }
    }

    private void loadArticle(Path path) {
        try {
            String content = Files.readString(path);
            Article article = new Article(content);
            articles.add(article);
        } catch (IOException e) {
            System.err.println("Error reading article file: " + path + " - " + e.getMessage());
        }
    }

    public List<Article> getArticles() {
        return Collections.unmodifiableList(articles);
    }

    public static void main(String[] args) {
        ArticleCollection articles = ArticleCollection.getInstance();
        System.out.println("Loaded " + articles.getArticles().size() + " articles:");
        for (Article article : articles.getArticles()) {
            System.out.println("  - " + article.getTitle());
        }
    }
}
