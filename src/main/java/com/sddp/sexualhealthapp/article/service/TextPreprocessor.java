package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Static utility for text normalization, tokenization, and field extraction. */
public class TextPreprocessor {

    /**
     * Common English stopwords to filter out during tokenization.
     * These words are too common to contribute meaningfully to search relevance.
     */
    private static final Set<String> STOPWORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
        "to", "was", "will", "with"
    );

    /** Prevent instantiation. */
    private TextPreprocessor() {
        throw new AssertionError("Cannot instantiate TextPreprocessor class");
    }

    /** Lowercase, remove special chars, collapse whitespace. Returns empty string for null. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    /** Split text into words, remove stopwords. */
    public static List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(text.split("\\s+"))
            .filter(word -> !word.isEmpty())
            .filter(word -> !STOPWORDS.contains(word))
            .collect(Collectors.toList());
    }

    /** Extract title, headings, content from article into map. */
    public static Map<String, String> extractFields(Article article) {
        if (article == null) {
            return Map.of(
                "title", "",
                "headings", "",
                "content", ""
            );
        }

        String title = article.getTitle() != null ? article.getTitle() : "";
        String headings = article.getSections().stream()
            .map(Article.Section::heading)
            .collect(Collectors.joining(" "));
        String content = article.getSections().stream()
            .map(Article.Section::content)
            .collect(Collectors.joining(" "));

        return Map.of(
            "title", title,
            "headings", headings,
            "content", content
        );
    }
}
