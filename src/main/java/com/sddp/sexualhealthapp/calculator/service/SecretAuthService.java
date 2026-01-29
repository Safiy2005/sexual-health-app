package com.sddp.sexualhealthapp.calculator.service;

import com.sddp.sexualhealthapp.calculator.model.SecretEquation;
import com.sddp.sexualhealthapp.security.SecureStorage;
import com.sddp.sexualhealthapp.util.AppConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SecretAuthService {

    private final SecureStorage secureStorage;

    public SecretAuthService() {
        this.secureStorage = new SecureStorage();
    }

    /**
     * Constructs a SecretAuthService with custom secure storage (for testing).
     *
     * @param secureStorage the secure storage instance to use
     */
    public SecretAuthService(SecureStorage secureStorage) {
        this.secureStorage = secureStorage;
    }

    /**
     * Sets up a new secret equation for authentication.
     * The equation is validated and then stored securely using BCrypt hashing.
     *
     * @param equation the secret equation to set up
     * @return true if setup was successful, false otherwise
     */
    public boolean setupSecretEquation(SecretEquation equation) {
        if (equation == null || !equation.isValid()) {
            return false;
        }

        String normalizedEquation = EquationMatcher.normalizeEquation(equation.getFullEquation());

        boolean saved = secureStorage.saveHashed(
            AppConstants.SECRET_EQUATION_KEY,
            normalizedEquation
        );

        if (saved) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            secureStorage.save(AppConstants.SECRET_CREATION_DATE_KEY, timestamp);
        }

        return saved;
    }

    /**
     * Verifies if an attempted equation matches the stored secret equation.
     * The comparison is done using BCrypt's secure verification.
     *
     * @param attemptedEquation the equation to verify
     * @return true if the equation matches the secret, false otherwise
     */
    public boolean verifyEquation(String attemptedEquation) {
        if (attemptedEquation == null || attemptedEquation.trim().isEmpty()) {
            return false;
        }

        String normalizedAttempt = EquationMatcher.normalizeEquation(attemptedEquation);

        return secureStorage.verifyHash(
            AppConstants.SECRET_EQUATION_KEY,
            normalizedAttempt
        );
    }

    public boolean hasSecretEquation() {
        return secureStorage.exists(AppConstants.SECRET_EQUATION_KEY);
    }

    /**
     * Updates the secret equation with a new one.
     * This requires the old equation to be provided for verification.
     *
     * @param oldEquation the current equation (for verification)
     * @param newEquation the new equation to set
     * @return true if update was successful, false otherwise
     */
    public boolean updateSecretEquation(SecretEquation oldEquation, SecretEquation newEquation) {
        if (oldEquation == null || !verifyEquation(oldEquation.getFullEquation())) {
            return false;
        }

        if (newEquation == null || !newEquation.isValid()) {
            return false;
        }

        secureStorage.delete(AppConstants.SECRET_EQUATION_KEY);
        return setupSecretEquation(newEquation);
    }

    /**
     * Removes the secret equation from storage.
     * This effectively resets the authentication, requiring setup again.
     *
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteSecretEquation() {
        boolean deletedEquation = secureStorage.delete(AppConstants.SECRET_EQUATION_KEY);
        boolean deletedDate = secureStorage.delete(AppConstants.SECRET_CREATION_DATE_KEY);
        return deletedEquation && deletedDate;
    }

    public LocalDateTime getSecretCreationDate() {
        return secureStorage.load(AppConstants.SECRET_CREATION_DATE_KEY)
            .map(dateStr -> LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .orElse(null);
    }

    /**
     * Gets a hint about the secret equation structure (without revealing the equation itself).
     * This is for future password recovery features.
     *
     * @return a hint string, or null if no secret is set
     */
    public String getSecretEquationHint() {
        if (!hasSecretEquation()) {
            return null;
        }

        LocalDateTime creationDate = getSecretCreationDate();
        if (creationDate != null) {
            return "Secret equation created on: " +
                   creationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        return "Secret equation is set";
    }

    public boolean isValidEquation(SecretEquation equation) {
        return equation != null && equation.isValid();
    }
}
