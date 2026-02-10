package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TextPreprocessorTest {

    @Test
    void testNormalize_Lowercase() {
        String result = TextPreprocessor.normalize("HELLO WORLD");
        assertEquals("hello world", result, "Should convert to lowercase");
    }

    @Test
    void testNormalize_RemovesSpecialChars() {
        String result = TextPreprocessor.normalize("hello! world?");
        assertEquals("hello world", result, "Should remove punctuation");
    }

    @Test
    void testNormalize_CollapsesWhitespace() {
        String result = TextPreprocessor.normalize("hello    world");
        assertEquals("hello world", result, "Should collapse multiple spaces");
    }

    @Test
    void testNormalize_TrimsWhitespace() {
        String result = TextPreprocessor.normalize("  hello world  ");
        assertEquals("hello world", result, "Should trim leading and trailing spaces");
    }

    @Test
    void testNormalize_HandlesNull() {
        String result = TextPreprocessor.normalize(null);
        assertEquals("", result, "Should return empty string for null input");
    }

    @Test
    void testNormalize_HandlesEmpty() {
        String result = TextPreprocessor.normalize("");
        assertEquals("", result, "Should return empty string for empty input");
    }

    @Test
    void testNormalize_PreservesNumbers() {
        String result = TextPreprocessor.normalize("STI 123");
        assertEquals("sti 123", result, "Should preserve numbers");
    }

    @Test
    void testNormalize_CombinedFormatting() {
        String result = TextPreprocessor.normalize("  HELLO!!!   World???  ");
        assertEquals("hello world", result, "Should handle multiple formatting issues");
    }

    @Test
    void testTokenize_RemovesStopwords() {
        List<String> result = TextPreprocessor.tokenize("the quick brown fox");
        assertEquals(List.of("quick", "brown", "fox"), result,
            "Should remove 'the' stopword");
    }

    @Test
    void testTokenize_HandlesNull() {
        List<String> result = TextPreprocessor.tokenize(null);
        assertTrue(result.isEmpty(), "Should return empty list for null");
    }

    @Test
    void testTokenize_HandlesEmpty() {
        List<String> result = TextPreprocessor.tokenize("");
        assertTrue(result.isEmpty(), "Should return empty list for empty string");
    }

    @Test
    void testTokenize_HandlesWhitespace() {
        List<String> result = TextPreprocessor.tokenize("   ");
        assertTrue(result.isEmpty(), "Should return empty list for whitespace");
    }

    @Test
    void testTokenize_SingleWord() {
        List<String> result = TextPreprocessor.tokenize("hello");
        assertEquals(List.of("hello"), result, "Should tokenize single word");
    }

    @Test
    void testTokenize_MultipleWords() {
        List<String> result = TextPreprocessor.tokenize("hello world test");
        assertEquals(List.of("hello", "world", "test"), result,
            "Should tokenize multiple words");
    }

    @Test
    void testTokenize_RemovesMultipleStopwords() {
        List<String> result = TextPreprocessor.tokenize("the cat and the dog");
        assertEquals(List.of("cat", "dog"), result,
            "Should remove all stopwords (the, and)");
    }

    @Test
    void testTokenize_AllStopwords() {
        List<String> result = TextPreprocessor.tokenize("the and for");
        assertTrue(result.isEmpty(),
            "Should return empty list when only stopwords present");
    }

    @Test
    void testExtractFields_FromArticle() {
        var article = new Article("""
                # Test Article Title

                ## Section One

                Content for section one.

                ## Section Two

                Content for section two.""");

        Map<String, String> fields = TextPreprocessor.extractFields(article);

        assertEquals("Test Article Title", fields.get("title"),
            "Should extract title");
        assertEquals("Section One Section Two", fields.get("headings"),
            "Should extract and join headings");
        assertTrue(fields.get("content").contains("Content for section one"),
            "Should extract content from first section");
        assertTrue(fields.get("content").contains("Content for section two"),
            "Should extract content from second section");
    }

    @Test
    void testExtractFields_HandlesNull() {
        Map<String, String> fields = TextPreprocessor.extractFields(null);

        assertEquals("", fields.get("title"), "Should return empty title for null");
        assertEquals("", fields.get("headings"), "Should return empty headings for null");
        assertEquals("", fields.get("content"), "Should return empty content for null");
    }

    @Test
    void testExtractFields_EmptyArticle() {
        var article = new Article("""
                # Title Only

                No sections here.""");

        Map<String, String> fields = TextPreprocessor.extractFields(article);

        assertEquals("Title Only", fields.get("title"));
        assertEquals("", fields.get("headings"), "Should return empty headings when no sections");
        assertEquals("", fields.get("content"), "Should return empty content when no sections");
    }

    @Test
    void testExtractFields_SingleSection() {
        var article = new Article("""
                # Article Title

                ## Only Section

                Single section content.""");

        Map<String, String> fields = TextPreprocessor.extractFields(article);

        assertEquals("Only Section", fields.get("headings"));
        assertTrue(fields.get("content").contains("Single section content"));
    }

    @Test
    void testExtractFields_PreservesOrder() {
        var article = new Article("""
                # Title

                ## First
                First content

                ## Second
                Second content

                ## Third
                Third content""");

        Map<String, String> fields = TextPreprocessor.extractFields(article);

        String headings = fields.get("headings");
        int firstIndex = headings.indexOf("First");
        int secondIndex = headings.indexOf("Second");
        int thirdIndex = headings.indexOf("Third");

        assertTrue(firstIndex < secondIndex && secondIndex < thirdIndex,
            "Should preserve section order");
    }

    @Test
    void testCannotInstantiate() {
        var thrown = assertThrows(Exception.class, () -> {
            var constructor = TextPreprocessor.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }, "Should throw exception when trying to instantiate");

        assertTrue(thrown.getCause() instanceof AssertionError,
            "Should throw AssertionError (wrapped in InvocationTargetException)");
    }
}
