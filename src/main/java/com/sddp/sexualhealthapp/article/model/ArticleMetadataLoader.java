package com.sddp.sexualhealthapp.article.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sddp.sexualhealthapp.util.AppConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads article metadata (tags, keywords, source) from articles-metadata.json.
 * The JSON maps each markdown filename to its metadata fields.
 */
public final class ArticleMetadataLoader {

    private ArticleMetadataLoader() {
    }

    /**
     * A single article's metadata entry from the JSON file.
     */
    public static class ArticleMetadata {
        private String title;
        private String source;
        private String filePath;
        private List<String> tags;
        private List<String> keywords;

        public String getTitle() { return title; }
        public String getSource() { return source; }
        public String getFilePath() { return filePath; }
        public List<String> getTags() { return tags != null ? tags : List.of(); }
        public List<String> getKeywords() { return keywords != null ? keywords : List.of(); }
    }

    /**
     * Loads metadata for all articles from the bundled JSON resource.
     *
     * @return map of markdown filename (e.g. "brook-abuse.md") to its metadata,
     *         or an empty map if loading fails
     */
    public static Map<String, ArticleMetadata> loadMetadata() {
        String resourcePath = AppConstants.ARTICLE_MARKDOWN_PATH + "/articles-metadata.json";

        try (InputStream is = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("articles-metadata.json not found at: " + resourcePath);
                return Collections.emptyMap();
            }

            Type mapType = new TypeToken<Map<String, ArticleMetadata>>() {}.getType();
            return new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), mapType);
        } catch (IOException e) {
            System.err.println("Error loading article metadata: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
