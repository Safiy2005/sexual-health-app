package com.sddp.sexualhealthapp.settings.ui;

import com.sddp.sexualhealthapp.settings.service.ParentalControlsPinService;
import com.sddp.sexualhealthapp.util.AppConstants;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Modal PIN prompt styled to match the rest of the app. Used whenever a
 * protected settings area needs to be unlocked.
 */
public final class ParentalControlsPinPrompt {

    private ParentalControlsPinPrompt() {
    }

    /**
     * Shows the PIN prompt as an application-modal overlay on top of the
     * owner scene and blocks until the user either enters the correct PIN or
     * cancels. Returns {@code true} only when the entered PIN matched.
     *
     * @param headline short explanation of why the PIN is being requested,
     *                 shown beneath the title
     * @param ownerNode any node currently on the owner scene (used to locate
     *                  the owner window and copy its theme classes)
     */
    public static boolean requestPin(String headline, Node ownerNode) {
        ParentalControlsPinService pinService = ParentalControlsPinService.getInstance();

        Scene ownerScene = ownerNode != null ? ownerNode.getScene() : null;
        Window ownerWindow = ownerScene != null ? ownerScene.getWindow() : null;

        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Parental controls");
        if (ownerWindow != null) {
            dialogStage.initOwner(ownerWindow);
        }

        Label title = new Label("Parental controls");
        title.getStyleClass().add("pin-dialog-title");

        Label subtitle = new Label(headline == null || headline.isBlank()
                ? "Enter your PIN to continue."
                : headline);
        subtitle.getStyleClass().add("pin-dialog-subtitle");
        subtitle.setWrapText(true);

        PasswordField pinField = new PasswordField();
        pinField.getStyleClass().addAll("search-field", "pin-dialog-field");
        pinField.setPromptText("PIN");

        Label error = new Label("Incorrect PIN. Please try again.");
        error.getStyleClass().add("pin-dialog-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("calendar-action-button");
        cancelButton.setCancelButton(true);

        Button unlockButton = new Button("Unlock");
        unlockButton.getStyleClass().add("calendar-action-button-primary");
        unlockButton.setDefaultButton(true);
        unlockButton.setDisable(true);

        HBox buttons = new HBox(10, cancelButton, unlockButton);
        buttons.getStyleClass().add("pin-dialog-buttons");
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, subtitle, pinField, error, buttons);
        card.getStyleClass().add("pin-dialog-card");
        card.setFillWidth(true);
        card.setMaxWidth(300);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane backdrop = new StackPane(card);
        backdrop.getStyleClass().add("pin-dialog-backdrop");
        backdrop.setPadding(new Insets(24));
        StackPane.setAlignment(card, Pos.CENTER);

        // Mirror theme + text size classes from the owner so the prompt
        // matches dark mode, high contrast, and text scaling.
        if (ownerScene != null && ownerScene.getRoot() != null) {
            ownerScene.getRoot().getStyleClass().stream()
                    .filter(cls -> cls.startsWith("theme-") || cls.startsWith("text-size-"))
                    .forEach(cls -> {
                        if (!backdrop.getStyleClass().contains(cls)) {
                            backdrop.getStyleClass().add(cls);
                        }
                    });
        }

        boolean[] accepted = { false };

        pinField.textProperty().addListener((obs, oldVal, newVal) -> {
            unlockButton.setDisable(!ParentalControlsPinService.isValidPinFormat(newVal));
            if (error.isVisible()) {
                error.setVisible(false);
                error.setManaged(false);
            }
        });

        Runnable attemptUnlock = () -> {
            if (!pinService.verifyPin(pinField.getText())) {
                error.setVisible(true);
                error.setManaged(true);
                pinField.selectAll();
                pinField.requestFocus();
                return;
            }
            accepted[0] = true;
            dialogStage.close();
        };

        unlockButton.setOnAction(event -> attemptUnlock.run());
        pinField.setOnAction(event -> {
            if (!unlockButton.isDisabled()) {
                attemptUnlock.run();
            }
        });
        cancelButton.setOnAction(event -> dialogStage.close());

        double width = ownerWindow != null && ownerWindow.getWidth() > 0 ? ownerWindow.getWidth() : 360;
        double height = ownerWindow != null && ownerWindow.getHeight() > 0 ? ownerWindow.getHeight() : 640;

        Scene scene = new Scene(backdrop, width, height);
        scene.setFill(Color.TRANSPARENT);
        for (String css : AppConstants.CSS_MAIN_APP_SCENE) {
            scene.getStylesheets().add(ParentalControlsPinPrompt.class.getResource(css).toExternalForm());
        }
        dialogStage.setScene(scene);

        if (ownerWindow != null) {
            dialogStage.setX(ownerWindow.getX());
            dialogStage.setY(ownerWindow.getY());
            dialogStage.setWidth(ownerWindow.getWidth());
            dialogStage.setHeight(ownerWindow.getHeight());
        }

        Platform.runLater(pinField::requestFocus);
        dialogStage.showAndWait();
        return accepted[0];
    }
}
