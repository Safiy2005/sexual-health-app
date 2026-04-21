package com.sddp.sexualhealthapp.settings.service;

import com.sddp.sexualhealthapp.security.SecureStorage;
import com.sddp.sexualhealthapp.util.AppConstants;

/**
 * Handles hashed storage and verification for the parental controls PIN.
 */
public class ParentalControlsPinService {

    private static ParentalControlsPinService instance;

    private final SecureStorage secureStorage;

    public static synchronized ParentalControlsPinService getInstance() {
        if (instance == null) {
            instance = new ParentalControlsPinService();
        }
        return instance;
    }

    public ParentalControlsPinService() {
        this(new SecureStorage());
    }

    public ParentalControlsPinService(SecureStorage secureStorage) {
        this.secureStorage = secureStorage;
    }

    public boolean hasPin() {
        return secureStorage.exists(AppConstants.PARENTAL_CONTROLS_PIN_KEY);
    }

    public boolean verifyPin(String pin) {
        if (!isValidPinFormat(pin)) {
            return false;
        }

        return secureStorage.verifyHash(AppConstants.PARENTAL_CONTROLS_PIN_KEY, normalize(pin));
    }

    public boolean setPin(String newPin) {
        if (!isValidPinFormat(newPin)) {
            return false;
        }

        return secureStorage.saveHashed(AppConstants.PARENTAL_CONTROLS_PIN_KEY, normalize(newPin));
    }

    public boolean changePin(String currentPin, String newPin) {
        if (!verifyPin(currentPin) || !isValidPinFormat(newPin)) {
            return false;
        }

        return setPin(newPin);
    }

    public boolean removePin(String currentPin) {
        if (!verifyPin(currentPin)) {
            return false;
        }

        return secureStorage.delete(AppConstants.PARENTAL_CONTROLS_PIN_KEY);
    }

    public boolean removePinIfPresent() {
        return secureStorage.delete(AppConstants.PARENTAL_CONTROLS_PIN_KEY);
    }

    public static boolean isValidPinFormat(String pin) {
        if (pin == null) {
            return false;
        }

        String normalized = normalize(pin);
        return normalized.matches("\\d+");
    }

    private static String normalize(String pin) {
        return pin == null ? "" : pin.trim();
    }
}
