package com.sddp.sexualhealthapp.calculator.service;

import com.sddp.sexualhealthapp.calculator.model.Calculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquationMatcherTest {

    @Test
    void testNormalizeEquation_RemovesWhitespace() {
        assertEquals("5+3=8", EquationMatcher.normalizeEquation("5 + 3 = 8"));
        assertEquals("5+3=8", EquationMatcher.normalizeEquation("  5+3=8  "));
    }

    @Test
    void testNormalizeEquation_StandardizesOperators() {
        assertEquals("5×3=15", EquationMatcher.normalizeEquation("5*3=15"));
        assertEquals("10÷2=5", EquationMatcher.normalizeEquation("10/2=5"));
    }

    @Test
    void testNormalizeEquation_LowerCase() {
        // Although equations are numbers, test the lowercase conversion
        assertEquals("abc", EquationMatcher.normalizeEquation("ABC"));
    }

    @Test
    void testNormalizeEquation_Null() {
        assertEquals("", EquationMatcher.normalizeEquation(null));
    }

    @Test
    void testNormalizeEquation_CombinedTransformations() {
        String input = "  10 * 5 = 50  ";
        String expected = "10×5=50";

        assertEquals(expected, EquationMatcher.normalizeEquation(input));
    }

    @Test
    void testExtractLastEquation_WithHistory() {
        Calculator calculator = new Calculator();

        // Perform a calculation to add to history
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");
        calculator.calculateResult();

        String lastEquation = EquationMatcher.extractLastEquation(calculator);

        assertEquals("5+3=8", lastEquation);
    }

    @Test
    void testExtractLastEquation_EmptyHistory() {
        Calculator calculator = new Calculator();

        String lastEquation = EquationMatcher.extractLastEquation(calculator);

        assertEquals("", lastEquation);
    }

    @Test
    void testExtractLastEquation_MultipleEquations() {
        Calculator calculator = new Calculator();

        // First equation
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");
        calculator.calculateResult();

        // Second equation
        calculator.appendDigit("10");
        calculator.setOperation(Calculator.Operation.SUBTRACT);
        calculator.appendDigit("2");
        calculator.calculateResult();

        String lastEquation = EquationMatcher.extractLastEquation(calculator);

        assertEquals("10-2=8", lastEquation);
    }

    @Test
    void testIsCompleteEquation_Valid() {
        assertTrue(EquationMatcher.isCompleteEquation("5+3=8"));
        assertTrue(EquationMatcher.isCompleteEquation("10-2=8"));
        assertTrue(EquationMatcher.isCompleteEquation("5×3=15"));
        assertTrue(EquationMatcher.isCompleteEquation("10÷2=5"));
    }

    @Test
    void testIsCompleteEquation_WithSpaces() {
        assertTrue(EquationMatcher.isCompleteEquation("5 + 3 = 8"));
    }

    @Test
    void testIsCompleteEquation_NoEquals() {
        assertFalse(EquationMatcher.isCompleteEquation("5+3"));
    }

    @Test
    void testIsCompleteEquation_NoOperator() {
        assertFalse(EquationMatcher.isCompleteEquation("53=8"));
    }

    @Test
    void testIsCompleteEquation_NoResult() {
        assertFalse(EquationMatcher.isCompleteEquation("5+3="));
    }

    @Test
    void testIsCompleteEquation_Null() {
        assertFalse(EquationMatcher.isCompleteEquation(null));
    }

    @Test
    void testIsCompleteEquation_Empty() {
        assertFalse(EquationMatcher.isCompleteEquation(""));
        assertFalse(EquationMatcher.isCompleteEquation("   "));
    }

    @Test
    void testIsResetCode_Valid() {
        assertTrue(EquationMatcher.isResetCode("999÷0"));
        assertTrue(EquationMatcher.isResetCode("999÷0.0"));
        assertTrue(EquationMatcher.isResetCode("999÷0.00"));
    }

    @Test
    void testIsResetCode_WithSpaces() {
        assertTrue(EquationMatcher.isResetCode("999 ÷ 0"));
        assertTrue(EquationMatcher.isResetCode("  999÷0  "));
    }

    @Test
    void testIsResetCode_WithSlash() {
        // Should normalize / to ÷
        assertTrue(EquationMatcher.isResetCode("999/0"));
    }

    @Test
    void testIsResetCode_Invalid() {
        assertFalse(EquationMatcher.isResetCode("999÷1"));
        assertFalse(EquationMatcher.isResetCode("998÷0"));
        assertFalse(EquationMatcher.isResetCode("999+0"));
        assertFalse(EquationMatcher.isResetCode("99÷0"));
    }

    @Test
    void testIsResetCode_Null() {
        assertFalse(EquationMatcher.isResetCode(null));
    }

    @Test
    void testIsResetCode_Empty() {
        assertFalse(EquationMatcher.isResetCode(""));
        assertFalse(EquationMatcher.isResetCode("   "));
    }

    @Test
    void testIsResetCode_WithDecimalResult() {
        // 999÷0.5 should NOT be a reset code
        assertFalse(EquationMatcher.isResetCode("999÷0.5"));
    }

    @Test
    void testNormalizeEquation_PreservesNegativeNumbers() {
        assertEquals("-5+3=-2", EquationMatcher.normalizeEquation("-5+3=-2"));
        assertEquals("-10×2=-20", EquationMatcher.normalizeEquation("-10*2=-20"));
    }

    @Test
    void testIsCompleteEquation_NegativeNumbers() {
        assertTrue(EquationMatcher.isCompleteEquation("-5+3=-2"));
        assertTrue(EquationMatcher.isCompleteEquation("10+-5=5"));
    }

    @Test
    void testExtractLastEquation_WithMultiplication() {
        Calculator calculator = new Calculator();

        calculator.appendDigit("7");
        calculator.setOperation(Calculator.Operation.MULTIPLY);
        calculator.appendDigit("6");
        calculator.calculateResult();

        String lastEquation = EquationMatcher.extractLastEquation(calculator);

        assertEquals("7×6=42", lastEquation);
    }

    @Test
    void testExtractLastEquation_WithDivision() {
        Calculator calculator = new Calculator();

        calculator.appendDigit("20");
        calculator.setOperation(Calculator.Operation.DIVIDE);
        calculator.appendDigit("4");
        calculator.calculateResult();

        String lastEquation = EquationMatcher.extractLastEquation(calculator);

        assertEquals("20÷4=5", lastEquation);
    }

    @Test
    void testIsCompleteEquation_ComplexOperands() {
        assertTrue(EquationMatcher.isCompleteEquation("123.45+67.89=191.34"));
        assertTrue(EquationMatcher.isCompleteEquation("1000÷25=40"));
    }

    @Test
    void testNormalizeEquation_MultipleSpaces() {
        assertEquals("5+3=8", EquationMatcher.normalizeEquation("5    +    3    =    8"));
    }

    @Test
    void testIsResetCode_WithEqualsSign() {
        // Reset code should work with or without the result part
        assertFalse(EquationMatcher.isResetCode("999÷0=error")); // Has result, not just the code
    }

    @Test
    void testNormalizeEquation_MixedOperators() {
        String input = "10 * 5 / 2 = 25"; // Not a valid simple equation, but test normalization
        String result = EquationMatcher.normalizeEquation(input);

        assertTrue(result.contains("×"));
        assertTrue(result.contains("÷"));
        assertFalse(result.contains("*"));
        assertFalse(result.contains("/"));
    }
}
