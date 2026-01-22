package com.sddp.sexualhealthapp.calculator.model;

import com.sddp.sexualhealthapp.util.AppConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing the state and operations of the calculator.
 * This class maintains the current display, operation state, and equation history
 * for secret equation matching.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class Calculator {

    /**
     * Enum representing the possible operations the calculator can perform.
     */
    public enum Operation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, NONE
    }

    private String currentDisplay;
    private double previousValue;
    private Operation currentOperation;
    private boolean isNewNumber;
    private List<String> equationHistory;
    private String leftOperand;
    private String operator;

    /**
     * Constructs a new Calculator with default values.
     */
    public Calculator() {
        this.currentDisplay = AppConstants.CALC_INITIAL_DISPLAY;
        this.previousValue = 0.0;
        this.currentOperation = Operation.NONE;
        this.isNewNumber = true;
        this.equationHistory = new ArrayList<>();
        this.leftOperand = "";
        this.operator = "";
    }

    /**
     * Appends a digit to the current display.
     *
     * @param digit the digit to append (0-9)
     */
    public void appendDigit(String digit) {
        if (isNewNumber) {
            currentDisplay = digit;
            isNewNumber = false;
        } else {
            if (currentDisplay.length() < AppConstants.MAX_DISPLAY_DIGITS) {
                if (currentDisplay.equals(AppConstants.CALC_INITIAL_DISPLAY)) {
                    currentDisplay = digit;
                } else {
                    currentDisplay += digit;
                }
            }
        }
    }

    /**
     * Appends a decimal point to the current display.
     */
    public void appendDecimal() {
        if (isNewNumber) {
            currentDisplay = "0.";
            isNewNumber = false;
        } else if (!currentDisplay.contains(".")) {
            currentDisplay += ".";
        }
    }

    /**
     * Sets the current operation and stores the previous value.
     *
     * @param operation the operation to set
     */
    public void setOperation(Operation operation) {
        if (currentOperation != Operation.NONE && !isNewNumber) {
            // Chain operations: calculate current result first
            calculateResult();
        }

        previousValue = parseDisplayValue();
        leftOperand = currentDisplay;
        operator = getOperatorSymbol(operation);
        currentOperation = operation;
        isNewNumber = true;
    }

    /**
     * Calculates the result of the current operation and updates the display.
     *
     * @return the calculated result
     */
    public double calculateResult() {
        if (currentOperation == Operation.NONE) {
            return parseDisplayValue();
        }

        double currentValue = parseDisplayValue();
        double result;

        switch (currentOperation) {
            case ADD:
                result = previousValue + currentValue;
                break;
            case SUBTRACT:
                result = previousValue - currentValue;
                break;
            case MULTIPLY:
                result = previousValue * currentValue;
                break;
            case DIVIDE:
                if (currentValue == 0.0) {
                    currentDisplay = AppConstants.CALC_ERROR_DIV_ZERO;
                    currentOperation = Operation.NONE;
                    isNewNumber = true;
                    return Double.POSITIVE_INFINITY;
                }
                result = previousValue / currentValue;
                break;
            default:
                result = currentValue;
                break;
        }

        // Store equation in history for secret matching
        String equation = buildEquationString(currentValue, result);
        addToHistory(equation);

        currentDisplay = formatResult(result);
        currentOperation = Operation.NONE;
        isNewNumber = true;
        previousValue = 0.0;
        leftOperand = "";
        operator = "";

        return result;
    }

    /**
     * Clears the calculator state completely.
     */
    public void clear() {
        currentDisplay = AppConstants.CALC_INITIAL_DISPLAY;
        previousValue = 0.0;
        currentOperation = Operation.NONE;
        isNewNumber = true;
        leftOperand = "";
        operator = "";
    }

    /**
     * Removes the last digit from the current display.
     */
    public void backspace() {
        if (!isNewNumber && currentDisplay.length() > 0) {
            if (currentDisplay.length() == 1 ||
                (currentDisplay.length() == 2 && currentDisplay.startsWith("-"))) {
                currentDisplay = AppConstants.CALC_INITIAL_DISPLAY;
                isNewNumber = true;
            } else {
                currentDisplay = currentDisplay.substring(0, currentDisplay.length() - 1);
            }
        }
    }

    /**
     * Gets the current equation being built (e.g., "5+3").
     *
     * @return the current equation string, or empty string if no operation is set
     */
    public String getCurrentEquation() {
        if (currentOperation == Operation.NONE || leftOperand.isEmpty()) {
            return "";
        }
        return leftOperand + operator + currentDisplay;
    }

    /**
     * Gets the equation history for secret matching.
     *
     * @return list of recent equations
     */
    public List<String> getEquationHistory() {
        return new ArrayList<>(equationHistory);
    }

    /**
     * Gets the current display value.
     *
     * @return the current display string
     */
    public String getCurrentDisplay() {
        return currentDisplay;
    }

    /**
     * Sets the current display value (for testing purposes).
     *
     * @param display the display string to set
     */
    public void setCurrentDisplay(String display) {
        this.currentDisplay = display;
        this.isNewNumber = false;
    }

    /**
     * Checks if the next input should start a new number.
     *
     * @return true if the next input starts a new number
     */
    public boolean isNewNumber() {
        return isNewNumber;
    }

    /**
     * Gets the current operation.
     *
     * @return the current operation
     */
    public Operation getCurrentOperation() {
        return currentOperation;
    }

    /**
     * Parses the current display value as a double.
     *
     * @return the parsed double value, or 0.0 if parsing fails
     */
    private double parseDisplayValue() {
        try {
            return Double.parseDouble(currentDisplay);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Formats a result for display, removing unnecessary decimal points and trailing zeros.
     *
     * @param result the result to format
     * @return the formatted string
     */
    private String formatResult(double result) {
        // Check if result is an integer
        if (result == (long) result) {
            return String.format("%d", (long) result);
        } else {
            // Format with up to 10 decimal places, removing trailing zeros
            String formatted = String.format("%.10f", result);
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");

            // Truncate if too long
            if (formatted.length() > AppConstants.MAX_DISPLAY_DIGITS) {
                formatted = formatted.substring(0, AppConstants.MAX_DISPLAY_DIGITS);
            }

            return formatted;
        }
    }

    /**
     * Builds an equation string from the current operation.
     *
     * @param rightValue the right operand value
     * @param result the result value
     * @return the equation string (e.g., "5+3=8")
     */
    private String buildEquationString(double rightValue, double result) {
        String left = formatResult(previousValue);
        String right = formatResult(rightValue);
        String res = formatResult(result);
        return left + operator + right + "=" + res;
    }

    /**
     * Adds an equation to the history, maintaining the maximum size.
     *
     * @param equation the equation to add
     */
    private void addToHistory(String equation) {
        equationHistory.add(equation);
        if (equationHistory.size() > AppConstants.MAX_EQUATION_HISTORY) {
            equationHistory.remove(0);
        }
    }

    /**
     * Gets the operator symbol for an operation.
     *
     * @param operation the operation
     * @return the operator symbol
     */
    private String getOperatorSymbol(Operation operation) {
        switch (operation) {
            case ADD:
                return "+";
            case SUBTRACT:
                return "-";
            case MULTIPLY:
                return "×";
            case DIVIDE:
                return "÷";
            default:
                return "";
        }
    }
}
