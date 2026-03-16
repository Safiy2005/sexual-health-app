package com.sddp.sexualhealthapp.settings.service;

import com.sddp.sexualhealthapp.settings.model.ContentPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentPreferencesServiceTest {

    private Path tempFile;

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void missingFile_returnsEmptyDefaults() throws IOException {
        tempFile = Files.createTempFile("content-preferences-", ".json");
        Files.deleteIfExists(tempFile);

        ContentPreferencesService service = new ContentPreferencesService(tempFile);

        assertEquals(ContentPreferences.empty(), service.getPreferences());
    }

    @Test
    void saveThenReload_preservesBlockedAndPreferredTags() throws IOException {
        tempFile = Files.createTempFile("content-preferences-", ".json");

        ContentPreferencesService service = new ContentPreferencesService(tempFile);
        service.savePreferences(new ContentPreferences(
                List.of("STIs"),
                List.of("LGBTQ+", "Mental Health & Wellbeing")));

        ContentPreferencesService reloaded = new ContentPreferencesService(tempFile);

        assertEquals(List.of("STIs"), reloaded.getPreferences().blockedTags());
        assertEquals(List.of("LGBTQ+", "Mental Health & Wellbeing"),
                reloaded.getPreferences().preferredTags());
    }

    @Test
    void invalidJson_fallsBackToDefaults() throws IOException {
        tempFile = Files.createTempFile("content-preferences-", ".json");
        Files.writeString(tempFile, "{ not valid json", StandardCharsets.UTF_8);

        ContentPreferencesService service = new ContentPreferencesService(tempFile);

        assertEquals(ContentPreferences.empty(), service.getPreferences());
    }
}
