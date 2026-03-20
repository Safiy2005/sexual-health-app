package com.sddp.sexualhealthapp.article.controller;

import com.sddp.sexualhealthapp.article.model.Article;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Builds the individual pages (title page and section pages) for the article
 * reader.
 * Each page is wrapped in a ScrollPane so long content is individually
 * scrollable.
 */
public final class ArticlePageBuilder {

    public record SectionPage(VBox root, VBox relatedArticlesBox, VBox relatedArticlesContainer) {
    }

    /**
     * Marker character that identifies a bullet-list line in formatted
     * section content (inserted by {@link Article}'s reformatBullets logic).
     */
    private static final String BULLET_MARKER = "→";

    private ArticlePageBuilder() {
        // Utility class
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
    public static SectionPage createSectionPage(Article.Section section, int sectionNumber, int totalSections) {
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

        VBox relatedArticlesBox = createRelatedArticlesBox();
        VBox relatedArticlesContainer = (VBox) relatedArticlesBox.getChildren().get(2);
        page.getChildren().add(relatedArticlesBox);

        return new SectionPage(wrapInScrollPane(page), relatedArticlesBox, relatedArticlesContainer);
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
                    TextFlow bulletFlow = createRichTextFlow(
                            itemText, "article-bullet-text", "article-bullet-text-bold",
                            true);
                    bulletFlow.getStyleClass().add("article-bullet-item");
                    bulletBox.getChildren().add(bulletFlow);
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
                    TextFlow textFlow = createRichTextFlow(
                            textBlock.toString(), "article-text", "article-text-bold",
                            false);
                    textFlow.getStyleClass().add("article-section-content");
                    page.getChildren().add(textFlow);
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
     * Parses text for {@link Article#BOLD_START}/{@link Article#BOLD_END}
     * markers and builds a {@link TextFlow} with individually styled
     * {@link Text} nodes for normal and bold segments.
     *
     * @param breakAfterLeadingBold if {@code true} and the content starts
     *                              with bold text, a newline is inserted
     *                              after the first bold segment so the
     *                              remaining text appears on a new line.
     */
    private static TextFlow createRichTextFlow(String content, String normalClass, String boldClass,
            boolean breakAfterLeadingBold) {
        TextFlow flow = new TextFlow();
        StringBuilder current = new StringBuilder();
        boolean inBold = false;
        boolean isFirstBold = true;
        // Content "starts with bold" if the first BOLD_START occurs before any
        // alphabetic character (i.e. only bullet symbols/whitespace precede it).
        int boldIdx = content.indexOf(Article.BOLD_START);
        boolean startsWithBold = boldIdx >= 0
                && content.substring(0, boldIdx).chars().noneMatch(Character::isLetterOrDigit);

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == Article.BOLD_START) {
                if (current.length() > 0) {
                    Text text = new Text(current.toString());
                    text.getStyleClass().add(normalClass);
                    flow.getChildren().add(text);
                    current.setLength(0);
                }
                inBold = true;
            } else if (c == Article.BOLD_END) {
                if (current.length() > 0) {
                    Text text = new Text(current.toString());
                    text.getStyleClass().add(boldClass);
                    flow.getChildren().add(text);
                    current.setLength(0);
                }
                // Insert newline after the first bold segment if it leads the bullet
                if (breakAfterLeadingBold && isFirstBold && startsWithBold) {
                    flow.getChildren().add(new Text("\n"));
                }
                isFirstBold = false;
                inBold = false;
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            Text text = new Text(current.toString());
            text.getStyleClass().add(inBold ? boldClass : normalClass);
            flow.getChildren().add(text);
        }

        return flow;
    }

    /**
     * Wraps a page VBox in a ScrollPane so long content is scrollable.
     */
    private static VBox wrapInScrollPane(VBox page) {
        ScrollPane scrollWrapper = new ScrollPane(page);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollWrapper.getStyleClass().add("search-scroll-pane");

        VBox wrapper = new VBox(scrollWrapper);
        wrapper.getStyleClass().add("article-page-wrapper");
        VBox.setVgrow(scrollWrapper, Priority.ALWAYS);
        return wrapper;
    }

    private static VBox createRelatedArticlesBox() {
        VBox relatedArticlesBox = new VBox(8);
        relatedArticlesBox.getStyleClass().add("article-related-box");
        relatedArticlesBox.setVisible(false);
        relatedArticlesBox.setManaged(false);

        Label title = new Label("Suggested Articles");
        title.getStyleClass().add("article-related-title");

        Label subtitle = new Label("Related to this page");
        subtitle.getStyleClass().add("article-related-subtitle");

        VBox relatedArticlesContainer = new VBox(8);
        relatedArticlesContainer.getStyleClass().add("article-related-list");

        relatedArticlesBox.getChildren().addAll(title, subtitle, relatedArticlesContainer);
        return relatedArticlesBox;
    }
}
