package com.sddp.sexualhealthapp.article.model;

import java.util.Optional;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;

public class Article {
    private record Section(String heading, String content) {
    }

    private String title;
    private Section[] sections;

    private Optional<String> extractText(Node node) {
        Node current = node.getFirstChild();

        while (current != null) {
            if (current instanceof Text text) {
                return Optional.ofNullable(text.getLiteral());
            }
            current = current.getFirstChild();
        }

        return Optional.empty();
    }

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

    public Article(String fileContent) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(fileContent);

        title = extractTitle(document).orElseThrow();
        sections = new Section[0];
    }

    public static void main(String[] args) {
        var article = new Article("# test\nlolol\n\ntesting\n ## lmao \n # test2");
    }
}
