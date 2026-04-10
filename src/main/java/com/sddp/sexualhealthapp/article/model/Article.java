package com.sddp.sexualhealthapp.article.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.text.*;

public class Article {
    public record Section(String heading, String content) {
    }

    /** Unicode PUA marker inserted before bold text in section content. */
    public static final char BOLD_START = '\uE000';
    /** Unicode PUA marker inserted after bold text in section content. */
    public static final char BOLD_END = '\uE001';

    private String title;
    private String source;
    private List<Section> sections;
    private List<String> tags = List.of();
    private List<String> keywords = List.of();
    private String fileName;

    private Optional<String> extractText(Node node) {
        String rendered = TextContentRenderer.builder().build().render(node);

        return rendered.isEmpty() ? Optional.empty() : Optional.ofNullable(rendered);
    }

    /**
     * Extracts text from a commonmark AST node, preserving bold formatting
     * by wrapping strong-emphasis content in {@link #BOLD_START}/{@link #BOLD_END}
     * markers. All other node types are handled by the default
     * {@link TextContentRenderer}.
     */
    private String extractRichText(Node node) {
        TextContentRenderer renderer = TextContentRenderer.builder()
                .nodeRendererFactory(context -> new NodeRenderer() {
                    @Override
                    public Set<Class<? extends Node>> getNodeTypes() {
                        return Set.of(StrongEmphasis.class);
                    }

                    @Override
                    public void render(Node node) {
                        context.getWriter().write(String.valueOf(BOLD_START));
                        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                            context.render(child);
                        }
                        context.getWriter().write(String.valueOf(BOLD_END));
                    }
                })
                .build();
        return renderer.render(node);
    }

    // I miss Haskell Maybe :(
    private Optional<String> extractTitle(Node markdownAst) {
        Node current = markdownAst.getFirstChild();

        while (current != null) {
            if (current instanceof Heading heading && heading.getLevel() == 1) {
                Optional<String> title = extractText(heading);

                if (title.isPresent()) {
                    return title;
                }
            }
            current = current.getNext();
        }

        return Optional.empty();
    }

    private List<Node> getContainedNodes(Heading node) {
        ArrayList<Node> nodes = new ArrayList<Node>();
        Node current = node.getNext();

        // Keep going while there are sibling nodes, and the sibling node is not a
        // heading that is a sibling or larger than the current one
        while (current != null && (!(current instanceof Heading))
                || (current instanceof Heading heading && heading.getLevel() > node.getLevel())) {
            nodes.add(current);

            current = current.getNext();
        }

        return nodes;
    }

    private String doubleNewlines(String content) {
        return content.replace("\n", "\n\n");
    }

    private String reformatBullets(String content) {
        // Replace lines starting with * with an arrow symbol, and add indentation
        return content.replaceAll("(?m)^\\* ", " → ");
    }

    private ArrayList<Section> extractSections(Node markdownAst) {
        ArrayList<Section> sections = new ArrayList<Section>();

        Node current = markdownAst.getFirstChild();

        while (current != null) {
            if (current instanceof Heading heading && heading.getLevel() == 2) {
                String title = extractText(heading).orElseThrow();

                List<Node> childSections = getContainedNodes(heading);

                var content = new StringBuilder();

                for (var node : childSections) {
                    content.append(extractRichText(node));
                    content.append("\n");
                }

                sections.add(new Section(title, reformatBullets(doubleNewlines(content.toString()))));
            }

            current = current.getNext();
        }

        return sections;
    }

    public List<Section> getSections() {
        return sections;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? List.copyOf(tags) : List.of();
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords != null ? List.copyOf(keywords) : List.of();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article other)) return false;
        return Objects.equals(fileName, other.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileName);
    }

    public Article(String fileContent) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(fileContent);

        title = extractTitle(document).orElseThrow();
        sections = extractSections(document);
    }

    public static void main(String[] args) {
        var article = new Article("""
                garbage text

                more garbage text *text*
                # *super cool title*

                ## a subheading

                more text
                    """);
        System.out.println(article.getTitle());
    }

}
