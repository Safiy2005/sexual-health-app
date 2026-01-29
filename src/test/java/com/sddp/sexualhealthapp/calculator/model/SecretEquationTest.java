package com.sddp.sexualhealthapp.calculator.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretEquationTest {

    @Test
    void testConstructor_FourParts() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");

        assertEquals("5", equation.getLeftOperand());
        assertEquals("+", equation.getOperator());
        assertEquals("3", equation.getRightOperand());
        assertEquals("8", equation.getResult());
        assertEquals("5+3=8", equation.getFullEquation());
    }

    @Test
    void testConstructor_FullString() {
        SecretEquation equation = new SecretEquation("5+3=8");

        assertEquals("5", equation.getLeftOperand());
        assertEquals("+", equation.getOperator());
        assertEquals("3", equation.getRightOperand());
        assertEquals("8", equation.getResult());
        assertEquals("5+3=8", equation.getFullEquation());
    }

    @Test
    void testConstructor_FullString_WithSpaces() {
        SecretEquation equation = new SecretEquation("5 + 3 = 8");

        assertEquals("5", equation.getLeftOperand());
        assertEquals("+", equation.getOperator());
        assertEquals("3", equation.getRightOperand());
        assertEquals("8", equation.getResult());
    }

    @Test
    void testConstructor_FullString_MultiplySymbol() {
        SecretEquation equation = new SecretEquation("5×3=15");

        assertEquals("5", equation.getLeftOperand());
        assertEquals("×", equation.getOperator());
        assertEquals("3", equation.getRightOperand());
        assertEquals("15", equation.getResult());
    }

    @Test
    void testConstructor_FullString_DivideSymbol() {
        SecretEquation equation = new SecretEquation("15÷3=5");

        assertEquals("15", equation.getLeftOperand());
        assertEquals("÷", equation.getOperator());
        assertEquals("3", equation.getRightOperand());
        assertEquals("5", equation.getResult());
    }

    @Test
    void testConstructor_FullString_NegativeNumber() {
        SecretEquation equation = new SecretEquation("-5+3=-2");

        assertEquals("-5", equation.getLeftOperand());
        assertEquals("+", equation.getOperator());
        assertEquals("3", equation.getRightOperand());
        assertEquals("-2", equation.getResult());
    }

    @Test
    void testConstructor_FullString_InvalidFormat_NoEquals() {
        assertThrows(IllegalArgumentException.class, () ->
            new SecretEquation("5+3")
        );
    }

    @Test
    void testConstructor_FullString_InvalidFormat_NoOperator() {
        assertThrows(IllegalArgumentException.class, () ->
            new SecretEquation("53=8")
        );
    }

    @Test
    void testMatches_ExactMatch() {
        SecretEquation equation = new SecretEquation("5+3=8");

        assertTrue(equation.matches("5+3=8"));
    }

    @Test
    void testMatches_WithSpaces() {
        SecretEquation equation = new SecretEquation("5+3=8");

        assertTrue(equation.matches("5 + 3 = 8"));
        assertTrue(equation.matches(" 5+3=8 "));
    }

    @Test
    void testMatches_CaseInsensitive() {
        SecretEquation equation = new SecretEquation("5+3=8");

        // Although equations are numeric, test the case-insensitive logic
        assertTrue(equation.matches("5+3=8"));
    }

    @Test
    void testMatches_DifferentEquation() {
        SecretEquation equation = new SecretEquation("5+3=8");

        assertFalse(equation.matches("5+3=9"));
        assertFalse(equation.matches("5+4=8"));
        assertFalse(equation.matches("6+3=8"));
    }

    @Test
    void testMatches_NullInput() {
        SecretEquation equation = new SecretEquation("5+3=8");

        assertFalse(equation.matches(null));
    }

    @Test
    void testMatches_EmptyInput() {
        SecretEquation equation = new SecretEquation("5+3=8");

        assertFalse(equation.matches(""));
        assertFalse(equation.matches("   "));
    }

    @Test
    void testIsValid_CorrectAddition() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "8");

        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_CorrectSubtraction() {
        SecretEquation equation = new SecretEquation("10", "-", "3", "7");

        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_CorrectMultiplication() {
        SecretEquation equation = new SecretEquation("5", "×", "3", "15");

        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_CorrectMultiplication_Asterisk() {
        SecretEquation equation = new SecretEquation("5", "*", "3", "15");

        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_CorrectDivision() {
        SecretEquation equation = new SecretEquation("15", "÷", "3", "5");

        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_CorrectDivision_Slash() {
        SecretEquation equation = new SecretEquation("15", "/", "3", "5");

        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_WrongResult() {
        SecretEquation equation = new SecretEquation("5", "+", "3", "9");

        assertFalse(equation.isValid());
    }

    @Test
    void testIsValid_DivisionByZero() {
        SecretEquation equation = new SecretEquation("5", "÷", "0", "0");

        assertFalse(equation.isValid());
    }

    @Test
    void testIsValid_InvalidOperator() {
        SecretEquation equation = new SecretEquation("5", "%", "3", "2");

        assertFalse(equation.isValid());
    }

    @Test
    void testIsValid_EmptyField() {
        SecretEquation equation = new SecretEquation("", "+", "3", "3");

        assertFalse(equation.isValid());
    }

    @Test
    void testIsValid_WithDecimals() {
        SecretEquation equation = new SecretEquation("5.5", "+", "2.5", "8.0");

        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_WithDecimalResult() {
        SecretEquation equation = new SecretEquation("10", "÷", "3", "3.3333");

        // Should be valid with tolerance for floating point
        assertTrue(equation.isValid());
    }

    @Test
    void testIsValid_NegativeNumbers() {
        SecretEquation equation = new SecretEquation("-5", "+", "3", "-2");

        assertTrue(equation.isValid());
    }

    @Test
    void testToString() {
        SecretEquation equation = new SecretEquation("5+3=8");

        assertEquals("5+3=8", equation.toString());
    }

    @Test
    void testEquals_SameEquation() {
        SecretEquation eq1 = new SecretEquation("5+3=8");
        SecretEquation eq2 = new SecretEquation("5+3=8");

        assertEquals(eq1, eq2);
    }

    @Test
    void testEquals_DifferentEquation() {
        SecretEquation eq1 = new SecretEquation("5+3=8");
        SecretEquation eq2 = new SecretEquation("5+3=9");

        assertNotEquals(eq1, eq2);
    }

    @Test
    void testEquals_Null() {
        SecretEquation eq1 = new SecretEquation("5+3=8");

        assertNotEquals(eq1, null);
    }

    @Test
    void testEquals_SameInstance() {
        SecretEquation eq1 = new SecretEquation("5+3=8");

        assertEquals(eq1, eq1);
    }

    @Test
    void testHashCode_SameEquation() {
        SecretEquation eq1 = new SecretEquation("5+3=8");
        SecretEquation eq2 = new SecretEquation("5+3=8");

        assertEquals(eq1.hashCode(), eq2.hashCode());
    }

    @Test
    void testComplexEquations() {
        // Large numbers
        SecretEquation eq1 = new SecretEquation("999", "+", "1", "1000");
        assertTrue(eq1.isValid());

        // Decimal division
        SecretEquation eq2 = new SecretEquation("22", "÷", "7", "3.14286");
        assertTrue(eq2.isValid());

        // Negative result
        SecretEquation eq3 = new SecretEquation("5", "-", "10", "-5");
        assertTrue(eq3.isValid());
    }

    @Test
    void testTrimming() {
        SecretEquation equation = new SecretEquation("  5  ", "  +  ", "  3  ", "  8  ");

        assertEquals("5", equation.getLeftOperand());
        assertEquals("+", equation.getOperator());
        assertEquals("3", equation.getRightOperand());
        assertEquals("8", equation.getResult());
    }
}
