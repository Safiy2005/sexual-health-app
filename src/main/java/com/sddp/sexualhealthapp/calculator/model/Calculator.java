package com.sddp.sexualhealthapp.calculator.model;

import com.sddp.sexualhealthapp.util.AppConstants;

import java.util.ArrayList;
import java.util.List;

public class Calculator {

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

    public Calculator() {
        this.currentDisplay = AppConstants.CALC_INITIAL_DISPLAY;
        this.previousValue = 0.0;
        this.currentOperation = Operation.NONE;
        this.isNewNumber = true;
        this.equationHistory = new ArrayList<>();
        this.leftOperand = "";
        this.operator = "";
    }

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

    public void appendDecimal() {
        if (isNewNumber) {
            currentDisplay = "0.";
            isNewNumber = false;
        } else if (!currentDisplay.contains(".")) {
            currentDisplay += ".";
        }
    }

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

    public void clear() {
        currentDisplay = AppConstants.CALC_INITIAL_DISPLAY;
        previousValue = 0.0;
        currentOperation = Operation.NONE;
        isNewNumber = true;
        leftOperand = "";
        operator = "";
    }

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

    public String getCurrentEquation() {
        if (currentOperation == Operation.NONE || leftOperand.isEmpty()) {
            return "";
        }
        return leftOperand + operator + currentDisplay;
    }

    public List<String> getEquationHistory() {
        return new ArrayList<>(equationHistory);
    }

    public String getCurrentDisplay() {
        return currentDisplay;
    }

    /**
     * Gets the full display string showing the complete equation being built.
     * Shows "7+6" format when building an equation, or just the current number otherwise.
     *
     * @return the full display string
     */
    public String getFullDisplay() {
        if (currentOperation == Operation.NONE || leftOperand.isEmpty()) {
            // No operation set, just show current display
            return currentDisplay;
        }

        if (isNewNumber) {
            // Operation just pressed, show "7+"
            return leftOperand + operator;
        } else {
            // Entering second number, show "7+6"
            return leftOperand + operator + currentDisplay;
        }
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

    public Operation getCurrentOperation() {
        return currentOperation;
    }

    private double parseDisplayValue() {
        try {
            return Double.parseDouble(currentDisplay);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

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

    private String buildEquationString(double rightValue, double result) {
        String left = formatResult(previousValue);
        String right = formatResult(rightValue);
        String res = formatResult(result);
        return left + operator + right + "=" + res;
    }

    private void addToHistory(String equation) {
        equationHistory.add(equation);
        if (equationHistory.size() > AppConstants.MAX_EQUATION_HISTORY) {
            equationHistory.remove(0);
        }
    }

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
