package com.sddp.sexualhealthapp.settings.service;

import com.sddp.sexualhealthapp.security.SecureStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParentalControlsPinServiceTest {

    private static final String TEST_PREFS_NODE = "/com/sddp/sexualhealthapp/test-parental-controls";

    private Preferences testPreferences;
    private ParentalControlsPinService service;

    @BeforeEach
    void setUp() {
        testPreferences = Preferences.userRoot().node(TEST_PREFS_NODE);
        service = new ParentalControlsPinService(new SecureStorage(testPreferences));
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        testPreferences.removeNode();
    }

    @Test
    void setPin_thenVerify_thenRemove() {
        assertTrue(service.setPin("1234"));
        assertTrue(service.hasPin());
        assertTrue(service.verifyPin("1234"));
        assertFalse(service.verifyPin("9999"));

        assertTrue(service.removePin("1234"));
        assertFalse(service.hasPin());
    }

    @Test
    void changePin_requiresCurrentPin() {
        service.setPin("1234");

        assertFalse(service.changePin("0000", "5678"));
        assertTrue(service.verifyPin("1234"));

        assertTrue(service.changePin("1234", "5678"));
        assertTrue(service.verifyPin("5678"));
        assertFalse(service.verifyPin("1234"));
    }

    @Test
    void invalidFormat_isRejected() {
        assertFalse(ParentalControlsPinService.isValidPinFormat(null));
        assertFalse(ParentalControlsPinService.isValidPinFormat(""));
        assertFalse(ParentalControlsPinService.isValidPinFormat("abc"));
        assertFalse(ParentalControlsPinService.isValidPinFormat("abcd"));
        assertFalse(ParentalControlsPinService.isValidPinFormat("1234a"));
        assertTrue(ParentalControlsPinService.isValidPinFormat("1"));
        assertTrue(ParentalControlsPinService.isValidPinFormat("1234"));
        assertTrue(ParentalControlsPinService.isValidPinFormat("123456789"));

        assertFalse(service.setPin("12a"));
        assertFalse(service.hasPin());
    }
}
