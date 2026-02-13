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

    /**
     * Marker character that identifies a bullet-list line in formatted
     * section content (inserted by {@link Article}'s reformatBullets logic).
     */
    private static final String BULLET_MARKER = "→";

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

        // Show the article source if available
        if (article.getSource() != null) {
            Label sourceLabel = new Label("Source: " + article.getSource());
            sourceLabel.getStyleClass().add("article-source-label");
            sourceLabel.setWrapText(true);
            page.getChildren().add(sourceLabel);
        }

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
     * Text paragraphs are rendered as regular labels, while consecutive
     * bullet-point lines are grouped into a visually distinct styled box.
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

        // Section content – split into text blocks and bullet-list groups
        addStyledContent(page, section.content());

        return wrapInScrollPane(page);
    }

    /**
     * Splits section content into text paragraphs and bullet-list groups,
     * rendering bullet lists inside a styled container that is visually
     * distinct from normal text.
     */
    private static void addStyledContent(VBox page, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] parts = content.trim().split("\n\n");
        int i = 0;

        while (i < parts.length) {
            String part = parts[i].strip();
            if (part.isEmpty()) {
                i++;
                continue;
            }

            if (isBulletLine(part)) {
                // Group consecutive bullet items into a styled box
                VBox bulletBox = new VBox(2);
                bulletBox.getStyleClass().add("article-bullet-list-box");

                while (i < parts.length) {
                    String bp = parts[i].strip();
                    if (bp.isEmpty()) {
                        i++;
                        continue;
                    }
                    if (!isBulletLine(bp)) {
                        break;
                    }

                    String itemText = "\u2022   " + extractBulletText(bp);
                    Label bulletLabel = new Label(itemText);
                    bulletLabel.getStyleClass().add("article-bullet-item");
                    bulletLabel.setWrapText(true);
                    bulletBox.getChildren().add(bulletLabel);
                    i++;
                }

                page.getChildren().add(bulletBox);
            } else {
                // Group consecutive non-bullet paragraphs into a single label
                StringBuilder textBlock = new StringBuilder();

                while (i < parts.length) {
                    String tp = parts[i].strip();
                    if (tp.isEmpty()) {
                        i++;
                        continue;
                    }
                    if (isBulletLine(tp)) {
                        break;
                    }

                    if (textBlock.length() > 0) {
                        textBlock.append("\n\n");
                    }
                    textBlock.append(tp);
                    i++;
                }

                if (textBlock.length() > 0) {
                    Label textLabel = new Label(textBlock.toString());
                    textLabel.getStyleClass().add("article-section-content");
                    textLabel.setWrapText(true);
                    page.getChildren().add(textLabel);
                }
            }
        }
    }

    /**
     * Returns {@code true} if the (stripped) line begins with the bullet marker
     * arrow.
     */
    private static boolean isBulletLine(String line) {
        return line.startsWith(BULLET_MARKER);
    }

    /**
     * Extracts the text after the bullet marker arrow, stripping leading
     * whitespace.
     */
    private static String extractBulletText(String line) {
        return line.substring(BULLET_MARKER.length()).stripLeading();
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
