package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import com.sddp.sexualhealthapp.article.service.ArticlePersonalizationService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
        return createArticleCard(article, score, searchQuery, List.of(), List.of(), null, false, onArticleClick);
    }

    public static VBox createArticleCard(Article article, double score,
            String searchQuery,
            List<String> highlightedTags,
            List<String> preferredMatchedTags,
            Consumer<Article> onArticleClick) {
        return createArticleCard(article, score, searchQuery, highlightedTags, preferredMatchedTags,
                null, false, onArticleClick);
    }

    public static VBox createArticleCard(Article article, double score,
            String searchQuery,
            List<String> highlightedTags,
            List<String> preferredMatchedTags,
            String statusText,
            boolean hidden,
            Consumer<Article> onArticleClick) {
        VBox card = new VBox(4);
        card.getStyleClass().add("article-card");
        if (hidden) {
            card.getStyleClass().add("article-card-hidden");
        }

        // Colour-code the card based on relevance tier (only during search)
        if (!hidden && score >= 0.5) {
            card.getStyleClass().add("article-card-relevant");
        } else if (!hidden && score >= 0.25) {
            card.getStyleClass().add("article-card-possible");
        }

        String reasonText = buildReasonText(searchQuery, highlightedTags, preferredMatchedTags, statusText);
        if (reasonText != null) {
            Label reasonLabel = new Label(reasonText);
            reasonLabel.getStyleClass().add("article-card-reason");
            if (hidden) {
                reasonLabel.getStyleClass().add("article-card-reason-hidden");
            }
            reasonLabel.setWrapText(true);
            card.getChildren().add(reasonLabel);
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
        List<String> tagsToShow = pickTags(article.getTags(), searchQuery, highlightedTags, preferredMatchedTags);
        for (String tag : tagsToShow) {
            Label tagLabel = new Label(tag);
            tagLabel.getStyleClass().add("article-card-tag");
            tagLabel.setMinWidth(Region.USE_PREF_SIZE);

            if (preferredMatchedTags.contains(tag)) {
                tagLabel.getStyleClass().add("article-card-tag-preferred");
            } else if (highlightedTags.contains(tag) || isTagRelevantToQuery(tag, searchQuery)) {
                tagLabel.getStyleClass().add("article-card-tag-highlighted");
            }

            bottomRow.getChildren().add(tagLabel);
        }

        card.getChildren().add(bottomRow);

        // Click to open article
        card.setOnMouseClicked(e -> onArticleClick.accept(article));

        return card;
    }

    private static String buildReasonText(String searchQuery,
            List<String> highlightedTags,
            List<String> preferredMatchedTags,
            String statusText) {
        if (statusText != null && !statusText.isBlank()) {
            return statusText;
        }
        if (!preferredMatchedTags.isEmpty()) {
            return "Prioritised for you: " + String.join(", ", preferredMatchedTags);
        }
        if (searchQuery != null && !searchQuery.isBlank() && !highlightedTags.isEmpty()) {
            return "Matched tags: " + String.join(", ", highlightedTags);
        }
        return null;
    }

    /**
     * Creates a Recently Read card that shows persisted section progress and
     * resumes from the saved section when opened.
     */
    public static VBox createRecentArticleCard(Article article, RecentlyReadEntry entry,
            Consumer<RecentlyReadEntry> onRecentClick) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("article-card", "recent-article-card");
        card.setPickOnBounds(true);

        Label title = new Label(article.getTitle());
        title.getStyleClass().add("article-card-title");
        title.setWrapText(true);
        title.setMaxWidth(260);
        title.setMouseTransparent(true);
        card.getChildren().add(title);

        int totalSections = article.getSections().size();
        int completedSections = Math.min(totalSections, Math.max(0, entry.lastReadSectionIndex() + 1));
        double progress = totalSections > 0 ? (double) completedSections / totalSections : 0.0;

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.getStyleClass().add("article-card-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setMouseTransparent(true);
        card.getChildren().add(progressBar);

        Label progressLabel = new Label(completedSections + " of " + totalSections + " sections");
        progressLabel.getStyleClass().add("article-card-progress-label");
        progressLabel.setMouseTransparent(true);
        card.getChildren().add(progressLabel);

        Label sectionCount = new Label(totalSections + " section" + (totalSections != 1 ? "s" : ""));
        sectionCount.getStyleClass().add("article-card-subtitle");
        sectionCount.setMouseTransparent(true);
        card.getChildren().add(sectionCount);

        card.setOnMouseClicked(e -> onRecentClick.accept(entry));
        return card;
    }

    /**
     * Picks the top tags to display, promoting tags that match the search query
     * to the front so users see why an article was returned.
     * Respects a character budget so tags are never truncated with ellipses.
     */
    private static List<String> pickTags(List<String> allTags, String searchQuery,
            List<String> highlightedTags,
            List<String> preferredMatchedTags) {
        if (allTags == null || allTags.isEmpty()) {
            return List.of();
        }

        List<String> candidates = new ArrayList<>();
        addPriorityTags(candidates, preferredMatchedTags);
        addPriorityTags(candidates, highlightedTags);

        if (searchQuery != null && !searchQuery.isBlank()) {
            for (String tag : allTags) {
                if (isTagRelevantToQuery(tag, searchQuery) && !candidates.contains(tag)) {
                    candidates.add(tag);
                }
            }
        }

        for (String tag : allTags) {
            if (!candidates.contains(tag)) {
                candidates.add(tag);
            }
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

    private static void addPriorityTags(List<String> candidates, List<String> priorityTags) {
        for (String tag : priorityTags) {
            if (!candidates.contains(tag)) {
                candidates.add(tag);
            }
        }
    }

    /**
     * Checks if a tag is relevant to the search query by comparing words.
     * Uses case-insensitive containment in both directions so that
     * searching "STI" matches a tag "STIs" and vice versa.
     */
    private static boolean isTagRelevantToQuery(String tag, String searchQuery) {
        return ArticlePersonalizationService.isTagRelevantToQuery(tag, searchQuery);
    }
}
