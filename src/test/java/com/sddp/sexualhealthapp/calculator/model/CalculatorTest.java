package com.sddp.sexualhealthapp.calculator.model;

import com.sddp.sexualhealthapp.util.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    private static final double DELTA = 0.0001;
    private Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }

    @Test
    void testInitialState() {
        assertEquals(AppConstants.CALC_INITIAL_DISPLAY, calculator.getCurrentDisplay());
        assertEquals(Calculator.Operation.NONE, calculator.getCurrentOperation());
        assertTrue(calculator.isNewNumber());
        assertTrue(calculator.getEquationHistory().isEmpty());
    }

    @Test
    void testAppendDigit_SingleDigit() {
        calculator.appendDigit("5");
        assertEquals("5", calculator.getCurrentDisplay());
        assertFalse(calculator.isNewNumber());
    }

    @Test
    void testAppendDigit_MultipleDigits() {
        calculator.appendDigit("5");
        calculator.appendDigit("3");
        calculator.appendDigit("7");
        assertEquals("537", calculator.getCurrentDisplay());
    }

    @Test
    void testAppendDigit_ReplacesZero() {
        assertEquals("0", calculator.getCurrentDisplay());
        calculator.appendDigit("5");
        assertEquals("5", calculator.getCurrentDisplay());
    }

    @Test
    void testAppendDigit_MaxLength() {
        // Append more than MAX_DISPLAY_DIGITS
        for (int i = 0; i < AppConstants.MAX_DISPLAY_DIGITS + 5; i++) {
            calculator.appendDigit("9");
        }
        assertEquals(AppConstants.MAX_DISPLAY_DIGITS, calculator.getCurrentDisplay().length());
    }

    @Test
    void testAppendDecimal_FirstDecimal() {
        calculator.appendDigit("5");
        calculator.appendDecimal();
        assertEquals("5.", calculator.getCurrentDisplay());
    }

    @Test
    void testAppendDecimal_OnlyOnce() {
        calculator.appendDigit("5");
        calculator.appendDecimal();
        calculator.appendDigit("3");
        calculator.appendDecimal(); // Should be ignored
        assertEquals("5.3", calculator.getCurrentDisplay());
    }

    @Test
    void testAppendDecimal_StartsWithZero() {
        calculator.appendDecimal();
        assertEquals("0.", calculator.getCurrentDisplay());
    }

    @Test
    void testSetOperation_Addition() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);

        assertEquals(Calculator.Operation.ADD, calculator.getCurrentOperation());
        assertTrue(calculator.isNewNumber());
    }

    @Test
    void testSetOperation_ChainOperations() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");
        calculator.setOperation(Calculator.Operation.MULTIPLY); // Should calculate 5+3 first

        assertEquals("8", calculator.getCurrentDisplay());
    }

    @Test
    void testCalculateResult_Addition() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");
        double result = calculator.calculateResult();

        assertEquals(8.0, result, DELTA);
        assertEquals("8", calculator.getCurrentDisplay());
    }

    @Test
    void testCalculateResult_Subtraction() {
        calculator.appendDigit("10");
        calculator.setOperation(Calculator.Operation.SUBTRACT);
        calculator.appendDigit("3");
        double result = calculator.calculateResult();

        assertEquals(7.0, result, DELTA);
        assertEquals("7", calculator.getCurrentDisplay());
    }

    @Test
    void testCalculateResult_Multiplication() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.MULTIPLY);
        calculator.appendDigit("3");
        double result = calculator.calculateResult();

        assertEquals(15.0, result, DELTA);
        assertEquals("15", calculator.getCurrentDisplay());
    }

    @Test
    void testCalculateResult_Division() {
        calculator.appendDigit("15");
        calculator.setOperation(Calculator.Operation.DIVIDE);
        calculator.appendDigit("3");
        double result = calculator.calculateResult();

        assertEquals(5.0, result, DELTA);
        assertEquals("5", calculator.getCurrentDisplay());
    }

    @Test
    void testCalculateResult_DivisionByZero() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.DIVIDE);
        calculator.appendDigit("0");
        double result = calculator.calculateResult();

        assertEquals(Double.POSITIVE_INFINITY, result);
        assertEquals(AppConstants.CALC_ERROR_DIV_ZERO, calculator.getCurrentDisplay());
    }

    @Test
    void testCalculateResult_NoOperation() {
        calculator.appendDigit("5");
        double result = calculator.calculateResult();

        assertEquals(5.0, result, DELTA);
    }

    @Test
    void testCalculateResult_WithDecimals() {
        calculator.appendDigit("5");
        calculator.appendDecimal();
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("2");
        calculator.appendDecimal();
        calculator.appendDigit("3");
        double result = calculator.calculateResult();

        assertEquals(7.8, result, DELTA);
    }

    @Test
    void testClear() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");

        calculator.clear();

        assertEquals(AppConstants.CALC_INITIAL_DISPLAY, calculator.getCurrentDisplay());
        assertEquals(Calculator.Operation.NONE, calculator.getCurrentOperation());
        assertTrue(calculator.isNewNumber());
    }

    @Test
    void testBackspace_RemovesLastDigit() {
        calculator.appendDigit("1");
        calculator.appendDigit("2");
        calculator.appendDigit("3");

        calculator.backspace();

        assertEquals("12", calculator.getCurrentDisplay());
    }

    @Test
    void testBackspace_LastDigit() {
        calculator.appendDigit("5");

        calculator.backspace();

        assertEquals(AppConstants.CALC_INITIAL_DISPLAY, calculator.getCurrentDisplay());
        assertTrue(calculator.isNewNumber());
    }

    @Test
    void testBackspace_WithNegativeNumber() {
        calculator.setCurrentDisplay("-5");

        calculator.backspace();

        assertEquals(AppConstants.CALC_INITIAL_DISPLAY, calculator.getCurrentDisplay());
    }

    @Test
    void testGetCurrentEquation_NoOperation() {
        calculator.appendDigit("5");

        assertEquals("", calculator.getCurrentEquation());
    }

    @Test
    void testGetCurrentEquation_WithOperation() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");

        assertEquals("5+3", calculator.getCurrentEquation());
    }

    @Test
    void testGetFullDisplay_NoOperation() {
        calculator.appendDigit("7");

        assertEquals("7", calculator.getFullDisplay());
    }

    @Test
    void testGetFullDisplay_OperationJustPressed() {
        calculator.appendDigit("7");
        calculator.setOperation(Calculator.Operation.ADD);

        assertEquals("7+", calculator.getFullDisplay());
    }

    @Test
    void testGetFullDisplay_EnteringSecondNumber() {
        calculator.appendDigit("7");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("6");

        assertEquals("7+6", calculator.getFullDisplay());
    }

    @Test
    void testEquationHistory_StoresCompletedEquations() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");
        calculator.calculateResult();

        List<String> history = calculator.getEquationHistory();
        assertEquals(1, history.size());
        assertEquals("5+3=8", history.get(0));
    }

    @Test
    void testEquationHistory_MultipleEquations() {
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

        List<String> history = calculator.getEquationHistory();
        assertEquals(2, history.size());
        assertEquals("5+3=8", history.get(0));
        assertEquals("10-2=8", history.get(1));
    }

    @Test
    void testEquationHistory_MaxSize() {
        // Add more than MAX_EQUATION_HISTORY equations
        for (int i = 0; i < AppConstants.MAX_EQUATION_HISTORY + 3; i++) {
            calculator.appendDigit("1");
            calculator.setOperation(Calculator.Operation.ADD);
            calculator.appendDigit("1");
            calculator.calculateResult();
        }

        List<String> history = calculator.getEquationHistory();
        assertEquals(AppConstants.MAX_EQUATION_HISTORY, history.size());
    }

    @Test
    void testComplexCalculation_ChainedOperations() {
        // Calculate (5 + 3) × 2
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.ADD);
        calculator.appendDigit("3");
        calculator.setOperation(Calculator.Operation.MULTIPLY); // Should calculate 5+3=8
        assertEquals("8", calculator.getCurrentDisplay());

        calculator.appendDigit("2");
        calculator.calculateResult();

        assertEquals("16", calculator.getCurrentDisplay());
    }

    @Test
    void testOperatorSymbols() {
        calculator.appendDigit("5");
        calculator.setOperation(Calculator.Operation.MULTIPLY);
        calculator.appendDigit("3");

        assertEquals("5×3", calculator.getCurrentEquation());

        calculator.clear();
        calculator.appendDigit("10");
        calculator.setOperation(Calculator.Operation.DIVIDE);
        calculator.appendDigit("2");

        assertEquals("10÷2", calculator.getCurrentEquation());
    }

    @Test
    void testResultFormatting_Integer() {
        calculator.appendDigit("10");
        calculator.setOperation(Calculator.Operation.DIVIDE);
        calculator.appendDigit("2");
        calculator.calculateResult();

        // Should display as "5" not "5.0"
        assertEquals("5", calculator.getCurrentDisplay());
    }

    @Test
    void testResultFormatting_Decimal() {
        calculator.appendDigit("10");
        calculator.setOperation(Calculator.Operation.DIVIDE);
        calculator.appendDigit("3");
        calculator.calculateResult();

        // Should have decimal places but no trailing zeros
        String result = calculator.getCurrentDisplay();
        assertTrue(result.startsWith("3.333"));
        assertFalse(result.endsWith("0"));
    }
}
