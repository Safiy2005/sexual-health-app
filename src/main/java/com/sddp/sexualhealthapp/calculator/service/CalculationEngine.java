package com.sddp.sexualhealthapp.calculator.service;

/**
 * Service class providing pure computational logic for calculator operations.
 * This class is stateless and all methods are static, making it easy to test
 * and use throughout the application.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class CalculationEngine {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CalculationEngine() {
        throw new AssertionError("Cannot instantiate CalculationEngine class");
    }

    public static double add(double a, double b) {
        return a + b;
    }

    public static double subtract(double a, double b) {
        return a - b;
    }

    public static double multiply(double a, double b) {
        return a * b;
    }

    /**
     * Divides the first number by the second.
     *
     * @param a the dividend
     * @param b the divisor
     * @return the quotient (a / b), returns POSITIVE_INFINITY or NEGATIVE_INFINITY if b is zero,
     *         or NaN if both a and b are zero
     */
    public static double divide(double a, double b) {
        // Let Java handle the division by zero naturally
        // It will return POSITIVE_INFINITY, NEGATIVE_INFINITY, or NaN appropriately
        return a / b;
    }

    public static double evaluateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be null or empty");
        }

        String trimmed = expression.trim().replaceAll("\\s+", "");

        // Find the operator (search from position 1 to allow for negative numbers)
        int operatorIndex = -1;
        char operator = ' ';

        for (int i = 1; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '×' || c == '/' || c == '÷') {
                operatorIndex = i;
                operator = c;
                break;
            }
        }

        if (operatorIndex == -1) {
            throw new IllegalArgumentException("No valid operator found in expression: " + expression);
        }

        try {
            String leftPart = trimmed.substring(0, operatorIndex);
            String rightPart = trimmed.substring(operatorIndex + 1);

            double left = Double.parseDouble(leftPart);
            double right = Double.parseDouble(rightPart);

            switch (operator) {
                case '+':
                    return add(left, right);
                case '-':
                    return subtract(left, right);
                case '*':
                case '×':
                    return multiply(left, right);
                case '/':
                case '÷':
                    return divide(left, right);
                default:
                    throw new IllegalArgumentException("Invalid operator: " + operator);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numbers in expression: " + expression, e);
        }
    }

    public static boolean isValidExpression(String expression) {
        try {
            evaluateExpression(expression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static double percentage(double number, double percentage) {
        return (number * percentage) / 100.0;
    }

    public static double square(double number) {
        return number * number;
    }

    public static double squareRoot(double number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot calculate square root of negative number");
        }
        return Math.sqrt(number);
    }
}
