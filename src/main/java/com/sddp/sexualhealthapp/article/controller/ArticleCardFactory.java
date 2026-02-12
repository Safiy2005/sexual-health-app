package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Factory for creating article card UI components used in the search results
 * list.
 */
public final class ArticleCardFactory {

    private ArticleCardFactory() {
    }

    /**
     * Creates an article card for the list view.
     * Cards are colour-coded based on search relevance score.
     *
     * @param article        the article to represent
     * @param score          the search relevance score (-1 if not from a search)
     * @param onArticleClick callback invoked when the card is clicked
     * @return a styled VBox representing the article card
     */
    public static VBox createArticleCard(Article article, double score, Consumer<Article> onArticleClick) {
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

        // Section count subtitle
        int sectionCount = article.getSections().size();
        Label subtitle = new Label(sectionCount + " section" + (sectionCount != 1 ? "s" : ""));
        subtitle.getStyleClass().add("article-card-subtitle");
        card.getChildren().add(subtitle);

        // Click to open article
        card.setOnMouseClicked(e -> onArticleClick.accept(article));

        return card;
    }
}
