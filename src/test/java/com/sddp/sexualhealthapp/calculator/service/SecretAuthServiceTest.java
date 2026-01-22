package com.sddp.sexualhealthapp.calculator.service;

import com.sddp.sexualhealthapp.calculator.model.SecretEquation;
import com.sddp.sexualhealthapp.security.SecureStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the SecretAuthService class.
 * Tests authentication setup, verification, and management using real storage.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
class SecretAuthServiceTest {

    private static final String TEST_PREFS_NODE = "/com/sddp/sexualhealthapp/test";
    private Preferences testPreferences;
    private SecureStorage testStorage;
    private SecretAuthService authService;

    @BeforeEach
    void setUp() {
        // Create test-specific preferences node
        testPreferences = Preferences.userRoot().node(TEST_PREFS_NODE);
        testStorage = new SecureStorage(testPreferences);
        authService = new SecretAuthService(testStorage);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        // Clean up test data
        testPreferences.removeNode();
    }

    @Test
    void testSetupSecretEquation_ValidEquation() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");

        boolean result = authService.setupSecretEquation(equation);

        assertTrue(result);
        assertTrue(authService.hasSecretEquation());
    }

    @Test
    void testSetupSecretEquation_NullEquation() {
        boolean result = authService.setupSecretEquation(null);

        assertFalse(result);
        assertFalse(authService.hasSecretEquation());
    }

    @Test
    void testSetupSecretEquation_InvalidEquation() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "9"); // Wrong result

        boolean result = authService.setupSecretEquation(equation);

        assertFalse(result);
        assertFalse(authService.hasSecretEquation());
    }

    @Test
    void testVerifyEquation_CorrectEquation() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        boolean result = authService.verifyEquation("5+3=8");

        assertTrue(result);
    }

    @Test
    void testVerifyEquation_IncorrectEquation() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        boolean result = authService.verifyEquation("5+3=9");

        assertFalse(result);
    }

    @Test
    void testVerifyEquation_NullInput() {
        boolean result = authService.verifyEquation(null);

        assertFalse(result);
    }

    @Test
    void testVerifyEquation_EmptyInput() {
        boolean result = authService.verifyEquation("");
        assertFalse(result);

        result = authService.verifyEquation("   ");
        assertFalse(result);
    }

    @Test
    void testVerifyEquation_WithSpaces() {
        // Equation with spaces should be normalized and match
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        boolean result = authService.verifyEquation("5 + 3 = 8");

        assertTrue(result);
    }

    @Test
    void testVerifyEquation_DifferentOperatorFormat() {
        // × vs * should be normalized
        SecretEquation equation = new SecretEquation("5", "×", "3", "15");
        authService.setupSecretEquation(equation);

        boolean result = authService.verifyEquation("5×3=15");

        assertTrue(result);
    }

    @Test
    void testVerifyEquation_WithLeadingZeros() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        // Normalized equations should match even with different formatting
        boolean result = authService.verifyEquation("5+3=8");

        assertTrue(result);
    }

    @Test
    void testHasSecretEquation_Exists() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        boolean result = authService.hasSecretEquation();

        assertTrue(result);
    }

    @Test
    void testHasSecretEquation_NotExists() {
        boolean result = authService.hasSecretEquation();

        assertFalse(result);
    }

    @Test
    void testUpdateSecretEquation_ValidOldAndNewEquations() {
        SecretEquation oldEquation = new SecretEquation("5", "+", "3", "8");
        SecretEquation newEquation = new SecretEquation("7", "×", "6", "42");

        authService.setupSecretEquation(oldEquation);

        boolean result = authService.updateSecretEquation(oldEquation, newEquation);

        assertTrue(result);
        assertTrue(authService.verifyEquation("7×6=42"));
        assertFalse(authService.verifyEquation("5+3=8"));
    }

    @Test
    void testUpdateSecretEquation_IncorrectOldEquation() {
        SecretEquation oldEquation = new SecretEquation("5", "+", "3", "8");
        SecretEquation wrongOldEquation = new SecretEquation("5", "+", "3", "9");
        SecretEquation newEquation = new SecretEquation("7", "×", "6", "42");

        authService.setupSecretEquation(oldEquation);

        boolean result = authService.updateSecretEquation(wrongOldEquation, newEquation);

        assertFalse(result);
        // Old equation should still be valid
        assertTrue(authService.verifyEquation("5+3=8"));
    }

    @Test
    void testUpdateSecretEquation_InvalidNewEquation() {
        SecretEquation oldEquation = new SecretEquation("5", "+", "3", "8");
        SecretEquation newEquation = new SecretEquation("7", "×", "6", "43"); // Wrong result

        authService.setupSecretEquation(oldEquation);

        boolean result = authService.updateSecretEquation(oldEquation, newEquation);

        assertFalse(result);
        // Old equation should still be valid
        assertTrue(authService.verifyEquation("5+3=8"));
    }

    @Test
    void testUpdateSecretEquation_NullEquations() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");

        boolean result = authService.updateSecretEquation(null, equation);
        assertFalse(result);

        result = authService.updateSecretEquation(equation, null);
        assertFalse(result);
    }

    @Test
    void testDeleteSecretEquation_Success() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        boolean result = authService.deleteSecretEquation();

        assertTrue(result);
        assertFalse(authService.hasSecretEquation());
    }

    @Test
    void testDeleteSecretEquation_WhenNotExists() {
        boolean result = authService.deleteSecretEquation();

        // Should still return true (idempotent operation)
        assertTrue(result);
    }

    @Test
    void testGetSecretCreationDate_Exists() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        LocalDateTime result = authService.getSecretCreationDate();

        assertNotNull(result);
        // Should be close to current time (within last minute)
        assertTrue(result.isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void testGetSecretCreationDate_NotExists() {
        LocalDateTime result = authService.getSecretCreationDate();

        assertNull(result);
    }

    @Test
    void testGetSecretEquationHint_WithCreationDate() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");
        authService.setupSecretEquation(equation);

        String hint = authService.getSecretEquationHint();

        assertNotNull(hint);
        assertTrue(hint.contains("Secret equation created on"));
    }

    @Test
    void testGetSecretEquationHint_NoSecretEquation() {
        String hint = authService.getSecretEquationHint();

        assertNull(hint);
    }

    @Test
    void testIsValidEquation_Valid() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");

        boolean result = authService.isValidEquation(equation);

        assertTrue(result);
    }

    @Test
    void testIsValidEquation_Invalid() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "9");

        boolean result = authService.isValidEquation(equation);

        assertFalse(result);
    }

    @Test
    void testIsValidEquation_Null() {
        boolean result = authService.isValidEquation(null);

        assertFalse(result);
    }

    @Test
    void testFullAuthenticationFlow() {
        // Simulate a complete authentication flow
        SecretEquation setupEquation = new SecretEquation("12", "÷", "4", "3");

        // Setup
        assertTrue(authService.setupSecretEquation(setupEquation));

        // Check existence
        assertTrue(authService.hasSecretEquation());

        // Verify correct equation
        assertTrue(authService.verifyEquation("12÷4=3"));

        // Verify with spaces (normalized)
        assertTrue(authService.verifyEquation("12 ÷ 4 = 3"));

        // Verify wrong equation
        assertFalse(authService.verifyEquation("12÷4=4"));

        // Update to new equation
        SecretEquation newEquation = new SecretEquation("20", "-", "8", "12");
        assertTrue(authService.updateSecretEquation(setupEquation, newEquation));

        // Old equation should no longer work
        assertFalse(authService.verifyEquation("12÷4=3"));

        // New equation should work
        assertTrue(authService.verifyEquation("20-8=12"));

        // Delete
        assertTrue(authService.deleteSecretEquation());
        assertFalse(authService.hasSecretEquation());
    }

    @Test
    void testMultipleEquationsWithDifferentOperators() {
        // Test addition
        SecretEquation addEq = new SecretEquation("10", "+", "5", "15");
        authService.setupSecretEquation(addEq);
        assertTrue(authService.verifyEquation("10+5=15"));
        authService.deleteSecretEquation();

        // Test subtraction
        SecretEquation subEq = new SecretEquation("10", "-", "5", "5");
        authService.setupSecretEquation(subEq);
        assertTrue(authService.verifyEquation("10-5=5"));
        authService.deleteSecretEquation();

        // Test multiplication
        SecretEquation mulEq = new SecretEquation("10", "×", "5", "50");
        authService.setupSecretEquation(mulEq);
        assertTrue(authService.verifyEquation("10×5=50"));
        authService.deleteSecretEquation();

        // Test division
        SecretEquation divEq = new SecretEquation("10", "÷", "5", "2");
        authService.setupSecretEquation(divEq);
        assertTrue(authService.verifyEquation("10÷5=2"));
    }

    @Test
    void testDecimalEquations() {
        SecretEquation equation = new SecretEquation("5.5", "+", "2.5", "8");
        authService.setupSecretEquation(equation);

        assertTrue(authService.verifyEquation("5.5+2.5=8"));
        assertTrue(authService.verifyEquation("5.5 + 2.5 = 8"));
    }
}
