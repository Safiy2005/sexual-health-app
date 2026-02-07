package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Builds the individual pages (title page and section pages) for the article
 * reader.
 * Each page is wrapped in a ScrollPane so long content is individually
 * scrollable.
 */
public final class ArticlePageBuilder {

    private ArticlePageBuilder() {
        // Utility class
    }

    /**
     * Creates the title/overview page for an article (page 0).
     * Displays the article title and a table of contents listing all sections.
     *
     * @param article the article to build the title page for
     * @return a VBox wrapped in a ScrollPane containing the title page
     */
    public static VBox createTitlePage(Article article) {
        VBox page = new VBox(12);
        page.getStyleClass().add("article-page");
        page.setAlignment(Pos.TOP_LEFT);
        page.setPadding(new Insets(4, 20, 24, 20));

        Label title = new Label(article.getTitle());
        title.getStyleClass().add("article-detail-title");
        title.setWrapText(true);
        page.getChildren().add(title);

        // Show a summary of available sections as a table of contents
        List<Article.Section> sections = article.getSections();
        if (!sections.isEmpty()) {
            Label tocHeader = new Label("In this article (" + sections.size() + " sections)");
            tocHeader.getStyleClass().add("article-toc-header");
            tocHeader.setWrapText(true);
            page.getChildren().add(tocHeader);

            for (int i = 0; i < sections.size(); i++) {
                Label tocItem = new Label((i + 1) + ".  " + sections.get(i).heading());
                tocItem.getStyleClass().add("article-toc-item");
                tocItem.setWrapText(true);
                page.getChildren().add(tocItem);
            }

            Label swipeHint = new Label("Swipe left to start reading →");
            swipeHint.getStyleClass().add("article-swipe-hint");
            swipeHint.setWrapText(true);
            page.getChildren().add(swipeHint);
        }

        return wrapInScrollPane(page);
    }

    /**
     * Creates a page for a single article section.
     *
     * @param section       the section to display
     * @param sectionNumber the 1-based section number
     * @param totalSections the total number of sections in the article
     * @return a VBox wrapped in a ScrollPane containing the section page
     */
    public static VBox createSectionPage(Article.Section section, int sectionNumber, int totalSections) {
        VBox page = new VBox(10);
        page.getStyleClass().add("article-page");
        page.setAlignment(Pos.TOP_LEFT);
        page.setPadding(new Insets(4, 20, 24, 20));

        // Section number badge
        Label badge = new Label("Section " + sectionNumber + " of " + totalSections);
        badge.getStyleClass().add("article-section-badge");
        page.getChildren().add(badge);

        // Section heading
        Label heading = new Label(section.heading());
        heading.getStyleClass().add("article-section-heading");
        heading.setWrapText(true);
        page.getChildren().add(heading);

        // Section content
        Label content = new Label(section.content().trim());
        content.getStyleClass().add("article-section-content");
        content.setWrapText(true);
        page.getChildren().add(content);

        return wrapInScrollPane(page);
    }

    /**
     * Wraps a page VBox in a ScrollPane so long content is scrollable.
     */
    private static VBox wrapInScrollPane(VBox page) {
        ScrollPane scrollWrapper = new ScrollPane(page);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.getStyleClass().add("search-scroll-pane");

        VBox wrapper = new VBox(scrollWrapper);
        wrapper.getStyleClass().add("article-page-wrapper");
        VBox.setVgrow(scrollWrapper, Priority.ALWAYS);
        return wrapper;
    }
}
