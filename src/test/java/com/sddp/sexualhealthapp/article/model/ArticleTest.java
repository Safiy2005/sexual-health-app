package com.sddp.sexualhealthapp.article.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArticleTest {

    @Test
    void testGetTitle() {
        var article = new Article("""
                garbage text

                more garbage text *text*
                # *super cool title*

                ## a subheading

                more text""");
        assertEquals(article.getTitle(), "super cool title");
    }

    @Test
    void testGetSections() {
        var article = new Article("""
                # some title

                some title sub-text

                ## section 1

                section text

                more *section text*

                ## section 2

                text2
                text1
                    """);
        var sections = article.getSections();

        assertEquals(sections.get(0), new Article.Section("section 1", "section text\nmore section text\n"));
        assertEquals(sections.get(1), new Article.Section("section 2", "text2\ntext1\n"));
    }
}
