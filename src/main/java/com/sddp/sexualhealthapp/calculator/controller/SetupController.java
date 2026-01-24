package com.sddp.sexualhealthapp.calculator.controller;

import com.sddp.sexualhealthapp.calculator.model.Calculator;
import com.sddp.sexualhealthapp.calculator.model.SecretEquation;
import com.sddp.sexualhealthapp.calculator.service.SecretAuthService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the setup wizard interface.
 * Uses the calculator interface with an instruction header at the top
 * to guide users through creating their secret equation.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SetupController {

    // Calculator UI elements
    @FXML
    private Label displayLabel;

    @FXML
    private Button clearButton;

    @FXML
    private Button backspaceButton;

    @FXML
    private Button equalsButton;

    // Instruction header elements
    @FXML
    private VBox instructionHeader;

    @FXML
    private Label instructionTitle;

    @FXML
    private Label instructionMessage;

    // Confirmation panel elements
    @FXML
    private VBox confirmationOverlay;

    @FXML
    private VBox confirmationPanel;

    @FXML
    private Label equationPreview;

    @FXML
    private Button confirmButton;

    @FXML
    private Button retryButton;

    private final Calculator calculator;
    private final SecretAuthService authService;

    // Tracks the equation that was entered (captured when equals is pressed)
    private String capturedEquation;
    private String capturedResult;

    // Setup state
    private SetupState currentState;

    /**
     * Enum representing the different states of the setup flow.
     */
    private enum SetupState {
        ENTERING_EQUATION,  // User is entering an equation
        AWAITING_CONFIRMATION,  // Equation entered, waiting for confirm/retry
        CONFIRMED  // User confirmed, saving equation
    }

    /**
     * Constructs a new SetupController with default services.
     */
    public SetupController() {
        this.calculator = new Calculator();
        this.authService = new SecretAuthService();
        this.currentState = SetupState.ENTERING_EQUATION;
    }

    /**
     * Initializes the controller after FXML injection.
     * Called automatically by JavaFX.
     */
    @FXML
    private void initialize() {
        updateDisplay();
        showEnteringState();
    }

    /**
     * Handles number button clicks.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleNumber(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            // User started typing again, reset to entering state
            resetToEnteringState();
        }

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
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }

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
                return;
        }

        calculator.setOperation(operation);
        updateDisplay();
    }

    /**
     * Handles equals button click.
     * Captures the equation and shows the confirmation panel.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleEquals(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            return; // Ignore equals while in confirmation state
        }

        // Capture the equation before calculating
        capturedEquation = calculator.getCurrentEquation();

        // Calculate the result
        calculator.calculateResult();
        capturedResult = calculator.getCurrentDisplay();
        updateDisplay();

        // Check if we have a valid equation
        if (capturedEquation != null && !capturedEquation.isEmpty()
                && !capturedResult.equals(AppConstants.CALC_ERROR_DIV_ZERO)) {
            showConfirmationState();
        } else {
            // Show error message
            showErrorMessage("Please enter a valid equation first");
        }
    }

    /**
     * Handles decimal point button click.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleDecimal(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }

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

        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }
    }

    /**
     * Handles backspace button click.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleBackspace(ActionEvent event) {
        if (currentState == SetupState.AWAITING_CONFIRMATION) {
            resetToEnteringState();
        }

        calculator.backspace();
        updateDisplay();
    }

    /**
     * Handles the confirm button click.
     * Validates and saves the secret equation.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleConfirm(ActionEvent event) {
        currentState = SetupState.CONFIRMED;

        // Parse the captured equation to create a SecretEquation
        SecretEquation equation = parseEquation(capturedEquation, capturedResult);

        if (equation == null) {
            showErrorMessage("Invalid equation. Please try again.");
            resetToEnteringState();
            return;
        }

        // Check if equation is trivial
        if (equation.isTrivial()) {
            showErrorMessage(AppConstants.ERROR_TRIVIAL_EQUATION);
            resetToEnteringState();
            return;
        }

        // Validate the math
        if (!equation.isValid()) {
            showErrorMessage(AppConstants.ERROR_INVALID_MATH);
            resetToEnteringState();
            return;
        }

        // Save the secret equation
        boolean success = authService.setupSecretEquation(equation);

        if (success) {
            showSuccessMessage();
            // Transition to calculator after a brief delay
            PauseTransition pause = new PauseTransition(Duration.millis(1500));
            pause.setOnFinished(e -> SceneManager.getInstance().transitionToCalculator());
            pause.play();
        } else {
            showErrorMessage("Failed to save. Please try again.");
            resetToEnteringState();
        }
    }

    /**
     * Handles the retry button click.
     * Resets the calculator and returns to entering state.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleRetry(ActionEvent event) {
        calculator.clear();
        updateDisplay();
        resetToEnteringState();
    }

    /**
     * Updates the display label with the full equation being built.
     * Shows "7+6" format while entering, or just the result after equals.
     */
    private void updateDisplay() {
        displayLabel.setText(calculator.getFullDisplay());
    }

    /**
     * Shows the entering equation state (initial state).
     * Instruction header shows guidance, confirmation panel is hidden.
     */
    private void showEnteringState() {
        currentState = SetupState.ENTERING_EQUATION;

        instructionTitle.setText("Create Your Secret Equation");
        instructionMessage.setText("Enter an equation you'll remember");
        instructionHeader.getStyleClass().removeAll("instruction-error", "instruction-success");

        // Hide confirmation panel
        confirmationOverlay.setVisible(false);
        confirmationOverlay.setManaged(false);
        confirmationOverlay.setMouseTransparent(true);
    }

    /**
     * Shows the confirmation state after equation is entered.
     * Updates instruction header and shows confirmation panel.
     */
    private void showConfirmationState() {
        currentState = SetupState.AWAITING_CONFIRMATION;

        // Build the full equation string for preview
        String fullEquation = capturedEquation + "=" + capturedResult;
        equationPreview.setText("Your secret: " + fullEquation);

        // Update instruction header
        instructionTitle.setText("Confirm Your Choice");
        instructionMessage.setText("Is this the equation you want?");

        // Show confirmation panel with fade animation
        confirmationOverlay.setVisible(true);
        confirmationOverlay.setManaged(true);
        confirmationOverlay.setMouseTransparent(false);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), confirmationPanel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Resets back to the entering equation state.
     */
    private void resetToEnteringState() {
        capturedEquation = null;
        capturedResult = null;
        showEnteringState();
    }

    /**
     * Shows an error message in the instruction header.
     *
     * @param message the error message to display
     */
    private void showErrorMessage(String message) {
        instructionTitle.setText("Oops!");
        instructionMessage.setText(message);
        instructionHeader.getStyleClass().add("instruction-error");

        // Hide confirmation panel
        confirmationOverlay.setVisible(false);
        confirmationOverlay.setManaged(false);

        // Remove error styling after a delay
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            instructionHeader.getStyleClass().remove("instruction-error");
            showEnteringState();
        });
        pause.play();
    }

    /**
     * Shows a success message in the instruction header.
     */
    private void showSuccessMessage() {
        instructionTitle.setText("Success!");
        instructionMessage.setText("Your secret equation has been saved.");
        instructionHeader.getStyleClass().add("instruction-success");

        // Hide confirmation panel
        confirmationOverlay.setVisible(false);
        confirmationOverlay.setManaged(false);
    }

    /**
     * Parses a calculator equation string into a SecretEquation object.
     *
     * @param equation the equation string (e.g., "7+6")
     * @param result the result string (e.g., "13")
     * @return the parsed SecretEquation, or null if parsing fails
     */
    private SecretEquation parseEquation(String equation, String result) {
        if (equation == null || equation.isEmpty() || result == null) {
            return null;
        }

        try {
            // Find the operator
            String operator = null;
            int operatorIndex = -1;
            String[] operators = {"+", "-", "×", "÷"};

            for (String op : operators) {
                int idx = equation.lastIndexOf(op);
                if (idx > 0) { // Must not be at start (negative numbers)
                    operatorIndex = idx;
                    operator = op;
                    break;
                }
            }

            if (operator == null || operatorIndex <= 0) {
                return null;
            }

            String leftOperand = equation.substring(0, operatorIndex);
            String rightOperand = equation.substring(operatorIndex + 1);

            // Validate that operands are numbers
            Double.parseDouble(leftOperand);
            Double.parseDouble(rightOperand);
            Double.parseDouble(result);

            return new SecretEquation(leftOperand, operator, rightOperand, result);
        } catch (Exception e) {
            return null;
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
