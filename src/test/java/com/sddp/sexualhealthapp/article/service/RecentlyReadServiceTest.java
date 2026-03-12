package com.sddp.sexualhealthapp.article.service;

import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentlyReadServiceTest {

    private Path tempFile;
    private RecentlyReadService service;
    private List<Article> testArticles;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = Files.createTempFile("recently-read-test-", ".json");
        Files.deleteIfExists(tempFile);
        testArticles = ArticleCollection.getInstance().getArticles().stream()
                .filter(article -> article.getFileName() != null && !article.getSections().isEmpty())
                .limit(6)
                .toList();
        service = new RecentlyReadService(tempFile, ArticleCollection.getInstance());
    }

    @Test
    void saveProgress_createsNewEntry() {
        Article article = testArticles.get(0);

        service.saveProgress(article.getFileName(), 0, Instant.parse("2026-03-11T09:15:00Z"));

        List<RecentlyReadEntry> entries = service.loadAll();
        assertEquals(1, entries.size());
        assertEquals(article.getFileName(), entries.get(0).articleId());
        assertEquals(0, entries.get(0).lastReadSectionIndex());
    }

    @Test
    void saveProgress_updatesExistingEntryWithoutDuplicating() {
        Article article = testArticles.get(0);

        service.saveProgress(article.getFileName(), 0, Instant.parse("2026-03-11T09:15:00Z"));
        service.saveProgress(article.getFileName(), 2, Instant.parse("2026-03-11T09:20:00Z"));

        List<RecentlyReadEntry> entries = service.loadAll();
        assertEquals(1, entries.size());
        assertEquals(2, entries.get(0).lastReadSectionIndex());
        assertEquals(Instant.parse("2026-03-11T09:20:00Z"), entries.get(0).lastReadAt());
    }

    @Test
    void getRecentEntries_keepsNewestFirst() {
        service.saveProgress(testArticles.get(0).getFileName(), 0, Instant.parse("2026-03-11T09:00:00Z"));
        service.saveProgress(testArticles.get(1).getFileName(), 1, Instant.parse("2026-03-11T10:00:00Z"));
        service.touch(testArticles.get(0).getFileName(), Instant.parse("2026-03-11T11:00:00Z"));

        List<RecentlyReadEntry> entries = service.getRecentEntries(5);
        assertEquals(testArticles.get(0).getFileName(), entries.get(0).articleId());
        assertEquals(testArticles.get(1).getFileName(), entries.get(1).articleId());
    }

    @Test
    void saveProgress_enforcesFiveItemLimit() {
        for (int i = 0; i < testArticles.size(); i++) {
            service.saveProgress(
                    testArticles.get(i).getFileName(),
                    0,
                    Instant.parse("2026-03-11T09:00:0" + i + "Z"));
        }

        List<RecentlyReadEntry> entries = service.loadAll();
        assertEquals(5, entries.size());
        assertEquals(testArticles.get(5).getFileName(), entries.get(0).articleId());
        assertTrue(entries.stream().noneMatch(entry -> testArticles.get(0).getFileName().equals(entry.articleId())));
    }

    @Test
    void loadAll_skipsUnknownArticleIds() throws Exception {
        Article article = testArticles.get(0);
        Files.createDirectories(tempFile.getParent());
        Files.writeString(tempFile, """
                [
                  {"articleId":"missing.md","lastReadSectionIndex":1,"lastReadAt":"2026-03-11T08:00:00Z"},
                  {"articleId":"%s","lastReadSectionIndex":0,"lastReadAt":"2026-03-11T09:00:00Z"}
                ]
                """.formatted(article.getFileName()));
        service.reloadFromDisk();

        List<RecentlyReadEntry> entries = service.loadAll();
        assertEquals(1, entries.size());
        assertEquals(article.getFileName(), entries.get(0).articleId());
    }

    @Test
    void loadAll_clampsOutOfRangeSectionIndices() throws Exception {
        Article article = testArticles.get(0);
        Files.createDirectories(tempFile.getParent());
        Files.writeString(tempFile, """
                [
                  {"articleId":"%s","lastReadSectionIndex":999,"lastReadAt":"2026-03-11T09:00:00Z"}
                ]
                """.formatted(article.getFileName()));
        service.reloadFromDisk();

        List<RecentlyReadEntry> entries = service.loadAll();
        assertEquals(1, entries.size());
        assertEquals(article.getSections().size() - 1, entries.get(0).lastReadSectionIndex());
    }

    @Test
    void loadAll_deduplicatesByNewestTimestamp() throws Exception {
        Article article = testArticles.get(0);
        Files.createDirectories(tempFile.getParent());
        Files.writeString(tempFile, """
                [
                  {"articleId":"%s","lastReadSectionIndex":0,"lastReadAt":"2026-03-11T09:00:00Z"},
                  {"articleId":"%s","lastReadSectionIndex":2,"lastReadAt":"2026-03-11T10:00:00Z"}
                ]
                """.formatted(article.getFileName(), article.getFileName()));
        service.reloadFromDisk();

        List<RecentlyReadEntry> entries = service.loadAll();
        assertEquals(1, entries.size());
        assertEquals(2, entries.get(0).lastReadSectionIndex());
    }

    @Test
    void invalidJson_recoversAsEmptyAndOverwritesOnNextSave() throws Exception {
        Files.createDirectories(tempFile.getParent());
        Files.writeString(tempFile, "{ definitely not valid json");
        service.reloadFromDisk();

        assertTrue(service.loadAll().isEmpty());

        Article article = testArticles.get(0);
        service.saveProgress(article.getFileName(), 0, Instant.parse("2026-03-11T10:30:00Z"));

        List<RecentlyReadEntry> entries = service.loadAll();
        assertEquals(1, entries.size());
        assertEquals(article.getFileName(), entries.get(0).articleId());
    }
}
