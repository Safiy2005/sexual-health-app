package com.sddp.sexualhealthapp.settings.service;

import com.sddp.sexualhealthapp.settings.model.DisguisePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguisePreferencesServiceTest {

    private Path tempFile;

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void missingFile_returnsDefaultPrivacyFirst() throws IOException {
        tempFile = Files.createTempFile("disguise-preferences-", ".json");
        Files.deleteIfExists(tempFile); // Delete it so it's "missing"

        DisguisePreferencesService service = new DisguisePreferencesService(tempFile);

        assertTrue(service.getPreferences().calcDisguiseEnabled(),
                "Missing file should default to disguise enabled");
    }

    @Test
    void saveThenReload_preservesDisguiseSetting() throws IOException {
        tempFile = Files.createTempFile("disguise-preferences-", ".json");

        DisguisePreferencesService service = new DisguisePreferencesService(tempFile);
        service.save(new DisguisePreferences(false)); // User disables disguise

        DisguisePreferencesService reloaded = new DisguisePreferencesService(tempFile);

        assertFalse(reloaded.getPreferences().calcDisguiseEnabled(),
                "Reloaded service should remember that disguise is false");
    }

    @Test
    void invalidJson_fallsBackToDefaults() throws IOException {
        tempFile = Files.createTempFile("disguise-preferences-", ".json");
        Files.writeString(tempFile, "{ not valid json", StandardCharsets.UTF_8);

        DisguisePreferencesService service = new DisguisePreferencesService(tempFile);

        assertTrue(service.getPreferences().calcDisguiseEnabled(),
                "Invalid JSON should safely fall back to the true default");
    }
}