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

}
