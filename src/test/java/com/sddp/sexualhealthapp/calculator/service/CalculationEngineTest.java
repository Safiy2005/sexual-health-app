package com.sddp.sexualhealthapp.calculator.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CalculationEngine class.
 * Tests all arithmetic operations and edge cases.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
class CalculationEngineTest {

    private static final double DELTA = 0.0001;

    @Test
    void testAdd_PositiveNumbers() {
        assertEquals(8.0, CalculationEngine.add(5, 3), DELTA);
        assertEquals(100.0, CalculationEngine.add(75, 25), DELTA);
    }

    @Test
    void testAdd_NegativeNumbers() {
        assertEquals(-8.0, CalculationEngine.add(-5, -3), DELTA);
        assertEquals(2.0, CalculationEngine.add(5, -3), DELTA);
        assertEquals(-2.0, CalculationEngine.add(-5, 3), DELTA);
    }

    @Test
    void testAdd_WithDecimals() {
        assertEquals(8.5, CalculationEngine.add(5.2, 3.3), DELTA);
        assertEquals(0.3, CalculationEngine.add(0.1, 0.2), DELTA);
    }

    @Test
    void testAdd_Zero() {
        assertEquals(5.0, CalculationEngine.add(5, 0), DELTA);
        assertEquals(0.0, CalculationEngine.add(0, 0), DELTA);
    }

    @Test
    void testSubtract_PositiveNumbers() {
        assertEquals(2.0, CalculationEngine.subtract(5, 3), DELTA);
        assertEquals(50.0, CalculationEngine.subtract(75, 25), DELTA);
    }

    @Test
    void testSubtract_NegativeNumbers() {
        assertEquals(-2.0, CalculationEngine.subtract(-5, -3), DELTA);
        assertEquals(8.0, CalculationEngine.subtract(5, -3), DELTA);
        assertEquals(-8.0, CalculationEngine.subtract(-5, 3), DELTA);
    }

    @Test
    void testSubtract_WithDecimals() {
        assertEquals(1.9, CalculationEngine.subtract(5.2, 3.3), DELTA);
        assertEquals(-0.1, CalculationEngine.subtract(0.1, 0.2), DELTA);
    }

    @Test
    void testSubtract_Zero() {
        assertEquals(5.0, CalculationEngine.subtract(5, 0), DELTA);
        assertEquals(0.0, CalculationEngine.subtract(0, 0), DELTA);
    }

    @Test
    void testMultiply_PositiveNumbers() {
        assertEquals(15.0, CalculationEngine.multiply(5, 3), DELTA);
        assertEquals(1875.0, CalculationEngine.multiply(75, 25), DELTA);
    }

    @Test
    void testMultiply_NegativeNumbers() {
        assertEquals(15.0, CalculationEngine.multiply(-5, -3), DELTA);
        assertEquals(-15.0, CalculationEngine.multiply(5, -3), DELTA);
        assertEquals(-15.0, CalculationEngine.multiply(-5, 3), DELTA);
    }

    @Test
    void testMultiply_WithDecimals() {
        assertEquals(17.16, CalculationEngine.multiply(5.2, 3.3), DELTA);
        assertEquals(0.02, CalculationEngine.multiply(0.1, 0.2), DELTA);
    }

    @Test
    void testMultiply_Zero() {
        assertEquals(0.0, CalculationEngine.multiply(5, 0), DELTA);
        assertEquals(0.0, CalculationEngine.multiply(0, 0), DELTA);
    }

    @Test
    void testDivide_PositiveNumbers() {
        assertEquals(2.0, CalculationEngine.divide(6, 3), DELTA);
        assertEquals(3.0, CalculationEngine.divide(75, 25), DELTA);
    }

    @Test
    void testDivide_NegativeNumbers() {
        assertEquals(2.0, CalculationEngine.divide(-6, -3), DELTA);
        assertEquals(-2.0, CalculationEngine.divide(6, -3), DELTA);
        assertEquals(-2.0, CalculationEngine.divide(-6, 3), DELTA);
    }

    @Test
    void testDivide_WithDecimals() {
        assertEquals(1.5758, CalculationEngine.divide(5.2, 3.3), DELTA);
        assertEquals(0.5, CalculationEngine.divide(0.1, 0.2), DELTA);
    }

    @Test
    void testDivide_ResultingInDecimal() {
        assertEquals(1.6667, CalculationEngine.divide(5, 3), DELTA);
        assertEquals(0.3333, CalculationEngine.divide(1, 3), DELTA);
    }

    @Test
    void testDivide_ByZero() {
        assertEquals(Double.POSITIVE_INFINITY, CalculationEngine.divide(5, 0));
        assertEquals(Double.NEGATIVE_INFINITY, CalculationEngine.divide(-5, 0));
        assertTrue(Double.isNaN(CalculationEngine.divide(0, 0)));
    }
}
