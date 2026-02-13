package com.sddp.sexualhealthapp.article.model;

import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertEquals("super cool title", article.getTitle());
    }

    @Test
    void testGetTitle_WithSimpleText() {
        var article = new Article("""
                # Simple Title

                Content here""");
        assertEquals("Simple Title", article.getTitle());
    }

    @Test
    void testGetTitle_WithMarkdownFormatting() {
        var article = new Article("""
                # Title with **bold** and *italic*

                Content""");
        assertEquals("Title with bold and italic", article.getTitle());
    }

    @Test
    void testGetTitle_FirstH1IsUsed() {
        var article = new Article("""
                # First Title

                Some content

                # Second Title

                More content""");
        assertEquals("First Title", article.getTitle());
    }

    @Test
    void testGetTitle_ThrowsWhenNoH1() {
        assertThrows(Exception.class, () -> {
            new Article("""
                    ## Only H2 heading

                    No H1 here""");
        }, "Should throw when no H1 heading is found");
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

        assertEquals(new Article.Section("section 1", "section text\n\nmore section text\n\n"), sections.get(0));
        assertEquals(new Article.Section("section 2", "text2\n\ntext1\n\n"), sections.get(1));
    }

    @Test
    void testGetSections_NotNull() {
        var article = new Article("""
                # Title

                ## Section

                Content""");

        assertNotNull(article.getSections(), "getSections should never return null");
    }

    @Test
    void testGetSections_EmptyWhenNoH2() {
        var article = new Article("""
                # Title

                Just content, no sections""");

        assertTrue(article.getSections().isEmpty(),
                "Should return empty list when no H2 sections exist");
    }

    @Test
    void testGetSections_MultipleSections() {
        var article = new Article("""
                # Main Title

                ## Section 1
                Content 1

                ## Section 2
                Content 2

                ## Section 3
                Content 3""");

        assertEquals(3, article.getSections().size(),
                "Should extract all H2 sections");
    }

    @Test
    void testGetSections_PreservesOrder() {
        var article = new Article("""
                # Title

                ## First
                First content

                ## Second
                Second content

                ## Third
                Third content""");

        List<Article.Section> sections = article.getSections();
        assertEquals("First", sections.get(0).heading());
        assertEquals("Second", sections.get(1).heading());
        assertEquals("Third", sections.get(2).heading());
    }

    @Test
    void testGetSections_HandlesNestedMarkdown() {
        var article = new Article("""
                # Title

                ## Section with **bold** and *italic*

                Content with [links](http://example.com) and `code`""");

        Article.Section section = article.getSections().get(0);
        assertEquals("Section with bold and italic", section.heading());

        // For some reason it wraps the link in quotes in the output
        assertTrue(section.content().contains("Content with \"links\""));
    }

    @Test
    void testGetSections_HandlesMultipleParagraphs() {
        var article = new Article("""
                # Title

                ## Section

                First paragraph

                Second paragraph

                Third paragraph""");

        Article.Section section = article.getSections().get(0);
        assertTrue(section.content().contains("First paragraph"));
        assertTrue(section.content().contains("Second paragraph"));
        assertTrue(section.content().contains("Third paragraph"));
    }

    @Test
    void testGetSections_HandlesH3Subsections() {
        var article = new Article("""
                # Title

                ## Main Section

                Main content

                ### Subsection

                Sub content""");

        List<Article.Section> sections = article.getSections();
        assertEquals(1, sections.size(), "H3 should be part of H2 section, not a separate section");
        assertTrue(sections.get(0).content().contains("Subsection"));
    }

    @Test
    void testSection_RecordEquality() {
        Article.Section section1 = new Article.Section("Title", "Content");
        Article.Section section2 = new Article.Section("Title", "Content");
        Article.Section section3 = new Article.Section("Different", "Content");

        assertEquals(section1, section2, "Sections with same values should be equal");
        assertNotEquals(section1, section3, "Sections with different values should not be equal");
    }

    @Test
    void testSection_RecordAccessors() {
        Article.Section section = new Article.Section("Test Heading", "Test Content");

        assertEquals("Test Heading", section.heading());
        assertEquals("Test Content", section.content());
    }

    @Test
    void testGetTitle_NotNull() {
        var article = new Article("""
                # Valid Title

                Content""");

        assertNotNull(article.getTitle(), "getTitle should never return null");
    }

    @Test
    void testGetSections_WithEmptyContent() {
        var article = new Article("""
                # Title

                ## Empty Section

                ## Another Empty Section""");

        assertEquals(2, article.getSections().size());
    }

    @Test
    void testArticle_HandlesComplexMarkdown() {
        var article = new Article("""
                # Sexual Health Information

                ## Overview

                This is an overview with **important** information.

                - Bullet point 1
                - Bullet point 2

                ## Symptoms

                Common symptoms include:

                1. First symptom
                2. Second symptom

                More details here.

                ## Treatment

                Treatment options *vary* based on diagnosis.""");

        assertEquals("Sexual Health Information", article.getTitle());
        assertEquals(3, article.getSections().size());
        assertEquals("Overview", article.getSections().get(0).heading());
        assertEquals("Symptoms", article.getSections().get(1).heading());
        assertEquals("Treatment", article.getSections().get(2).heading());
    }
}
