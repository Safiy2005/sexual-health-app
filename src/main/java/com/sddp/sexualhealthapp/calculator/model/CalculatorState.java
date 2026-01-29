package com.sddp.sexualhealthapp.calculator.model;

public class CalculatorState {

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

    public CalculatorState() {
        this.currentMode = AppMode.SETUP;
        this.isSecretEquationSet = false;
    }

    public void transitionTo(AppMode newMode) {
        if (!canTransitionTo(newMode)) {
            throw new IllegalStateException(
                "Cannot transition from " + currentMode + " to " + newMode
            );
        }
        this.currentMode = newMode;
    }

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

    public boolean isSecretEquationSet() {
        return isSecretEquationSet;
    }

    public void setSecretEquationSet(boolean secretEquationSet) {
        this.isSecretEquationSet = secretEquationSet;
    }

    public void reset() {
        this.currentMode = AppMode.SETUP;
        this.isSecretEquationSet = false;
    }
}
