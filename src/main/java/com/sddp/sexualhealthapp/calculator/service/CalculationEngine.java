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

    /**
     * Adds two numbers.
     *
     * @param a the first number
     * @param b the second number
     * @return the sum of a and b
     */
    public static double add(double a, double b) {
        return a + b;
    }

    /**
     * Subtracts the second number from the first.
     *
     * @param a the first number
     * @param b the second number
     * @return the difference (a - b)
     */
    public static double subtract(double a, double b) {
        return a - b;
    }

    /**
     * Multiplies two numbers.
     *
     * @param a the first number
     * @param b the second number
     * @return the product of a and b
     */
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

    /**
     * Evaluates a simple arithmetic expression.
     * Supported format: "number operator number" (e.g., "5+3", "10-2")
     *
     * @param expression the expression to evaluate
     * @return the result of the expression
     * @throws IllegalArgumentException if the expression is invalid
     */
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

    /**
     * Checks if an expression is valid (can be evaluated).
     *
     * @param expression the expression to validate
     * @return true if the expression is valid, false otherwise
     */
    public static boolean isValidExpression(String expression) {
        try {
            evaluateExpression(expression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Calculates the percentage of a number.
     *
     * @param number the number
     * @param percentage the percentage (e.g., 50 for 50%)
     * @return the percentage value of the number
     */
    public static double percentage(double number, double percentage) {
        return (number * percentage) / 100.0;
    }

    /**
     * Calculates the square of a number.
     *
     * @param number the number to square
     * @return the square of the number
     */
    public static double square(double number) {
        return number * number;
    }

    /**
     * Calculates the square root of a number.
     *
     * @param number the number
     * @return the square root of the number
     * @throws IllegalArgumentException if the number is negative
     */
    public static double squareRoot(double number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot calculate square root of negative number");
        }
        return Math.sqrt(number);
    }
}
