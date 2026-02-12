package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Factory for creating article card UI components used in the search results
 * list.
 */
public final class ArticleCardFactory {

    private static final int MAX_DISPLAY_TAGS = 3;
    // Approximate character budget for all tags combined on the bottom row.
    // Card inner width (~296px) minus section count (~70px) minus gaps (~24px)
    // leaves ~200px. At ~6px per character + ~14px padding per tag, this gives
    // roughly 30 usable characters across all visible tags.
    private static final int TAG_CHAR_BUDGET = 30;

    private ArticleCardFactory() {
    }

    /**
     * Creates an article card for the list view.
     * Cards are colour-coded based on search relevance score.
     *
     * @param article        the article to represent
     * @param score          the search relevance score (-1 if not from a search)
     * @param searchQuery    the current search query (empty string if browsing)
     * @param onArticleClick callback invoked when the card is clicked
     * @return a styled VBox representing the article card
     */
    public static VBox createArticleCard(Article article, double score,
                                         String searchQuery, Consumer<Article> onArticleClick) {
        VBox card = new VBox(4);
        card.getStyleClass().add("article-card");

        // Colour-code the card based on relevance tier (only during search)
        if (score >= 0.5) {
            card.getStyleClass().add("article-card-relevant");
        } else if (score >= 0.25) {
            card.getStyleClass().add("article-card-possible");
        }

        // Title row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(article.getTitle());
        title.getStyleClass().add("article-card-title");
        title.setWrapText(true);
        title.setMaxWidth(260);
        titleRow.getChildren().add(title);

        card.getChildren().add(titleRow);

        // Bottom row: section count + tag chips side by side
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        int sectionCount = article.getSections().size();
        Label subtitle = new Label(sectionCount + " section" + (sectionCount != 1 ? "s" : ""));
        subtitle.getStyleClass().add("article-card-subtitle");
        bottomRow.getChildren().add(subtitle);

        // Tag chips (up to 3, fitted to available space, reordered by search relevance)
        List<String> tagsToShow = pickTags(article.getTags(), searchQuery);
        for (String tag : tagsToShow) {
            Label tagLabel = new Label(tag);
            tagLabel.getStyleClass().add("article-card-tag");
            tagLabel.setMinWidth(Region.USE_PREF_SIZE);

            if (isTagRelevantToQuery(tag, searchQuery)) {
                tagLabel.getStyleClass().add("article-card-tag-highlighted");
            }

            bottomRow.getChildren().add(tagLabel);
        }

        card.getChildren().add(bottomRow);

        // Click to open article
        card.setOnMouseClicked(e -> onArticleClick.accept(article));

        return card;
    }

    /**
     * Picks the top tags to display, promoting tags that match the search query
     * to the front so users see why an article was returned.
     * Respects a character budget so tags are never truncated with ellipses.
     */
    private static List<String> pickTags(List<String> allTags, String searchQuery) {
        if (allTags == null || allTags.isEmpty()) {
            return List.of();
        }

        // Build a priority-ordered candidate list (search-matching tags first)
        List<String> candidates;
        if (searchQuery == null || searchQuery.isBlank()) {
            candidates = allTags;
        } else {
            List<String> matching = new ArrayList<>();
            List<String> rest = new ArrayList<>();
            for (String tag : allTags) {
                if (isTagRelevantToQuery(tag, searchQuery)) {
                    matching.add(tag);
                } else {
                    rest.add(tag);
                }
            }
            candidates = new ArrayList<>(matching);
            candidates.addAll(rest);
        }

        // Greedily take tags until we hit the count limit or character budget
        List<String> result = new ArrayList<>();
        int charsUsed = 0;
        for (String tag : candidates) {
            if (result.size() >= MAX_DISPLAY_TAGS) {
                break;
            }
            if (charsUsed + tag.length() > TAG_CHAR_BUDGET && !result.isEmpty()) {
                break;
            }
            result.add(tag);
            charsUsed += tag.length();
        }

        return result;
    }

    /**
     * Checks if a tag is relevant to the search query by comparing words.
     * Uses case-insensitive containment in both directions so that
     * searching "STI" matches a tag "STIs" and vice versa.
     */
    private static boolean isTagRelevantToQuery(String tag, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return false;
        }

        String tagLower = tag.toLowerCase();
        String queryLower = searchQuery.toLowerCase().trim();

        // Check if any query word appears in the tag or vice versa
        String[] queryWords = queryLower.split("\\s+");
        for (String word : queryWords) {
            if (word.length() >= 2 && (tagLower.contains(word) || word.contains(tagLower))) {
                return true;
            }
        }

        return false;
    }
}
