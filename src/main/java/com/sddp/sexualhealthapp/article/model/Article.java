package com.sddp.sexualhealthapp.article.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.*;

public class Article {
    private record Section(String heading, String content) {
    }

    private String title;
    private List<Section> sections;

    private Optional<String> extractText(Node node) {
        String rendered = TextContentRenderer.builder().build().render(node);

        return rendered.isEmpty() ? Optional.empty() : Optional.ofNullable(rendered);
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
        // heading of a greater than or equal to level
        while (current != null && (!(current instanceof Heading))
                || (current instanceof Heading heading && heading.getLevel() > node.getLevel())) {
            nodes.add(current);

            current = current.getNext();
        }

        return nodes;
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
                    content.append(extractText(node).orElse(""));
                    content.append("\n");
                }

                sections.add(new Section(title, content.toString()));
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

    public Article(String fileContent) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(fileContent);

        title = extractTitle(document).orElseThrow();
        sections = extractSections(document);
    }

    public static void main(String[] args) {
        var article = new Article(
                "# test\nlolol\n\ntesting\n ## section 1 \n section 1 text \n\nsection 1 text2 \n ## section 2 \n section 2 text \n\n section 2 text2 \nmaybe new text\n# test2");
    }
}
