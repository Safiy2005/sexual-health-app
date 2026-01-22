package com.sddp.sexualhealthapp.calculator.service;

import com.sddp.sexualhealthapp.calculator.model.Calculator;

/**
 * Service class for matching and normalizing equations.
 * Handles pattern matching logic for detecting secret equations in calculator usage.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class EquationMatcher {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EquationMatcher() {
        throw new AssertionError("Cannot instantiate EquationMatcher class");
    }

    /**
     * Normalizes an equation string by removing whitespace and standardizing operators.
     *
     * @param equation the equation to normalize
     * @return the normalized equation string
     */
    public static String normalizeEquation(String equation) {
        if (equation == null) {
            return "";
        }

        // Remove all whitespace
        String normalized = equation.replaceAll("\\s+", "");

        // Standardize operators
        normalized = normalized.replace('*', '×');
        normalized = normalized.replace('/', '÷');

        // Convert to lowercase for case-insensitive comparison
        return normalized.toLowerCase();
    }

    /**
     * Extracts the last completed equation from the calculator state.
     * This is typically the most recent equation in the history.
     *
     * @param calculator the calculator model
     * @return the last equation, or empty string if no equations exist
     */
    public static String extractLastEquation(Calculator calculator) {
        java.util.List<String> history = calculator.getEquationHistory();
        if (history.isEmpty()) {
            return "";
        }
        return history.get(history.size() - 1);
    }

    /**
     * Compares two equations to determine if they match.
     * Comparison is done after normalizing both equations.
     *
     * @param equation1 the first equation
     * @param equation2 the second equation
     * @return true if the equations match, false otherwise
     */
    public static boolean compareEquations(String equation1, String equation2) {
        String norm1 = normalizeEquation(equation1);
        String norm2 = normalizeEquation(equation2);
        return norm1.equals(norm2);
    }

    /**
     * Checks if an equation contains all required components (operands, operator, equals, result).
     *
     * @param equation the equation to check
     * @return true if the equation is complete, false otherwise
     */
    public static boolean isCompleteEquation(String equation) {
        if (equation == null || equation.trim().isEmpty()) {
            return false;
        }

        String normalized = normalizeEquation(equation);

        // Must contain an equals sign
        if (!normalized.contains("=")) {
            return false;
        }

        // Must contain at least one operator before the equals sign
        int equalsIndex = normalized.indexOf('=');
        String leftPart = normalized.substring(0, equalsIndex);

        return containsOperator(leftPart) && equalsIndex < normalized.length() - 1;
    }

    /**
     * Checks if a string contains a valid arithmetic operator.
     *
     * @param str the string to check
     * @return true if the string contains an operator, false otherwise
     */
    private static boolean containsOperator(String str) {
        for (int i = 1; i < str.length(); i++) { // Start at 1 to allow negative numbers
            char c = str.charAt(i);
            if (c == '+' || c == '-' || c == '×' || c == '÷') {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats an equation with proper spacing for display purposes.
     *
     * @param equation the equation to format
     * @return the formatted equation string
     */
    public static String formatEquationForDisplay(String equation) {
        if (equation == null || equation.trim().isEmpty()) {
            return "";
        }

        String normalized = normalizeEquation(equation);
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '+' || c == '×' || c == '÷' || c == '=') {
                formatted.append(' ').append(c).append(' ');
            } else if (c == '-' && i > 0 && Character.isDigit(normalized.charAt(i - 1))) {
                // This is a minus operator, not a negative sign
                formatted.append(' ').append(c).append(' ');
            } else {
                formatted.append(c);
            }
        }

        return formatted.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Checks if an equation is the reset code (999÷0 or 999/0).
     * This special code is used to reset the secret equation.
     *
     * @param equation the equation to check
     * @return true if the equation is the reset code, false otherwise
     */
    public static boolean isResetCode(String equation) {
        if (equation == null || equation.trim().isEmpty()) {
            return false;
        }

        // Normalize the equation (removes whitespace, standardizes operators)
        String normalized = normalizeEquation(equation);

        // Check for reset code patterns: 999÷0, with optional decimal (999÷0.0)
        return normalized.matches("999[÷]0(\\.0*)?");
    }

    /**
     * Validates that an equation string matches the expected pattern.
     * Pattern: number operator number = result
     *
     * @param equation the equation to validate
     * @return true if the pattern is valid, false otherwise
     */
    public static boolean matchesEquationPattern(String equation) {
        if (!isCompleteEquation(equation)) {
            return false;
        }

        String normalized = normalizeEquation(equation);

        // Split on equals sign
        String[] parts = normalized.split("=");
        if (parts.length != 2) {
            return false;
        }

        // Check if left side has an operator
        String leftSide = parts[0];
        String rightSide = parts[1];

        // Right side should be a number
        if (!isNumeric(rightSide)) {
            return false;
        }

        // Left side should contain one operator
        return containsOperator(leftSide);
    }

    /**
     * Checks if a string represents a valid number (including decimals and negative numbers).
     *
     * @param str the string to check
     * @return true if the string is numeric, false otherwise
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
