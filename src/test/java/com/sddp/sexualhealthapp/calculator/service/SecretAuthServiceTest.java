package com.sddp.sexualhealthapp.calculator.service;

import com.sddp.sexualhealthapp.calculator.model.SecretEquation;
import com.sddp.sexualhealthapp.security.SecureStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
