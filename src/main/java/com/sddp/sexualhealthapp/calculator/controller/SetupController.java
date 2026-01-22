package com.sddp.sexualhealthapp.calculator.controller;

import com.sddp.sexualhealthapp.calculator.model.SecretEquation;
import com.sddp.sexualhealthapp.calculator.service.SecretAuthService;
import com.sddp.sexualhealthapp.navigation.SceneManager;
import com.sddp.sexualhealthapp.util.AppConstants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the setup wizard interface.
 * Handles first-time setup of the secret equation.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SetupController {

    @FXML
    private TextField leftOperandField;

    @FXML
    private ChoiceBox<String> operatorChoice;

    @FXML
    private TextField rightOperandField;

    @FXML
    private TextField resultField;

    @FXML
    private Label previewLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private Button confirmButton;

    private final SecretAuthService authService;

    /**
     * Constructs a new SetupController with default authentication service.
     */
    public SetupController() {
        this.authService = new SecretAuthService();
    }

    /**
     * Initializes the controller after FXML injection.
     * Called automatically by JavaFX.
     */
    @FXML
    private void initialize() {
        // Populate operator choices
        operatorChoice.getItems().addAll("+", "-", "×", "÷");
        operatorChoice.setValue("+"); // Default to addition

        // Add listeners to update preview when fields change
        leftOperandField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        operatorChoice.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        rightOperandField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        resultField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());

        // Initial preview update
        updatePreview();
    }

    /**
     * Handles the confirm button click.
     * Validates the input and sets up the secret equation if valid.
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleConfirm(ActionEvent event) {
        // Hide any previous error
        errorLabel.setVisible(false);

        // Validate inputs
        ValidationResult validation = validateInputs();
        if (!validation.isValid()) {
            displayError(validation.getMessage());
            return;
        }

        try {
            // Create secret equation
            SecretEquation equation = new SecretEquation(
                leftOperandField.getText(),
                operatorChoice.getValue(),
                rightOperandField.getText(),
                resultField.getText()
            );

            // Validate the equation
            if (!equation.isValid()) {
                displayError(AppConstants.ERROR_INVALID_MATH);
                return;
            }

            // Check if equation is trivial
            if (equation.isTrivial()) {
                displayError(AppConstants.ERROR_TRIVIAL_EQUATION);
                return;
            }

            // Set up the secret equation
            boolean success = authService.setupSecretEquation(equation);

            if (success) {
                // Transition to calculator
                SceneManager.getInstance().transitionToCalculator();
            } else {
                displayError("Failed to save secret equation. Please try again.");
            }

        } catch (Exception e) {
            displayError("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the preview label to show the current equation.
     */
    private void updatePreview() {
        String left = leftOperandField.getText().trim();
        String operator = operatorChoice.getValue();
        String right = rightOperandField.getText().trim();
        String result = resultField.getText().trim();

        if (left.isEmpty() && operator.isEmpty() && right.isEmpty() && result.isEmpty()) {
            previewLabel.setText("Your equation: ___");
        } else {
            String preview = (left.isEmpty() ? "_" : left) +
                           operator +
                           (right.isEmpty() ? "_" : right) +
                           "=" +
                           (result.isEmpty() ? "_" : result);
            previewLabel.setText("Your equation: " + preview);
        }
    }

    /**
     * Validates all input fields.
     *
     * @return a ValidationResult containing the validation status and message
     */
    private ValidationResult validateInputs() {
        // Check if all fields are filled
        if (leftOperandField.getText().trim().isEmpty() ||
            rightOperandField.getText().trim().isEmpty() ||
            resultField.getText().trim().isEmpty()) {
            return new ValidationResult(false, AppConstants.ERROR_EMPTY_FIELDS);
        }

        // Check if operator is selected
        if (operatorChoice.getValue() == null || operatorChoice.getValue().isEmpty()) {
            return new ValidationResult(false, "Please select an operator.");
        }

        // Check if inputs are valid numbers
        try {
            Double.parseDouble(leftOperandField.getText().trim());
            Double.parseDouble(rightOperandField.getText().trim());
            Double.parseDouble(resultField.getText().trim());
        } catch (NumberFormatException e) {
            return new ValidationResult(false, AppConstants.ERROR_INVALID_NUMBER);
        }

        return new ValidationResult(true, "");
    }

    /**
     * Displays an error message to the user.
     *
     * @param message the error message to display
     */
    private void displayError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Inner class to represent validation results.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
