package com.sddp.sexualhealthapp.calculator.model;

import java.time.LocalDateTime;
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

    public SecretEquation(String leftOperand, String operator, String rightOperand, String result) {
        this.leftOperand = leftOperand.trim();
        this.operator = operator.trim();
        this.rightOperand = rightOperand.trim();
        this.result = result.trim();
        this.fullEquation = this.leftOperand + this.operator + this.rightOperand + "=" + this.result;
        this.createdDate = LocalDateTime.now();
    }

    public SecretEquation(String fullEquation) {
        this.fullEquation = fullEquation.replaceAll("\\s+", "");
        this.createdDate = LocalDateTime.now();

        String[] parts = parseEquation(this.fullEquation);
        this.leftOperand = parts[0];
        this.operator = parts[1];
        this.rightOperand = parts[2];
        this.result = parts[3];
    }

    public boolean matches(String inputEquation) {
        if (inputEquation == null || inputEquation.trim().isEmpty()) {
            return false;
        }

        String normalized = inputEquation.replaceAll("\\s+", "");
        return this.fullEquation.equalsIgnoreCase(normalized);
    }

    public boolean isValid() {
        if (leftOperand.isEmpty() || operator.isEmpty() ||
            rightOperand.isEmpty() || result.isEmpty()) {
            return false;
        }

        try {
            double left = Double.parseDouble(leftOperand);
            double right = Double.parseDouble(rightOperand);
            double expectedResult = Double.parseDouble(result);

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

            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getFullEquation() {
        return fullEquation;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public String getOperator() {
        return operator;
    }

    public String getRightOperand() {
        return rightOperand;
    }

    public String getResult() {
        return result;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    private String[] parseEquation(String equation) {
        int equalsIndex = equation.indexOf('=');
        if (equalsIndex == -1) {
            throw new IllegalArgumentException("Invalid equation format: missing '='");
        }

        String leftPart = equation.substring(0, equalsIndex);
        String result = equation.substring(equalsIndex + 1);

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
