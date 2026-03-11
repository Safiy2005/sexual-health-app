package com.sddp.sexualhealthapp.article.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.sddp.sexualhealthapp.article.model.Article;
import com.sddp.sexualhealthapp.article.model.ArticleCollection;
import com.sddp.sexualhealthapp.article.model.RecentlyReadEntry;
import com.sddp.sexualhealthapp.util.AppConstants;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persists recently read article state in a manually inspectable JSON file.
 */
public class RecentlyReadService {

    private static final int MAX_RECENT_ITEMS = 5;

    private final Object lock = new Object();
    private final Path storageFilePath;
    private final Gson gson;
    private final Map<String, Article> articlesById;
    private List<RecentlyReadEntry> cachedEntries;

    public RecentlyReadService() {
        this(Paths.get(AppConstants.ARTICLE_STATE_DIR, AppConstants.RECENTLY_READ_FILE),
                ArticleCollection.getInstance());
    }

    public RecentlyReadService(Path storageFilePath) {
        this(storageFilePath, ArticleCollection.getInstance());
    }

    public RecentlyReadService(Path storageFilePath, ArticleCollection articleCollection) {
        this.storageFilePath = storageFilePath;
        this.gson = createGson();
        this.articlesById = articleCollection.getArticles().stream()
                .filter(article -> article.getFileName() != null && !article.getSections().isEmpty())
                .collect(Collectors.toMap(Article::getFileName, article -> article));
        this.cachedEntries = loadAndSanitizeFromDisk();
    }

    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(Instant.class,
                        (JsonDeserializer<Instant>) (json, type, ctx) -> Instant.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    public List<RecentlyReadEntry> loadAll() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(cachedEntries));
        }
    }

    /**
     * Reloads the in-memory recent list from disk, reapplying sanitization.
     * Useful for tests and manual workflows that intentionally edit the JSON file.
     */
    public void reloadFromDisk() {
        synchronized (lock) {
            cachedEntries = loadAndSanitizeFromDisk();
        }
    }

    public List<RecentlyReadEntry> getRecentEntries(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        synchronized (lock) {
            return cachedEntries.stream()
                    .limit(limit)
                    .toList();
        }
    }

    public Optional<RecentlyReadEntry> getEntry(String articleId) {
        if (articleId == null || articleId.isBlank()) {
            return Optional.empty();
        }

        synchronized (lock) {
            return cachedEntries.stream()
                    .filter(entry -> articleId.equals(entry.articleId()))
                    .findFirst();
        }
    }

    public void saveProgress(String articleId, int sectionIndex, Instant timestamp) {
        saveProgressInMemory(articleId, sectionIndex, timestamp);
        flush();
    }

    public void touch(String articleId, Instant timestamp) {
        touchInMemory(articleId, timestamp);
        flush();
    }

    /**
     * Updates the in-memory recent list immediately without doing disk I/O.
     * Call {@link #flush()} to persist the updated state.
     */
    public void saveProgressInMemory(String articleId, int sectionIndex, Instant timestamp) {
        if (timestamp == null || articleId == null || articleId.isBlank()) {
            return;
        }

        Article article = articlesById.get(articleId);
        if (article == null || article.getSections().isEmpty()) {
            return;
        }

        int clampedSectionIndex = clampSectionIndex(article, sectionIndex);

        synchronized (lock) {
            List<RecentlyReadEntry> updatedEntries = new ArrayList<>(cachedEntries);
            upsert(updatedEntries, new RecentlyReadEntry(articleId, clampedSectionIndex, timestamp));
            cachedEntries = updatedEntries;
        }
    }

    /**
     * Updates only the timestamp for an existing in-memory entry.
     * Call {@link #flush()} to persist the updated state.
     */
    public void touchInMemory(String articleId, Instant timestamp) {
        if (timestamp == null || articleId == null || articleId.isBlank()) {
            return;
        }

        synchronized (lock) {
            List<RecentlyReadEntry> updatedEntries = new ArrayList<>(cachedEntries);
            boolean updated = false;

            for (int i = 0; i < updatedEntries.size(); i++) {
                RecentlyReadEntry entry = updatedEntries.get(i);
                if (articleId.equals(entry.articleId())) {
                    updatedEntries.set(i, new RecentlyReadEntry(
                            entry.articleId(),
                            entry.lastReadSectionIndex(),
                            timestamp));
                    updated = true;
                    break;
                }
            }

            if (updated) {
                sortAndPrune(updatedEntries);
                cachedEntries = updatedEntries;
            }
        }
    }

    /**
     * Persists the current in-memory recent list to disk.
     */
    public void flush() {
        List<RecentlyReadEntry> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(cachedEntries);
        }
        saveEntries(snapshot);
    }

    private void upsert(List<RecentlyReadEntry> entries, RecentlyReadEntry updatedEntry) {
        boolean replaced = false;
        for (int i = 0; i < entries.size(); i++) {
            if (updatedEntry.articleId().equals(entries.get(i).articleId())) {
                entries.set(i, updatedEntry);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            entries.add(updatedEntry);
        }

        sortAndPrune(entries);
    }

    private List<RecentlyReadEntry> loadAndSanitizeFromDisk() {
        List<RecentlyReadEntry> rawEntries = loadFromFile();
        Map<String, RecentlyReadEntry> deduped = new LinkedHashMap<>();

        for (RecentlyReadEntry entry : rawEntries) {
            if (entry == null || entry.articleId() == null || entry.articleId().isBlank() || entry.lastReadAt() == null) {
                continue;
            }

            Article article = articlesById.get(entry.articleId());
            if (article == null || article.getSections().isEmpty()) {
                continue;
            }

            int clampedSectionIndex = clampSectionIndex(article, entry.lastReadSectionIndex());
            RecentlyReadEntry sanitized = new RecentlyReadEntry(entry.articleId(), clampedSectionIndex, entry.lastReadAt());

            RecentlyReadEntry existing = deduped.get(entry.articleId());
            if (existing == null || sanitized.lastReadAt().isAfter(existing.lastReadAt())) {
                deduped.put(entry.articleId(), sanitized);
            }
        }

        List<RecentlyReadEntry> sanitized = new ArrayList<>(deduped.values());
        sortAndPrune(sanitized);
        return sanitized;
    }

    private int clampSectionIndex(Article article, int sectionIndex) {
        int maxIndex = article.getSections().size() - 1;
        if (maxIndex < 0) {
            return 0;
        }
        return Math.max(0, Math.min(sectionIndex, maxIndex));
    }

    private void sortAndPrune(List<RecentlyReadEntry> entries) {
        entries.sort(Comparator.comparing(RecentlyReadEntry::lastReadAt).reversed());
        if (entries.size() > MAX_RECENT_ITEMS) {
            entries.subList(MAX_RECENT_ITEMS, entries.size()).clear();
        }
    }

    private List<RecentlyReadEntry> loadFromFile() {
        if (!Files.exists(storageFilePath)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(storageFilePath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<RecentlyReadEntry>>() {
            }.getType();
            List<RecentlyReadEntry> loaded = gson.fromJson(reader, listType);
            return loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Failed to load recently read state: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveEntries(List<RecentlyReadEntry> entries) {
        try {
            Files.createDirectories(storageFilePath.getParent());
            try (Writer writer = Files.newBufferedWriter(storageFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(entries, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save recently read state: " + e.getMessage());
        }
    }
}
