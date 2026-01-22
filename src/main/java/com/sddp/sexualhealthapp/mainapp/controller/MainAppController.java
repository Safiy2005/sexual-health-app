package com.sddp.sexualhealthapp.mainapp.controller;

import com.sddp.sexualhealthapp.navigation.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 * Controller for the main application placeholder.
 * This is a simple placeholder that the team will replace with actual app functionality.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class MainAppController {

    /**
     * Handles the back to calculator button click (for testing).
     *
     * @param event the action event from the button click
     */
    @FXML
    private void handleBackToCalculator(ActionEvent event) {
        SceneManager.getInstance().transitionToCalculator();
    }
}
