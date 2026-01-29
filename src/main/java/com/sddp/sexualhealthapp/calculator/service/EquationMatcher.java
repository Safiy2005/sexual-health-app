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

    public static boolean compareEquations(String equation1, String equation2) {
        String norm1 = normalizeEquation(equation1);
        String norm2 = normalizeEquation(equation2);
        return norm1.equals(norm2);
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

        String normalized = normalizeEquation(equation);

        // Check for reset code patterns: 999÷0, with optional decimal (999÷0.0)
        return normalized.matches("999[÷]0(\\.0*)?");
    }

    public static boolean matchesEquationPattern(String equation) {
        if (!isCompleteEquation(equation)) {
            return false;
        }

        String normalized = normalizeEquation(equation);

        String[] parts = normalized.split("=");
        if (parts.length != 2) {
            return false;
        }

        String leftSide = parts[0];
        String rightSide = parts[1];

        if (!isNumeric(rightSide)) {
            return false;
        }

        return containsOperator(leftSide);
    }

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
