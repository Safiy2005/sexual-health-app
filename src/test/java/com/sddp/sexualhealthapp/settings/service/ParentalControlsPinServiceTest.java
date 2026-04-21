package com.sddp.sexualhealthapp.settings.service;

import com.sddp.sexualhealthapp.security.SecureStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParentalControlsPinServiceTest {

    private ParentalControlsPinService service;

    @BeforeEach
    void setUp() {
        service = new ParentalControlsPinService(new InMemorySecureStorage());
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

    /**
     * Minimal SecureStorage double that keeps "hashed" values in a HashMap.
     * We skip the real BCrypt hashing since the pin service tests only care
     * about the round-trip logic, not the hashing implementation.
     */
    private static final class InMemorySecureStorage extends SecureStorage {

        private final Map<String, String> store = new HashMap<>();

        @Override
        public boolean saveHashed(String key, String plainValue) {
            if (key == null || plainValue == null) {
                return false;
            }
            store.put(key, plainValue);
            return true;
        }

        @Override
        public boolean verifyHash(String key, String plainValue) {
            if (plainValue == null) {
                return false;
            }
            String stored = store.get(key);
            return stored != null && stored.equals(plainValue);
        }

        @Override
        public Optional<String> load(String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public boolean exists(String key) {
            return store.containsKey(key);
        }

        @Override
        public boolean delete(String key) {
            store.remove(key);
            return true;
        }
    }
}
