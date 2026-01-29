package com.sddp.sexualhealthapp.calculator.service;

import com.sddp.sexualhealthapp.calculator.model.Calculator;

public class EquationMatcher {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EquationMatcher() {
        throw new AssertionError("Cannot instantiate EquationMatcher class");
    }

    public static String normalizeEquation(String equation) {
        if (equation == null) {
            return "";
        }

        String normalized = equation.replaceAll("\\s+", "");
        normalized = normalized.replace('*', '×');
        normalized = normalized.replace('/', '÷');

        // Convert to lowercase for case-insensitive comparison
        return normalized.toLowerCase();
    }

    public static String extractLastEquation(Calculator calculator) {
        java.util.List<String> history = calculator.getEquationHistory();
        if (history.isEmpty()) {
            return "";
        }
        return history.get(history.size() - 1);
    }

    public static boolean isCompleteEquation(String equation) {
        if (equation == null || equation.trim().isEmpty()) {
            return false;
        }

        String normalized = normalizeEquation(equation);

        if (!normalized.contains("=")) {
            return false;
        }

        int equalsIndex = normalized.indexOf('=');
        String leftPart = normalized.substring(0, equalsIndex);

        return containsOperator(leftPart) && equalsIndex < normalized.length() - 1;
    }

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

        String normalized = normalizeEquation(equation);

        // Check for reset code patterns: 999÷0, with optional decimal (999÷0.0)
        return normalized.matches("999[÷]0(\\.0*)?");
    }
}
