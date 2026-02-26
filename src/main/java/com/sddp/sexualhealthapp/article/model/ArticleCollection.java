package com.sddp.sexualhealthapp.article.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.sddp.sexualhealthapp.util.AppConstants;

public class ArticleCollection {
    private static ArticleCollection instance;
    private List<Article> articles;

    /** Filename → source string, loaded from articles-metadata.json */
    private Map<String, String> sourcesByFilename;

    private ArticleCollection() {
        articles = new ArrayList<>();
        sourcesByFilename = loadMetadata();
        loadArticles();
        attachMetadata();
    }

    public static ArticleCollection getInstance() {
        if (instance == null) {
            instance = new ArticleCollection();
        }
        return instance;
    }

    /**
     * Loads article metadata JSON and builds a map of filename → source.
     * Uses simple string parsing to avoid adding a JSON library dependency.
     */
    private Map<String, String> loadMetadata() {
        Map<String, String> sources = new HashMap<>();
        try (InputStream is = ClassLoader.getSystemResourceAsStream(
                AppConstants.ARTICLE_MARKDOWN_PATH + "/articles-metadata.json")) {
            if (is == null) {
                System.err.println("articles-metadata.json not found");
                return sources;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Simple parsing: find each top-level key and its "source" field
            // Keys look like: "filename.md": { ... "source": "Brook" ... }
            int pos = 0;
            while (pos < json.length()) {
                // Find the next top-level key (a quoted string before a colon + brace)
                int keyStart = json.indexOf('"', pos);
                if (keyStart < 0)
                    break;
                int keyEnd = json.indexOf('"', keyStart + 1);
                if (keyEnd < 0)
                    break;
                String key = json.substring(keyStart + 1, keyEnd);

                // Find the opening brace for this entry's object
                int braceStart = json.indexOf('{', keyEnd);
                if (braceStart < 0)
                    break;

                // Find the matching closing brace (handle nesting)
                int depth = 1;
                int braceEnd = braceStart + 1;
                while (braceEnd < json.length() && depth > 0) {
                    if (json.charAt(braceEnd) == '{')
                        depth++;
                    else if (json.charAt(braceEnd) == '}')
                        depth--;
                    braceEnd++;
                }

                String block = json.substring(braceStart, braceEnd);
                // Extract "source": "value"
                int srcIdx = block.indexOf("\"source\"");
                if (srcIdx >= 0) {
                    int colonIdx = block.indexOf(':', srcIdx);
                    int valStart = block.indexOf('"', colonIdx + 1);
                    int valEnd = block.indexOf('"', valStart + 1);
                    if (valStart >= 0 && valEnd >= 0) {
                        sources.put(key, block.substring(valStart + 1, valEnd));
                    }
                }
                pos = braceEnd;
            }
        } catch (IOException e) {
            System.err.println("Error reading articles-metadata.json: " + e.getMessage());
        }
        return sources;
    }

    private void loadArticles() {
        try {
            URI uri = ClassLoader.getSystemResource(AppConstants.ARTICLE_MARKDOWN_PATH).toURI();
            Path articlesPath = Paths.get(uri);

            try (Stream<Path> paths = Files.walk(articlesPath, 1)) {
                paths.filter(path -> path.toString().endsWith(".md"))
                        .sorted()
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

            // Store filename on article for metadata lookup (tags, source, etc.)
            String filename = path.getFileName().toString();
            article.setFileName(filename);
            String source = sourcesByFilename.get(filename);
            if (source != null) {
                article.setSource(source);
            }

            articles.add(article);
        } catch (IOException e) {
            System.err.println("Error reading article file: " + path + " - " + e.getMessage());
        }
    }

    private void attachMetadata() {
        Map<String, ArticleMetadataLoader.ArticleMetadata> metadata = ArticleMetadataLoader.loadMetadata();

        for (Article article : articles) {
            ArticleMetadataLoader.ArticleMetadata meta = metadata.get(article.getFileName());
            if (meta != null) {
                article.setTags(meta.getTags());
            }
        }
    }

    public List<Article> getArticles() {
        return Collections.unmodifiableList(articles);
    }

    public static void main(String[] args) {
        ArticleCollection articles = ArticleCollection.getInstance();
        System.out.println("Loaded " + articles.getArticles().size() + " articles:");
        for (Article article : articles.getArticles()) {
            System.out.println("  - " + article.getTitle() + " (source: " + article.getSource() + ")");
        }
    }
}
