package com.sddp.sexualhealthapp.calculator.controller;

import com.sddp.sexualhealthapp.calculator.model.Calculator;
import com.sddp.sexualhealthapp.calculator.service.EquationMatcher;
import com.sddp.sexualhealthapp.calculator.service.SecretAuthService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller for the calculator interface.
 * Handles all calculator button interactions and checks for secret equation authentication.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class CalculatorController {

    @FXML
    private Label displayLabel;

    @FXML
    private Button clearButton;

    @FXML
    private Button backspaceButton;

    @FXML
    private Button equalsButton;

    private final Calculator calculator;
    private final SecretAuthService authService;

    /**
     * Constructs a new CalculatorController with default services.
     */
    public CalculatorController() {
        this.calculator = new Calculator();
        this.authService = new SecretAuthService();
    }

    /**
     * Initializes the controller after FXML injection.
     * Called automatically by JavaFX.
     */
    @FXML
    private void initialize() {
        updateDisplay();
    }

    /**
     * Handles number button clicks.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleNumber(ActionEvent event) {
        Button button = (Button) event.getSource();
        String digit = button.getText();
        calculator.appendDigit(digit);
        updateDisplay();
    }

    /**
     * Handles operation button clicks (+, -, ×, ÷).
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleOperation(ActionEvent event) {
        Button button = (Button) event.getSource();
        String operatorText = button.getText();

        Calculator.Operation operation;
        switch (operatorText) {
            case "+":
                operation = Calculator.Operation.ADD;
                break;
            case "-":
                operation = Calculator.Operation.SUBTRACT;
                break;
            case "×":
                operation = Calculator.Operation.MULTIPLY;
                break;
            case "÷":
                operation = Calculator.Operation.DIVIDE;
                break;
            default:
                return; // Unknown operator
        }

        calculator.setOperation(operation);
        updateDisplay();
    }

    /**
     * Handles equals button click.
     * Calculates the result and checks if the equation matches the secret.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleEquals(ActionEvent event) {
        // Check for reset code BEFORE calculating
        checkForResetCode();
        
        calculator.calculateResult();
        updateDisplay();

        // Check if the equation matches the secret
        checkForSecretEquation();
    }

    /**
     * Handles decimal point button click.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleDecimal(ActionEvent event) {
        calculator.appendDecimal();
        updateDisplay();
    }

    /**
     * Handles clear button click.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleClear(ActionEvent event) {
        calculator.clear();
        updateDisplay();
    }

    /**
     * Handles backspace button click.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleBackspace(ActionEvent event) {
        calculator.backspace();
        updateDisplay();
    }

    /**
     * Updates the display label with the full equation being built.
     * Shows "7+6" format while entering, or just the result after equals.
     */
    private void updateDisplay() {
        displayLabel.setText(calculator.getFullDisplay());
    }

    /**
     * Checks if the last completed equation matches the secret equation.
     * If it matches, transitions to the main app.
     */
    private void checkForSecretEquation() {
        // Get the last equation from history
        String lastEquation = EquationMatcher.extractLastEquation(calculator);

        if (lastEquation != null && !lastEquation.isEmpty()) {
            // Verify against stored secret
            if (authService.verifyEquation(lastEquation)) {
                // Successful authentication - transition to main app
                System.out.println("Authentication successful!");
                SceneManager.getInstance().transitionToMainApp();
            }
        }
    }

    /**
     * Checks if the user entered the reset code (999÷0).
     * If detected, deletes the secret equation and navigates to setup.
     */
    private void checkForResetCode() {
        // Get the current equation being entered
        String currentEquation = calculator.getCurrentEquation();
        
        // Use EquationMatcher to check for reset code
        if (EquationMatcher.isResetCode(currentEquation)) {
            System.out.println("Reset code detected! Deleting secret equation...");
            
            // Delete the secret equation
            boolean deleted = authService.deleteSecretEquation();
            
            if (deleted) {
                System.out.println("Secret equation deleted successfully.");
                // Clear the calculator
                calculator.clear();
                updateDisplay();
                // Navigate to setup screen
                SceneManager.getInstance().transitionToSetup();
            } else {
                System.err.println("Failed to delete secret equation.");
            }
        }
    }

    /**
     * Gets the calculator model (for testing purposes).
     *
     * @return the calculator model
     */
    public Calculator getCalculator() {
        return calculator;
    }
}
