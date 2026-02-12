package com.sddp.sexualhealthapp.calculator.controller;

import com.sddp.sexualhealthapp.calculator.model.Calculator;
import com.sddp.sexualhealthapp.calculator.service.EquationMatcher;
import com.sddp.sexualhealthapp.calculator.service.SecretAuthService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

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

    @FXML
    private void handleNumber(ActionEvent event) {
        Button button = (Button) event.getSource();
        String digit = button.getText();
        calculator.appendDigit(digit);
        updateDisplay();
    }

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

    @FXML
    private void handleEquals(ActionEvent event) {
        // Check for reset code BEFORE calculating
        checkForResetCode();

        calculator.calculateResult();
        updateDisplay();

        checkForSecretEquation();
    }

    @FXML
    private void handleDecimal(ActionEvent event) {
        calculator.appendDecimal();
        updateDisplay();
    }

    @FXML
    private void handleClear(ActionEvent event) {
        calculator.clear();
        updateDisplay();
    }

    @FXML
    private void handleBackspace(ActionEvent event) {
        calculator.backspace();
        updateDisplay();
    }

    private void updateDisplay() {
        displayLabel.setText(calculator.getFullDisplay());
    }

    /**
     * Checks if the last completed equation matches the secret equation.
     * If it matches, transitions to the main app.
     */
    private void checkForSecretEquation() {
        String lastEquation = EquationMatcher.extractLastEquation(calculator);

        if (lastEquation != null && !lastEquation.isEmpty()) {
            if (authService.verifyEquation(lastEquation)) {
                if (SceneManager.getInstance().isTransitioning()) return;
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
        String currentEquation = calculator.getCurrentEquation();

        if (EquationMatcher.isResetCode(currentEquation)) {
            System.out.println("Reset code detected! Deleting secret equation...");
            
            boolean deleted = authService.deleteSecretEquation();

            if (deleted) {
                if (SceneManager.getInstance().isTransitioning()) return;
                System.out.println("Secret equation deleted successfully.");
                calculator.clear();
                updateDisplay();
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
