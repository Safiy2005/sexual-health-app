package com.sddp.sexualhealthapp.calculator.model;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Model class representing the overall application state.
 * Manages which mode the application is in and tracks attempted equations
 * for authentication purposes.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class CalculatorState {

    /**
     * Enum representing the possible modes/states of the application.
     */
    public enum AppMode {
        /** Calculator mode - disguised front */
        CALCULATOR,
        /** Setup mode - first-time configuration */
        SETUP,
        /** Main app mode - full application access */
        MAIN_APP
    }

    private AppMode currentMode;
    private boolean isSecretEquationSet;
    private Queue<String> recentAttemptedEquations;
    private static final int MAX_TRACKED_ATTEMPTS = 10;

    /**
     * Constructs a new CalculatorState with default values.
     */
    public CalculatorState() {
        this.currentMode = AppMode.SETUP;
        this.isSecretEquationSet = false;
        this.recentAttemptedEquations = new LinkedList<>();
    }

    /**
     * Transitions to a new application mode.
     *
     * @param newMode the mode to transition to
     * @throws IllegalStateException if the transition is not allowed
     */
    public void transitionTo(AppMode newMode) {
        if (!canTransitionTo(newMode)) {
            throw new IllegalStateException(
                "Cannot transition from " + currentMode + " to " + newMode
            );
        }
        this.currentMode = newMode;
    }

    /**
     * Checks if a transition to the specified mode is allowed.
     *
     * @param targetMode the target mode
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(AppMode targetMode) {
        switch (currentMode) {
            case SETUP:
                // From SETUP, can only go to CALCULATOR after secret is set
                return targetMode == AppMode.CALCULATOR && isSecretEquationSet;

            case CALCULATOR:
                // From CALCULATOR, can go to MAIN_APP after authentication
                return targetMode == AppMode.MAIN_APP;

            case MAIN_APP:
                // From MAIN_APP, can go back to CALCULATOR (escape mode)
                return targetMode == AppMode.CALCULATOR;

            default:
                return false;
        }
    }

    /**
     * Adds an attempted equation to the tracking queue.
     * This is used for analytics and future rate-limiting features.
     *
     * @param equation the equation that was attempted
     */
    public void addAttemptedEquation(String equation) {
        recentAttemptedEquations.offer(equation);
        if (recentAttemptedEquations.size() > MAX_TRACKED_ATTEMPTS) {
            recentAttemptedEquations.poll();
        }
    }

    /**
     * Gets the most recent attempted equation.
     *
     * @return the most recent equation, or null if no attempts have been made
     */
    public String getLastAttemptedEquation() {
        if (recentAttemptedEquations.isEmpty()) {
            return null;
        }
        // Peek at the last element without removing it
        String[] attempts = recentAttemptedEquations.toArray(new String[0]);
        return attempts[attempts.length - 1];
    }

    /**
     * Gets the number of recent equation attempts.
     *
     * @return the number of tracked attempts
     */
    public int getAttemptCount() {
        return recentAttemptedEquations.size();
    }

    /**
     * Clears the tracked equation attempts.
     */
    public void clearAttempts() {
        recentAttemptedEquations.clear();
    }

    /**
     * Gets the current application mode.
     *
     * @return the current mode
     */
    public AppMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Sets the current mode (for initialization purposes).
     *
     * @param mode the mode to set
     */
    public void setCurrentMode(AppMode mode) {
        this.currentMode = mode;
    }

    /**
     * Checks if a secret equation has been set.
     *
     * @return true if a secret equation is set, false otherwise
     */
    public boolean isSecretEquationSet() {
        return isSecretEquationSet;
    }

    /**
     * Sets whether a secret equation has been configured.
     *
     * @param secretEquationSet true if a secret equation is set
     */
    public void setSecretEquationSet(boolean secretEquationSet) {
        this.isSecretEquationSet = secretEquationSet;
    }

    /**
     * Resets the calculator state to initial values.
     */
    public void reset() {
        this.currentMode = AppMode.SETUP;
        this.isSecretEquationSet = false;
        this.recentAttemptedEquations.clear();
    }
}
