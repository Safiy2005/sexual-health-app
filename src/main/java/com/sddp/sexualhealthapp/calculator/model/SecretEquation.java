package com.sddp.sexualhealthapp.calculator.model;

import com.sddp.sexualhealthapp.util.AppConstants;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * Model class representing a secret equation used for authentication.
 * A secret equation consists of left operand, operator, right operand, and result.
 * Example: "5" "+" "3" "=" "8" represents the equation "5+3=8"
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SecretEquation {

    private final String leftOperand;
    private final String operator;
    private final String rightOperand;
    private final String result;
    private final String fullEquation;
    private final LocalDateTime createdDate;

    /**
     * Constructs a new SecretEquation.
     *
     * @param leftOperand the left operand (e.g., "5")
     * @param operator the operator ("+", "-", "×", "÷")
     * @param rightOperand the right operand (e.g., "3")
     * @param result the expected result (e.g., "8")
     */
    public SecretEquation(String leftOperand, String operator, String rightOperand, String result) {
        this.leftOperand = leftOperand.trim();
        this.operator = operator.trim();
        this.rightOperand = rightOperand.trim();
        this.result = result.trim();
        this.fullEquation = this.leftOperand + this.operator + this.rightOperand + "=" + this.result;
        this.createdDate = LocalDateTime.now();
    }

    /**
     * Constructs a SecretEquation from a full equation string.
     *
     * @param fullEquation the full equation string (e.g., "5+3=8")
     * @throws IllegalArgumentException if the equation format is invalid
     */
    public SecretEquation(String fullEquation) {
        this.fullEquation = fullEquation.replaceAll("\\s+", ""); // Remove all whitespace
        this.createdDate = LocalDateTime.now();

        // Parse the equation
        String[] parts = parseEquation(this.fullEquation);
        this.leftOperand = parts[0];
        this.operator = parts[1];
        this.rightOperand = parts[2];
        this.result = parts[3];
    }

    /**
     * Checks if this secret equation matches the given input equation.
     * The comparison is case-insensitive and whitespace-agnostic.
     *
     * @param inputEquation the input equation to check
     * @return true if the equations match, false otherwise
     */
    public boolean matches(String inputEquation) {
        if (inputEquation == null || inputEquation.trim().isEmpty()) {
            return false;
        }

        String normalized = inputEquation.replaceAll("\\s+", "");
        return this.fullEquation.equalsIgnoreCase(normalized);
    }

    /**
     * Validates whether this secret equation is mathematically correct and secure.
     *
     * @return true if the equation is valid and secure, false otherwise
     */
    public boolean isValid() {
        // Check if all fields are non-empty
        if (leftOperand.isEmpty() || operator.isEmpty() ||
            rightOperand.isEmpty() || result.isEmpty()) {
            return false;
        }

        // Check if operands and result are valid numbers
        try {
            double left = Double.parseDouble(leftOperand);
            double right = Double.parseDouble(rightOperand);
            double expectedResult = Double.parseDouble(result);

            // Calculate the actual result
            double actualResult;
            switch (operator) {
                case "+":
                    actualResult = left + right;
                    break;
                case "-":
                    actualResult = left - right;
                    break;
                case "×":
                case "*":
                    actualResult = left * right;
                    break;
                case "÷":
                case "/":
                    if (right == 0.0) {
                        return false; // Division by zero
                    }
                    actualResult = left / right;
                    break;
                default:
                    return false; // Invalid operator
            }

            // Check if the result matches (with small tolerance for floating point comparison)
            if (Math.abs(actualResult - expectedResult) > 0.0001) {
                return false;
            }

            // Check if equation is trivial (too easy to guess)
            return !isTrivial();

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if this equation is considered trivial (too easy to guess).
     *
     * @return true if the equation is trivial, false otherwise
     */
    public boolean isTrivial() {
        String normalized = fullEquation.toLowerCase();
        return Arrays.stream(AppConstants.TRIVIAL_EQUATIONS)
                .anyMatch(trivial -> trivial.equalsIgnoreCase(normalized));
    }

    /**
     * Gets the full equation string.
     *
     * @return the full equation (e.g., "5+3=8")
     */
    public String getFullEquation() {
        return fullEquation;
    }

    /**
     * Gets the left operand.
     *
     * @return the left operand
     */
    public String getLeftOperand() {
        return leftOperand;
    }

    /**
     * Gets the operator.
     *
     * @return the operator
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Gets the right operand.
     *
     * @return the right operand
     */
    public String getRightOperand() {
        return rightOperand;
    }

    /**
     * Gets the result.
     *
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * Gets the creation date of this equation.
     *
     * @return the creation date
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Parses an equation string into its components.
     *
     * @param equation the equation string to parse
     * @return array of [leftOperand, operator, rightOperand, result]
     * @throws IllegalArgumentException if the equation format is invalid
     */
    private String[] parseEquation(String equation) {
        // Find the equals sign
        int equalsIndex = equation.indexOf('=');
        if (equalsIndex == -1) {
            throw new IllegalArgumentException("Invalid equation format: missing '='");
        }

        String leftPart = equation.substring(0, equalsIndex);
        String result = equation.substring(equalsIndex + 1);

        // Find the operator in the left part
        int operatorIndex = -1;
        String operator = "";

        // Check for operators (search from position 1 to allow for negative numbers)
        for (int i = 1; i < leftPart.length(); i++) {
            char c = leftPart.charAt(i);
            if (c == '+' || c == '-' || c == '×' || c == '*' || c == '÷' || c == '/') {
                operatorIndex = i;
                operator = String.valueOf(c);
                break;
            }
        }

        if (operatorIndex == -1) {
            throw new IllegalArgumentException("Invalid equation format: missing operator");
        }

        String leftOperand = leftPart.substring(0, operatorIndex);
        String rightOperand = leftPart.substring(operatorIndex + 1);

        return new String[]{leftOperand, operator, rightOperand, result};
    }

    @Override
    public String toString() {
        return fullEquation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecretEquation that = (SecretEquation) o;
        return fullEquation.equals(that.fullEquation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullEquation);
    }
}
