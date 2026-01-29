package com.sddp.sexualhealthapp.calculator.service;

import com.sddp.sexualhealthapp.calculator.model.SecretEquation;
import com.sddp.sexualhealthapp.security.SecureStorage;
import com.sddp.sexualhealthapp.util.AppConstants;

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

        return secureStorage.saveHashed(
            AppConstants.SECRET_EQUATION_KEY,
            normalizedEquation
        );
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
     * Removes the secret equation from storage.
     * This effectively resets the authentication, requiring setup again.
     *
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteSecretEquation() {
        return secureStorage.delete(AppConstants.SECRET_EQUATION_KEY);
    }
}
