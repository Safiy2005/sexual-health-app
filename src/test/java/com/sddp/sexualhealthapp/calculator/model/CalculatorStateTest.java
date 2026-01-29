package com.sddp.sexualhealthapp.calculator.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorStateTest {

    private CalculatorState state;

    @BeforeEach
    void setUp() {
        state = new CalculatorState();
    }

    @Test
    void testInitialState() {
        assertEquals(CalculatorState.AppMode.SETUP, state.getCurrentMode());
        assertFalse(state.isSecretEquationSet());
    }

    @Test
    void testTransition_SetupToCalculator() {
        state.setSecretEquationSet(true);

        state.transitionTo(CalculatorState.AppMode.CALCULATOR);

        assertEquals(CalculatorState.AppMode.CALCULATOR, state.getCurrentMode());
    }

    @Test
    void testTransition_SetupToCalculator_WithoutSecret() {
        assertThrows(IllegalStateException.class, () ->
            state.transitionTo(CalculatorState.AppMode.CALCULATOR)
        );
    }

    @Test
    void testTransition_CalculatorToMainApp() {
        state.setSecretEquationSet(true);
        state.transitionTo(CalculatorState.AppMode.CALCULATOR);

        state.transitionTo(CalculatorState.AppMode.MAIN_APP);

        assertEquals(CalculatorState.AppMode.MAIN_APP, state.getCurrentMode());
    }

    @Test
    void testTransition_MainAppToCalculator() {
        state.setSecretEquationSet(true);
        state.transitionTo(CalculatorState.AppMode.CALCULATOR);
        state.transitionTo(CalculatorState.AppMode.MAIN_APP);

        state.transitionTo(CalculatorState.AppMode.CALCULATOR);

        assertEquals(CalculatorState.AppMode.CALCULATOR, state.getCurrentMode());
    }

    @Test
    void testCanTransition_SetupToCalculator_WithSecret() {
        state.setSecretEquationSet(true);

        assertTrue(state.canTransitionTo(CalculatorState.AppMode.CALCULATOR));
    }

    @Test
    void testCanTransition_SetupToCalculator_WithoutSecret() {
        assertFalse(state.canTransitionTo(CalculatorState.AppMode.CALCULATOR));
    }

    @Test
    void testCanTransition_CalculatorToMainApp() {
        state.setSecretEquationSet(true);
        state.transitionTo(CalculatorState.AppMode.CALCULATOR);

        assertTrue(state.canTransitionTo(CalculatorState.AppMode.MAIN_APP));
    }

    @Test
    void testCanTransition_MainAppToCalculator() {
        state.setSecretEquationSet(true);
        state.transitionTo(CalculatorState.AppMode.CALCULATOR);
        state.transitionTo(CalculatorState.AppMode.MAIN_APP);

        assertTrue(state.canTransitionTo(CalculatorState.AppMode.CALCULATOR));
    }

    @Test
    void testCanTransition_InvalidTransitions() {
        // SETUP -> MAIN_APP not allowed
        assertFalse(state.canTransitionTo(CalculatorState.AppMode.MAIN_APP));

        // CALCULATOR -> SETUP not allowed
        state.setSecretEquationSet(true);
        state.transitionTo(CalculatorState.AppMode.CALCULATOR);
        assertFalse(state.canTransitionTo(CalculatorState.AppMode.SETUP));
    }

    @Test
    void testSetCurrentMode() {
        state.setCurrentMode(CalculatorState.AppMode.CALCULATOR);

        assertEquals(CalculatorState.AppMode.CALCULATOR, state.getCurrentMode());
    }

    @Test
    void testSetSecretEquationSet() {
        assertFalse(state.isSecretEquationSet());

        state.setSecretEquationSet(true);

        assertTrue(state.isSecretEquationSet());
    }

    @Test
    void testReset() {
        state.setSecretEquationSet(true);
        state.setCurrentMode(CalculatorState.AppMode.CALCULATOR);

        state.reset();

        assertEquals(CalculatorState.AppMode.SETUP, state.getCurrentMode());
        assertFalse(state.isSecretEquationSet());
    }

    @Test
    void testAppModeEnum() {
        assertEquals(3, CalculatorState.AppMode.values().length);
        assertEquals(CalculatorState.AppMode.CALCULATOR, CalculatorState.AppMode.valueOf("CALCULATOR"));
        assertEquals(CalculatorState.AppMode.SETUP, CalculatorState.AppMode.valueOf("SETUP"));
        assertEquals(CalculatorState.AppMode.MAIN_APP, CalculatorState.AppMode.valueOf("MAIN_APP"));
    }

    @Test
    void testCompleteFlow_SetupToMainApp() {
        // Start in SETUP mode
        assertEquals(CalculatorState.AppMode.SETUP, state.getCurrentMode());

        // Complete setup
        state.setSecretEquationSet(true);

        // Transition to CALCULATOR
        state.transitionTo(CalculatorState.AppMode.CALCULATOR);
        assertEquals(CalculatorState.AppMode.CALCULATOR, state.getCurrentMode());

        // Authenticate successfully
        state.transitionTo(CalculatorState.AppMode.MAIN_APP);
        assertEquals(CalculatorState.AppMode.MAIN_APP, state.getCurrentMode());

        // Can go back to calculator (escape mode)
        state.transitionTo(CalculatorState.AppMode.CALCULATOR);
        assertEquals(CalculatorState.AppMode.CALCULATOR, state.getCurrentMode());
    }
}
